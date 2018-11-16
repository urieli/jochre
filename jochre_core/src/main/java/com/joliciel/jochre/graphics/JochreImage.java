///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.jochre.graphics;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.Entity;
import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.doc.DocumentDao;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.util.ImagePixelGrabber;
import com.joliciel.jochre.graphics.util.ImagePixelGrabberImpl;
import com.joliciel.jochre.security.SecurityDao;
import com.joliciel.jochre.security.User;
import com.joliciel.talismane.utils.Monitorable;
import com.joliciel.talismane.utils.ProgressMonitor;
import com.joliciel.talismane.utils.SimpleProgressMonitor;

/**
 * A representation of an image that contains the results of segmentataion
 * analysis. In other words, the image has been broken up into paragraphs, rows,
 * groups (words) and shapes (letters).
 * 
 * @author Assaf Urieli
 *
 */
public class JochreImage implements Entity, ImageGrid, Monitorable {
  private static final Logger LOG = LoggerFactory.getLogger(JochreImage.class);
  private int id;
  int blackThreshold;
  int separationThreshold;
  String name;
  int width;
  int height;
  List<Paragraph> paragraphs;
  int pageId;
  JochrePage page;
  int index;
  int whiteLimit;
  int blackLimit;
  private int ownerId;
  private User owner;

  int[] normalizedBrightnessValues;
  int whiteGapFillFactor;
  double averageRowHeight = 0;
  boolean originalImageChanged = false;
  private BufferedImage originalImage = null;
  int shapeCount = -1;
  private ImagePixelGrabber pixelGrabber;

  private double confidence = -1;
  ImageStatus imageStatus;

  private Map<String, Shape> shapeMap = null;
  SimpleProgressMonitor currentMonitor = null;
  int shapesSaved = 0;

  private boolean meanHorizontalSlopeCalculated = false;
  private double meanHorizontalSlope = 0;

  private final JochreSession jochreSession;
  private final GraphicsDao graphicsDao;

  JochreImage(JochreSession jochreSession) {
    this.jochreSession = jochreSession;
    this.graphicsDao = GraphicsDao.getInstance(jochreSession);
  }

  public JochreImage(BufferedImage originalImage, JochreSession jochreSession) {
    this(jochreSession);
    this.originalImage = originalImage;
  }

  ImagePixelGrabber getPixelGrabber() {
    if (this.pixelGrabber == null) {
      this.pixelGrabber = new ImagePixelGrabberImpl(this.getOriginalImage());
    }
    return this.pixelGrabber;
  }

  @Override
  public int getAbsolutePixel(int x, int y) {
    int brightness = this.getRawAbsolutePixel(x, y);
    // brightness now gives us a greyscale value
    // all we need to do is normalise it
    return this.normalize(brightness);
  }

  @Override
  public int getPixel(int x, int y) {
    return this.getAbsolutePixel(x, y);
  }

  @Override
  public int getRawPixel(int x, int y) {
    return this.getRawAbsolutePixel(x, y);
  }

  @Override
  public int getRawAbsolutePixel(int x, int y) {
    return this.getPixelGrabber().getPixelBrightness(x, y);
  }

  @Override
  public boolean isPixelBlack(int x, int y, int threshold) {
    if (x < 0 || y < 0 || x >= this.getWidth() || y >= this.getHeight())
      return false;

    if (this.getPixel(x, y) <= threshold)
      return true;
    else
      return false;
  }

  /**
   * A black brightness threshold, used for analysing the contents of a shape
   * as black-or-white bits in order to recognise the letter. Anything &lt;=
   * the black threshold should be considered black when analysing a letter.
   */
  public int getBlackThreshold() {
    return blackThreshold;
  }

  public void setBlackThreshold(int blackThreshold) {
    this.blackThreshold = blackThreshold;
  }

  /**
   * The name given to this image.
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Image width.
   */
  @Override
  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  /**
   * Image height.
   */
  @Override
  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  /**
   * The paragraphs contained in this image.
   */
  public List<Paragraph> getParagraphs() {
    if (paragraphs == null) {
      if (this.id == 0)
        paragraphs = new ArrayList<>();
      else
        paragraphs = graphicsDao.findParagraphs(this);
    }
    return paragraphs;
  }

  @Override
  public void save() {
    if (this.currentMonitor != null)
      this.currentMonitor.setCurrentAction("imageMonitor.savingImage");
    if (this.pageId == 0 && this.page != null)
      this.pageId = this.page.getId();
    graphicsDao.saveJochreImage(this);
    if (this.paragraphs != null) {
      int index = 0;
      for (Paragraph paragraph : this.paragraphs) {
        paragraph.setIndex(index++);
        paragraph.save();
      }
    }

    if (this.originalImageChanged) {
      graphicsDao.saveOriginalImage(this);
    }
  }

  public Paragraph newParagraph() {
    Paragraph paragraph = new Paragraph(jochreSession);
    this.getParagraphs().add(paragraph);
    paragraph.setImage(this);
    return paragraph;
  }

  public int getPageId() {
    return pageId;
  }

  public void setPageId(int pageId) {
    this.pageId = pageId;
  }

