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
 * Inverts a regular merge feature, giving 1-result.
 * @author Assaf Urieli
 *
 */
public class InverseMergeFeature extends AbstractMergeFeature<Double> implements
		DoubleFeature<ShapePair> {

	MergeFeature<Double> feature;
	
	public InverseMergeFeature(MergeFeature<Double> feature) {
		super();
		this.feature = feature;
		this.setName(this.feature.getName() + "{inverse}");
	}

	
	@Override
	public FeatureResult<Double> checkInternal(ShapePair pair) {
		FeatureResult<Double> rawOutcome = feature.check(pair);
		FeatureResult<Double> outcome = null;
		if (rawOutcome!=null) {
			double weight = rawOutcome.getOutcome();
			double inverseWeight = 1 - weight;
			if (inverseWeight<0)
				inverseWeight = 0;
			outcome = this.generateResult(inverseWeight);
		}
		return outcome;
	}
}
