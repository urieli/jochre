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
import com.joliciel.talismane.machineLearning.Solution;

/**
 * A sequence of shapes resulting from a shape split or merge, with a score.
 * @author Assaf Urieli
 *
 */
public interface ShapeSequence extends List<ShapeInSequence>, Solution<SplitMergeOutcome> {
	/**
	 * The score attached to this particular sequence.
	 * @return
	 */
	public double getScore();
	
	/**
	 * Add a given shape to this sequence.
	 * @param shape
	 * @return
	 */
	public ShapeInSequence addShape(Shape shape);

	/**
	 * Add a given shape to this sequence, for the original shape provided.
	 * @param shape
	 * @return
	 */
	public ShapeInSequence addShape(Shape shape, Shape originalShape);

	/**
	 * Add a given shape to this sequence, for the original shapes in the array.
	 * @param shape
	 * @return
	 */
	public ShapeInSequence addShape(Shape shape, Shape[] originalShapes);
	
	/**
	 * Add a fiven shape to this sequence for the original shapes in the list provided.
	 * @param shape
	 * @param originalShapes
	 * @return
	 */
	public ShapeInSequence addShape(Shape shape, List<Shape> originalShapes);
}
