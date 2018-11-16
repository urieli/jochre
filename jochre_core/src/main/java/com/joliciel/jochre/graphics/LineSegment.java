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
package com.joliciel.jochre.graphics;

import java.util.BitSet;

/**
 * A line segment within an image, defined by its start point and end point.
 * 
 * @author Assaf Urieli
 *
 */
public final class LineSegment implements Comparable<LineSegment> {
  private int startX;
  private int startY;
  private int endX;
  private int endY;
  private int length;
  private Shape shape;
  private LineDefinition lineDefinition;
  private BitSet enclosingRectangle;

  public LineSegment(Shape shape, LineDefinition lineDefinition, int startX, int startY, int endX, int endY) {
    this.shape = shape;
    this.lineDefinition = lineDefinition;

    this.startX = startX;
    this.endX = endX;
    this.startY = startY;
    this.endY = endY;
  }

  public int getStartX() {
    return startX;
  }

  public int getStartY() {
    return startY;
  }

  public int getEndX() {
    return endX;
  }

  public int getEndY() {
    return endY;
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  @Override
  public int compareTo(LineSegment lineSegment) {
    int result = (lineSegment.getLength() - this.getLength());
    if (result != 0)
      return result;

    if ((this.getStartY() == lineSegment.getEndY()) && (this.getEndY() == lineSegment.getStartY()) && (this.getStartX() == lineSegment.getEndX())
        && (this.getEndX() == lineSegment.getStartX()))
      return 0;

    result = this.getStartY() - lineSegment.getStartY();
    if (result != 0)
      return result;
    result = this.getStartX() - lineSegment.getStartX();
    if (result != 0)
      return result;
    result = this.getEndY() - lineSegment.getEndY();
    if (result != 0)
      return result;
    result = this.getEndX() - lineSegment.getEndX();
    return result;
  }

  /**
   * Returns a (tilted) rectangle enclosing this line segment if each end-point
   * is extended by halfWidth pixels in both directions towards the edges of the
   * shape.
   * 
   */
  public BitSet getEnclosingRectangle(int halfWidth) {
    if (enclosingRectangle == null) {
      enclosingRectangle = new BitSet(shape.getHeight() * shape.getWidth());

      this.lineDefinition.trace(enclosingRectangle, shape, startX, startY, length, 0);
      int leftX;
      int leftY;
      int rightX;
      int rightY;

      for (int i = 1; i <= halfWidth; i++) {
        // either surround from top and bottom, or from right and left,
        // depending
        // on the sector
        int sector = this.lineDefinition.getSector();
        if (sector == 0 || sector == 3 || sector == 4 || sector == 7) {
          leftX = startX;
          leftY = startY - i;
          rightX = startX;
          rightY = startY + i;
        } else {
          leftX = startX - i;
          leftY = startY;
          rightX = startX + i;
          rightY = startY;
        }

        // this isn't exactly a rectangle (more like a parallelogram) but oh
        // well
        this.lineDefinition.trace(enclosingRectangle, shape, leftX, leftY, length, 0);
        this.lineDefinition.trace(enclosingRectangle, shape, rightX, rightY, length, 0);
      }
    }

    return enclosingRectangle;
  }

  /**
   * The intersection of the enclosing rectangle of this line segment with the
   * enclosing rectangle of another line segment.
   */
  public BitSet getEnclosingRectangleIntersection(LineSegment otherLine, int halfWidth) {
    BitSet enclosingRectangle1 = this.getEnclosingRectangle(halfWidth);
    BitSet enclosingRectangle2 = otherLine.getEnclosingRectangle(halfWidth);
    BitSet intersection = (BitSet) enclosingRectangle1.clone();
    intersection.and(enclosingRectangle2);
    return intersection;
  }

  /**
   * The line definition defining this line segment.
   */
  public LineDefinition getLineDefinition() {
    return lineDefinition;
  }

  public void setLineDefinition(LineDefinition lineDefinition) {
    this.lineDefinition = lineDefinition;
  }

  /**
   * The shape containing this line segment.
   */
  public Shape getShape() {
    return this.shape;
  }

  public void setShape(Shape shape) {
    this.shape = shape;
  }

}
