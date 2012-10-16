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
import java.util.PriorityQueue;

import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.Decision;

/**
 * Returns shapes each representing a single letter (after splitting/merging),
 * regardless of the original boundaries.
 * @author Assaf Urieli
 *
 */
class LetterByLetterBoundaryDetector implements BoundaryDetector {
	private BoundaryServiceInternal boundaryService;
	private ShapeSplitter shapeSplitter;
	private ShapeMerger shapeMerger;
	private int beamWidth = 10;
	private double minWidthRatioForSplit = 1.1;
	private double minHeightRatioForSplit = 1.0;
	private double maxWidthRatioForMerge = 1.2;
	private double maxDistanceRatioForMerge = 0.15;
	private BoundaryDecisionFactory boundaryDecisionFactory = new BoundaryDecisionFactory();
	
	@Override
	public List<ShapeSequence> findBoundaries(GroupOfShapes group) {
		// find the possible shape sequences that make up this group
		ShapeSequence emptySequence = boundaryService.getEmptyShapeSequence();
		PriorityQueue<ShapeSequence> heap = new PriorityQueue<ShapeSequence>();
		heap.add(emptySequence);
		for (Shape shape : group.getShapes()) {
			PriorityQueue<ShapeSequence> previousHeap = heap;
			heap = new PriorityQueue<ShapeSequence>();

			// check if shape is wide enough to bother with
			
			double widthRatio = (double) shape.getWidth() / (double) shape.getXHeight();
			double heightRatio = (double) shape.getHeight() / (double) shape.getXHeight();

			// Splitting/merging shapes as required
			List<ShapeSequence> splitSequences = null;
			if (this.shapeSplitter!=null && widthRatio>=minWidthRatioForSplit && heightRatio>=minHeightRatioForSplit) {
				splitSequences = shapeSplitter.split(shape);
			} else {
				// create a sequence containing only this shape
				ShapeSequence singleShapeSequence = boundaryService.getEmptyShapeSequence();
				singleShapeSequence.addShape(shape);
				
				splitSequences = new ArrayList<ShapeSequence>();
				splitSequences.add(singleShapeSequence);
			}
			
			// limit the breadth to K
			int maxSequences = previousHeap.size() > this.beamWidth ? this.beamWidth : previousHeap.size();
			
			for (int j = 0; j<maxSequences; j++) {
				ShapeSequence history = previousHeap.poll();
				for (ShapeSequence splitSequence : splitSequences) {
					ShapeInSequence previousShapeInSequence = null;
					Shape previousShape = null;
					if (history.size()>0) {
						previousShapeInSequence = history.get(history.size()-1);
						previousShape = previousShapeInSequence.getShape();
					}
					
					ShapeInSequence firstShapeInSequence = splitSequence.get(0);
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
					if (mergeProb > 0) {
						Shape mergedShape = shapeMerger.merge(previousShape, firstShape);
						ShapeSequence mergedSequence = boundaryService.getShapeSequencePlusOne(history);
						mergedSequence.remove(mergedSequence.size()-1);
						
						List<Shape> originalShapesForMerge = new ArrayList<Shape>();
						originalShapesForMerge.addAll(previousShapeInSequence.getOriginalShapes());
						originalShapesForMerge.addAll(firstShapeInSequence.getOriginalShapes());
						mergedSequence.addShape(mergedShape, originalShapesForMerge);
						boolean isFirstShape = true;
						for (ShapeInSequence splitShape : splitSequence) {
							if (!isFirstShape)
								mergedSequence.add(splitShape);
							isFirstShape = false;
						}
						heap.add(mergedSequence);
						
						Decision<SplitMergeOutcome> mergeDecision = this.boundaryDecisionFactory.createDecision(MergeOutcome.DO_MERGE.getCode(), mergeProb);
						mergedSequence.addDecision(mergeDecision);
						for (Decision<SplitMergeOutcome> splitDecision : splitSequence.getDecisions())
							mergedSequence.addDecision(splitDecision);
					}
					
					if (mergeProb<1) {
						ShapeSequence totalSequence = boundaryService.getShapeSequencePlusOne(history);
						if (mergeProb>0) {
							Decision<SplitMergeOutcome> mergeDecision = this.boundaryDecisionFactory.createDecision(MergeOutcome.DO_NOT_MERGE.getCode(), 1-mergeProb);
							totalSequence.addDecision(mergeDecision);
						}
						for (Decision<SplitMergeOutcome> splitDecision : splitSequence.getDecisions())
							totalSequence.addDecision(splitDecision);
						
						for (ShapeInSequence splitShape : splitSequence) {
							totalSequence.add(splitShape);
						}
						heap.add(totalSequence);
					}
				} // next split sequence for this shape
			} // next history from previous heap
		} // next shape in group
		
		List<ShapeSequence> result = new ArrayList<ShapeSequence>();
		for (int i=0;i<this.beamWidth;i++) {
			if (heap.isEmpty())
				break;
			ShapeSequence nextSequence = heap.poll();
			result.add(nextSequence);
		}
		
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

	public int getBeamWidth() {
		return beamWidth;
	}

	public void setBeamWidth(int beamWidth) {
		this.beamWidth = beamWidth;
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

}
