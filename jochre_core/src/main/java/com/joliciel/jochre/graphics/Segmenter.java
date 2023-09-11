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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.stats.CardinalityComparator;
import com.joliciel.jochre.stats.DBSCANClusterer;
import com.joliciel.jochre.stats.MeanAbsoluteDeviation;
import com.joliciel.talismane.utils.Monitorable;
import com.joliciel.talismane.utils.ProgressMonitor;
import com.joliciel.talismane.utils.SimpleProgressMonitor;
import com.typesafe.config.Config;

/**
 * Takes a SourceImage and converts it into an JochreImage, segmented into
 * ordered Rows, Groups and Shapes. The Locale matters for the ordering (e.g.
 * right-to-left or left-to-write).
 * 
 * @author Assaf Urieli
 *
 */
public class Segmenter implements Monitorable {
  private static final Logger LOG = LoggerFactory.getLogger(Segmenter.class);
  private boolean drawSegmentation = false;
  private BufferedImage segmentedImage = null;
  private Graphics2D graphics2D = null;
  private SimpleProgressMonitor currentMonitor;
  private boolean splitAndJoin = false;

  private final SourceImage sourceImage;
  private final JochreSession jochreSession;
  private final int maxShapeStackSize;

  private final boolean clean;

  public Segmenter(SourceImage sourceImage, JochreSession jochreSession) {
    this.sourceImage = sourceImage;
    this.jochreSession = jochreSession;
    Config segmenterConfig = jochreSession.getConfig().getConfig("jochre.segmenter");
    drawSegmentation = segmenterConfig.getBoolean("draw-segmented-image");
    maxShapeStackSize = segmenterConfig.getInt("max-shape-stack-size");
    clean = segmenterConfig.getBoolean("is-clean-segment");
  }

  /**
   * Divide an image up into rows, groups and shapes (corresponding to rows,
   * words and letters).
   */

  public void segment() {
    LOG.debug("########## segment #########");

    if (currentMonitor != null) {
      currentMonitor.setCurrentAction("imageMonitor.findingShapes");
    }
    Set<Shape> shapes = this.findContiguousShapes(sourceImage);
    if (this.isDrawSegmentation()) {
      segmentedImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
      graphics2D = segmentedImage.createGraphics();
      graphics2D.drawImage(sourceImage.getOriginalImage(), 0, 0, sourceImage.getWidth(), sourceImage.getHeight(), null);
    }

    this.removeSpecks(sourceImage, shapes);

    if (!clean) {
      this.removeOversizedShapes(shapes);
    }

    if (currentMonitor != null) {
      currentMonitor.setCurrentAction("imageMonitor.groupingShapesIntoRows");
      currentMonitor.setPercentComplete(0.2);
    }
    List<Rectangle> whiteAreas = sourceImage.getWhiteAreas(shapes);

    // if (this.drawSegmentation) {
    // graphics2D.setStroke(new BasicStroke(1));
    // graphics2D.setPaint(Color.ORANGE);
    // for (Rectangle whiteArea : whiteAreas) {
    // graphics2D.drawRect(whiteArea.getLeft(), whiteArea.getTop(),
    // whiteArea.getRight() - whiteArea.getLeft(),
    // whiteArea.getBottom()-whiteArea.getTop());
    // }
    // }

    // first we group shapes into rows based on white areas which don't rely
    // on knowledge of page slope
    // having the rows allows us to estimate page slope
    List<RowOfShapes> rows = this.groupShapesIntoRows(sourceImage, shapes, whiteAreas, false);

    this.addRowsToJochreImage(sourceImage, rows);

    this.findGuideLines(sourceImage);

    List<Rectangle> columnSeparators;
    if (!clean) {
      columnSeparators = sourceImage.findColumnSeparators();
      if (this.drawSegmentation) {
        graphics2D.setStroke(new BasicStroke(3));
        graphics2D.setPaint(Color.ORANGE);
        for (Rectangle whiteArea : columnSeparators) {
          int topLeft = (int) Math.round(whiteArea.getLeft() + sourceImage.getXAdjustment(whiteArea.getTop())) + 3;
          int bottomLeft = (int) Math.round(whiteArea.getLeft() + sourceImage.getXAdjustment(whiteArea.getBottom())) + 3;
          int topRight = (int) Math.round(whiteArea.getRight() + sourceImage.getXAdjustment(whiteArea.getTop())) - 3;
          int bottomRight = (int) Math.round(whiteArea.getRight() + sourceImage.getXAdjustment(whiteArea.getBottom())) - 3;
          graphics2D.drawLine(topLeft, whiteArea.getTop() + 3, bottomLeft, whiteArea.getBottom() - 3);
          graphics2D.drawLine(topRight, whiteArea.getTop() + 3, bottomRight, whiteArea.getBottom() - 3);
          graphics2D.drawLine(topLeft, whiteArea.getTop() + 3, topRight, whiteArea.getTop() + 3);
          graphics2D.drawLine(bottomLeft, whiteArea.getBottom() - 3, bottomRight, whiteArea.getBottom() - 3);
        }
      }

      // now we re-do the grouping of shapes into rows, this time with proper
      // column breaks to avoid
      // rows that cross-over columns
      rows = this.groupShapesIntoRows(sourceImage, shapes, columnSeparators, true);

      this.addRowsToJochreImage(sourceImage, rows);

      this.findGuideLines(sourceImage);
    } else {
      columnSeparators = new ArrayList<>();
    }

    this.splitRows(sourceImage);

    if (this.splitAndJoin) {
      // figure out if the shapes contain a lot of "holes"
      // if they do, join them together
      // if they don't, try to split them
      int fillFactor = this.getFillFactor(sourceImage);
      if (fillFactor >= 2) {
        this.joinShapesHorizontally(sourceImage);
      }

      if (currentMonitor != null) {
        currentMonitor.setCurrentAction("imageMonitor.splittingShapes");
        currentMonitor.setPercentComplete(0.4);
      }
      this.splitShapes(sourceImage, fillFactor);
    }

    // this.removeSpecks(sourceImage);

    this.joinShapesVertically(sourceImage);

    this.findGuideLines(sourceImage);
    this.combineRowsVertically(sourceImage);

    this.removeOrphans(sourceImage, false);

    this.removeFalseColumns(sourceImage, columnSeparators);

    if (currentMonitor != null) {
      currentMonitor.setCurrentAction("imageMonitor.groupingShapesIntoWords");
      currentMonitor.setPercentComplete(0.6);
    }
    this.groupShapesIntoWords(sourceImage);

    this.removeOrphans(sourceImage, true);

    if (!clean) {
      this.cleanMargins(sourceImage);
    }

    if (currentMonitor != null) {
      currentMonitor.setCurrentAction("imageMonitor.analysingFontSize");
      currentMonitor.setPercentComplete(0.7);
    }
    this.splitRowsByFontSize(sourceImage);

    if (currentMonitor != null) {
      currentMonitor.setCurrentAction("imageMonitor.groupingRowsIntoParagraphs");
      currentMonitor.setPercentComplete(0.9);
    }
    this.groupRowsIntoParagraphs(sourceImage);
    sourceImage.recalculateIndexes();

    sourceImage.setShapeCount(this.getShapeCount(sourceImage));

    if (this.isDrawSegmentation()) {
      this.drawSegmentation(sourceImage);
    }

    if (currentMonitor != null) {
      currentMonitor.setFinished(true);
    }
  }

  /**
   * Split rows if they're particularly high, and contain considerable white
   * space in the middle. Shapes causing the join will be removed if too high,
   * or attached to the closest row otherwise.
   */
  void splitRows(SourceImage sourceImage) {
    LOG.debug("########## splitRows #########");

    // Calculate the min row height to be considered for splitting
    double minHeightForSplit = sourceImage.getAverageShapeHeight();
    LOG.debug("minHeightForSplit: " + minHeightForSplit);

    double slopeMean = sourceImage.getMeanHorizontalSlope();

    List<RowOfShapes> candidateRows = new ArrayList<RowOfShapes>();
    for (RowOfShapes row : sourceImage.getRows()) {
      if (row.getRight() == row.getLeft())
        continue;
      int height = row.getBottom() - row.getTop();
      if (height >= minHeightForSplit) {
        LOG.debug("Adding candidate " + row.toString());
        candidateRows.add(row);
      }
    }

    // For each row to be considered for splitting, see if there are lines
    // of white space inside it.
    Hashtable<RowOfShapes, List<RowOfShapes>> splitRows = new Hashtable<RowOfShapes, List<RowOfShapes>>();
    for (RowOfShapes row : candidateRows) {
      SimpleRegression regression = new SimpleRegression();
      // y = intercept + slope * x
      LOG.debug("Left point: (" + row.getLeft() + " , " + row.getTop() + ")");
      regression.addData(row.getLeft(), row.getTop());
      double rightHandY = row.getTop() + ((row.getRight() - row.getLeft()) * slopeMean);
      LOG.debug("Right point: (" + row.getRight() + " , " + rightHandY + ")");
      regression.addData(row.getRight(), rightHandY);

      int yDelta = (int) Math.ceil(Math.abs(rightHandY - row.getTop()));
      int yInterval = yDelta + (row.getBottom() - row.getTop() + 1) + yDelta;

      LOG.debug("yDelta: " + yDelta);
      LOG.debug("yInterval: " + yInterval);
      // let's get pixel counts shape by shape, and leave out the rest (in
      // case rows overlap vertically)
      int[] pixelCounts = new int[yInterval];
      for (Shape shape : row.getShapes()) {
        LOG.trace("Shape " + shape);
        int yDeltaAtLeft = (int) Math.round(regression.predict(shape.getLeft()));
        LOG.trace("yDeltaAtLeft: " + yDeltaAtLeft);
        // the shape offset + the offset between the regression line and
        // the row top
        // + the delta we left at the start in case the line slopes
        // upwards to the right
        int topIndex = (shape.getTop() - row.getTop()) + (row.getTop() - yDeltaAtLeft) + yDelta;
        LOG.trace("topIndex: (" + shape.getTop() + " - " + row.getTop() + ") + (" + row.getTop() + " - " + yDeltaAtLeft + ") + " + yDelta + " = "
            + topIndex);
        for (int x = 0; x < shape.getWidth(); x++) {
          for (int y = 0; y < shape.getHeight(); y++) {
            if (shape.isPixelBlack(x, y, sourceImage.getBlackThreshold())) {
              pixelCounts[topIndex + y]++;
            }
          }
        }
      }

      Mean pixelCountMean = new Mean();
      StandardDeviation pixelCountStdDev = new StandardDeviation();
      for (int i = 0; i < yInterval; i++) {
        LOG.debug("Pixel count " + i + ": " + pixelCounts[i]);
        pixelCountMean.increment(pixelCounts[i]);
        pixelCountStdDev.increment(pixelCounts[i]);
      }
      LOG.debug("pixel count mean: " + pixelCountMean.getResult() + ", std dev: " + pixelCountStdDev.getResult());

      // If there's a split required, we're going to go considerably above
      // and below the mean several times
      double lowThreshold = pixelCountMean.getResult() / 2.0;
      double highThreshold = pixelCountMean.getResult() * 2.0;
      boolean inRow = false;
      List<Integer> switches = new ArrayList<Integer>();
      for (int i = 0; i < yInterval; i++) {
        if (!inRow && pixelCounts[i] > highThreshold) {
          LOG.debug("In row at " + i + ", pixel count " + pixelCounts[i]);
          inRow = true;
          switches.add(i);
        } else if (inRow && pixelCounts[i] < lowThreshold) {
          LOG.debug("Out of row at " + i + ", pixel count " + pixelCounts[i]);
          inRow = false;
          switches.add(i);
        }
      }
      if (switches.size() > 2) {
        // we have more than one row
        List<Integer> rowSeparations = new ArrayList<Integer>();

        // find the row separators
        for (int switchIndex = 1; switchIndex < switches.size() - 2; switchIndex = switchIndex + 2) {
          int outOfRow = switches.get(switchIndex);
          int intoRow = switches.get(switchIndex + 1);
          int minPixelCount = (int) Math.ceil(highThreshold);
          int minIndex = -1;
          // find the row with the lowest pixel count
          for (int i = outOfRow; i <= intoRow; i++) {
            if (pixelCounts[i] < minPixelCount) {
              minPixelCount = pixelCounts[i];
              minIndex = i;
            }
          }
          rowSeparations.add(minIndex);
        }

        // separate the shapes among the rows
        List<RowOfShapes> newRows = new ArrayList<RowOfShapes>(rowSeparations.size() + 1);
        for (int i = 0; i <= rowSeparations.size(); i++) {
          newRows.add(new RowOfShapes(sourceImage, jochreSession));
        }

        // add a separator at the beginning and end
        rowSeparations.add(0, 0);
        rowSeparations.add(yInterval + 1);
        for (Shape shape : row.getShapes()) {
          int yDeltaAtLeft = (int) Math.round(regression.predict(shape.getLeft()));
          int topIndex = (shape.getTop() - row.getTop()) + (row.getTop() - yDeltaAtLeft) + yDelta;
          int firstSepAfterShapeBottom = rowSeparations.size();
          int lastSepBeforeShapeTop = -1;

          for (int i = rowSeparations.size() - 1; i >= 0; i--) {
            int rowSeparation = rowSeparations.get(i);
            if (rowSeparation <= topIndex) {
              lastSepBeforeShapeTop = i;
              break;
            }
          }

          for (int i = 0; i < rowSeparations.size(); i++) {
            int rowSeparation = rowSeparations.get(i);
            if (rowSeparation >= topIndex + shape.getHeight()) {
              firstSepAfterShapeBottom = i;
              break;
            }
          }

          if (lastSepBeforeShapeTop == firstSepAfterShapeBottom - 1) {
            // shape clearly belongs to one row
            RowOfShapes newRow = newRows.get(lastSepBeforeShapeTop);
            newRow.addShape(shape);
          } else {
            // is the shape much closer to one row than another?
            // if yes, add it to then add it to this row
            int[] yPixelsPerRow = new int[newRows.size()];
            for (int i = 0; i < newRows.size(); i++) {
              int separatorTop = rowSeparations.get(i);
              int separatorBottom = rowSeparations.get(i + 1);
              int top = topIndex < separatorTop ? separatorTop : topIndex;
              int bottom = topIndex + shape.getHeight() < separatorBottom ? topIndex + shape.getHeight() : separatorBottom;
              yPixelsPerRow[i] = bottom - top;
            }

            int pixelsInMaxRow = 0;
            int maxPixelRowIndex = -1;
            for (int i = 0; i < newRows.size(); i++) {
              if (yPixelsPerRow[i] > pixelsInMaxRow) {
                pixelsInMaxRow = yPixelsPerRow[i];
                maxPixelRowIndex = i;
              }
            }
            double minPercentage = 0.8;
            if (((double) pixelsInMaxRow / (double) shape.getHeight()) >= minPercentage) {
              RowOfShapes newRow = newRows.get(maxPixelRowIndex);
              newRow.addShape(shape);
            } else {
              // otherwise, the shape needs to be got rid of
              // as it's causing massive confusion
              // do this by simply not adding it anywhere
            }
          } // is the shape in one row exactly?
        } // next shape
        splitRows.put(row, newRows);
      } // do we have more than one row?
    } // next row

    for (RowOfShapes row : splitRows.keySet()) {
      List<RowOfShapes> newRows = splitRows.get(row);
      sourceImage.replaceRow(row, newRows);
    }
  }

