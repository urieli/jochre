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
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.MachineLearningService;

class BoundaryServiceImpl implements BoundaryServiceInternal {
	private MachineLearningService machineLearningService;
	GraphicsService graphicsService;
	BoundaryDao boundaryDao;
	
	public BoundaryServiceImpl() {
	}

	@Override
	public BoundaryDetector getDeterministicBoundaryDetector(
			ShapeSplitter shapeSplitter, ShapeMerger shapeMerger,
			double minProbabilityForDecision) {
		DeterministicBoundaryDetector boundaryDetector = new DeterministicBoundaryDetector();
		boundaryDetector.setShapeSplitter(shapeSplitter);
		boundaryDetector.setShapeMerger(shapeMerger);
		boundaryDetector.setMinProbabilityForDecision(minProbabilityForDecision);
		boundaryDetector.setBoundaryService(this);
		return boundaryDetector;		
	}

	@Override
	public BoundaryDetector getLetterByLetterBoundaryDetector(ShapeSplitter shapeSplitter,
			ShapeMerger shapeMerger, int beamWidth) {
		LetterByLetterBoundaryDetector boundaryDetector = new LetterByLetterBoundaryDetector();
		boundaryDetector.setShapeSplitter(shapeSplitter);
		boundaryDetector.setShapeMerger(shapeMerger);
		boundaryDetector.setBeamWidth(beamWidth);
		boundaryDetector.setBoundaryService(this);
		return boundaryDetector;
	}

	@Override
	public BoundaryDetector getOriginalBoundaryDetector() {
		OriginalBoundaryDetector boundaryDetector = new OriginalBoundaryDetector();
		boundaryDetector.setBoundaryService(this);
		return boundaryDetector;
	}


	@Override
	public ShapeSequence getEmptyShapeSequence() {
		ShapeSequenceImpl shapeSequence = new ShapeSequenceImpl();
		shapeSequence.setBoundaryServiceInternal(this);
		return shapeSequence;
	}

	@Override
	public ShapeSequence getShapeSequencePlusOne(ShapeSequence history) {
		ShapeSequenceImpl shapeSequence = new ShapeSequenceImpl(history);
		shapeSequence.setBoundaryServiceInternal(this);
		return shapeSequence;
	}


	@Override
	public ShapeSequence getShapeSequence(ShapeSequence sequence1,
			ShapeSequence sequence2) {
		ShapeSequenceImpl shapeSequence = new ShapeSequenceImpl(sequence1, sequence2);
		shapeSequence.setBoundaryServiceInternal(this);
		return shapeSequence;
	}


	@Override
	public ShapeSplitter getTrainingCorpusShapeSplitter() {
		TrainingCorpusShapeSplitter shapeSplitter = new TrainingCorpusShapeSplitter();
		shapeSplitter.setBoundaryServiceInternal(this);
		shapeSplitter.setGraphicsService(this.getGraphicsService());
		return shapeSplitter;
	}


	@Override
	public ShapeMerger getTrainingCorpusShapeMerger() {
		TrainingCorpusShapeMerger shapeMerger = new TrainingCorpusShapeMerger();
		shapeMerger.setGraphicsService(this.getGraphicsService());
		return shapeMerger;
	}


	@Override
	public SplitCandidateFinder getSplitCandidateFinder() {
		SplitCandidateFinderImpl splitCandidateFinder = new SplitCandidateFinderImpl();
		splitCandidateFinder.setBoundaryServiceInternal(this);
		return splitCandidateFinder;
	}


	@Override
	public Split getEmptySplit(Shape shape) {
		SplitInternal split = this.getEmptySplitInternal();
		split.setShape(shape);
		return split;
	}


	@Override
	public SplitInternal getEmptySplitInternal() {
		SplitImpl split = new SplitImpl();
		split.setBoundaryServiceInternal(this);
		split.setGraphicsService(this.getGraphicsService());
		return split;
	}

	@Override
	public void saveSplit(SplitInternal split) {
		this.getBoundaryDao().saveSplit(split);
	}

	@Override
	public List<Split> findSplits(Shape shape) {
		return this.getBoundaryDao().findSplits(shape);
	}

