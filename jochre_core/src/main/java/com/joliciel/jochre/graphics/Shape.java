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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.Entity;
import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.BoundaryDao;
import com.joliciel.jochre.boundaries.Split;
import com.joliciel.jochre.graphics.util.ImagePixelGrabber;
import com.joliciel.jochre.graphics.util.ImagePixelGrabberImpl;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.HasFeatureCache;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.utils.PersistentList;
import com.joliciel.talismane.utils.PersistentListImpl;

/**
 * A rectangle containing a shape that needs to be identified as a grapheme.
 * 
 * @author Assaf Urieli
 */
public class Shape implements ImageGrid, Entity, Rectangle, ShapeWrapper, HasFeatureCache {
	/**
	 * Different methods for measuring the brightness of various sections of the
	 * shape, where a section is a rectangle within a grid that has been overlaid
	 * onto the shape.
	 */
	public enum SectionBrightnessMeasurementMethod {
		/**
		 * A simple sum of the brightness for all pixels in each section, where
		 * white = 0 and black = 255.
		 */
		RAW,

		/**
		 * The sum provided by RAW, divided by the number of pixels in the section.
		 * Since sections can have different sizes, it's critical to normalise.
		 */
		SIZE_NORMALISED,

		/**
		 * The section with the maximum normalised brightness is taken to be 1. All
		 * other sections are given a value from 0 to 1, based on their normalised
		 * brightness relative to the max.
		 */
		RELATIVE_TO_MAX_SECTION,

		/**
		 * For each section, gives a value from 0 to 1, showing which portion of the
		 * total brightness it contains.
		 */
		PORTION_OF_TOTAL_BRIGHTNESS,
	}

	private static final Logger LOG = LoggerFactory.getLogger(Shape.class);
	private int id;

	private int top;
	private int left;
	private int bottom;
	private int right;

	private int baseLine;
	private int meanLine;
	private int capLine;

	private int index;

	private RowOfShapes row;
	private GroupOfShapes group;
	private int groupId;
	private JochreImage jochreImage;

	private String letter = "";
	private String originalGuess = "";

	private LetterGuesserService letterGuesserService;

	private Map<String, Map<SectionBrightnessMeasurementMethod, double[][]>> brightnessBySectorMap = new HashMap<String, Map<SectionBrightnessMeasurementMethod, double[][]>>();
	private Map<String, Map<SectionBrightnessMeasurementMethod, Double>> brightnessMeanBySectorMap = new HashMap<String, Map<SectionBrightnessMeasurementMethod, Double>>();

	private Map<String, FeatureResult<?>> featureResults = new HashMap<String, FeatureResult<?>>();

	private Dictionary<String, BitSet> bitsets = new Hashtable<String, BitSet>();
	private Dictionary<Integer, BitSet> outlines = new Hashtable<Integer, BitSet>();

	private int[] brightnessCounts;
	private boolean blackAndWhite = false;

	private int[] verticalCounts = null;
	private int[][] verticalContour = null;

	private BufferedImage image;
	private ImagePixelGrabber pixelGrabber;

	private boolean dirty = true;

	private Set<Decision> letterGuesses = null;
	private int totalBrightness = 0;

	private int[] startingPoint;

	TreeSet<VerticalLineSegment> lines = null;
	Collection<BridgeCandidate> bridgeCandidates = null;

	PersistentList<Split> splits = null;
	Double confidence = null;

	private final JochreSession jochreSession;
	private final GraphicsDao graphicsDao;

	Shape(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.graphicsDao = GraphicsDao.getInstance(jochreSession);
	}

	Shape(JochreImage container, JochreSession jochreSession) {
		this(jochreSession);
		this.jochreImage = container;
	}

	/**
	 * Get a shape which is a single pixel inside a given SourceImage.
	 */
	public Shape(JochreImage sourceImage, int x, int y, JochreSession jochreSession) {
		this(sourceImage, jochreSession);
		this.setLeft(x);
		this.setRight(x);
		this.setTop(y);
		this.setBottom(y);

		this.setStartingPoint(new int[] { x, y });
	}

	/**
	 * Get a shape which is a rectangle inside a given SourceImage.
	 */
	public Shape(JochreImage sourceImage, int left, int top, int right, int bottom, JochreSession jochreSession) {
		this(sourceImage, jochreSession);
		this.setLeft(left);
		this.setRight(right);
		this.setTop(top);
		this.setBottom(bottom);

		this.setStartingPoint(new int[] { left, top });
	}

	@Override
	public int getHeight() {
		return bottom - top + 1;
	}

	/**
	 * If the pixel is in the shape, return its brightness value. Otherwise,
	 * return zero.
	 */
	public int getPixelInShape(int x, int y) {
		if (x < 0 || x >= this.getWidth() || y < 0 || y >= this.getHeight())
			return 0;
		else
			return this.getPixel(x, y);
	}

	@Override
	public int getPixel(int x, int y) {
		if (this.image != null) {
			int pixel = this.getPixelGrabber().getPixelBrightness(x, y);
			return this.getJochreImage().normalize(pixel);
		} else {
			return jochreImage.getAbsolutePixel(left + x, top + y);
		}
	}

	@Override
	public int getAbsolutePixel(int x, int y) {
		if (this.image != null) {
			int pixel = this.getPixelGrabber().getPixelBrightness(x - this.left, y - this.top);
			return this.getJochreImage().normalize(pixel);
		} else
			return jochreImage.getAbsolutePixel(x, y);
	}

	@Override
	public int getRawPixel(int x, int y) {
		if (this.image != null) {
			int pixel = this.getPixelGrabber().getPixelBrightness(x, y);
			return pixel;
		} else
			return jochreImage.getRawAbsolutePixel(left + x, top + y);
	}

	@Override
	public int getRawAbsolutePixel(int x, int y) {
		if (this.image != null) {
			int pixel = this.getPixelGrabber().getPixelBrightness(x - this.left, y - this.top);
			return pixel;
		} else
			return jochreImage.getRawAbsolutePixel(x, y);
	}

	@Override
	public int getWidth() {
		return right - left + 1;
	}

	/**
	 * The topmost y coordinate of this shape within the containing ImageGrid.
	 */
	@Override
	public int getTop() {
		return top;
	}

	public void setTop(int top) {
		if (this.top != top) {
			this.top = top;
			this.dirty = true;
		}
	}

	/**
	 * The leftmost x coordinate of this shape within the containing ImageGrid.
	 */
	@Override
	public int getLeft() {
		return left;
	}

	public void setLeft(int left) {
		if (this.left != left) {
			this.left = left;
			this.dirty = true;
		}
	}

	@Override
	public int getBottom() {
		return bottom;
	}

	public void setBottom(int bottom) {
		if (this.bottom != bottom) {
			this.bottom = bottom;
			this.dirty = true;
		}
	}

	@Override
	public int getRight() {
		return right;
	}

	public void setRight(int right) {
		if (this.right != right) {
			this.right = right;
			this.dirty = true;
		}
	}

	/**
	 * The group of shapes containing this particular shape.
	 */
	public GroupOfShapes getGroup() {
		if (this.groupId != 0 && this.group == null) {
			this.group = this.graphicsDao.loadGroupOfShapes(this.groupId);
		}
		return group;
	}

	public void setGroup(GroupOfShapes group) {
		this.group = group;
		if (group != null)
			this.setGroupId(group.getId());
		else
			this.setGroupId(0);
	}

	/**
	 * The relative y-index of line at the bottom of standard lowercase letters.
	 */

	public int getBaseLine() {
		return baseLine;
	}

	public void setBaseLine(int baseLine) {
		if (this.baseLine != baseLine) {
			this.baseLine = baseLine;
			this.dirty = true;
		}
	}

	/**
	 * The relative y-index of the line at the top of standard lowercase letters.
	 */

	public int getMeanLine() {
		return meanLine;
	}

	public void setMeanLine(int meanLine) {
		if (this.meanLine != meanLine) {
			this.meanLine = meanLine;
			this.dirty = true;
		}
	}

	/**
	 * The relative y-index of the line at the top of standard uppercase letters.
	 */

	public int getCapLine() {
		return capLine;
	}

	public void setCapLine(int capLine) {
		if (this.capLine != capLine) {
			this.capLine = capLine;
			this.dirty = true;
		}
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		if (this.index != index) {
			this.index = index;
			this.dirty = true;
		}
	}

	@Override
	public void save() {
		if (this.group != null && this.groupId == 0)
			this.setGroupId(this.group.getId());

		if (this.dirty)
			this.graphicsDao.saveShape(this);

		if (this.splits != null) {
			for (Split split : this.splits.getItemsRemoved()) {
				split.delete();
			}
			for (Split split : this.splits) {
				split.save();
			}
		}

		this.getJochreImage().onSaveShape(this);
	}

	/**
	 * The letter represented by this shape (for shapes in the training set).
	 */
	public String getLetter() {
		return letter;
	}

	public void setLetter(String letter) {
		if (letter == null)
			letter = "";
		if (!this.letter.equals(letter)) {
			this.letter = letter;
			this.dirty = true;
		}
	}

	public void delete() {
		this.graphicsDao.deleteShape(this);
	}

	public RowOfShapes getRow() {
		return row;
	}

