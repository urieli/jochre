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
package com.joliciel.jochre.letterGuesser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.letterGuesser.features.LetterFeature;
import com.joliciel.talismane.utils.DecisionMaker;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.util.PerformanceMonitor;
import com.joliciel.talismane.utils.util.WeightedOutcome;

final class LetterGuesserImpl implements LetterGuesser {
	private static final Log LOG = LogFactory.getLog(LetterGuesserImpl.class);
	
	private static final double MIN_PROB_TO_STORE = 0.001;
	
	DecisionMaker decisionMaker = null;
	Set<LetterFeature<?>> features = null;
	
	LetterGuesserServiceInternal letterGuesserServiceInternal;
	
	public LetterGuesserImpl(Set<LetterFeature<?>> features, DecisionMaker decisionMaker) {
		this.decisionMaker = decisionMaker;
		this.features = features;
	}
	
	public String guessLetter(ShapeInSequence shapeInSequence) {
		return this.guessLetter(shapeInSequence, null);
	}


	@Override
	public String guessLetter(ShapeInSequence shapeInSequence, LetterSequence history) {
		PerformanceMonitor.startTask("LetterGuesserImpl.guessLetter");
		try {
			Shape shape = shapeInSequence.getShape();
			if (LOG.isTraceEnabled())
				LOG.trace("guessLetter, shape: " + shape);
			
			
			List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
			
			PerformanceMonitor.startTask("analyse features");
			try {
				for (LetterFeature<?> feature : features) {
					PerformanceMonitor.startTask(feature.getName());
					try {
						LetterGuesserContext context = this.letterGuesserServiceInternal.getContext(shapeInSequence, history);
						FeatureResult<?> featureResult = feature.check(context);
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
			
			List<WeightedOutcome<String>> weightedOutcomes = null;
			PerformanceMonitor.startTask("decision maker");
			try {
				weightedOutcomes = decisionMaker.decide(featureResults);
			} finally {
				PerformanceMonitor.endTask("decision maker");
			}
			
			String bestOutcome = null;
			PerformanceMonitor.startTask("store outcomes");
			try {
				shape.getWeightedOutcomes().clear();
		
				for (WeightedOutcome<String> weightedOutcome : weightedOutcomes) {
					if (weightedOutcome.getWeight()>=MIN_PROB_TO_STORE) {
						shape.getWeightedOutcomes().add(weightedOutcome);
					}
				}
				
				bestOutcome = shape.getWeightedOutcomes().iterator().next().getOutcome();
			} finally {
				PerformanceMonitor.endTask("store outcomes");
			}
			
			if (LOG.isTraceEnabled()) {
				LOG.trace("Shape: " + shape);
				LOG.trace("Letter: " + shape.getLetter());
				LOG.trace("Best outcome: " + bestOutcome);
			}

			return bestOutcome;
		} finally {
			PerformanceMonitor.endTask("LetterGuesserImpl.guessLetter");
		}
	}

	public LetterGuesserServiceInternal getLetterGuesserServiceInternal() {
		return letterGuesserServiceInternal;
	}

	public void setLetterGuesserServiceInternal(
			LetterGuesserServiceInternal letterGuesserServiceInternal) {
		this.letterGuesserServiceInternal = letterGuesserServiceInternal;
	}
	
}
