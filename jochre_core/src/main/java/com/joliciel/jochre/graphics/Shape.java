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
import java.util.BitSet;
import java.util.List;
import java.util.Set;

import com.joliciel.jochre.Entity;
import com.joliciel.jochre.boundaries.Split;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.features.HasFeatureCache;

/**
 * A rectangle containing a shape that needs to be identified as a grapheme.
 * @author Assaf Urieli
 */
public interface Shape extends ImageGrid, Entity, Rectangle, ShapeWrapper, HasFeatureCache {
	
	/**
	 * Different methods for measuring the brightness of various sections of the shape,
	 * where a section is a rectangle within a grid that has been overlaid onto the shape.
	 */
	public enum SectionBrightnessMeasurementMethod {
		/**
		 * A simple sum of the brightness for all pixels in each section,
		 * where white = 0 and black = 255.
		 */
		RAW,
		
		/**
		 * The sum provided by RAW, divided by the number of pixels in the section.
		 * Since sections can have different sizes, it's critical to normalise.
		 */
		SIZE_NORMALISED,
		
		/**
		 * The section with the maximum normalised brightness is taken to be 1.
		 * All other sections are given a value from 0 to 1, based on their
		 * normalised brightness relative to the max.
		 */
		RELATIVE_TO_MAX_SECTION,
		
		/**
		 * For each section, gives a value from 0 to 1, showing which
		 * portion of the total brightness it contains.
		 */
		PORTION_OF_TOTAL_BRIGHTNESS,
	}
	
	/**
	 * The leftmost x coordinate of this shape within the containing ImageGrid.
	 */
	public int getLeft();
	
	/**
	 * The topmost y coordinate of this shape within the containing ImageGrid.
	 */
	public int getTop();
	public int getRight();
	public int getBottom();

	public void setLeft(int left);
	public void setTop (int top);
	public void setRight (int right);
	public void setBottom (int bottom);
	
	/**
	 * Checks if a pixel is black using the container image's calculated black threshold.
	 */
	public boolean isPixelBlack(int x, int y);

	/**
	 * Similar to {@link #isPixelBlack(int, int, int)} but adds an additional
	 * "white gap fill factor" to help fill in white spaces when an algorithm
	 * is sensitive to these.
	 * Basically, the fill factor fills in any pixel when at least 5 out of 8
	 * surrounding the pixel are black. This is performed n times, where n =
	 * whiteGapFillFactor, progressively filling in any white gaps.
	 * Note that outlines will also get "padded", as the algorithm doesn't 
	 * differentiate internal gaps from external ones.
	 */
	public boolean isPixelBlack(int x, int y, int threshold, int whiteGapFillFactor);
	
	/**
	 * Return a BitSet representation of this shape
	 * for a given brightness threshold and given white gap fill factor.
	 * See {@link #isPixelBlack(int, int, int, int)} for details on how we determine
	 * if a given bit is true or false. The BitSet structured from top-left to bottom-right,
	 * travelling horizontally first.
	 */
	public BitSet getBlackAndWhiteBitSet(int threshold);

	/**
	 * Return a BitSet representation of this shape
	 * for a given brightness threshold and given white gap fill factor.
	 * See {@link #isPixelBlack(int, int, int, int)} for details on how we determine
	 * if a given bit is true or false. The BitSet structured from top-left to bottom-right,
	 * travelling horizontally first.
	 */
	public BitSet getBlackAndWhiteBitSet(int threshold, int whiteGapFillFactor);
	
	/**
	 * The image containing this shape.
	 */
	public JochreImage getJochreImage();
	
	/**
	 * The group of shapes containing this particular shape.
	 */
	public GroupOfShapes getGroup();
	public void setGroup(GroupOfShapes group);
	
	/**
	 * The relative y-index of line at the bottom of standard lowercase letters.
	 */
	public int getBaseLine();
	public void setBaseLine(int baseLine);

	/**
	 * The relative y-index of the line at the top of standard lowercase letters.
	 */
	public int getMeanLine();
	public void setMeanLine(int meanLine);

	/**
	 * The relative y-index of the line at the top of standard uppercase letters.
	 */
	public int getCapLine();
	public void setCapLine(int capLine);
	
	public int getIndex();
	public void setIndex(int index);

	/**
	 * The letter represented by this shape (for shapes in the training set).
	 */
	public String getLetter();
	public void setLetter(String letter);
	
	/**
	 * The original guess for this shape, when it was automatically analysed by Jochre.
	 */
	public String getOriginalGuess();
	public void setOriginalGuess(String originalGuess);

	public int getGroupId();
	public void setGroupId(int groupId);
	
	public void delete();
	
	public RowOfShapes getRow();
	public void setRow(RowOfShapes row);
	
	/**
	 * Divides the shape as follows:
	 * Above the meanline, there will be marginSectionCount rows by verticalSectionCount columns
	 * Between the meanline and baseline, there will be horizontalSectionCount rows by verticalSectionCount columns
	 * Below the baseline, there will be marginSectionCount rows by verticalSectionCount columns
	 * For example, if verticalSectionCount = 5, horizontalSectionCount = 5 and marginSectionCount = 1:<br/>
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
	 * </code>
	 * If a section break occurs inside a pixel, the pixel's brightness count
	 * is divided proportionally between the two sections.
	 * @return double[verticalSectionCount][horizontalSectionCount+2*marginSectionCount]
	 */
	public double[][] getBrightnessBySection(int verticalSectionCount, int horizontalSectionCount, int marginSectionCount, SectionBrightnessMeasurementMethod measurementMethod);
	
