///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.jochre.search;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochreIndexDirectory.Instructions;
import com.joliciel.jochre.search.SearchStatusHolder.SearchStatus;
import com.joliciel.jochre.search.alto.AltoDocument;
import com.joliciel.jochre.search.alto.AltoPage;
import com.joliciel.jochre.search.alto.AltoPageConsumer;
import com.joliciel.jochre.search.alto.AltoReader;
import com.joliciel.jochre.search.alto.AltoString;
import com.joliciel.jochre.search.alto.AltoStringFixer;
import com.joliciel.jochre.search.alto.AltoTextBlock;
import com.joliciel.jochre.search.alto.AltoTextLine;
import com.joliciel.jochre.search.feedback.Correction;
import com.joliciel.jochre.search.feedback.FeedbackSuggestion;

public class JochreIndexBuilder implements Runnable, TokenExtractor {
  private static final Logger LOG = LoggerFactory.getLogger(JochreIndexBuilder.class);
  private final File contentDir;
  private final SearchStatusHolder searchStatusHolder;
  private final String configId;
  private final JochreSearchConfig config;
  private final JochreSearchManager manager;

  private final boolean forceUpdate;

  private final int wordsPerDoc;

  private List<JochreToken> currentStrings = null;

  /**
   * 
   * @param config
   * @param manager
   * @param forceUpdate
   *          should all documents in the index be updated, or only those with
   *          changes more recent than the last update date.
   * @param feedbackDAO
   *          if not null, will read suggestions from the database
   * @param searchStatusHolder
   * @throws IOException
   */
  public JochreIndexBuilder(String configId, boolean forceUpdate) throws IOException {
    this.searchStatusHolder = SearchStatusHolder.getInstance();
    this.configId = configId;
    this.config = JochreSearchConfig.getInstance(configId);
    this.contentDir = config.getContentDir();
    this.manager = JochreSearchManager.getInstance(configId);
    this.wordsPerDoc = config.getConfig().getInt("index-builder.words-per-document");
    this.forceUpdate = forceUpdate;
  }

  @Override
  public void run() {
    this.updateIndex();
  }

