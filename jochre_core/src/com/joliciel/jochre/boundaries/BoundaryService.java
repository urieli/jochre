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

import java.util.List;
import java.util.Set;

import com.joliciel.jochre.boundaries.features.MergeFeature;
import com.joliciel.jochre.boundaries.features.SplitFeature;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.DecisionMaker;

public interface BoundaryService {	
	public CorpusEventStream getJochreSplitEventStream(ImageStatus[] imageStatusesToInclude, Set<SplitFeature<?>> splitFeatures, int imageCount, double minWidthRatio, double minHeightRatio);
	public CorpusEventStream getJochreMergeEventStream(ImageStatus[] imageStatusesToInclude, Set<MergeFeature<?>> mergeFeatures, int imageCount, double maxWidthRatio, double maxDistanceRatio);

	/**
	 * Returns the single "most likely" shape sequence, as long as each decision has a score >= minProbabilityForDecision.
	 * Otherwise, returns the original boundaries.
	 * @param shapeSplitter
	 * @param shapeMerger
	 * @param minProbabilityForDecision minimum probability for applying a split or merge
	 * @return
	 */
	public BoundaryDetector getDeterministicBoundaryDetector(ShapeSplitter shapeSplitter, ShapeMerger shapeMerger, double minProbabilityForDecision);
	
	/**
	 * Returns shapes each representing a single letter (after splitting/merging),
	 * regardless of the original boundaries.
	 */
	public BoundaryDetector getLetterByLetterBoundaryDetector(ShapeSplitter shapeSplitter, ShapeMerger shapeMerger, int beamWidth);
	
	/**
	 * Returns the original group's shapes exactly as is.
	 *
	 */
	public BoundaryDetector getOriginalBoundaryDetector();
	
	/**
	 * Get an empty shape sequence.
	 * @return
	 */
	ShapeSequence getEmptyShapeSequence();
	
	public List<Split> findSplits(Shape shape);

	Split getEmptySplit(Shape shape);
	
	
	/**
	 * Combine two shape sequences into a single sequence.
	 * @param sequence1
	 * @param sequence2
	 * @return
	 */
	public ShapeSequence getShapeSequence(ShapeSequence sequence1, ShapeSequence sequence2);
	
	public ShapeSplitter getTrainingCorpusShapeSplitter();
	
	public ShapeMerger getTrainingCorpusShapeMerger();
	
	public SplitCandidateFinder getSplitCandidateFinder();
	
	/**
	 * A maxent shape splitter.
	 * @param splitCandidateFinder find split candidates
	 * @param features shape features to use for split probability analysis
	 * @param splitFeatures split features to use for split probability analysis
	 * @param decisionMaker the decision maker for the splits
	 * @param minWidthRatio min ratio of shape width to shape x-height to even consider a split
	 * @param beamWidth max number of split possibilities to return
	 * @param maxDepth max depth to go looking for splits - the number of possible splits is 2^maxDepth
	 * @return
	 */
	public ShapeSplitter getShapeSplitter(SplitCandidateFinder splitCandidateFinder,
			Set<SplitFeature<?>> splitFeatures, DecisionMaker<SplitOutcome> decisionMaker, 
			double minWidthRatio, 
			int beamWidth,
			int maxDepth);
	
	/**
	 * A splitter evaluator.
	 * @param tolerance the max distance between a split guess and a real split to consider we got it right.
	 * @param minWidthRatio min ratio of shape width to shape x-height to even consider a split
	 * @param minHeightRatio 
	 * @return
	 */
	public SplitEvaluator getSplitEvaluator(int tolerance, double minWidthRatio, double minHeightRatio);
	
	ShapePair getShapePair(Shape firstShape, Shape secondShape);
	
	public MergeEvaluator getMergeEvaluator(double maxWidthRatio, double maxDistanceRatio);
	
	public ShapeMerger getShapeMerger(Set<MergeFeature<?>> mergeFeatures, DecisionMaker<MergeOutcome> decisionMaker);

	public DecisionFactory<SplitOutcome> getSplitDecisionFactory();
	
	public DecisionFactory<MergeOutcome> getMergeDecisionFactory();
}
