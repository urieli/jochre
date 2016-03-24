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

import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.jochre.boundaries.Split;

/**
 * The intuition behind this feature is that if a shape is split, the right-hand shape and left-hand shape
 * should have approximately the same weight (in pixels).
 * Given the right shape and left shape,
 * a feature giving the ratio of the smaller shape to the larger shape (in terms of total brighteness).
 * The ratio will always be a value from 0 to 1.
 * @author Assaf Urieli
 *
 */
public class SplitShapeWeightRatioFeature extends AbstractSplitFeature<Double> implements DoubleFeature<Split> {

	@Override
	public FeatureResult<Double> checkInternal(Split split, RuntimeEnvironment env) {
		FeatureResult<Double> result = null;
		int[] verticalCounts = split.getShape().getVerticalCounts();
		int rightCount = 0;
		int leftCount = 0;
		for (int i = 0; i<split.getShape().getWidth(); i++) {
			if (i<split.getPosition())
				leftCount += verticalCounts[i];
			if (i>split.getPosition())
				rightCount += verticalCounts[i];
		}
		double ratio = (double) leftCount / (double) rightCount;
		if (ratio > 1)
			ratio = 1 / ratio;
		
		result = this.generateResult(ratio);
		return result;
	}

}