  /**
   * Update the index by scanning all of the sub-directories of this contentDir
   * for updates. The sub-directory path is considered to uniquely identify a
   * work. Sub-directory contents are described in {@link JochreIndexDirectory}. A
   * work will only be updated if the date of it's text layer is later than the
   * previous index date (stored in the index), or if forceUpdate=true. If the
   * work is updated, any previous documents with the same path are first deleted.
   * Multiple Lucene documents can be created from a single work, if
   * {@link #getWordsPerDoc()}&gt;0.
   */
  public void updateIndex() {
    long startTime = System.currentTimeMillis();
    try {
      if (searchStatusHolder.getStatus() != SearchStatus.WAITING) {
        throw new IndexingUnderwayException();
      }

      searchStatusHolder.setStatus(SearchStatus.PREPARING);

      Map<String, Analyzer> analyzerPerField = new HashMap<>();
      analyzerPerField.put(JochreIndexField.text.name(), new JochreTextLayerAnalyser(this, configId));
      analyzerPerField.put(JochreIndexField.author.name(), new JochreKeywordAnalyser(configId));
      analyzerPerField.put(JochreIndexField.authorEnglish.name(), new JochreKeywordAnalyser(configId));
      analyzerPerField.put(JochreIndexField.publisher.name(), new JochreKeywordAnalyser(configId));

      PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new JochreMetaDataAnalyser(configId),
          analyzerPerField);
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
      iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      try (IndexWriter indexWriter = new IndexWriter(manager.getIndexDir(), iwc)) {

        File[] subdirs = contentDir.listFiles(new FileFilter() {

          @Override
          public boolean accept(File pathname) {
            return pathname.isDirectory();
          }
        });

        if (subdirs.length == 0)
          throw new IllegalArgumentException("content dir is empty: " + contentDir.getPath());

        Arrays.sort(subdirs);

        searchStatusHolder.setStatus(SearchStatus.BUSY);
        searchStatusHolder.setTotalCount(subdirs.length);

        for (File subdir : subdirs) {
          try {
            searchStatusHolder.setAction("Indexing " + subdir.getName());
            this.processDocument(indexWriter, subdir, forceUpdate);
            searchStatusHolder.incrementSuccessCount(1);
          } catch (Exception e) {
            LOG.error("Failed to index " + subdir.getName(), e);
            searchStatusHolder.incrementFailureCount(1);
          }
        }

        LOG.info("Commiting index...");
        searchStatusHolder.setStatus(SearchStatus.COMMITING);
        indexWriter.commit();
        indexWriter.close();
        manager.getManager().maybeRefresh();
      }
    } catch (IOException e) {
      LOG.error("Failed to commit indexWriter", e);
      throw new RuntimeException(e);
    } finally {
      searchStatusHolder.setStatus(SearchStatus.WAITING);
      long endTime = System.currentTimeMillis();
      long totalTime = endTime - startTime;
      LOG.info("Total time (ms): " + totalTime);
    }
  }

  private void processDocument(IndexWriter indexWriter, File documentDir, boolean forceUpdate) {
    try {
      boolean updateIndex = false;

      JochreIndexDirectory jochreIndexDirectory = new JochreIndexDirectory(documentDir, configId);
      Instructions instructions = jochreIndexDirectory.getInstructions();
      switch (instructions) {
      case Delete:
        this.deleteDocumentInternal(indexWriter, jochreIndexDirectory);
        return;
      case Skip:
        return;
      case Update:
        if (LOG.isDebugEnabled())
          LOG.debug("For " + documentDir.getName() + " found update instructions.");
        updateIndex = true;
      case None:
        // do nothing
        break;
      }

      if (forceUpdate)
        updateIndex = true;

      if (!updateIndex) {
        long ocrDate = jochreIndexDirectory.getAltoFile().lastModified();
        if (jochreIndexDirectory.getMetaDataFile() != null) {
          long metaDate = jochreIndexDirectory.getMetaDataFile().lastModified();
          if (metaDate > ocrDate)
            ocrDate = metaDate;
        }
        long lastIndexDate = Long.MIN_VALUE;

        IndexSearcher indexSearcher = manager.getManager().acquire();
        try {
          Term term = new Term(JochreIndexField.name.name(), jochreIndexDirectory.getName());
          Query termQuery = new TermQuery(term);
          TopDocs topDocs = indexSearcher.search(termQuery, 1);
          if (topDocs.scoreDocs.length > 0) {
            Document doc = indexSearcher.doc(topDocs.scoreDocs[0].doc);
            lastIndexDate = doc.getField(JochreIndexField.indexTime.name()).numericValue().longValue();
          }
        } finally {
          manager.getManager().release(indexSearcher);
        }

        if (LOG.isTraceEnabled())
          LOG.trace("lastIndexDate: " + lastIndexDate + ", ocrDate: " + ocrDate);
        if (ocrDate > lastIndexDate) {
          if (LOG.isDebugEnabled()) {
            Instant lastIndexInstant = Instant.ofEpochMilli(lastIndexDate);
            Instant ocrInstant = Instant.ofEpochMilli(ocrDate);
            LOG.debug("For " + documentDir.getName() + "OCR date more recent than index date. lastIndexDate: "
                + lastIndexInstant.toString() + ", ocrDate: " + ocrInstant.toString());
          }
          updateIndex = true;
        }
      }

      if (updateIndex) {
        this.updateDocumentInternal(indexWriter, jochreIndexDirectory, -1, -1);
      } else {
        if (LOG.isTraceEnabled())
          LOG.trace("Index for " + documentDir.getName() + " already up-to-date.");
      } // should update index?

      if (instructions.equals(Instructions.Update)) {
        jochreIndexDirectory.removeUpdateInstructions();
      }
    } catch (IOException e) {
      LOG.error("Failed to process " + documentDir.getName(), e);
      throw new RuntimeException(e);
    }
  }

  private void updateDocumentInternal(IndexWriter indexWriter, JochreIndexDirectory jochreIndexDirectory, int startPage,
      int endPage) {
    try {
      LOG.info("Updating index for " + jochreIndexDirectory.getName());

      this.deleteDocumentInternal(indexWriter, jochreIndexDirectory);

      AltoDocument altoDoc = new AltoDocument(jochreIndexDirectory.getName());
      AltoReader reader = new AltoReader(altoDoc);
      AltoPageIndexer altoPageIndexer = new AltoPageIndexer(indexWriter, this, jochreIndexDirectory, startPage,
          endPage);
      reader.addConsumer(altoPageIndexer);

      UnclosableInputStream uis = jochreIndexDirectory.getAltoInputStream();
      reader.parseFile(uis, jochreIndexDirectory.getName());
      uis.reallyClose();
    } catch (IOException e) {
      LOG.error("Failed to update jochreIndexDirectory " + jochreIndexDirectory.getName(), e);
      throw new RuntimeException(e);
    }
  }

  private final class AltoPageIndexer implements AltoPageConsumer {
    private JochreIndexBuilder parent;
    private final IndexWriter indexWriter;

    private int docCount = 0;
    private int cumulWordCount = 0;
    private List<AltoPage> currentPages = new ArrayList<>();
    private List<AltoPage> previousPages = new ArrayList<>();
    private List<JochreToken> previousStrings = new ArrayList<>();
    private List<JochreToken> currentStrings = new ArrayList<>();
    private int startPage = -1;
    private int endPage = -1;
    private AltoStringFixer altoStringFixer;

    private JochreIndexDirectory directory;
    private Map<Integer, List<FeedbackSuggestion>> pageSuggestionMap;
    private Map<JochreIndexField, List<Correction>> correctionMap;

    public AltoPageIndexer(IndexWriter indexWriter, JochreIndexBuilder parent, JochreIndexDirectory directory,
        int startPage, int endPage) {
      this.indexWriter = indexWriter;
      this.parent = parent;
      this.directory = directory;
      this.startPage = startPage;
      this.endPage = endPage;
      this.altoStringFixer = AltoStringFixer.getInstance(configId);
      this.pageSuggestionMap = FeedbackSuggestion.findSuggestions(directory.getPath(), configId);
      this.correctionMap = Correction.findCorrections(directory.getPath(), configId);
    }

    @Override
    public void onNextPage(AltoPage page) {
      if (startPage >= 0 && page.getIndex() < startPage)
        return;
      if (endPage >= 0 && page.getIndex() > endPage)
        return;
      LOG.debug("Processing page: " + page.getIndex());
      currentPages.add(page);

      List<FeedbackSuggestion> suggestions = pageSuggestionMap.get(page.getIndex());

      Map<Rectangle, List<FeedbackSuggestion>> suggestionMap = new HashMap<>();
      if (suggestions != null) {
        for (FeedbackSuggestion suggestion : suggestions) {
          Rectangle rectangle = suggestion.getWord().getRectangle();
          List<FeedbackSuggestion> wordSuggestions = suggestionMap.get(rectangle);
          if (wordSuggestions == null) {
            wordSuggestions = new ArrayList<>();
            suggestionMap.put(rectangle, wordSuggestions);
          }
          wordSuggestions.add(suggestion);
        }
      }

      for (AltoTextBlock textBlock : page.getTextBlocks()) {
        textBlock.joinHyphens();
        if (this.altoStringFixer != null)
          this.altoStringFixer.fix(textBlock);

        for (AltoTextLine textLine : textBlock.getTextLines()) {
          for (AltoString string : textLine.getStrings()) {
            if (string.isWhiteSpace())
              continue;

            List<FeedbackSuggestion> wordSuggestions = suggestionMap.get(string.getRectangle());
            if (wordSuggestions != null) {
              FeedbackSuggestion lastSuggestion = wordSuggestions.get(wordSuggestions.size() - 1);
              LOG.debug("Applying suggestion: " + lastSuggestion.getText() + " instead of " + string.getContent());
              string.setContent(lastSuggestion.getText());
            }
            currentStrings.add(string);
          }
        }
      }

      int wordCount = page.wordCount();
      cumulWordCount += wordCount;
      LOG.debug("Word count: " + wordCount + ", cumul word count: " + cumulWordCount);
      if (wordsPerDoc > 0 && cumulWordCount >= wordsPerDoc) {
        if (previousPages.size() > 0) {
          parent.setCurrentStrings(previousStrings);
          LOG.debug("Creating new index doc: " + docCount);
          JochreIndexDocument indexDoc = new JochreIndexDocument(directory, docCount, previousPages, correctionMap,
              configId);
          indexDoc.save(indexWriter);
          docCount++;
        }

        previousPages = currentPages;
        previousStrings = currentStrings;

        cumulWordCount = 0;
        parent.setCurrentStrings(new ArrayList<JochreToken>());
        currentPages = new ArrayList<>();
        currentStrings = new ArrayList<>();
      }
    }

    @Override
    public void onComplete() {
      previousPages.addAll(currentPages);
      previousStrings.addAll(currentStrings);
      parent.setCurrentStrings(previousStrings);
      if (previousPages.size() > 0) {
        LOG.debug("Creating new index doc: " + docCount);
        JochreIndexDocument indexDoc = new JochreIndexDocument(directory, docCount, previousPages, correctionMap,
            configId);
        indexDoc.save(indexWriter);
        docCount++;
      }
    }
  }

  private void deleteDocumentInternal(IndexWriter indexWriter, JochreIndexDirectory jochreIndexDirectory) {
    try {
      Term term = new Term(JochreIndexField.path.name(), jochreIndexDirectory.getPath());
      indexWriter.deleteDocuments(term);
    } catch (IOException e) {
      LOG.error("Failed to delete jochreIndexDirectory " + jochreIndexDirectory.getName(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<JochreToken> findTokens(String fieldName, Reader input) {
    return currentStrings;
  }

  public List<JochreToken> getCurrentStrings() {
    return currentStrings;
  }

  public void setCurrentStrings(List<JochreToken> currentStrings) {
    this.currentStrings = currentStrings;
  }

  /**
   * By default should all documents in the index be updated, or only those with
   * changes more recent than the update date.
   */
  public boolean isForceUpdate() {
    return forceUpdate;
  }

  public File getContentDir() {
    return contentDir;
  }
}
