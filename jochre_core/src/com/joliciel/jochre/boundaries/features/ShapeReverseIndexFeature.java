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
package com.joliciel.jochre.boundaries.features;

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.jochre.boundaries.ShapeInSequence;

/**
 * Returns shape index counting from the end of the group containing it,
 * but only for the last and next-to-last shape.
 * @author Assaf Urieli
 *
 */
public final class ShapeReverseIndexFeature extends AbstractShapeInSequenceFeature<Integer> implements IntegerFeature<ShapeInSequence> {
	@Override
	public FeatureResult<Integer> checkInternal(ShapeInSequence shapeInSequence) {
		FeatureResult<Integer> outcome = null;
		int reverseIndex = shapeInSequence.getShapeSequence().size() - (shapeInSequence.getIndex()+1);
		if (reverseIndex<=1) {
			outcome = this.generateResult(reverseIndex);
		}
		return outcome;
	}
}