  void removeOversizedShapes(Set<Shape> shapes) {
    LOG.debug("########## removeOversizedShapes #########");
    Mean shapeHeightMean = new Mean();
    Mean shapeWidthMean = new Mean();

    for (Shape shape : shapes) {
      shapeHeightMean.increment(shape.getHeight());
      shapeWidthMean.increment(shape.getWidth());
    }

    double heightMean = shapeHeightMean.getResult();
    double widthMean = shapeWidthMean.getResult();
    LOG.debug("heightMean: " + heightMean);
    LOG.debug("widthMean: " + widthMean);

    shapeHeightMean = new Mean();
    shapeWidthMean = new Mean();
    StandardDeviation shapeHeightStdDev = new StandardDeviation();
    for (Shape shape : shapes) {
      if (shape.getHeight() > heightMean && shape.getHeight() < (heightMean * 2.0) && shape.getWidth() > widthMean
          && shape.getWidth() < (widthMean * 2.0)) {
        shapeHeightMean.increment(shape.getHeight());
        shapeHeightStdDev.increment(shape.getHeight());
        shapeWidthMean.increment(shape.getWidth());
      }
    }

    heightMean = shapeHeightMean.getResult();
    widthMean = shapeWidthMean.getResult();
    LOG.debug("average shape heightMean: " + heightMean);
    LOG.debug("average shape widthMean: " + widthMean);

    double minHeightBigShape = heightMean * 6;
    double minWidthWideShape = widthMean * 6;
    double minHeightWideShape = heightMean * 1.5;
    double minHeightTallShape = heightMean * 2.5;
    double maxWidthTallShape = widthMean / 2;
    LOG.debug("minHeightBigShape: " + minHeightBigShape);
    LOG.debug("minWidthWideShape: " + minWidthWideShape);
    LOG.debug("minHeightWideShape: " + minHeightWideShape);
    LOG.debug("minHeightTallShape: " + minHeightTallShape);
    LOG.debug("maxWidthTallShape: " + maxWidthTallShape);

    List<Shape> largeShapes = new ArrayList<Shape>();
    List<Shape> horizontalRules = new ArrayList<Shape>();
    for (Shape shape : shapes) {
      if (shape.getHeight() > minHeightBigShape) {
        LOG.debug("Removing " + shape + " (height)");
        largeShapes.add(shape);
      } else if (shape.getWidth() > minWidthWideShape && shape.getHeight() > minHeightWideShape) {
        // we don't want to remove horizontal bars, but we do want to
        // remove other shapes.
        // why not? I suppose horizontal bars are easily represented as
        // characters?
        LOG.debug("Removing " + shape + " (width)");
        largeShapes.add(shape);
      } else if (shape.getWidth() > minWidthWideShape) {
        // ok, we will remove horizontal rules after all
        LOG.debug("Removing " + shape + " (horizontal rule)");
        largeShapes.add(shape);
        horizontalRules.add(shape);
      } else if (shape.getWidth() <= maxWidthTallShape && shape.getHeight() > minHeightTallShape) {
        LOG.debug("Removing " + shape + " (narrow)");
        largeShapes.add(shape);
      }
    }

    // Only want to remove enclosed shapes if the large shape isn't a
    // frame/grid
    // A) first reduce the shape by 5 percent and see it's cardinality
    // reduces vastly (in which case it's a frame)
    // if so, don't remove enclosed shapes
    // B) next, detect white rectangles within the shape - if they're big
    // enough, don't remove enclosed shapes LOG.debug("Are large shapes
    // frames or illustrations?");
    double maxFrameCardinalityRatio = 0.5;
    double minFrameWhiteAreaSizeRatio = 0.9;
    List<Shape> illustrations = new ArrayList<Shape>(largeShapes);
    for (Shape largeShape : largeShapes) {
      LOG.debug(largeShape.toString());
      int xOrigin = largeShape.getStartingPoint()[0] - largeShape.getLeft();
      int yOrigin = largeShape.getStartingPoint()[1] - largeShape.getTop();

      // We want to fill up a mirror of the contiguous pixels within this
      // shape,
      // which is what we'll use for further analysis to know
      // if it's a frame or not.
      WritableImageGrid mirror = new ImageMirror(largeShape);
      Shape dummyShape = new Shape(sourceImage, xOrigin, yOrigin, jochreSession);
      this.getShape(largeShape, dummyShape, mirror, xOrigin, yOrigin, sourceImage.getSeparationThreshold());

      int adjustedLeft = (int) Math.round(mirror.getWidth() * 0.05);
      int adjustedRight = (int) Math.round(mirror.getWidth() * 0.95);
      int adjustedTop = (int) Math.round(mirror.getHeight() * 0.05);
      int adjustedBottom = (int) Math.round(mirror.getHeight() * 0.95);

      int cardinality = 0;
      int innerCardinality = 0;
      for (int x = 0; x < mirror.getWidth(); x++) {
        for (int y = 0; y < mirror.getHeight(); y++) {
          if (mirror.getPixel(x, y) > 0) {
            cardinality++;
            if (x >= adjustedLeft && x <= adjustedRight && y >= adjustedTop && y <= adjustedBottom)
              innerCardinality++;
          }
        }
      }

      LOG.debug("cardinality: " + cardinality);
      LOG.debug("innerCardinality: " + innerCardinality);
      double ratio = (double) innerCardinality / (double) cardinality;
      LOG.debug("ratio: " + ratio);
      if (ratio <= maxFrameCardinalityRatio) {
        LOG.debug("maxFrameCardinalityRatio: " + maxFrameCardinalityRatio);
        LOG.debug("Frame by cardinality! Removing from illustrations");
        illustrations.remove(largeShape);
      } else {
        // Now, it could still be a grid
        // to find this out we need to detect white areas inside the
        // shape.
        WhiteAreaFinder whiteAreaFinder = new WhiteAreaFinder();
        double minWhiteAreaWidth = widthMean * 10;
        double minWhiteAreaHeight = heightMean * 4;
        List<Rectangle> whiteAreas = whiteAreaFinder.getWhiteAreas(mirror, 0, 0, 0, mirror.getWidth() - 1, mirror.getHeight() - 1, minWhiteAreaWidth,
            minWhiteAreaHeight);
        int whiteAreaSize = 0;
        for (Rectangle whiteArea : whiteAreas) {
          whiteAreaSize += (whiteArea.getWidth() * whiteArea.getHeight());
        }

        int totalSize = mirror.getWidth() * mirror.getHeight();
        LOG.debug("whiteAreaSize: " + whiteAreaSize);
        LOG.debug("totalSize: " + totalSize);

        double sizeRatio = (double) whiteAreaSize / (double) totalSize;
        LOG.debug("sizeRatio: " + sizeRatio);

        if (sizeRatio >= minFrameWhiteAreaSizeRatio) {
          LOG.debug("minFrameWhiteAreaSizeRatio: " + minFrameWhiteAreaSizeRatio);
          LOG.debug("Frame by white area size! Removing from illustrations");
          illustrations.remove(largeShape);
        }

      }
    }

    for (Shape largeShape : illustrations) {
      // Add this to large shapes if it's not a "frame"
      // large shapes are used for paragraph detection
      sourceImage.getLargeShapes().add(largeShape);
    }

    // remove shapes that are enclosed inside illustrations
    List<Shape> enclosedShapesToDelete = new ArrayList<Shape>();
    int extension = 5;
    for (Shape shape : shapes) {
      for (Shape shapeToDelete : illustrations) {
        if (shape.getLeft() >= shapeToDelete.getLeft() - extension && shape.getRight() <= shapeToDelete.getRight() + extension
            && shape.getTop() >= shapeToDelete.getTop() - extension && shape.getBottom() <= shapeToDelete.getBottom() + extension) {
          LOG.debug("Enclosed shape: " + shape);
          LOG.debug(" enclosed by " + shapeToDelete);
          enclosedShapesToDelete.add(shape);
        }
      }
    }

    // TODO: too long with two lists, need to replace shapes with Set to
    // ease finding
    shapes.removeAll(largeShapes);
    shapes.removeAll(enclosedShapesToDelete);

    // remove shapes that are practically touching horizontal rules
    // (probably segments of the rule that got split)
    extension = 3;
    List<Shape> listToTestAgainst = horizontalRules;
    for (int i = 0; i < 3; i++) {
      List<Shape> horizontalRuleSegments = new ArrayList<Shape>();
      for (Shape horizontalRule : listToTestAgainst) {
        for (Shape shape : shapes) {
          if ((shape.getLeft() <= horizontalRule.getRight() + extension || shape.getRight() >= horizontalRule.getLeft() - extension)
              && shape.getTop() >= horizontalRule.getTop() - extension && shape.getBottom() <= horizontalRule.getBottom() + extension) {
            LOG.debug("Horizontal rule segment: " + shape);
            LOG.debug(" touching " + horizontalRule);
            horizontalRuleSegments.add(shape);
            enclosedShapesToDelete.add(shape);
          }
        }
      }
      shapes.removeAll(horizontalRuleSegments);
      listToTestAgainst = horizontalRuleSegments;
      if (listToTestAgainst.size() == 0)
        break;
    }

  }

  /**
   * If any two shapes in the same line are only separated by a thin line,
   * join them together
   */
  void joinShapesHorizontally(SourceImage sourceImage) {
    LOG.debug("########## joinShapesHorizontally #########");
    for (RowOfShapes row : sourceImage.getRows()) {
      this.joinShapesHorizontally(row);
    } // next row
  }

  void joinShapesHorizontally(RowOfShapes row) {
    LOG.debug("joinShapesHorizontally Row " + row.getIndex());
    List<Shape> shapesToDelete = new ArrayList<Shape>();
    int threshold = 2;
    int maxPreviousShapes = 4;
    List<Shape> previousShapes = new ArrayList<Shape>();
    for (Shape shape : row.getShapes()) {
      if (LOG.isTraceEnabled())
        LOG.trace(shape.toString());
      for (Shape previousShape : previousShapes) {
        int space = 0;
        if (sourceImage.isLeftToRight())
          space = shape.getLeft() - previousShape.getRight();
        else
          space = previousShape.getLeft() - shape.getRight();
        if (LOG.isTraceEnabled()) {
          LOG.trace("previousShape: " + previousShape);
          LOG.trace("Space : " + space);
        }
        int singleShapeThresholdWidth = (int) Math.round(sourceImage.getAverageShapeWidth() * 1.5);
        if (space <= threshold && previousShape.getTop() <= shape.getBottom() && previousShape.getBottom() >= shape.getTop()
            && (shape.getWidth() + previousShape.getWidth() <= singleShapeThresholdWidth)) {
          // check that the two shapes have dark areas near each other
          List<Integer> shape1BorderPoints = new ArrayList<Integer>();
          int shape1MinBorder = sourceImage.isLeftToRight() ? previousShape.getWidth() - threshold : 0;
          int shape1MaxBorder = sourceImage.isLeftToRight() ? previousShape.getWidth() : threshold;

          if (LOG.isTraceEnabled()) {
            LOG.trace("Candidate.");
            LOG.trace("shape1MinBorder" + shape1MinBorder);
            LOG.trace("shape1MaxBorder" + shape1MaxBorder);
          }
          StringBuilder sb = new StringBuilder();

          for (int x = shape1MinBorder; x < shape1MaxBorder; x++) {
            for (int y = 0; y < previousShape.getHeight(); y++) {
              if (previousShape.isPixelBlack(x, y, sourceImage.getBlackThreshold())) {
                shape1BorderPoints.add(previousShape.getTop() + y);
                sb.append(previousShape.getTop() + y);
                sb.append(',');
              }
            }
          }
          if (LOG.isTraceEnabled())
            LOG.trace(sb.toString());
          List<Integer> shape2BorderPoints = new ArrayList<Integer>();
          sb = new StringBuilder();
          int shape2MinBorder = sourceImage.isLeftToRight() ? 0 : shape.getWidth() - threshold;
          int shape2MaxBorder = sourceImage.isLeftToRight() ? threshold : shape.getWidth();
          if (LOG.isTraceEnabled()) {
            LOG.trace("shape2MinBorder" + shape2MinBorder);
            LOG.trace("shape2MaxBorder" + shape2MaxBorder);
          }
          for (int x = shape2MinBorder; x < shape2MaxBorder; x++) {
            for (int y = 0; y < shape.getHeight(); y++) {
              if (shape.isPixelBlack(x, y, sourceImage.getBlackThreshold())) {
                shape2BorderPoints.add(shape.getTop() + y);
                sb.append(shape.getTop() + y);
                sb.append(',');
              }
            }
          }
          LOG.trace(sb.toString());
          boolean haveNeighbour = false;
          for (int shape1BorderPoint : shape1BorderPoints) {
            for (int shape2BorderPoint : shape2BorderPoints) {
              if (Math.abs(shape2BorderPoint - shape1BorderPoint) <= threshold) {
                LOG.trace("haveNeighbour");
                haveNeighbour = true;
                break;
              }
            }
            if (haveNeighbour)
              break;
          }

          if (haveNeighbour) {
            LOG.debug("Combining " + shape);
            LOG.debug(" with " + previousShape);
            int minLeft = previousShape.getLeft() <= shape.getLeft() ? previousShape.getLeft() : shape.getLeft();
            int maxRight = previousShape.getRight() >= shape.getRight() ? previousShape.getRight() : shape.getRight();
            int minTop = previousShape.getTop() <= shape.getTop() ? previousShape.getTop() : shape.getTop();
            int maxBottom = previousShape.getBottom() >= shape.getBottom() ? previousShape.getBottom() : shape.getBottom();

            shape.setLeft(minLeft);
            shape.setTop(minTop);
            shape.setRight(maxRight);
            shape.setBottom(maxBottom);

            shapesToDelete.add(previousShape);
          }
        }
      }
      previousShapes.add(shape);
      if (previousShapes.size() > maxPreviousShapes)
        previousShapes.remove(0);
    } // next shape

    if (shapesToDelete.size() > 0) {
      for (Shape shapeToDelete : shapesToDelete)
        row.removeShape(shapeToDelete);
      row.recalculate();
    }
  }

  private int getFillFactor(SourceImage sourceImage) {
    LOG.debug("########## getFillFactor #########");
    List<Shape> sample = this.getSample(sourceImage.getRows(), 40, true);
    Mean mean = new Mean();
    ShapeFiller shapeFiller = new ShapeFiller();
    for (Shape shape : sample) {
      LOG.debug("Shape: " + shape);
      int fillFactor = shapeFiller.getFillFactor(shape, sourceImage.getBlackThreshold());
      LOG.debug("fillFactor: " + fillFactor);
      mean.increment(fillFactor);
    }
    double meanFillFactor = mean.getResult();
    LOG.debug("meanFillFactor: " + meanFillFactor);
    int imageFillFactor = (int) Math.round(mean.getResult());
    LOG.debug("imageFillFactor: " + imageFillFactor);
    return imageFillFactor;
  }

  /**
   * Get all contiguous shapes out of the image grid.
   */
  Set<Shape> findContiguousShapes(SourceImage sourceImage) {
    LOG.debug("########## findContiguousShapes #########");
    // As we get them out of the image grid, we write them to a writeable
    // grid so as to avoid duplicate extraction
    WritableImageGrid mirror = new ImageMirror(sourceImage);
    Set<Shape> shapes = new TreeSet<Shape>(new ShapeTopToBottomComparator());

    for (int y = 0; y < sourceImage.getHeight(); y++) {
      for (int x = 0; x < sourceImage.getWidth(); x++) {
        if (sourceImage.isPixelBlack(x, y, sourceImage.getSeparationThreshold())) {
          // if this pixel has already been found, ignore it
          if (mirror.getPixel(x, y) > 0)
            continue;

          // get the shape surrounding this pixel
          Shape shape = this.getShape(sourceImage, mirror, x, y);
          shapes.add(shape);
          // List<Shape> splitShapes = this.getShapes(sourceImage,
          // mirror, x, y);
          // shapes.addAll(splitShapes);
        }
      }
    }
    return shapes;
  }

  List<RowOfShapes> groupShapesIntoRows(SourceImage sourceImage, Set<Shape> shapes, List<Rectangle> whiteAreas, boolean useSlope) {
    LOG.debug("########## groupShapesIntoRows #########");
    LOG.debug("useSlope? " + useSlope);

    List<RowOfShapes> rows = new ArrayList<RowOfShapes>();
    for (Shape shape : shapes)
      shape.setRow(null);

    List<Shape> shapesToRemove = new ArrayList<Shape>();
    for (Shape shape : shapes) {
      for (Rectangle whiteArea : whiteAreas) {
        double whiteAreaRight = whiteArea.getRight();
        double whiteAreaLeft = whiteArea.getLeft();
        if (useSlope) {
          double xAdjustment = sourceImage.getXAdjustment(shape.getTop());

          whiteAreaRight += xAdjustment;
          whiteAreaLeft += xAdjustment;
        }

        if (whiteAreaRight > shape.getRight() && whiteAreaLeft < shape.getLeft() && whiteArea.getTop() < shape.getTop()
            && whiteArea.getBottom() > shape.getBottom()) {
          // shape is surrounded
          shapesToRemove.add(shape);
          LOG.debug("Removing shape " + shape);
          LOG.debug("Surrounded by white area: " + whiteArea);
        }
      }
    }
    shapes.removeAll(shapesToRemove);

    // calculate the means
    // get average shape width & height
    DescriptiveStatistics shapeWidthStats = new DescriptiveStatistics();
    for (Shape shape : shapes) {
      shapeWidthStats.addValue(shape.getWidth());
    }
    double averageShapeWidth = shapeWidthStats.getPercentile(50);
    LOG.debug("averageShapeWidth: " + averageShapeWidth);

    // now, arrange the shapes in rows
    // we're guaranteed that no two shapes overlap at this point.
    // Now, it's possible that two shapes in the same line have no vertical
    // overlap (e.g. a comma and an apostrophe)
    // so we have to go searching a bit further afield, say five shapes in
    // each direction
    // but if we go too far, we may end up joining two lines together if the
    // page isn't quite straight

    // let's begin with any old shape and find the shapes closest to it
    // horizontally
    // e.g. up to 8 horizontal means to the right and left
    // as we find shapes that go with it, we add them to the same line
    int i = 0;
    int j = 0;
    int numberOfMeanWidthsForSearch = 8;
    LOG.debug("numberOfMeanWidthsForSearch: " + numberOfMeanWidthsForSearch);
    LOG.debug("search distance: " + averageShapeWidth * numberOfMeanWidthsForSearch);

    for (Shape shape : shapes) {
      if (shape.getRow() == null) {
        RowOfShapes row = new RowOfShapes(sourceImage, jochreSession);
        row.addShape(shape);
        row.setIndex(j++);
        rows.add(row);
        LOG.trace("========= New row " + row.getIndex() + "============");
        LOG.trace("Adding " + shape + " to row " + row.getIndex());
      }
      int searchLeft = (int) (shape.getLeft() - (numberOfMeanWidthsForSearch * averageShapeWidth));
      int searchRight = (int) (shape.getRight() + (numberOfMeanWidthsForSearch * averageShapeWidth));
      LOG.trace("Shape " + i++ + ": " + shape + "(row " + shape.getRow().getIndex() + ")");
      LOG.trace("searchLeft: " + searchLeft);
      LOG.trace("searchRight: " + searchRight);

      // construct an array to represent where white areas overlap with
      // the search area
      int[][] leftSearchArea = new int[shape.getLeft() - searchLeft][2];
      int[][] rightSearchArea = new int[searchRight - shape.getRight()][2];
      for (int k = 0; k < leftSearchArea.length; k++) {
        leftSearchArea[k][0] = shape.getTop();
        leftSearchArea[k][1] = shape.getBottom();
      }
      for (int k = 0; k < rightSearchArea.length; k++) {
        rightSearchArea[k][0] = shape.getTop();
        rightSearchArea[k][1] = shape.getBottom();
      }

      int newSearchLeft = searchLeft;
      int newSearchRight = searchRight;
      for (Rectangle whiteArea : whiteAreas) {
        double whiteAreaRight = whiteArea.getRight();
        double whiteAreaLeft = whiteArea.getLeft();
        if (useSlope) {
          double xAdjustment = sourceImage.getXAdjustment(shape.getTop());

          whiteAreaRight += xAdjustment;
          whiteAreaLeft += xAdjustment;
          LOG.trace(whiteArea + ", xAdjustment=" + xAdjustment + " , whiteAreaLeft=" + whiteAreaLeft + " , whiteAreaRight=" + whiteAreaRight);
        }

        if (whiteAreaRight > newSearchLeft && whiteAreaLeft < shape.getLeft() && whiteArea.getTop() <= shape.getBottom()
            && whiteArea.getBottom() >= shape.getTop()) {

          LOG.trace("overlap on left with: " + whiteArea.toString());

          if (whiteArea.getTop() <= shape.getTop() && whiteArea.getBottom() >= shape.getBottom() && whiteAreaRight > newSearchLeft) {
            newSearchLeft = (int) Math.round(whiteAreaRight);
            LOG.trace("Complete, newSearchLeft = " + newSearchLeft);
          } else {
            LOG.trace("Partial, starting at " + whiteArea.getRight());
            for (int k = whiteArea.getRight() - searchLeft; k >= 0; k--) {
              if (k < leftSearchArea.length) {
                if (whiteArea.getBottom() < shape.getBottom() && leftSearchArea[k][0] < whiteArea.getBottom())
                  leftSearchArea[k][0] = whiteArea.getBottom() + 1;
                else if (whiteArea.getTop() > shape.getTop() && leftSearchArea[k][1] > whiteArea.getTop())
                  leftSearchArea[k][1] = whiteArea.getTop() - 1;

                if (leftSearchArea[k][0] >= leftSearchArea[k][1] && searchLeft + k > newSearchLeft) {
                  newSearchLeft = searchLeft + k;
                  LOG.trace("Complete from " + newSearchLeft);
                  break;
                }
              }
            }
            // if (LOG.isTraceEnabled()) {
            // StringBuilder sb = new StringBuilder();
            // for (int k=0;k<leftSearchArea.length;k++) {
            // String top = "" +
            // (leftSearchArea[k][0]-shape.getTop());
            // sb.append(String.format("%1$#" + 3 + "s", top)+ ",");
            // }
            // LOG.trace(sb.toString());
            // sb = new StringBuilder();
            // for (int k=0;k<leftSearchArea.length;k++) {
            // String bottom = "" +
            // (leftSearchArea[k][1]-shape.getTop());
            // sb.append(String.format("%1$#" + 3 + "s", bottom)+
            // ",");
            // }
            // LOG.trace(sb.toString());
            // }
          }
        } else if (whiteAreaLeft < newSearchRight && whiteAreaRight > shape.getRight() && whiteArea.getTop() <= shape.getBottom()
            && whiteArea.getBottom() >= shape.getTop()) {
          LOG.trace("overlap on right with: " + whiteArea.toString());

          if (whiteArea.getTop() <= shape.getTop() && whiteArea.getBottom() >= shape.getBottom() && newSearchRight > whiteAreaLeft) {
            newSearchRight = (int) Math.round(whiteAreaLeft);
            LOG.trace("Complete, newSearchRight = " + newSearchRight);

          } else {
            LOG.trace("Partial, starting at " + whiteArea.getLeft());
            for (int k = whiteArea.getLeft() - shape.getRight(); k < rightSearchArea.length; k++) {
              if (k > 0 && k < leftSearchArea.length && k < rightSearchArea.length) {
                if (whiteArea.getBottom() < shape.getBottom() && leftSearchArea[k][0] < whiteArea.getBottom())
                  rightSearchArea[k][0] = whiteArea.getBottom() + 1;
                else if (whiteArea.getTop() > shape.getTop() && leftSearchArea[k][1] > whiteArea.getTop())
                  rightSearchArea[k][1] = whiteArea.getTop() - 1;

                if (rightSearchArea[k][0] >= rightSearchArea[k][1] && newSearchRight > shape.getRight() + k) {
                  newSearchRight = shape.getRight() + k;
                  LOG.trace("Complete from " + newSearchRight);
                  break;
                }
              }
            }
            // if (LOG.isTraceEnabled()) {
            // StringBuilder sb = new StringBuilder();
            // for (int k=0;k<rightSearchArea.length;k++) {
            // String top = "" +
            // (rightSearchArea[k][0]-shape.getTop());
            // sb.append(String.format("%1$#" + 3 + "s", top)+ ",");
            // }
            // LOG.trace(sb.toString());
            // sb = new StringBuilder();
            // for (int k=0;k<rightSearchArea.length;k++) {
            // String bottom = "" +
            // (rightSearchArea[k][1]-shape.getTop());
            // sb.append(String.format("%1$#" + 3 + "s", bottom)+
            // ",");
            // }
            // LOG.trace(sb.toString());
            // }
          }
        }
      }
      LOG.trace("searchLeft adjusted for white columns: " + newSearchLeft);
      LOG.trace("searchRight adjusted for white columns: " + newSearchRight);

      // min 10% overlap to assume same row
      double minOverlap = 0.10;

      for (Shape otherShape : shapes) {
        boolean haveSomeOverlap = false;
        if (!shape.getRow().equals(otherShape.getRow()) && !otherShape.equals(shape)) {

          // shapes are arranged from the top down
          if (otherShape.getTop() > shape.getBottom()) {
            break;
          }

          if (otherShape.getRight() > newSearchLeft && otherShape.getRight() < shape.getLeft() && otherShape.getTop() <= shape.getBottom()
              && otherShape.getBottom() >= shape.getTop()) {
            int k = otherShape.getRight() - searchLeft;
            if (otherShape.getTop() <= leftSearchArea[k][1] && otherShape.getBottom() >= leftSearchArea[k][0])
              haveSomeOverlap = true;
          } else if (otherShape.getLeft() < newSearchRight && otherShape.getLeft() > shape.getRight() && otherShape.getTop() <= shape.getBottom()
              && otherShape.getBottom() >= shape.getTop()) {
            int k = otherShape.getLeft() - shape.getRight();
            if (otherShape.getTop() <= rightSearchArea[k][1] && otherShape.getBottom() >= rightSearchArea[k][0])
              haveSomeOverlap = true;
          }
          if (haveSomeOverlap) {
            int overlap1 = shape.getBottom() - otherShape.getTop() + 1;
            int overlap2 = otherShape.getBottom() - shape.getTop() + 1;
            int overlap = overlap1 < overlap2 ? overlap1 : overlap2;
            boolean addShapeToRow = false;
            if ((((double) overlap / (double) shape.getHeight()) > minOverlap)
                || (((double) overlap / (double) otherShape.getHeight()) > minOverlap)) {
              addShapeToRow = true;
            }

            if (addShapeToRow) {
              LOG.debug("Adding " + otherShape + " to row " + shape.getRow().getIndex());
              if (otherShape.getRow() == null) {
                shape.getRow().addShape(otherShape);
              } else {
                // two rows need to be merged
                LOG.debug("========= Merge rows " + shape.getRow().getIndex() + " with " + otherShape.getRow().getIndex() + "==========");
                RowOfShapes otherRow = otherShape.getRow();
                shape.getRow().addShapes(otherRow.getShapes());
                rows.remove(otherRow);
              }
            }
          } // add shape to row ?
        } // should shape be considered?
      } // next other shape
    } // next shape

    return rows;
  }