	public void setRow(RowOfShapes row) {
		this.row = row;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		if (this.groupId != groupId) {
			this.groupId = groupId;
			this.dirty = true;
		}
	}

	/**
	 * Divides the shape as follows: Above the meanline, there will be
	 * marginSectionCount rows by verticalSectionCount columns Between the
	 * meanline and baseline, there will be horizontalSectionCount rows by
	 * verticalSectionCount columns Below the baseline, there will be
	 * marginSectionCount rows by verticalSectionCount columns For example, if
	 * verticalSectionCount = 5, horizontalSectionCount = 5 and marginSectionCount
	 * = 1:<br/>
	 * <code>
	 * 0,0 1,0 2,0 3,0 4,0 (everything above the mean-line)<br/>
	 * mean-line<br/>
	 * 0,1 1,1 2,1 3,1 4,1<br/>
	 * 0,2 1,2 2,2 3,2 4,2<br/>
	 * 0,3 1,3 2,3 3,3 4,3<br/>
	 * 0,4 1,4 2,4 3,4 4,4<br/>
	 * 0,5 1,5 2,5 3,5 4,5<br/>
	 * base-line<br/>
	 * 0,6 1,6 2,6 3,6 4,6 (everything below the base-line)<br/>
	 * </code> If a section break occurs inside a pixel, the pixel's brightness
	 * count is divided proportionally between the two sections.
	 * 
	 * @return double[verticalSectionCount][horizontalSectionCount+2*
	 *         marginSectionCount]
	 */
	public double[][] getBrightnessBySection(int verticalSectionCount, int horizontalSectionCount, int marginSectionCount,
			SectionBrightnessMeasurementMethod measurementMethod) {
		return this.getBrightnessBySector(verticalSectionCount, horizontalSectionCount, marginSectionCount, false, measurementMethod);
	}

	/**
	 * The starting rectangle is taken based on the shape's xHeight (baseline -
	 * meanline). Basically, we take a square with the following boundaries: top =
	 * meanline, bottom = baseline, right = right, left = right - xHeight. To
	 * this, we add at the top and the bottom a margin, each with a height of
	 * xHeight * topBottomMarginWidth. We add on the right (or on the left, if
	 * lang is right-to-left) a margin with a width of xHeight *
	 * horizontalMarginWidth. Any pixels outside the shape's original boundaries
	 * are assumed to be white. Any pixels inside the shape's original boundaries,
	 * but outside the rectangle's boundaries are ignored (in the case where the
	 * shape is wider than xHeight * (1+horizontalMarginWidth), or taller than
	 * xHeight * (1+2*topBottomMarginWidth)). The resulting rectangle is divided
	 * into verticalSectionCount equally spaced vertical sections, and
	 * horizontalSectionCount equally spaced horizontal sections. If a section
	 * break occurs inside a pixel, the pixel's brightness count is divided
	 * proportionally between the two sections.
	 * 
	 * @param verticalSectionCount
	 *          the number of vertical sections
	 * @param horizontalSectionCount
	 *          the number of horizontal sections
	 * @param topBottomMarginWidth
	 *          the width of the top magin
	 * @return double[verticalSectionCount][horizontalSectionCount]
	 */
	public double[][] getBrightnessBySection(int verticalSectionCount, int horizontalSectionCount, double topBottomMarginWidth, double horizontalMarginWidth,
			SectionBrightnessMeasurementMethod measurementMethod) {
		String key = verticalSectionCount + "|" + horizontalSectionCount + "|" + topBottomMarginWidth + "|" + horizontalMarginWidth;

		Map<SectionBrightnessMeasurementMethod, double[][]> brightnessBySector = this.brightnessBySectorMap.get(key);
		if (brightnessBySector == null) {
			int xSectorCount = verticalSectionCount;
			int ySectorCount = horizontalSectionCount;

			double[] verticalBreaks = new double[xSectorCount + 1];
			double[] horizontalBreaks = new double[ySectorCount + 1];

			int xHeight = this.getBaseLine() - this.getMeanLine() + 1;
			if (LOG.isTraceEnabled())
				LOG.trace("xHeight: " + xHeight);

			double totalWidth = xHeight + (xHeight * horizontalMarginWidth);
			double leftOffset = 0.0;
			if (this.getJochreImage().isLeftToRight())
				leftOffset = this.getWidth() - totalWidth;
			double verticalSectionWidth = totalWidth / (verticalSectionCount);

			verticalBreaks[0] = leftOffset;
			for (int i = 1; i <= xSectorCount; i++) {
				verticalBreaks[i] = leftOffset + (verticalSectionWidth * i);
				if (LOG.isTraceEnabled())
					LOG.trace("Vertical break " + i + ": " + verticalBreaks[i]);
			}
			verticalBreaks[xSectorCount] = this.getWidth();

			double totalHeight = xHeight + (xHeight * topBottomMarginWidth * 2.0);
			double topOffset = this.getMeanLine() - (xHeight * topBottomMarginWidth);
			double horizontalSectionHeight = totalHeight / (horizontalSectionCount);

			horizontalBreaks[0] = topOffset;
			for (int i = 1; i <= ySectorCount; i++) {
				horizontalBreaks[i] = topOffset + horizontalSectionHeight * (i);
				if (LOG.isTraceEnabled())
					LOG.trace("Horizontal break " + i + ": " + horizontalBreaks[i]);
			}

			brightnessBySector = this.getBrightnessBySector(key, verticalBreaks, horizontalBreaks);

		}
		return brightnessBySector.get(measurementMethod);
	}

	public double[][] getBrightnessBySector(int verticalSectionCount, int horizontalSectionCount, int marginSectionCount, boolean includeHorizontalMargin,
			SectionBrightnessMeasurementMethod measurementMethod) {
		String key = verticalSectionCount + "|" + horizontalSectionCount + "|" + marginSectionCount + "|" + includeHorizontalMargin;
		Map<SectionBrightnessMeasurementMethod, double[][]> brightnessBySector = this.brightnessBySectorMap.get(key);
		if (brightnessBySector == null) {
			int xSectorCount = verticalSectionCount;
			if (includeHorizontalMargin)
				xSectorCount += marginSectionCount;
			int ySectorCount = horizontalSectionCount + (2 * marginSectionCount);

			double[] verticalBreaks = new double[xSectorCount + 1];
			double[] horizontalBreaks = new double[ySectorCount + 1];

			double xHeight = this.getBaseLine() - this.getMeanLine() + 1;
			if (LOG.isTraceEnabled())
				LOG.trace("xHeight: " + xHeight);

			double verticalSectorWidth = 0.0;
			double horizontalMarginSectorWidth = 0.0;
			double leftOffset = 0;
			double horizontalMarginWidthPixels = 0;
			if (includeHorizontalMargin) {
				double totalWidth = xHeight * 1.5;
				if (!this.getJochreImage().isLeftToRight())
					leftOffset = this.getWidth() - totalWidth;
				horizontalMarginWidthPixels = totalWidth - xHeight;
				horizontalMarginSectorWidth = (horizontalMarginWidthPixels) / marginSectionCount;
				verticalSectorWidth = xHeight / (verticalSectionCount);
			} else {
				verticalSectorWidth = (double) this.getWidth() / (double) xSectorCount;
			}

			int xIndex = 0;
			verticalBreaks[xIndex++] = leftOffset;

			if (this.getJochreImage().isLeftToRight()) {
				for (int i = 0; i < verticalSectionCount; i++) {
					verticalBreaks[xIndex++] = verticalSectorWidth * (i + 1);
				}

				for (int i = 0; i < xSectorCount - verticalSectionCount; i++) {
					verticalBreaks[xIndex++] = (verticalSectorWidth * verticalSectionCount) + (horizontalMarginSectorWidth * (i + 1));
				}

			} else {
				for (int i = 0; i < xSectorCount - verticalSectionCount; i++) {
					verticalBreaks[xIndex++] = leftOffset + horizontalMarginSectorWidth * (i + 1);
				}

				for (int i = 0; i < verticalSectionCount; i++) {
					verticalBreaks[xIndex++] = leftOffset + horizontalMarginWidthPixels + verticalSectorWidth * (i + 1);
				}
			}
			verticalBreaks[xSectorCount] = this.getWidth();

			if (LOG.isTraceEnabled())
				for (int i = 0; i < verticalBreaks.length; i++) {
					LOG.trace("Vertical break " + i + ": " + verticalBreaks[i]);
				}

			int yIndex = 0;
			horizontalBreaks[yIndex++] = 0;
			double headerHeight = this.getMeanLine();
			if (headerHeight < 0)
				headerHeight = 0;
			if (LOG.isTraceEnabled())
				LOG.trace("headerHeight: " + headerHeight);
			double headerSectorHeight = headerHeight / marginSectionCount;
			if (LOG.isTraceEnabled())
				LOG.trace("headerSectorHeight: " + headerSectorHeight);
			for (int i = 0; i < marginSectionCount; i++) {
				horizontalBreaks[yIndex++] = headerSectorHeight * (i + 1);
			}

			double horizontalSectorHeight = xHeight / horizontalSectionCount;
			if (LOG.isTraceEnabled())
				LOG.trace("horizontalSectorHeight: " + horizontalSectorHeight);
			for (int i = 0; i < horizontalSectionCount; i++) {
				horizontalBreaks[yIndex++] = this.getMeanLine() + (horizontalSectorHeight * (i + 1));
			}

			double footerHeight = this.getHeight() - this.getBaseLine() - 1;
			if (footerHeight < 0)
				footerHeight = 0;
			if (LOG.isTraceEnabled())
				LOG.trace("footerHeight: " + footerHeight);
			double footerSectorHeight = footerHeight / marginSectionCount;
			if (LOG.isTraceEnabled())
				LOG.trace("footerSectorHeight: " + footerSectorHeight);
			for (int i = 0; i < marginSectionCount; i++) {
				horizontalBreaks[yIndex++] = this.getBaseLine() + 1 + (footerSectorHeight * (i + 1));
			}

			horizontalBreaks[horizontalBreaks.length - 1] = this.getHeight();
			if (LOG.isTraceEnabled())
				for (int i = 0; i < horizontalBreaks.length; i++) {
					LOG.trace("Horizontal break " + i + ": " + horizontalBreaks[i]);
				}

			brightnessBySector = this.getBrightnessBySector(key, verticalBreaks, horizontalBreaks);
		}
		return brightnessBySector.get(measurementMethod);
	}

