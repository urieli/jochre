package com.joliciel.jochre.search;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.alto.AltoPage;
import com.joliciel.jochre.search.alto.AltoString;
import com.joliciel.jochre.search.alto.AltoTextBlock;
import com.joliciel.jochre.search.alto.AltoTextLine;
import com.joliciel.jochre.utils.JochreException;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

class JochreIndexDocumentImpl implements JochreIndexDocument {
	private static final Logger LOG = LoggerFactory.getLogger(JochreIndexDocumentImpl.class);

	private String contents;
	private Document doc;
	private int sectionNumber = -1;
	private String path = null;
	private String name = null;
	private JochreIndexDirectory directory = null;
	private int startPage = -1;
	private int endPage = -1;
	private int length = -1;
	private TIntObjectMap<TIntObjectMap<Rectangle>> rectangles = null;
	private TIntObjectMap<TIntIntMap> startIndexes = null;
	private TIntIntMap rowCounts = null;
	private int docId = -1;
	private JochreIndexSearcher indexSearcher = null;
	private JochreIndexTermLister termLister = null;

	private SearchServiceInternal searchService;

	/* Indexed, tokenized, not stored. */
	public static final FieldType TYPE_NOT_STORED = new FieldType();

	/* Indexed, tokenized, stored. */
	public static final FieldType TYPE_STORED = new FieldType();

	/* Not indexed, not tokenized, stored. */
	public static final FieldType TYPE_NOT_INDEXED = new FieldType();

	static {
		TYPE_NOT_STORED.setTokenized(true);
		TYPE_NOT_STORED.setStoreTermVectors(true);
		TYPE_NOT_STORED.setStoreTermVectorPositions(true);
		TYPE_NOT_STORED.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		TYPE_NOT_STORED.freeze();

		TYPE_STORED.setTokenized(true);
		TYPE_STORED.setStored(true);
		TYPE_STORED.setStoreTermVectors(true);
		TYPE_STORED.setStoreTermVectorPositions(true);
		TYPE_STORED.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		TYPE_STORED.freeze();

		TYPE_NOT_INDEXED.setTokenized(false);
		TYPE_NOT_INDEXED.setStored(true);
		TYPE_NOT_INDEXED.setStoreTermVectors(false);
		TYPE_NOT_INDEXED.setStoreTermVectorPositions(false);
		TYPE_NOT_INDEXED.freeze();
	}

	public JochreIndexDocumentImpl(JochreIndexSearcher indexSearcher, int docId) {
		try {
			this.docId = docId;
			this.indexSearcher = indexSearcher;
			this.doc = indexSearcher.getIndexSearcher().doc(docId);
			this.sectionNumber = Integer.parseInt(this.doc.get(JochreIndexField.index.name()));
			this.path = this.doc.get(JochreIndexField.path.name());
			File dir = new File(indexSearcher.getContentDir(), this.path);
			this.directory = new JochreIndexDirectoryImpl(indexSearcher.getContentDir(), dir);
			this.name = this.directory.getName();
		} catch (IOException e) {
			LOG.error("Failed setup jochreIndexDocument for id " + docId, e);
			throw new RuntimeException(e);
		}
	}