  void addRowsToJochreImage(SourceImage sourceImage, List<RowOfShapes> rows) {
    LOG.debug("########## addRowsToJochreImage #########");

    sourceImage.getRows().clear();

    TreeSet<RowOfShapes> rowSet = new TreeSet<RowOfShapes>(new RowOfShapesVerticalLocationComparator());
    rowSet.addAll(rows);
    int i = 0;
    LOG.debug("====== Row list ========");
    for (RowOfShapes row : rowSet) {
      // order the shapes within the rows
      // here is where left-to-right or right-to-left matters
      row.reorderShapes();
      sourceImage.addRow(row);
      int oldIndex = row.getIndex();
      row.setIndex(i++);

      LOG.debug(row.toString() + " (old index = " + oldIndex + ")");
    }
  }

  /**
   * We attempt to remove specks, where a speck is defined as a relatively
   * small shape at a relatively large distance from other shapes.
   */
  void removeSpecks(SourceImage sourceImage, Set<Shape> shapes) {
    LOG.debug("########## removeSpecks #########");

    DescriptiveStatistics shapeWidthStats = new DescriptiveStatistics();
    DescriptiveStatistics shapeHeightStats = new DescriptiveStatistics();

    for (Shape shape : shapes) {
      shapeWidthStats.addValue(shape.getWidth());
      shapeHeightStats.addValue(shape.getHeight());
    }

    double shapeWidthMedian = shapeWidthStats.getPercentile(65);
    double shapeHeightMedian = shapeHeightStats.getPercentile(65);
    LOG.debug("meanShapeWidth: " + shapeWidthMedian);
    LOG.debug("meanShapeHeight: " + shapeHeightMedian);

    int maxSpeckHeightFloor = (int) Math.ceil(shapeHeightMedian / 6.0);
    int maxSpeckWidthFloor = (int) Math.ceil(shapeWidthMedian / 6.0);

    // set reasonable minimum values, in case page has a huge amount of dirt
    // that affected the median
    if (maxSpeckHeightFloor < 5)
      maxSpeckHeightFloor = 5;
    if (maxSpeckWidthFloor < 5)
      maxSpeckWidthFloor = 5;

    int maxSpeckHeightCeiling = maxSpeckHeightFloor * 2;
    int maxSpeckWidthCeiling = maxSpeckWidthFloor * 2;

    int speckXDistanceThresholdFloor = (int) Math.floor(shapeWidthMedian);
    int speckYDistanceThresholdFloor = (int) Math.floor(shapeHeightMedian / 4.0);
    int speckXDistanceThresholdCeiling = speckXDistanceThresholdFloor * 2;
    int speckYDistanceThresholdCeiling = speckYDistanceThresholdFloor * 2;

    LOG.debug("maxSpeckHeightFloor=" + maxSpeckHeightFloor);
    LOG.debug("maxSpeckWidthFloor=" + maxSpeckWidthFloor);
    LOG.debug("speckXDistanceThresholdFloor=" + speckXDistanceThresholdFloor);
    LOG.debug("speckYDistanceThresholdFloor=" + speckYDistanceThresholdFloor);
    LOG.debug("maxSpeckHeightCeiling=" + maxSpeckHeightCeiling);
    LOG.debug("maxSpeckWidthCeiling=" + maxSpeckWidthCeiling);
    LOG.debug("speckXDistanceThresholdCeiling=" + speckXDistanceThresholdCeiling);
    LOG.debug("speckYDistanceThresholdCeiling=" + speckYDistanceThresholdCeiling);

    List<Shape> specks = new ArrayList<Shape>();
    List<double[]> speckCoordinates = new ArrayList<double[]>();
    List<Shape> specksToRemove = new ArrayList<Shape>();

    for (Shape shape : shapes) {
      if (shape.getHeight() < maxSpeckHeightFloor && shape.getWidth() < maxSpeckWidthFloor) {
        specksToRemove.add(shape);
      } else if (shape.getHeight() < maxSpeckHeightCeiling && shape.getWidth() < maxSpeckWidthCeiling) {
        specks.add(shape);
        speckCoordinates.add(shape.getCentrePoint());
      }
    }

    // group the specks into clusters, which will be added or removed as a
    // whole
    // Note that a cluster could be a valid diacritic that's split into a
    // few specks
    // or just a bunch of specks off on their own
    DBSCANClusterer<Shape> clusterer = new DBSCANClusterer<Shape>(specks, speckCoordinates);
    Set<Set<Shape>> speckClusters = clusterer.cluster(speckXDistanceThresholdFloor, 2, true);
    for (Set<Shape> speckCluster : speckClusters) {
      // safeguard to remove huge clusters of specks
      if (speckCluster.size() > 20) {
        specksToRemove.addAll(speckCluster);
        continue;
      }

      int speckHeight = 0;
      int speckWidth = 0;
      int clusterTop = -1;
      int clusterBottom = -1;
      int clusterRight = -1;
      int clusterLeft = -1;
      for (Shape speck : speckCluster) {
        LOG.debug("Speck?, " + speck);
        if (speck.getWidth() > speckWidth)
          speckWidth = speck.getWidth();
        if (speck.getHeight() > speckHeight)
          speckHeight = speck.getHeight();

        if (clusterTop < 0 || speck.getTop() < clusterTop)
          clusterTop = speck.getTop();
        if (clusterLeft < 0 || speck.getLeft() < clusterLeft)
          clusterLeft = speck.getLeft();
        if (speck.getBottom() > clusterBottom)
          clusterBottom = speck.getBottom();
        if (speck.getRight() > clusterRight)
          clusterRight = speck.getRight();

      }

      boolean useWidth = speckWidth > speckHeight;
      double scale = 1.0;
      if (useWidth)
        scale = speckWidth < maxSpeckWidthFloor ? 0.0
            : (speckWidth > maxSpeckWidthCeiling ? 1.0 : ((double) speckWidth - maxSpeckWidthFloor) / (maxSpeckWidthCeiling - maxSpeckWidthFloor));
      else
        scale = speckHeight < maxSpeckHeightFloor ? 0.0
            : (speckHeight > maxSpeckHeightCeiling ? 1.0
                : ((double) speckHeight - maxSpeckHeightFloor) / (maxSpeckHeightCeiling - maxSpeckHeightFloor));

      int speckXDistanceThreshold = (int) Math
          .ceil(speckXDistanceThresholdFloor + scale * (speckXDistanceThresholdCeiling - speckXDistanceThresholdFloor));
      int speckYDistanceThreshold = (int) Math
          .ceil(speckYDistanceThresholdFloor + scale * (speckYDistanceThresholdCeiling - speckYDistanceThresholdFloor));

      LOG.debug("speckHeight=" + speckHeight);
      LOG.debug("speckWidth=" + speckWidth);
      LOG.debug("speckXDistanceThreshold=" + speckXDistanceThreshold);
      LOG.debug("speckYDistanceThreshold=" + speckYDistanceThreshold);

      Shape nearestShape = null;
      double minDistance = 0.0;
      int nearestShapeXDiff = 0;
      int nearestShapeYDiff = 0;

      for (Shape otherShape : shapes) {
        // limit to nearby shapes
        if (otherShape.getTop() > clusterBottom + speckYDistanceThreshold + 1)
          break;
        if (otherShape.getBottom() < clusterTop - speckYDistanceThreshold - 1)
          continue;
        if (otherShape.getRight() < clusterLeft - speckXDistanceThreshold - 1)
          continue;
        if (otherShape.getLeft() > clusterRight + speckXDistanceThreshold + 1)
          continue;

        // Note: tried !specks.contains(otherShape), but sometimes we
        // have a valid case
        // where a diacritic is "split" into two specks
        if (!specks.contains(otherShape)) {
          int xDiff = 0;
          int yDiff = 0;
          int leftDiff = 0;
          int rightDiff = 0;
          int topDiff = 0;
          int botDiff = 0;

          if (otherShape.getLeft() <= clusterRight && otherShape.getRight() >= clusterLeft) {
            xDiff = 0;
          } else {
            leftDiff = Math.abs(clusterLeft - otherShape.getRight());
            rightDiff = Math.abs(clusterRight - otherShape.getLeft());
            xDiff = (leftDiff < rightDiff) ? leftDiff : rightDiff;
          }

          if (otherShape.getTop() <= clusterBottom && otherShape.getBottom() >= clusterTop) {
            yDiff = 0;
          } else {
            int nearestTop = (otherShape.getTop() > otherShape.getTop() + otherShape.getMeanLine()) ? otherShape.getTop() + otherShape.getMeanLine()
                : otherShape.getTop();
            int nearestBot = (otherShape.getBottom() < otherShape.getTop() + otherShape.getBaseLine())
                ? otherShape.getTop() + otherShape.getBaseLine() : otherShape.getBottom();
            topDiff = Math.abs(clusterTop - nearestBot);
            botDiff = Math.abs(clusterBottom - nearestTop);
            yDiff = (topDiff < botDiff) ? topDiff : botDiff;
          }

          double distance = Math.sqrt((xDiff * xDiff) + (yDiff * yDiff));

          if (nearestShape == null || distance < minDistance) {
            nearestShape = otherShape;
            minDistance = distance;
            nearestShapeXDiff = xDiff;
            nearestShapeYDiff = yDiff;
            LOG.trace("leftDiff=" + leftDiff + ", rightDiff=" + rightDiff);
            LOG.trace("topDiff=" + topDiff + ", botDiff=" + botDiff);
          } // found closer shape?
        } // is this the speck?
      } // loop shapes around the reference shape

      if (nearestShape != null) {
        LOG.trace("Nearest shape, top(" + nearestShape.getTop() + ") " + "left(" + nearestShape.getLeft() + ") " + "bot(" + nearestShape.getBottom()
            + ") " + "right(" + nearestShape.getRight() + ")");
        LOG.trace("Distance=" + minDistance + ", xDiff=" + nearestShapeXDiff + ", yDiff=" + nearestShapeYDiff);
      }
      boolean removeSpecks = false;
      if (nearestShape == null)
        removeSpecks = true;
      else {
        // calculate the shortest distance from the nearest shape to the
        // speck cluster
        for (Shape speck : speckCluster) {
          int xDiff = 0;
          int yDiff = 0;
          int leftDiff = 0;
          int rightDiff = 0;
          int topDiff = 0;
          int botDiff = 0;

          if (nearestShape.getLeft() <= speck.getRight() && nearestShape.getRight() >= speck.getLeft()) {
            xDiff = 0;
          } else {
            leftDiff = Math.abs(speck.getLeft() - nearestShape.getRight());
            rightDiff = Math.abs(speck.getRight() - nearestShape.getLeft());
            xDiff = (leftDiff < rightDiff) ? leftDiff : rightDiff;
          }

          if (nearestShape.getTop() <= speck.getBottom() && nearestShape.getBottom() >= speck.getTop()) {
            yDiff = 0;
          } else {
            int nearestTop = (nearestShape.getTop() > nearestShape.getTop() + nearestShape.getMeanLine())
                ? nearestShape.getTop() + nearestShape.getMeanLine() : nearestShape.getTop();
            int nearestBot = (nearestShape.getBottom() < nearestShape.getTop() + nearestShape.getBaseLine())
                ? nearestShape.getTop() + nearestShape.getBaseLine() : nearestShape.getBottom();
            topDiff = Math.abs(speck.getTop() - nearestBot);
            botDiff = Math.abs(speck.getBottom() - nearestTop);
            yDiff = (topDiff < botDiff) ? topDiff : botDiff;
          }

          double distance = Math.sqrt((xDiff * xDiff) + (yDiff * yDiff));

          if (distance < minDistance) {
            minDistance = distance;
            nearestShapeXDiff = xDiff;
            nearestShapeYDiff = yDiff;
            LOG.debug("Found closer speck:");
            LOG.debug("leftDiff=" + leftDiff + ", rightDiff=" + rightDiff);
            LOG.debug("topDiff=" + topDiff + ", botDiff=" + botDiff);
          } // found closer shape?
        }
        // Then, for all of these specks, find the one that's closest to
        // the nearest non-speck
        // if this distance > threshold, get rid of all of 'em
        // otherwise, keep 'em all
        if (nearestShapeXDiff > speckXDistanceThreshold || nearestShapeYDiff > speckYDistanceThreshold)
          removeSpecks = true;
      }
      if (removeSpecks) {
        for (Shape otherSpeck : speckCluster) {
          LOG.debug("Removing speck " + otherSpeck);
          specksToRemove.add(otherSpeck);
        }
      }
    } // next speck

    shapes.removeAll(specksToRemove);
  }

