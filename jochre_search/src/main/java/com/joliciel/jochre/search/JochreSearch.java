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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.mail.MessagingException;

import com.joliciel.jochre.search.feedback.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.joliciel.jochre.search.JochreQuery.SortBy;
import com.joliciel.jochre.search.highlight.HighlightManager;
import com.joliciel.jochre.search.highlight.Highlighter;
import com.joliciel.jochre.search.highlight.ImageSnippet;
import com.joliciel.jochre.search.highlight.LuceneQueryHighlighter;
import com.joliciel.jochre.search.highlight.Snippet;
import com.joliciel.jochre.search.lexicon.LexicalEntryReader;
import com.joliciel.jochre.search.lexicon.Lexicon;
import com.joliciel.jochre.search.lexicon.RegexLexicalEntryReader;
import com.joliciel.jochre.search.lexicon.TextFileLexicon;
import com.joliciel.jochre.utils.Either;
import com.joliciel.jochre.utils.JochreException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import freemarker.template.TemplateException;

/**
 * Command-line entry point into Jochre Search.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreSearch {
  private static final Logger LOG = LoggerFactory.getLogger(JochreSearch.class);
  private static DecimalFormatSymbols enSymbols = new DecimalFormatSymbols(Locale.US);

  private final String configId;

  public JochreSearch(String configId) {
    if (configId == null) {
      Config config = ConfigFactory.load();
      this.configId = config.getString("jochre.search.config-id");
    } else {
      this.configId = configId;
    }
  }

  public JochreSearch() {
    this(null);
  }

  public enum Command {
    /**
     * Start a thread to update the index, takin ginto account any recent changes.
     */
    index("application/json;charset=UTF-8"),
    /**
     * Search the index.
     */
    search("application/json;charset=UTF-8"),
    /**
     * Return the terms corresponding to a given search query.
     */
    highlight("application/json;charset=UTF-8"),
    /**
     * Return the most relevant snippets corresponding to a given search query.
     */
    snippets("application/json;charset=UTF-8"),
    /**
     * List all of the fields in a given index document.
     */
    view("application/json;charset=UTF-8"),
    /**
     * List all of the terms in a given index document.
     */
    list("application/json;charset=UTF-8"),
    /**
     * Return the image corresponding to a specific word.
     */
    wordImage("image/png"),
    /**
     * Make a suggestion for an OCR error.
     */
    suggest("application/json;charset=UTF-8"),
    /**
     * Suggest a correction for document metadata.
     */
    correct("application/json;charset=UTF-8"),
    /**
     * Send an e-mail for a given correction.
     */
    sendCorrectionEmail("application/json;charset=UTF-8"),
    /**
     * Undo a correction for document metadata,
     */
    undo("application/json;charset=UTF-8"),
    /**
     * Serialize a lexicon.
     */
    serializeLexicon("application/json;charset=UTF-8"),
    /**
     * Deserialize a lexicon and test certain words.
     */
    deserializeLexicon("application/json;charset=UTF-8"),
    /**
     * Return the highlighted text corresponding to a particular snippet.
     */
    textSnippet("application/json;charset=UTF-8"),
    /**
     * Return the image corresponding to a particular snippet.
     */
    imageSnippet("image/png"),
    /**
     * Find the word corresponding to a particular document location.
     */
    word("application/json;charset=UTF-8"),
    /**
     * Check the status of the index update thread.
     */
    indexStatus("application/json;charset=UTF-8"),
    /**
     * Refresh the index reader, to take into account any index updates.
     */
    refresh("application/json;charset=UTF-8"),
    /**
     * Search a given set of fields for the top-n terms matching a given prefix.
     */
    prefixSearch("application/json;charset=UTF-8"),
    /**
     * Returns the document metadata for a given document name or id.
     */
    document("application/json;charset=UTF-8"),
    /**
     * Write document contents as HTML.
     */
    contents("text/html;charset=UTF-8"),
    /**
     * How many books were indexed by the current searcher.
     */
    bookCount("application/json;charset=UTF-8"),
    /**
     * Export all suggestion text + image to the provided outDir.
     */
    exportSuggestions("application/json;charset=UTF-8");

    private final String contentType;

    private Command(String contentType) {
      this.contentType = contentType;
    }

    public String getContentType() {
      return contentType;
    }

  }

  public void execute(Map<String, String> argMap, Either<PrintWriter, OutputStream> output) {
    long startTime = System.currentTimeMillis();
    Command command = null;
    try {
      command = Command.valueOf(argMap.get("command"));
      argMap.remove("command");

      JochreSearchConfig config = JochreSearchConfig.getInstance(configId);

      LOG.debug("##### Arguments:");
      for (Entry<String, String> arg : argMap.entrySet()) {
        LOG.debug(arg.getKey() + ": " + arg.getValue());
      }

      boolean forceUpdate = false;
      String docName = null;
      int docIndex = -1;
      int docId = -1;
      Set<Integer> docIds = null;
      int decimalPlaces = config.getConfig().getInt("decimal-places");

      // query
      String queryString = null;
      List<String> authors = new ArrayList<>();
      boolean authorInclude = true;
      String titleQueryString = null;
      boolean expandInflections = true;
      SortBy sortBy = SortBy.Score;
      boolean sortAscending = true;
      Integer fromYear = null;
      Integer toYear = null;
      String reference = null;
      int pageNumber = 0;
      int resultsPerPage = config.getConfig().getInt("results-per-page");

      // lexicon handling
      String lexiconDirPath = null;
      String lexiconRegexPath = null;
      String word = null;

      // snippets
      int snippetCount = -1;
      double minWeight = 0.0;
      boolean includeText = false;
      boolean includeGraphics = false;
      String snippetJson = null;

      // word images
      int startOffset = -1;

      // suggestions
      String suggestion = null;
      String suggestion2 = null;
      String user = null;
      String languageCode = null;
      String fontCode = null;
      String ip = config.getConfig().getString("default-ip");

      // prefix search
      JochreIndexField field = null;
      String prefix = null;
      int maxResults = 0;

      // corrections
      boolean applyEverywhere = false;
      int correctionId = -1;

      // File system
      String outDirPath = null;

      for (Entry<String, String> argMapEntry : argMap.entrySet()) {
        String argName = argMapEntry.getKey();
        String argValue = argMapEntry.getValue();

        if (argName.equals("forceUpdate")) {
          forceUpdate = argValue.equals("true");
        } else if (argName.equals("docName")) {
          docName = argValue;
        } else if (argName.equals("docIndex")) {
          docIndex = Integer.parseInt(argValue);
        } else if (argName.equals("docId")) {
          docId = Integer.parseInt(argValue);
        } else if (argName.equalsIgnoreCase("query")) {
          queryString = argValue;
        } else if (argName.equalsIgnoreCase("authors")) {
          if (argValue.length() > 0) {
            String[] authorArray = argValue.split("\\|");
            for (String author : authorArray)
              if (author.length() > 0)
                authors.add(author);
          }
        } else if (argName.equalsIgnoreCase("authorInclude")) {
          authorInclude = argValue.equals("true");
        } else if (argName.equalsIgnoreCase("title")) {
          titleQueryString = argValue;
        } else if (argName.equalsIgnoreCase("decimalPlaces")) {
          decimalPlaces = Integer.parseInt(argValue);
        } else if (argName.equals("expand")) {
          expandInflections = argValue.equals("true");
        } else if (argName.equals("lexiconDir")) {
          lexiconDirPath = argValue;
        } else if (argName.equals("lexiconRegex")) {
          lexiconRegexPath = argValue;
        } else if (argName.equals("word")) {
          word = argValue;
        } else if (argName.equals("snippetCount")) {
          snippetCount = Integer.parseInt(argValue);
        } else if (argName.equals("minWeight")) {
          minWeight = Double.parseDouble(argValue);
        } else if (argName.equals("startOffset")) {
          startOffset = Integer.parseInt(argValue);
        } else if (argName.equals("suggestion")) {
          suggestion = argValue;
        } else if (argName.equals("suggestion2")) {
          suggestion2 = argValue;
        } else if (argName.equals("languageCode")) {
          languageCode = argValue;
        } else if (argName.equals("fontCode")) {
          fontCode = argValue;
        } else if (argName.equals("ip")) {
          ip = argValue;
        } else if (argName.equals("includeText")) {
          includeText = argValue.equalsIgnoreCase("true");
        } else if (argName.equals("includeGraphics")) {
          includeGraphics = argValue.equalsIgnoreCase("true");
        } else if (argName.equals("snippet")) {
          snippetJson = argValue;
        } else if (argName.equalsIgnoreCase("docIds")) {
          if (argValue.length() > 0) {
            String[] idArray = argValue.split(",");
            docIds = new HashSet<>();
            for (String id : idArray)
              docIds.add(Integer.parseInt(id));
          }
        } else if (argName.equals("startOffset")) {
          startOffset = Integer.parseInt(argValue);
        } else if (argName.equals("user")) {
          user = argValue;
        } else if (argName.equals("field")) {
          field = JochreIndexField.valueOf(argValue);
        } else if (argName.equals("prefix")) {
          prefix = argValue;
        } else if (argName.equals("maxResults")) {
          maxResults = Integer.parseInt(argValue);
        } else if (argName.equals("sortBy")) {
          sortBy = SortBy.valueOf(argValue);
        } else if (argName.equals("sortAscending")) {
          sortAscending = argValue.equals("true");
        } else if (argName.equals("fromYear")) {
          fromYear = Integer.parseInt(argValue);
        } else if (argName.equals("toYear")) {
          toYear = Integer.parseInt(argValue);
        } else if (argName.equals("reference")) {
          reference = argValue;
        } else if (argName.equals("page")) {
          pageNumber = Integer.parseInt(argValue);
        } else if (argName.equals("resultsPerPage")) {
          resultsPerPage = Integer.parseInt(argValue);
        } else if (argName.equals("applyEverywhere")) {
          applyEverywhere = argValue.equals("true");
        } else if (argName.equals("correctionId")) {
          correctionId = Integer.parseInt(argValue);
        } else if (argName.equals("outDir")) {
          outDirPath = argValue;
        } else {
          throw new RuntimeException("Unknown option: " + argName);
        }
      }

      JochreSearchManager searchManager = JochreSearchManager.getInstance(configId);
      DecimalFormat df = new DecimalFormat("0." + StringUtils.repeat('0', decimalPlaces), enSymbols);

      PrintWriter out = null;
      if (output.isLeft())
        out = output.getLeft();

      switch (command) {
      case index: {
        JochreIndexBuilder builder = new JochreIndexBuilder(configId, forceUpdate);
        new Thread(builder).start();
        out.write("{\"response\":\"index thread started\"}\n");
        break;
      }
      case indexStatus: {
        SearchStatusHolder searchStatusHolder = SearchStatusHolder.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jsonGen = jsonFactory.createGenerator(out);

        jsonGen.writeStartObject();
        jsonGen.writeStringField("status", searchStatusHolder.getStatus().name());
        jsonGen.writeStringField("message", searchStatusHolder.getMessage());
        jsonGen.writeNumberField("total", searchStatusHolder.getTotalCount());
        jsonGen.writeNumberField("processed", searchStatusHolder.getProcessedCount());
        jsonGen.writeNumberField("success", searchStatusHolder.getSuccessCount());
        jsonGen.writeNumberField("failure", searchStatusHolder.getFailureCount());
        Date updateDate = new Date(searchStatusHolder.getLastUpdated());
        jsonGen.writeStringField("lastUpdated", dateFormat.format(updateDate));
        jsonGen.writeNumberField("totalTime", searchStatusHolder.getTotalTime());

        jsonGen.writeEndObject();
        jsonGen.flush();
        break;
      }
      case refresh: {
        JochreSearchManager manager = JochreSearchManager.getInstance(configId);
        manager.getManager().maybeRefresh();
        out.write("{\"response\":\"index reader refreshed\"}\n");
        break;
      }
      case search:
      case highlight:
      case snippets: {
        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {
          JochreQuery query = new JochreQuery(configId, queryString, authors, authorInclude, titleQueryString, fromYear,
              toYear, expandInflections, sortBy, sortAscending, reference);

          try {
            JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, configId);

            switch (command) {
            case search: {
              Pair<TopDocs, Integer> results = searcher.search(query, pageNumber, resultsPerPage);
              JsonFactory jsonFactory = new JsonFactory();
              JsonGenerator jsonGen = jsonFactory.createGenerator(out);

              jsonGen.writeStartObject();
              jsonGen.writeNumberField("totalHits", results.getRight());
              jsonGen.writeNumberField("maxResults", config.getMaxResults());
              jsonGen.writeBooleanField("highlights", query.hasHighlights());
              jsonGen.writeArrayFieldStart("results");

              TopDocs topDocs = results.getLeft();
              for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                jsonGen.writeStartObject();
                JochreIndexDocument doc = new JochreIndexDocument(indexSearcher, scoreDoc.doc, configId);
                jsonGen.writeFieldName("doc");
                doc.toJson(jsonGen);

                if (Float.isNaN(scoreDoc.score)) {
                  jsonGen.writeNumberField("score", 0.0);
                } else {
                  double roundedScore = df.parse(df.format(scoreDoc.score)).doubleValue();
                  jsonGen.writeNumberField("score", roundedScore);
                }

                jsonGen.writeEndObject();
              }

              jsonGen.writeEndArray();
              jsonGen.writeEndObject();
              jsonGen.flush();

              if (config.hasDatabase()) {
                FeedbackQuery feedbackQuery = new FeedbackQuery(user, ip, configId);
                feedbackQuery.setResultCount(results.getRight().intValue());
                if (query.getQueryString() != null && query.getQueryString().length() > 0)
                  feedbackQuery.addClause(FeedbackCriterion.text, query.getQueryString());
                if (query.getAuthors().size() > 0) {
                  feedbackQuery.addClause(FeedbackCriterion.author, String.join("|", query.getAuthors()));
                  feedbackQuery.addClause(FeedbackCriterion.includeAuthors, "" + query.isAuthorInclude());
                }
                if (query.getTitleQueryString() != null && query.getTitleQueryString().length() > 0)
                  feedbackQuery.addClause(FeedbackCriterion.title, query.getTitleQueryString());
                if (!query.isExpandInflections())
                  feedbackQuery.addClause(FeedbackCriterion.strict, "true");
                if (query.getFromYear() != null)
                  feedbackQuery.addClause(FeedbackCriterion.fromYear, query.getFromYear().toString());
                if (query.getToYear() != null)
                  feedbackQuery.addClause(FeedbackCriterion.toYear, query.getToYear().toString());
                if (query.getSortBy() != SortBy.Score) {
                  feedbackQuery.addClause(FeedbackCriterion.sortBy, query.getSortBy().name());
                  feedbackQuery.addClause(FeedbackCriterion.sortAscending, "" + query.isSortAscending());
                }
                if (query.getReference() != null)
                  feedbackQuery.addClause(FeedbackCriterion.reference, query.getReference());

                feedbackQuery.save();
              }
              break;
            }
            default: {
              if (!query.hasHighlights())
                throw new RuntimeException(
                    "For command " + command + " a query is required - no highlights available.");

              if (docIds == null) {
                Pair<TopDocs, Integer> result = searcher.search(query, pageNumber, resultsPerPage);

                docIds = new LinkedHashSet<>();
                for (ScoreDoc scoreDoc : result.getLeft().scoreDocs) {
                  docIds.add(scoreDoc.doc);
                  LOG.debug("### Next document");
                  Document doc = indexSearcher.doc(scoreDoc.doc);
                  for (IndexableField oneField : doc.getFields()) {
                    if (!oneField.name().equals(JochreIndexField.text.name())
                        && !oneField.name().startsWith(JochreIndexField.rect.name())
                        && !oneField.name().startsWith(JochreIndexField.start.name()))
                      LOG.debug(oneField.toString());
                  }
                }
              }
              Set<String> searchFields = new HashSet<>();
              searchFields.add(JochreIndexField.text.name());

              Highlighter highlighter = new LuceneQueryHighlighter(query, indexSearcher, searchFields);
              HighlightManager highlightManager = new HighlightManager(indexSearcher, searchFields, configId);
              highlightManager.setDecimalPlaces(decimalPlaces);
              highlightManager.setMinWeight(minWeight);
              highlightManager.setIncludeText(includeText);
              highlightManager.setIncludeGraphics(includeGraphics);
              if (snippetCount > 0)
                highlightManager.setSnippetCount(snippetCount);

              if (command == Command.highlight) {
                highlightManager.highlight(highlighter, docIds, out);
              } else {
                highlightManager.findSnippets(highlighter, docIds, out);
              }
              break;
            }
            }
          } catch (JochreQueryParseException e) {
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator jsonGen = jsonFactory.createGenerator(out);

            jsonGen.writeStartObject();
            jsonGen.writeStringField("parseException", "true");
            jsonGen.writeStringField("message", e.getMessage());

            jsonGen.writeEndObject();
            jsonGen.flush();
          }
          out.write("\n");
          out.flush();
        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        break;
      }
      case textSnippet: {
        if (snippetJson == null)
          throw new JochreException("Command " + command + " requires a snippet");
        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {
          Snippet snippet = new Snippet(snippetJson);

          if (LOG.isDebugEnabled()) {
            Document doc = indexSearcher.doc(snippet.getDocId());
            LOG.debug("Snippet in: " + doc.get(JochreIndexField.path.name()));
          }

          Set<String> searchFields = new HashSet<>();
          searchFields.add(JochreIndexField.text.name());
          HighlightManager highlightManager = new HighlightManager(indexSearcher, searchFields, configId);
          String text = highlightManager.displaySnippet(snippet);

          out.write(text);
        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        break;
      }
      case imageSnippet: {
        if (snippetJson == null)
          throw new JochreException("Command " + command + " requires a snippet");
        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {
          Snippet snippet = new Snippet(snippetJson);

          if (LOG.isDebugEnabled()) {
            Document doc = indexSearcher.doc(snippet.getDocId());
            LOG.debug("Snippet in: " + doc.get(JochreIndexField.path.name()));
          }
          Set<String> searchFields = new HashSet<>();
          searchFields.add(JochreIndexField.text.name());
          HighlightManager highlightManager = new HighlightManager(indexSearcher, searchFields, configId);
          ImageSnippet imageSnippet = highlightManager.getImageSnippet(snippet);
          ImageOutputStream ios = ImageIO.createImageOutputStream(output.getRight());
          BufferedImage image = imageSnippet.getImage();
          ImageReader imageReader = ImageIO.getImageReadersByMIMEType("image/png").next();
          ImageWriter imageWriter = ImageIO.getImageWriter(imageReader);
          imageWriter.setOutput(ios);
          imageWriter.write(image);
          ios.flush();
        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        break;
      }
      case word: {
        if (startOffset < 0)
          throw new JochreException("Command " + command + " requires a startOffset");
        if (docId < 0 && (docName == null || docIndex < 0))
          throw new RuntimeException(
              "For command " + command + " either a docName and docIndex, or a docId is required");

        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {
          JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, configId);
          if (docId < 0) {
            Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
            docId = docs.keySet().iterator().next();
          }
          JochreIndexDocument jochreDoc = new JochreIndexDocument(indexSearcher, docId, configId);
          JochreIndexWord jochreWord = jochreDoc.getWord(startOffset);
          String word1 = jochreWord.getText();
          String word2 = null;
          if (word1.contains(JochreSearchConstants.INDEX_NEWLINE)) {
            word2 = word1.substring(word1.indexOf(JochreSearchConstants.INDEX_NEWLINE) + 1);
            word1 = word1.substring(0, word1.indexOf(JochreSearchConstants.INDEX_NEWLINE));
          }
          JsonFactory jsonFactory = new JsonFactory();
          JsonGenerator jsonGen = jsonFactory.createGenerator(out);
          jsonGen.writeStartObject();
          jsonGen.writeStringField("word", word1);
          if (word2 != null)
            jsonGen.writeStringField("word2", word2);
          jsonGen.writeEndObject();
          jsonGen.flush();
        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        break;
      }
      case view: {
        if (docId < 0 && docIndex < 0)
          throw new RuntimeException("For command " + command + " either docName and docIndex, or docId are required");
        if (docId < 0) {
          if (docName == null)
            throw new RuntimeException("For command " + command + " docName is required");
          if (docIndex < 0)
            throw new RuntimeException("For command " + command + " docIndex is required");
        }

        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {
          JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, configId);
          if (docId < 0) {
            Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
            docId = docs.keySet().iterator().next();
          }

          Document doc = indexSearcher.doc(docId);

          JsonFactory jsonFactory = new JsonFactory();
          JsonGenerator jsonGen = jsonFactory.createGenerator(out);

          jsonGen.writeStartObject();
          for (IndexableField oneField : doc.getFields()) {
            if (!oneField.name().equals(JochreIndexField.text.name()))
              jsonGen.writeStringField(oneField.name(), oneField.stringValue());
          }
          jsonGen.writeEndObject();

          jsonGen.flush();
          out.write("\n");
          out.flush();
        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        break;
      }
      case list: {
        if (docId < 0 && docIndex < 0)
          throw new RuntimeException("For command " + command + " either docName and docIndex, or docId are required");
        if (docId < 0) {
          if (docName == null)
            throw new RuntimeException("For command " + command + " docName is required");
          if (docIndex < 0)
            throw new RuntimeException("For command " + command + " docIndex is required");
        }

        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {
          JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, configId);
          if (docId < 0) {
            Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
            docId = docs.keySet().iterator().next();
          }
          JochreIndexTermLister lister = new JochreIndexTermLister(docId, indexSearcher);
          lister.list(out);
          out.write("\n");
          out.flush();
        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        break;
      }
      case wordImage: {
        if (docId < 0 && docIndex < 0)
          throw new RuntimeException("For command " + command + " either docName and docIndex, or docId are required");
        if (docId < 0) {
          if (docName == null)
            throw new RuntimeException("For command " + command + " docName is required");
          if (docIndex < 0)
            throw new RuntimeException("For command " + command + " docIndex is required");
        }
        if (startOffset < 0)
          throw new RuntimeException("For command " + command + " startOffset is required");

        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {
          JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, configId);
          if (docId < 0) {
            Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
            docId = docs.keySet().iterator().next();
          }
          JochreIndexDocument jochreDoc = new JochreIndexDocument(indexSearcher, docId, configId);
          JochreIndexWord jochreWord = jochreDoc.getWord(startOffset);
          LOG.debug("jochreDoc: " + jochreDoc.getPath());
          LOG.debug("word: " + jochreWord.getText());
          LOG.debug("startOffset: " + jochreWord.getStartOffset());
          BufferedImage wordImage = jochreWord.getImage();

          ImageOutputStream ios = ImageIO.createImageOutputStream(output.getRight());
          ImageReader imageReader = ImageIO.getImageReadersByMIMEType("image/png").next();
          ImageWriter imageWriter = ImageIO.getImageWriter(imageReader);
          imageWriter.setOutput(ios);
          imageWriter.write(wordImage);
          ios.flush();
        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        break;
      }
      case suggest: {
        if (!config.hasDatabase())
          throw new RuntimeException("For command " + command + " a database is required");
        if (docId < 0 && docIndex < 0)
          throw new RuntimeException("For command " + command + " either docName and docIndex, or docId are required");
        if (docId < 0) {
          if (docName == null)
            throw new RuntimeException("For command " + command + " docName is required");
          if (docIndex < 0)
            throw new RuntimeException("For command " + command + " docIndex is required");
        }
        if (startOffset < 0)
          throw new RuntimeException("For command " + command + " startOffset is required");
        if (suggestion == null)
          throw new RuntimeException("For command " + command + " suggestion is required");
        if (user == null)
          throw new RuntimeException("For command " + command + " user is required");
        if (fontCode == null)
          throw new RuntimeException("For command " + command + " fontCode is required");
        if (languageCode == null)
          throw new RuntimeException("For command " + command + " languageCode is required");

        String fullSuggestion = suggestion;
        if (suggestion2 != null && suggestion2.length() > 0)
          fullSuggestion += JochreSearchConstants.INDEX_NEWLINE + suggestion2;

        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {
          JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, configId);
          Document luceneDoc = null;
          if (docId < 0) {
            Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
            luceneDoc = docs.values().iterator().next();
            docId = docs.keySet().iterator().next();
          } else {
            luceneDoc = searcher.loadDocument(docId);
          }

          FeedbackSuggestion sug = new FeedbackSuggestion(indexSearcher, docId, startOffset, fullSuggestion, user, ip,
              fontCode, languageCode, configId);
          sug.save();

          // Mark the document for re-indexing
          String path = luceneDoc.get(JochreIndexField.path.name());
          JochreIndexDirectory jochreIndexDirectory = new JochreIndexDirectory(path, configId);
          jochreIndexDirectory.addUpdateInstructions();

          // Start the index thread
          JochreIndexBuilder builder = new JochreIndexBuilder(configId, false);
          new Thread(builder).start();
        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        out.write("{\"response\":\"suggestion saved\"}\n");
        break;
      }
      case correct: {
        if (!config.hasDatabase())
          throw new RuntimeException("For command " + command + " a database is required");

        if (docId < 0 && docName == null)
          throw new RuntimeException("For command " + command + " either docName or docId are required");
        if (field == null)
          throw new RuntimeException("For command " + command + " field is required");
        if (suggestion == null)
          throw new RuntimeException("For command " + command + " suggestion is required");
        if (user == null)
          throw new RuntimeException("For command " + command + " user is required");

        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {

          JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, configId);

          Document luceneDoc = null;
          if (docId < 0) {
            Map<Integer, Document> docs = searcher.findDocuments(docName);
            luceneDoc = docs.values().iterator().next();
          } else {
            luceneDoc = searcher.loadDocument(docId);
          }
          String previousValue = luceneDoc.get(field.name());
          FeedbackDocument feedbackDoc = FeedbackDocument
              .findOrCreateDocument(luceneDoc.get(JochreIndexField.path.name()), configId);

          Correction correction = new Correction(feedbackDoc, field, user, ip, suggestion, previousValue,
              applyEverywhere, configId);
          correction.save();
          // Now apply the correction to the index itself
          List<String> docNames = new ArrayList<>();
          if (applyEverywhere) {
            // find all documents affected by this update and mark them for re-indexing
            TopDocs topDocs = searcher.search(correction.getField(), correction.getPreviousValue());
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
              Document doc = indexSearcher.doc(scoreDoc.doc);
              String path = doc.get(JochreIndexField.path.name());
              JochreIndexDirectory jochreIndexDirectory = new JochreIndexDirectory(path, configId);
              jochreIndexDirectory.addUpdateInstructions();
              docNames.add(doc.get(JochreIndexField.name.name()));
            }
          } else {
            // mark the document for re-indexing
            String path = luceneDoc.get(JochreIndexField.path.name());
            JochreIndexDirectory jochreIndexDirectory = new JochreIndexDirectory(path, configId);
            jochreIndexDirectory.addUpdateInstructions();
            docNames.add(luceneDoc.get(JochreIndexField.name.name()));
          }

          // update the list of documents affected by this correction
          correction.setDocuments(docNames);
          correction.save();

          // start the index thread
          JochreIndexBuilder builder = new JochreIndexBuilder(configId, false);
          new Thread(builder).start();

          // wrap e-mail in runnable to return directly to client
          new Thread(new Runnable() {
            @Override
            public void run() {
              try {
                LOG.debug("Sending e-mail for correction");
                // send an e-mail if required
                correction.sendEmail();
              } catch (MessagingException | IOException | TemplateException e) {
                LOG.error("Unable to send correction e-mail", e);
              }
            }
          }).start();

        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        out.write("{\"response\":\"correction saved\"}\n");
        break;
      }
      case sendCorrectionEmail: {
        if (correctionId < 0)
          throw new RuntimeException("For command " + command + " correctionId is required");

        Correction correction = Correction.loadCorrection(correctionId, configId);
        correction.sendEmail();
        out.write("{\"response\":\"correction e-mail sent\"}\n");
        break;
      }
      case undo: {
        if (correctionId < 0)
          throw new RuntimeException("For command " + command + " correctionId is required");

        // Mark correction for ignoring
        Correction correction = Correction.loadCorrection(correctionId, configId);
        correction.setIgnore(true);
        correction.save();

        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {

          JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, configId);

          List<String> docNames = correction.getDocuments();
          for (String doc : docNames) {
            // mark the document for re-indexing
            Map<Integer, Document> docs = searcher.findDocuments(doc);
            Document luceneDoc = docs.values().iterator().next();
            String path = luceneDoc.get(JochreIndexField.path.name());
            JochreIndexDirectory jochreIndexDirectory = new JochreIndexDirectory(path, configId);
            jochreIndexDirectory.addUpdateInstructions();
          }

          // Mark initial document for update
          String docPath = correction.getDocument().getPath();
          JochreIndexDirectory jochreIndexDirectory = new JochreIndexDirectory(docPath, configId);
          jochreIndexDirectory.addUpdateInstructions();

          // start the index thread
          JochreIndexBuilder builder = new JochreIndexBuilder(configId, forceUpdate);
          new Thread(builder).start();

          out.write("{\"response\":\"correction undo thread started\"}\n");
        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        break;
      }
      case prefixSearch: {
        if (field == null)
          throw new RuntimeException("For command " + command + " field is required");
        if (prefix == null)
          throw new RuntimeException("For command " + command + " prefix is required");

        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {
          FieldTermPrefixFinder finder = new FieldTermPrefixFinder(indexSearcher, field, prefix, maxResults, configId);
          JsonFactory jsonFactory = new JsonFactory();
          JsonGenerator jsonGen = jsonFactory.createGenerator(out);

          jsonGen.writeStartArray();
          for (String result : finder.getResults()) {
            jsonGen.writeString(result);
          }
          jsonGen.writeEndArray();

          jsonGen.flush();
          out.write("\n");
          out.flush();
        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        break;
      }
      case document: {
        if (docId < 0 && docName == null)
          throw new RuntimeException("For command " + command + " either docName  or docId are required");

        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {
          JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, configId);
          List<JochreIndexDocument> docs = new ArrayList<>();
          if (docName != null) {
            Map<Integer, Document> docMap = searcher.findDocuments(docName);
            for (int id : docMap.keySet())
              docs.add(new JochreIndexDocument(indexSearcher, id, configId));
            Collections.sort(docs, new Comparator<JochreIndexDocument>() {
              @Override
              public int compare(JochreIndexDocument d1, JochreIndexDocument d2) {
                return d1.getSectionNumber() - d2.getSectionNumber();
              }
            });
          } else {
            JochreIndexDocument doc = new JochreIndexDocument(indexSearcher, docId, configId);
            docs.add(doc);
          }

          JsonFactory jsonFactory = new JsonFactory();
          JsonGenerator jsonGen = jsonFactory.createGenerator(out);

          jsonGen.writeStartArray();
          for (JochreIndexDocument doc : docs) {
            doc.toJson(jsonGen);
          }
          jsonGen.writeEndArray();

          jsonGen.flush();
          out.write("\n");
          out.flush();
        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        break;

      }
      case contents: {
        if (docName == null)
          throw new RuntimeException("For command " + command + " docName is required");
        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {
          JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, configId);
          Map<Integer, Document> docMap = searcher.findDocuments(docName);
          List<JochreIndexDocument> docs = new ArrayList<>();
          for (int id : docMap.keySet())
            docs.add(new JochreIndexDocument(indexSearcher, id, configId));
          Collections.sort(docs, new Comparator<JochreIndexDocument>() {
            @Override
            public int compare(JochreIndexDocument d1, JochreIndexDocument d2) {
              return d1.getSectionNumber() - d2.getSectionNumber();
            }
          });
          for (JochreIndexDocument doc : docs) {
            DocumentContentHTMLWriter htmlWriter = new DocumentContentHTMLWriter(out, doc, config);
            htmlWriter.writeContents();
          }

        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        break;
      }
      case bookCount: {
        IndexSearcher indexSearcher = searchManager.getManager().acquire();
        try {
          int bookCount = ((JochreSearcher) indexSearcher).getBookCount();
          JsonFactory jsonFactory = new JsonFactory();
          JsonGenerator jsonGen = jsonFactory.createGenerator(out);

          jsonGen.writeStartObject();
          jsonGen.writeNumberField("bookCount", bookCount);
          jsonGen.writeEndObject();

          jsonGen.flush();
          out.write("\n");
          out.flush();

        } finally {
          searchManager.getManager().release(indexSearcher);
        }
        break;
      }
        case exportSuggestions: {
          if (outDirPath == null)
            throw new RuntimeException("For command " + command + " outDir is required");

          File outDir = new File(outDirPath);
          outDir.mkdirs();
          File outFile = new File(outDir, "suggestions.txt");
          File imageDir = new File(outDir, "images");
          imageDir.mkdirs();

          int i = 0;
          try (Writer writer = new PrintWriter(new FileWriter(outFile, StandardCharsets.UTF_8))) {
            FeedbackService feedbackService = new FeedbackService(configId);
            int start = 0;
            int interval = 100;
            while (true) {
              List<FeedbackSuggestion> suggestions = feedbackService.findSuggestions(start, start + interval);
              if (suggestions.size() == 0) {
                break;
              }
              for (FeedbackSuggestion sug : suggestions) {
                BufferedImage image = sug.getWord().getImage();
                String imageId = String.format("%09d", sug.getId());
                String imageFileName = "suggestion_" + imageId + ".png";
                File imageFile = new File(imageDir, imageFileName);

                BufferedImage result = new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
                result.createGraphics().drawImage(image, 0, 0, Color.WHITE, null);

                ImageIO.write(result, "png", imageFile);
                writer.write(imageFileName + "\t" + sug.getText() + "\n");
                writer.flush();
                i++;
              }
              start += interval;
              if (maxResults > 0 && i > maxResults) {
                break;
              }
            }
          }
          break;
        }
      case serializeLexicon: {
        if (lexiconDirPath == null)
          throw new RuntimeException("For command " + command + " lexiconDir is required");
        if (lexiconRegexPath == null)
          throw new RuntimeException("For command " + command + " lexiconRegex is required");

        File lexiconDir = new File(lexiconDirPath);
        File[] lexiconFiles = lexiconDir.listFiles();
        TextFileLexicon lexicon = new TextFileLexicon(configId);

        File regexFile = new File(lexiconRegexPath);
        Scanner regexScanner = new Scanner(
            new BufferedReader(new InputStreamReader(new FileInputStream(regexFile), "UTF-8")));
        LexicalEntryReader lexicalEntryReader = new RegexLexicalEntryReader(regexScanner);

        for (File file : lexiconFiles) {
          LOG.info("Adding " + file.getName());
          lexicon.addLexiconFile(file, lexicalEntryReader);
        }

        File outFile = config.getLexiconFile();
        lexicon.serialize(outFile);
        break;
      }
      case deserializeLexicon: {
        if (word == null)
          throw new RuntimeException("For command " + command + " word is required");

        Lexicon lexicon = config.getLexicon();
        Set<String> lemmas = lexicon.getLemmas(word);
        LOG.info("Word: " + word);
        if (lemmas != null) {
          for (String lemma : lemmas) {
            Set<String> words = lexicon.getWords(lemma);
            LOG.info("# Lemma: " + lemma + ", words: " + words.toString());
          }
        }
        break;
      }
      default: {
        throw new RuntimeException("Unknown command: " + command);
      }
      }
      if (output.isLeft())
        output.getLeft().flush();
      else
        output.getRight().flush();
    } catch (RuntimeException e) {
      LOG.error("Failed to run command " + command, e);
      throw e;
    } catch (Exception e) {
      LOG.error("Failed to run command " + command, e);
      throw new RuntimeException(e);
    } finally {
      long endTime = System.currentTimeMillis();
      LOG.info("Command " + command + " completed in " + (endTime - startTime) + " ms");
    }
  }

  /**
   * Command-line entry point.
   */
  public static void main(String[] args) throws Exception {

    Map<String, String> argMap = new HashMap<>();

    for (String arg : args) {
      int equalsPos = arg.indexOf('=');
      String argName = arg.substring(0, equalsPos);
      String argValue = arg.substring(equalsPos + 1);
      argMap.put(argName, argValue);
    }

    String logConfigPath = argMap.get("logConfigFile");
    if (logConfigPath != null) {
      LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

      File slf4jFile = new File(logConfigPath);

      if (slf4jFile.exists()) {
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        // Call context.reset() to clear any previous configuration,
        // e.g. default configuration
        loggerContext.reset();
        configurator.doConfigure(slf4jFile);
      } else {
        throw new Exception("LogConfigFile not found: " + logConfigPath);
      }
      argMap.remove("logConfigFile");
    }

    long startTime = System.currentTimeMillis();
    try {
      String configId = argMap.get("configId");
      argMap.remove("configId");

      Command command = Command.valueOf(argMap.get("command"));
      Either<PrintWriter, OutputStream> output;
      if (command.getContentType().startsWith("image")) {
        String outFilePath = argMap.get("outFile");
        argMap.remove("outFile");
        File outfile = new File(outFilePath);
        FileOutputStream fos = new FileOutputStream(outfile);
        output = Either.ofRight(fos);
      } else {
        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        output = Either.ofLeft(out);
      }

      JochreSearch main = new JochreSearch(configId);
      main.execute(argMap, output);

      if (output.isRight())
        output.getRight().close();
    } finally {
      long endTime = System.currentTimeMillis();
      LOG.info("Total time: " + (endTime - startTime) + " ms");
    }
  }

}
