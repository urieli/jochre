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

import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

/**
 * Returns shape index starting at the start of the group containing it.
 * 
 * @author Assaf Urieli
 *
 */
public final class ShapeIndexFeature extends AbstractShapeInSequenceFeature<Integer> implements IntegerFeature<ShapeInSequenceWrapper> {
	@Override
	public FeatureResult<Integer> checkInternal(ShapeInSequenceWrapper wrapper, RuntimeEnvironment env) {
		ShapeInSequence shapeInSequence = wrapper.getShapeInSequence();
		FeatureResult<Integer> outcome = this.generateResult(shapeInSequence.getIndex());
		return outcome;
	}
}
