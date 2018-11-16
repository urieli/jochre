package com.joliciel.jochre.search;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochreIndexTermLister.JochreTerm;
import com.joliciel.jochre.utils.JochreException;

/**
 * A single word found within a {@link JochreIndexDocument} at a given offset,
 * used to retrieve the word's attributes such as its text, its rectangle or its
 * image.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreIndexWord {
  private static final Logger LOG = LoggerFactory.getLogger(JochreIndexWord.class);
  private final int startOffset;
  private final JochreIndexDocument doc;
  private final JochreTerm jochreTerm;
  private final JochreSearchConfig config;

  private Rectangle rectangle;
  private Rectangle secondRectangle;
  private String text;
  private BufferedImage image;
  private Rectangle rowRectangle;
  private BufferedImage rowImage;
  private Rectangle secondRowRectangle;
  private BufferedImage secondRowImage;

  public JochreIndexWord(JochreIndexDocument doc, int startOffset, JochreSearchConfig config, JochreIndexTermLister termLister) throws IOException {
    this.doc = doc;
    this.config = config;
    NavigableMap<Integer, JochreTerm> termMap = termLister.getTextTermByOffset();

    jochreTerm = termMap.floorEntry(startOffset).getValue();
    if (jochreTerm == null)
      throw new JochreException("No term found at startoffset " + startOffset + ", in doc " + doc.getName() + ", section " + doc.getSectionNumber());

    if (LOG.isTraceEnabled()) {
      LOG.trace(jochreTerm.toString());
      SortedMap<Integer, JochreTerm> ascendingMap = termMap.tailMap(startOffset);
      Iterator<Integer> ascendingKeys = ascendingMap.keySet().iterator();
      for (int i = 0; i < 5; i++) {
        if (ascendingKeys.hasNext()) {
          int key = ascendingKeys.next();
          LOG.trace(termMap.get(key).toString());
        }
      }
    }
    this.startOffset = jochreTerm.getStart();
  }

  /**
   * The word's start offset.
   */
  public int getStartOffset() {
    return startOffset;
  }

  /**
   * The document containing this word.
   */
  public JochreIndexDocument getDocument() {
    return doc;
  }

  /**
   * The rectangle surrounding this word within the page.
   */
  public Rectangle getRectangle() {
    if (rectangle == null)
      rectangle = jochreTerm.getPayload().getRectangle();
    return rectangle;
  }

  /**
   * The second rectangle for this word, when it is a hyphenated word split across
   * two rows.
   */
  public Rectangle getSecondRectangle() {
    if (secondRectangle == null)
      secondRectangle = jochreTerm.getPayload().getSecondaryRectangle();
    return secondRectangle;
  }

  /**
   * The word's text.
   */
  public String getText() {
    if (this.text == null)
      text = doc.getContents().substring(jochreTerm.getStart(), jochreTerm.getEnd());
    return text;
  }

  /**
   * The word's image - if it is a hyphenated word, the image includes both
   * halves.
   */
  public BufferedImage getImage() {
    this.getImages();
    return image;
  }

  /**
   * The rectangle of the row containing this word, within the page.
   */
  public Rectangle getRowRectangle() {
    if (rowRectangle == null) {
      rowRectangle = doc.getRowRectangle(jochreTerm.getPayload().getPageIndex(), jochreTerm.getPayload().getRowIndex());
    }
    return rowRectangle;
  }

  /**
   * The image of the row containing this word.
   */
  public BufferedImage getRowImage() {
    this.getImages();
    return rowImage;
  }

  /**
   * This 2nd row's rectangle within the page, when the word is a hyphenated word
   * split across two rows.
   */
  public Rectangle getSecondRowRectangle() {
    if (secondRowRectangle == null) {
      if (jochreTerm.getPayload().getSecondaryRectangle() != null) {
        secondRowRectangle = doc.getRowRectangle(jochreTerm.getPayload().getPageIndex(), jochreTerm.getPayload().getRowIndex() + 1);
      }
    }
    return secondRowRectangle;
  }

  /**
   * This 2nd row's image, when the word is a hyphenated word split across two
   * rows.
   */
  public BufferedImage getSecondRowImage() {
    this.getImages();
    return secondRowImage;
  }

  private void getImages() {
    if (this.image == null) {
      int pageIndex = jochreTerm.getPayload().getPageIndex();
      BufferedImage originalImage = doc.getImage(pageIndex);
      Rectangle rect = jochreTerm.getPayload().getRectangle();
      image = originalImage.getSubimage(rect.x, rect.y, rect.width, rect.height);

      Rectangle secondaryRect = jochreTerm.getPayload().getSecondaryRectangle();
      if (secondaryRect != null) {
        BufferedImage secondSnippet = originalImage.getSubimage(secondaryRect.x, secondaryRect.y, secondaryRect.width, secondaryRect.height);
        if (config.isLeftToRight())
          image = joinBufferedImage(image, secondSnippet);
        else
          image = joinBufferedImage(secondSnippet, image);
      }

      Rectangle rowRect = this.getRowRectangle();
      rowImage = originalImage.getSubimage(rowRect.x, rowRect.y, rowRect.width, rowRect.height);

      Rectangle secondRowRect = this.getSecondRectangle();
      if (secondRowRect != null) {
        secondRowImage = originalImage.getSubimage(secondRowRect.x, secondRowRect.y, secondRowRect.width, secondRowRect.height);
      }
    }
  }

  /**
   * Page index for this word.
   */
  public int getPageIndex() {
    return this.jochreTerm.getPayload().getPageIndex();
  }

  /**
   * From http://stackoverflow.com/questions/20826216/copy-two-buffered-image-
   * into-one-image-side-by-side
   */
  public static BufferedImage joinBufferedImage(BufferedImage img1, BufferedImage img2) {
    // do some calculations first
    int offset = 5;
    int wid = img1.getWidth() + img2.getWidth() + offset;
    int height = Math.max(img1.getHeight(), img2.getHeight()) + offset;
    // create a new buffer and draw two images into the new image
    BufferedImage newImage = new BufferedImage(wid, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = newImage.createGraphics();
    Color oldColor = g2.getColor();
    // fill background
    g2.setPaint(Color.WHITE);
    g2.fillRect(0, 0, wid, height);
    // draw image
    g2.setColor(oldColor);
    g2.drawImage(img1, null, 0, 0);
    g2.drawImage(img2, null, img1.getWidth() + offset, 0);
    g2.dispose();
    return newImage;
  }
}
