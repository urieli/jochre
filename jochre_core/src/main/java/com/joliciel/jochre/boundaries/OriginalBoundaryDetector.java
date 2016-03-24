package com.joliciel.jochre.boundaries;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.Shape;

/**
 * Returns the original group's shapes exactly as is.
 * @author Assaf Urieli
 *
 */
class OriginalBoundaryDetector implements BoundaryDetector {	
	private double minWidthRatioForSplit = 1.1;
	private double minHeightRatioForSplit = 1.0;
	private double maxWidthRatioForMerge = 1.2;
	private double maxDistanceRatioForMerge = 0.15;
	private BoundaryService boundaryService;
	
	@Override
	public List<ShapeSequence> findBoundaries(GroupOfShapes group) {
		List<ShapeSequence> shapeSequences = new ArrayList<ShapeSequence>();
		ShapeSequence emptySequence = boundaryService.getEmptyShapeSequence();
		for (Shape shape : group.getShapes()) {
			emptySequence.addShape(shape);
		}
		shapeSequences.add(emptySequence);
		return shapeSequences;
	}

	public double getMinWidthRatioForSplit() {
		return minWidthRatioForSplit;
	}

	public void setMinWidthRatioForSplit(double minWidthRatioForSplit) {
		this.minWidthRatioForSplit = minWidthRatioForSplit;
	}

	public double getMinHeightRatioForSplit() {
		return minHeightRatioForSplit;
	}

	public void setMinHeightRatioForSplit(double minHeightRatioForSplit) {
		this.minHeightRatioForSplit = minHeightRatioForSplit;
	}

	public double getMaxWidthRatioForMerge() {
		return maxWidthRatioForMerge;
	}

	public void setMaxWidthRatioForMerge(double maxWidthRatioForMerge) {
		this.maxWidthRatioForMerge = maxWidthRatioForMerge;
	}

	public double getMaxDistanceRatioForMerge() {
		return maxDistanceRatioForMerge;
	}

	public void setMaxDistanceRatioForMerge(double maxDistanceRatioForMerge) {
		this.maxDistanceRatioForMerge = maxDistanceRatioForMerge;
	}

	public BoundaryService getBoundaryService() {
		return boundaryService;
	}

	public void setBoundaryService(BoundaryService boundaryService) {
		this.boundaryService = boundaryService;
	}

}
