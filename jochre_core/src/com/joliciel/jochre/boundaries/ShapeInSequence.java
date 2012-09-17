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
package com.joliciel.jochre.boundaries;

import java.util.List;

import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;

/**
 * Represents a Shape forming part of a ShapeSequence.
 * Allows us to include the same Shape in multiple Sequences, which is useful to avoid having to
 * recalculate various shape parameters several times.
 * Also allows us to gather information about the shape's position in the sequence and the other shapes
 * in the sequence.
 * @author Assaf Urieli
 *
 */
public interface ShapeInSequence extends ShapeWrapper {	
	/**
	 * The index within this sequence.
	 * @return
	 */
	public int getIndex();
	public void setIndex(int index);
	
	/**
	 * The sequence containing this shape.
	 * @return
	 */
	public ShapeSequence getShapeSequence();
	
	/**
	 * Get the shape or shapes from which this shape was formed by splitting,
	 * merging, or simply placing it in the sequence.
	 * @return
	 */
	public List<Shape> getOriginalShapes();
	
}
