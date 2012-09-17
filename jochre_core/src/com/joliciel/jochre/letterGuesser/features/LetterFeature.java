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
package com.joliciel.jochre.letterGuesser.features;

import com.joliciel.jochre.letterGuesser.LetterGuesserContext;
import com.joliciel.talismane.utils.features.Feature;
import com.joliciel.talismane.utils.features.FeatureResult;

/**
 * A single feature that checks a given shape and returns a result.
 * The result can include a nominal part and a weight.
 * The weight should be a value from 0 to 1.
 * @author Assaf Urieli
 */
public interface LetterFeature<Y> extends Feature<LetterGuesserContext,Y> {
	/**
	 * Check the feature on this shape and return the result.
	 * @param shape the shape to be checked.
	 * @param history the letter sequence assigned during the current analysis to shapes preceding the current shape within the same word
	 * @return
	 */
	public FeatureResult<Y> check(LetterGuesserContext context);
}