  void removeOrphans(SourceImage sourceImage, boolean hasGroups) {
    LOG.debug("########## removeOrphans #########");

    LOG.debug("Average shape width" + sourceImage.getAverageShapeWidth());
    LOG.debug("Average shape height" + sourceImage.getAverageShapeHeight());

    int maxSpeckHeight = (int) Math.ceil(sourceImage.getAverageShapeHeight() / 6.0);
    int maxSpeckWidth = (int) Math.ceil(sourceImage.getAverageShapeWidth() / 6.0);

    LOG.debug("maxSpeckHeight: " + maxSpeckHeight);
    LOG.debug("maxSpeckWidth: " + maxSpeckWidth);

    int maxSpeckWidthAlone = (int) Math.ceil(sourceImage.getAverageShapeWidth() / 8.0);
    LOG.debug("maxSpeckWidthAlone: " + maxSpeckWidthAlone);

    Set<RowOfShapes> alteredRows = new HashSet<RowOfShapes>();
    Set<GroupOfShapes> alteredGroups = new HashSet<GroupOfShapes>();

    List<Shape> shapesToDelete = new ArrayList<Shape>();
    List<GroupOfShapes> groupsToDelete = new ArrayList<GroupOfShapes>();
    List<RowOfShapes> rowsToDelete = new ArrayList<RowOfShapes>();
    if (hasGroups) {
      for (RowOfShapes row : sourceImage.getRows()) {
        for (GroupOfShapes group : row.getGroups()) {
          for (Shape shape : group.getShapes()) {
            if ((shape.getWidth() < maxSpeckWidth && shape.getHeight() < maxSpeckHeight) || (shape.getWidth() < maxSpeckWidthAlone)) {
              LOG.debug("Removing shape: " + shape);

              shapesToDelete.add(shape);
              alteredRows.add(row);
              alteredGroups.add(group);
            }
          }
        }
      }
    } else {
      for (RowOfShapes row : sourceImage.getRows()) {
        for (Shape shape : row.getShapes()) {
          if ((shape.getWidth() < maxSpeckWidth && shape.getHeight() < maxSpeckHeight) || (shape.getWidth() < maxSpeckWidthAlone)) {
            LOG.debug("Removing shape: " + shape);
            shapesToDelete.add(shape);
            alteredRows.add(row);
          }
        }
      }
    }
    for (Shape shape : shapesToDelete) {
      if (!hasGroups) {
        RowOfShapes row = shape.getRow();
        row.getShapes().remove(shape);
        if (row.getShapes().size() == 0)
          rowsToDelete.add(row);
      } else {
        GroupOfShapes group = shape.getGroup();
        group.getShapes().remove(shape);
        if (group.getShapes().size() == 0)
          groupsToDelete.add(group);
      }
    }

    if (hasGroups) {
      int maxGroupSpeckHeight = (int) Math.ceil(sourceImage.getAverageShapeHeight() / 4.0);
      int maxGroupSpeckWidth = (int) Math.ceil(sourceImage.getAverageShapeWidth() / 4.0);

      LOG.debug("maxGroupSpeckHeight: " + maxGroupSpeckHeight);
      LOG.debug("maxGroupSpeckWidth: " + maxGroupSpeckHeight);
      for (RowOfShapes row : sourceImage.getRows()) {
        for (GroupOfShapes group : row.getGroups()) {
          boolean hasNonSpeck = false;
          for (Shape shape : group.getShapes()) {
            if (shape.getHeight() > maxGroupSpeckHeight || shape.getWidth() > maxGroupSpeckWidth) {
              hasNonSpeck = true;
              break;
            }
          }
          if (!hasNonSpeck) {
            LOG.debug("Removing group with shapes:");
            for (Shape shape : group.getShapes())
              LOG.debug("Shape: " + shape);
            group.getShapes().clear();
            groupsToDelete.add(group);
            alteredRows.add(row);
          }
        }
      }
      for (GroupOfShapes group : groupsToDelete) {
        RowOfShapes row = group.getRow();
        row.getGroups().remove(group);
        if (row.getGroups().size() == 0)
          rowsToDelete.add(row);
      }

      int minRowHeight = (int) Math.ceil(sourceImage.getAverageShapeHeight());
      int minRowWidth = (int) Math.ceil(sourceImage.getAverageShapeWidth());
      LOG.debug("minRowHeight: " + minRowHeight);
      LOG.debug("minRowWidth: " + minRowWidth);

      int minWideRowHeight = (int) Math.ceil(sourceImage.getAverageShapeHeight() * 0.75);
      int minWideRowWidth = (int) Math.ceil(sourceImage.getAverageShapeWidth() * 2.0);
      LOG.debug("minWideRowHeight: " + minWideRowHeight);
      LOG.debug("minWideRowWidth: " + minWideRowWidth);

      int maxRowSpeckHeight = (int) Math.ceil(sourceImage.getAverageShapeHeight() / 2.0);
      int maxRowSpeckWidth = (int) Math.ceil(sourceImage.getAverageShapeWidth() / 2.0);
      LOG.debug("maxRowSpeckHeight: " + maxGroupSpeckHeight);
      LOG.debug("maxRowSpeckWidth: " + maxGroupSpeckHeight);

      for (RowOfShapes row : sourceImage.getRows()) {
        if (row.getBottom() - row.getTop() < minRowHeight && row.getRight() - row.getLeft() < minRowWidth) {
          rowsToDelete.add(row);
        } else if (row.getBottom() - row.getTop() < minWideRowHeight && row.getRight() - row.getLeft() < minWideRowWidth) {
          rowsToDelete.add(row);
        } else {
          boolean hasNonSpeck = false;
          for (GroupOfShapes group : row.getGroups()) {
            for (Shape shape : group.getShapes()) {
              if (shape.getHeight() > maxRowSpeckHeight || shape.getWidth() > maxRowSpeckWidth) {
                hasNonSpeck = true;
                break;
              }
            }
            if (hasNonSpeck)
              break;
          }
          if (!hasNonSpeck) {
            rowsToDelete.add(row);
          }
        }
      }
    }

    for (RowOfShapes row : sourceImage.getRows()) {
      if (!hasGroups && row.getShapes().size() == 0) {
        rowsToDelete.add(row);
      }
    }

    for (RowOfShapes row : rowsToDelete) {
      LOG.debug("Removing row with shapes:");
      for (GroupOfShapes group : row.getGroups())
        for (Shape shape : group.getShapes())
          LOG.debug("Shape: " + shape);
      row.getGroups().clear();
      sourceImage.getRows().remove(row);
    }
    for (GroupOfShapes group : alteredGroups)
      group.recalculate();

    for (RowOfShapes row : alteredRows)
      row.recalculate();

    if (alteredRows.size() > 0 || alteredGroups.size() > 0)
      sourceImage.recalculate();
  }

  /**
   * If any two shapes in the same line take up the same horizontal space, we
   * can join them vertically
   */
  void joinShapesVertically(SourceImage sourceImage) {
    LOG.debug("########## joinShapesVertically #########");
    for (RowOfShapes row : sourceImage.getRows()) {
      this.joinShapesVertically(row);
    } // next row
  }

  /**
   * If any two shapes in this row take up the same horizontal space, we can
   * join them vertically
   */
  void joinShapesVertically(RowOfShapes row) {
    LOG.debug("joinShapesVertically Row " + row.getIndex());
    LOG.debug("Shape height: mean=" + sourceImage.getAverageShapeHeight() + ", stddev=" + sourceImage.getAverageShapeHeightMargin());
    LOG.debug("Shape width: mean=" + sourceImage.getAverageShapeWidth() + ", stddev=" + sourceImage.getAverageShapeWidthMargin());

    int maxSpeckHeight = (int) Math.ceil(sourceImage.getAverageShapeHeight() / 6.0);
    int maxSpeckWidth = (int) Math.ceil(sourceImage.getAverageShapeWidth() / 6.0);

    LOG.debug("maxSpeckHeight: " + maxSpeckHeight);
    LOG.debug("maxSpeckWidth: " + maxSpeckWidth);

    // remove shapes 5 times, in case we have multiple vertical overlaps
    for (int k = 0; k < 5; k++) {
      LOG.debug("k=" + k);
      List<Shape> shapesToDelete = new ArrayList<Shape>();
      int i = 0;
      for (Shape shape : row.getShapes()) {
        // LOG.debug("Checking " + shape);
        if (shape.getHeight() < maxSpeckHeight && shape.getWidth() < maxSpeckWidth) {
          // only join other shapes to normal height shapes, not to
          // specks
          i++;
          continue;
        }
        int j = 0;
        for (Shape otherShape : row.getShapes()) {
          if (j <= i) {
            j++;
            continue;
          }
          if (j > i + 6)
            break;
          // LOG.debug("Comparing to " + otherShape);
          if (otherShape.getLeft() <= shape.getRight() && otherShape.getRight() >= shape.getLeft()) {
            // LOG.debug("Found overlap between " + shape + " and "
            // + otherShape);
            // there is some overlap... how much?
            int maxLeft = otherShape.getLeft() >= shape.getLeft() ? otherShape.getLeft() : shape.getLeft();
            int minRight = otherShape.getRight() <= shape.getRight() ? otherShape.getRight() : shape.getRight();
            int intersection = (minRight - maxLeft + 1);
            int intersectionMultiplied = intersection * 4;
            if (intersectionMultiplied >= shape.getWidth() || intersectionMultiplied >= otherShape.getWidth()) {
              LOG.debug("Combining " + shape);
              LOG.debug(" with " + otherShape);
              int minLeft = otherShape.getLeft() <= shape.getLeft() ? otherShape.getLeft() : shape.getLeft();
              int maxRight = otherShape.getRight() >= shape.getRight() ? otherShape.getRight() : shape.getRight();
              int minTop = otherShape.getTop() <= shape.getTop() ? otherShape.getTop() : shape.getTop();
              int maxBottom = otherShape.getBottom() >= shape.getBottom() ? otherShape.getBottom() : shape.getBottom();

              shape.setLeft(minLeft);
              shape.setTop(minTop);
              shape.setRight(maxRight);
              shape.setBottom(maxBottom);
              shape.recalculate();

              shapesToDelete.add(otherShape);
            }

          } // there is a horizontal overlap
          j++;
        } // check following few shapes
        i++;
      } // next shape
      if (shapesToDelete.size() == 0)
        break;
      for (Shape shapeToDelete : shapesToDelete)
        row.removeShape(shapeToDelete);
    } // do the whole thing several times in a row
  }

  /**
   * Find the baseline, meanline and capline for each shape, based on other
   * shapes on the same row this is likely to depend on the alphabet, e.g. the
   * hebrew alphabet has no capline as such. Returns a List of
   * SimpleRegression representing the centerline for each of the rows.
   */
  void findGuideLines(SourceImage sourceImage) {
    LOG.debug("########## findGuideLines #########");
    for (RowOfShapes row : sourceImage.getRows()) {
      row.assignGuideLines();
    }
  }

  /**
   * If a row begins with a larger font and then suddenly reduces (e.g.
   * dictionary entries), we split it into two separate rows and recalculate
   * guidelines for each.
   */
  void splitRowsByFontSize(SourceImage sourceImage) {
    LOG.debug("########## splitRowsByFontSize #########");
    for (RowOfShapes row : sourceImage.getRows()) {
      row.splitByFontSize();
    }
  }

  /**
   * Combine rows that represent thin lines directly above or below another
   * row (e.g. diacritics)
   */
  void combineRowsVertically(SourceImage sourceImage) {
    LOG.debug("########## combineRows #########");
    // We thought of using row height, but mean row height is not a good
    // enough
    // indicator when there are title rows with very big characters.
    // Instead, we need to go with Distance between rows when compared to
    // mean - baseline
    // where distance between rows is measured between the tops and bottoms
    // of nearby shapes.

    int maxRowHeight = 0;
    for (RowOfShapes row : sourceImage.getRows()) {
      int rowHeight = row.getXHeightMax();
      if (rowHeight > maxRowHeight)
        maxRowHeight = rowHeight;
    }
    LOG.debug("maxRowHeight: " + maxRowHeight);

    TreeSet<RowOfShapes> rowSet = new TreeSet<RowOfShapes>(new RowOfShapesVerticalLocationComparator());
    rowSet.addAll(sourceImage.getRows());
    List<RowOfShapes> rows = new ArrayList<RowOfShapes>(rowSet);

    List<RowOfShapes> rowsToDelete = new ArrayList<RowOfShapes>();

    double maxShapeWidth = sourceImage.getAverageShapeWidth() * 8.0;
    LOG.debug("maxShapeWidth: " + maxShapeWidth);

    double maxRatioForCombine = 0.6;
    LOG.debug("maxRatioForCombine: " + maxRatioForCombine);

    int i = 0;
    while (i < rows.size()) {
      RowOfShapes currentRow = rows.get(i);
      boolean rowsCombined = false;

      if (!rowsToDelete.contains(currentRow)) {
        LOG.trace("Checking " + currentRow.toString());
        int currentRowHeight = currentRow.getXHeightMax();
        LOG.trace("xHeightMax =  " + currentRowHeight);

        RowOfShapes nearestRow = null;
        double shortestDistance = Double.MAX_VALUE;
        int masterRowHeight = -1;

        for (RowOfShapes otherRow : rows) {
          if (!rowsToDelete.contains(otherRow) && !(currentRow.equals(otherRow))) {
            // limit our search to nearby rows
            if (Math.abs(currentRow.getBaseLineMiddlePoint() - otherRow.getBaseLineMiddlePoint()) < (2.0 * maxRowHeight)
                && (currentRow.getRight() >= otherRow.getLeft()) && (otherRow.getRight() >= currentRow.getLeft())) {
              LOG.trace("Comparing to " + otherRow.toString());
              int otherRowHeight = otherRow.getXHeightMax();
              LOG.trace("xHeightMax =  " + otherRowHeight);

              RowOfShapes masterRow = currentRowHeight > otherRowHeight ? currentRow : otherRow;
              RowOfShapes slaveRow = currentRowHeight > otherRowHeight ? otherRow : currentRow;

              double heightRatio = ((double) slaveRow.getXHeightMax() / (double) masterRow.getXHeightMax());
              LOG.trace("height ratio (" + slaveRow.getXHeightMax() + " / " + masterRow.getXHeightMax() + "): " + heightRatio);
              if (heightRatio > maxRatioForCombine)
                continue;

              // avoid combining very long horizontal rules with
              // other rows
              // their top gives a false impression of being
              // closer to the other row's bottom.
              if ((masterRow.getMaxShapeWidth() > maxShapeWidth || slaveRow.getMaxShapeWidth() > maxShapeWidth))
                continue;

              double distance = 0;
              if (currentRow.getBaseLineMiddlePoint() < otherRow.getBaseLineMiddlePoint()) {
                distance = (otherRow.getBaseLineMiddlePoint() - otherRow.getXHeightMax()) - currentRow.getBaseLineMiddlePoint();
                LOG.trace("(otherRow.baseLineMiddlePoint() " + otherRow.getBaseLineMiddlePoint() + " - otherRow.getXHeightMax() "
                    + otherRow.getXHeightMax() + ") - currentRow.baseLineMiddlePoint() " + currentRow.getBaseLineMiddlePoint());
              } else {
                distance = (currentRow.getBaseLineMiddlePoint() - currentRow.getXHeightMax()) - otherRow.getBaseLineMiddlePoint();
                LOG.trace("(currentRow.baseLineMiddlePoint() " + currentRow.getBaseLineMiddlePoint() + " - currentRow.getXHeightMax() "
                    + currentRow.getXHeightMax() + ") - otherRow.baseLineMiddlePoint() " + otherRow.getBaseLineMiddlePoint());
              }
              LOG.debug("Distance between rows: " + distance);

              if (distance < shortestDistance) {
                LOG.trace("Found new closest row: " + otherRow);
                nearestRow = otherRow;
                shortestDistance = distance;
                masterRowHeight = (currentRowHeight >= otherRowHeight) ? currentRowHeight : otherRowHeight;
              }
            }
          }
        }

        if (nearestRow != null) {
          // The number 3 below is chosen arbitrarily - basically we
          // want a
          // relative way of indicating that the rows are very near to
          // each other.
          double minDistanceForCombine = ((double) masterRowHeight / 3);
          LOG.trace("minDistanceForCombine: " + minDistanceForCombine);
          if (shortestDistance < minDistanceForCombine) {
            LOG.debug("Combining the two rows");
            LOG.debug(currentRow.toString());
            LOG.debug(nearestRow.toString());
            rowsToDelete.add(nearestRow);
            currentRow.addShapes(nearestRow.getShapes());
            currentRow.reorderShapes();
            currentRow.recalculate();

            this.joinShapesVertically(currentRow);
            currentRow.assignGuideLines();

            LOG.debug("Resulting row: " + currentRow.toString());

            rowsCombined = true;
          }
        }
      }
      // We may need to combine multiple rows
      // so we only advance if no combination has taken place
      if (!rowsCombined)
        i++;

    }

    // actually delete the rows
    for (RowOfShapes rowToDelete : rowsToDelete) {
      sourceImage.getRows().remove(rowToDelete);
    }
    LOG.debug("########## end combineRows #########");
  }

  /**
   * Group the shapes into words.
   */
  void groupShapesIntoWords(SourceImage sourceImage) {
    LOG.debug("########## groupShapesIntoWords #########");
    for (Set<RowOfShapes> rowCluster : sourceImage.getRowClusters()) {
      this.groupShapesIntoWords(rowCluster);
    }
  }

  void groupShapesIntoWords(Set<RowOfShapes> rowCluster) {
    LOG.debug("Next row cluster of size " + rowCluster.size());
    // group the shapes together into words
    Mean spaceMean = new Mean();
    StandardDeviation spaceStdDev = new StandardDeviation();
    int maxSpaceLog = 120;
    int[] spaceCounts = new int[maxSpaceLog];
    List<Integer> spaces = new ArrayList<Integer>();

    for (RowOfShapes row : rowCluster) {
      Shape previousShape = null;
      for (Shape shape : row.getShapes()) {
        if (previousShape != null) {
          int space = 0;
          if (sourceImage.isLeftToRight())
            space = shape.getLeft() - previousShape.getRight();
          else
            space = previousShape.getLeft() - shape.getRight();
          if (LOG.isTraceEnabled()) {
            LOG.trace(shape.toString());
            LOG.trace("Space : " + space);
          }
          if (space < maxSpaceLog && space >= 0)
            spaceCounts[space]++;
          if (space >= 0) {
            spaces.add(space);
            spaceMean.increment(space);
            spaceStdDev.increment(space);
          }
        }
        previousShape = shape;
      } // next shape
    }

    for (int i = 0; i < maxSpaceLog; i++) {
      // LOG.debug("Space count " + i + ": " + spaceCounts[i]);
    }
    double spaceMeanVal = spaceMean.getResult();
    double spaceStdDevVal = spaceStdDev.getResult();
    LOG.debug("Space mean: " + spaceMeanVal);
    LOG.debug("Space std dev: " + spaceStdDevVal);

    // If however there is only a single word on the row, the
    // standard deviation will be very low.
    boolean singleWord = false;
    if (spaceStdDevVal * 2 < spaceMeanVal) {
      LOG.debug("Assuming a single word per row");
      singleWord = true;
    }

    // Since there should be two groups, one for letters and one for words,
    // the mean should be somewhere in between. We now look for the mean on
    // the
    // lesser group and will use it as the basis for comparison.
    spaceMean = new Mean();
    spaceStdDev = new StandardDeviation();
    for (int space : spaces) {
      if (space < spaceMeanVal && space >= 0) {
        spaceMean.increment(space);
        spaceStdDev.increment(space);
      }
    }
    spaceMeanVal = spaceMean.getResult();
    spaceStdDevVal = spaceStdDev.getResult();
    LOG.debug("Letter space mean: " + spaceMeanVal);
    LOG.debug("Letter space std dev: " + spaceStdDevVal);

    int letterSpaceThreshold = 0;
    if (singleWord)
      letterSpaceThreshold = Integer.MAX_VALUE;
    else
      letterSpaceThreshold = (int) Math.round(spaceMeanVal + (4.0 * spaceStdDevVal));

    for (RowOfShapes row : rowCluster) {
      LOG.debug(row.toString());
      // row.getGroups().clear();
      row.organiseShapesInGroups(letterSpaceThreshold);
    } // next row
  }