	public JochreIndexDocumentImpl(JochreIndexDirectory directory, int index, List<AltoPage> pages) {
		this.directory = directory;
		this.sectionNumber = index;
		this.name = this.directory.getName();
		this.path = directory.getPath();

		StringBuilder sb = new StringBuilder();
		rectangles = new TIntObjectHashMap<TIntObjectMap<Rectangle>>();
		startIndexes = new TIntObjectHashMap<TIntIntMap>();
		rowCounts = new TIntIntHashMap();
		int lastSpanStart = 0;

		for (AltoPage page : pages) {
			if (LOG.isTraceEnabled())
				LOG.trace("Adding page " + page.getIndex());

			rowCounts.put(page.getIndex(), page.getTextLines().size());
			TIntObjectMap<Rectangle> rowRectangles = rectangles.get(page.getIndex());
			if (rowRectangles == null) {
				rowRectangles = new TIntObjectHashMap<Rectangle>();
				rectangles.put(page.getIndex(), rowRectangles);
			}
			TIntIntMap rowStartIndexes = startIndexes.get(page.getIndex());
			if (rowStartIndexes == null) {
				rowStartIndexes = new TIntIntHashMap(256, 0.7f, -1, -1);
				startIndexes.put(page.getIndex(), rowStartIndexes);
			}

			boolean spaceAdded = true;
			for (AltoTextBlock textBlock : page.getTextBlocks()) {
				boolean endOfLineSpaceRequired = false;
				for (AltoTextLine textLine : textBlock.getTextLines()) {
					if (textLine.getStrings().size() > 0) {

						if (LOG.isTraceEnabled())
							LOG.trace("Adding row " + textLine.getIndex());

						if (endOfLineSpaceRequired && !spaceAdded) {
							// add a space between lines, unless the last line
							// ended
							// with a hyphen
							sb.append(JochreSearchConstants.INDEX_NEWLINE);
							spaceAdded = true;
						}
						AltoString lastString = null;
						for (AltoString string : textLine.getStrings()) {
							int newSpanStart = sb.length();
							if (string.isWhiteSpace() && !spaceAdded && lastString != null) {
								sb.append(' ');
								spaceAdded = true;
							} else {
								sb.append(string.getContent());
								spaceAdded = false;
								if (LOG.isTraceEnabled())
									LOG.trace("Added " + string + ". Offset: " + sb.length());
							}
							int newSpanEnd = sb.length();

							string.setSpanStart(newSpanStart);
							string.setSpanEnd(newSpanEnd);

							lastString = string;
						}
						if (lastString != null && lastString.getContent().contains(JochreSearchConstants.INDEX_NEWLINE)) {
							endOfLineSpaceRequired = false;
						} else {
							endOfLineSpaceRequired = true;
						}

						lastSpanStart = textLine.getStrings().get(0).getSpanStart();
					} // have strings on this row

					rowStartIndexes.put(textLine.getIndex(), lastSpanStart);
					rowRectangles.put(textLine.getIndex(), textLine.getRectangle());
				} // next row in this block
				if (spaceAdded && sb.length() > 0) {
					sb.deleteCharAt(sb.length() - 1);
					spaceAdded = false;
				}

				sb.append(JochreSearchConstants.INDEX_PARAGRAPH);
				spaceAdded = true;

				if (LOG.isTraceEnabled())
					LOG.trace("Added newline. Offset: " + sb.length());
			} // next block in this page
		} // next page

		this.contents = sb.toString();
		this.length = this.contents.length();

		this.startPage = pages.get(0).getIndex();
		this.endPage = pages.get(pages.size() - 1).getIndex();
	}