  /**
   * The page containing this image (in multi-page documents).
   */
  public JochrePage getPage() {
    if (this.page == null && this.pageId != 0) {
      DocumentDao documentDao = DocumentDao.getInstance(jochreSession);
      this.page = documentDao.loadJochrePage(this.pageId);
    }
    return page;
  }

  void setPage(JochrePage page) {
    this.page = page;
    this.pageId = page.getId();
  }

  /**
   * The index of this image on the page.
   */
  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  /**
   * The separation threshold, used for analysing the image as black-or-white
   * bits to determine where letters separate. Anything &lt;= the separation
   * threshold should be considered black when separating letters.
   */
  public int getSeparationThreshold() {
    return separationThreshold;
  }

  public void setSeparationThreshold(int separationThreshold) {
    this.separationThreshold = separationThreshold;
  }

  /**
   * The brightness limit above which pixels are considered to be pure white.
   * Used to normalise brightness on the image.
   */
  public int getWhiteLimit() {
    return whiteLimit;
  }

  public void setWhiteLimit(int whiteLimit) {
    this.whiteLimit = whiteLimit;
  }

  /**
   * The brightness limit below which pixels are considered to be pure black.
   * Used to normalise brightness on the image.
   */
  public int getBlackLimit() {
    return blackLimit;
  }

  public void setBlackLimit(int blackLimit) {
    this.blackLimit = blackLimit;
  }

  /**
   * Returns the normalized brightness value corresponding to this brightness,
   * taking into account the black limit and white limit.
   */
  public final int normalize(int brightness) {
    if (normalizedBrightnessValues == null) {
      normalizedBrightnessValues = new int[256];
      double greyscaleMultiplier = (255.0 / (whiteLimit - blackLimit));
      for (int i = 0; i < 256; i++) {
        if (i < blackLimit)
          normalizedBrightnessValues[i] = 0;
        if (i > whiteLimit)
          normalizedBrightnessValues[i] = 255;
        normalizedBrightnessValues[i] = (int) Math.round((i - blackLimit) * greyscaleMultiplier);
      }
    }

    return normalizedBrightnessValues[brightness];
  }

  /**
   * Fill factor for gaps that were mistakenly left empty when converting a
   * grayscale image to black and white. Defaults to 0, but should be set
   * higher if many white gaps appear in image.
   */
  public int getWhiteGapFillFactor() {
    return whiteGapFillFactor;
  }

  public void setWhiteGapFillFactor(int whiteGapFillFactor) {
    this.whiteGapFillFactor = whiteGapFillFactor;
  }

  /**
   * This image's status.
   */
  public ImageStatus getImageStatus() {
    return imageStatus;
  }

  public void setImageStatus(ImageStatus imageStatus) {
    this.imageStatus = imageStatus;
  }

  /**
   * Clears out objects in memory to avoid filling it up.
   */
  public void clearMemory() {
    this.paragraphs = null;
    this.originalImage = null;
    this.pixelGrabber = null;
    this.shapeMap = null;
    System.gc();
  }

  public void recalculate() {
    this.averageRowHeight = 0;
  }

  /**
   * The average row height for this shape, from meanline to baseline.
   */
  public double getAverageRowHeight() {
    if (averageRowHeight == 0) {
      DescriptiveStatistics rowHeightStats = new DescriptiveStatistics();
      for (Paragraph paragraph : this.getParagraphs()) {
        for (RowOfShapes row : paragraph.getRows()) {
          int height = row.getXHeight();
          rowHeightStats.addValue(height);
        }
      }
      averageRowHeight = rowHeightStats.getPercentile(50);
      LOG.debug("averageRowHeight: " + averageRowHeight);
    }
    return averageRowHeight;
  }

  /**
   * The image on which this SourceImage was built.
   */
  public BufferedImage getOriginalImage() {
    if (this.originalImage == null) {
      graphicsDao.loadOriginalImage(this);
    }
    return originalImage;
  }

  public void setOriginalImage(BufferedImage originalImage) {
    this.originalImage = originalImage;
    originalImageChanged = true;
  }

  void setOriginalImageDB(BufferedImage originalImage) {
    this.originalImage = originalImage;
  }

  /**
   * The number of shapes in this image.
   */
  public int getShapeCount() {
    if (shapeCount < 0) {
      shapeCount = graphicsDao.getShapeCount(this);
    }
    return shapeCount;
  }

  public void setShapeCount(int shapeCount) {
    this.shapeCount = shapeCount;
  }

