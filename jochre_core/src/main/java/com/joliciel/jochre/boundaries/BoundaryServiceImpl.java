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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.features.BoundaryFeatureService;
import com.joliciel.jochre.boundaries.features.MergeFeature;
import com.joliciel.jochre.boundaries.features.SplitFeature;
import com.joliciel.jochre.graphics.CorpusSelectionCriteria;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.MachineLearningModelFactory;

class BoundaryServiceImpl implements BoundaryServiceInternal {
	private static final Logger LOG = LoggerFactory.getLogger(BoundaryServiceImpl.class);
	private BoundaryFeatureService boundaryFeatureService;
	private BoundaryDao boundaryDao;

	private final JochreSession jochreSession;

	public BoundaryServiceImpl(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
	}

	@Override
	public BoundaryDetector getDeterministicBoundaryDetector(ShapeSplitter shapeSplitter, ShapeMerger shapeMerger, double minProbabilityForDecision) {
		DeterministicBoundaryDetector boundaryDetector = new DeterministicBoundaryDetector();
		boundaryDetector.setShapeSplitter(shapeSplitter);
		boundaryDetector.setShapeMerger(shapeMerger);
		boundaryDetector.setMinProbabilityForDecision(minProbabilityForDecision);
		boundaryDetector.setBoundaryService(this);
		return boundaryDetector;
	}

