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
 * Used to fill "holes" within a shape, and also determine how much filling is required.
 * @author Assaf Urieli
 *
 */
public interface ShapeFiller {
	
	/**
	 * Given a shape and a "black" threshold, how many time do we need to run the filling
	 * algorithm to fill up the holes.
	 */
	public int getFillFactor(Shape shape, int threshold);
	
	/**
	 * Return a bitset corresponding to the "filled-in" shape.
	 */
	public BitSet fillShape(Shape shape, int threshold, int fillFactor);
}