	/**
	 * Similar to the other getBrightnessTotalsBySector methods, except that the
	 * base rectangle is taken to be exactly the shape's rectangle, with no
	 * special treatment of the areas above the meanline or below the baseline.
	 * The rectangle is simply divided into verticalSectionCount vertical sections
	 * and horizontalSectionCount horizontal sections. If a section break occurs
	 * inside a pixel, the pixel's brightness count is divided proportionally
	 * between the two sections.
	 * 
	 * @return double[verticalSectionCount][horizontalSectionCount]
	 */
	public double[][] getBrightnessBySection(int verticalSectionCount, int horizontalSectionCount, SectionBrightnessMeasurementMethod measurementMethod) {
		String key = verticalSectionCount + "|" + horizontalSectionCount;
		Map<SectionBrightnessMeasurementMethod, double[][]> brightnessBySector = this.brightnessBySectorMap.get(key);
		if (brightnessBySector == null) {
			int xSectorCount = verticalSectionCount;
			int ySectorCount = horizontalSectionCount;

			double[] verticalBreaks = new double[xSectorCount + 1];
			double[] horizontalBreaks = new double[ySectorCount + 1];

			double verticalSectorWidth = (double) this.getWidth() / (double) xSectorCount;

			verticalBreaks[0] = 0;
			for (int i = 1; i <= verticalSectionCount; i++) {
				verticalBreaks[i] = verticalSectorWidth * i;
			}
			verticalBreaks[xSectorCount] = this.getWidth();

			if (LOG.isTraceEnabled())
				for (int i = 0; i < verticalBreaks.length; i++) {
					LOG.trace("Vertical break " + i + ": " + verticalBreaks[i]);
				}

			double horizontalSectorHeight = (double) this.getHeight() / (double) horizontalSectionCount;
			if (LOG.isTraceEnabled())
				LOG.trace("horizontalSectorHeight: " + horizontalSectorHeight);
			horizontalBreaks[0] = 0;
			for (int i = 1; i <= horizontalSectionCount; i++) {
				horizontalBreaks[i] = horizontalSectorHeight * i;
			}

			horizontalBreaks[ySectorCount] = this.getHeight();

			if (LOG.isTraceEnabled())
				for (int i = 0; i < horizontalBreaks.length; i++) {
					LOG.trace("Horizontal break " + i + ": " + horizontalBreaks[i]);
				}

			brightnessBySector = this.getBrightnessBySector(key, verticalBreaks, horizontalBreaks);
		}
		return brightnessBySector.get(measurementMethod);
	}

	Map<SectionBrightnessMeasurementMethod, double[][]> getBrightnessBySector(String key, double[] verticalBreaks, double[] horizontalBreaks) {
		Map<SectionBrightnessMeasurementMethod, double[][]> brightnessByMethod = this.brightnessBySectorMap.get(key);
		if (brightnessByMethod == null) {
			int xSectorCount = verticalBreaks.length - 1;
			int ySectorCount = horizontalBreaks.length - 1;
			double[][] totals = new double[xSectorCount][ySectorCount];

			// calculate the y-distribution among sections
			double[][] yDistribution = new double[this.getHeight()][ySectorCount];
			int[] yStart = new int[this.getHeight()];
			int[] yEnd = new int[this.getHeight()];
			int[] xStart = new int[this.getWidth()];
			int[] xEnd = new int[this.getWidth()];

			for (int y = 0; y < this.getHeight(); y++) {
				for (int i = 0; i < ySectorCount; i++) {
					double horizontalBreak = horizontalBreaks[i + 1];
					if (y < horizontalBreak && horizontalBreak < y + 1) {
						yDistribution[y][i] = horizontalBreak - Math.floor(horizontalBreak);
						yStart[y] = i;
						yEnd[y] = i;
						if (i + 1 < yDistribution[y].length) {
							yDistribution[y][i + 1] = 1 - yDistribution[y][i];
							yEnd[y] = i + 1;
						}
						break;
					} else if (y < horizontalBreak) {
						yDistribution[y][i] = 1.0;
						yStart[y] = i;
						yEnd[y] = i;
						break;
					}
				}
			} // next y-coordinate in shape

			// calculate the x-distribution among sections
			double[][] xDistribution = new double[this.getWidth()][xSectorCount];
			for (int x = 0; x < this.getWidth(); x++) {
				for (int i = 0; i < xSectorCount; i++) {
					double verticalBreak = verticalBreaks[i + 1];
					if (x < verticalBreak && verticalBreak < x + 1) {
						xDistribution[x][i] = verticalBreak - Math.floor(verticalBreak);
						xStart[x] = i;
						xEnd[x] = i;
						if (i + 1 < xDistribution[x].length) {
							xDistribution[x][i + 1] = 1 - xDistribution[x][i];
							xEnd[x] = i + 1;
						}
						break;
					} else if (x < verticalBreak) {
						xDistribution[x][i] = 1.0;
						xStart[x] = i;
						xEnd[x] = i;
						break;
					}
				}
			} // next x-coordinate in shape

			// get brightnesses
			double totalBrightness = 0.0;
			for (int y = 0; y < this.getHeight(); y++) {
				for (int x = 0; x < this.getWidth(); x++) {
					double brightness = 255.0 - this.getPixelInShape(x, y);
					totalBrightness += brightness;
					for (int i = xStart[x]; i <= xEnd[x]; i++)
						for (int j = yStart[y]; j <= yEnd[y]; j++)
							totals[i][j] += xDistribution[x][i] * yDistribution[y][j] * brightness;
				}
			}

			// calculate the pixel count for each section
			double[][] pixelCounts = new double[xSectorCount][ySectorCount];
			for (int i = 0; i < xSectorCount; i++)
				for (int j = 0; j < ySectorCount; j++) {
					double sectionWidth = verticalBreaks[i + 1] - verticalBreaks[i];
					double sectionHeight = horizontalBreaks[j + 1] - horizontalBreaks[j];
					pixelCounts[i][j] = sectionWidth * sectionHeight;
				}

			// calculate the ratio for each section
			double[][] ratios = new double[xSectorCount][ySectorCount];
			double maxRatio = 0.0;
			for (int i = 0; i < xSectorCount; i++)
				for (int j = 0; j < ySectorCount; j++) {
					if (pixelCounts[i][j] > 0) {
						ratios[i][j] = totals[i][j] / pixelCounts[i][j];
						if (ratios[i][j] > maxRatio)
							maxRatio = ratios[i][j];
					} else {
						ratios[i][j] = 0;
					}
				}

			// calculate relative to the max normalised value
			double[][] relativeToMax = new double[xSectorCount][ySectorCount];
			if (maxRatio > 0) {
				for (int i = 0; i < ratios.length; i++) {
					for (int j = 0; j < ratios[0].length; j++) {
						double ratio = ratios[i][j];
						relativeToMax[i][j] = ratio / maxRatio;
					}
				}
			}

			// calculate relative to the total value
			double[][] relativeToTotal = new double[xSectorCount][ySectorCount];

			if (totalBrightness > 0) {
				for (int i = 0; i < totals.length; i++) {
					for (int j = 0; j < totals[0].length; j++) {
						double brightness = totals[i][j];
						relativeToTotal[i][j] = brightness / totalBrightness;
					}
				}
			}

			if (LOG.isTraceEnabled()) {
				LOG.trace("Results: ");
				for (int j = 0; j < ratios[0].length; j++) {
					StringBuilder sb = new StringBuilder();
					StringBuilder sb2 = new StringBuilder();
					StringBuilder sb3 = new StringBuilder();
					sb.append("Totals(" + j + ")");
					sb2.append("Counts(" + j + ")");
					sb3.append("Ratios(" + j + ")");
					for (int i = 0; i < ratios.length; i++) {
						sb.append((int) totals[i][j]);
						sb.append(',');
						sb2.append((int) pixelCounts[i][j]);
						sb2.append(',');
						sb3.append((int) ratios[i][j]);
						sb3.append(',');
					}
					LOG.trace(sb.toString());
					LOG.trace(sb2.toString());
					LOG.trace(sb3.toString());
				}
			}

			brightnessByMethod = new HashMap<Shape.SectionBrightnessMeasurementMethod, double[][]>();
			brightnessByMethod.put(SectionBrightnessMeasurementMethod.RAW, totals);
			brightnessByMethod.put(SectionBrightnessMeasurementMethod.SIZE_NORMALISED, ratios);
			brightnessByMethod.put(SectionBrightnessMeasurementMethod.RELATIVE_TO_MAX_SECTION, relativeToMax);
			brightnessByMethod.put(SectionBrightnessMeasurementMethod.PORTION_OF_TOTAL_BRIGHTNESS, relativeToTotal);

			this.brightnessBySectorMap.put(key, brightnessByMethod);
		}

		return brightnessByMethod;
	}

