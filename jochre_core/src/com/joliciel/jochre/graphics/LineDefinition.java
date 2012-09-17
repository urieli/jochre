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

import java.util.BitSet;
import java.util.List;

/**
 * Defines a line direction from an origin point
 * in a manner which simplifies manipulation in raster spaces.
 * @author Assaf Urieli
 *
 */
public interface LineDefinition {
	/**
	 * The 45 degree sector of this line definition.
	 * Sector 0 starts at the horizontal facing left and moves down to
	 * a diagonal facing left and down. There are 8 sectors.
	 * @return
	 */
	int getSector();
	
	/**
	 * The index of this line definition.
	 * Starts at 0 for the horizontal going left, and increases counter-clockwise.
	 * There are 8 lines per 45 degree sector.
	 * Thus, lines with an adjacent index will have a similar slope.
	 * @return
	 */
	int getIndex();
	
	/**
	 * The x delta of the default axis. Values will be -1, 0 or +1.
	 * @return
	 */
	int getDeltaX();
	
	/**
	 * The y delta of the default axis. Values will be -1, 0 or +1.
	 * @return
	 */
	int getDeltaY();
	
	/**
	 * The x-increment to take when a step has been completed.
	 * Values will be -1, 0 or +1.
	 * @return
	 */
	int getXIncrement();
	
	/**
	 * The y-increment to take when a step has been completed.
	 * Values will be -1, 0 or +1.
	 * @return
	 */
	int getYIncrement();
	
	/**
	 * The numbers represent the pixels to traverse before in the default axis
	 * before making a single increment in the increment axis.
	 * Thus, {1} means take 1 step in the default axis and increments simultaneously.
	 * {2} means take 2 steps in the default axis, incrementing on the 2nd step.
	 * {2,1} means take 2 steps, increment, take 1 step, increment, and repeat.
	 * {0} represents infinity = stay in the default axis.
	 */
	List<Integer> getSteps();
	void setSteps(List<Integer> steps);
	
	/**
	 * Trace a line using this line definition from a given origin
	 * for a number of pixels = length.
	 * If parts of the line lie outside the bitset, they will simply not be traced.
	 * The rotation allows us to rotate the sector of this line definition
	 * counter-clockwise by the number of sectors indicated. 
	 * @param bitset bitset in which to trace the line
	 * @param shape shape containing this line
	 * @param xOrigin
	 * @param yOrigin
	 * @param length in pixels excluding the origin
	 * @param rotation number of sectors to rotate, can be a negative number.
	 */
	void trace(BitSet bitset, Shape shape, int xOrigin, int yOrigin, int length, int rotation);
	
	/**
	 * Follow a line using this line definition from a given origin
	 * for a number of pixels = length.
	 * The rotation allows us to rotate the sector of this line definition
	 * counter-clockwise by the number of sectors indicated. 
	 * @param shape shape containing this line
	 * @param xOrigin
	 * @param yOrigin
	 * @param length in pixels excluding the origin
	 * @param rotation number of sectors to rotate, can be a negative number.
	 * @return the x,y coordinates of the resulting point as {x,y}
	 */
	int[] follow(Shape shape, int xOrigin, int yOrigin, int length, int rotation);
	
	/**
	 * Follow this line until we exit the shape (that is, until we hit a pixel in the shape
	 * that is off). For other parameters @see #follow(Shape, int, int, int, int)
	 * @param threshold the threshold below which a pixel is considered black (on)
	 * @return the last pixel along the line that is black, and the total length, as {x, y, length}
	 */
	int[] followInShape(Shape shape, int xOrigin, int yOrigin, int rotation, int threshold, int whiteGapFillFactor);
	
	/**
	 * Given a known vector within a shape, find the lengths of line segments perpendicular to this vector,
	 * sampled every sampleGap pixels.
	 * @param shape the shape to analyse
	 * @param xOrigin the x-origin of the vector
	 * @param yOrigin the y-origin of the vector
	 * @param length the length of the vector
	 * @param threshold the threshold for considering a pixel black
	 * @param whiteGapFillFactor
	 * @param sampleStep the number of pixels to skip before taking the next sample
	 * @return a List of Integer containing the length of each line segment sampled
	 */
	List<Integer> findArrayListThickness(Shape shape, int xOrigin, int yOrigin, int length, int threshold, int whiteGapFillFactor, int sampleStep);
}
