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

/**
 * A line segment within an image, defined by its start point and end point.
 * @author Assaf Urieli
 *
 */
public interface LineSegment extends Comparable<LineSegment> {

	public abstract void setLength(int length);

	public abstract int getLength();

	public abstract int getEndY();

	public abstract int getEndX();

	public abstract int getStartY();

	public abstract int getStartX();
	
	/**
	 * The line definition defining this line segment.
	 */
	public abstract LineDefinition getLineDefinition();
	
	/**
	 * The shape containing this line segment.
	 */
	public abstract Shape getShape();
	
	/**
	 * Returns a (tilted) rectangle
	 * enclosing this line segment if each end-point is extended by halfWidth pixels
	 * in both directions towards the edges of the shape.
	 * 
	 */
	public abstract BitSet getEnclosingRectangle(int halfWidth);
	
	/**
	 * The intersection of the enclosing rectangle of this line segment
	 * with the enclosing rectangle of another line segment.
	 */
	public abstract BitSet getEnclosingRectangleIntersection(LineSegment otherLine, int halfWidth);
	
}
