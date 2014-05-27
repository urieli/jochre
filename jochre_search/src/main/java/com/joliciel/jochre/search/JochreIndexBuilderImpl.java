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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import com.joliciel.talismane.utils.LogUtils;


class JochreIndexBuilderImpl implements JochreIndexBuilder, TokenOffsetObserver {
	private static final Log LOG = LogFactory.getLog(JochreIndexBuilderImpl.class);
	private static String[] imageExtensions = new String[] {"png","jpg","jpeg","gif","tiff"};
	private File indexDir;
	private Map<Integer, JochreXmlLetter> offsetLetterMap;
	private CoordinateStorage coordinateStorage;
	private int wordsPerDoc=3000;
	private IndexWriter indexWriter;
	
	private SearchServiceInternal searchService;

	public JochreIndexBuilderImpl(File indexDir) {
		try {
			this.indexDir = indexDir;
			Directory directory = FSDirectory.open(this.indexDir);
			
			JochreAnalyzer analyzer = new JochreAnalyzer(Version.LUCENE_46);
			analyzer.setObserver(this);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_46, analyzer);
			iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			this.indexWriter = new IndexWriter(directory, iwc);
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	public void updateIndex(File contentDir) {
		long startTime = System.currentTimeMillis();
		try {
			File[] subdirs = contentDir.listFiles(new FileFilter() {
				
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});
			
			for (File subdir : subdirs) {
				this.addDocumentDir(subdir, false);
			}
			
			indexWriter.commit();
			indexWriter.close();

		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} finally {
			long endTime = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			LOG.info("Total time (ms): " + totalTime);
		}
	}

	@Override
	public void addDocumentDir(File documentDir) {
		long startTime = System.currentTimeMillis();
		try {
			this.addDocumentDir(documentDir, true);
			indexWriter.commit();
			indexWriter.close();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} finally {
			long endTime = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			LOG.info("Total time (ms): " + totalTime);
		}
	}

	public void addDocumentDir(File documentDir, boolean forceUpdate) {
		try {
			File zipFile = new File(documentDir, documentDir.getName() + ".zip");
			if (!zipFile.exists()) {
				LOG.info("Nothing to index in " + documentDir.getName());
				return;
			} else {
				LOG.debug("Checking " + documentDir.getName());
			}
			long zipDate = zipFile.lastModified();
			
			boolean updateIndex = false;
			File lastIndexDateFile = new File(documentDir, "indexDate.txt");
			
			if (forceUpdate) {
				updateIndex = true;
			} else {
				long lastIndexDate = Long.MIN_VALUE;
				
				if (lastIndexDateFile.exists()) {
					Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(lastIndexDateFile), "UTF-8")));
					while (scanner.hasNextLine()) {
						lastIndexDate = Long.parseLong(scanner.nextLine());
						break;
					}
					scanner.close();
				}
				if (zipDate>lastIndexDate)
					updateIndex = true;
			}
			
