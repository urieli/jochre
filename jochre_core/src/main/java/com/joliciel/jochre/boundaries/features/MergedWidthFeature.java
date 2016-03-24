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
import com.joliciel.jochre.boundaries.ShapePair;

/**
 * Provides the ratio of the merged width to the x-height, scaled so that 1.5 = 1.0.
 * @author Assaf Urieli
 */
public class MergedWidthFeature extends AbstractMergeFeature<Double> implements DoubleFeature<ShapePair> {
	DoubleFeature<ShapePair> maxWidthFeature;
	
	public MergedWidthFeature(DoubleFeature<ShapePair> maxWidthFeature) {
		super();
		this.maxWidthFeature = maxWidthFeature;
		this.setName(super.getName() + "(" + maxWidthFeature.getName() + ")");
	}


	@Override
	public FeatureResult<Double> checkInternal(ShapePair pair, RuntimeEnvironment env) {
		FeatureResult<Double> result = null;
		FeatureResult<Double> maxWidthResult = maxWidthFeature.check(pair, env);
		if (maxWidthResult!=null) {
			double maxWidth = maxWidthResult.getOutcome();
			double width = pair.getWidth();
			
			double xHeight = pair.getXHeight();
			
			double ratio = (width / xHeight) / (maxWidth);
			if (ratio > 1.0)
				 ratio = 1.0;
			
			result = this.generateResult(ratio);
		}
		return result;
		
	}

}
