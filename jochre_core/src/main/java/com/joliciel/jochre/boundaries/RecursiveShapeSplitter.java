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
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.boundaries.features.SplitFeature;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.utils.PerformanceMonitor;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * Splits a shape using a DecisionMaker.
 * Makes it possible to split a shape multiple times, by applying the split algorithm recursively,
 * up to a pre-defined depth, to the sub-shapes resulting from each split.
 * <br/>In the recursive case, the scoring of the various sequences needs to satisfy the following intuition:
 * <br/>Shape A has a 50% probability of being split into A1 and A2.
 * <br/>Shape A1 has a 50% probability of being split into A11 and A12.
 * <br/>Shape A2 has a 50% probability of being split into A21 and A22.
 * <br/>This results in 5 possible sequences:
 * <ol>
 * <li>A</li>
 * <li>A1-A2</li>
 * <li>A11-A12-A2</li>
 * <li>A1-A21-A22</li>
 * <li>A11-A12-A21-A22</li>
 * </ol>
 * We want the five sequences to be returned as being equi-probable.
 * If we simply multiply the probabilities of all the splits applied, we get: 1=50%, 2=25%, 3=12.5%, 4=12.5%, 5=6.25%.
 * Instead, we do the following: at each level of recursion, we set the highest probability option to 1, and set all of the
 * other probabilities proportionally to the highest.
 * This will give equiprobable sequences in the above case, and gives the intuitive response in other cases.
 * @author Assaf Urieli
 *
 */
