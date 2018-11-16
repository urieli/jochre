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

import com.joliciel.jochre.boundaries.features.ShapeInSequenceWrapper;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;

/**
 * Represents a Shape forming part of a ShapeSequence. Allows us to include the
 * same Shape in multiple Sequences, which is useful to avoid having to
 * recalculate various shape parameters several times. Also allows us to gather
 * information about the shape's position in the sequence and the other shapes
 * in the sequence.
 * 
 * @author Assaf Urieli
 *
 */
public class ShapeInSequence implements ShapeWrapper, ShapeInSequenceWrapper {
  private final Shape shape;
  private int index;
  private final ShapeSequence shapeSequence;
  private final List<Shape> originalShapes = new ArrayList<Shape>();

  public ShapeInSequence(Shape shape, ShapeSequence shapeSequence, int index) {
    super();
    this.shape = shape;
    this.index = index;
    this.shapeSequence = shapeSequence;
  }

  @Override
  public Shape getShape() {
    return shape;
  }

  /**
   * The index within this sequence.
   */
  public int getIndex() {
    return index;
  }

  void setIndex(int index) {
    this.index = index;
  }

  /**
   * The sequence containing this shape.
   */
  public ShapeSequence getShapeSequence() {
    return shapeSequence;
  }

  /**
   * Get the shape or shapes from which this shape was formed by splitting,
   * merging, or simply placing it in the sequence.
   */
  public List<Shape> getOriginalShapes() {
    return originalShapes;
  }

  @Override
  public ShapeInSequence getShapeInSequence() {
    return this;
  }

  @Override
  public String toString() {
    return "ShapeInSequence [shape=" + shape + ", index=" + index + "]";
  }

}