			if (!updateIndex) {
				LOG.info("Index for " + documentDir.getName() + "already up-to-date.");
			} else {
				LOG.info("Updating index for " + documentDir.getName());
				Term term = new Term("id", documentDir.getName());
				indexWriter.deleteDocuments(term);
				
				File[] offsetFiles = documentDir.listFiles(new FilenameFilter() {
					
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".obj");
					}
				});
				
				for (File offsetFile : offsetFiles) {
					offsetFile.delete();
				}
				
				int i = 0;
				
				Map<String,String> fields = new TreeMap<String, String>();
				File metaDataFile = new File(documentDir, "metadata.txt");
				Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(metaDataFile), "UTF-8")));
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					String key = line.substring(0, line.indexOf('\t'));
					String value = line.substring(line.indexOf('\t'));
					fields.put(key, value);
				}
				scanner.close();
				
				JochreXmlDocument xmlDoc = this.searchService.newDocument();
				JochreXmlReader reader = this.searchService.getJochreXmlReader(xmlDoc);
				
				ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
				ZipEntry ze = null;
			    while ((ze = zis.getNextEntry()) != null) {
					LOG.debug("Adding zipEntry " + i + ": " + ze.getName());
					String baseName = ze.getName().substring(0, ze.getName().lastIndexOf('.'));
					UnclosableInputStream uis = new UnclosableInputStream(zis);
					reader.parseFile(uis, baseName);
			    	i++;
			    }
			    zis.close();
				
				i = 0;
				StringBuilder sb = new StringBuilder();
				coordinateStorage = searchService.getCoordinateStorage();
				offsetLetterMap = new HashMap<Integer, JochreXmlLetter>();
				int startPage = -1;
				int endPage = -1;
				int docCount = 0;
				int wordCount = 0;
				int cumulWordCount = 0;
				for (JochreXmlImage image : xmlDoc.getImages()) {
					if (startPage<0) startPage = image.getPageIndex();
					endPage = image.getPageIndex();
					int remainingWords = xmlDoc.wordCount() - (cumulWordCount + wordCount);
					LOG.debug("Word count: " + wordCount + ", cumul word count: " + cumulWordCount + ", total xml words: " + xmlDoc.wordCount() + ", remaining words: " + remainingWords);
					if (wordsPerDoc>0 && wordCount >= wordsPerDoc && remainingWords >= wordsPerDoc) {
						LOG.debug("Creating new index doc: " + docCount);
						JochreIndexDocument indexDoc = searchService.newJochreIndexDocument(documentDir, docCount, sb, coordinateStorage, startPage, endPage, fields);
						indexDoc.save(indexWriter);
						docCount++;
						
						sb = new StringBuilder();
						coordinateStorage = searchService.getCoordinateStorage();
						startPage = image.getPageIndex();
						offsetLetterMap = new HashMap<Integer, JochreXmlLetter>();
						cumulWordCount += wordCount;
						wordCount = 0;
					}
					
					LOG.debug("Processing page: " + image.getFileNameBase());
					
					File imageFile = null;
					for (String imageExtension : imageExtensions) {
						imageFile = new File(documentDir, image.getFileNameBase() + "." + imageExtension);
						if (imageFile.exists())
							break;
						imageFile = null;
					}
					if (imageFile==null)
						throw new RuntimeException("No image found in directory " + documentDir.getAbsolutePath() + ", baseName " + image.getFileNameBase());
	
					coordinateStorage.addImage(sb.length(), imageFile.getName(), image.getPageIndex());
					
					for (JochreXmlParagraph par : image.getParagraphs()) {
						coordinateStorage.addParagraph(sb.length(), new Rectangle(par.getLeft(), par.getTop(), par.getRight(), par.getBottom()));
						for (JochreXmlRow row : par.getRows()) {
							coordinateStorage.addRow(sb.length(), new Rectangle(row.getLeft(), row.getTop(), row.getRight(), row.getBottom()));
							int k=0;
							for (JochreXmlWord word : row.getWords()) {
								wordCount++;
								for (JochreXmlLetter letter : word.getLetters()) {
									offsetLetterMap.put(sb.length(), letter);
									if (letter.getText().length()>1) {
										for (int j=1; j<letter.getText().length(); j++) {
											offsetLetterMap.put(sb.length()+j, letter);
										}
									}
									sb.append(letter.getText());
								}
								k++;
								boolean finalDash = false;
								if (k==row.getWords().size() && word.getText().endsWith("-") && word.getText().length()>1)
									finalDash = true;
								if (!finalDash)
									sb.append(" ");
							}
						}
						sb.append("\n");
					}
					i++;
				}
				JochreIndexDocument indexDoc = searchService.newJochreIndexDocument(documentDir, docCount, sb, coordinateStorage, startPage, endPage, fields);
				indexDoc.save(indexWriter);
				
				Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lastIndexDateFile, false),"UTF8"));
				writer.write("" + zipDate);
				writer.flush();

				writer.close();
			} // should update index?
			
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

	@Override
	public void onNewToken(CharTermAttribute termAtt, OffsetAttribute offsetAtt) {
		List<Rectangle> rectangles = new ArrayList<Rectangle>();
		JochreXmlWord currentWord = null;
		Rectangle currentRectangle = null;
		for (int i=offsetAtt.startOffset(); i<offsetAtt.endOffset(); i++) {
			JochreXmlLetter letter = offsetLetterMap.get(i);
			if (letter.getWord().equals(currentWord)) {
				currentRectangle.expand(letter.getLeft(), letter.getTop(), letter.getRight(), letter.getBottom());
			} else {
				if (currentRectangle!=null) {
					rectangles.add(currentRectangle);
				}
				currentWord = letter.getWord();
				currentRectangle = new Rectangle(letter.getLeft(), letter.getTop(), letter.getRight(), letter.getBottom());
			}
		}
		if (currentRectangle!=null)
			rectangles.add(currentRectangle);
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("Adding term " + termAtt.toString() + ", offset " + offsetAtt.startOffset() + ", rectangles: " + rectangles.toString());
		}
		
		coordinateStorage.setRectangles(offsetAtt.startOffset(), rectangles);
	}

	public int getWordsPerDoc() {
		return wordsPerDoc;
	}

	public void setWordsPerDoc(int wordsPerDoc) {
		this.wordsPerDoc = wordsPerDoc;
	}

}