	/**
	 * Mean brightness for the sections defined above.
	 */
	public double getBrightnessMeanBySection(int verticalSectionCount, int horizontalSectionCount, int marginSectionCount,
			SectionBrightnessMeasurementMethod measurementMethod) {
		String key = verticalSectionCount + "|" + horizontalSectionCount + "|" + marginSectionCount;
		return this.getBrightnessMeanBySector(key, measurementMethod);
	}

	/**
	 * Mean brightness for the sections defined above.
	 */
	public double getBrightnessMeanBySection(int verticalSectionCount, int horizontalSectionCount, SectionBrightnessMeasurementMethod measurementMethod) {
		String key = verticalSectionCount + "|" + horizontalSectionCount;
		return this.getBrightnessMeanBySector(key, measurementMethod);
	}

	/**
	 * Mean brightness for the sections defined above.
	 */
	public double getBrightnessMeanBySection(int verticalSectionCount, int horizontalSectionCount, double topBottomMarginWidth, double horizontalMarginWidth,
			SectionBrightnessMeasurementMethod measurementMethod) {
		String key = verticalSectionCount + "|" + horizontalSectionCount + "|" + topBottomMarginWidth + "|" + horizontalMarginWidth;
		return this.getBrightnessMeanBySector(key, measurementMethod);
	}

	double getBrightnessMeanBySector(String key, SectionBrightnessMeasurementMethod measurementMethod) {
		Map<SectionBrightnessMeasurementMethod, Double> methodToMeanMap = this.brightnessMeanBySectorMap.get(key);
		if (methodToMeanMap == null) {
			methodToMeanMap = new HashMap<Shape.SectionBrightnessMeasurementMethod, Double>();
			this.brightnessMeanBySectorMap.put(key, methodToMeanMap);
		}
		Double brightnessMeanBySectorObj = methodToMeanMap.get(measurementMethod);
		double brightnessMeanBySector = 0.0;
		if (brightnessMeanBySectorObj == null) {
			Mean mean = new Mean();
			Map<SectionBrightnessMeasurementMethod, double[][]> brightnessByMethod = this.brightnessBySectorMap.get(key);
			double[][] brightnessGrid = brightnessByMethod.get(measurementMethod);
			for (int i = 0; i < brightnessGrid.length; i++)
				mean.incrementAll(brightnessGrid[i]);
			brightnessMeanBySector = mean.getResult();
			methodToMeanMap.put(measurementMethod, brightnessMeanBySector);
		} else {
			brightnessMeanBySector = brightnessMeanBySectorObj.doubleValue();
		}
		return brightnessMeanBySector;
	}

	/**
	 * Checks if a pixel is black using the container image's calculated black
	 * threshold.
	 */
	public boolean isPixelBlack(int x, int y) {
		int threshold = this.getJochreImage().getBlackThreshold();
		return this.isPixelBlack(x, y, threshold);
	}

	@Override
	public boolean isPixelBlack(int x, int y, int threshold) {
		return this.isPixelBlack(x, y, threshold, 0);
	}

	/**
	 * Similar to {@link #isPixelBlack(int, int, int)} but adds an additional
	 * "white gap fill factor" to help fill in white spaces when an algorithm is
	 * sensitive to these. Basically, the fill factor fills in any pixel when at
	 * least 5 out of 8 surrounding the pixel are black. This is performed n
	 * times, where n = whiteGapFillFactor, progressively filling in any white
	 * gaps. Note that outlines will also get "padded", as the algorithm doesn't
	 * differentiate internal gaps from external ones.
	 */
	public boolean isPixelBlack(int x, int y, int threshold, int whiteGapFillFactor) {
		if (x < 0 || y < 0 || x >= this.getWidth() || y >= this.getHeight())
			return false;
		BitSet bitset = this.getBlackAndWhiteBitSet(threshold, whiteGapFillFactor);
		return bitset.get(y * this.getWidth() + x);
	}

	/**
	 * Return a BitSet representation of this shape for a given brightness
	 * threshold and given white gap fill factor. See
	 * {@link #isPixelBlack(int, int, int, int)} for details on how we determine
	 * if a given bit is true or false. The BitSet structured from top-left to
	 * bottom-right, travelling horizontally first.
	 */
	public BitSet getBlackAndWhiteBitSet(int threshold) {
		String key = "" + threshold;
		BitSet bitset = this.bitsets.get(key);
		if (bitset == null) {
			bitset = new BitSet(this.getWidth() * this.getHeight());
			int counter = 0;
			for (int j = 0; j < this.getHeight(); j++)
				for (int i = 0; i < this.getWidth(); i++) {
					int pixel = this.getPixel(i, j);
					bitset.set(counter++, pixel <= threshold);
				}
			this.bitsets.put(key, bitset);
		}
		return bitset;
	}

	/**
	 * Return a BitSet representation of this shape for a given brightness
	 * threshold and given white gap fill factor. See
	 * {@link #isPixelBlack(int, int, int, int)} for details on how we determine
	 * if a given bit is true or false. The BitSet structured from top-left to
	 * bottom-right, travelling horizontally first.
	 */
	public BitSet getBlackAndWhiteBitSet(int threshold, int whiteGapFillFactor) {
		String key = threshold + "|" + whiteGapFillFactor;
		BitSet bitset = this.bitsets.get(key);
		if (bitset == null) {
			bitset = this.getBlackAndWhiteBitSet(threshold);
			// if the image is black-and-white, fill in any bits
			// that may have been emptied during the scan
			if (whiteGapFillFactor > 0 && this.isBlackAndWhite()) {
				ShapeFiller shapeFiller = new ShapeFiller();
				bitset = shapeFiller.fillShape(this, threshold, whiteGapFillFactor);

			} else {
				bitset = this.getBlackAndWhiteBitSet(threshold);
			}
			this.bitsets.put(key, bitset);
		}
		return bitset;
	}

	/**
	 * Find outline of the shape as a BitSet.
	 */
	public BitSet getOutline(int threshold) {
		BitSet outline = this.outlines.get(threshold);
		if (outline == null) {
			outline = new BitSet(this.getHeight() * this.getWidth());
			int counter = 0;
			for (int y = 0; y < this.getHeight(); y++) {
				for (int x = 0; x < this.getWidth(); x++) {
					boolean black = this.isPixelBlack(x, y, threshold);
					if (!black)
						outline.set(counter++, false);
					else {
						boolean innerPixel = this.isPixelBlack(x - 1, y, threshold) && this.isPixelBlack(x + 1, y, threshold) && this.isPixelBlack(x, y - 1, threshold)
								&& this.isPixelBlack(x, y + 1, threshold);
						outline.set(counter++, !innerPixel);
					} // is it black?
				} // next x
			} // next y
			this.outlines.put(threshold, outline);
		}
		return outline;
	}

	/**
	 * The image containing this shape.
	 */
	public JochreImage getJochreImage() {
		if (this.jochreImage == null) {
			this.jochreImage = this.getGroup().getRow().getParagraph().getImage();
		}
		return jochreImage;
	}

	void setJochreImage(JochreImage jochreImage) {
		this.jochreImage = jochreImage;
	}

	boolean isBlackAndWhite() {
		if (brightnessCounts == null) {
			brightnessCounts = new int[256];
			for (int y = 0; y < this.getHeight(); y++)
				for (int x = 0; x < this.getWidth(); x++) {
					int brightness = this.getPixel(x, y);
					brightnessCounts[brightness]++;
				}

			int levelCount = 0;
			for (int i = 0; i < 256; i++) {
				if (brightnessCounts[i] > 0)
					levelCount++;
				if (levelCount > 2)
					break;
			}
			blackAndWhite = levelCount <= 2;
		}
		return blackAndWhite;
	}

	/**
	 * Get the total brightness counts for each vertical line.
	 * 
	 * @return an array of int with size {@link #getWidth()}.
	 */
	public int[] getVerticalCounts() {
		if (this.verticalCounts == null) {
			this.verticalCounts = new int[this.getWidth()];
			for (int x = 0; x < this.getWidth(); x++) {
				for (int y = 0; y < this.getHeight(); y++) {
					int brightness = this.getPixel(x, y);
					if (brightness < 0)
						brightness = 256 + brightness;
					this.verticalCounts[x] += 255 - brightness;
				}
			}
		}
		return this.verticalCounts;
	}