	@Override
	public void deleteSplit(Split split) {
		this.getBoundaryDao().deleteSplit(split);
	}
	
	
	public GraphicsService getGraphicsService() {
		return graphicsService;
	}


	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}
	

	public BoundaryDao getBoundaryDao() {
		return boundaryDao;
	}

	public void setBoundaryDao(BoundaryDao boundaryDao) {
		this.boundaryDao = boundaryDao;
		boundaryDao.setBoundaryServiceInternal(this);
	}


	@Override
	public CorpusEventStream getJochreSplitEventStream(
			ImageStatus[] imageStatusesToInclude,
			Set<SplitFeature<?>> splitFeatures, int imageCount, double minWidthRatio, double minHeightRatio) {
		JochreSplitEventStream eventStream = new JochreSplitEventStream(splitFeatures);
		eventStream.setBoundaryService(this);
		eventStream.setGraphicsService(this.getGraphicsService());
		eventStream.setMachineLearningService(this.getMachineLearningService());
		eventStream.setImageStatusesToInclude(imageStatusesToInclude);
		eventStream.setImageCount(imageCount);
		eventStream.setMinWidthRatio(minWidthRatio);
		eventStream.setMinHeightRatio(minHeightRatio);
		
		SplitCandidateFinder splitCandidateFinder = this.getSplitCandidateFinder();
		eventStream.setSplitCandidateFinder(splitCandidateFinder);
		return eventStream;
	}

	@Override
	public CorpusEventStream getJochreMergeEventStream(
			ImageStatus[] imageStatusesToInclude, Set<MergeFeature<?>> mergeFeatures, int imageCount,
			double maxWidthRatio, double maxDistanceRatio) {
		JochreMergeEventStream eventStream = new JochreMergeEventStream(mergeFeatures);
		eventStream.setBoundaryService(this);
		eventStream.setGraphicsService(this.getGraphicsService());
		eventStream.setMachineLearningService(this.getMachineLearningService());
		eventStream.setImageStatusesToInclude(imageStatusesToInclude);
		eventStream.setImageCount(imageCount);
		eventStream.setMaxWidthRatio(maxWidthRatio);
		eventStream.setMaxDistanceRatio(maxDistanceRatio);

		return eventStream;
	}

	@Override
	public ShapeSplitter getShapeSplitter(
			SplitCandidateFinder splitCandidateFinder,
			Set<SplitFeature<?>> splitFeatures,
			DecisionMaker<SplitOutcome> decisionMaker, double minWidthRatio, int beamWidth, int maxDepth) {
		RecursiveShapeSplitter shapeSplitter = new RecursiveShapeSplitter(splitCandidateFinder, splitFeatures, decisionMaker);
		shapeSplitter.setMinWidthRatio(minWidthRatio);
		shapeSplitter.setBeamWidth(beamWidth);
		shapeSplitter.setMaxDepth(maxDepth);
		shapeSplitter.setBoundaryServiceInternal(this);
		
		return shapeSplitter;
	}


	@Override
	public SplitEvaluator getSplitEvaluator(int tolerance, double minWidthRatio, double minHeightRatio) {
		SplitEvaluatorImpl evaluator = new SplitEvaluatorImpl();
		evaluator.setTolerance(tolerance);
		evaluator.setMinWidthRatio(minWidthRatio);
		evaluator.setMinHeightRatio(minHeightRatio);
		evaluator.setBoundaryServiceInternal(this);
		return evaluator;
	}


	@Override
	public ShapeInSequence getShapeInSequence(ShapeSequence sequence,
			Shape shape, int index) {
		ShapeInSequenceImpl shapeInSequence = new ShapeInSequenceImpl(shape, index, sequence);
		return shapeInSequence;
	}


	@Override
	public ShapePair getShapePair(Shape firstShape, Shape secondShape) {
		ShapePairImpl pair = new ShapePairImpl(firstShape, secondShape);
		return pair;
	}


	@Override
	public MergeEvaluator getMergeEvaluator(double maxWidthRatio,
			double maxDistanceRatio) {
		MergeEvaluatorImpl evaluator = new MergeEvaluatorImpl();
		evaluator.setMaxWidthRatio(maxWidthRatio);
		evaluator.setMaxDistanceRatio(maxDistanceRatio);
		evaluator.setBoundaryServiceInternal(this);
		return evaluator;
	}


	@Override
	public ShapeMerger getShapeMerger(Set<MergeFeature<?>> mergeFeatures,
			DecisionMaker<MergeOutcome> decisionMaker) {
		ShapeMergerImpl merger = new ShapeMergerImpl(decisionMaker, mergeFeatures);
		merger.setBoundaryServiceInternal(this);
		return merger;
	}

	@Override
	public DecisionFactory<SplitOutcome> getSplitDecisionFactory() {
		return new SplitDecisionFactory();
	}

	@Override
	public DecisionFactory<MergeOutcome> getMergeDecisionFactory() {
		return new MergeDecisionFactory();
	}


	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}


	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

}
