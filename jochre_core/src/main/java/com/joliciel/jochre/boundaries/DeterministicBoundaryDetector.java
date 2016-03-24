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

import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.MachineLearningService;

/**
 * Returns a single "most likely" shape boundary guess,
 * regardless of the original boundaries.
 * @author Assaf Urieli
 *
 */
class DeterministicBoundaryDetector implements BoundaryDetector {
	private BoundaryServiceInternal boundaryService;
	private MachineLearningService machineLearningService;
	
	private ShapeSplitter shapeSplitter;
	private ShapeMerger shapeMerger;
	private double minWidthRatioForSplit = 1.1;
	private double minHeightRatioForSplit = 1.0;
	private double maxWidthRatioForMerge = 1.2;
	private double maxDistanceRatioForMerge = 0.15;
	private double minProbabilityForDecision = 0.5;
	
	@Override
	public List<ShapeSequence> findBoundaries(GroupOfShapes group) {
		// find the possible shape sequences that make up this group
		ShapeSequence bestSequence = boundaryService.getEmptyShapeSequence();
		for (Shape shape : group.getShapes()) {
			// check if shape is wide enough to bother with		
			double widthRatio = (double) shape.getWidth() / (double) shape.getXHeight();
			double heightRatio = (double) shape.getHeight() / (double) shape.getXHeight();

			// Splitting/merging shapes as required
			ShapeSequence bestSplitSequence = null;
			if (this.shapeSplitter!=null && widthRatio>=minWidthRatioForSplit && heightRatio>=minHeightRatioForSplit) {
				List<ShapeSequence> splitSequences = shapeSplitter.split(shape);
				double bestProb = 0;
				for (ShapeSequence splitSequence : splitSequences) {
					if (splitSequence.getScore() > bestProb) {
						bestSplitSequence = splitSequence;
						bestProb = splitSequence.getScore();
					}
				}
				if (bestProb < minProbabilityForDecision) {
					// create a sequence containing only this shape
					ShapeSequence singleShapeSequence = boundaryService.getEmptyShapeSequence();
					singleShapeSequence.addShape(shape);
					bestSplitSequence = singleShapeSequence;
				}
			} else {
				// create a sequence containing only this shape
				ShapeSequence singleShapeSequence = boundaryService.getEmptyShapeSequence();
				singleShapeSequence.addShape(shape);
				bestSplitSequence = singleShapeSequence;
			}
			
			ShapeInSequence previousShapeInSequence = null;
			Shape previousShape = null;
			if (bestSequence.size()>0) {
				previousShapeInSequence = bestSequence.get(bestSequence.size()-1);
				previousShape = previousShapeInSequence.getShape();
			}
			
			ShapeInSequence firstShapeInSequence = bestSplitSequence.get(0);
			Shape firstShape = firstShapeInSequence.getShape();
			
			double mergeProb = 0;
			if (this.shapeMerger!=null && previousShape!=null) {
				ShapePair mergeCandidate = boundaryService.getShapePair(previousShape, shape);
				double mergeCandidateWidthRatio = 0;
				double mergeCandidateDistanceRatio = 0;
				
				mergeCandidateWidthRatio = (double) mergeCandidate.getWidth() / (double) mergeCandidate.getXHeight();
				mergeCandidateDistanceRatio = (double) mergeCandidate.getInnerDistance() / (double) mergeCandidate.getXHeight();
				
				if (mergeCandidateWidthRatio <= maxWidthRatioForMerge && mergeCandidateDistanceRatio <= maxDistanceRatioForMerge) {
					mergeProb = shapeMerger.checkMerge(previousShape, firstShape);
				}
			}
			if (mergeProb > minProbabilityForDecision) {
				Shape mergedShape = shapeMerger.merge(previousShape, firstShape);
				bestSequence.remove(bestSequence.size()-1);
				
				List<Shape> originalShapesForMerge = new ArrayList<Shape>();
				originalShapesForMerge.addAll(previousShapeInSequence.getOriginalShapes());
				originalShapesForMerge.addAll(firstShapeInSequence.getOriginalShapes());
				bestSequence.addShape(mergedShape, originalShapesForMerge);
				boolean isFirstShape = true;
				for (ShapeInSequence splitShape : bestSplitSequence) {
					if (!isFirstShape)
						bestSequence.add(splitShape);
					isFirstShape = false;
				}
				
				Decision mergeDecision = machineLearningService.createDecision(MergeOutcome.DO_MERGE.name(), mergeProb);
				bestSequence.addDecision(mergeDecision);
				for (Decision splitDecision : bestSplitSequence.getDecisions())
					bestSequence.addDecision(splitDecision);
			} else {
				if (mergeProb>0) {
					Decision mergeDecision = machineLearningService.createDecision(MergeOutcome.DO_NOT_MERGE.name(), 1-mergeProb);
					bestSequence.addDecision(mergeDecision);
				}
				for (Decision splitDecision : bestSplitSequence.getDecisions())
					bestSequence.addDecision(splitDecision);
				
				for (ShapeInSequence splitShape : bestSplitSequence) {
					bestSequence.add(splitShape);
				}
			}
		} // next shape in group
		
		List<ShapeSequence> result = new ArrayList<ShapeSequence>();
		result.add(bestSequence);
		
		return result;
	}

	public BoundaryServiceInternal getBoundaryService() {
		return boundaryService;
	}

	public void setBoundaryService(BoundaryServiceInternal boundaryService) {
		this.boundaryService = boundaryService;
	}

	public ShapeSplitter getShapeSplitter() {
		return shapeSplitter;
	}

	public void setShapeSplitter(ShapeSplitter shapeSplitter) {
		this.shapeSplitter = shapeSplitter;
	}

	public ShapeMerger getShapeMerger() {
		return shapeMerger;
	}

	public void setShapeMerger(ShapeMerger shapeMerger) {
		this.shapeMerger = shapeMerger;
	}

	@Override
	public double getMinWidthRatioForSplit() {
		return minWidthRatioForSplit;
	}

	@Override
	public void setMinWidthRatioForSplit(double minWidthRatioForSplit) {
		this.minWidthRatioForSplit = minWidthRatioForSplit;
	}

	@Override
	public double getMaxWidthRatioForMerge() {
		return maxWidthRatioForMerge;
	}

	@Override
	public void setMaxWidthRatioForMerge(double maxWidthRatioForMerge) {
		this.maxWidthRatioForMerge = maxWidthRatioForMerge;
	}

	@Override
	public double getMaxDistanceRatioForMerge() {
		return maxDistanceRatioForMerge;
	}

	@Override
	public void setMaxDistanceRatioForMerge(double maxDistanceRatioForMerge) {
		this.maxDistanceRatioForMerge = maxDistanceRatioForMerge;
	}

	@Override
	public double getMinHeightRatioForSplit() {
		return minHeightRatioForSplit;
	}

	@Override
	public void setMinHeightRatioForSplit(double minHeightRatioForSplit) {
		this.minHeightRatioForSplit = minHeightRatioForSplit;
	}

	public double getMinProbabilityForDecision() {
		return minProbabilityForDecision;
	}

	public void setMinProbabilityForDecision(double minProbabilityForDecision) {
		this.minProbabilityForDecision = minProbabilityForDecision;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

}
