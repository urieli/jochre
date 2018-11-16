package com.joliciel.jochre.boundaries;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.Rectangle;
import com.joliciel.jochre.graphics.RectangleImpl;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.ClassificationSolution;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.GeometricMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.Solution;

/**
 * A sequence of shapes resulting from a shape split or merge, with a score.
 * 
 * @author Assaf Urieli
 *
 */
public class ShapeSequence extends ArrayList<ShapeInSequence> implements ClassificationSolution, Comparable<ShapeSequence> {
  private static final long serialVersionUID = 1L;

  private double score = 0.0;
  private boolean scoreCalculated = false;

  private List<Decision> decisions = new ArrayList<Decision>();
  private List<Solution> underlyingSolutions = new ArrayList<Solution>();
  @SuppressWarnings("rawtypes")
  private ScoringStrategy scoringStrategy = new GeometricMeanScoringStrategy();

  public ShapeSequence() {
    super();
  }

  public ShapeSequence(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Create a shape sequence from an existing history, with space for one
   * additional shape at the end.
   */
  public ShapeSequence(ShapeSequence history) {
    super(history.size() + 1);
    this.addAll(history);
    this.decisions.addAll(history.getDecisions());
  }

  /**
   * Combine two sequences into one.
   */
  public ShapeSequence(ShapeSequence sequence1, ShapeSequence sequence2) {
    super((sequence1 == null ? 0 : sequence1.size()) + (sequence2 == null ? 0 : sequence2.size()));

    if (sequence1 != null) {
      this.addAll(sequence1);
      this.decisions.addAll(sequence1.getDecisions());
    }
    if (sequence2 != null) {
      this.addAll(sequence2);
      this.decisions.addAll(sequence2.getDecisions());
    }

    int i = 0;
    for (ShapeInSequence shapeInSequence : this) {
      shapeInSequence.setIndex(i);
      i++;
    }
  }

  /**
   * The score attached to this particular sequence.
   */
  @Override
  @SuppressWarnings("unchecked")
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
    if (this.getScore() < o.getScore()) {
      return 1;
    } else if (this.getScore() > o.getScore()) {
      return -1;
    } else {
      return 1;
    }
  }

  ShapeInSequence addShapeInternal(Shape shape, Shape originalShape) {
    ShapeInSequence shapeInSequence = new ShapeInSequence(shape, this, this.size());
    this.add(shapeInSequence);
    if (shape.getGroup() == null)
      shape.setGroup(originalShape.getGroup());
    return shapeInSequence;
  }

  /**
   * Add a given shape to this sequence.
   */
  public ShapeInSequence addShape(Shape shape) {
    ShapeInSequence shapeInSequence = this.addShapeInternal(shape, shape);
    shapeInSequence.getOriginalShapes().add(shape);
    return shapeInSequence;
  }

  /**
   * Add a given shape to this sequence, for the original shape provided.
   */
  public ShapeInSequence addShape(Shape shape, Shape originalShape) {
    ShapeInSequence shapeInSequence = this.addShapeInternal(shape, originalShape);
    shapeInSequence.getOriginalShapes().add(originalShape);
    return shapeInSequence;
  }

  /**
   * Add a given shape to this sequence, for the original shapes in the array.
   */
  public ShapeInSequence addShape(Shape shape, Shape[] originalShapes) {
    ShapeInSequence shapeInSequence = this.addShapeInternal(shape, originalShapes[0]);
    for (Shape originalShape : originalShapes)
      shapeInSequence.getOriginalShapes().add(originalShape);
    return shapeInSequence;
  }

  /**
   * Add a given shape to this sequence for the original shapes in the list
   * provided.
   */
  public ShapeInSequence addShape(Shape shape, List<Shape> originalShapes) {
    ShapeInSequence shapeInSequence = this.addShapeInternal(shape, originalShapes.get(0));
    for (Shape originalShape : originalShapes)
      shapeInSequence.getOriginalShapes().add(originalShape);
    return shapeInSequence;
  }

  @Override
  public List<Decision> getDecisions() {
    return decisions;
  }

  @Override
  public List<Solution> getUnderlyingSolutions() {
    return underlyingSolutions;
  }

  @Override
  public void addDecision(Decision decision) {
    this.decisions.add(decision);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public ScoringStrategy getScoringStrategy() {
    return scoringStrategy;
  }

  @Override
  public void setScoringStrategy(@SuppressWarnings("rawtypes") ScoringStrategy scoringStrategy) {
    this.scoringStrategy = scoringStrategy;
  }

  /**
   * Return the rectangle enclosing this shape sequence in a particular group.
   */
  public Rectangle getRectangleInGroup(GroupOfShapes group) {
    boolean haveShapes = false;
    RectangleImpl rectangle = new RectangleImpl(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    for (ShapeInSequence shapeInSequence : this) {
      Shape shape = shapeInSequence.getShape();
      if (shape.getGroup().equals(group)) {
        haveShapes = true;
        if (shape.getLeft() < rectangle.getLeft())
          rectangle.setLeft(shape.getLeft());
        if (shape.getTop() < rectangle.getTop())
          rectangle.setTop(shape.getTop());
        if (shape.getRight() > rectangle.getRight())
          rectangle.setRight(shape.getRight());
        if (shape.getBottom() > rectangle.getBottom())
          rectangle.setBottom(shape.getBottom());
      }
    }
    if (!haveShapes)
      rectangle = null;
    return rectangle;
  }

}
