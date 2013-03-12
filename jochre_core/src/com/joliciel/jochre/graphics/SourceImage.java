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

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A wrapper for a JochreImage which includes an actual graphical image.
 * In addition to an ImageGrid, the SourceImage has certain calculated parameters to help breaking it up into letters.
 * The brightness of the underlying image is normalised to allow comparable black-to-white ranges regardless
 * of the lightest and darkest pixels.
 * @author Assaf
 *
 */
public interface SourceImage extends JochreImage, ImageGrid {	
	/**
	 * The rows found in this image (ignoring paragraph splits, or before paragraph splits).
	 */
	public List<RowOfShapes> getRows();
	
	/**
	 * Add a row to this source image.
	 * @return
	 */
	public void addRow(RowOfShapes row);
	
	/**
	 * Remove a row from the current image.
	 * @param row
	 */
	public void removeRow(RowOfShapes row);
	
	/**
	 * Replace a given row with a set of replacement rows.
	 * @param row
	 * @param newRows
	 */
	public void replaceRow(RowOfShapes row, List<RowOfShapes> newRows);
	
	/**
	 * Get clusters of rows, clustered together by height.
	 * @return
	 */
	public Set<Set<RowOfShapes>> getRowClusters();
	
	/**
	 * The containing document's locale.
	 * @return
	 */
	public Locale getLocale();
		
	/**
	 * Return the "average" width for shapes currently included on rows in this image.
	 * Because of the possibility of a skewed distribution, returns the median.
	 * @return
	 */
	public double getAverageShapeWidth();
	
	/**
	 * Returns a margin to consider on either side of the average shape width, to return only "average shapes".
	 * @return
	 */	
	public double getAverageShapeWidthMargin();
	
	/**
	 * Return the "average" height for shapes currently included on rows in this image.
	 * Because of the possibility of a skewed distribution, returns the median.
	 * @return
	 */
	public double getAverageShapeHeight();
	
	/**
	 * Returns a margin to consider on either side of the average shape height, to return only "average shapes".
	 * @return
	 */	
	public double getAverageShapeHeightMargin();
	
	/**
	 * Recalculate the various statistical measurements for this source image.
	 * Should be called after any shapes are added/removed.
	 */
	public void recalculate();
	
	/**
	 * Calculate the mean horizontal slope of rows on this image.
	 */
	public double getMeanHorizontalSlope();

	/**
	 * Get white areas which delimit rows (to break columns up into separate rows).
	 * Assumes specks have already been removed (to avoid reducing white areas artificially).
	 * @param shapes the shapes to be considered when looking for white space.
	 * @return a List of {whiteArea.left, whiteArea.top, whiteArea.right, whiteArea.bottom}
	 */
	public abstract List<Rectangle> getWhiteAreas(List<Shape> shapes);

	public abstract List<Shape> getLargeShapes();

	/**
	 * Get white areas around large shapes.
	 * Assumes rows have already been calculated.
	 * Used for recognising "false indents" when delimiting paragraphs.
	 * @return
	 */
	public abstract List<Rectangle> getWhiteAreasAroundLargeShapes();
	
	public void setShapeCount(int shapeCount);
	
	/**
	 * Returns rectangles representing separations between columns.
	 * @return
	 */
	public List<Rectangle> findColumnSeparators();
	
	/**
	 * Get the x-adjustment at a particular y-coordinate, in view of the page slope.
	 * @param yCoordinate
	 * @return
	 */
	public double getXAdjustment(double yCoordinate);

	public abstract void setDrawPixelSpread(boolean drawPixelSpread);

	public abstract boolean isDrawPixelSpread();
}
