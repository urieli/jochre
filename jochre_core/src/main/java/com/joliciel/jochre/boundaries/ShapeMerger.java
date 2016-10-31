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
package com.joliciel.jochre.boundaries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.boundaries.features.MergeFeature;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

/**
 * Decides whether or not to merge two shapes into a single shape.
 * 
 * @author Assaf Urieli
 *
 */
public class ShapeMerger {
	private static final Logger LOG = LoggerFactory.getLogger(ShapeMerger.class);

	private final DecisionMaker decisionMaker;
	private final Set<MergeFeature<?>> mergeFeatures;

	public ShapeMerger(Set<MergeFeature<?>> mergeFeatures, DecisionMaker decisionMaker) {
		this.decisionMaker = decisionMaker;
		this.mergeFeatures = mergeFeatures;
	}

	/**
	 * Given two sequential shape, returns the probability of a merge.
	 */
	public double checkMerge(Shape shape1, Shape shape2) {
		ShapePair mergeCandidate = new ShapePair(shape1, shape2);
		if (LOG.isTraceEnabled())
			LOG.trace("mergeCandidate: " + mergeCandidate);

		List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();

		// analyse features
		for (MergeFeature<?> feature : mergeFeatures) {
			RuntimeEnvironment env = new RuntimeEnvironment();
			FeatureResult<?> featureResult = feature.check(mergeCandidate, env);
			if (featureResult != null) {
				featureResults.add(featureResult);
				if (LOG.isTraceEnabled()) {
					LOG.trace(featureResult.toString());
				}
			}
		}

		List<Decision> decisions = decisionMaker.decide(featureResults);

		double yesProb = 0.0;
		for (Decision decision : decisions) {
			if (decision.getOutcome().equals(MergeOutcome.DO_MERGE)) {
				yesProb = decision.getProbability();
				break;
			}
		}

		if (LOG.isTraceEnabled()) {
			LOG.trace("yesProb: " + yesProb);
		}
		return yesProb;
	}

	/**
	 * Merge two sequential shapes into a single shape.
	 */
	public Shape merge(Shape shape1, Shape shape2) {
		ShapePair pair = new ShapePair(shape1, shape2);
		Shape mergedShape = shape1.getJochreImage().getShape(pair.getLeft(), pair.getTop(), pair.getRight(), pair.getBottom());
		return mergedShape;
	}

}