  /**
   * Clear out anything found in the right & left margins
   */
  void cleanMargins(SourceImage sourceImage) {
    LOG.debug("########## cleanMargins #########");

    int minCardinalityForMargin = 8;
    double averageShapeWidth = sourceImage.getAverageShapeWidth();

    LOG.debug("Finding right margin");
    double rightLimit = sourceImage.getWidth() * 0.67;

    // first, create a DBScan cluster of all rows near the right-hand side
    List<RowOfShapes> rightHandRows = new ArrayList<RowOfShapes>();
    List<double[]> rightCoordinates = new ArrayList<double[]>();

    for (RowOfShapes row : sourceImage.getRows()) {
      double right = row.getRight();
      if (right >= rightLimit) {
        LOG.trace(row.toString());
        LOG.trace("Right: " + right + " + " + row.getXAdjustment() + " = " + (right - row.getXAdjustment()));
        right -= row.getXAdjustment();
        rightHandRows.add(row);
        rightCoordinates.add(new double[] { right });
      }
    }

    DBSCANClusterer<RowOfShapes> rightMarginClusterer = new DBSCANClusterer<RowOfShapes>(rightHandRows, rightCoordinates);
    Set<Set<RowOfShapes>> rowClusters = rightMarginClusterer.cluster(averageShapeWidth, minCardinalityForMargin, true);

    TreeSet<Set<RowOfShapes>> orderedRowClusters = new TreeSet<Set<RowOfShapes>>(new CardinalityComparator<RowOfShapes>());
    orderedRowClusters.addAll(rowClusters);

    int i = 0;

    // find the right-most cluster with sufficient cardinality, and assume
    // it's the right margin
    DescriptiveStatistics rightMarginStats = null;
    for (Set<RowOfShapes> cluster : orderedRowClusters) {
      DescriptiveStatistics rightStats = new DescriptiveStatistics();
      for (RowOfShapes row : cluster)
        rightStats.addValue(row.getRight() - row.getXAdjustment());

      LOG.debug("Cluster " + i + ". Cardinality=" + cluster.size());
      LOG.debug("Right mean : " + rightStats.getMean());
      LOG.debug("Right std dev: " + rightStats.getStandardDeviation());

      if (cluster.size() >= minCardinalityForMargin && (rightMarginStats == null || rightMarginStats.getMean() < rightStats.getMean())) {
        rightMarginStats = rightStats;
      }
      i++;
    }

    // see how many rows would violate this margin - if too many, assume no
    // margin
    // these rows are only rows which extend across the margin
    if (rightMarginStats != null) {
      LOG.debug("Right margin mean : " + rightMarginStats.getMean());
      LOG.debug("Right margin std dev: " + rightMarginStats.getStandardDeviation());

      double rightMarginLimit = rightMarginStats.getMean() + sourceImage.getAverageShapeWidth();
      LOG.debug("rightMarginLimit: " + rightMarginLimit);
      int numRowsToChop = 0;
      for (RowOfShapes row : sourceImage.getRows()) {
        if (row.getRight() >= rightLimit) {
          if (row.getRight() - row.getXAdjustment() >= rightMarginLimit && row.getLeft() - row.getXAdjustment() <= rightMarginLimit) {
            LOG.debug("Found overlapping row : " + row);
            LOG.debug("Adjusted right : " + (row.getRight() - row.getXAdjustment()));
            numRowsToChop++;
          }
        }
      }
      if (numRowsToChop >= 3) {
        LOG.debug("Too many overlapping rows - ignoring margin");
        rightMarginStats = null;
      }
    }

    if (rightMarginStats != null) {
      double rightMarginLimit = rightMarginStats.getMean() + sourceImage.getAverageShapeWidth();
      List<RowOfShapes> rowsToRemove = new ArrayList<RowOfShapes>();
      for (RowOfShapes row : sourceImage.getRows()) {
        double right = row.getRight() - row.getXAdjustment();
        LOG.trace(row.toString());
        LOG.trace("Adjusted right: " + right);

        if (right >= rightMarginLimit) {
          LOG.trace("Has out-of-margin stuff!");
          // need to chop off groups to the right of this threshold
          List<GroupOfShapes> groupsToChop = new ArrayList<GroupOfShapes>();
          for (GroupOfShapes group : row.getGroups()) {
            if (group.getLeft() - row.getXAdjustment() > rightMarginLimit) {
              groupsToChop.add(group);
              LOG.debug("Chopping group outside of right margin: " + group);
            }
          }
          for (GroupOfShapes group : groupsToChop) {
            row.getShapes().removeAll(group.getShapes());
          }
          row.getGroups().removeAll(groupsToChop);

          if (row.getGroups().size() == 0) {
            LOG.debug("Removing empty " + row);
            rowsToRemove.add(row);
          } else {
            row.recalculate();
            row.assignGuideLines();
          }
        } // does this row extend beyond the margin?
      } // next row
      sourceImage.getRows().removeAll(rowsToRemove);
    } // have a right margin

    LOG.debug("Finding left margin");
    double leftLimit = sourceImage.getWidth() * 0.33;

    // first, create a DBScan cluster of all rows near the left-hand side
    List<RowOfShapes> leftHandRows = new ArrayList<RowOfShapes>();
    List<double[]> leftCoordinates = new ArrayList<double[]>();

    for (RowOfShapes row : sourceImage.getRows()) {
      double left = row.getLeft();
      if (left <= leftLimit) {
        LOG.trace(row.toString());
        LOG.trace("Left: " + left + " - " + row.getXAdjustment() + " = " + (left - row.getXAdjustment()));
        left -= row.getXAdjustment();
        leftHandRows.add(row);
        leftCoordinates.add(new double[] { left });
      }
    }

    DBSCANClusterer<RowOfShapes> leftMarginClusterer = new DBSCANClusterer<RowOfShapes>(leftHandRows, leftCoordinates);
    Set<Set<RowOfShapes>> rowClustersLeft = leftMarginClusterer.cluster(averageShapeWidth, minCardinalityForMargin, true);

    TreeSet<Set<RowOfShapes>> orderedRowClustersLeft = new TreeSet<Set<RowOfShapes>>(new CardinalityComparator<RowOfShapes>());
    orderedRowClustersLeft.addAll(rowClustersLeft);

    i = 0;

    // find the left-most cluster with sufficient cardinality, and assume
    // it's the left margin
    DescriptiveStatistics leftMarginStats = null;
    for (Set<RowOfShapes> cluster : orderedRowClustersLeft) {
      DescriptiveStatistics leftStats = new DescriptiveStatistics();
      for (RowOfShapes row : cluster)
        leftStats.addValue(row.getLeft() - row.getXAdjustment());

      LOG.debug("Cluster " + i + ". Cardinality=" + cluster.size());
      LOG.debug("Left mean : " + leftStats.getMean());
      LOG.debug("Left std dev: " + leftStats.getStandardDeviation());

      if (cluster.size() >= minCardinalityForMargin && (leftMarginStats == null || leftMarginStats.getMean() > leftStats.getMean())) {
        leftMarginStats = leftStats;
      }
      i++;
    }

    // see how many rows would violate this margin - if too many, assume no
    // margin
    // these rows are only rows which extend across the margin
    if (leftMarginStats != null) {
      LOG.debug("Left margin mean : " + leftMarginStats.getMean());
      LOG.debug("Left margin std dev: " + leftMarginStats.getStandardDeviation());

      double leftMarginLimit = leftMarginStats.getMean() - sourceImage.getAverageShapeWidth();
      LOG.debug("leftMarginLimit: " + leftMarginLimit);
      int numRowsToChop = 0;
      for (RowOfShapes row : sourceImage.getRows()) {
        if (row.getLeft() <= leftLimit) {
          if (row.getLeft() - row.getXAdjustment() <= leftMarginLimit && row.getRight() - row.getXAdjustment() >= leftMarginLimit) {
            LOG.debug("Found overlapping row : " + row);
            LOG.debug("Adjusted left : " + (row.getLeft() - row.getXAdjustment()));
            numRowsToChop++;
          }
        }
      }
      if (numRowsToChop >= 3) {
        LOG.debug("Too many overlapping rows - ignoring margin");
        leftMarginStats = null;
      }
    }

    if (leftMarginStats != null) {
      double leftMarginLimit = leftMarginStats.getMean() - sourceImage.getAverageShapeWidth();
      List<RowOfShapes> rowsToRemove = new ArrayList<RowOfShapes>();
      for (RowOfShapes row : sourceImage.getRows()) {
        double left = row.getLeft() - row.getXAdjustment();
        LOG.trace(row.toString());
        LOG.trace("Adjusted left: " + left);

        if (left <= leftMarginLimit) {
          LOG.trace("Has out-of-margin stuff!");
          // need to chop off groups to the left of this threshold
          List<GroupOfShapes> groupsToChop = new ArrayList<GroupOfShapes>();
          for (GroupOfShapes group : row.getGroups()) {
            if (group.getRight() - row.getXAdjustment() < leftMarginLimit) {
              groupsToChop.add(group);
              LOG.debug("Chopping group outside of left margin: " + group);
            }
          }
          for (GroupOfShapes group : groupsToChop) {
            row.getShapes().removeAll(group.getShapes());
          }
          row.getGroups().removeAll(groupsToChop);

          if (row.getGroups().size() == 0) {
            LOG.debug("Removing empty " + row);
            rowsToRemove.add(row);
          } else {
            row.recalculate();
            row.assignGuideLines();
          }
        } // does this row extend beyond the margin?
      } // next row
      sourceImage.getRows().removeAll(rowsToRemove);
    } // have a left margin
  }

  /**
   * Returns a list of areas (horizontal blocks from top to bottom) each split
   * into columns (vertical blocks from left to right).
   */
  List<List<Column>> getColumns(SourceImage sourceImage, List<Column> columns) {
    // Need to take into account a big horizontal space - Pietrushka page 14
    // Find horizontal spaces that go all the way across and are wider than
    // a certain threshold
    // simply do a boolean column and black out everything in a row, than
    // see if there are any remaining spaces above a certain threshold
    // Columns are thus arranged into "areas", separated by white-space.
    boolean[] fullRows = new boolean[sourceImage.getHeight()];
    for (RowOfShapes row : sourceImage.getRows()) {
      for (int y = row.getTop(); y <= row.getBottom(); y++) {
        fullRows[y] = true;
      }
    }
    DescriptiveStatistics rowHeightStats = new DescriptiveStatistics();

    for (RowOfShapes row : sourceImage.getRows()) {
      int height = row.getXHeight();
      rowHeightStats.addValue(height);
    }
    double avgRowHeight = rowHeightStats.getPercentile(50);
    LOG.debug("meanRowHeight: " + avgRowHeight);
    double minHeightForWhiteSpace = avgRowHeight * 1.3;
    LOG.debug("minHeightForWhiteSpace: " + minHeightForWhiteSpace);

    // find the "white rows" - any horizontal white space
    // in the page which is sufficiently high
    List<int[]> whiteRows = new ArrayList<int[]>();
    boolean inWhite = false;
    int startWhite = 0;
    for (int y = 0; y < sourceImage.getHeight(); y++) {
      if (!inWhite && !fullRows[y]) {
        inWhite = true;
        startWhite = y;
      } else if (inWhite && fullRows[y]) {
        int length = y - startWhite;
        if (length > minHeightForWhiteSpace) {
          LOG.debug("Adding whiteRow " + startWhite + "," + (y - 1));
          whiteRows.add(new int[] { startWhite, y - 1 });
        }
        inWhite = false;
      }
    }
    if (inWhite)
      whiteRows.add(new int[] { startWhite, sourceImage.getHeight() - 1 });
    whiteRows.add(new int[] { sourceImage.getHeight(), sourceImage.getHeight() });

    // place rows in "areas" defined by the "white rows" found above
    List<List<RowOfShapes>> areas = new ArrayList<List<RowOfShapes>>();
    int startY = -1;
    for (int[] whiteRow : whiteRows) {
      List<RowOfShapes> area = new ArrayList<RowOfShapes>();
      for (RowOfShapes row : sourceImage.getRows()) {
        if (row.getTop() >= startY && row.getBottom() <= whiteRow[0]) {
          area.add(row);
        }
      }
      if (area.size() > 0) {
        areas.add(area);
      }
      startY = whiteRow[1];
    }

    // break up each area into vertical columns
    LOG.debug("break up each area into vertical columns");
    if (columns == null)
      columns = new ArrayList<Column>();
    List<List<Column>> columnsPerAreaList = new ArrayList<List<Column>>();
    for (List<RowOfShapes> area : areas) {
      LOG.debug("Next area");
      List<Column> columnsPerArea = new ArrayList<Column>();
      columnsPerAreaList.add(columnsPerArea);
      TreeSet<RowOfShapes> rows = new TreeSet<RowOfShapes>(new RowOfShapesVerticalLocationComparator());
      rows.addAll(area);
      for (RowOfShapes row : rows) {
        // try to place this row in one of the columns directly above
        // it.
        // this means that a row which overlaps more than one column has
        // to "close" this column, so it is no longer considered
        List<Column> overlappingColumns = new ArrayList<Column>();
        for (Column column : columnsPerArea) {
          if (!column.closed) {
            RowOfShapes lastRowInColumn = column.get(column.size() - 1);
            if (row.getRight() - row.getXAdjustment() >= lastRowInColumn.getLeft() - lastRowInColumn.getXAdjustment()
                && row.getLeft() - row.getXAdjustment() <= lastRowInColumn.getRight() - lastRowInColumn.getXAdjustment()) {
              overlappingColumns.add(column);
            }
          }
        }
        if (overlappingColumns.size() == 1) {
          Column myColumn = overlappingColumns.get(0);
          RowOfShapes lastRowInMyColumn = myColumn.get(0);

          // close any columns that are now at a distance of more than
          // one row
          for (Column column : columnsPerArea) {
            if (!column.closed && !column.equals(myColumn)) {
              RowOfShapes lastRowInColumn = column.get(column.size() - 1);
              if (lastRowInMyColumn.getTop() > lastRowInColumn.getBottom()) {
                column.closed = true;
                LOG.debug("Closing distant column " + lastRowInColumn);
              }
            }
          }

          myColumn.add(row);
          LOG.debug(row.toString());
          LOG.debug("  added to column " + lastRowInMyColumn);
        } else {
          for (Column overlappingColumn : overlappingColumns) {
            overlappingColumn.closed = true;
            RowOfShapes lastRowInColumn = overlappingColumn.get(overlappingColumn.size() - 1);
            LOG.debug("Closing overlapping column " + lastRowInColumn);
          }
          Column myColumn = new Column(sourceImage);
          myColumn.add(row);
          LOG.debug("Found new column");
          LOG.debug(row.toString());
          columns.add(myColumn);
          columnsPerArea.add(myColumn);
        }
      }
    } // next area

    for (Column column : columns)
      column.recalculate();

    return columnsPerAreaList;
  }

  /**
   * Group columns together depending on whether they align vertically or not.
   * Column groups allow us to increase the statistical weight of columns by
   * adding more rows, as long as they're aligned vertically.
   */
  List<List<Column>> getColumnGroups(List<List<Column>> columnsPerAreaList) {
    // We'll assume that two columns from two consecutive areas are in the
    // same vertical group if they overlap with each other horizontally
    // and don't overlap with any other column in the other column's area.
    List<List<Column>> columnGroups = new ArrayList<List<Column>>();
    List<Column> columnsInPrevArea = null;
    for (List<Column> columnsPerArea : columnsPerAreaList) {
      if (columnsInPrevArea != null) {
        for (Column prevColumn : columnsInPrevArea) {
          LOG.debug("Checking " + prevColumn);
          // find the column group containing the previous column
          List<Column> myColumnGroup = null;
          for (List<Column> columnGroup : columnGroups) {
            if (columnGroup.contains(prevColumn)) {
              myColumnGroup = columnGroup;
              break;
            }
          }
          if (myColumnGroup == null) {
            myColumnGroup = new ArrayList<Column>();
            LOG.debug("Creating column group for column " + prevColumn.toString());
            columnGroups.add(myColumnGroup);
            myColumnGroup.add(prevColumn);
          }

          // does only one column overlap with this one?
          Column overlappingColumn = null;
          for (Column column : columnsPerArea) {
            if (column.adjustedRight >= prevColumn.adjustedLeft && column.adjustedLeft <= prevColumn.adjustedRight) {
              if (overlappingColumn == null) {
                LOG.debug("I overlap with " + column);

                overlappingColumn = column;
              } else {
                LOG.debug("But I overlap also with " + column);

                overlappingColumn = null;
                break;
              }
            }
          }
          if (overlappingColumn != null) {
            // does it overlap with only me?
            for (Column otherPrevColumn : columnsInPrevArea) {
              if (otherPrevColumn.equals(prevColumn))
                continue;
              if (overlappingColumn.adjustedRight >= otherPrevColumn.adjustedLeft
                  && overlappingColumn.adjustedLeft <= otherPrevColumn.adjustedRight) {
                LOG.debug("But it overlaps also with " + otherPrevColumn);
                overlappingColumn = null;
                break;
              }
            }
          }
          if (overlappingColumn != null) {
            myColumnGroup.add(overlappingColumn);
            LOG.debug("Adding " + overlappingColumn);
            LOG.debug(" to group with " + prevColumn);
          }

        } // next previous column
      } // have previous columns
      columnsInPrevArea = columnsPerArea;
    } // next area
    if (columnsInPrevArea != null) {
      for (Column prevColumn : columnsInPrevArea) {
        // find the column group containing the previous column
        List<Column> myColumnGroup = null;
        for (List<Column> columnGroup : columnGroups) {
          if (columnGroup.contains(prevColumn)) {
            myColumnGroup = columnGroup;
            break;
          }
        }
        if (myColumnGroup == null) {
          myColumnGroup = new ArrayList<Column>();
          LOG.debug("Creating column group for column " + prevColumn.toString());
          columnGroups.add(myColumnGroup);
          myColumnGroup.add(prevColumn);
        }
      }
    }
    return columnGroups;
  }

