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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;

class RowOfShapesImpl implements RowOfShapesInternal {
	private static final Logger LOG = LoggerFactory.getLogger(RowOfShapesImpl.class);

	private int id;

	private List<Shape> shapes;
	private List<GroupOfShapes> groups;
	private int index;

	private int paragraphId;
	private Paragraph paragraph;
	private SourceImage container;

	private GraphicsServiceInternal graphicsService;

	private Mean heightMean = null;

	private boolean coordinatesFound = false;
	private int left;
	private int top;
	private int right;
	private int bottom;
	private int xHeight;
	private int xHeightMax;
	private int maxShapeWidth = 0;
	private double xAdjustment = 0;
	private boolean xAdjustmentCalculated = false;

	private BufferedImage image;

	boolean shapeStatisticsCalculated = false;
	double averageShapeWidth;
	double averageShapeWidthMargin;
	double averageShapeHeight;
	double averageShapeHeightMargin;

	private SimpleRegression regression;
	private Boolean junk = null;

	RowOfShapesImpl() {
	}

	public RowOfShapesImpl(SourceImage container) {
		this.container = container;
	}

	@Override
	public List<Shape> getShapes() {
		if (shapes == null) {
			shapes = new ArrayList<Shape>();
			if (this.id != 0) {
				for (GroupOfShapes group : this.getGroups()) {
					shapes.addAll(group.getShapes());
				}
			}
		}
		return shapes;
	}

	@Override
	public void reorderShapes() {
		Comparator<Shape> comparator = null;

		if (this.isLeftToRight())
			comparator = new ShapeLeftToRightComparator();
		else
			comparator = new ShapeRightToLeftComparator();

		TreeSet<Shape> shapeSet = new TreeSet<Shape>(comparator);
		shapeSet.addAll(this.getShapes());

		this.getShapes().clear();
		this.getShapes().addAll(shapeSet);
	}

	@Override
	public void addShape(Shape shape) {
		this.getShapes().add(shape);
		shape.setRow(this);
	}

	@Override
	public void addShapes(Collection<Shape> shapes) {
		this.getShapes().addAll(shapes);
		for (Shape shape : shapes)
			shape.setRow(this);
	}

	@Override
	public void removeShape(Shape shape) {
		this.getShapes().remove(shape);
	}