	/**
	 * Recalculate the various statistical measurements for this shape. Should be
	 * called after the shape coordinates have changed.
	 */
	public void recalculate() {
		image = null;
		pixelGrabber = null;

		brightnessBySectorMap = new HashMap<String, Map<SectionBrightnessMeasurementMethod, double[][]>>();
		brightnessMeanBySectorMap = new HashMap<String, Map<SectionBrightnessMeasurementMethod, Double>>();

		bitsets = new Hashtable<String, BitSet>();
		outlines = new Hashtable<Integer, BitSet>();

		brightnessCounts = null;

		verticalCounts = null;
		verticalContour = null;
		totalBrightness = 0;
	}

	@Override
	public String toString() {
		return "Shape, left(" + this.getLeft() + ")" + ", top(" + this.getTop() + ")" + ", right(" + this.getRight() + ")" + ", bot(" + this.getBottom() + ")"
				+ " [id=" + this.getId() + "]";
	}

	/**
	 * Get the image behind this shape.
	 */
	public BufferedImage getImage() {
		if (image == null && this.jochreImage != null) {
			image = this.jochreImage.getOriginalImage().getSubimage(this.getLeft(), this.getTop(), this.getWidth(), this.getHeight());
		}
		return image;
	}

	void setImage(BufferedImage image) {
		this.image = image;
	}

	ImagePixelGrabber getPixelGrabber() {
		if (this.pixelGrabber == null) {
			this.pixelGrabber = new ImagePixelGrabberImpl(this.getImage());
		}
		return this.pixelGrabber;
	}

	void setPixelGrabber(ImagePixelGrabber pixelGrabber) {
		this.pixelGrabber = pixelGrabber;
	}

	/**
	 * Get the centre point of this shape.
	 */
	public double[] getCentrePoint() {
		double yCentre = (this.getTop() + this.getBottom()) / 2.0;
		double xCentre = (this.getLeft() + this.getRight()) / 2.0;
		return new double[] { xCentre, yCentre };
	}

	boolean isDirty() {
		return dirty;
	}

	void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	/**
	 * An ordered set of letter guesses for the current shape.
	 */
	public Set<Decision> getLetterGuesses() {
		if (this.letterGuesses == null) {
			this.letterGuesses = new TreeSet<Decision>();
		}
		return letterGuesses;
	}

	/**
	 * Writes a textual form of the image (pixel by pixel) to the log.
	 */
	public void writeImageToLog() {
		DecimalFormat df = new DecimalFormat("000");
		for (int y = 0; y < this.getHeight(); y++) {
			String line = "";
			if (y == this.getMeanLine()) {
				line += "MMM";
			} else if (y == this.getBaseLine()) {
				line += "BBB";
			} else {
				line += df.format(y);
			}
			for (int x = 0; x < this.getWidth(); x++) {
				if (this.isPixelBlack(x, y, this.getJochreImage().getBlackThreshold()))
					line += "x";
				else
					line += "o";
			}
			LOG.debug(line);
		}
	}

	public LetterGuesserService getLetterGuesserService() {
		return letterGuesserService;
	}

	public void setLetterGuesserService(LetterGuesserService letterGuesserService) {
		this.letterGuesserService = letterGuesserService;
	}

	/**
	 * The original guess for this shape, when it was automatically analysed by
	 * Jochre.
	 */
	public String getOriginalGuess() {
		return originalGuess;
	}

	public void setOriginalGuess(String originalGuess) {
		if (originalGuess == null)
			originalGuess = "";
		if (!this.originalGuess.equals(originalGuess)) {
			this.originalGuess = originalGuess;
			this.dirty = true;
		}
	}

	public int getTotalBrightness() {
		if (totalBrightness == 0) {
			for (int y = 0; y < this.getHeight(); y++) {
				for (int x = 0; x < this.getWidth(); x++) {
					int brightness = 255 - this.getPixel(x, y);
					totalBrightness += brightness;
				}
			}
		}
		return totalBrightness;
	}

	/**
	 * The point from which this shape was actually built. Only available when
	 * segmenting (not saved).
	 */
	public int[] getStartingPoint() {
		if (this.startingPoint == null) {

			int startX = -1, startY = -1;
			for (int y = 0; y < this.getHeight(); y++) {
				for (int x = 0; x < this.getWidth(); x++) {
					if (this.isPixelBlack(x, y, this.getJochreImage().getSeparationThreshold())) {
						startX = x;
						startY = y;
						break;
					}
				}
				if (startX >= 0)
					break;
			}
			this.startingPoint = new int[] { startX, startY };
		}
		return startingPoint;
	}

	public void setStartingPoint(int[] startingPoint) {
		this.startingPoint = startingPoint;
	}

	/**
	 * Difference between baseline and meanline.
	 */
	public int getXHeight() {
		return this.baseLine - this.meanLine;
	}

	/**
	 * A representation of the shape as a set of vertical line segments.
	 */
	TreeSet<VerticalLineSegment> getVerticalLineSegments() {
		if (lines == null) {
			WritableImageGrid mirror = new ImageMirror(this);

			int[] startingPoint = this.getStartingPoint();
			int startX = startingPoint[0];
			int startY = startingPoint[1];

			// let's imagine
			// 0 X 0 0 x x
			// x x x 0 0 x
			// 0 0 x x x x

			// as we build the shape, we keep track in memory of all of the vertical
			// line segments that we find
			// and which vertical line segments touch them to the right and left
			// a segment can have more than one left segment (if they're broken by a
			// white space)
			Stack<VerticalLineSegment> lineStack = new Stack<VerticalLineSegment>();
			lines = new TreeSet<VerticalLineSegment>();
			VerticalLineSegment firstLine = new VerticalLineSegment(startX, startY);

			lineStack.push(firstLine);

			while (!lineStack.isEmpty()) {
				VerticalLineSegment line = lineStack.pop();
				// Add this line's pixels to the mirror so that we don't touch it again.
				for (int rely = line.yTop; rely <= line.yBottom; rely++)
					mirror.setPixel(line.x, rely, 1);

				// extend the vertical line segment up and down from this point
				for (int rely = line.yTop - 1; rely >= 0; rely--) {
					if (this.isPixelBlack(line.x, rely, this.getJochreImage().getSeparationThreshold())) {
						mirror.setPixel(line.x, rely, 1);
						line.yTop = rely;
					} else {
						break;
					}
				}
				for (int rely = line.yBottom + 1; rely < this.getHeight(); rely++) {
					if (this.isPixelBlack(line.x, rely, this.getJochreImage().getSeparationThreshold())) {
						mirror.setPixel(line.x, rely, 1);
						line.yBottom = rely;
					} else {
						break;
					}
				}

				if (LOG.isTraceEnabled())
					LOG.trace("Adding line x = " + line.x + ", top = " + line.yTop + ", bottom = " + line.yBottom);
				lines.add(line);

				// find any points to the left of this segment
				int relx = line.x - 1;
				VerticalLineSegment leftLine = null;
				for (int rely = line.yTop - 1; rely <= line.yBottom + 1; rely++) {
					if (this.isPixelBlack(relx, rely, this.getJochreImage().getSeparationThreshold())) {
						if (leftLine == null) {
							leftLine = new VerticalLineSegment(relx, rely);
						} else {
							leftLine.yBottom = rely;
						}
					} else {
						if (leftLine != null) {
							if (mirror.getPixel(relx, leftLine.yTop) > 0) {
								// if we already found this line before - let's find it again.
								for (VerticalLineSegment lineSegment : lines) {
									if (lineSegment.x == relx) {
										if (lineSegment.yTop <= leftLine.yTop && leftLine.yTop <= lineSegment.yBottom) {
											leftLine = lineSegment;
											break;
										}
									}
								}
							} else if (lineStack.contains(leftLine)) {
								leftLine = lineStack.get(lineStack.indexOf(leftLine));
							} else {
								lineStack.push(leftLine);
							}
							line.leftSegments.add(leftLine);
							leftLine = null;
						}
					}
				} // next rely

				// add the last line
				if (leftLine != null) {
					if (mirror.getPixel(relx, leftLine.yTop) > 0) {
						// if we already found this line before - let's find it again.
						for (VerticalLineSegment lineSegment : lines) {
							if (lineSegment.x == relx) {
								if (lineSegment.yTop <= leftLine.yTop && leftLine.yTop <= lineSegment.yBottom) {
									leftLine = lineSegment;
									break;
								}
							}
						}
					} else if (lineStack.contains(leftLine)) {
						leftLine = lineStack.get(lineStack.indexOf(leftLine));
					} else {
						lineStack.push(leftLine);
					}
					line.leftSegments.add(leftLine);
				}

				// find any points to the right of this segment
				relx = line.x + 1;
				VerticalLineSegment rightLine = null;
				for (int rely = line.yTop - 1; rely <= line.yBottom + 1; rely++) {
					if (this.isPixelBlack(relx, rely, this.getJochreImage().getSeparationThreshold())) {
						if (rightLine == null) {
							rightLine = new VerticalLineSegment(relx, rely);
						} else {
							rightLine.yBottom = rely;
						}
					} else {
						if (rightLine != null) {
							if (mirror.getPixel(relx, rightLine.yTop) > 0) {
								// if we already found this line before - let's find it again.
								for (VerticalLineSegment lineSegment : lines) {
									if (lineSegment.x == relx) {
										if (lineSegment.yTop <= rightLine.yTop && rightLine.yTop <= lineSegment.yBottom) {
											rightLine = lineSegment;
											break;
										}
									}
								}
							} else if (lineStack.contains(rightLine)) {
								rightLine = lineStack.get(lineStack.indexOf(rightLine));
							} else {
								lineStack.push(rightLine);
							}
							line.rightSegments.add(rightLine);
							rightLine = null;
						}
					}
				} // next rely

				// add the last line
				if (rightLine != null) {
					if (mirror.getPixel(relx, rightLine.yTop) > 0) {
						// if we already found this line before - let's find it again.
						for (VerticalLineSegment lineSegment : lines) {
							if (lineSegment.x == relx) {
								if (lineSegment.yTop <= rightLine.yTop && rightLine.yTop <= lineSegment.yBottom) {
									rightLine = lineSegment;
									break;
								}
							}
						}
					} else if (lineStack.contains(rightLine)) {
						rightLine = lineStack.get(lineStack.indexOf(rightLine));
					} else {
						lineStack.push(rightLine);
					}
					line.rightSegments.add(rightLine);
				}
			} // next line on stack
			LOG.debug("Found " + lines.size() + " lines");
		}
		return lines;
	}

