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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.boundaries.features.MergeFeature;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.utils.DecisionMaker;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.util.PerformanceMonitor;
import com.joliciel.talismane.utils.util.WeightedOutcome;

public class ShapeMergerImpl implements ShapeMerger {
	private static final Log LOG = LogFactory.getLog(ShapeMergerImpl.class);
	DecisionMaker decisionMaker;
	Set<MergeFeature<?>> mergeFeatures;
	BoundaryServiceInternal boundaryServiceInternal;
	
	public ShapeMergerImpl(DecisionMaker decisionMaker,
			Set<MergeFeature<?>> mergeFeatures) {
		super();
		this.decisionMaker = decisionMaker;
		this.mergeFeatures = mergeFeatures;
	}

	@Override
	public double checkMerge(Shape shape1, Shape shape2) {
		PerformanceMonitor.startTask("ShapeMergerImpl.checkMerge");
		try {
			ShapePair mergeCandidate = this.boundaryServiceInternal.getShapePair(shape1, shape2);		
			if (LOG.isTraceEnabled())
				LOG.trace("mergeCandidate: " + mergeCandidate);
			
			List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
			
			PerformanceMonitor.startTask("analyse features");
			try {
				for (MergeFeature<?> feature : mergeFeatures) {
					PerformanceMonitor.startTask(feature.getName());
					try {
						FeatureResult<?> featureResult = feature.check(mergeCandidate);
						if (featureResult!=null) {
							featureResults.add(featureResult);
							if (LOG.isTraceEnabled()) {
								LOG.trace(featureResult.toString());
							}
						}
					} finally {
						PerformanceMonitor.endTask(feature.getName());
					}
				}
			} finally {
				PerformanceMonitor.endTask("analyse features");
			}
			
			List<WeightedOutcome<String>> outcomes = null;
			PerformanceMonitor.startTask("decision maker");
			try {
				outcomes = decisionMaker.decide(featureResults);
			} finally {
				PerformanceMonitor.endTask("decision maker");
			}
			
			double yesProb = 0.0;
			for (WeightedOutcome<String> weightedOutcome : outcomes) {
				if (weightedOutcome.getOutcome().equals("YES")) {
					yesProb = weightedOutcome.getWeight();
					break;
				}
			}
			
			if (LOG.isTraceEnabled()) {
				LOG.trace("yesProb: " + yesProb);
			}
			return yesProb;
		} finally {
			PerformanceMonitor.endTask("ShapeMergerImpl.checkMerge");
		}
	}

	@Override
	public Shape merge(Shape shape1, Shape shape2) {
		ShapePair pair = this.boundaryServiceInternal.getShapePair(shape1, shape2);
		Shape mergedShape = shape1.getJochreImage().getShape(pair.getLeft(), pair.getTop(), pair.getRight(), pair.getBottom());
		return mergedShape;
	}

	public BoundaryServiceInternal getBoundaryServiceInternal() {
		return boundaryServiceInternal;
	}

	public void setBoundaryServiceInternal(
			BoundaryServiceInternal boundaryServiceInternal) {
		this.boundaryServiceInternal = boundaryServiceInternal;
	}

}