	@Override
	public int getShapeIndex(Shape shape) {
		int i = 0;
		for (Shape oneShape : this.getShapes()) {
			if (oneShape.equals(shape)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	@Override
	public List<GroupOfShapes> getGroups() {
		if (groups == null) {
			if (this.id == 0)
				groups = new ArrayList<GroupOfShapes>();
			else {
				groups = this.graphicsService.findGroups(this);
				// greedily add the shapes to avoid multiple SQL calls
				List<Shape> shapes = this.graphicsService.findShapes(this);
				for (GroupOfShapes group : groups) {
					((GroupOfShapesInternal) group).setRow(this);
					((GroupOfShapesInternal) group).addShapes(shapes);
				}
			}
		}
		return groups;
	}

	@Override
	public GroupOfShapes newGroup() {
		GroupOfShapesInternal group = graphicsService.getEmptyGroupOfShapesInternal();
		group.setRow(this);
		this.getGroups().add(group);
		return group;
	}

	@Override
	public void addGroup(GroupOfShapes group) {
		GroupOfShapesInternal iGroup = (GroupOfShapesInternal) group;
		iGroup.setRow(this);
		this.addShapes(iGroup.getShapes());
		this.getGroups().add(group);
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public void setIndex(int index) {
		this.index = index;
	}

	public GraphicsServiceInternal getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsServiceInternal graphicsService) {
		this.graphicsService = graphicsService;
	}

	@Override
	public void save() {
		if (this.paragraph != null && this.paragraphId == 0)
			this.paragraphId = this.paragraph.getId();

		this.graphicsService.saveRowOfShapes(this);
		if (this.groups != null) {
			int index = 0;
			for (GroupOfShapes group : this.groups) {
				group.setIndex(index++);
				group.save();
			}
		}
	}

	@Override
	public int getParagraphId() {
		return paragraphId;
	}

	@Override
	public void setParagraphId(int paragraphId) {
		this.paragraphId = paragraphId;
	}

	@Override
	public Paragraph getParagraph() {
		if (this.paragraph == null && this.paragraphId != 0) {
			this.paragraph = this.graphicsService.loadParagraph(paragraphId);
		}
		return paragraph;
	}

	@Override
	public void setParagraph(Paragraph paragraph) {
		this.paragraph = paragraph;
		if (paragraph != null)
			this.setParagraphId(paragraph.getId());
		else
			this.setParagraphId(0);
	}

	@Override
	public double getMeanHeight() {
		if (this.heightMean == null) {
			this.heightMean = new Mean();
			for (Shape shape : this.getShapes()) {
				this.heightMean.increment(shape.getHeight());
			}
		}
		return this.heightMean.getResult();
	}

	@Override
	public double getBaseLineMiddlePoint() {
		double xMidPoint = (((double) this.getRight() + (double) this.getLeft()) / 2.0);
		Shape midShape = this.findNearestShape((int) Math.round(xMidPoint));
		double yMidPoint = 0;
		if (midShape != null)
			yMidPoint = midShape.getTop() + midShape.getBaseLine();
		else
			yMidPoint = (((double) this.getBottom() + (double) this.getTop()) / 2.0);
		return yMidPoint;
	}

	Point2D.Double getIntersectionPoint(Line2D.Double line1, Line2D.Double line2) {
		// if (! line1.intersectsLine(line2) ) return null;

		double px = line1.getX1(), py = line1.getY1(), rx = line1.getX2() - px, ry = line1.getY2() - py;
		double qx = line2.getX1(), qy = line2.getY1(), sx = line2.getX2() - qx, sy = line2.getY2() - qy;

		double det = sx * ry - sy * rx;
		if (det == 0) {
			return null;
		} else {
			double z = (sx * (qy - py) + sy * (px - qx)) / det;
			// if (z==0 || z==1) return null; // intersection at end point!
			return new Point2D.Double(px + z * rx, py + z * ry);
		}
	}

	JochreImage getJochreImage() {
		if (this.container != null) {
			return this.container;
		}
		return this.getParagraph().getImage();
	}

	@Override
	public SourceImage getContainer() {
		return container;
	}

	@Override
	public void setContainer(SourceImage container) {
		this.container = container;
	}

	@Override
	public Shape findNearestShape(int xCoordinate) {
		Shape nearestShape = null;
		int smallestDistance = -1;
		for (Shape shape : this.getShapes()) {
			if (shape.getLeft() <= xCoordinate && xCoordinate <= shape.getRight()) {
				nearestShape = shape;
				break;
			}
			if (nearestShape == null || Math.abs(xCoordinate - shape.getLeft()) <= smallestDistance) {
				smallestDistance = Math.abs(xCoordinate - shape.getLeft());
				nearestShape = shape;
			}
			if (Math.abs(xCoordinate - shape.getRight()) <= smallestDistance) {
				smallestDistance = Math.abs(xCoordinate - shape.getRight());
				nearestShape = shape;
			}
			if (!nearestShape.equals(shape)) {
				// as soon as we start getting farther away from the
				// x-coordinate, we can break out
				break;
			}
		}
		return nearestShape;
	}

	@Override
	public Shape findEnclosingShape(int xCoordinate) {
		Shape nearestShape = null;
		for (Shape shape : this.getShapes()) {
			if (shape.getLeft() <= xCoordinate && xCoordinate <= shape.getRight()) {
				nearestShape = shape;
				break;
			}
		}
		return nearestShape;
	}

	@Override
	public void recalculate() {
		this.heightMean = null;
		this.coordinatesFound = false;

		this.regression = null;
		this.shapeStatisticsCalculated = false;
		this.xAdjustmentCalculated = false;
		this.maxShapeWidth = 0;
	}

	@Override
	public int getLeft() {
		this.findCoordinates();
		return this.left;
	}

	@Override
	public int getTop() {
		this.findCoordinates();
		return this.top;
	}

	@Override
	public int getRight() {
		this.findCoordinates();
		return this.right;
	}

	@Override
	public int getBottom() {
		this.findCoordinates();
		return this.bottom;
	}

	boolean isLeftToRight() {
		boolean leftToRight = true;
		if (this.container != null)
			leftToRight = this.container.isLeftToRight();
		else
			leftToRight = this.getParagraph().getImage().getPage().getDocument().isLeftToRight();
		return leftToRight;
	}

	private void findCoordinates() {
		if (!coordinatesFound) {
			left = Integer.MAX_VALUE;
			top = Integer.MAX_VALUE;
			right = Integer.MIN_VALUE;
			bottom = Integer.MIN_VALUE;

			for (Shape shape : this.getShapes()) {
				if (shape.getLeft() < left)
					left = shape.getLeft();
				if (shape.getTop() < top)
					top = shape.getTop();
				if (shape.getRight() > right)
					right = shape.getRight();
				if (shape.getBottom() > bottom)
					bottom = shape.getBottom();
			}
			coordinatesFound = true;
		}
	}

	@Override
	public BufferedImage getImage() {
		if (this.image == null && this.container != null) {
			int buffer = 5;
			int width = this.container.getOriginalImage().getWidth();
			int height = this.getBottom() - this.getTop() + 1 + (buffer * 2);
			int bottom = (this.getTop() - buffer) + height;

			if (bottom > this.container.getOriginalImage().getHeight()) {
				int overlap = bottom - this.container.getOriginalImage().getHeight();
				height = height - overlap;
			}
			BufferedImage rowImage = this.container.getOriginalImage().getSubimage(0, this.getTop() - buffer, width, height);

			Graphics2D graphics2d = rowImage.createGraphics();

			// white out the space to the right & left of this row
			graphics2d.setColor(Color.WHITE);
			graphics2d.fillRect(0, 0, this.getLeft() - 5, height);
			graphics2d.fillRect(this.getRight() + 5, 0, width - this.getRight(), height);
			this.image = rowImage;

		}
		return image;
	}

	@Override
	public void setImage(BufferedImage image) {
		this.image = image;
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
			RowOfShapes otherRow = (RowOfShapes) obj;
			return (this.getId() == otherRow.getId());
		}
	}

	@Override
	public void clearMemory() {
		this.image = null;
	}

	@Override
	public double getAverageShapeWidth() {
		this.calculateShapeStatistics();
		return this.averageShapeWidth;
	}

	@Override
	public double getAverageShapeWidthMargin() {
		this.calculateShapeStatistics();
		return this.averageShapeWidthMargin;
	}

	@Override
	public double getAverageShapeHeight() {
		this.calculateShapeStatistics();
		return this.averageShapeHeight;
	}

	@Override
	public double getAverageShapeHeightMargin() {
		this.calculateShapeStatistics();
		return this.averageShapeHeightMargin;
	}

	void calculateShapeStatistics() {
		if (!shapeStatisticsCalculated) {
			DescriptiveStatistics shapeWidthStats = new DescriptiveStatistics();
			DescriptiveStatistics shapeHeightStats = new DescriptiveStatistics();

			for (Shape shape : this.getShapes()) {
				shapeWidthStats.addValue(shape.getWidth());
				shapeHeightStats.addValue(shape.getHeight());
			}

			double minWidth = shapeWidthStats.getPercentile(33);
			double maxWidth = shapeWidthStats.getPercentile(66);
			double minHeight = shapeHeightStats.getPercentile(33);
			double maxHeight = shapeHeightStats.getPercentile(66);
			this.averageShapeWidth = shapeWidthStats.getPercentile(50);
			this.averageShapeHeight = shapeHeightStats.getPercentile(50);

			this.averageShapeWidthMargin = (maxWidth - minWidth) / 2.0;
			this.averageShapeHeightMargin = (maxHeight - minHeight) / 2.0;

			this.shapeStatisticsCalculated = true;
		}
	}

	/**
	 * The regression passes through the bottom of average shapes on this line. It
	 * gives the line's slope, and a starting point for finding the baseline and
	 * meanline.
	 */
	@Override
	public SimpleRegression getRegression() {
		if (this.regression == null) {
			// begin by calculating some sort of average line crossing the whole
			// row, so that we can see if the row is
			// rising or falling to start with?
			// Calculate the line crossing the mid-point of all "average" shapes
			// on this row
			// get the "smoothed" linear approximation of the mid-points
			regression = new SimpleRegression();

			int numShapes = 0;
			int minShapes = 10;
			DescriptiveStatistics shapeWidthStats = new DescriptiveStatistics();
			DescriptiveStatistics shapeHeightStats = new DescriptiveStatistics();

			for (Shape shape : this.getShapes()) {
				shapeWidthStats.addValue(shape.getWidth());
				shapeHeightStats.addValue(shape.getHeight());
			}

			double minWidth = shapeWidthStats.getPercentile(25);
			double maxWidth = shapeWidthStats.getPercentile(75);
			double minHeight = shapeHeightStats.getPercentile(25);
			double maxHeight = shapeHeightStats.getPercentile(75);

			for (Shape shape : this.getShapes()) {
				// only add points whose shape is of "average" width and height
				// (to leave out commas, etc.)
				if (shape.getWidth() >= minWidth && shape.getWidth() <= maxWidth && shape.getHeight() >= minHeight && shape.getHeight() <= maxHeight) {

					// using bottom only, since rows with different font sizes
					// tend to align bottom
					regression.addData((((double) shape.getLeft() + (double) shape.getRight()) / 2.0), (shape.getBottom()));
					numShapes++;
				}
			}

			// special case where row contains very few shapes (generally letter
			// or number + period)
			boolean horizontalLine = false;
			if (numShapes < minShapes) {
				LOG.debug("Too few shapes: " + numShapes + ", assuming straight horizontal line");
				horizontalLine = true;
			} else if ((this.getRight() - this.getLeft()) < (this.getContainer().getWidth() / 6.0)) {
				LOG.debug("Too narrow: " + (this.getRight() - this.getLeft()) + ", assuming straight horizontal line");
				horizontalLine = true;
			}
			if (horizontalLine) {
				// assume a straight horizontal line
				Mean midPointMean = new Mean();
				for (Shape shape : this.getShapes()) {
					// only add points whose shape is of "average" height (to
					// leave out commas, etc.)
					if (shape.getWidth() >= minWidth && shape.getWidth() <= maxWidth && shape.getHeight() >= minHeight && shape.getHeight() <= maxHeight) {
						midPointMean.increment(shape.getBottom());
					}
				}
				if (midPointMean.getN() == 0) {
					for (Shape shape : this.getShapes()) {
						midPointMean.increment(shape.getBottom());
					}
				}
				double meanMidPoint = midPointMean.getResult();
				regression = new SimpleRegression();
				regression.addData(this.getLeft(), meanMidPoint);
				regression.addData(this.getRight(), meanMidPoint);
			}

			// displays intercept of regression line
			LOG.debug("intercept: " + regression.getIntercept());

			// displays slope of regression line
			LOG.debug("slope: " + regression.getSlope());

			// displays slope standard error
			LOG.debug("std err: " + regression.getSlopeStdErr());

			LOG.debug("x = 0, y = " + regression.predict(0));
			LOG.debug("x = " + this.getContainer().getWidth() + ", y = " + regression.predict(this.getContainer().getWidth()));
		}
		return regression;
	}

	@Override
	public void assignGuideLines() {
		LOG.debug("assignGuideLines, " + this.toString());
		int xHeight = this.assignGuideLines(null);
		LOG.debug("Setting xHeight and xHeightMax to " + xHeight);
		this.setXHeight(xHeight);
		this.setXHeightMax(xHeight);
	}

	/**
	 * Assign guidelines for a certain subset of shapes, and return the x-height.
	 */
	int assignGuideLines(List<GroupOfShapes> groupsToAssign) {
		LOG.debug("assignGuideLines internal");
		double meanHorizontalSlope = this.getContainer().getMeanHorizontalSlope();

		// the base-line and mean-line will be at a fixed distance away from the
		// midpoint
		// the question is, which distance!
		// To find this out, we count number of black pixels on each row above
		// this line
		// And then start analysing from the top and the bottom until the number
		// drops off sharply

		// The notion of "groupsToAssign" is used to only assign guidelines
		// to a subset of the groups on the line
		// when the line contains two different font sizes
		List<Shape> shapes = new ArrayList<Shape>();
		if (groupsToAssign != null) {
			for (GroupOfShapes group : groupsToAssign) {
				shapes.addAll(group.getShapes());
			}
		} else {
			shapes = this.getShapes();
		}

		int i = 0;
		DescriptiveStatistics shapeWidthStats = new DescriptiveStatistics();
		DescriptiveStatistics shapeHeightStats = new DescriptiveStatistics();

		for (Shape shape : this.getShapes()) {
			shapeWidthStats.addValue(shape.getWidth());
			shapeHeightStats.addValue(shape.getHeight());
		}

		double minWidth = shapeWidthStats.getPercentile(25);
		double maxWidth = shapeWidthStats.getPercentile(75);
		double minHeight = shapeHeightStats.getPercentile(45);
		double maxHeight = shapeHeightStats.getPercentile(75);

		double rowMidPointX = (this.getLeft() + this.getRight()) / 2.0;

		// calculating the Y midpoint by the shapes in the row, instead of by
		// the top & bottom of row
		Mean rowMidPointYMean = new Mean();
		for (Shape shape : this.getShapes()) {
			// only add points whose shape is of "average" width and height (to
			// leave out commas, etc.)
			if (shape.getWidth() >= minWidth && shape.getWidth() <= maxWidth && shape.getHeight() >= minHeight && shape.getHeight() <= maxHeight) {
				rowMidPointYMean.increment((shape.getBottom() + shape.getTop()) / 2.0);
			}
		}

		double rowMidPointY = (this.getTop() + this.getBottom()) / 2.0;
		if (rowMidPointYMean.getN() > 0)
			rowMidPointY = rowMidPointYMean.getResult();
		LOG.debug("rowMidPointX: " + rowMidPointX);
		LOG.debug("rowMidPointY: " + rowMidPointY);

		// figure out where the top-most shape starts and the bottom-most shape
		// ends, relative to the y midline
		int minTop = Integer.MAX_VALUE;
		int maxBottom = Integer.MIN_VALUE;
		List<Integer> rowYMidPoints = new ArrayList<Integer>(shapes.size());
		for (Shape shape : shapes) {
			double shapeMidPointX = (shape.getLeft() + shape.getRight()) / 2.0;
			int shapeMidPointY = (int) Math.round(rowMidPointY + (meanHorizontalSlope * (shapeMidPointX - rowMidPointX)));
			rowYMidPoints.add(shapeMidPointY);

			int relativeTop = shape.getTop() - shapeMidPointY;
			int relativeBottom = shape.getBottom() - shapeMidPointY;

			if (relativeTop < minTop)
				minTop = relativeTop;
			if (relativeBottom > maxBottom)
				maxBottom = relativeBottom;
		}
		if (minTop > 0)
			minTop = 0;
		if (maxBottom < 0)
			maxBottom = 0;

		int yIntervalTop = 0 - minTop;
		int yIntervalBottom = maxBottom;
		int yInterval = yIntervalTop + 1 + yIntervalBottom;
		LOG.debug("yIntervalTop: " + yIntervalTop);
		LOG.debug("yIntervalBottom: " + yIntervalBottom);
		LOG.debug("yInterval: " + yInterval);
		int[] pixelCounts = new int[yInterval];

		// Get the pixel count for each row
		// examining one shape at a time to limit ourselves to the pixels that
		// are
		// actually considered to be in this row
		int blackThreshold = this.getContainer().getSeparationThreshold();
		int shapeIndex = 0;
		int shapeCount = 0;
		for (Shape shape : shapes) {
			if (shape.getHeight() >= minHeight) {
				LOG.trace(shape.toString());
				shapeCount++;
				int shapeMidPointY = rowYMidPoints.get(shapeIndex);
				int zeroLine = shapeMidPointY - yIntervalTop;
				int topIndex = shape.getTop() - zeroLine;
				for (int x = 0; x < shape.getWidth(); x++) {
					for (int y = 0; y < shape.getHeight(); y++) {
						int yIndex = topIndex + y;
						if (yIndex >= 0 && yIndex < pixelCounts.length && shape.isPixelBlack(x, y, blackThreshold)) {
							pixelCounts[yIndex]++;
						}
					}
				}
			}
			shapeIndex++;
		}
		LOG.debug("Got pixels from " + shapeCount + " shapes.");

		boolean notEnoughShapes = shapeCount < 3;
		LOG.debug("notEnoughShapes? " + notEnoughShapes);

		// We start at the top
		// As soon as we reach a line with more pixels than the mean, we assume
		// this is the mean-line
		Mean pixelCountMeanTop = new Mean();
		StandardDeviation pixelCountStdDevTop = new StandardDeviation();
		for (i = 0; i <= yIntervalTop; i++) {
			pixelCountMeanTop.increment(pixelCounts[i]);
			pixelCountStdDevTop.increment(pixelCounts[i]);
		}
		LOG.debug("Top: pixel count mean: " + pixelCountMeanTop.getResult() + ", std dev: " + pixelCountStdDevTop.getResult());

		double threshold = pixelCountMeanTop.getResult() * 1.1;
		if (notEnoughShapes) {
			threshold = threshold / 2.0;
		}
		double lowerThreshold = threshold / 2.0;

		LOG.debug("Top threshold: " + threshold);
		LOG.debug("Top lowerThreshold: " + lowerThreshold);

		int meanLine = 0;
		boolean findMeanLine = true;
		for (i = 0; i <= yIntervalTop; i++) {
			int pixelCount = pixelCounts[i];
			if (findMeanLine && pixelCount > threshold) {
				meanLine = i;
				findMeanLine = false;
			} else if (!findMeanLine && pixelCount < lowerThreshold) {
				findMeanLine = true;
			}
		}

		// We start at the bottom
		// As soon as we reach a line with more pixels than the mean, we assume
		// this is the base-line

		Mean pixelCountMeanBottom = new Mean();
		StandardDeviation pixelCountStdDevBottom = new StandardDeviation();
		for (i = pixelCounts.length - 1; i >= yIntervalTop; i--) {
			pixelCountMeanBottom.increment(pixelCounts[i]);
			pixelCountStdDevBottom.increment(pixelCounts[i]);
		}
		LOG.debug("Bottom: pixel count mean: " + pixelCountMeanBottom.getResult() + ", std dev: " + pixelCountStdDevBottom.getResult());

		threshold = pixelCountMeanBottom.getResult() * 1.1;
		if (notEnoughShapes) {
			threshold = threshold / 2.0;
		}
		lowerThreshold = threshold / 2.0;

		LOG.debug("Bottom threshold: " + threshold);
		LOG.debug("Bottom lowerThreshold: " + lowerThreshold);
		int baseLine = meanLine;
		boolean findBaseLine = true;
		for (i = pixelCounts.length - 1; i >= yIntervalTop; i--) {
			int pixelCount = pixelCounts[i];
			if (findBaseLine && pixelCount > threshold) {
				baseLine = i;
				findBaseLine = false;
			} else if (!findBaseLine && pixelCount < lowerThreshold) {
				findBaseLine = true;
			}
		}

		for (i = 0; i < yInterval; i++) {
			int pixelCount = pixelCounts[i];
			if (i == meanLine)
				LOG.trace("======= MEAN LINE " + i + " ==========");
			LOG.trace("pixel row " + i + ". pixel count " + pixelCount);
			if (i == baseLine)
				LOG.trace("======= BASE LINE " + i + " ==========");
		}

		// assign base lines and mean lines to each shape
		shapeIndex = 0;
		for (Shape shape : shapes) {
			int shapeMidPointY = rowYMidPoints.get(shapeIndex);
			int yMeanline = (shapeMidPointY - yIntervalTop) + meanLine;
			int yBaseline = (shapeMidPointY - yIntervalTop) + baseLine;
			LOG.trace(shape.toString() + ", meanLine: " + (yMeanline - shape.getTop()) + ", baseLine: " + (yBaseline - shape.getTop()));
			shape.setBaseLine(yBaseline - shape.getTop());
			shape.setMeanLine(yMeanline - shape.getTop());
			shapeIndex++;
		} // next shape

		int xHeight = baseLine - meanLine;
		return xHeight;
	}

	@Override
	public void organiseShapesInGroups(double letterSpaceThreshold) {
		LOG.debug("organiseShapesInGroups, " + this.toString());

		Shape previousShape = null;
		GroupOfShapes currentGroup = this.newGroup();
		if (LOG.isTraceEnabled())
			LOG.trace("New word");
		int i = 1;
		for (Shape shape : this.getShapes()) {
			if (previousShape != null) {
				int space = 0;
				if (this.getContainer().isLeftToRight())
					space = shape.getLeft() - previousShape.getRight();
				else
					space = previousShape.getLeft() - shape.getRight();
				if (LOG.isTraceEnabled())
					LOG.trace("Space: " + space + ", threshold: " + letterSpaceThreshold);
				if (space > letterSpaceThreshold) {
					// new word
					if (LOG.isTraceEnabled())
						LOG.trace("New word");
					currentGroup = this.newGroup();
					currentGroup.setIndex(i++);
				}
			}
			if (LOG.isTraceEnabled())
				LOG.trace(shape.toString());
			currentGroup.addShape(shape);
			previousShape = shape;
		} // next shape

		for (GroupOfShapes group : this.getGroups()) {
			int j = 0;
			for (Shape shape : group.getShapes()) {
				shape.setIndex(j++);
			}
		}
	}

	@Override
	public int getXHeight() {
		return xHeight;
	}

	@Override
	public void setXHeight(int height) {
		this.xHeight = height;
	}

	@Override
	public String toString() {
		return "Row " + this.getIndex() + ", left(" + this.getLeft() + ")" + ", top(" + this.getTop() + ")" + ", right(" + this.getRight() + ")" + ", bot("
				+ this.getBottom() + ")";
	}

	/**
	 * If there are different font-sizes in the current row, calculate separate
	 * guidelines for the separate font-sizes. Assumes groups have already been
	 * assigned.
	 * 
	 * @return index of first group after split
	 */
	@Override
	public void splitByFontSize() {
		LOG.debug("splitByFontSize, " + this.toString());
		double[] meanAscenderToXHeightRatios = new double[this.getGroups().size()];
		int i = 0;
		double xHeight = this.getXHeight();
		double minHeightRatio = 0.7;
		for (GroupOfShapes group : this.getGroups()) {
			Mean meanAscenderToXHeightRatio = new Mean();
			for (Shape shape : group.getShapes()) {
				if ((shape.getHeight() / xHeight) > minHeightRatio) {
					double ascenderToXHeightRatio = (shape.getBaseLine() / xHeight);
					LOG.trace("Shape " + shape.getIndex() + ": " + ascenderToXHeightRatio);
					meanAscenderToXHeightRatio.increment(ascenderToXHeightRatio);
				}
			}
			if (meanAscenderToXHeightRatio.getN() > 0) {
				meanAscenderToXHeightRatios[i] = meanAscenderToXHeightRatio.getResult();
				LOG.debug(group.toString() + ": " + meanAscenderToXHeightRatios[i]);
			}
			i++;
		}

		double threshold = 0.15;
		LOG.debug("threshold: " + threshold);

		double lastRatio = 0;

		List<int[]> bigAreas = new ArrayList<int[]>();
		int bigAreaStart = 0;
		int inBigArea = -1;
		for (i = 0; i < this.getGroups().size(); i++) {
			if (i > 0) {
				if (meanAscenderToXHeightRatios[i] != 0) {
					if ((inBigArea < 0 || inBigArea == 1) && lastRatio - meanAscenderToXHeightRatios[i] >= threshold) {
						// big drop
						int[] bigArea = new int[] { bigAreaStart, i - 1 };
						bigAreas.add(bigArea);
						LOG.debug("Adding big area " + bigArea[0] + "," + bigArea[1]);
						inBigArea = 0;
					} else if ((inBigArea < 0 || inBigArea == 0) && meanAscenderToXHeightRatios[i] - lastRatio >= threshold) {
						// big leap
						bigAreaStart = i;
						inBigArea = 1;
					}
				}
			}

			if (meanAscenderToXHeightRatios[i] != 0)
				lastRatio = meanAscenderToXHeightRatios[i];
		}
		if (inBigArea == 1) {
			int[] bigArea = new int[] { bigAreaStart, this.getGroups().size() - 1 };
			bigAreas.add(bigArea);
			LOG.debug("Adding big area " + bigArea[0] + "," + bigArea[1]);
		}

		// Now, which of these big areas are really big enough
		if (bigAreas.size() > 0) {
			double minBrightnessRatioForSplit = 1.5;
			Mean brightnessMean = new Mean();
			Mean[] meanCardinalities = new Mean[bigAreas.size()];
			for (i = 0; i < bigAreas.size(); i++) {
				meanCardinalities[i] = new Mean();
			}
			i = 0;
			for (GroupOfShapes group : this.getGroups()) {
				int bigAreaIndex = -1;
				int j = 0;
				for (int[] bigArea : bigAreas) {
					if (i >= bigArea[0] && i <= bigArea[1]) {
						bigAreaIndex = j;
						break;
					}
					j++;
				}
				for (Shape shape : group.getShapes()) {
					if ((shape.getHeight() / xHeight) > minHeightRatio) {
						if (bigAreaIndex >= 0) {
							meanCardinalities[bigAreaIndex].increment(shape.getTotalBrightness());
						} else {
							brightnessMean.increment(shape.getTotalBrightness());
						}
					}
				}
				i++;
			} // next group

			boolean[] bigAreaConfirmed = new boolean[bigAreas.size()];
			boolean hasSplit = false;
			LOG.debug("brightnessMean for small areas: " + brightnessMean.getResult());
			for (i = 0; i < bigAreas.size(); i++) {
				int[] bigArea = bigAreas.get(i);
				double ratio = meanCardinalities[i].getResult() / brightnessMean.getResult();
				LOG.debug("big area " + bigArea[0] + "," + bigArea[1]);
				LOG.debug("brightness mean: " + meanCardinalities[i].getResult());
				LOG.debug("brightness ratio: " + ratio);
				if (ratio > minBrightnessRatioForSplit) {
					// split found!
					LOG.debug("Confirmed!");
					bigAreaConfirmed[i] = true;
					hasSplit = true;
				}
			}

			List<GroupOfShapes> bigGroups = null;
			List<GroupOfShapes> littleGroups = null;

			if (hasSplit) {
				bigGroups = new ArrayList<GroupOfShapes>();
				littleGroups = new ArrayList<GroupOfShapes>();
				i = 0;
				boolean lastGroupSingleShapeLittle = false;
				boolean lastGroupBig = false;
				GroupOfShapes lastGroup = null;
				for (GroupOfShapes group : this.getGroups()) {
					boolean singleShapeLittleGroup = false;
					int bigAreaIndex = -1;
					int j = 0;
					for (int[] bigArea : bigAreas) {
						if (i >= bigArea[0] && i <= bigArea[1]) {
							bigAreaIndex = j;
							break;
						}
						j++;
					}
					if (bigAreaIndex >= 0 && bigAreaConfirmed[bigAreaIndex]) {
						if (lastGroupSingleShapeLittle) {
							// Can't keep single shape little groups on their
							// own
							LOG.debug("Switching last group to big: " + lastGroup.toString());
							littleGroups.remove(littleGroups.size() - 1);
							bigGroups.add(lastGroup);
						}
						LOG.debug("Adding big group " + group.toString());
						bigGroups.add(group);
						lastGroupBig = true;
					} else {
						LOG.debug("Adding little group " + group.toString());
						littleGroups.add(group);

						if (group.getShapes().size() == 1 && lastGroupBig) {
							singleShapeLittleGroup = true;
						}
						lastGroupBig = false;
					}
					lastGroupSingleShapeLittle = singleShapeLittleGroup;
					lastGroup = group;
					i++;
				} // next group

				hasSplit = bigGroups.size() > 0 && littleGroups.size() > 0;
			}

			if (hasSplit) {
				int xHeightBig = this.assignGuideLines(bigGroups);
				int xHeightLittle = this.assignGuideLines(littleGroups);

				// There may be a better way of determining which xHeight to use
				// for the row
				// than simply based on number of groups, e.g. group width, etc.
				if (bigGroups.size() > littleGroups.size()) {
					LOG.debug("Setting xHeight to " + xHeightBig);
					this.setXHeight(xHeightBig);
				} else {
					LOG.debug("Setting xHeight to " + xHeightLittle);
					this.setXHeight(xHeightLittle);
				}
				LOG.debug("Setting xHeightMax to " + xHeightBig);
				this.setXHeightMax(xHeightBig);
			} // has split
		} // split candidate

	}

	@Override
	public int getXHeightMax() {
		return xHeightMax;
	}

	public void setXHeightMax(int xHeightMax) {
		this.xHeightMax = xHeightMax;
	}

	@Override
	public int getMaxShapeWidth() {
		if (maxShapeWidth == 0) {
			for (Shape shape : this.getShapes()) {
				if (shape.getWidth() > maxShapeWidth) {
					maxShapeWidth = shape.getWidth();
				}
			}
		}
		return maxShapeWidth;
	}

	@Override
	public double getXAdjustment() {
		if (!xAdjustmentCalculated) {
			double rowVerticalMidPoint = this.getBaseLineMiddlePoint();
			xAdjustment = this.getContainer().getXAdjustment(rowVerticalMidPoint);

			xAdjustmentCalculated = true;
		}
		return xAdjustment;
	}

	public boolean isJunk() {
		if (junk == null) {
			if (this.getGroups().size() > 0) {
				double averageConfidence = 0;
				double shapeCount = 0;
				for (GroupOfShapes group : this.getGroups()) {
					if (group.getShapes().size() > 0) {
						for (Shape shape : group.getShapes()) {
							averageConfidence += shape.getConfidence();
							shapeCount += 1;
						}
					}
				}
				averageConfidence = averageConfidence / shapeCount;

				JochreSession jochreSession = JochreSession.getInstance();
				if (averageConfidence < jochreSession.getJunkConfidenceThreshold())
					junk = true;
				else
					junk = false;
			} else {
				junk = true;
			}
		}
		return junk;
	}

	@Override
	public int getWidth() {
		return right - left + 1;
	}

	@Override
	public int getHeight() {
		return bottom - top + 1;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

}
