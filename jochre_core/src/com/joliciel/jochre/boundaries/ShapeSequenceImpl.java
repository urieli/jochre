package com.joliciel.jochre.boundaries;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.jochre.graphics.Shape;

class ShapeSequenceImpl extends ArrayList<ShapeInSequence> implements ShapeSequence, Comparable<ShapeSequence> {
	private static final long serialVersionUID = 8564092412152008511L;
	
	private List<Double> decisionProbabilities = new ArrayList<Double>();
	private BoundaryServiceInternal boundaryServiceInternal;
	
	ShapeSequenceImpl() {
		super();
	}
	
	public ShapeSequenceImpl(int initialCapacity) {
		super(initialCapacity);
	}
	
	/**
	 * Create a shape sequence from an existing history,
	 * with space for one additional shape at the end.
	 * @param history
	 */
	public ShapeSequenceImpl(ShapeSequence history) {
		super(history.size()+1);
		this.addAll(history);
	}
	
	/**
	 * Combine two sequences into one.
	 * @param sequence1
	 * @param sequence2
	 */
	public ShapeSequenceImpl(ShapeSequence sequence1, ShapeSequence sequence2) {
		super(sequence1.size() + sequence2.size());
		
		this.addAll(sequence1);
		this.addAll(sequence2);
		for (double prob : sequence1.getDecisionProbabilities())
			this.addDecision(prob);
		for (double prob : sequence2.getDecisionProbabilities())
			this.addDecision(prob);
		
		int i = 0;
		for (ShapeInSequence shapeInSequence : this) {
			shapeInSequence.setIndex(i);
			i++;
		}
	}
	
	@Override
	public double getScore() {
		double score = 0;
		
		for (double decisionProb : decisionProbabilities) {
			score += Math.log(decisionProb);
		}
		
		// apply a geometric mean
		if (decisionProbabilities.size()>0)
			score = score / decisionProbabilities.size();
		score = Math.exp(score);
		return score;
	}

	@Override
	public void addDecision(double probability) {
		this.decisionProbabilities.add(probability);
	}

	@Override
	public List<Double> getDecisionProbabilities() {
		return decisionProbabilities;
	}

	@Override
	public int compareTo(ShapeSequence o) {
		if (this.equals(o))
			return 0;
		if (this.getScore()<o.getScore()) {
			return 1;
		} else if (this.getScore()>o.getScore()) {
			return -1;
		} else {
			return 1;
		}
	}

	ShapeInSequence addShapeInternal(Shape shape, Shape originalShape) {
		ShapeInSequence shapeInSequence = boundaryServiceInternal.getShapeInSequence(this, shape, this.size());
		this.add(shapeInSequence);
		if (shape.getGroup()==null)
			shape.setGroup(originalShape.getGroup());
		return shapeInSequence;
	}
	
	@Override
	public ShapeInSequence addShape(Shape shape) {
		ShapeInSequence shapeInSequence = this.addShapeInternal(shape, shape);
		shapeInSequence.getOriginalShapes().add(shape);
		return shapeInSequence;
	}

	public BoundaryServiceInternal getBoundaryServiceInternal() {
		return boundaryServiceInternal;
	}

	public void setBoundaryServiceInternal(
			BoundaryServiceInternal boundaryServiceInternal) {
		this.boundaryServiceInternal = boundaryServiceInternal;
	}

	@Override
	public ShapeInSequence addShape(Shape shape, Shape originalShape) {
		ShapeInSequence shapeInSequence = this.addShapeInternal(shape, originalShape);
		shapeInSequence.getOriginalShapes().add(originalShape);
		return shapeInSequence;
	}

	@Override
	public ShapeInSequence addShape(Shape shape, Shape[] originalShapes) {
		ShapeInSequence shapeInSequence = this.addShapeInternal(shape, originalShapes[0]);
		for (Shape originalShape : originalShapes)
			shapeInSequence.getOriginalShapes().add(originalShape);
		return shapeInSequence;
	}
	
	@Override
	public ShapeInSequence addShape(Shape shape, List<Shape> originalShapes) {
		ShapeInSequence shapeInSequence = this.addShapeInternal(shape, originalShapes.get(0));
		for (Shape originalShape : originalShapes)
			shapeInSequence.getOriginalShapes().add(originalShape);
		return shapeInSequence;
	}


}
