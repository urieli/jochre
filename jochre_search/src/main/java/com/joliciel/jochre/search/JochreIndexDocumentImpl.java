package com.joliciel.jochre.search;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;

import com.joliciel.jochre.search.alto.AltoPage;
import com.joliciel.jochre.search.alto.AltoString;
import com.joliciel.jochre.search.alto.AltoTextBlock;
import com.joliciel.jochre.search.alto.AltoTextLine;
import com.joliciel.talismane.utils.LogUtils;

class JochreIndexDocumentImpl implements JochreIndexDocument {
	private static final Log LOG = LogFactory.getLog(JochreIndexDocumentImpl.class);
	private static String[] imageExtensions = new String[] {"png","jpg","jpeg","gif","tiff"};

	private String contents;
	private Document doc;
	private int index = -1;
	private String path = null;
	private String name = null;
	private File directory = null;
	private String author = null;
	private String title = null;
	private String url = null;
	private int startPage = -1;
	private int endPage = -1;
	private TIntObjectMap<TIntObjectMap<TIntObjectMap<Rectangle>>> rectangles = null;
	
	@SuppressWarnings("unused")
	private Map<String,String> fields;

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
	
	public JochreIndexDocumentImpl(IndexSearcher indexSearcher, int docId) {
		try {
			this.doc = indexSearcher.doc(docId);
			this.index = Integer.parseInt(this.doc.get("index"));
			this.path = this.doc.get("path");
			this.directory = new File(this.path);
			this.name = this.directory.getName();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public JochreIndexDocumentImpl(File directory, int index, List<AltoPage> pages, Map<String,String> fields) {
		this.directory = directory;
		this.fields = fields;
		this.index = index;
		this.name = this.directory.getName();
		
		StringBuilder sb = new StringBuilder();
		rectangles = new TIntObjectHashMap<TIntObjectMap<TIntObjectMap<Rectangle>>>();
		int offsetBase = 0;
		for (AltoPage page : pages) {
			int lastOffset = 0;
			TIntObjectMap<TIntObjectMap<Rectangle>> blockRectangles = rectangles.get(page.getPageIndex());
			if (blockRectangles==null) {
				blockRectangles = new TIntObjectHashMap<TIntObjectMap<Rectangle>>();
				rectangles.put(page.getPageIndex(), blockRectangles);
			}
			for (AltoTextBlock textBlock : page.getTextBlocks()) {
				TIntObjectMap<Rectangle> rowRectangles = blockRectangles.get(textBlock.getIndex());
				if (rowRectangles==null) {
					rowRectangles = new TIntObjectHashMap<Rectangle>();
					blockRectangles.put(textBlock.getIndex(), rowRectangles);
				}
				for (AltoTextLine textLine : textBlock.getTextLines()) {
					rowRectangles.put(textLine.getIndex(), textLine.getRectangle());
					for (AltoString string : textLine.getStrings()) {
						if (string.getSpanStart()>lastOffset) {
							sb.append(" ");
							LOG.debug("Added space before " + string);
						}
						
						sb.append(string.getContent());
						lastOffset = string.getSpanEnd();
						
						if (offsetBase + lastOffset != sb.length()) {
							LOG.debug("offsetBase: " + offsetBase + " +  lastOffset " + lastOffset + " = " + (offsetBase + lastOffset));
							LOG.debug("sb.length(): " + sb.length());
							LOG.debug("sb: " + sb.toString().replace('\n', '|'));
							LOG.debug("");
						}
					}
				}
				sb.append("\n");
				lastOffset+=1;
			}
			offsetBase += lastOffset;
		}
		this.contents = sb.toString();
		
		this.startPage = pages.get(0).getPageIndex();
		this.endPage = pages.get(pages.size()-1).getPageIndex();

		this.author = fields.get("Author");
		this.title = fields.get("Title");
		this.url = fields.get("Keywords");
	}
	
	public void save(IndexWriter indexWriter) {
		try {
			doc = new Document();
			doc.add(new Field("name", directory.getName(), TYPE_NOT_INDEXED));
			doc.add(new Field("startPage", "" + startPage, TYPE_NOT_INDEXED));
			doc.add(new Field("endPage", "" + endPage, TYPE_NOT_INDEXED));
			doc.add(new Field("index", "" + index, TYPE_NOT_INDEXED));
			doc.add(new Field("text", contents, TYPE_STORED));
			doc.add(new Field("path", directory.getAbsolutePath(), TYPE_NOT_INDEXED));
			
			if (author!=null)
				doc.add(new StringField("author", author, Field.Store.YES));
			if (title!=null)
				doc.add(new StringField("title", title, Field.Store.YES));
			if (url!=null)
				doc.add(new StringField("url", url, Field.Store.YES));
			
			for (int pageIndex : rectangles.keys()) {
				TIntObjectMap<TIntObjectMap<Rectangle>> blockRectangles = rectangles.get(pageIndex);
				for (int blockIndex : blockRectangles.keys()) {
					TIntObjectMap<Rectangle> rowRectangles = blockRectangles.get(blockIndex);
					for (int rowIndex : rowRectangles.keys()) {
						Rectangle rect = rowRectangles.get(rowIndex);
						String fieldName = "r" + pageIndex + "_" + blockIndex + "_" + rowIndex;
						doc.add(new Field(fieldName, rect.getString(), TYPE_NOT_INDEXED));
					}
				}
			}

			indexWriter.addDocument(doc);
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	@Override
	public String getContents() {
		if (this.contents==null) {
			this.contents = doc.get("text");
		}
		return contents;
	}

	public File getDirectory() {
		return directory;
	}

	public String getAuthor() {
		return author;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public Rectangle getRectangle(int pageIndex, int textBlockIndex,
			int textLineIndex) {
		Rectangle rect = null;
		if (rectangles!=null) {
			TIntObjectMap<TIntObjectMap<Rectangle>> blockRectangles = rectangles.get(pageIndex);
			TIntObjectMap<Rectangle> rowRectangles = blockRectangles.get(textBlockIndex);
			rect = rowRectangles.get(textLineIndex);
		} else if (doc!=null) {
			String fieldName = "r" + pageIndex + "_" + textBlockIndex + "_" + textLineIndex;
			String rectString = this.doc.get(fieldName);
			rect = new Rectangle(rectString);
		}
		return rect;
	}

	public int getStartPage() {
		if (startPage<0 && this.doc!=null) {
			startPage = Integer.parseInt(doc.get("startPage"));
		}
		return startPage;
	}

	public int getEndPage() {
		if (endPage<0 && this.doc!=null) {
			endPage = Integer.parseInt(doc.get("endPage"));
		}
		return endPage;
	}

	public String getName() {
		return name;
	}

	@Override
	public File getImageFile(int pageIndex) {
		File imageFile = null;
		for (String imageExtension : imageExtensions) {
			String formatted = this.getName() + String.format("%04d", pageIndex);

			imageFile = new File(this.getDirectory(), formatted + "." + imageExtension);
			if (imageFile.exists())
				break;
			imageFile = null;
		}
		return imageFile;
	}
}