	/**
	 * The starting rectangle is taken based on the shape's xHeight (baseline - meanline).
	 * Basically, we take a square with the following boundaries:
	 * top = meanline, bottom = baseline, right = right, left = right - xHeight.
	 * To this, we add at the top and the bottom a margin, each with a height of xHeight * topBottomMarginWidth.
	 * We add on the right (or on the left, if lang is right-to-left) a margin with a width of xHeight * horizontalMarginWidth.
	 * Any pixels outside the shape's original boundaries are assumed to be white.
	 * Any pixels inside the shape's original boundaries, but outside the rectangle's boundaries are ignored
	 * (in the case where the shape is wider than xHeight * (1+horizontalMarginWidth), or taller than xHeight * (1+2*topBottomMarginWidth)).
	 * The resulting rectangle is divided into verticalSectionCount equally spaced vertical sections,
	 * and horizontalSectionCount equally spaced horizontal sections.
	 * If a section break occurs inside a pixel, the pixel's brightness count
	 * is divided proportionally between the two sections.
	 * @param verticalSectionCount the number of vertical sections
	 * @param horizontalSectionCount the number of horizontal sections
	 * @param topBottomMarginWidth the width of the top magin
	 * @return double[verticalSectionCount][horizontalSectionCount]
	 */
	public double[][] getBrightnessBySection(int verticalSectionCount, int horizontalSectionCount, double topBottomMarginWidth, double horizontalMarginWidth, SectionBrightnessMeasurementMethod measurementMethod);

	/**
	 * Similar to the other getBrightnessTotalsBySector methods, except that the base rectangle is taken to be exactly the shape's rectangle,
	 * with no special treatment of the areas above the meanline or below the baseline.
	 * The rectangle is simply divided into verticalSectionCount vertical sections and horizontalSectionCount horizontal sections.
	 * If a section break occurs inside a pixel, the pixel's brightness count
	 * is divided proportionally between the two sections.
	 * @return double[verticalSectionCount][horizontalSectionCount]
	 */
	public double[][] getBrightnessBySection(int verticalSectionCount, int horizontalSectionCount, SectionBrightnessMeasurementMethod measurementMethod);

	/**
	 * Mean brightness for the sections defined above.
	 */
	public double getBrightnessMeanBySection(int verticalSectionCount, int horizontalSectionCount, int marginSectionCount, SectionBrightnessMeasurementMethod measurementMethod);
	/**
	 * Mean brightness for the sections defined above.
	 */
	public double getBrightnessMeanBySection(int verticalSectionCount, int horizontalSectionCount, SectionBrightnessMeasurementMethod measurementMethod);
	/**
	 * Mean brightness for the sections defined above.
	 */
	public double getBrightnessMeanBySection(int verticalSectionCount, int horizontalSectionCount, double topBottomMarginWidth, double horizontalMarginWidth, SectionBrightnessMeasurementMethod measurementMethod);
	
	/**
	 * Find outline of the shape as a BitSet.
	 */
	public BitSet getOutline(int threshold);
	
	/**
	 * Get the total brightness counts for each vertical line.
	 * @return an array of int with size {@link #getWidth()}.
	 */
	public int[] getVerticalCounts();
	
	/**
	 * Recalculate the various statistical measurements for this shape.
	 * Should be called after the shape coordinates have changed.
	 */
	public void recalculate();
	
	/**
	 * Get the image behind this shape.
	 */
	public BufferedImage getImage();
	
	/**
	 * Get the centre point of this shape.
	 */
	public double[] getCentrePoint();
	
	/**
	 * An ordered set of letter guesses for the current shape.
	 */
	public Set<Decision> getLetterGuesses();
	
	/**
	 * Writes a textual form of the image (pixel by pixel) to the log.
	 */
	public void writeImageToLog();

	public abstract int getTotalBrightness();
	
	/**
	 * The point from which this shape was actually built.
	 * Only available when segmenting (not saved).
	 */
	public int[] getStartingPoint();

	/**
	 * Difference between baseline and meanline.
	 */
	public abstract int getXHeight();
	
	/**
	 * Of all of the bridge candidates in this shape, gives the single best candidate.
	 * Criteria include: bridge width, pixel weight to either side of the bridge, and 
	 * overlap between right and left shapes.
	 * To select bridge candidates, uses a maximum width (in pixels) based on the x-height.
	 */
	public BridgeCandidate getBestBridgeCandidate();
	
	/**
	 * Return any splits marked on this shape.
	 */
	public List<Split> getSplits();
	
	/**
	 * Add a split to this shape at the indicated position.
	 */
	public Split addSplit(int position);
	
	/**
	 * Remove a split from this shape at the indicated position.
	 */
	public void deleteSplit(int position);
	
	/**
	 * The distance to the shape's edges, as seen from the top and bottom.
	 * @return int[width][2], giving, for each x-coordinate in the shape, the distance to the shape's first black pixel, as seen from the top (0) and bottom (1).
	 */
	public int[][] getVerticalContour();
	
	/**
	 * The confidence in the current letter guess, in a scale from 0 to 1.
	 */
	public double getConfidence();
	public void setConfidence(double confidence);
}