class RecursiveShapeSplitter implements ShapeSplitter {
	private static final Log LOG = LogFactory.getLog(RecursiveShapeSplitter.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(JochreSplitEventStream.class);

	private SplitCandidateFinder splitCandidateFinder;
	private BoundaryServiceInternal boundaryServiceInternal;
	private FeatureService featureService;
	private MachineLearningService machineLearningService;
	
	private Set<SplitFeature<?>> splitFeatures = null;
	private DecisionMaker decisionMaker;
	
	double minWidthRatio = 1.1;
	int beamWidth = 5;
	int maxDepth = 3;
	
	public RecursiveShapeSplitter(SplitCandidateFinder splitCandidateFinder,
			Set<SplitFeature<?>> splitFeatures, DecisionMaker decisionMaker) {
		super();
		this.splitCandidateFinder = splitCandidateFinder;
		this.splitFeatures = splitFeatures;
		this.decisionMaker = decisionMaker;
	}

	@Override
	public List<ShapeSequence> split(Shape shape) {
		LOG.trace("Splitting shape: " + shape);
		boolean leftToRight = shape.getJochreImage().isLeftToRight();
		List<ShapeSequence> shapeSequences = this.split(shape, 0, shape, leftToRight);
		int i = 0;
		for (ShapeSequence shapeSequence : shapeSequences) {
			LOG.debug("Sequence" + i + ", score=" + shapeSequence.getScore());
			for(ShapeInSequence splitGuess : shapeSequence) {
				LOG.debug("Shape, left(" + splitGuess.getShape().getLeft() + ")"
						+ ", top(" + splitGuess.getShape().getTop() + ")"
						+ ", right(" + splitGuess.getShape().getRight() + ")"
						+ ", bot(" + splitGuess.getShape().getBottom() + ")"
						+ " [id=" + splitGuess.getShape().getId() + "]");
			}
			i++;
		}
		return shapeSequences;
	}
	
	List<ShapeSequence> split(Shape shape, int depth, Shape originalShape, boolean leftToRight) {
		String padding = "-";
		for (int i = 0; i<depth; i++)
			padding += "-";
		padding += " ";
		if (LOG.isTraceEnabled()) {
			LOG.trace(padding + "Splitting shape: " + shape.getLeft() + " , " + shape.getRight());
			LOG.trace(padding + "depth: " + depth);
		}
		List<ShapeSequence> shapeSequences = new ArrayList<ShapeSequence>();
		// check if shape is wide enough to bother with
		double widthRatio = (double) shape.getWidth() / (double) shape.getXHeight();
		if (LOG.isTraceEnabled())
			LOG.trace(padding + "widthRatio: " + widthRatio);
		
		if (widthRatio<minWidthRatio || depth>=maxDepth) {
			if (LOG.isTraceEnabled())
				LOG.trace(padding + "too narrow or too deep");
			ShapeSequence shapeSequence = this.boundaryServiceInternal.getEmptyShapeSequence();
			shapeSequence.addShape(shape, originalShape);
			shapeSequences.add(shapeSequence);
		} else {
			List<Split> splitCandidates = this.splitCandidateFinder.findSplitCandidates(shape);
			TreeSet<ShapeSequence> myShapeSequences = new TreeSet<ShapeSequence>();
			
			TreeSet<WeightedOutcome<Split>> weightedSplits = new TreeSet<WeightedOutcome<Split>>();
			for (Split splitCandidate : splitCandidates) {
				double splitProb = this.shouldSplit(splitCandidate);
				WeightedOutcome<Split> weightedSplit = new WeightedOutcome<Split>(splitCandidate, splitProb);
				weightedSplits.add(weightedSplit);
			}
			
			double maxSplitProb = 0.0;
			if (weightedSplits.size()>0)
				maxSplitProb = weightedSplits.first().getWeight();
			
			double noSplitProb = 1 - maxSplitProb;
			if (noSplitProb>maxSplitProb)
				maxSplitProb = noSplitProb;
			
			Split noSplit = boundaryServiceInternal.getEmptySplit(shape);
			noSplit.setPosition(-1);
			WeightedOutcome<Split> weightedNoSplit = new WeightedOutcome<Split>(noSplit, noSplitProb);
			weightedSplits.add(weightedNoSplit);
			
			boolean topCandidate = true;
			double topCandidateWeight = 1.0;
			for (WeightedOutcome<Split> weightedSplit : weightedSplits) {
				Split splitCandidate = weightedSplit.getOutcome();
				double splitProb = weightedSplit.getWeight();
				if (LOG.isTraceEnabled())
					LOG.trace(padding + "splitCandidate: left=" + splitCandidate.getShape().getLeft() + ", pos=" + splitCandidate.getPosition() + ", initial prob: " + splitProb);

				if (LOG.isTraceEnabled()) {
					if (topCandidate) {
						LOG.trace(padding + "topCandidate");
					}
				}
				
				if (splitCandidate.getPosition()<0) {
					// This is the no-split candidate
					if (topCandidate)
						topCandidateWeight = 1.0;
					
					ShapeSequence shapeSequence = boundaryServiceInternal.getEmptyShapeSequence();
					shapeSequence.addShape(shape, originalShape);
					double prob = (splitProb / maxSplitProb) * topCandidateWeight;
					if (LOG.isTraceEnabled())
						LOG.trace(padding + "noSplit prob=(" + splitProb + " / " + maxSplitProb + ") * " + topCandidateWeight + " = " + prob);
					
					Decision decision = machineLearningService.createDecision(SplitOutcome.DO_NOT_SPLIT.name(), prob);
					shapeSequence.addDecision(decision);
					myShapeSequences.add(shapeSequence);
				} else {
					// a proper split
					Shape leftShape = shape.getJochreImage().getShape(shape.getLeft(), shape.getTop(), shape.getLeft() + splitCandidate.getPosition(), shape.getBottom());
					Shape rightShape = shape.getJochreImage().getShape(shape.getLeft() + splitCandidate.getPosition()+1, shape.getTop(), shape.getRight(), shape.getBottom());

					// for each split recursively try to split it again up to depth of m
					// Note: m=2 is probably enough, since we're not expecting more than 4 letters per shape (3 splits)
					List<ShapeSequence> leftShapeSequences = this.split(leftShape, depth+1, originalShape, leftToRight);
					List<ShapeSequence> rightShapeSequences = this.split(rightShape, depth+1, originalShape, leftToRight);
					
					if (topCandidate) {
						// find the no-split sequence in each sub-sequence
						ShapeSequence noSplitLeft = null;
						for (ShapeSequence leftShapeSequence : leftShapeSequences) {
							if (leftShapeSequence.size()==1) {
								noSplitLeft = leftShapeSequence;
								break;
							}
						}
						
						ShapeSequence noSplitRight = null;
						for (ShapeSequence rightShapeSequence : rightShapeSequences) {
							if (rightShapeSequence.size()==1) {
								noSplitRight = rightShapeSequence;
								break;
							}
						}
					
						// we should be guaranteed to find a noSplitLeft and noSplitRight
						// since a no-split candidate is always returned
						topCandidateWeight = noSplitLeft.getScore() * noSplitRight.getScore();
						if (LOG.isTraceEnabled())
							LOG.trace(padding + "topCandidateWeight=" + noSplitLeft.getScore() + " *" + noSplitRight.getScore() + " = " + topCandidateWeight);
					}
					
					for (ShapeSequence leftShapeSequence : leftShapeSequences) {
						for (ShapeSequence rightShapeSequence : rightShapeSequences) {
							ShapeSequence newSequence = null;
							if (leftToRight)
								newSequence = boundaryServiceInternal.getShapeSequence(leftShapeSequence, rightShapeSequence);
							else
								newSequence = boundaryServiceInternal.getShapeSequence(rightShapeSequence, leftShapeSequence);
							if (LOG.isTraceEnabled()) {
								StringBuilder sb = new StringBuilder();
								for (ShapeInSequence splitShape : newSequence) {
									sb.append("(" + splitShape.getShape().getLeft() + "," + splitShape.getShape().getRight() + ") ");
								}
								LOG.trace(padding + sb.toString());
							}
							double totalProb = 1.0;
							for (Decision decision : newSequence.getDecisions()) {
								totalProb = totalProb * decision.getProbability();
							}
							newSequence.getDecisions().clear();
							double prob = 0.0;
							if (topCandidate) {
								prob = totalProb * (splitProb / maxSplitProb);
								if (LOG.isTraceEnabled())
									LOG.trace(padding + "prob=" + totalProb + " * (" + splitProb + " / " + maxSplitProb + ") = " + prob);
							} else {
								prob = totalProb * (splitProb / maxSplitProb) * topCandidateWeight;
								if (LOG.isTraceEnabled())
									LOG.trace(padding + "prob=" + totalProb + " * (" + splitProb + " / " + maxSplitProb + ") * " + topCandidateWeight + " = " + prob);
							}
							
							Decision decision = machineLearningService.createDecision(SplitOutcome.DO_SPLIT.name(), prob);
							newSequence.addDecision(decision);
							myShapeSequences.add(newSequence);
						}
					}
				}
				
				topCandidate = false;
			}
			
			int i = 0;
			for (ShapeSequence shapeSequence : myShapeSequences) {
				// Note: we always return the no-split option, even it it's very low probability
				if (shapeSequence.size()==1||i<beamWidth) {
					shapeSequences.add(shapeSequence);
				}
				i++;
			}
		}
		
		return shapeSequences;
	}


	public double shouldSplit(Split splitCandidate) {
		MONITOR.startTask("shouldSplit");
		try {
			
			List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
			
			MONITOR.startTask("analyse features");
			try {
				for (SplitFeature<?> feature : splitFeatures) {
					MONITOR.startTask(feature.getName());
					try {
						RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
						FeatureResult<?> featureResult = feature.check(splitCandidate, env);
						if (featureResult!=null) {
							featureResults.add(featureResult);
							if (LOG.isTraceEnabled()) {
								LOG.trace(featureResult.toString());
							}
						}
					} finally {
						MONITOR.endTask();
					}
				}
			} finally {
				MONITOR.endTask();
			}
			
			List<Decision> decisions = null;
			MONITOR.startTask("decision maker");
			try {
				decisions = decisionMaker.decide(featureResults);
			} finally {
				MONITOR.endTask();
			}
			
			double yesProb = 0.0;
			for (Decision decision : decisions) {
				if (decision.getOutcome().equals(SplitOutcome.DO_SPLIT.name())) {
					yesProb = decision.getProbability();
					break;
				}
			}
			
			if (LOG.isTraceEnabled()) {
				LOG.trace("splitCandidate: left=" + splitCandidate.getShape().getLeft() + ", pos=" + splitCandidate.getPosition());
				LOG.trace("yesProb: " + yesProb);
			}
	
			return yesProb;
		} finally {
			MONITOR.endTask();
		}
	}
	
	public SplitCandidateFinder getSplitCandidateFinder() {
		return splitCandidateFinder;
	}

	public void setSplitCandidateFinder(SplitCandidateFinder splitCandidateFinder) {
		this.splitCandidateFinder = splitCandidateFinder;
	}


	public BoundaryServiceInternal getBoundaryServiceInternal() {
		return boundaryServiceInternal;
	}

	public void setBoundaryServiceInternal(
			BoundaryServiceInternal boundaryServiceInternal) {
		this.boundaryServiceInternal = boundaryServiceInternal;
	}

	/**
	 * The minimum ratio between the shape's width and it's x-height
	 * for the shape to even be considered for splitting.
	 */
	public double getMinWidthRatio() {
		return minWidthRatio;
	}

	public void setMinWidthRatio(double minWidthRatio) {
		this.minWidthRatio = minWidthRatio;
	}

	/**
	 * The beam width indicating the maximum possible decisions to return for each
	 * shape (applies recursively as well).
	 */
	public int getBeamWidth() {
		return beamWidth;
	}

	public void setBeamWidth(int beamWidth) {
		this.beamWidth = beamWidth;
	}

	/**
	 * The maximum recursive depth to search for splits in a single shape.
	 * The maximum number of splits = 2^maxDepth.
	 */
	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

}