  /**
   * When looking for columns, we sometimes add a false column, in pages where
   * a long vertical white line happens to break a row into columns. We try to
   * recognise this phenomenon and restore the original row.
   */
  void removeFalseColumns(SourceImage sourceImage, List<Rectangle> columnSeparators) {
    LOG.debug("########## start removeFalseColumns #########");
    List<Column> columns = new ArrayList<Column>();
    List<List<Column>> columnsPerAreaList = this.getColumns(sourceImage, columns);

    // Intermediate step to reform the vertical columns, if they exist
    List<List<Column>> columnGroups = this.getColumnGroups(columnsPerAreaList);

    // Merge columns from each group
    List<Column> mergedColumns = new ArrayList<Column>();
    for (List<Column> columnGroup : columnGroups) {
      Column mergedColumn = new Column(sourceImage);
      for (Column column : columnGroup) {
        for (RowOfShapes row : column) {
          mergedColumn.add(row);
        }
      }
      mergedColumn.recalculate();
      mergedColumns.add(mergedColumn);
    }

    // retain those pairs where we have a possible row continuation, meaning
    // a) they contain aligned rows with a small distance between them
    // b) one of the rows is much shorter than the other (typical of this
    // type of fringe phenomenon)
    // c) the "small" column contains a relatively small number of rows
    List<Column[]> columnsToMerge = new ArrayList<Column[]>();
    List<RowOfShapes[]> rowsToMerge = new ArrayList<RowOfShapes[]>();
    int counter = 0;
    do {
      LOG.debug("Running iteration " + counter++);
      // find each merged column's right- and left-hand neighbors
      Map<Column, List<Column>> rightHandNeighbors = new HashMap<Column, List<Column>>();
      Map<Column, List<Column>> leftHandNeighbors = new HashMap<Column, List<Column>>();

      for (Column column : mergedColumns) {
        rightHandNeighbors.put(column, new ArrayList<Column>());
        leftHandNeighbors.put(column, new ArrayList<Column>());
      }

      for (int i = 0; i < mergedColumns.size() - 1; i++) {
        Column column = mergedColumns.get(i);
        for (int j = i + 1; j < mergedColumns.size(); j++) {
          Column otherColumn = mergedColumns.get(j);
          if (column.top <= otherColumn.bottom && column.bottom >= otherColumn.top) {
            if (column.adjustedLeft <= otherColumn.adjustedLeft) {
              rightHandNeighbors.get(column).add(otherColumn);
              leftHandNeighbors.get(otherColumn).add(column);
            } else {
              leftHandNeighbors.get(column).add(otherColumn);
              rightHandNeighbors.get(otherColumn).add(column);
            }
          }
        }
      }

      columnsToMerge = new ArrayList<Column[]>();
      rowsToMerge = new ArrayList<RowOfShapes[]>();
      for (Column column : mergedColumns) {
        LOG.debug(column.toString());
        Map<Column, List<Column>> neighbors = rightHandNeighbors;
        if (!sourceImage.isLeftToRight())
          neighbors = leftHandNeighbors;

        for (Column otherColumn : neighbors.get(column)) {
          // is the "other column" much narrower?
          if ((double) otherColumn.getWidth() / (double) column.getWidth() <= 0.5) {
            LOG.debug("Found narrow merge candidate: " + otherColumn);
            boolean smallSize = column.size() <= 4 || otherColumn.size() <= 0.4 * column.size();
            if (!smallSize) {
              LOG.debug("otherColumn has too many rows, exiting");
              continue;
            }

            // for each row in the otherColumn, find an aligned row
            // in the column
            List<RowOfShapes[]> matches = new ArrayList<RowOfShapes[]>();
            for (RowOfShapes otherRow : otherColumn) {
              LOG.debug("Trying to match otherRow: " + otherRow.toString());
              LOG.debug("otherRow.getBaseLineMiddlePoint() " + otherRow.getBaseLineMiddlePoint());
              for (RowOfShapes row : column) {
                LOG.debug("row: " + row.toString());
                LOG.debug("row.getBaseLineMiddlePoint() " + row.getBaseLineMiddlePoint());
                if (Math.abs(row.getBaseLineMiddlePoint() - otherRow.getBaseLineMiddlePoint()) < sourceImage.getAverageShapeHeight() / 4.0
                    && ((sourceImage.isLeftToRight() && otherRow.getLeft() - row.getRight() < sourceImage.getAverageShapeWidth() * 2.0)
                        || (!sourceImage.isLeftToRight()
                            && row.getLeft() - otherRow.getRight() < sourceImage.getAverageShapeWidth() * 2.0))) {
                  LOG.debug("Found match : " + row + " AND " + otherRow);
                  matches.add(new RowOfShapes[] { row, otherRow });
                  break;
                }
              }
            }
            if (matches.size() == otherColumn.size()) {
              // we have matches for all rows
              LOG.debug("Have matches for all rows, merging");
              columnsToMerge.add(new Column[] { column, otherColumn });
              rowsToMerge.addAll(matches);
            } // have perfect match
          } // have match candidate
        } // next neighbor
      } // next column

      if (columnsToMerge.size() > 0) {
        for (RowOfShapes[] rowMatch : rowsToMerge) {
          RowOfShapes masterRow = rowMatch[0];
          RowOfShapes slaveRow = rowMatch[1];
          masterRow.addShapes(slaveRow.getShapes());
          masterRow.recalculate();

          masterRow.addShapes(slaveRow.getShapes());
          masterRow.reorderShapes();
          masterRow.recalculate();

          this.joinShapesVertically(masterRow);
          masterRow.assignGuideLines();
          sourceImage.removeRow(slaveRow);
        }
        for (Column[] columnMatch : columnsToMerge) {
          Column masterColumn = columnMatch[0];
          Column slaveColumn = columnMatch[1];
          masterColumn.recalculate();
          mergedColumns.remove(slaveColumn);
        }
      }
    } while (columnsToMerge.size() > 0);

    LOG.debug("########## end removeFalseColumns #########");
  }

  /**
   * Detects paragraph splits and assign rows to correct paragraphs.
   */
  void groupRowsIntoParagraphs(SourceImage sourceImage) {
    LOG.debug("########## groupRowsIntoParagraphs #########");
    // We'll use various possible indicators, including
    // indented start, indented end, and spacing between rows.

    // On pages with a single big paragraph makes it hypersensitive to
    // differences in row-start/row-end
    // This means we cannot use deviation. Instead, we use the average shape
    // width on the page.
    // We also adjust maxLeft & minRight to match the vertical line slope

    // This is now complicated by the possibility of multiple columns

    List<Column> columns = new ArrayList<Column>();
    List<List<Column>> columnsPerAreaList = this.getColumns(sourceImage, columns);

    // Intermediate step to reform the vertical columns, if they exist
    // basically the idea is that if the columns are aligned vertically,
    // then the thresholds for paragraph indents
    // should be shared, to increase the statistical sample size and reduce
    // anomalies.
    List<List<Column>> columnGroups = this.getColumnGroups(columnsPerAreaList);

    // What we really want here is, for each column (in the case of
    // right-to-left),
    // two clusters on the right
    // and one relatively big cluster on the left.
    // anything outside of the cluster on the left is an EOP.
    boolean hasTab = false;
    for (List<Column> columnGroup : columnGroups) {
      LOG.debug("Next column group");
      double averageShapeWidth = sourceImage.getAverageShapeWidth();
      LOG.debug("averageShapeWidth: " + averageShapeWidth);
      double epsilon = averageShapeWidth / 2.0;
      LOG.debug("epsilon: " + epsilon);

      int columnGroupTop = sourceImage.getHeight();
      int columnGroupBottom = 0;
      int columnGroupLeft = sourceImage.getWidth();
      int columnGroupRight = 0;
      for (Column column : columnGroup) {
        if (column.top < columnGroupTop)
          columnGroupTop = (int) Math.round(column.top);
        if (column.bottom > columnGroupBottom)
          columnGroupBottom = (int) Math.round(column.bottom);
        if (column.adjustedLeft < columnGroupLeft)
          columnGroupLeft = (int) Math.round(column.adjustedLeft);
        if (column.adjustedRight > columnGroupRight)
          columnGroupRight = (int) Math.round(column.adjustedRight);
      }

      // right thresholds
      LOG.debug("Calculating right thresholds");

      // first, create a DBScan cluster of all rows by their adjusted
      // right coordinate
      List<RowOfShapes> rightHandRows = new ArrayList<RowOfShapes>();
      List<double[]> rightCoordinates = new ArrayList<double[]>();

      for (Column column : columnGroup) {
        for (RowOfShapes row : column) {
          double right = row.getRight() - row.getXAdjustment();
          // double rightOverlap =
          // this.findLargeShapeOverlapOnRight(row, column,
          // sourceImage);
          // if (rightOverlap==0) {
          // // leave out any right-overlapping rows here
          // // since we need accurate statistics for margin detection
          // // This is questionable - especially since a long
          // vertical bar (see Petriushka)
          // // tends to give all rows a left overlap. Also, because
          // the overlap is calculated based
          // // on the mean right & mean left, not based on any sort
          // of margin clusters.
          // rightHandRows.add(row);
          // rightCoordinates.add(new double[] {right});
          // }
          rightHandRows.add(row);
          rightCoordinates.add(new double[] { right });

        }
      }

      int minCardinalityForRightMargin = 5;
      DBSCANClusterer<RowOfShapes> rightMarginClusterer = new DBSCANClusterer<RowOfShapes>(rightHandRows, rightCoordinates);
      Set<Set<RowOfShapes>> rowClusters = rightMarginClusterer.cluster(epsilon, minCardinalityForRightMargin, true);

      TreeSet<Set<RowOfShapes>> orderedRowClusters = new TreeSet<Set<RowOfShapes>>(new CardinalityComparator<RowOfShapes>());
      orderedRowClusters.addAll(rowClusters);

      int i = 0;

      // find the two right-most clusters, and assume they are the margin
      // & the tab
      DescriptiveStatistics rightMarginStats = null;
      DescriptiveStatistics rightTabStats = null;
      for (Set<RowOfShapes> cluster : orderedRowClusters) {
        DescriptiveStatistics rightStats = new DescriptiveStatistics();
        MeanAbsoluteDeviation rightDev = new MeanAbsoluteDeviation();
        for (RowOfShapes row : cluster) {
          int rowIndex = rightHandRows.indexOf(row);
          double right = rightCoordinates.get(rowIndex)[0];
          rightStats.addValue(right);
          rightDev.increment(right);
        }

        LOG.debug("Cluster " + i + ". Cardinality=" + cluster.size());
        LOG.debug("Right mean : " + rightStats.getMean());
        LOG.debug("Right dev: " + rightDev.getResult());

        if (cluster.size() >= minCardinalityForRightMargin) {
          if (rightMarginStats == null || rightMarginStats.getMean() < rightStats.getMean()) {
            if (rightMarginStats != null)
              rightTabStats = rightMarginStats;
            rightMarginStats = rightStats;
          } else if (rightTabStats == null || rightTabStats.getMean() < rightStats.getMean()) {
            rightTabStats = rightStats;
          }
        } else {
          break;
        }
        i++;
      } // next right-coordinate cluster

      double rightMargin = sourceImage.getWidth();
      double rightTab = sourceImage.getWidth();
      if (rightMarginStats != null) {
        rightMargin = rightMarginStats.getMean();
      } else {
        List<Rectangle> columnSeparators = sourceImage.findColumnSeparators();
        for (Rectangle columnSeparator : columnSeparators) {
          if (columnSeparator.getTop() <= columnGroupTop && columnSeparator.getBottom() >= columnGroupBottom
              && columnSeparator.getLeft() >= columnGroupRight) {
            if (columnSeparator.getLeft() < rightMargin)
              rightMargin = columnSeparator.getLeft();
          }
        }
      }
      if (rightTabStats != null) {
        rightTab = rightTabStats.getMean();
      }

      LOG.debug("rightMargin: " + rightMargin);
      LOG.debug("rightTab: " + rightTab);

      // left thresholds
      LOG.debug("Calculating left thresholds");

      // first, create a DBScan cluster of all rows by their adjusted left
      // coordinate
      List<RowOfShapes> leftHandRows = new ArrayList<RowOfShapes>();
      List<double[]> leftCoordinates = new ArrayList<double[]>();

      for (Column column : columnGroup) {
        for (RowOfShapes row : column) {
          double left = row.getLeft() - row.getXAdjustment();
          // double leftOverlap =
          // this.findLargeShapeOverlapOnLeft(row, column,
          // sourceImage);
          // if (leftOverlap == 0) {
          // // leave out any overlapping rows from margin calcs,
          // // since we need accurate statistics here
          // leftHandRows.add(row);
          // leftCoordinates.add(new double[] {left});
          // }
          leftHandRows.add(row);
          leftCoordinates.add(new double[] { left });
        }
      }

      int minCardinalityForLeftMargin = 5;
      DBSCANClusterer<RowOfShapes> leftMarginClusterer = new DBSCANClusterer<RowOfShapes>(leftHandRows, leftCoordinates);
      Set<Set<RowOfShapes>> leftRowClusters = leftMarginClusterer.cluster(epsilon, minCardinalityForLeftMargin, true);

      TreeSet<Set<RowOfShapes>> orderedLeftRowClusters = new TreeSet<Set<RowOfShapes>>(new CardinalityComparator<RowOfShapes>());
      orderedLeftRowClusters.addAll(leftRowClusters);

      i = 0;

      // find the two left-most clusters, and assume they are the margin &
      // the tab
      DescriptiveStatistics leftMarginStats = null;
      DescriptiveStatistics leftTabStats = null;
      for (Set<RowOfShapes> cluster : orderedLeftRowClusters) {
        DescriptiveStatistics leftStats = new DescriptiveStatistics();
        MeanAbsoluteDeviation leftDev = new MeanAbsoluteDeviation();
        for (RowOfShapes row : cluster) {
          int rowIndex = leftHandRows.indexOf(row);
          double left = leftCoordinates.get(rowIndex)[0];
          leftStats.addValue(left);
          leftDev.increment(left);
        }

        LOG.debug("Cluster " + i + ". Cardinality=" + cluster.size());
        LOG.debug("Left mean : " + leftStats.getMean());
        LOG.debug("Left dev: " + leftDev.getResult());

        if (cluster.size() >= minCardinalityForLeftMargin) {
          if (leftMarginStats == null || leftMarginStats.getMean() > leftStats.getMean()) {
            if (leftMarginStats != null)
              leftTabStats = leftMarginStats;
            leftMarginStats = leftStats;
          } else if (leftTabStats == null || leftTabStats.getMean() > leftStats.getMean()) {
            leftTabStats = leftStats;
          }
        } else {
          break;
        }
        i++;
      } // next left-coordinate cluster

      double leftMargin = 0;
      double leftTab = 0;
      if (leftMarginStats != null) {
        leftMargin = leftMarginStats.getMean();
      } else {
        List<Rectangle> columnSeparators = sourceImage.findColumnSeparators();
        for (Rectangle columnSeparator : columnSeparators) {
          if (columnSeparator.getTop() <= columnGroupTop && columnSeparator.getBottom() >= columnGroupBottom
              && columnSeparator.getRight() <= columnGroupLeft) {
            if (columnSeparator.getRight() > leftMargin)
              leftMargin = columnSeparator.getRight();
          }
        }
      }
      if (leftTabStats != null) {
        leftTab = leftTabStats.getMean();
      }

      LOG.debug("leftMargin: " + leftMargin);
      LOG.debug("leftTab: " + leftTab);

      for (Column column : columnGroup) {
        if (sourceImage.isLeftToRight()) {
          column.startMargin = leftMargin;
          if (leftTabStats != null) {
            column.startTab = leftTab;
            column.hasTab = true;
          } else {
            LOG.debug("No left tab - setting based on left margin");
            column.startTab = leftMargin + (5.0 * sourceImage.getAverageShapeWidth());
            column.hasTab = false;
          }

          column.endMargin = rightMargin;
        } else {
          column.startMargin = rightMargin;
          if (rightTabStats != null) {
            column.startTab = rightTab;
            column.hasTab = true;
          } else {
            LOG.debug("No right tab - setting based on right margin");
            column.startTab = rightMargin - (5.0 * sourceImage.getAverageShapeWidth());
            column.hasTab = false;
          }

          column.endMargin = leftMargin;
        }
        LOG.debug("Margins for " + column);
        LOG.debug("startMargin: " + column.startMargin);
        LOG.debug("startTab: " + column.startTab);
        LOG.debug("endMargin: " + column.endMargin);
      } // next column
    } // next column group
    LOG.debug("hasTab: " + hasTab);

    double safetyMargin = 1.5 * sourceImage.getAverageShapeWidth();

    // Now, paragraphs are either "indented", "outdented" or not "dented" at
    // all (no tabs).
    // This applies to the entire page.
    // To recognise indenting vs. outdenting, we have to see if the row
    // preceding each
    // indent/outdent is full or partial. In the case of indentation,
    // partial rows will
    // typically be followed by an indent. In the case of outdentation,
    // partial rows will
    // typically be followed by an outdent.
    // Note that this is a bit of a simplification: some pages are both
    // outdented and indented
    // at worst, such pages will have bad paragraphization :(
    boolean isIndented = true;

    int indentCount = 0;
    int outdentCount = 0;
    for (List<Column> columnGroup : columnGroups) {
      LOG.debug("Next column group");
      boolean prevRowPartial = false;
      for (Column column : columnGroup) {
        if (column.hasTab) {
          for (RowOfShapes row : column) {
            if (sourceImage.isLeftToRight()) {
              if (prevRowPartial) {
                if (row.getLeft() - row.getXAdjustment() > column.startTab - safetyMargin) {
                  indentCount++;
                } else if (row.getLeft() - row.getXAdjustment() < column.startMargin + safetyMargin) {
                  outdentCount++;
                }
              }
              if (row.getRight() - row.getXAdjustment() < column.endMargin - safetyMargin) {
                prevRowPartial = true;
              } else {
                prevRowPartial = false;
              }
            } else {
              if (prevRowPartial) {
                if (row.getRight() - row.getXAdjustment() < column.startTab + safetyMargin) {
                  indentCount++;
                } else if (row.getRight() - row.getXAdjustment() > column.startMargin - safetyMargin) {
                  outdentCount++;
                }
              }
              if (row.getLeft() - row.getXAdjustment() > column.endMargin + safetyMargin) {
                prevRowPartial = true;
              } else {
                prevRowPartial = false;
              }
            } // left-to-right?
          } // next row
        } // column has tab
      } // next column
    } // next column group
    isIndented = (indentCount + 2 >= outdentCount);
    LOG.debug("indentCount: " + indentCount);
    LOG.debug("outdentCount: " + outdentCount);
    LOG.debug("isIndented: " + isIndented);

    // order the columns
    TreeSet<Column> orderedColumns = new TreeSet<Column>(columns);
    columns.clear();
    columns.addAll(orderedColumns);

    // find the paragraphs found in each column
    for (Column column : columns) {
      LOG.debug("--- Next column ---");

      // break up the column into paragraphs
      Paragraph paragraph = null;
      RowOfShapes previousRow = null;
      int maxShapesForStandaloneParagraph = 2;
      List<RowOfShapes> rowsForStandaloneParagraphs = new ArrayList<RowOfShapes>();
      Point2D previousPointStartMargin = null;
      Point2D previousPointStartTab = null;
      Point2D previousPointEndMargin = null;

      for (RowOfShapes row : column) {
        boolean rowForStandaloneParagraph = false;
        boolean newParagraph = false;
        if (row.getShapes().size() <= maxShapesForStandaloneParagraph) {
          rowsForStandaloneParagraphs.add(row);
          rowForStandaloneParagraph = true;
        } else {
          double rightOverlap = this.findLargeShapeOverlapOnRight(row, column, sourceImage);
          double leftOverlap = this.findLargeShapeOverlapOnLeft(row, column, sourceImage);

          if (drawSegmentation) {
            double rowVerticalMidPoint = row.getBaseLineMiddlePoint();
            double startMarginX = column.startMargin + row.getXAdjustment();
            double startTabX = column.startTab + row.getXAdjustment();
            double endMarginX = column.endMargin + row.getXAdjustment();

            if (sourceImage.isLeftToRight()) {
              startMarginX += safetyMargin;
              startTabX -= safetyMargin;
              endMarginX -= safetyMargin;

              startMarginX += leftOverlap;
              startTabX += leftOverlap;
              endMarginX -= rightOverlap;
            } else {
              startMarginX -= safetyMargin;
              startTabX += safetyMargin;
              endMarginX += safetyMargin;

              startMarginX -= rightOverlap;
              startTabX -= rightOverlap;
              endMarginX += leftOverlap;
            }

            Point2D.Double currentPointStartMargin = new Point2D.Double(startMarginX, rowVerticalMidPoint);
            Point2D.Double currentPointStartTab = new Point2D.Double(startTabX, rowVerticalMidPoint);
            Point2D.Double currentPointEndMargin = new Point2D.Double(endMarginX, rowVerticalMidPoint);

            if (previousPointStartMargin != null) {
              graphics2D.setStroke(new BasicStroke(1));
              graphics2D.setPaint(Color.BLUE);
              graphics2D.drawLine((int) Math.round(previousPointStartMargin.getX()), (int) Math.round(previousPointStartMargin.getY()),
                  (int) Math.round(currentPointStartMargin.getX()), (int) Math.round(currentPointStartMargin.getY()));
              graphics2D.drawLine((int) Math.round(previousPointEndMargin.getX()), (int) Math.round(previousPointEndMargin.getY()),
                  (int) Math.round(currentPointEndMargin.getX()), (int) Math.round(currentPointEndMargin.getY()));

              graphics2D.setPaint(Color.RED);
              graphics2D.drawLine((int) Math.round(previousPointStartTab.getX()), (int) Math.round(previousPointStartTab.getY()),
                  (int) Math.round(currentPointStartTab.getX()), (int) Math.round(currentPointStartTab.getY()));

              graphics2D.setPaint(Color.RED);
              graphics2D.drawLine((int) Math.round(previousPointEndMargin.getX()), (int) Math.round(previousPointEndMargin.getY()),
                  (int) Math.round(currentPointEndMargin.getX()), (int) Math.round(currentPointEndMargin.getY()));
            }
            previousPointStartMargin = currentPointStartMargin;
            previousPointStartTab = currentPointStartTab;
            previousPointEndMargin = currentPointEndMargin;
          }

          if (previousRow == null) {
            LOG.debug("New paragraph (first)");
            newParagraph = true;
          } else {
            if (sourceImage.isLeftToRight()) {
              if (previousRow.getRight() - previousRow.getXAdjustment() - rightOverlap < column.endMargin - safetyMargin) {
                LOG.debug("New paragraph (previous EOP)");
                LOG.debug(previousRow.getRight() + " - " + ((int) previousRow.getXAdjustment()) + " - " + ((int) rightOverlap) + " ("
                    + (previousRow.getRight() - previousRow.getXAdjustment() - rightOverlap) + ") " + " < " + ((int) column.endMargin)
                    + " - " + ((int) safetyMargin) + " (" + ((int) column.endMargin - safetyMargin) + ")");
                newParagraph = true;
              } else if (column.hasTab && isIndented && row.getLeft() - row.getXAdjustment() + leftOverlap > column.startTab - safetyMargin) {
                LOG.debug("New paragraph (indent)");
                newParagraph = true;
              } else if (column.hasTab && !isIndented && row.getLeft() - row.getXAdjustment() + leftOverlap < column.startMargin + safetyMargin) {
                LOG.debug("New paragraph (outdent)");
                newParagraph = true;
              }
            } else {
              if (previousRow.getLeft() - previousRow.getXAdjustment() + leftOverlap > column.endMargin + safetyMargin) {
                LOG.debug("New paragraph (previous EOP)");
                LOG.debug(previousRow.getLeft() + " - " + ((int) previousRow.getXAdjustment()) + " + " + ((int) leftOverlap) + " ("
                    + (previousRow.getLeft() - previousRow.getXAdjustment() + leftOverlap) + ") " + " < " + ((int) column.endMargin) + " + "
                    + ((int) safetyMargin) + " (" + ((int) column.endMargin + safetyMargin) + ")");
                newParagraph = true;
              } else if (column.hasTab && isIndented && row.getRight() - row.getXAdjustment() - rightOverlap < column.startTab + safetyMargin) {
                LOG.debug("New paragraph (indent)");
                newParagraph = true;
              } else if (column.hasTab && !isIndented
                  && row.getRight() - row.getXAdjustment() - rightOverlap > column.startMargin - safetyMargin) {
                LOG.debug("New paragraph (outdent)");
                newParagraph = true;
              }
            } // left-to-right?
          } // have previous row
        } // standalone paragraph?

        if (!rowForStandaloneParagraph)
          LOG.debug(row.toString());

        if (newParagraph) {
          if (rowsForStandaloneParagraphs.size() > 0) {
            for (RowOfShapes oneRow : rowsForStandaloneParagraphs) {
              LOG.debug("Standalone paragraph");
              LOG.debug("Standalone row: left(" + oneRow.getLeft() + "), top(" + oneRow.getTop() + "), right(" + oneRow.getRight() + "), bottom("
                  + oneRow.getBottom() + ")");
              Paragraph standaloneParagraph = sourceImage.newParagraph();
              standaloneParagraph.addRow(oneRow);
            }
            rowsForStandaloneParagraphs.clear();
          }
          paragraph = sourceImage.newParagraph();
        }
        // LOG.debug("Row: left(" + row.getLeft() + "), right(" +
        // row.getRight() + "), width(" + (row.getRight() -
        // row.getLeft() + 1) + ")");

        if (!rowForStandaloneParagraph) {
          paragraph.addRow(row);
          previousRow = row;
        }
      } // next row in column
      if (rowsForStandaloneParagraphs.size() > 0) {
        for (RowOfShapes oneRow : rowsForStandaloneParagraphs) {
          LOG.debug("Standalone paragraph");
          LOG.debug("Standalone row: left(" + oneRow.getLeft() + "), top(" + oneRow.getTop() + "), right(" + oneRow.getRight() + "), bottom("
              + oneRow.getBottom() + ")");
          Paragraph standaloneParagraph = sourceImage.newParagraph();
          standaloneParagraph.addRow(oneRow);
        }
        rowsForStandaloneParagraphs.clear();
      }
    } // next column

  }

