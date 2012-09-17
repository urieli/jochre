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
package com.joliciel.jochre.letterGuesser;

import com.joliciel.jochre.boundaries.ShapeInSequence;

/**
 * Guesses the letters for a given shape.
 * @author Assaf Urieli
 *
 */
public interface LetterGuesser {
	/**
	 * Analyses this shape, using the context provided for features that are not intrinsic.
	 * Updates shape.getWeightedOutcomes to include all outcomes above a certain threshold of probability.
	 * @param shape
	 * @return the best outcome for this shape.
	 */
	public abstract String guessLetter(ShapeInSequence shapeInSequence, LetterSequence history);
}