	/**
	 * Returns vertical line segments that are likely to be a bridge between two
	 * connected letters (due to an ink splotch, etc.).
	 */
	Collection<BridgeCandidate> getBridgeCandidates(double maxBridgeWidth) {
		if (this.bridgeCandidates == null) {
			TreeSet<VerticalLineSegment> lines = this.getVerticalLineSegments();

			// Now, detect "bridges" which could indicate that the shape should be
			// split

			// First, detect which spaces are "enclosed" and which touch the outer
			// walls
			// To do this, build up a set of all inverse (white) lines
			TreeSet<VerticalLineSegment> inverseLines = new TreeSet<VerticalLineSegment>();
			int currentX = -1;
			VerticalLineSegment previousLine = null;
			for (VerticalLineSegment line : lines) {
				// LOG.debug("Checking line x = " + line.x + ", top = " + line.yTop + ",
				// bottom = " + line.yBottom);
				if (line.x != currentX) {
					// new x-coordinate
					if (previousLine != null && previousLine.yBottom < this.getHeight() - 1) {
						VerticalLineSegment inverseLine = new VerticalLineSegment(previousLine.x, previousLine.yBottom + 1);
						inverseLine.yBottom = this.getHeight() - 1;
						inverseLines.add(inverseLine);
						// LOG.debug("Adding inverse line x = " + inverseLine.x + ", top = "
						// + inverseLine.yTop + ", bottom = " + inverseLine.yBottom);
					}
					if (line.yTop > 0) {
						VerticalLineSegment inverseLine = new VerticalLineSegment(line.x, line.yTop - 1);
						inverseLine.yTop = 0;
						inverseLines.add(inverseLine);
						// LOG.debug("Adding inverse line x = " + inverseLine.x + ", top = "
						// + inverseLine.yTop + ", bottom = " + inverseLine.yBottom);
					}
					currentX = line.x;
				} else if (previousLine != null) {
					VerticalLineSegment inverseLine = new VerticalLineSegment(previousLine.x, previousLine.yBottom + 1);
					inverseLine.yBottom = line.yTop - 1;
					inverseLines.add(inverseLine);
					// LOG.debug("Adding inverse line x = " + inverseLine.x + ", top = " +
					// inverseLine.yTop + ", bottom = " + inverseLine.yBottom);
				}
				previousLine = line;
			}
			if (previousLine != null && previousLine.yBottom < this.getHeight() - 1) {
				VerticalLineSegment inverseLine = new VerticalLineSegment(previousLine.x, previousLine.yBottom + 1);
				inverseLine.yBottom = this.getHeight() - 1;
				inverseLines.add(inverseLine);
				// LOG.debug("Adding inverse line x = " + inverseLine.x + ", top = " +
				// inverseLine.yTop + ", bottom = " + inverseLine.yBottom);
			}
			LOG.debug("inverseLines size: " + inverseLines.size());

			// Calculate neighbours for inverse lines
			for (VerticalLineSegment inverseLine : inverseLines) {
				for (VerticalLineSegment otherLine : inverseLines) {
					if (otherLine.x == inverseLine.x + 1) {
						if (inverseLine.yTop - 1 <= otherLine.yBottom && otherLine.yTop <= inverseLine.yBottom + 1) {
							inverseLine.rightSegments.add(otherLine);
							otherLine.leftSegments.add(inverseLine);
						}
					}
					if (otherLine.x == inverseLine.x - 1) {
						if (inverseLine.yTop - 1 <= otherLine.yBottom && otherLine.yTop <= inverseLine.yBottom + 1) {
							inverseLine.leftSegments.add(otherLine);
							otherLine.rightSegments.add(inverseLine);
						}
					}
				}
			}

			// Eliminate any white lines which somehow touch an edge
			Stack<VerticalLineSegment> lineStack = new Stack<VerticalLineSegment>();
			Set<VerticalLineSegment> outerInverseLines = new HashSet<VerticalLineSegment>();
			for (VerticalLineSegment inverseLine : inverseLines) {
				if (inverseLine.yTop == 0 || inverseLine.x == 0 || inverseLine.yBottom == this.getHeight() - 1 || inverseLine.x == this.getWidth() - 1)
					lineStack.push(inverseLine);
			}
			while (!lineStack.isEmpty()) {
				VerticalLineSegment inverseLine = lineStack.pop();
				if (!inverseLine.touched) {
					inverseLine.touched = true;
					outerInverseLines.add(inverseLine);
					// LOG.debug("Outer inverse line x = " + inverseLine.x + ", top = " +
					// inverseLine.yTop + ", bottom = " + inverseLine.yBottom);

					for (VerticalLineSegment rightLine : inverseLine.rightSegments)
						lineStack.push(rightLine);
					for (VerticalLineSegment leftLine : inverseLine.leftSegments) {
						lineStack.push(leftLine);
					}
				}
			}
			LOG.debug("outerInverseLines size: " + outerInverseLines.size());

			Set<VerticalLineSegment> enclosedInverseLines = new HashSet<VerticalLineSegment>(inverseLines);
			enclosedInverseLines.removeAll(outerInverseLines);
			LOG.debug("enclosedInverseLines.size: " + enclosedInverseLines.size());
			if (LOG.isDebugEnabled()) {
				for (VerticalLineSegment inverseLine : enclosedInverseLines)
					LOG.debug("Enclosed inverse line x = " + inverseLine.x + ", top = " + inverseLine.yTop + ", bottom = " + inverseLine.yBottom);
			}

			// Add bridge candidates
			// based on maximum line length and having exactly one neighbour on each
			// side
			LOG.debug("Adding bridge candidates");
			List<BridgeCandidate> candidateList = new ArrayList<BridgeCandidate>();
			for (VerticalLineSegment line : lines) {
				if (line.rightSegments.size() == 1 && line.leftSegments.size() == 1 && line.length() <= maxBridgeWidth) {
					// also the bridge width should be considered where two vertical lines
					// touch each other
					// rather than for the full length of the line
					BridgeCandidate candidate = null;
					VerticalLineSegment rightLine = line.rightSegments.iterator().next();
					VerticalLineSegment leftLine = line.leftSegments.iterator().next();
					int leftTopTouch = (leftLine.yTop > line.yTop ? leftLine.yTop : line.yTop);
					int leftBottomTouch = (leftLine.yBottom < line.yBottom ? leftLine.yBottom : line.yBottom);
					int rightTopTouch = (rightLine.yTop > line.yTop ? rightLine.yTop : line.yTop);
					int rightBottomTouch = (rightLine.yBottom < line.yBottom ? rightLine.yBottom : line.yBottom);

					int rightLength = rightTopTouch - rightBottomTouch;
					int leftLength = leftTopTouch - leftBottomTouch;

					if (line.length() <= maxBridgeWidth || rightLength <= maxBridgeWidth || leftLength <= maxBridgeWidth) {
						candidate = new BridgeCandidate(this, line);

						if (rightLength < leftLength && rightLength < line.length()) {
							candidate.topTouch = rightTopTouch;
							candidate.bottomTouch = rightBottomTouch;
						} else if (leftLength < line.length()) {
							candidate.topTouch = leftTopTouch;
							candidate.bottomTouch = leftBottomTouch;
						}
						LOG.debug("Adding bridge candidate x = " + candidate.x + ", top = " + candidate.yTop + ", bottom = " + candidate.yBottom);
						candidateList.add(candidate);
					}
				}
			}
			LOG.debug("Bridge candidate size: " + candidateList.size());

			LOG.debug("Eliminating candidates with shorter neighbor");
			Set<BridgeCandidate> candidatesToEliminate = null;
			if (candidateList.size() > 0) {
				// eliminate any bridge candidates that touch a shorter bridge candidate
				candidatesToEliminate = new HashSet<BridgeCandidate>();
				for (int i = 0; i < candidateList.size() - 1; i++) {
					BridgeCandidate candidate = candidateList.get(i);
					for (int j = i + 1; j < candidateList.size(); j++) {
						BridgeCandidate otherCandidate = candidateList.get(j);
						if (otherCandidate.x == candidate.x + 1 && candidate.rightSegments.contains(otherCandidate)) {
							if ((candidate.bridgeWidth()) <= (otherCandidate.bridgeWidth())) {
								LOG.debug("Eliminating candidate x = " + otherCandidate.x + ", top = " + otherCandidate.yTop + ", bottom = " + otherCandidate.yBottom);
								candidatesToEliminate.add(otherCandidate);
							} else {
								LOG.debug("Eliminating candidate x = " + candidate.x + ", top = " + candidate.yTop + ", bottom = " + candidate.yBottom);
								candidatesToEliminate.add(candidate);
							}
						}
					}
				}
				candidateList.removeAll(candidatesToEliminate);

				LOG.debug("Bridge candidate size: " + candidateList.size());

				// To be a bridge, three additional things have to be true:
				// (A) intersection between right & left shape = null
				// (B) weight of right shape & weight of left shape > a certain
				// threshold
				// (C) little overlap right boundary of left shape, left boundary of
				// right shape

				LOG.debug("Eliminating candidates touching enclosed space");
				// (A) intersection between right & left shape = null
				// Intersection between right and left shape is non-null
				// if the line segment X touches an enclosed space immediately above or
				// below
				candidatesToEliminate = new HashSet<BridgeCandidate>();
				for (BridgeCandidate candidate : candidateList) {
					boolean nullIntersection = true;
					for (VerticalLineSegment inverseLine : enclosedInverseLines) {
						if (candidate.x == inverseLine.x) {
							if (inverseLine.yBottom == candidate.yTop - 1 || inverseLine.yTop == candidate.yBottom + 1) {
								nullIntersection = false;
								break;
							}
						}
					}
					if (!nullIntersection) {
						LOG.debug("Eliminating candidate x = " + candidate.x + ", top = " + candidate.yTop + ", bottom = " + candidate.yBottom);
						candidatesToEliminate.add(candidate);
					}
				}
				candidateList.removeAll(candidatesToEliminate);
				LOG.debug("Remaining bridge candidate size: " + candidateList.size());

				// another criterion for avoiding "false splits" is that on both side of
				// the bridge
				// the shapes pretty rapidly expand in width both up and down
				LOG.debug("Eliminating candidates without vertical expansion on both sides");
				candidatesToEliminate = new HashSet<BridgeCandidate>();
				int expansionLimit = (int) Math.ceil((this.getWidth()) / 6.0);
				for (BridgeCandidate candidate : candidateList) {
					// take into account the portion touching on the right or left
					boolean isCandidate = true;
					Stack<VerticalLineSegment> leftLines = new Stack<VerticalLineSegment>();
					Stack<Integer> leftDepths = new Stack<Integer>();
					leftLines.push(candidate);
					leftDepths.push(0);
					int leftTop = candidate.topTouch;
					int leftBottom = candidate.bottomTouch;
					while (!leftLines.isEmpty()) {
						VerticalLineSegment line = leftLines.pop();
						int depth = leftDepths.pop();
						if (line.yTop < leftTop)
							leftTop = line.yTop;
						if (line.yBottom > leftBottom)
							leftBottom = line.yBottom;
						if (depth <= expansionLimit) {
							for (VerticalLineSegment leftSegment : line.leftSegments) {
								leftLines.push(leftSegment);
								leftDepths.push(depth + 1);
							}
						}
					}
					if (leftTop == candidate.topTouch || leftBottom == candidate.bottomTouch)
						isCandidate = false;
					if (isCandidate) {
						Stack<VerticalLineSegment> rightLines = new Stack<VerticalLineSegment>();
						Stack<Integer> rightDepths = new Stack<Integer>();
						rightLines.push(candidate);
						rightDepths.push(0);
						int rightTop = candidate.topTouch;
						int rightBottom = candidate.bottomTouch;
						while (!rightLines.isEmpty()) {
							VerticalLineSegment line = rightLines.pop();
							int depth = rightDepths.pop();
							if (line.yTop < rightTop)
								rightTop = line.yTop;
							if (line.yBottom > rightBottom)
								rightBottom = line.yBottom;
							if (depth <= expansionLimit) {
								for (VerticalLineSegment rightSegment : line.rightSegments) {
									rightLines.push(rightSegment);
									rightDepths.push(depth + 1);
								}
							}
						}
						if (rightTop == candidate.topTouch || rightBottom == candidate.bottomTouch)
							isCandidate = false;
					}
					if (!isCandidate) {
						LOG.debug("Eliminating candidate x = " + candidate.x + ", top = " + candidate.yTop + ", bottom = " + candidate.yBottom);
						candidatesToEliminate.add(candidate);
					}
				}
				candidateList.removeAll(candidatesToEliminate);
				LOG.debug("Remaining bridge candidate size: " + candidateList.size());

				if (LOG.isDebugEnabled()) {
					for (VerticalLineSegment candidate : candidateList) {
						LOG.debug("Remaining candidate x = " + candidate.x + ", top = " + candidate.yTop + ", bottom = " + candidate.yBottom);
					}
				}
			}

			if (candidateList.size() > 0) {
				// (B) weight of right shape & weight of left shape > a certain
				// threshold
				// (C) little overlap right boundary of left shape, left boundary of
				// right shape
				//
				// We can now divide the shape into n groups, each separated by a
				// candidate
				// We recursively build a group until we reach a candidate
				// and indicate whether it's the right or left border of the candidate.
				// We then keep going from the candidate on to the next one
				// We keep tab of the size of each group and of its right & left
				// boundaries
				// at the end we can easily determine the right and left boundaries of
				// each,
				// as well as the right & left pixel weight
				List<VerticalLineGroup> groups = new ArrayList<VerticalLineGroup>();

				VerticalLineSegment firstLine = lines.first();
				lineStack = new Stack<VerticalLineSegment>();
				Stack<BridgeCandidate> candidateStack = new Stack<BridgeCandidate>();
				Stack<Boolean> fromLeftStack = new Stack<Boolean>();
				Stack<Boolean> candidateFromLeftStack = new Stack<Boolean>();
				lineStack.push(firstLine);
				fromLeftStack.push(true);
				VerticalLineGroup group = new VerticalLineGroup(this);
				List<BridgeCandidate> touchedCandidates = new ArrayList<BridgeCandidate>();
				while (!lineStack.isEmpty()) {
					while (!lineStack.isEmpty()) {
						VerticalLineSegment line = lineStack.pop();
						boolean fromLeft = fromLeftStack.pop();
						if (line.touched)
							continue;

						line.touched = true;
						if (candidateList.contains(line)) {
							// a candidate!
							LOG.debug("Touching candidate x = " + line.x + ", top = " + line.yTop + ", bottom = " + line.yBottom);
							BridgeCandidate candidate = null;
							for (BridgeCandidate existingCandidate : candidateList) {
								if (existingCandidate.equals(line)) {
									candidate = existingCandidate;
									break;
								}
							}

							boolean foundCandidate = touchedCandidates.contains(candidate);

							if (!foundCandidate) {
								touchedCandidates.add(candidate);
								candidateStack.push(candidate);
								candidateFromLeftStack.push(fromLeft);
								if (fromLeft) {
									// coming from the left
									group.rightCandidates.add(candidate);
									candidate.leftGroup = group;
								} else {
									group.leftCandidates.add(candidate);
									candidate.rightGroup = group;
								}
							}
						} else {
							// not a candidate
							LOG.debug("Touching line length = " + line.length() + ", x = " + line.x + ", top = " + line.yTop + ", bottom = " + line.yBottom);
							group.pixelCount += line.length();
							if (line.x < group.leftBoundary)
								group.leftBoundary = line.x;
							if (line.x > group.rightBoundary)
								group.rightBoundary = line.x;
							if (line.yTop < group.topBoundary)
								group.topBoundary = line.yTop;
							if (line.yBottom > group.bottomBoundary)
								group.bottomBoundary = line.yBottom;
							for (VerticalLineSegment leftLine : line.leftSegments) {
								lineStack.push(leftLine);
								fromLeftStack.push(false);
							}
							for (VerticalLineSegment rightLine : line.rightSegments) {
								lineStack.push(rightLine);
								fromLeftStack.push(true);
							}
						}
					} // no more lines in this group
					groups.add(group);
					if (!candidateStack.isEmpty()) {
						BridgeCandidate candidate = candidateStack.pop();
						boolean fromLeft = candidateFromLeftStack.pop();
						// lineStack.push(candidate.line);
						// fromLeftStack.push(fromLeft);
						LOG.debug("*** New Group ***");
						LOG.debug("Next candidate:  x = " + candidate.x + ", top = " + candidate.yTop + ", bottom = " + candidate.yBottom);
						group = new VerticalLineGroup(this);
						if (fromLeft) {
							group.leftCandidates.add(candidate);
							candidate.rightGroup = group;
						} else {
							group.rightCandidates.add(candidate);
							candidate.leftGroup = group;
						}

						// add this candidate's neighbours to the lineStack
						for (VerticalLineSegment leftLine : candidate.leftSegments) {
							lineStack.push(leftLine);
							fromLeftStack.push(false);
						}
						for (VerticalLineSegment rightLine : candidate.rightSegments) {
							lineStack.push(rightLine);
							fromLeftStack.push(true);
						}
					} // next candidate on candidate stack
				} // no more lines to process

				if (LOG.isDebugEnabled()) {
					LOG.debug("Found " + groups.size() + " groups");
					int i = 1;
					for (VerticalLineGroup aGroup : groups) {
						LOG.debug(
								"Group " + i++ + ", pixelCount: " + aGroup.pixelCount + ", leftBoundary: " + aGroup.leftBoundary + ", rightBoundary: " + aGroup.rightBoundary);
						LOG.debug("Candidates on left: ");
						for (BridgeCandidate candidate : aGroup.leftCandidates)
							LOG.debug("Candidate x = " + candidate.x + ", top = " + candidate.yTop + ", bottom = " + candidate.yBottom);
						LOG.debug("Candidates on right: ");
						for (BridgeCandidate candidate : aGroup.rightCandidates)
							LOG.debug("Candidate x = " + candidate.x + ", top = " + candidate.yTop + ", bottom = " + candidate.yBottom);

					}
					LOG.debug("Found " + candidateList.size() + " candidates");
					for (BridgeCandidate candidate : candidateList) {
						LOG.debug("Candidate x = " + candidate.x + ", top = " + candidate.yTop + ", bottom = " + candidate.yBottom);
						LOG.debug("- Left group = pixelCount: " + candidate.leftGroup.pixelCount + ", leftBoundary: " + candidate.leftGroup.leftBoundary
								+ ", rightBoundary: " + candidate.leftGroup.rightBoundary);
						LOG.debug("- Right group = pixelCount: " + candidate.rightGroup.pixelCount + ", leftBoundary: " + candidate.rightGroup.leftBoundary
								+ ", rightBoundary: " + candidate.rightGroup.rightBoundary);
					}
				} // should we log?

				// calculate each candidate's pixel totals and boundaries
				for (BridgeCandidate candidate : candidateList) {
					for (VerticalLineGroup lineGroup : groups)
						lineGroup.touched = false;
					Stack<VerticalLineGroup> groupStack = new Stack<VerticalLineGroup>();
					groupStack.push(candidate.leftGroup);
					while (!groupStack.isEmpty()) {
						VerticalLineGroup lineGroup = groupStack.pop();
						if (lineGroup.touched)
							continue;
						lineGroup.touched = true;
						candidate.leftPixels += lineGroup.pixelCount;
						if (lineGroup.leftBoundary < candidate.leftShapeLeftBoundary)
							candidate.leftShapeLeftBoundary = lineGroup.leftBoundary;
						if (lineGroup.rightBoundary > candidate.leftShapeRightBoundary)
							candidate.leftShapeRightBoundary = lineGroup.rightBoundary;
						for (BridgeCandidate leftCandidate : lineGroup.leftCandidates) {
							if (!candidate.equals(leftCandidate)) {
								candidate.leftPixels += leftCandidate.length();
								groupStack.push(leftCandidate.leftGroup);
							}
						}
						for (BridgeCandidate rightCandidate : lineGroup.rightCandidates) {
							if (!candidate.equals(rightCandidate)) {
								candidate.leftPixels += rightCandidate.length();
								groupStack.push(rightCandidate.rightGroup);
							}
						}
					} // next left group
					groupStack.push(candidate.rightGroup);
					while (!groupStack.isEmpty()) {
						VerticalLineGroup lineGroup = groupStack.pop();
						if (lineGroup.touched)
							continue;
						lineGroup.touched = true;
						candidate.rightPixels += lineGroup.pixelCount;
						if (lineGroup.leftBoundary < candidate.rightShapeLeftBoundary)
							candidate.rightShapeLeftBoundary = lineGroup.leftBoundary;
						if (lineGroup.rightBoundary > candidate.rightShapeRightBoundary)
							candidate.rightShapeRightBoundary = lineGroup.rightBoundary;
						for (BridgeCandidate leftCandidate : lineGroup.leftCandidates) {
							if (!candidate.equals(leftCandidate)) {
								candidate.rightPixels += leftCandidate.length();
								groupStack.push(leftCandidate.leftGroup);
							}
						}
						for (BridgeCandidate rightCandidate : lineGroup.rightCandidates) {
							if (!candidate.equals(rightCandidate)) {
								candidate.rightPixels += rightCandidate.length();
								groupStack.push(rightCandidate.rightGroup);
							}
						}
					} // next right group
				} // next candidate

			} // do we have any candidates?
			this.bridgeCandidates = candidateList;
		} // lazy load

		return this.bridgeCandidates;
	}