  @Override
  public int hashCode() {
    if (this.id == 0)
      return super.hashCode();
    else
      return ((Integer) this.getId()).hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this.id == 0) {
      return super.equals(obj);
    } else {
      JochreImage other = (JochreImage) obj;
      return (this.getId() == other.getId());
    }
  }

  public int getOwnerId() {
    return ownerId;
  }

  void setOwnerId(int ownerId) {
    this.ownerId = ownerId;
    this.owner = null;
  }

  /**
   * The image's current owner, among the Jochre users.
   */
  public User getOwner() {
    if (this.owner == null && this.ownerId != 0) {
      SecurityDao securityDao = SecurityDao.getInstance(jochreSession);
      this.owner = securityDao.loadUser(this.ownerId);
    }
    return owner;
  }

  public void setOwner(User owner) {
    this.setOwnerId(owner.getId());
    this.owner = owner;
  }

  /**
   * Returns a shape with the coordinates provided.
   */
  public Shape getShape(int left, int top, int right, int bottom) {
    String key = left + "," + top + "," + right + "," + bottom;

    if (this.shapeMap == null)
      this.shapeMap = new HashMap<>();
    Shape shape = this.shapeMap.get(key);
    if (shape == null) {
      shape = new Shape(this, jochreSession);
      shape.setLeft(left);
      shape.setTop(top);
      shape.setRight(right);
      shape.setBottom(bottom);
      this.shapeMap.put(key, shape);
    }
    return shape;
  }

  /**
   * Is this containing document's locale left-to-right or right-to-left
   * (ignoring top-to-bottom for now!)
   */
  public boolean isLeftToRight() {
    if (this.getPage() != null && this.getPage().getDocument() != null)
      return this.getPage().getDocument().isLeftToRight();
    return jochreSession.getLinguistics().isLeftToRight();
  }

  @Override
  public ProgressMonitor monitorTask() {
    currentMonitor = new SimpleProgressMonitor();
    shapesSaved = 0;
    return currentMonitor;
  }

  /**
   * Called whenever a shape is finished saving.
   */

  void onSaveShape(Shape shape) {
    if (this.currentMonitor != null) {
      shapesSaved++;
      int shapeCount = this.getShapeCount();
      if (shapeCount == 0)
        shapeCount = 1;
      this.currentMonitor.setPercentComplete((double) shapesSaved / (double) shapeCount);
    }
  }

  /**
   * The average confidence of the current page, in a scale from 0 to 1.
   */
  public double getConfidence() {
    if (confidence < 0) {
      confidence = 0;
      int count = 0;
      for (Paragraph paragraph : paragraphs) {
        for (RowOfShapes row : paragraph.getRows()) {
          for (GroupOfShapes group : row.getGroups()) {
            count++;
            confidence += Math.log(group.getConfidence());
          }
        }
      }
      if (count == 0) {
        confidence = 0;
      } else {
        confidence /= count;
        confidence = Math.exp(confidence);
      }
    }
    return confidence;
  }

  /**
   * A rectangle containing all of this images paragraphs.
   */
  public Rectangle getPrintSpace() {
    RectangleImpl printSpace = new RectangleImpl(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    for (Paragraph paragraph : this.getParagraphs()) {
      if (!paragraph.isJunk()) {
        if (paragraph.getTop() < printSpace.getTop())
          printSpace.setTop(paragraph.getTop());
        if (paragraph.getLeft() < printSpace.getLeft())
          printSpace.setLeft(paragraph.getLeft());
        if (paragraph.getBottom() > printSpace.getBottom())
          printSpace.setBottom(paragraph.getBottom());
        if (paragraph.getRight() > printSpace.getRight())
          printSpace.setRight(paragraph.getRight());
      }
    }
    return printSpace;
  }

  /**
   * Recalculate all indexes on this image.
   */
  public void recalculateIndexes() {
    int iPar = 0;
    for (Paragraph par : this.getParagraphs()) {
      par.setIndex(iPar++);
      int iRow = 0;
      for (RowOfShapes row : par.getRows()) {
        row.setIndex(iRow++);
        int iGroup = 0;
        for (GroupOfShapes group : row.getGroups()) {
          group.setIndex(iGroup++);
          int iShape = 0;
          for (Shape shape : group.getShapes()) {
            shape.setIndex(iShape++);
          }
        }
      }
    }
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  /**
   * Calculate the mean horizontal slope of rows on this image.
   */
  public double getMeanHorizontalSlope() {
    if (!meanHorizontalSlopeCalculated) {
      Mean meanForSlope = new Mean();
      for (Paragraph par : this.getParagraphs()) {
        for (RowOfShapes row : par.getRows()) {
          if (row.getShapes().size() > 1) {
            Shape shape1 = this.isLeftToRight() ? row.getShapes().get(0) : row.getShapes().get(row.getShapes().size() - 1);
            Shape shape2 = this.isLeftToRight() ? row.getShapes().get(row.getShapes().size() - 1) : row.getShapes().get(0);
            double slope = ((double) ((shape2.getTop() + shape2.getBaseLine()) - (shape1.getTop() + shape1.getBaseLine())))
                / ((double) (shape2.getRight() - shape1.getLeft()));
            meanForSlope.increment(slope);
          }
        }
      }
      if (meanForSlope.getN() > 0)
        meanHorizontalSlope = meanForSlope.getResult();
      else
        meanHorizontalSlope = 0;
      meanHorizontalSlopeCalculated = true;
    }
    return meanHorizontalSlope;
  }

  /**
   * Mean horizontal slope in degrees.
   */
  public double getMeanSlopeDegrees() {
    return Math.toDegrees(Math.atan(0 - this.getMeanHorizontalSlope()));
  }
}