  private double findLargeShapeOverlapOnLeft(RowOfShapes row, Column column, SourceImage sourceImage) {
    double overlap = 0;
    double leftMargin = 0;
    if (sourceImage.isLeftToRight())
      leftMargin = column.startMargin;
    else
      leftMargin = column.endMargin;
    for (Rectangle whiteArea : sourceImage.getWhiteAreasAroundLargeShapes()) {
      if (whiteArea.getTop() <= row.getBottom() && whiteArea.getBottom() >= row.getTop()) {
        if (whiteArea.getLeft() - row.getXAdjustment() < leftMargin && whiteArea.getRight() - row.getXAdjustment() > leftMargin) {
          overlap = (whiteArea.getRight() - row.getXAdjustment()) - leftMargin;
          LOG.debug("Overlaps large shape (" + whiteArea.getLeft() + "," + whiteArea.getTop() + "," + whiteArea.getRight() + ","
              + whiteArea.getBottom() + ")" + " on left by " + overlap);
        }
      }
    }
    return overlap;
  }

  private double findLargeShapeOverlapOnRight(RowOfShapes row, Column column, SourceImage sourceImage) {
    double overlap = 0;
    double rightMargin = 0;
    if (sourceImage.isLeftToRight())
      rightMargin = column.endMargin;
    else
      rightMargin = column.startMargin;
    for (Rectangle whiteArea : sourceImage.getWhiteAreasAroundLargeShapes()) {
      if (whiteArea.getTop() <= row.getBottom() && whiteArea.getBottom() >= row.getTop()) {
        if (whiteArea.getLeft() - row.getXAdjustment() < rightMargin && whiteArea.getRight() - row.getXAdjustment() > rightMargin) {
          overlap = rightMargin - (whiteArea.getLeft() - row.getXAdjustment());
          LOG.debug("Overlaps large shape (" + whiteArea.getLeft() + "," + whiteArea.getTop() + "," + whiteArea.getRight() + ","
              + whiteArea.getBottom() + ")" + " on right by " + overlap);
        }
      }
    }
    return overlap;
  }

  /**
   * A vertical group of rows which will be analysed together for paragraphs
   * 
   * @author Assaf Urieli
   *
   */
  @SuppressWarnings("serial")
  private static final class Column extends ArrayList<RowOfShapes> implements Comparable<Column> {
    private SourceImage sourceImage;

    public double startMargin;
    public double startTab;
    public double endMargin;

    public double adjustedLeft;
    public double adjustedRight;
    public double top;
    public double bottom;

    public boolean closed = false;
    public boolean hasTab = false;

    private Column(SourceImage sourceImage) {
      super();
      this.sourceImage = sourceImage;
    }

    public void recalculate() {
      adjustedLeft = sourceImage.getWidth();
      adjustedRight = 0;
      top = sourceImage.getHeight();
      bottom = 0;

      for (RowOfShapes row : this) {
        double left = row.getLeft() - row.getXAdjustment();
        double right = row.getRight() - row.getXAdjustment();
        if (left < adjustedLeft) {
          adjustedLeft = left;
        }
        if (right > adjustedRight) {
          adjustedRight = right;
        }
        if (row.getTop() < top)
          top = row.getTop();
        if (row.getBottom() > bottom)
          bottom = row.getBottom();
      }

      if (sourceImage.isLeftToRight()) {
        this.startMargin = adjustedLeft;
        this.endMargin = adjustedRight;
      } else {
        this.startMargin = adjustedRight;
        this.endMargin = adjustedLeft;
      }
    }

    @Override
    public String toString() {
      return "Column [adjustedLeft=" + (int) Math.round(adjustedLeft) + ", adjustedRight=" + (int) Math.round(adjustedRight) + ", top=" + top
          + ", bottom=" + bottom + "]";
    }