	/**
	 * Of all of the bridge candidates in this shape, gives the single best
	 * candidate. Criteria include: bridge width, pixel weight to either side of
	 * the bridge, and overlap between right and left shapes.
	 * 
	 * @maxBridgeWidth the maximum width in pixels of the bridge - used to reduce
	 *                 the search space
	 */
	BridgeCandidate getBestBridgeCandidate(double maxBridgeWidth) {
		Collection<BridgeCandidate> bridgeCandidates = this.getBridgeCandidates(maxBridgeWidth);
		BridgeCandidate bestCandidate = null;
		if (bridgeCandidates.size() == 0) {
			// do nothing
		} else if (bridgeCandidates.size() == 1) {
			bestCandidate = bridgeCandidates.iterator().next();
		} else {
			// TODO: rank the candidates
			// we could do machine learning here, but we'll need a proper dataset for
			// that
			double bestScore = 0;
			for (BridgeCandidate candidate : bridgeCandidates) {
				if (candidate.score() > bestScore) {
					bestCandidate = candidate;
					bestScore = candidate.score();
				}
			}
		}
		return bestCandidate;
	}

	/**
	 * Of all of the bridge candidates in this shape, gives the single best
	 * candidate. Criteria include: bridge width, pixel weight to either side of
	 * the bridge, and overlap between right and left shapes. To select bridge
	 * candidates, uses a maximum width (in pixels) based on the x-height.
	 */
	public BridgeCandidate getBestBridgeCandidate() {
		double maxBridgeWidth = this.getXHeight() / 4.0;
		return this.getBestBridgeCandidate(maxBridgeWidth);
	}

