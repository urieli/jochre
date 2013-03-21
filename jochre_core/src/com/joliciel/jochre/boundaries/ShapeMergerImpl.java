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
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class ShapeMergerImpl implements ShapeMerger {
	private static final Log LOG = LogFactory.getLog(ShapeMergerImpl.class);
	DecisionMaker<MergeOutcome> decisionMaker;
	Set<MergeFeature<?>> mergeFeatures;
	BoundaryServiceInternal boundaryServiceInternal;
	FeatureService featureService;
	
	public ShapeMergerImpl(DecisionMaker<MergeOutcome> decisionMaker,
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
						RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
						FeatureResult<?> featureResult = feature.check(mergeCandidate, env);
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
			
			List<Decision<MergeOutcome>> decisions = null;
			PerformanceMonitor.startTask("decision maker");
			try {
				decisions = decisionMaker.decide(featureResults);
			} finally {
				PerformanceMonitor.endTask("decision maker");
			}
			
			double yesProb = 0.0;
			for (Decision<MergeOutcome> decision : decisions) {
				if (decision.getOutcome().equals(MergeOutcome.DO_MERGE)) {
					yesProb = decision.getProbability();
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

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

}
