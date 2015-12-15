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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.joliciel.jochre.search.SearchStatusHolder.SearchStatus;
import com.joliciel.jochre.search.alto.AltoDocument;
import com.joliciel.jochre.search.alto.AltoPage;
import com.joliciel.jochre.search.alto.AltoPageConsumer;
import com.joliciel.jochre.search.alto.AltoReader;
import com.joliciel.jochre.search.alto.AltoService;
import com.joliciel.jochre.search.alto.AltoString;
import com.joliciel.jochre.search.alto.AltoTextBlock;
import com.joliciel.jochre.search.alto.AltoTextLine;
import com.joliciel.talismane.utils.LogUtils;

class JochreIndexBuilderImpl implements JochreIndexBuilder, TokenExtractor {
	private static final Log LOG = LogFactory.getLog(JochreIndexBuilderImpl.class);
	private File indexDir;
	private File contentDir;
	private boolean forceUpdate = false;
	
	private int wordsPerDoc=3000;
	private IndexWriter indexWriter;
	private IndexReader indexReader;
	private IndexSearcher indexSearcher;

	private List<JochreToken> currentStrings = null;		
	
	private SearchServiceInternal searchService;
	private AltoService altoService;
	
	public JochreIndexBuilderImpl(File indexDir, File contentDir) {
		super();
		this.indexDir = indexDir;
		this.contentDir = contentDir;
	}

	private void initialise() {
		if (this.indexWriter==null) {
			try {
				Path path = this.indexDir.toPath();
				Directory directory = FSDirectory.open(path);
				
				Analyzer analyzer = searchService.getJochreAnalyser(this);
				IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
				this.indexWriter = new IndexWriter(directory, iwc);

				try {
					indexReader = DirectoryReader.open(directory);
					
					indexSearcher = new IndexSearcher(indexReader);
				} catch (IndexNotFoundException e) {
					LOG.debug("No index at : " + indexDir.getAbsolutePath());
				}
			} catch (IOException ioe) {
				LogUtils.logError(LOG, ioe);
				throw new RuntimeException(ioe);
			}
		}
	}
	

	@Override
	public void run() {
		SearchStatusHolder searchStatusHolder = searchService.getSearchStatusHolder();
		
		if (searchStatusHolder.getStatus()!=SearchStatus.WAITING) {
			throw new JochreSearchException("Search index construction already underway. Try again later.");
		}
		
		this.updateIndex(contentDir, forceUpdate);
	}

	public void updateIndex(File contentDir, boolean forceUpdate) {
		long startTime = System.currentTimeMillis();
		SearchStatusHolder searchStatusHolder = searchService.getSearchStatusHolder();
		try {
			searchStatusHolder.setStatus(SearchStatus.PREPARING);
			
			this.initialise();
			File[] subdirs = contentDir.listFiles(new FileFilter() {
				
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});
			
			searchStatusHolder.setStatus(SearchStatus.BUSY);
			searchStatusHolder.setTotalCount(subdirs.length);

			for (File subdir : subdirs) {
				searchStatusHolder.setAction("Indexing " + subdir.getName());
				this.processDocument(subdir, forceUpdate);
				searchStatusHolder.incrementSuccessCount(1);
			}
			
			searchStatusHolder.setStatus(SearchStatus.COMMITING);
			indexWriter.commit();
			indexWriter.close();
			searchService.purgeSearcher();

		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} finally {
			searchStatusHolder.setStatus(SearchStatus.WAITING);
			long endTime = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			LOG.info("Total time (ms): " + totalTime);
		}
	}

	@Override
	public void updateDocument(File documentDir, int startPage, int endPage) {
		long startTime = System.currentTimeMillis();
		try {
			this.initialise();
			JochreIndexDirectory directory = searchService.getJochreIndexDirectory(documentDir);
			this.updateDocumentInternal(directory, startPage, endPage);
			indexWriter.commit();
			indexWriter.close();
			searchService.purgeSearcher();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} finally {
			long endTime = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			LOG.info("Total time (ms): " + totalTime);
		}
	}

