package com.joliciel.jochre.boundaries;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.Shape;

/**
 * Returns the original group's shapes exactly as is.
 * 
 * @author Assaf Urieli
 *
 */
public class OriginalBoundaryDetector implements BoundaryDetector {
	@Override
	public List<ShapeSequence> findBoundaries(GroupOfShapes group) {
		List<ShapeSequence> shapeSequences = new ArrayList<ShapeSequence>();
		ShapeSequence emptySequence = new ShapeSequence();
		for (Shape shape : group.getShapes()) {
			emptySequence.addShape(shape);
		}
		shapeSequences.add(emptySequence);
		return shapeSequences;
	}
}
