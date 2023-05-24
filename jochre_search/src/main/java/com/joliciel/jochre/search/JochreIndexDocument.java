package com.joliciel.jochre.search;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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

import com.fasterxml.jackson.core.JsonGenerator;
import com.joliciel.jochre.search.alto.AltoPage;
import com.joliciel.jochre.search.alto.AltoString;
import com.joliciel.jochre.search.alto.AltoTextBlock;
import com.joliciel.jochre.search.alto.AltoTextLine;
import com.joliciel.jochre.search.feedback.Correction;
import com.joliciel.jochre.utils.JochreException;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * A wrapper for a document in the Lucene sense - a single book could be split
 * into multiple documents, each with a different page range.
 * 
 * The document is either read from the index, or it is read from a set of Alto
 * pages to be saved in the index.
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
  private final String id;
  private final String publisher;
  private final String author;
  private final String title;
  private final String url;
  private final String authorEnglish;
  private final String titleEnglish;
  private final String date;
  private final String volume;
  private final int startPage;
  private final int endPage;
  private final JochreIndexDirectory directory;
  private final int length;
  private Long indexTime = null;
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

  /**
   * Construct a document already stored in the index.
   * @throws IOException
   */
  public JochreIndexDocument(IndexSearcher indexSearcher, int docId, String configId) throws IOException {
    this.config = JochreSearchConfig.getInstance(configId);
    this.indexSearcher = indexSearcher;
    this.docId = docId;

    this.doc = indexSearcher.doc(docId);
    this.id = doc.get(JochreIndexField.id.name());
    this.name = doc.get(JochreIndexField.name.name());
    this.path = this.doc.get(JochreIndexField.path.name());
    this.sectionNumber = Integer.parseInt(doc.getField(JochreIndexField.sectionNumber.name()).stringValue());
    File dir = new File(config.getContentDir(), this.path);
    this.directory = new JochreIndexDirectory(dir, configId);

    this.startPage = doc.getField(JochreIndexField.startPage.name()).numericValue().intValue();
    this.endPage = doc.getField(JochreIndexField.endPage.name()).numericValue().intValue();
    this.publisher = doc.get(JochreIndexField.publisher.name());
    this.author = doc.get(JochreIndexField.author.name());
    this.title = doc.get(JochreIndexField.title.name());
    this.url = doc.get(JochreIndexField.url.name());
    this.authorEnglish = doc.get(JochreIndexField.authorEnglish.name());
    this.titleEnglish = doc.get(JochreIndexField.titleEnglish.name());
    this.date = doc.get(JochreIndexField.date.name());
    this.volume = doc.get(JochreIndexField.volume.name());
    this.length = doc.getField(JochreIndexField.length.name()).numericValue().intValue();
  }

  /**
   * Construct a document from a list of Alto pages, to be stored in the index.
   */
  public JochreIndexDocument(JochreIndexDirectory directory, int index, List<AltoPage> pages,
      Map<JochreIndexField, List<Correction>> correctionMap, String configId) {
    this.config = JochreSearchConfig.getInstance(configId);
    this.directory = directory;
    this.sectionNumber = index;
    this.name = this.directory.getName();
    this.path = directory.getPath();
    // Apply corrections if applicable
    Map<JochreIndexField, String> metaData = this.directory.getMetaData();
    for (JochreIndexField field : correctionMap.keySet()) {
      List<Correction> corrections = correctionMap.get(field);
      for (Correction correction : corrections) {
        correction.apply(this.path, metaData);
      }
    }
    this.id = metaData.get(JochreIndexField.id);
    this.publisher = metaData.get(JochreIndexField.publisher);
    this.author = metaData.get(JochreIndexField.author);
    this.title = metaData.get(JochreIndexField.title);
    this.url = metaData.get(JochreIndexField.url);
    this.authorEnglish = metaData.get(JochreIndexField.authorEnglish);
    this.titleEnglish = metaData.get(JochreIndexField.titleEnglish);
    this.date = metaData.get(JochreIndexField.date);
    this.volume = metaData.get(JochreIndexField.volume);

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

      if (this.id != null)
        doc.add(new StringField(JochreIndexField.id.name(), this.id, Field.Store.YES));
      if (this.authorEnglish != null)
        doc.add(new TextField(JochreIndexField.authorEnglish.name(), this.authorEnglish, Field.Store.YES));
      if (this.titleEnglish != null)
        doc.add(new Field(JochreIndexField.titleEnglish.name(), this.titleEnglish, TYPE_STORED));
      if (this.publisher != null)
        doc.add(new TextField(JochreIndexField.publisher.name(), this.publisher, Field.Store.YES));
      if (this.date != null) {
        doc.add(new StringField(JochreIndexField.date.name(), this.date, Field.Store.YES));
        try {
          doc.add(new IntPoint(JochreIndexField.year.name(), Integer.parseInt(this.date)));
          doc.add(new SortedNumericDocValuesField(JochreIndexField.yearSort.name(), Integer.parseInt(this.date)));
        } catch (NumberFormatException nfe) {
          // not a number, oh well
        }
      }
      if (this.author != null)
        doc.add(new TextField(JochreIndexField.author.name(), this.author, Field.Store.YES));
      if (this.title != null)
        doc.add(new Field(JochreIndexField.title.name(), this.title, TYPE_STORED));
      if (this.volume != null)
        doc.add(new StringField(JochreIndexField.volume.name(), this.volume, Field.Store.YES));

      if (this.url != null)
        doc.add(new StringField(JochreIndexField.url.name(), this.url, Field.Store.YES));

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

  public void toJson(JsonGenerator jsonGen) throws IOException {
    jsonGen.writeStartObject();
    jsonGen.writeNumberField("docId", docId);
    jsonGen.writeStringField(JochreIndexField.name.name(), this.name);
    jsonGen.writeNumberField(JochreIndexField.startPage.name(), this.startPage);
    jsonGen.writeNumberField(JochreIndexField.endPage.name(), this.endPage);
    jsonGen.writeNumberField(JochreIndexField.sectionNumber.name(), this.sectionNumber);
    jsonGen.writeStringField(JochreIndexField.path.name(), this.path);
    if (this.id != null)
      jsonGen.writeStringField(JochreIndexField.id.name(), this.id);
    if (this.authorEnglish != null)
      jsonGen.writeStringField(JochreIndexField.authorEnglish.name(), this.authorEnglish);
    if (this.titleEnglish != null)
      jsonGen.writeStringField(JochreIndexField.titleEnglish.name(), titleEnglish);
    if (this.url != null)
      jsonGen.writeStringField(JochreIndexField.url.name(), this.url);
    if (this.author != null)
      jsonGen.writeStringField(JochreIndexField.author.name(), this.author);
    if (this.title != null)
      jsonGen.writeStringField(JochreIndexField.title.name(), this.title);
    if (this.volume != null)
      jsonGen.writeStringField(JochreIndexField.volume.name(), this.volume);
    if (this.publisher != null)
      jsonGen.writeStringField(JochreIndexField.publisher.name(), this.publisher);
    if (this.date != null)
      jsonGen.writeStringField(JochreIndexField.date.name(), this.date);
    jsonGen.writeNumberField(JochreIndexField.length.name(), this.length);
    if (this.indexTime != null)
      jsonGen.writeNumberField(JochreIndexField.indexTime.name(), this.indexTime.longValue());

    jsonGen.writeEndObject();
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
        throw new IndexFieldNotFoundException("No rectangle found for " + fieldName + " in document "
            + this.doc.get(JochreIndexField.name.name()) + ", pages " + this.doc.get(JochreIndexField.startPage.name())
            + " to " + this.doc.get(JochreIndexField.endPage.name()) + " (docId=" + this.docId + ")");
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
        throw new IndexFieldNotFoundException("No start index found for " + fieldName + " in document "
            + this.doc.get(JochreIndexField.name.name()) + ", pages " + this.doc.get(JochreIndexField.startPage.name())
            + " to " + this.doc.get(JochreIndexField.endPage.name()));
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
        throw new IndexFieldNotFoundException("NorowCount found for " + fieldName + " in document "
            + this.doc.get(JochreIndexField.name.name()) + ", pages " + this.doc.get(JochreIndexField.startPage.name())
            + " to " + this.doc.get(JochreIndexField.endPage.name()));
      }
      rowCount = rowCountObj.intValue();
    }
    return rowCount;
  }

  /**
   * The index of the first page contained in this document.
   */
  public int getStartPage() {
    return startPage;
  }

  /**
   * The index of the last page contained in this document.
   */
  public int getEndPage() {
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

  @Override
  public String toString() {
    return "JochreIndexDocument [sectionNumber=" + sectionNumber + ", path=" + path + ", name=" + name + ", startPage="
        + startPage + ", endPage=" + endPage + ", length=" + length + ", docId=" + docId + "]";
  }

}