	public void deleteDocument(File documentDir) {
		try {
			this.initialise();
			this.deleteDocumentInternal(documentDir);
			indexWriter.commit();
			indexWriter.close();
			searchService.purgeSearcher();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	private void processDocument(File documentDir, boolean forceUpdate) {
		try {
			
			boolean updateIndex = false;
			
			JochreIndexDirectory jochreIndexDirectory = this.searchService.getJochreIndexDirectory(documentDir);
			switch (jochreIndexDirectory.getInstructions()) {
			case Delete:
				this.deleteDocumentInternal(documentDir);
				return;
			case Skip:
				return;
			case Update:
				updateIndex = true;
			case None:
				// do nothing
				break;
			}
	
			if (forceUpdate)
				updateIndex = true;
			
			if (!updateIndex) {
				long ocrDate = jochreIndexDirectory.getAltoFile().lastModified();
				long lastIndexDate = Long.MIN_VALUE;
				
				if (indexSearcher!=null) {
					Term term = new Term("name", jochreIndexDirectory.getName());
					Query termQuery = new TermQuery(term);
					TopDocs topDocs = indexSearcher.search(termQuery, 1);
					if (topDocs.scoreDocs.length>0) {
						Document doc = indexSearcher.doc(topDocs.scoreDocs[0].doc);
						lastIndexDate = Long.parseLong(doc.get("indexTime"));
					}
				}
				
				LOG.debug("lastIndexDate: " + lastIndexDate + ", ocrDate: " + ocrDate);
				if (ocrDate>lastIndexDate)
					updateIndex = true;
			}
			
			if (updateIndex) {
				this.updateDocumentInternal(jochreIndexDirectory, -1, -1);
			} else {
				LOG.info("Index for " + documentDir.getName() + " already up-to-date.");
			} // should update index?
			
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	private void updateDocumentInternal(JochreIndexDirectory jochreIndexDirectory, int startPage, int endPage) {
		try {
			LOG.info("Updating index for " + jochreIndexDirectory.getName());
			
			this.deleteDocumentInternal(jochreIndexDirectory.getDirectory());
			
			AltoDocument altoDoc = this.altoService.newDocument(jochreIndexDirectory.getName());
			AltoReader reader = this.altoService.getAltoReader(altoDoc);
			AltoPageIndexer altoPageIndexer = new AltoPageIndexer(this, jochreIndexDirectory, startPage, endPage);
			reader.addConsumer(altoPageIndexer);
			
			UnclosableInputStream uis = jochreIndexDirectory.getAltoInputStream();
			reader.parseFile(uis, jochreIndexDirectory.getName());
			uis.reallyClose();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	private static final class AltoPageIndexer implements AltoPageConsumer {
		public static final Pattern PUNCTUATION = Pattern.compile("\\p{Punct}", Pattern.UNICODE_CHARACTER_CLASS);
		private JochreIndexBuilderImpl parent;
		private int docCount = 0;
		private int cumulWordCount = 0;
		private List<AltoPage> currentPages = new ArrayList<AltoPage>();
		private List<AltoPage> previousPages = new ArrayList<AltoPage>();
		private List<JochreToken> previousStrings = new ArrayList<JochreToken>();
		private List<JochreToken> currentStrings = new ArrayList<JochreToken>();
		private int startPage = -1;
		private int endPage = -1;
		
		private JochreIndexDirectory directory;
		
		public AltoPageIndexer(JochreIndexBuilderImpl parent, JochreIndexDirectory directory, int startPage, int endPage) {
			super();
			this.parent = parent;
			this.directory = directory;
			this.startPage = startPage;
			this.endPage = endPage;
		}

		@Override
		public void onNextPage(AltoPage page) {
			if (startPage>=0 && page.getPageIndex()<startPage)
				return;
			if (endPage>=0 && page.getPageIndex()>endPage)
				return;
			LOG.debug("Processing page: " + page.getPageIndex());
			currentPages.add(page);
			for (AltoTextBlock textBlock : page.getTextBlocks()) {
				textBlock.joinHyphens();
				for (AltoTextLine textLine : textBlock.getTextLines()) {
					for (AltoString string : textLine.getStrings()) {
						if (string.isWhiteSpace() || PUNCTUATION.matcher(string.getContent()).matches())
							continue;
						
						currentStrings.add(string);
					}
				}
			}
			
			int wordCount = page.wordCount();
			cumulWordCount += wordCount;
			LOG.debug("Word count: " + wordCount + ", cumul word count: " + cumulWordCount);
			if (parent.getWordsPerDoc()>0 && cumulWordCount >= parent.getWordsPerDoc()) {
				if (previousPages.size()>0) {
					parent.setCurrentStrings(previousStrings);
					LOG.debug("Creating new index doc: " + docCount);
					JochreIndexDocument indexDoc = parent.getSearchService().newJochreIndexDocument(directory, docCount, previousPages);
					indexDoc.save(parent.getIndexWriter());
					docCount++;
				}

				previousPages = currentPages;
				previousStrings = currentStrings;
				
				cumulWordCount = 0;
				parent.setCurrentStrings(new ArrayList<JochreToken>());
				currentPages = new ArrayList<AltoPage>();
				currentStrings = new ArrayList<JochreToken>();
			}
		}

		@Override
		public void onComplete() {
			previousPages.addAll(currentPages);
			previousStrings.addAll(currentStrings);
			parent.setCurrentStrings(previousStrings);
			LOG.debug("Creating new index doc: " + docCount);
			JochreIndexDocument indexDoc = parent.getSearchService().newJochreIndexDocument(directory, docCount, previousPages);
			indexDoc.save(parent.getIndexWriter());
			docCount++;
		}
	}

	
	private void deleteDocumentInternal(File documentDir) {
		try {
			Term term = new Term("path", documentDir.getAbsolutePath());
			indexWriter.deleteDocuments(term);
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	public SearchServiceInternal getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchServiceInternal searchService) {
		this.searchService = searchService;
	}

	public int getWordsPerDoc() {
		return wordsPerDoc;
	}

	public void setWordsPerDoc(int wordsPerDoc) {
		this.wordsPerDoc = wordsPerDoc;
	}

	public AltoService getAltoService() {
		return altoService;
	}

	public void setAltoService(AltoService altoService) {
		this.altoService = altoService;
	}

	public IndexWriter getIndexWriter() {
		return indexWriter;
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

	@Override
	public boolean isForceUpdate() {
		return forceUpdate;
	}

	@Override
	public void setForceUpdate(boolean forceUpdate) {
		this.forceUpdate = forceUpdate;
	}
}
