package com.joliciel.jochre.boundaries;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.HarmonicMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.Solution;

class ShapeSequenceImpl extends ArrayList<ShapeInSequence> implements ShapeSequence, Comparable<ShapeSequence> {
	private static final long serialVersionUID = 8564092412152008511L;
	
	private double score = 0.0;
	private boolean scoreCalculated = false;
	private BoundaryServiceInternal boundaryServiceInternal;
	
	private List<Decision<SplitMergeOutcome>> decisions = new ArrayList<Decision<SplitMergeOutcome>>();
	private List<Solution<?>> underlyingSolutions = new ArrayList<Solution<?>>();
	private ScoringStrategy scoringStrategy = new HarmonicMeanScoringStrategy();
	
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
		this.decisions.addAll(history.getDecisions());
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
		this.decisions.addAll(sequence1.getDecisions());
		this.decisions.addAll(sequence2.getDecisions());
		
		int i = 0;
		for (ShapeInSequence shapeInSequence : this) {
			shapeInSequence.setIndex(i);
			i++;
		}
	}
	
	@Override
	public double getScore() {
		if (!scoreCalculated) {
			score = this.scoringStrategy.calculateScore(this);
			scoreCalculated = true;
		}
		return score;
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

	@Override
	public List<Decision<SplitMergeOutcome>> getDecisions() {
		return decisions;
	}

	@Override
	public List<Solution<?>> getUnderlyingSolutions() {
		return underlyingSolutions;
	}

	@Override
	public void addDecision(Decision<SplitMergeOutcome> decision) {
		this.decisions.add(decision);
	}

	public ScoringStrategy getScoringStrategy() {
		return scoringStrategy;
	}

	public void setScoringStrategy(ScoringStrategy scoringStrategy) {
		this.scoringStrategy = scoringStrategy;
	}

}