	@Override
	public void save(IndexWriter indexWriter) {
		try {
			doc = new Document();
			doc.add(new StringField(JochreIndexField.name.name(), directory.getName(), Field.Store.YES));
			doc.add(new StringField(JochreIndexField.path.name(), this.path, Field.Store.YES));
			doc.add(new Field(JochreIndexField.startPage.name(), "" + startPage, TYPE_NOT_INDEXED));
			doc.add(new Field(JochreIndexField.endPage.name(), "" + endPage, TYPE_NOT_INDEXED));
			doc.add(new IntField(JochreIndexField.index.name(), sectionNumber, Field.Store.YES));
			doc.add(new Field(JochreIndexField.text.name(), contents, TYPE_STORED));
			doc.add(new IntField(JochreIndexField.length.name(), length, Field.Store.YES));
			doc.add(new LongField(JochreIndexField.indexTime.name(), System.currentTimeMillis(), Field.Store.YES));

			if (this.directory.getMetaData().containsKey(JochreIndexField.id.name()))
				doc.add(new StringField(JochreIndexField.id.name(), this.directory.getMetaData().get(JochreIndexField.id.name()), Field.Store.YES));
			if (this.directory.getMetaData().containsKey(JochreIndexField.author.name()))
				doc.add(new Field(JochreIndexField.author.name(), this.directory.getMetaData().get(JochreIndexField.author.name()), TYPE_STORED));
			if (this.directory.getMetaData().containsKey(JochreIndexField.title.name()))
				doc.add(new Field(JochreIndexField.title.name(), this.directory.getMetaData().get(JochreIndexField.title.name()), TYPE_STORED));
			if (this.directory.getMetaData().containsKey(JochreIndexField.publisher.name()))
				doc.add(new Field(JochreIndexField.publisher.name(), this.directory.getMetaData().get(JochreIndexField.publisher.name()), TYPE_STORED));
			if (this.directory.getMetaData().containsKey(JochreIndexField.date.name()))
				doc.add(new StringField(JochreIndexField.date.name(), this.directory.getMetaData().get(JochreIndexField.date.name()), Field.Store.YES));
			if (this.directory.getMetaData().containsKey(JochreIndexField.authorLang.name()))
				doc.add(new Field(JochreIndexField.authorLang.name(), this.directory.getMetaData().get(JochreIndexField.authorLang.name()), TYPE_STORED));
			if (this.directory.getMetaData().containsKey(JochreIndexField.titleLang.name()))
				doc.add(new Field(JochreIndexField.titleLang.name(), this.directory.getMetaData().get(JochreIndexField.titleLang.name()), TYPE_STORED));
			if (this.directory.getMetaData().containsKey(JochreIndexField.volume.name()))
				doc.add(new StringField(JochreIndexField.volume.name(), this.directory.getMetaData().get(JochreIndexField.volume.name()), Field.Store.YES));

			if (this.directory.getMetaData().containsKey(JochreIndexField.url.name()))
				doc.add(new StringField(JochreIndexField.url.name(), this.directory.getMetaData().get(JochreIndexField.url.name()), Field.Store.YES));

			for (int pageIndex : rectangles.keys()) {
				TIntObjectMap<Rectangle> rowRectangles = rectangles.get(pageIndex);
				for (int rowIndex : rowRectangles.keys()) {
					Rectangle rect = rowRectangles.get(rowIndex);
					String fieldName = "rect" + pageIndex + "_" + rowIndex;
					doc.add(new Field(fieldName, this.rectToString(rect), TYPE_NOT_INDEXED));
				}
			}

			for (int pageIndex : startIndexes.keys()) {
				TIntIntMap rowStartIndexes = startIndexes.get(pageIndex);
				for (int rowIndex : rowStartIndexes.keys()) {
					int startIndex = rowStartIndexes.get(rowIndex);
					String fieldName = "start" + pageIndex + "_" + rowIndex;
					doc.add(new IntField(fieldName, startIndex, Field.Store.YES));
				}
			}

			for (int pageIndex : rowCounts.keys()) {
				int rowCount = rowCounts.get(pageIndex);
				String fieldName = "rowCount" + pageIndex;
				doc.add(new IntField(fieldName, rowCount, Field.Store.YES));
			}

			indexWriter.addDocument(doc);

			if (LOG.isTraceEnabled()) {
				for (IndexableField field : doc.getFields()) {
					if (!field.name().equals(JochreIndexField.text.name()))
						LOG.trace(field.toString());
				}
			}
		} catch (IOException e) {
			LOG.error("Failed save JochreIndexDocument " + this.getName(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getContents() {
		if (this.contents == null) {
			this.contents = doc.get(JochreIndexField.text.name());
		}
		return contents;
	}

	public int getLength() {
		if (this.length < 0)
			this.length = doc.getField(JochreIndexField.length.name()).numericValue().intValue();
		return length;
	}

	@Override
	public Rectangle getRowRectangle(int pageIndex, int rowIndex) {
		Rectangle rect = null;
		if (rectangles != null) {
			TIntObjectMap<Rectangle> rowRectangles = rectangles.get(pageIndex);
			if (rowRectangles == null)
				throw new IndexFieldNotFoundException("No rectangles for pageIndex " + pageIndex);

			rect = rowRectangles.get(rowIndex);
			if (rect == null)
				throw new IndexFieldNotFoundException("No rectangles for pageIndex " + pageIndex + ", rowIndex " + rowIndex);
		} else if (doc != null) {
			String fieldName = "rect" + pageIndex + "_" + rowIndex;
			String rectString = this.doc.get(fieldName);
			if (rectString == null) {
				throw new IndexFieldNotFoundException("No rectangle found for " + fieldName + " in document " + this.doc.get(JochreIndexField.name.name())
						+ ", pages " + this.doc.get(JochreIndexField.startPage.name()) + " to " + this.doc.get(JochreIndexField.endPage.name()) + " (docId="
						+ this.docId + ")");
			}
			rect = this.stringToRect(rectString);
		}
		return rect;
	}

	@Override
	public int getStartIndex(int pageIndex, int rowIndex) {
		int startIndex = -1;
		if (startIndexes != null) {
			TIntIntMap rowStartIndexes = startIndexes.get(pageIndex);
			if (rowStartIndexes == null)
				throw new IndexFieldNotFoundException("No start indexes for pageIndex " + pageIndex);

			startIndex = rowStartIndexes.get(rowIndex);
			if (startIndex == -1)
				throw new IndexFieldNotFoundException("No start index for pageIndex " + pageIndex + ", rowIndex " + rowIndex);
		} else if (doc != null) {
			String fieldName = "start" + pageIndex + "_" + rowIndex;
			Number startIndexObj = null;
			IndexableField field = this.doc.getField(fieldName);
			if (field != null)
				startIndexObj = field.numericValue();
			if (startIndexObj == null) {
				throw new IndexFieldNotFoundException("No start index found for " + fieldName + " in document " + this.doc.get(JochreIndexField.name.name())
						+ ", pages " + this.doc.get(JochreIndexField.startPage.name()) + " to " + this.doc.get(JochreIndexField.endPage.name()));
			}
			startIndex = startIndexObj.intValue();
		}
		return startIndex;
	}

	@Override
	public int getEndIndex(int pageIndex, int rowIndex) {
		int endIndex = -1;
		if (rowIndex + 1 < this.getRowCount(pageIndex))
			endIndex = this.getStartIndex(pageIndex, rowIndex + 1);
		else {
			for (int i = pageIndex + 1; i <= this.getEndPage(); i++) {
				try {
					endIndex = this.getStartIndex(i, 0);
					break;
				} catch (IndexFieldNotFoundException e) {
					// do nothing
				}
			}
		}
		if (endIndex == -1) {
			endIndex = this.getLength();
		}
		return endIndex;
	}

	@Override
	public int getRowCount(int pageIndex) {
		int rowCount = -1;
		if (rowCounts != null) {
			return rowCounts.get(pageIndex);
		} else {
			String fieldName = "rowCount" + pageIndex;
			Number rowCountObj = null;
			IndexableField field = this.doc.getField(fieldName);
			if (field != null)
				rowCountObj = field.numericValue();
			if (rowCountObj == null) {
				throw new IndexFieldNotFoundException("NorowCount found for " + fieldName + " in document " + this.doc.get(JochreIndexField.name.name())
						+ ", pages " + this.doc.get(JochreIndexField.startPage.name()) + " to " + this.doc.get(JochreIndexField.endPage.name()));
			}
			rowCount = rowCountObj.intValue();
		}
		return rowCount;
	}

	@Override
	public int getStartPage() {
		if (startPage < 0 && this.doc != null) {
			startPage = Integer.parseInt(doc.get(JochreIndexField.startPage.name()));
		}
		return startPage;
	}

	@Override
	public int getEndPage() {
		if (endPage < 0 && this.doc != null) {
			endPage = Integer.parseInt(doc.get(JochreIndexField.endPage.name()));
		}
		return endPage;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public BufferedImage getImage(int pageIndex) {
		PdfImageReader pdfImageReader = new PdfImageReader(this.directory.getPdfFile());
		BufferedImage image = pdfImageReader.readImage(pageIndex);
		return image;
	}

	private String rectToString(Rectangle rect) {
		return rect.x + "|" + rect.y + "|" + rect.width + "|" + rect.height;
	}

	private Rectangle stringToRect(String string) {
		String[] parts = string.split("\\|");
		int x = Integer.parseInt(parts[0]);
		int y = Integer.parseInt(parts[1]);
		int width = Integer.parseInt(parts[2]);
		int height = Integer.parseInt(parts[3]);
		Rectangle rect = new Rectangle(x, y, width, height);
		return rect;
	}

	@Override
	public JochreIndexWord getWord(int startOffset) {
		if (this.indexSearcher == null)
			throw new JochreException("Can only get word image for documents already in index");
		JochreIndexWord word = searchService.getWord(this, startOffset);
		return word;

	}

	@Override
	public JochreIndexTermLister getTermLister() {
		if (this.termLister == null)
			termLister = new JochreIndexTermLister(docId, indexSearcher.getIndexSearcher());
		return termLister;
	}

	public SearchServiceInternal getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchServiceInternal searchService) {
		this.searchService = searchService;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public int getSectionNumber() {
		return sectionNumber;
	}
}
