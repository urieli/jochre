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
import com.joliciel.jochre.boundaries.Split;

/**
 * The intuition behind this feature is that a bridge in a merged shape
 * should be no thicker than half the x-height, and the thinner the better.
 * A feature giving (x-height * 0.5) / (total vertical brightness at split position / 255)
 * Values range from 0 to 1
 * @author Assaf Urieli
 *
 */
public class BridgeWidthFeature extends AbstractSplitFeature<Double> implements DoubleFeature<Split> {

	@Override
	public FeatureResult<Double> checkInternal(Split split) {
		FeatureResult<Double> result = null;
		int[] verticalCounts = split.getShape().getVerticalCounts();
		double verticalCount = (double) verticalCounts[split.getPosition()] / 255;
		if (verticalCount==0)
			verticalCount = 1;
		double width = ((double) split.getShape().getXHeight() / 2.0) / verticalCount;
		if (width > 1.0)
			width = 1.0;
		result = this.generateResult(width);

		return result;
	}

}
