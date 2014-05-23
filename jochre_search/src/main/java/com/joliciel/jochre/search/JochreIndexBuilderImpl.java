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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import com.joliciel.talismane.utils.LogUtils;


class JochreIndexBuilderImpl implements JochreIndexBuilder, TokenOffsetObserver {
	private static final Log LOG = LogFactory.getLog(JochreIndexBuilderImpl.class);
	private File indexDir;
	private File documentDir;
	private Map<Integer, SearchLetter> offsetLetterMap;
	private CoordinateStorage coordinateStorage;
	
	private SearchServiceInternal searchService;

	/* Indexed, tokenized, not stored. */
	public static final FieldType TYPE_NOT_STORED = new FieldType();

	/* Indexed, tokenized, stored. */
	public static final FieldType TYPE_STORED = new FieldType();

	/* Not indexed, not tokenized, stored. */
	public static final FieldType TYPE_NOT_INDEXED = new FieldType();

	static {
	    TYPE_NOT_STORED.setIndexed(true);
	    TYPE_NOT_STORED.setTokenized(true);
	    TYPE_NOT_STORED.setStoreTermVectors(true);
	    TYPE_NOT_STORED.setStoreTermVectorPositions(true);
	    TYPE_NOT_STORED.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
	    TYPE_NOT_STORED.freeze();

	    TYPE_STORED.setIndexed(true);
	    TYPE_STORED.setTokenized(true);
	    TYPE_STORED.setStored(true);
	    TYPE_STORED.setStoreTermVectors(true);
	    TYPE_STORED.setStoreTermVectorPositions(true);
	    TYPE_STORED.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
	    TYPE_STORED.freeze();
	    
	    TYPE_NOT_INDEXED.setIndexed(false);
	    TYPE_NOT_INDEXED.setTokenized(false);
	    TYPE_NOT_INDEXED.setStored(true);
	    TYPE_NOT_INDEXED.setStoreTermVectors(false);
	    TYPE_NOT_INDEXED.setStoreTermVectorPositions(false);
	    TYPE_NOT_INDEXED.freeze();
	}
	
	public JochreIndexBuilderImpl(File indexDir, File documentDir) {
		super();
		this.indexDir = indexDir;
		this.documentDir = documentDir;
	}
	
	@Override
	public void buildIndex() {
		long startTime = System.currentTimeMillis();
		try {
			Directory directory = FSDirectory.open(this.indexDir);
			
			JochreAnalyzer analyzer = new JochreAnalyzer(Version.LUCENE_46);
			analyzer.setObserver(this);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_46, analyzer);
			iwc.setOpenMode(OpenMode.CREATE);
			IndexWriter indexWriter = new IndexWriter(directory, iwc);

			int i = 0;
			
			SearchDocument jochreDoc = this.searchService.newDocument();
			JochreXmlReader reader = this.searchService.getJochreXmlReader(jochreDoc);
			
			File[] xmlFiles = documentDir.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			});
			
			for (File xmlFile : xmlFiles) {
				LOG.debug("Adding xmlFile " + i + ": " + xmlFile.getName());
				reader.parseFile(xmlFile);
			}
			
			for (SearchPage page : jochreDoc.getPages()) {
				LOG.debug("Processing page: " + page.getFileNameBase());
				Document doc = new Document();
				
				offsetLetterMap = new HashMap<Integer, SearchLetter>();
				coordinateStorage = searchService.getCoordinateStorage();
				StringBuilder sb = new StringBuilder();
				for (SearchParagraph par : page.getParagraphs()) {
					for (SearchRow row : par.getRows()) {
						coordinateStorage.addRow(sb.length(), new Rectangle(row.getLeft(), row.getTop(), row.getRight(), row.getBottom()));
						int k=0;
						for (SearchWord word : row.getWords()) {
							for (SearchLetter letter : word.getLetters()) {
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
				}
				
				String contents = sb.toString();
				doc.add(new Field("text", contents, TYPE_STORED));

				doc.add(new StringField("id", page.getFileNameBase(), Field.Store.YES));
				doc.add(new Field("path", documentDir.getAbsolutePath(), TYPE_NOT_INDEXED));
				
				indexWriter.addDocument(doc);
				
				File offsetPositionFile = new File(documentDir, page.getFileNameBase() + "_offsets.obj");
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(offsetPositionFile, false));
				oos.writeObject(coordinateStorage);
				oos.flush();
				oos.close();
				i++;
			}
			indexWriter.close();
			
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		} finally {
			long endTime = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			LOG.info("Total time (ms): " + totalTime);

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
		SearchWord currentWord = null;
		Rectangle currentRectangle = null;
		for (int i=offsetAtt.startOffset(); i<offsetAtt.endOffset(); i++) {
			SearchLetter letter = offsetLetterMap.get(i);
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
	
	
}
