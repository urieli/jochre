package com.joliciel.jochre.search;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NavigableMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;

import com.joliciel.jochre.search.JochreIndexTermLister.JochreTerm;
import com.joliciel.jochre.search.alto.AltoPage;
import com.joliciel.jochre.search.alto.AltoString;
import com.joliciel.jochre.search.alto.AltoTextBlock;
import com.joliciel.jochre.search.alto.AltoTextLine;
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.utils.LogUtils;

class JochreIndexDocumentImpl implements JochreIndexDocument {
	private static final Log LOG = LogFactory.getLog(JochreIndexDocumentImpl.class);
	
	private String contents;
	private Document doc;
	private int index = -1;
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
	private IndexSearcher indexSearcher = null;
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
	
	public JochreIndexDocumentImpl(IndexSearcher indexSearcher, int docId) {
		try {
			this.docId = docId;
			this.indexSearcher = indexSearcher;
			this.doc = indexSearcher.doc(docId);
			this.index = Integer.parseInt(this.doc.get(JochreIndexField.index.name()));
			this.path = this.doc.get(JochreIndexField.path.name());
			File dir = new File(this.path);
			this.directory = new JochreIndexDirectoryImpl(dir);
			this.name = this.directory.getName();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new JochreException(e);
		}
	}

	public JochreIndexDocumentImpl(JochreIndexDirectory directory, int index, List<AltoPage> pages) {
		this.directory = directory;
		this.index = index;
		this.name = this.directory.getName();
		
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
			if (rowRectangles==null) {
				rowRectangles = new TIntObjectHashMap<Rectangle>();
				rectangles.put(page.getIndex(), rowRectangles);
			}
			TIntIntMap rowStartIndexes = startIndexes.get(page.getIndex());
			if (rowStartIndexes==null) {
				rowStartIndexes = new TIntIntHashMap(256, 0.7f, -1, -1);
				startIndexes.put(page.getIndex(), rowStartIndexes);
			}
			
			for (AltoTextBlock textBlock : page.getTextBlocks()) {
				for (AltoTextLine textLine : textBlock.getTextLines()) {
					if (LOG.isTraceEnabled())
						LOG.trace("Adding row " + textLine.getIndex());
					rowRectangles.put(textLine.getIndex(), textLine.getRectangle());
					AltoString lastString = null;
					for (AltoString string : textLine.getStrings()) {
						int newSpanStart = sb.length();
						if (string.isWhiteSpace()) {
							sb.append(' ');							
						} else {
							sb.append(string.getContent());
							if (LOG.isTraceEnabled())
								LOG.trace("Added " + string + ". Offset: " + sb.length());
						}
						int newSpanEnd = sb.length();
						
						string.setSpanStart(newSpanStart);
						string.setSpanEnd(newSpanEnd);

						lastString = string;
					}
					if (lastString!=null && lastString.getContent().endsWith("-")) {
						// no space added after hyphen at end of line
					} else {
						sb.append(' ');
					}
					if (textLine.getStrings().size()>0) {
						lastSpanStart = textLine.getStrings().get(0).getSpanStart();
						rowStartIndexes.put(textLine.getIndex(), lastSpanStart);
					} else {
						rowStartIndexes.put(textLine.getIndex(), lastSpanStart);
					}
				}
				sb.append("\n");
				if (LOG.isTraceEnabled())
					LOG.trace("Added newline. Offset: " + sb.length());
			}
		}
		this.contents = sb.toString();
		this.length = this.contents.length();
		
