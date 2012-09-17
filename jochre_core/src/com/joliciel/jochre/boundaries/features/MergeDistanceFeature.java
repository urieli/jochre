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

import com.joliciel.talismane.utils.features.DoubleFeature;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.jochre.boundaries.ShapePair;

/**
 * The distance between the edges of the merged shapes,
 * expressed proportionally to 0.5 * the x-height.
 * @author Assaf Urieli
 *
 */
public class MergeDistanceFeature extends AbstractMergeFeature<Double> implements DoubleFeature<ShapePair> {
	DoubleFeature<ShapePair> maxDistanceFeature;
	
	public MergeDistanceFeature(DoubleFeature<ShapePair> maxDistanceFeature) {
		super();
		this.maxDistanceFeature = maxDistanceFeature;
		this.setName(super.getName() + "(" + maxDistanceFeature.getName() + ")");
	}


	@Override
	public FeatureResult<Double> checkInternal(ShapePair pair) {
		FeatureResult<Double> result = null;
		FeatureResult<Double> maxDistanceResult = maxDistanceFeature.check(pair);
		if (maxDistanceResult!=null) {
			double maxDistance = maxDistanceResult.getOutcome();
			double distance = pair.getInnerDistance();
	
			double xHeight = pair.getXHeight();
	
			double ratio = (distance / xHeight);
			ratio = ratio / maxDistance;
			
			if (ratio < 0)
				ratio = 0.0;
			if (ratio > 1)
				ratio = 1.0;
			
			result = this.generateResult(ratio);
		}
		return result;

	}

}
