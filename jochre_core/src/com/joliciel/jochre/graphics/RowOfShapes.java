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
import java.util.Collection;
import java.util.List;

import org.apache.commons.math.stat.regression.SimpleRegression;

import com.joliciel.jochre.Entity;

/**
 * A single row of shapes on an image, corresponding to a written row.
 * @author Assaf Urieli
 *
 */
public interface RowOfShapes extends Entity {
	/**
	 * The width to which row images will be scaled.
	 */
	public static final int ROW_IMAGE_WIDTH = 720;

	/**
	 * The shapes contained on this row.
	 */
	public List<Shape> getShapes();
	
	/**
	 * Add a shape to this row.
	 * @param shape
	 */
	public void addShape(Shape shape);
	
	/**
	 * Add a bunch of shapes to this row.
	 * @param shapes
	 */
	public void addShapes(Collection<Shape> shapes);
	
	/**
	 * Remove a shape from this row.
	 * @param shape
	 */
	public void removeShape(Shape shape);
	
	/**
	 * Reorder the shapes in this row, to put them in the proper order again after
	 * a new shape has been added.
	 */
	public void reorderShapes();
	
	/**
	 * Get the position of the shape in the shape collection,
	 * or -1 if not found.
	 * @param shape
	 * @return
	 */
	public int getShapeIndex(Shape shape);
	
	/**
	 * The groups contained in this row.
	 * @return
	 */
	public List<GroupOfShapes> getGroups();
	
	public GroupOfShapes newGroup();
	
	/**
	 * The index of this row, from 0 (top-most in right-to-left or left-to-right languages) to n.
	 * @return
	 */
	public int getIndex();
	public void setIndex(int index);
	public int getParagraphId();
	public Paragraph getParagraph();
	
	/**
	 * The mean height of all shapes in this row.
	 * @return
	 */
	public double getMeanHeight();
	
	/**
	 * The y-coordinate middle point of the base line, based on the vertical line splitting the image in two.
	 */
	public double getBaseLineMiddlePoint();
	
	/**
	 * Find the shape closest to a given x-coordinate.
	 * @param xCoordinate
	 * @return
	 */
	public Shape findNearestShape(int xCoordinate);
	
	/**
	 * Find a shape enclosing this x-coordinate.
	 * @param xCoordinate
	 * @return
	 */
	public Shape findEnclosingShape(int xCoordinate);
	
	/**
	 * Recalculate the various statistical measurements for this row.
	 * Should be called after the row has had any shapes added or removed.
	 */
	public void recalculate();
	
	/**
	 * The leftmost x coordinate of this row (based on the shapes it contains).
	 */
	public int getLeft();
	
	/**
	 * The leftmost y coordinate of this row (based on the shapes it contains).
	 */
	public int getTop();

	/**
	 * The rightmost x coordinate of this row (based on the shapes it contains).
	 */
	public int getRight();
	
	/**
	 * The bottom-most y coordinate of this row (based on the shapes it contains).
	 */
	public int getBottom();
	
	/**
	 * Gives the height between the base-line and mean-line on this row.
	 * @return
	 */
	public int getXHeight();
	
	/**
	 * Get an image, exactly 600 pixels wide, representing this row.
	 * @return
	 */
	public BufferedImage getImage();
	
	/**
	 * Get the regression representing this row's slope/intercept.
	 * @return
	 */
	public SimpleRegression getRegression();
	
	/**
	 * Return the "average" width for shapes currently included on rows in this row.
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
	 * Return the "average" height for shapes currently included on rows in this row.
	 * Because of the possibility of a skewed distribution, returns the median.
	 */
	public double getAverageShapeHeight();
	
	/**
	 * Returns a margin to consider on either side of the average shape height, to return only "average shapes".
	 * @return
	 */	
	public double getAverageShapeHeightMargin();
	
	/**
	 * Find the baseline, meanline and capline for each shape, based on other shapes on the same row
	 * this is likely to depend on the alphabet, e.g. the hebrew alphabet has no capline as such.
	 */
	public void assignGuideLines();
	
	/**
	 * The adjustment to make to this row's x-coordinates to make it comparable with other rows,
	 * in view of the row's y-coordinate and the page's scanning slope.
	 * @return
	 */
	public double getXAdjustment();

	public abstract void organiseShapesInGroups(double letterSpaceThreshold);

	public abstract void addGroup(GroupOfShapes group);

	public abstract void splitByFontSize();

	public abstract int getXHeightMax();

	public abstract int getMaxShapeWidth();
}