	/**
	 * Return any splits marked on this shape.
	 */
	public List<Split> getSplits() {
		if (splits == null) {
			splits = new PersistentListImpl<Split>();
			BoundaryDao boundaryDao = BoundaryDao.getInstance(jochreSession);
			splits.addAll(boundaryDao.findSplits(this));
		}
		return splits;
	}

	/**
	 * Add a split to this shape at the indicated position.
	 */
	public Split addSplit(int position) {
		List<Split> splits = this.getSplits();
		Split split = new Split(this, jochreSession);
		split.setPosition(position);
		splits.add(split);
		return split;
	}

	/**
	 * Remove a split from this shape at the indicated position.
	 */
	public void deleteSplit(int position) {
		Split splitToDelete = null;
		for (Split split : this.getSplits()) {
			if (split.getPosition() == position) {
				splitToDelete = split;
				break;
			}
		}
		if (splitToDelete != null)
			this.getSplits().remove(splitToDelete);
	}

	/**
	 * The distance to the shape's edges, as seen from the top and bottom.
	 * 
	 * @return int[width][2], giving, for each x-coordinate in the shape, the
	 *         distance to the shape's first black pixel, as seen from the top (0)
	 *         and bottom (1).
	 */
	public int[][] getVerticalContour() {
		if (verticalContour == null) {
			verticalContour = new int[this.getWidth()][2];
			for (int x = 0; x < this.getWidth(); x++) {
				for (int y = 0; y < this.getHeight(); y++) {
					if (this.isPixelBlack(x, y)) {
						verticalContour[x][0] = y;
						break;
					}
				}
				for (int y = this.getHeight() - 1; y >= 0; y--) {
					if (this.isPixelBlack(x, y)) {
						verticalContour[x][1] = y;
						break;
					}
				}
			}
		}
		return verticalContour;
	}

	@Override
	public int hashCode() {
		final int prime = 3;
		int result = 1;
		result = prime * result + bottom;
		result = prime * result + left;
		result = prime * result + right;
		result = prime * result + top;
		result = prime * result + ((jochreImage == null) ? 0 : jochreImage.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Shape other = (Shape) obj;
		if (bottom != other.bottom)
			return false;
		if (left != other.left)
			return false;
		if (right != other.right)
			return false;
		if (top != other.top)
			return false;
		if (!this.getJochreImage().equals(other.getJochreImage()))
			return false;
		return true;
	}

	@Override
	public Shape getShape() {
		return this;
	}

	/**
	 * The confidence in the current letter guess, in a scale from 0 to 1.
	 */
	public double getConfidence() {
		if (confidence == null) {
			confidence = 1.0;
			if (this.letterGuesses != null) {
				for (Decision guess : this.letterGuesses) {
					if (guess.getOutcome().equals(this.letter)) {
						confidence = guess.getProbability();
						break;
					}
				}
			}
		}
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	@Override
	@SuppressWarnings("unchecked")

	public <T, Y> FeatureResult<Y> getResultFromCache(Feature<T, Y> feature, RuntimeEnvironment env) {
		FeatureResult<Y> result = null;

		String key = feature.getName() + env.getKey();
		if (this.featureResults.containsKey(key)) {
			result = (FeatureResult<Y>) this.featureResults.get(key);
		}
		return result;
	}

	@Override
	public <T, Y> void putResultInCache(Feature<T, Y> feature, FeatureResult<Y> featureResult, RuntimeEnvironment env) {
		String key = feature.getName() + env.getKey();
		this.featureResults.put(key, featureResult);
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
