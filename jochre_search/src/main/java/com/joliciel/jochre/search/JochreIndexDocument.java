package com.joliciel.jochre.search;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
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

/**
 * A wrapper for a document in the Lucene sense - a single book could be split
 * into multiple documents, each with a different page range.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreIndexDocument {
	private static final Logger LOG = LoggerFactory.getLogger(JochreIndexDocument.class);

	private String contents;
	private Document doc;
	private final JochreSearchConfig config;
	private final IndexSearcher indexSearcher;
	private final int sectionNumber;
	private final String path;
	private final String name;
	private final JochreIndexDirectory directory;
	private int startPage = -1;
	private int endPage = -1;
	private int length = -1;
	private TIntObjectMap<TIntObjectMap<Rectangle>> rectangles = null;
	private TIntObjectMap<TIntIntMap> startIndexes = null;
	private TIntIntMap rowCounts = null;
	private int docId = -1;

	private JochreIndexTermLister termLister;

	/* Indexed, tokenized, stored. */
	public static final FieldType TYPE_STORED = new FieldType();

	static {
		TYPE_STORED.setTokenized(true);
		TYPE_STORED.setStored(true);
		TYPE_STORED.setStoreTermVectors(true);
		TYPE_STORED.setStoreTermVectorPositions(true);
		TYPE_STORED.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		TYPE_STORED.freeze();
	}

	public JochreIndexDocument(IndexSearcher indexSearcher, int docId, JochreSearchConfig config) throws IOException {
		this.config = config;
		this.indexSearcher = indexSearcher;
		this.docId = docId;

		this.doc = indexSearcher.doc(docId);
		this.sectionNumber = Integer.parseInt(doc.getField(JochreIndexField.sectionNumber.name()).stringValue());
		this.path = this.doc.get(JochreIndexField.path.name());
		File dir = new File(config.getContentDir(), this.path);
		this.directory = new JochreIndexDirectory(config.getContentDir(), dir);
		this.name = this.directory.getName();
	}

	public JochreIndexDocument(JochreIndexDirectory directory, int index, List<AltoPage> pages, JochreSearchConfig config) {
		this.config = config;
		this.directory = directory;
		this.sectionNumber = index;
		this.name = this.directory.getName();
		this.path = directory.getPath();
		this.doc = null;
		this.indexSearcher = null;

		StringBuilder sb = new StringBuilder();
		rectangles = new TIntObjectHashMap<>();
		startIndexes = new TIntObjectHashMap<>();
		rowCounts = new TIntIntHashMap();
		int lastSpanStart = 0;

		for (AltoPage page : pages) {
			if (LOG.isTraceEnabled())
				LOG.trace("Adding page " + page.getIndex());

			rowCounts.put(page.getIndex(), page.getTextLines().size());
			TIntObjectMap<Rectangle> rowRectangles = rectangles.get(page.getIndex());
			if (rowRectangles == null) {
				rowRectangles = new TIntObjectHashMap<>();
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

	/**
	 * Save the document to the Lucene index.
	 */
	public void save(IndexWriter indexWriter) {
		try {
			doc = new Document();
			doc.add(new StringField(JochreIndexField.name.name(), directory.getName(), Field.Store.YES));
			doc.add(new StringField(JochreIndexField.path.name(), this.path, Field.Store.YES));
			doc.add(new StoredField(JochreIndexField.startPage.name(), startPage));
			doc.add(new StoredField(JochreIndexField.endPage.name(), endPage));
			doc.add(new StringField(JochreIndexField.sectionNumber.name(), "" + sectionNumber, Field.Store.YES));
			doc.add(new Field(JochreIndexField.text.name(), contents, TYPE_STORED));
			doc.add(new StoredField(JochreIndexField.length.name(), length));
			doc.add(new StoredField(JochreIndexField.indexTime.name(), System.currentTimeMillis()));

			if (this.directory.getMetaData().containsKey(JochreIndexField.id.name()))
				doc.add(new StringField(JochreIndexField.id.name(), this.directory.getMetaData().get(JochreIndexField.id.name()), Field.Store.YES));
			if (this.directory.getMetaData().containsKey(JochreIndexField.authorEnglish.name()))
				doc.add(new TextField(JochreIndexField.authorEnglish.name(), this.directory.getMetaData().get(JochreIndexField.authorEnglish.name()),
						Field.Store.YES));
			if (this.directory.getMetaData().containsKey(JochreIndexField.titleEnglish.name()))
				doc.add(new Field(JochreIndexField.titleEnglish.name(), this.directory.getMetaData().get(JochreIndexField.titleEnglish.name()), TYPE_STORED));
			if (this.directory.getMetaData().containsKey(JochreIndexField.publisher.name()))
				doc.add(new TextField(JochreIndexField.publisher.name(), this.directory.getMetaData().get(JochreIndexField.publisher.name()), Field.Store.YES));
			if (this.directory.getMetaData().containsKey(JochreIndexField.date.name())) {
				String year = this.directory.getMetaData().get(JochreIndexField.date.name());
				doc.add(new StringField(JochreIndexField.date.name(), year, Field.Store.YES));
				try {
					doc.add(new IntPoint(JochreIndexField.year.name(), Integer.parseInt(year)));
					doc.add(new SortedNumericDocValuesField(JochreIndexField.yearSort.name(), Integer.parseInt(year)));
				} catch (NumberFormatException nfe) {
					// not a number, oh well
				}
			}
			if (this.directory.getMetaData().containsKey(JochreIndexField.author.name()))
				doc.add(new TextField(JochreIndexField.author.name(), this.directory.getMetaData().get(JochreIndexField.author.name()), Field.Store.YES));
			if (this.directory.getMetaData().containsKey(JochreIndexField.title.name()))
				doc.add(new Field(JochreIndexField.title.name(), this.directory.getMetaData().get(JochreIndexField.title.name()), TYPE_STORED));
			if (this.directory.getMetaData().containsKey(JochreIndexField.volume.name()))
				doc.add(new StringField(JochreIndexField.volume.name(), this.directory.getMetaData().get(JochreIndexField.volume.name()), Field.Store.YES));

			if (this.directory.getMetaData().containsKey(JochreIndexField.url.name()))
				doc.add(new StringField(JochreIndexField.url.name(), this.directory.getMetaData().get(JochreIndexField.url.name()), Field.Store.YES));

			for (int pageIndex : rectangles.keys()) {
				TIntObjectMap<Rectangle> rowRectangles = rectangles.get(pageIndex);
				for (int rowIndex : rowRectangles.keys()) {
					Rectangle rect = rowRectangles.get(rowIndex);
					String fieldName = JochreIndexField.rect.name() + pageIndex + "_" + rowIndex;
					doc.add(new StoredField(fieldName, this.rectToString(rect)));
				}
			}

			for (int pageIndex : startIndexes.keys()) {
				TIntIntMap rowStartIndexes = startIndexes.get(pageIndex);
				for (int rowIndex : rowStartIndexes.keys()) {
					int startIndex = rowStartIndexes.get(rowIndex);
					String fieldName = JochreIndexField.start.name() + pageIndex + "_" + rowIndex;
					doc.add(new StoredField(fieldName, startIndex));
				}
			}

			for (int pageIndex : rowCounts.keys()) {
				int rowCount = rowCounts.get(pageIndex);
				String fieldName = JochreIndexField.rowCount.name() + pageIndex;
				doc.add(new StoredField(fieldName, rowCount));
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

	/**
	 * The full string contents of the document.
	 */
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

	/**
	 * Get the rectangle enclosing a particular row.
	 */
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
			String fieldName = JochreIndexField.rect.name() + pageIndex + "_" + rowIndex;
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

	/**
	 * Return the content index of the first character on a given row.
	 */
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
			String fieldName = JochreIndexField.start.name() + pageIndex + "_" + rowIndex;
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

	/**
	 * Return the content index following the last character on a given row.
	 */
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

	/**
	 * Get the number of rows on a given page.
	 */
	public int getRowCount(int pageIndex) {
		int rowCount = -1;
		if (rowCounts != null) {
			return rowCounts.get(pageIndex);
		} else {
			String fieldName = JochreIndexField.rowCount.name() + pageIndex;
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

	/**
	 * The index of the first page contained in this document.
	 */
	public int getStartPage() {
		if (startPage < 0 && this.doc != null) {
			startPage = doc.getField(JochreIndexField.startPage.name()).numericValue().intValue();
		}
		return startPage;
	}

	/**
	 * The index of the last page contained in this document.
	 */
	public int getEndPage() {
		if (endPage < 0 && this.doc != null) {
			endPage = doc.getField(JochreIndexField.endPage.name()).numericValue().intValue();
		}
		return endPage;
	}

	/**
	 * The document's name, used as a prefix for various files in the document's
	 * directory.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the image corresponding to a particular page index.
	 */
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

	/**
	 * Get the word starting at the given offset, or at the maximum offset prior to
	 * this one.
	 * 
	 * @throws IOException
	 */
	public JochreIndexWord getWord(int startOffset) throws IOException {
		if (this.indexSearcher == null)
			throw new JochreException("Can only get word image for documents already in index");
		if (this.termLister == null)
			this.termLister = new JochreIndexTermLister(docId, indexSearcher);
		JochreIndexWord word = new JochreIndexWord(this, startOffset, this.config, this.termLister);
		return word;
	}

	/**
	 * The document's path, relative to the content directory.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * The document's section number, which, together with the path, identifies it
	 * uniquely.
	 */
	public int getSectionNumber() {
		return sectionNumber;
	}

	/**
	 * The Lucene doc id for this document.
	 */
	public int getDocId() {
		return docId;
	}
}