	@Override
	public BoundaryDetector getLetterByLetterBoundaryDetector(ShapeSplitter shapeSplitter, ShapeMerger shapeMerger, int beamWidth) {
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
	public ShapeSequence getShapeSequence(ShapeSequence sequence1, ShapeSequence sequence2) {
		ShapeSequenceImpl shapeSequence = new ShapeSequenceImpl(sequence1, sequence2);
		shapeSequence.setBoundaryServiceInternal(this);
		return shapeSequence;
	}

	@Override
	public ShapeSplitter getTrainingCorpusShapeSplitter(JochreSession jochreSession) {
		TrainingCorpusShapeSplitter shapeSplitter = new TrainingCorpusShapeSplitter(jochreSession);
		shapeSplitter.setBoundaryServiceInternal(this);
		return shapeSplitter;
	}

	@Override
	public ShapeMerger getTrainingCorpusShapeMerger() {
		TrainingCorpusShapeMerger shapeMerger = new TrainingCorpusShapeMerger();
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
		SplitImpl split = new SplitImpl(jochreSession);
		split.setBoundaryServiceInternal(this);
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

	public BoundaryDao getBoundaryDao() {
		return boundaryDao;
	}

	public void setBoundaryDao(BoundaryDao boundaryDao) {
		this.boundaryDao = boundaryDao;
		boundaryDao.setBoundaryServiceInternal(this);
	}

	@Override
	public ClassificationEventStream getJochreSplitEventStream(CorpusSelectionCriteria criteria, Set<SplitFeature<?>> splitFeatures, double minWidthRatio,
			double minHeightRatio) {
		JochreSplitEventStream eventStream = new JochreSplitEventStream(splitFeatures, jochreSession);
		eventStream.setBoundaryService(this);
		eventStream.setCriteria(criteria);
		eventStream.setMinWidthRatio(minWidthRatio);
		eventStream.setMinHeightRatio(minHeightRatio);

		SplitCandidateFinder splitCandidateFinder = this.getSplitCandidateFinder();
		eventStream.setSplitCandidateFinder(splitCandidateFinder);
		return eventStream;
	}

	@Override
	public ClassificationEventStream getJochreMergeEventStream(CorpusSelectionCriteria criteria, Set<MergeFeature<?>> mergeFeatures, double maxWidthRatio,
			double maxDistanceRatio) {
		JochreMergeEventStream eventStream = new JochreMergeEventStream(mergeFeatures, jochreSession);
		eventStream.setBoundaryService(this);
		eventStream.setCriteria(criteria);
		eventStream.setMaxWidthRatio(maxWidthRatio);
		eventStream.setMaxDistanceRatio(maxDistanceRatio);

		return eventStream;
	}

	@Override
	public ShapeSplitter getShapeSplitter(SplitCandidateFinder splitCandidateFinder, Set<SplitFeature<?>> splitFeatures, DecisionMaker decisionMaker,
			double minWidthRatio, int beamWidth, int maxDepth) {
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
	public ShapeInSequence getShapeInSequence(ShapeSequence sequence, Shape shape, int index) {
		ShapeInSequenceImpl shapeInSequence = new ShapeInSequenceImpl(shape, index, sequence);
		return shapeInSequence;
	}

	@Override
	public ShapePair getShapePair(Shape firstShape, Shape secondShape) {
		ShapePairImpl pair = new ShapePairImpl(firstShape, secondShape);
		return pair;
	}

	@Override
	public MergeEvaluator getMergeEvaluator(double maxWidthRatio, double maxDistanceRatio) {
		MergeEvaluatorImpl evaluator = new MergeEvaluatorImpl();
		evaluator.setMaxWidthRatio(maxWidthRatio);
		evaluator.setMaxDistanceRatio(maxDistanceRatio);
		evaluator.setBoundaryServiceInternal(this);
		return evaluator;
	}

	@Override
	public ShapeMerger getShapeMerger(Set<MergeFeature<?>> mergeFeatures, DecisionMaker decisionMaker) {
		ShapeMergerImpl merger = new ShapeMergerImpl(decisionMaker, mergeFeatures);
		merger.setBoundaryServiceInternal(this);
		return merger;
	}

	@Override
	public BoundaryDetector getBoundaryDetector(File splitModelFile, File mergeModelFile) {
		try {
			double minWidthRatioForSplit = 1.1;
			double minHeightRatioForSplit = 1.0;
			int splitBeamWidth = 5;
			int maxSplitDepth = 2;

			SplitCandidateFinder splitCandidateFinder = this.getSplitCandidateFinder();
			splitCandidateFinder.setMinDistanceBetweenSplits(5);

			MachineLearningModelFactory modelFactory = new MachineLearningModelFactory();

			ClassificationModel splitModel = null;
			try (ZipInputStream splitZis = new ZipInputStream(new FileInputStream(splitModelFile))) {
				splitModel = modelFactory.getClassificationModel(splitZis);
			}
			List<String> splitFeatureDescriptors = splitModel.getFeatureDescriptors();
			Set<SplitFeature<?>> splitFeatures = boundaryFeatureService.getSplitFeatureSet(splitFeatureDescriptors);
			ShapeSplitter shapeSplitter = this.getShapeSplitter(splitCandidateFinder, splitFeatures, splitModel.getDecisionMaker(), minWidthRatioForSplit,
					splitBeamWidth, maxSplitDepth);

			ClassificationModel mergeModel = null;
			try (ZipInputStream mergeZis = new ZipInputStream(new FileInputStream(mergeModelFile))) {
				mergeModel = modelFactory.getClassificationModel(mergeZis);
			}

			List<String> mergeFeatureDescriptors = mergeModel.getFeatureDescriptors();
			Set<MergeFeature<?>> mergeFeatures = boundaryFeatureService.getMergeFeatureSet(mergeFeatureDescriptors);
			double maxWidthRatioForMerge = 1.2;
			double maxDistanceRatioForMerge = 0.15;
			double minProbForDecision = 0.5;

			ShapeMerger shapeMerger = this.getShapeMerger(mergeFeatures, mergeModel.getDecisionMaker());

			BoundaryDetector boundaryDetector = this.getDeterministicBoundaryDetector(shapeSplitter, shapeMerger, minProbForDecision);
			boundaryDetector.setMinWidthRatioForSplit(minWidthRatioForSplit);
			boundaryDetector.setMinHeightRatioForSplit(minHeightRatioForSplit);
			boundaryDetector.setMaxWidthRatioForMerge(maxWidthRatioForMerge);
			boundaryDetector.setMaxDistanceRatioForMerge(maxDistanceRatioForMerge);

			return boundaryDetector;
		} catch (IOException e) {
			LOG.error("Failed to get boundary detector", e);
			throw new RuntimeException(e);
		}
	}

	public BoundaryFeatureService getBoundaryFeatureService() {
		return boundaryFeatureService;
	}

	public void setBoundaryFeatureService(BoundaryFeatureService boundaryFeatureService) {
		this.boundaryFeatureService = boundaryFeatureService;
	}

}