    @Override
    public int compareTo(Column o) {
      if (this.equals(o))
        return 0;

      boolean verticalOverlap = this.top < o.bottom && o.top < this.bottom;
      if (sourceImage.isLeftToRight()) {
        if (this.adjustedRight < o.adjustedLeft && verticalOverlap) {
          return -1;
        } else if (o.adjustedRight < this.adjustedLeft && verticalOverlap) {
          return 1;
        } else if (this.top < o.top) {
          return -1;
        } else {
          return 1;
        }
      } else {
        if (this.adjustedLeft > o.adjustedRight && verticalOverlap) {
          return -1;
        } else if (o.adjustedLeft > this.adjustedRight && verticalOverlap) {
          return 1;
        } else if (this.top < o.top) {
          return -1;
        } else {
          return 1;
        }

      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      long temp;
      temp = Double.doubleToLongBits(adjustedLeft);
      result = prime * result + (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(adjustedRight);
      result = prime * result + (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(bottom);
      result = prime * result + (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(top);
      result = prime * result + (int) (temp ^ (temp >>> 32));
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (getClass() != obj.getClass())
        return false;
      Column other = (Column) obj;
      if (Double.doubleToLongBits(adjustedLeft) != Double.doubleToLongBits(other.adjustedLeft))
        return false;
      if (Double.doubleToLongBits(adjustedRight) != Double.doubleToLongBits(other.adjustedRight))
        return false;
      if (Double.doubleToLongBits(bottom) != Double.doubleToLongBits(other.bottom))
        return false;
      if (Double.doubleToLongBits(top) != Double.doubleToLongBits(other.top))
        return false;
      return true;
    }

    public int getWidth() {
      return (int) (adjustedRight - adjustedLeft + 1);
    }

    @SuppressWarnings("unused")
    public int getHeight() {
      return (int) (bottom - top + 1);
    }

  }

  /**
   * Draw an image of the segmentation performed.
   */
  void drawSegmentation(SourceImage sourceImage) {
    LOG.debug("########## drawSegmentation #########");
    for (Paragraph paragraph : sourceImage.getParagraphs()) {
      for (RowOfShapes row : paragraph.getRows()) {
        int[] lastMeanLine = null;
        int[] lastBaseLine = null;

        for (GroupOfShapes group : row.getGroups()) {
          int groupLeft = 0;
          int groupTop = 0;
          int groupRight = 0;
          int groupBottom = 0;
          boolean firstShape = true;
          graphics2D.setStroke(new BasicStroke(1));
          graphics2D.setPaint(Color.BLUE);
          for (Shape shape : group.getShapes()) {
            if (firstShape) {
              groupLeft = shape.getLeft();
              groupTop = shape.getTop();
              groupRight = shape.getRight();
              groupBottom = shape.getBottom();
              firstShape = false;
            } else {
              if (shape.getLeft() < groupLeft)
                groupLeft = shape.getLeft();
              if (shape.getTop() < groupTop)
                groupTop = shape.getTop();
              if (shape.getRight() > groupRight)
                groupRight = shape.getRight();
              if (shape.getBottom() > groupBottom)
                groupBottom = shape.getBottom();
            }
            graphics2D.drawRect(shape.getLeft(), shape.getTop(), shape.getWidth(), shape.getHeight());
          } // next shape
          groupLeft -= 2;
          groupTop -= 2;
          groupRight += 2;
          groupBottom += 2;
          graphics2D.setStroke(new BasicStroke(2));
          graphics2D.setPaint(Color.GREEN);
          graphics2D.drawRect(groupLeft, groupTop, (groupRight - groupLeft) + 1, (groupBottom - groupTop) + 1);

          graphics2D.setStroke(new BasicStroke(1));
          graphics2D.setPaint(Color.RED);

          if (lastBaseLine != null) {
            int xHeight = group.getBaseLine()[1] - group.getMeanLine()[1];
            int lastXHeight = lastBaseLine[1] - lastMeanLine[1];
            if (xHeight == lastXHeight) {
              if (sourceImage.isLeftToRight()) {
                graphics2D.drawLine(group.getMeanLine()[2], group.getMeanLine()[3], lastMeanLine[0], lastMeanLine[1]);
                graphics2D.drawLine(group.getBaseLine()[2], group.getBaseLine()[3], lastBaseLine[0], lastBaseLine[1]);
              } else {
                graphics2D.drawLine(group.getMeanLine()[0], group.getMeanLine()[1], lastMeanLine[2], lastMeanLine[3]);
                graphics2D.drawLine(group.getBaseLine()[0], group.getBaseLine()[1], lastBaseLine[2], lastBaseLine[3]);
              }
            }
          }
          graphics2D.drawLine(group.getMeanLine()[0], group.getMeanLine()[1], group.getMeanLine()[2], group.getMeanLine()[3]);
          graphics2D.drawLine(group.getBaseLine()[0], group.getBaseLine()[1], group.getBaseLine()[2], group.getBaseLine()[3]);
          lastBaseLine = group.getBaseLine();
          lastMeanLine = group.getMeanLine();
        } // next group

      } // next row
      graphics2D.setStroke(new BasicStroke(2));
      graphics2D.setPaint(Color.DARK_GRAY);
      graphics2D.drawRect(paragraph.getLeft() - 2, paragraph.getTop() - 2, paragraph.getRight() - paragraph.getLeft() + 4,
          paragraph.getBottom() - paragraph.getTop() + 4);
    } // next paragraph
  }

  Shape getShape(SourceImage sourceImage, WritableImageGrid mirror, int x, int y) {
    Shape shape = new Shape(sourceImage, x, y, jochreSession);

    this.getShape(sourceImage, shape, mirror, x, y, sourceImage.getSeparationThreshold());
    
    return shape;
  }
  
  void getShape(ImageGrid sourceImage, Shape shape, WritableImageGrid mirror, int x, int y, int blackThreshold) {
    // find xMax for this x
    int xMax = x+1;
    for ( ; x<=sourceImage.getWidth(); xMax++) {
      if (!sourceImage.isPixelBlack(xMax, y, blackThreshold)) {
        break;
      }
    }

    this.findContiguousPixels(sourceImage, mirror, shape, x, xMax, y, blackThreshold);
    LOG.trace("Got shape for pixel (" + x + "," + y + "): " + shape);
  }

  void findContiguousPixels(ImageGrid sourceImage, WritableImageGrid mirror, Shape shape, int xMin, int xMax, int y, int blackThreshold) {
    // let's imagine
    // 0 X 0 0 x x
    // x x x 0 0 x
    // 0 0 x x x x
    // so we have to go up and to the left to keep finding contiguous black
    // pixels.
    Stack<int[]> segmentStack = new Stack<int[]>();
    segmentStack.push(new int[] { xMin, xMax, y });

    while (!segmentStack.isEmpty()) {
      if (segmentStack.size() > maxShapeStackSize) {
        throw new SegmentationException("While finding contiguous shapes, stack to large");
      }
      
      int[] segment = segmentStack.pop();
      xMin = segment[0];
      xMax = segment[1];
      y = segment[2];
      if (LOG.isTraceEnabled()) {
        LOG.trace("Popping next segment, xMin=" + xMin + ", xMax=" + xMax + ", y=" +y);
      }
      // Add this pixel to the mirror so that we don't touch it again.
      for (int x=xMin; x<xMax; x++) {
        mirror.setPixel(x, y, 1);
      }
      if (xMin < shape.getLeft()) {
        shape.setLeft(xMin);
        if (LOG.isTraceEnabled()) LOG.trace("Set shape left to " + shape.getLeft());
      }
      if (xMax-1 > shape.getRight()) {
        shape.setRight(xMax - 1);
        if (LOG.isTraceEnabled()) LOG.trace("Set shape right to " + shape.getRight());
      }
      if (y > shape.getBottom()) {
        shape.setBottom(y);
        if (LOG.isTraceEnabled()) LOG.trace("Set shape bottom to " + shape.getBottom());
      }
      // we don't have to check top, cause it's all going
      // from top to bottom.
      
      for (int rely = y - 1; rely <= y + 1; rely+=2) {
        boolean inBlack = false;

        int currentStart = xMin;
        for (int relx = xMin-1; relx <= xMax; relx++) {
          if (mirror.getPixel(relx, rely) > 0)
            continue;

          if (sourceImage.isPixelBlack(relx, rely, blackThreshold)) {
            if (!inBlack) {
              // start of a new black segment
              currentStart = relx;
              inBlack = true;
            }
            // If it's the left-most point, go as far left as necessary
            if (relx == xMin - 1) {
              for (int i = xMin - 2; i >= 0; i--) {
                if (!sourceImage.isPixelBlack(i, rely, blackThreshold)) {
                  currentStart = i+1;
                  break;
                }
              }
            }
            // If it's the right-most point, go as far right as necessary
            if (relx == xMax) {
              int xEnd = xMax;
              for (int i = xMax + 1; i <= sourceImage.getWidth(); i++) {
                if (!sourceImage.isPixelBlack(i, rely, blackThreshold)) {
                  xEnd = i;
                  break;
                }
              }
              segmentStack.push(new int[]{currentStart, xEnd, rely});
              if (LOG.isTraceEnabled()) {
                LOG.trace("At xMax pushed " + Arrays.toString(segmentStack.peek()));
              }
            }
          } else {
            if (inBlack) {
              // end of the previous segment
              segmentStack.push(new int[]{currentStart, relx, rely});
              if (LOG.isTraceEnabled()) {
                LOG.trace("End of segment, pushed" + Arrays.toString(segmentStack.peek()));
              }
              inBlack = false;
            }
          }
        }
      }
    }
  }

  void splitShapes(SourceImage sourceImage, int fillFactor) {
    LOG.debug("########## splitShapes #########");
    // Cluster rows into rows of a similar height
    // Once we have this, we look for any shapes that are wider than average
    // and attempt to split them by looking for any bridges that are
    // considerable thinner
    // than the stroke thickness and yet have big pixel counts on either
    // side.

    // In order to split, we need four parameters
    // 1) minShapeWidth: the minimum shape width to consider for a split
    // 2) maxBridgeWidth: the maximum bridge width to use as a dividing
    // bridge between two shapes when splitting
    // 3) minLetterWeight: the minimum pixel count that can represent a
    // separate letter when splitting
    // 4) maxHorizontalOverlap: the maximum horizontal overlap between the
    // left-hand and right-hand shape

    // These parameters are different for different font sizes
    // Therefore, we first need to group the rows on the image into clusters
    // by height

    double imageShapeMean = sourceImage.getAverageShapeWidth();
    double maxWidthForSplit = imageShapeMean * 6.0; // avoid splitting
    // horizontal rules!

    Set<Set<RowOfShapes>> rowClusters = sourceImage.getRowClusters();
    for (Set<RowOfShapes> rowCluster : rowClusters) {
      LOG.debug("Analysing row cluster");
      // 1) minShapeWidth: calculate the minimum shape width to be
      // considered for splitting

      // first get the mean
      Mean meanWidth = new Mean();
      List<Shape> shapes = new ArrayList<Shape>();
      for (RowOfShapes row : rowCluster) {
        for (Shape shape : row.getShapes()) {
          meanWidth.increment(shape.getWidth());
          shapes.add(shape);
        }
      }
      double shapeWidthMean = meanWidth.getResult();
      LOG.debug("Mean width: " + shapeWidthMean);
      meanWidth.clear();

      // Note: there is much trial and error for these numbers
      // but the general guideline is that it is easier to deal downstream
      // with bad joins than with bad splits
      // so we prefer to err on the upper side
      double fillFactorScale = 0.15 * fillFactor;
      double widthForSplittingLower = shapeWidthMean * (1.6 + fillFactorScale);
      double widthForSplittingUpper = shapeWidthMean * (2.2 + fillFactorScale);

      LOG.debug("widthForSplittingLower: " + widthForSplittingLower);
      LOG.debug("widthForSplittingUpper: " + widthForSplittingUpper);
      LOG.debug("maxWidthForSplit: " + maxWidthForSplit);
      List<Shape> candidates = new ArrayList<Shape>();
      for (RowOfShapes row : rowCluster) {
        LOG.debug("Next row " + row.getIndex());
        for (Shape shape : row.getShapes()) {
          LOG.trace("Shape width " + shape.getWidth());
          if (shape.getWidth() > widthForSplittingLower && shape.getWidth() < maxWidthForSplit) {
            candidates.add(shape);
            LOG.debug("Found candidate with width " + shape.getWidth() + ": " + shape);
          }
        }
      }

      if (candidates.size() > 0) {
        // we'll take a random sampling of shapes for the next
        // parameters
        int sampleSize = 30;
        List<Shape> sample = this.getSample(rowCluster, sampleSize, true);

        Mean meanPixelCount = new Mean();
        Vectorizer vectorizer = new Vectorizer();
        List<Integer> thicknesses = new ArrayList<Integer>();
        for (Shape shape : sample) {
          BitSet bitset = shape.getBlackAndWhiteBitSet(sourceImage.getSeparationThreshold(), 0);
          meanPixelCount.increment(bitset.cardinality());
          List<LineSegment> vectors = vectorizer.vectorize(shape);

          int height = shape.getHeight();
          int sampleStep = (int) Math.ceil(height / 8);

          for (LineSegment vector : vectors) {
            List<Integer> vectorThickness = vector.getLineDefinition().findArrayListThickness(shape, vector.getStartX(), vector.getStartY(),
                vector.getLength(), sourceImage.getSeparationThreshold(), 0, sampleStep);
            thicknesses.addAll(vectorThickness);
          }

        }

        double pixelCountMean = meanPixelCount.getResult();

        Mean meanThickness = new Mean();
        for (int thickness : thicknesses) {
          meanThickness.increment(thickness);
        }
        double thicknessMean = meanThickness.getResult();

        meanThickness = new Mean();
        for (int thickness : thicknesses) {
          if (thickness < thicknessMean)
            meanThickness.increment(thickness);
        }

        thicknessMean = meanThickness.getResult();
        LOG.debug("thicknessMean: " + thicknessMean);

        // 2) maxBridgeWidth: the maximum bridge width to use as a
        // dividing bridge between two shapes when splitting
        double maxBridgeWidthLower = thicknessMean * 0.5;
        double maxBridgeWidthUpper = thicknessMean * 0.8;
        LOG.debug("maxBridgeWidthLower: " + maxBridgeWidthLower);
        LOG.debug("maxBridgeWidthUpper: " + maxBridgeWidthUpper);

        // 3) minLetterWeight: the minimum pixel count that can
        // represent a separate letter when splitting
        int minLetterWeight = (int) Math.floor(pixelCountMean / 4.0);
        LOG.debug("minLetterWeight: " + minLetterWeight);

        // 4) maxHorizontalOverlap: the maximum horizontal overlap
        // between the left-hand and right-hand shape
        int maxOverlap = (int) Math.ceil(shapeWidthMean / 8.0);
        LOG.debug("maxOverlap: " + maxOverlap);

        Map<Shape, List<Shape>> shapesToSplit = new Hashtable<Shape, List<Shape>>();
        for (Shape candidate : candidates) {
          LOG.debug("Trying to split candidate " + candidate);
          for (int y = 0; y < candidate.getHeight(); y++) {
            String line = "";
            if (y == candidate.getMeanLine())
              line += "M";
            else if (y == candidate.getBaseLine())
              line += "B";
            else
              line += y;
            for (int x = 0; x < candidate.getWidth(); x++) {
              if (candidate.isPixelBlack(x, y, sourceImage.getBlackThreshold()))
                line += "x";
              else
                line += "o";
            }
            LOG.debug(line);
          }
          if (candidate.getHeight() < 3.0 * maxBridgeWidthUpper) {
            LOG.debug("Shape too narrow - probably a long dash.");
            continue;
          }
          int maxBridgeWidth;
          if (candidate.getWidth() > widthForSplittingUpper)
            maxBridgeWidth = (int) Math.ceil(maxBridgeWidthUpper);
          else {
            // since many bridges are thicker than expected
            // add a rule that the thicker the bridge is, the wider
            // the image needs to be
            maxBridgeWidth = (int) Math.ceil(maxBridgeWidthLower + ((candidate.getWidth() - widthForSplittingLower)
                / (widthForSplittingUpper - widthForSplittingLower) * (maxBridgeWidthUpper - maxBridgeWidthLower)));
          }
          List<Shape> splitShapes = this.splitShape(candidate, sourceImage, maxBridgeWidth, minLetterWeight, maxOverlap);
          if (splitShapes.size() > 1) {
            LOG.debug("Split found");
            for (Shape splitShape : splitShapes) {
              splitShape.setRow(candidate.getRow());
            }
            shapesToSplit.put(candidate, splitShapes);
          }
        }

        LOG.debug("Replacing shapes with split shapes");
        List<RowOfShapes> rowsToReorder = new ArrayList<RowOfShapes>();
        for (Shape shape : shapesToSplit.keySet()) {
          List<Shape> newShapes = shapesToSplit.get(shape);
          RowOfShapes row = shape.getRow();
          row.removeShape(shape);
          row.addShapes(newShapes);
          rowsToReorder.add(row);
        }

        for (RowOfShapes row : rowsToReorder)
          row.reorderShapes();
      }
    }
    LOG.debug("splitShapes complete");
  }

  /**
   * Split a shape into 2 or more shapes, in the case where two letters have
   * been mistakenly joined together.
   * 
   * @param shape
   *            the shape to split
   * @param sourceImage
   *            the source image containing this shape
   * @param maxBridgeWidth
   *            maximum width of a bridge between the two letters (measured
   *            vertically)
   * @param minLetterWeight
   *            minimum pixel count for a shape portion to be counted a
   *            separate letter
   * @param maxOverlap
   *            maximum vertical overlap (in pixels) between a right-hand and
   *            left-hand shape to be counted as separate letters
   * @return List of Shape, where the list is empty if no split was performed
   */
  List<Shape> splitShape(Shape shape, SourceImage sourceImage, int maxBridgeWidth, int minLetterWeight, int maxOverlap) {
    LOG.debug("Trying to split shape: " + shape.toString());
    LOG.debug("maxBridgeWidth " + maxBridgeWidth);
    LOG.debug("minLetterWeight " + minLetterWeight);
    LOG.debug("maxOverlap " + maxOverlap);

    Collection<BridgeCandidate> bridgeCandidates = shape.getBridgeCandidates(maxBridgeWidth);

    if (bridgeCandidates.size() > 0) {
      // (B) weight of right shape & weight of left shape > a certain
      // threshold
      // (C) little overlap right boundary of left shape, left boundary of
      // right shape

      // check if the right and left weight of each bridge candidate is
      // sufficiently big
      LOG.debug("minLetterWeight: " + minLetterWeight);
      LOG.debug("maxOverlap: " + maxOverlap);
      LOG.debug("Eliminating candidates based on pixel count and overlap");
      Set<BridgeCandidate> candidatesToEliminate = new HashSet<BridgeCandidate>();
      for (BridgeCandidate candidate : bridgeCandidates) {
        LOG.debug("Bridge candidate: leftPixels = " + candidate.leftPixels + ", rightPixels = " + candidate.rightPixels);
        LOG.debug("leftShapeRightBoundary = " + candidate.leftShapeRightBoundary + ", rightShapeLeftBoundary = " + candidate.rightShapeLeftBoundary);
        boolean isBridge = true;
        if (candidate.rightPixels < minLetterWeight || candidate.leftPixels < minLetterWeight)
          isBridge = false;
        if (candidate.leftShapeRightBoundary - candidate.rightShapeLeftBoundary > maxOverlap)
          isBridge = false;
        if (!isBridge)
          candidatesToEliminate.add(candidate);
      }

      bridgeCandidates.removeAll(candidatesToEliminate);
      LOG.debug("Remaining bridge candidate size: " + bridgeCandidates.size());

    } // have candidates

    List<Shape> shapes = new ArrayList<Shape>();

    // apply any splits detected
    if (bridgeCandidates.size() > 0) {
      int[] startingPoint = shape.getStartingPoint();
      int startX = startingPoint[0];
      int startY = startingPoint[1];

      for (BridgeCandidate bridge : bridgeCandidates) {
        bridge.leftGroup.touched = false;
        bridge.rightGroup.touched = false;
      }

      // perform split
      for (BridgeCandidate bridge : bridgeCandidates) {
        Shape leftShape = new Shape(sourceImage, startX, startY, jochreSession);
        leftShape.setLeft(shape.getRight());
        leftShape.setRight(shape.getLeft());
        leftShape.setTop(shape.getBottom());
        leftShape.setBottom(shape.getTop());

        Shape rightShape = new Shape(sourceImage, startX, startY, jochreSession);
        rightShape.setLeft(shape.getRight());
        rightShape.setRight(shape.getLeft());
        rightShape.setTop(shape.getBottom());
        rightShape.setBottom(shape.getTop());

        Stack<VerticalLineGroup> groupStack = new Stack<VerticalLineGroup>();
        groupStack.push(bridge.leftGroup);
        while (!groupStack.isEmpty()) {
          VerticalLineGroup lineGroup = groupStack.pop();
          if (lineGroup.touched)
            continue;
          lineGroup.touched = true;
          LOG.debug("Touching group, pixelCount: " + lineGroup.pixelCount + ", leftBoundary: " + lineGroup.leftBoundary + ", rightBoundary: "
              + lineGroup.rightBoundary);
          if (shape.getLeft() + lineGroup.leftBoundary < leftShape.getLeft())
            leftShape.setLeft(shape.getLeft() + lineGroup.leftBoundary);
          if (shape.getLeft() + lineGroup.rightBoundary > leftShape.getRight())
            leftShape.setRight(shape.getLeft() + lineGroup.rightBoundary);
          if (shape.getTop() + lineGroup.topBoundary < leftShape.getTop())
            leftShape.setTop(shape.getTop() + lineGroup.topBoundary);
          if (shape.getTop() + lineGroup.bottomBoundary > leftShape.getBottom())
            leftShape.setBottom(shape.getTop() + lineGroup.bottomBoundary);
          for (BridgeCandidate leftCandidate : lineGroup.leftCandidates) {
            if (!bridge.equals(leftCandidate) && !(bridgeCandidates.contains(leftCandidate))) {
              groupStack.push(leftCandidate.leftGroup);
            }
          }
          for (BridgeCandidate rightCandidate : lineGroup.rightCandidates) {
            if (!bridge.equals(rightCandidate) && !(bridgeCandidates.contains(rightCandidate))) {
              groupStack.push(rightCandidate.rightGroup);
            }
          }
        } // next left group
        groupStack.push(bridge.rightGroup);
        while (!groupStack.isEmpty()) {
          VerticalLineGroup lineGroup = groupStack.pop();
          if (lineGroup.touched)
            continue;
          lineGroup.touched = true;
          LOG.debug("Touching group, pixelCount: " + lineGroup.pixelCount + ", leftBoundary: " + lineGroup.leftBoundary + ", rightBoundary: "
              + lineGroup.rightBoundary);
          if (shape.getLeft() + lineGroup.leftBoundary < rightShape.getLeft())
            rightShape.setLeft(shape.getLeft() + lineGroup.leftBoundary);
          if (shape.getLeft() + lineGroup.rightBoundary > rightShape.getRight())
            rightShape.setRight(shape.getLeft() + lineGroup.rightBoundary);
          if (shape.getTop() + lineGroup.topBoundary < rightShape.getTop())
            rightShape.setTop(shape.getTop() + lineGroup.topBoundary);
          if (shape.getTop() + lineGroup.bottomBoundary > rightShape.getBottom())
            rightShape.setBottom(shape.getTop() + lineGroup.bottomBoundary);
          for (BridgeCandidate leftCandidate : lineGroup.leftCandidates) {
            if (!bridge.equals(leftCandidate) && !(bridgeCandidates.contains(leftCandidate))) {
              groupStack.push(leftCandidate.leftGroup);
            }
          }
          for (BridgeCandidate rightCandidate : lineGroup.rightCandidates) {
            if (!bridge.equals(rightCandidate) && !(bridgeCandidates.contains(rightCandidate))) {
              groupStack.push(rightCandidate.rightGroup);
            }
          }
        } // next right group
        if (leftShape.getWidth() > 0) {
          LOG.debug("Adding left split: " + leftShape);
          shapes.add(leftShape);
        }
        if (rightShape.getWidth() > 0) {
          LOG.debug("Adding right split: " + rightShape);
          shapes.add(rightShape);
        }
      } // next bridge
    } // do we have any bridges?

    // TODO: we need to join split shapes back together when more than 1
    // split is applied
    // and the shape in the middle is too small on its own (< minPixelCount)
    return shapes;
  }

  /**
   * Should we or should we not draw the segmentation?
   */
  public boolean isDrawSegmentation() {
    return drawSegmentation;
  }

  public void setDrawSegmentation(boolean drawSegmentation) {
    this.drawSegmentation = drawSegmentation;
  }

  /**
   * The source image to be analysed.
   */
  public SourceImage getSourceImage() {
    return sourceImage;
  }

  /**
   * A representation of the segmentation performed, if isDrawSegmentation is
   * true.
   */
  public BufferedImage getSegmentedImage() {
    return segmentedImage;
  }

  /**
   * Get a random sample (with replacement) of shapes on this image.
   */
  List<Shape> getSample(Collection<RowOfShapes> rows, int sampleSize, boolean bigShapesOnly) {
    double minShapeWidth = 0;
    double minShapeHeight = 0;
    double maxShapeWidth = Double.MAX_VALUE;
    double maxShapeHeight = Double.MAX_VALUE;
    if (bigShapesOnly) {
      Mean widthMean = new Mean();
      Mean heightMean = new Mean();
      for (RowOfShapes row : rows) {
        for (Shape shape : row.getShapes()) {
          widthMean.increment(shape.getWidth());
          heightMean.increment(shape.getHeight());
        }
      }
      minShapeWidth = widthMean.getResult();
      minShapeHeight = heightMean.getResult();
      maxShapeWidth = minShapeWidth * 2.5;
      maxShapeHeight = minShapeHeight * 2.5;
    }
    List<Shape> sample = new ArrayList<Shape>(sampleSize);
    int countBad = 0;
    while (sample.size() < sampleSize) {
      if (countBad >= 10) {
        minShapeWidth = 0;
        minShapeHeight = 0;
        maxShapeWidth = Double.MAX_VALUE;
        maxShapeHeight = Double.MAX_VALUE;
      }
      double random = Math.random();
      int rowIndex = (int) Math.floor(random * rows.size());
      Iterator<RowOfShapes> iRows = rows.iterator();
      RowOfShapes row = null;
      for (int i = 0; i <= rowIndex; i++) {
        row = iRows.next();
      }
      random = Math.random();

      int index = (int) Math.floor(random * row.getShapes().size());
      Shape shape = row.getShapes().get(index);
      if (shape.getWidth() > minShapeWidth && shape.getHeight() > minShapeHeight && shape.getWidth() < maxShapeWidth
          && shape.getHeight() < maxShapeHeight) {
        sample.add(shape);
        countBad = 0;
      } else {
        countBad++;
      }
    }
    return sample;
  }

  int getShapeCount(SourceImage sourceImage) {
    int totalShapeCount = 0;
    for (Paragraph paragraph : sourceImage.getParagraphs()) {
      for (RowOfShapes row : paragraph.getRows()) {
        for (GroupOfShapes group : row.getGroups()) {
          totalShapeCount += group.getShapes().size();
        }
      }
    }
    return totalShapeCount;
  }

  @Override
  public ProgressMonitor monitorTask() {
    currentMonitor = new SimpleProgressMonitor();
    return currentMonitor;
  }

  /**
   * Should we split and join shapes initially or not. If not, it is assumed a
   * machine learning algorithm will do this later for us.
   */
  public boolean isSplitAndJoin() {
    return splitAndJoin;
  }

  public void setSplitAndJoin(boolean splitAndJoin) {
    this.splitAndJoin = splitAndJoin;
  }

}
