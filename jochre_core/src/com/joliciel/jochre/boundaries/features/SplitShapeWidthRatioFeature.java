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
import com.joliciel.jochre.graphics.Shape;

/**
 * The intuition behind this feature is that if a shape is split, the right-hand shape and left-hand shape
 * should have approximately the same width (in pixels).
 * Given the right shape and left shape,
 * a feature giving the ratio of the narrower shape to the wider shape.
 * The ratio will always be a value from 0 to 1.
 * @author Assaf Urieli
 *
 */
public class SplitShapeWidthRatioFeature extends AbstractSplitFeature<Double> implements DoubleFeature<Split> {

	@Override
	public FeatureResult<Double> checkInternal(Split split, RuntimeEnvironment env) {
		FeatureResult<Double> result = null;
		Shape shape = split.getShape();
		int rightWidth = (shape.getWidth() - 1) - (split.getPosition()+1);
		int leftWidth = split.getPosition() - 1;
		
		double ratio = (double) leftWidth / (double) rightWidth;
		if (ratio > 1)
			ratio = 1 / ratio;
		
		result = this.generateResult(ratio);
		return result;
	}

}