		this.startPage = pages.get(0).getIndex();
		this.endPage = pages.get(pages.size()-1).getIndex();
	}
	
	public void save(IndexWriter indexWriter) {
		try {
			doc = new Document();
			doc.add(new StringField(JochreIndexField.name.name(), directory.getName(), Field.Store.YES));
			doc.add(new StringField(JochreIndexField.path.name(),directory.getDirectory().getAbsolutePath(), Field.Store.YES));
			doc.add(new Field(JochreIndexField.startPage.name(), "" + startPage, TYPE_NOT_INDEXED));
			doc.add(new Field(JochreIndexField.endPage.name(), "" + endPage, TYPE_NOT_INDEXED));
			doc.add(new IntField(JochreIndexField.index.name(), index, Field.Store.YES));
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
						LOG.trace(field);
				}
			}
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new JochreException(ioe);
		}
	}
	
	@Override
	public String getContents() {
		if (this.contents==null) {
			this.contents = doc.get(JochreIndexField.text.name());
		}
		return contents;
	}
	
	public int getLength() {
		if (this.length<0)
			this.length = doc.getField(JochreIndexField.length.name()).numericValue().intValue();
		return length;
	}
	
	@Override
	public Rectangle getRectangle(int pageIndex,
			int rowIndex) {
		Rectangle rect = null;
		if (rectangles!=null) {
			TIntObjectMap<Rectangle> rowRectangles = rectangles.get(pageIndex);
			if (rowRectangles==null)
				throw new IndexFieldNotFoundException("No rectangles for pageIndex " + pageIndex);
			
			rect = rowRectangles.get(rowIndex);
			if (rect==null)
				throw new IndexFieldNotFoundException("No rectangles for pageIndex " + pageIndex + ", rowIndex " + rowIndex);
		} else if (doc!=null) {
			String fieldName = "rect" + pageIndex + "_" + rowIndex;
			String rectString = this.doc.get(fieldName);
			if (rectString==null) {
				throw new IndexFieldNotFoundException("No rectangle found for " + fieldName + " in document " + this.doc.get(JochreIndexField.name.name())
						+ ", pages " + this.doc.get(JochreIndexField.startPage.name()) + " to " + this.doc.get(JochreIndexField.endPage.name()));
			}
			rect = this.stringToRect(rectString);
		}
		return rect;
	}
	
	@Override
	public int getStartIndex(int pageIndex,
			int rowIndex) {
		int startIndex = -1;
		if (startIndexes!=null) {
			TIntIntMap rowStartIndexes = startIndexes.get(pageIndex);
			if (rowStartIndexes==null)
				throw new IndexFieldNotFoundException("No start indexes for pageIndex " + pageIndex);
			
			startIndex = rowStartIndexes.get(rowIndex);
			if (startIndex==-1)
				throw new IndexFieldNotFoundException("No start index for pageIndex " + pageIndex + ", rowIndex " + rowIndex);
		} else if (doc!=null) {
			String fieldName = "start" + pageIndex + "_" + rowIndex;
			Number startIndexObj = null;
			IndexableField field = this.doc.getField(fieldName);
			if (field!=null)
				startIndexObj = field.numericValue();
			if (startIndexObj==null) {
				throw new IndexFieldNotFoundException("No start index found for " + fieldName + " in document " + this.doc.get(JochreIndexField.name.name())
						+ ", pages " + this.doc.get(JochreIndexField.startPage.name()) + " to " + this.doc.get(JochreIndexField.endPage.name()));
			}
			startIndex = startIndexObj.intValue();
		}
		return startIndex;
	}
	
	@Override
	public int getEndIndex(int pageIndex,
			int rowIndex) {
		int endIndex = -1;
		if (rowIndex+1 < this.getRowCount(pageIndex))
			endIndex = this.getStartIndex(pageIndex, rowIndex+1);
		else {
			for (int i=pageIndex+1; i<=this.getEndPage(); i++) {
				try {
					endIndex = this.getStartIndex(i, 0);
					break;
				} catch (IndexFieldNotFoundException e) {
					// do nothing
				}
			}
		}
		if (endIndex==-1) {
			endIndex = this.getLength();
		}
		return endIndex;
	}
	
	public int getRowCount(int pageIndex) {
		int rowCount = -1;
		if (rowCounts!=null) {
			return rowCounts.get(pageIndex);
		} else {
			String fieldName = "rowCount" + pageIndex;
			Number rowCountObj = null;
			IndexableField field = this.doc.getField(fieldName);
			if (field!=null)
				rowCountObj = field.numericValue();
			if (rowCountObj==null) {
				throw new IndexFieldNotFoundException("NorowCount found for " + fieldName + " in document " + this.doc.get(JochreIndexField.name.name())
						+ ", pages " + this.doc.get(JochreIndexField.startPage.name()) + " to " + this.doc.get(JochreIndexField.endPage.name()));
			}
			rowCount = rowCountObj.intValue();
		}
		return rowCount;
	}
	
	public int getStartPage() {
		if (startPage<0 && this.doc!=null) {
			startPage = Integer.parseInt(doc.get(JochreIndexField.startPage.name()));
		}
		return startPage;
	}

	public int getEndPage() {
		if (endPage<0 && this.doc!=null) {
			endPage = Integer.parseInt(doc.get(JochreIndexField.endPage.name()));
		}
		return endPage;
	}

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
	public BufferedImage getWordImage(int startOffset) {
		if (this.indexSearcher==null)
			throw new JochreException("Can only get word image for documents already in index");
		JochreIndexTermLister termLister = this.getTermLister();
		NavigableMap<Integer,JochreTerm> termMap = termLister.getTextTermByOffset();
		
		JochreTerm jochreTerm = termMap.floorEntry(startOffset).getValue();
		if (jochreTerm == null) {
			throw new JochreException("No term found at startoffset " + startOffset + ", in doc " + this.getName() + ", index " + this.index);
		}
		int pageIndex = jochreTerm.getPayload().getPageIndex();
		BufferedImage originalImage = this.getImage(pageIndex);
		Rectangle rect = jochreTerm.getPayload().getRectangle();
		BufferedImage imageSnippet = originalImage.getSubimage(rect.x, rect.y, rect.width, rect.height);
		
		Rectangle secondaryRect = jochreTerm.getPayload().getSecondaryRectangle();
		if (secondaryRect!=null) {
			BufferedImage secondSnippet = originalImage.getSubimage(secondaryRect.x, secondaryRect.y, secondaryRect.width, secondaryRect.height);
			imageSnippet = joinBufferedImage(secondSnippet, imageSnippet);
		}
		return imageSnippet;
	}
	
    /**
     * From http://stackoverflow.com/questions/20826216/copy-two-buffered-image-into-one-image-side-by-side
     */
    public static BufferedImage joinBufferedImage(BufferedImage img1, BufferedImage img2) {
        //do some calculations first
        int offset  = 5;
        int wid = img1.getWidth()+img2.getWidth()+offset;
        int height = Math.max(img1.getHeight(),img2.getHeight())+offset;
        //create a new buffer and draw two images into the new image
        BufferedImage newImage = new BufferedImage(wid,height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();
        Color oldColor = g2.getColor();
        //fill background
        g2.setPaint(Color.WHITE);
        g2.fillRect(0, 0, wid, height);
        //draw image
        g2.setColor(oldColor);
        g2.drawImage(img1, null, 0, 0);
        g2.drawImage(img2, null, img1.getWidth()+offset, 0);
        g2.dispose();
        return newImage;
    }
	
	@Override
	public String getWord(int startOffset) {
		if (this.indexSearcher==null)
			throw new JochreException("Can only get word image for documents already in index");
		JochreIndexTermLister termLister = this.getTermLister();
		NavigableMap<Integer,JochreTerm> termMap = termLister.getTextTermByOffset();
		
		JochreTerm jochreTerm = termMap.floorEntry(startOffset).getValue();
		if (jochreTerm == null) {
			throw new JochreException("No term found at startoffset " + startOffset + ", in doc " + this.getName() + ", index " + this.index);
		}
		String word = this.getContents().substring(jochreTerm.start, jochreTerm.end);
		return word;
	}

	private JochreIndexTermLister getTermLister() {
		if (this.termLister==null)
			termLister = new JochreIndexTermLister(docId, indexSearcher);
		return termLister;
	}
	
	public SearchServiceInternal getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchServiceInternal searchService) {
		this.searchService = searchService;
	}
}
