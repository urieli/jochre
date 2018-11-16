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

import java.util.HashSet;
import java.util.Set;

/**
 * A single vertical line segment within a shape,
 * surrounded on either side (top and bottom) by white space or the shape's edge.
 * @author Assaf Urieli
 *
 */
public class VerticalLineSegment implements Comparable<VerticalLineSegment> {
  VerticalLineSegment(int x, int y) {
    this.x = x;
    this.yTop = y;
    this.yBottom = y;
  }
  /**
   * x coordinate of this vertical line segment
   */
  public int x;
  
  /**
   * top y coordinate of this vertical line segment
   */
  public int yTop;
  
  /**
   * bottom y coordinate of this vertical line segment
   */
  public int yBottom;
  
  /**
   * Vertical line segments touching this line segment on the left
   * (could be several, if they're separated by white space).
   */
  public Set<VerticalLineSegment> leftSegments = new HashSet<VerticalLineSegment>();
  
  /**
   * Vertical line segments touching this line segment on the right
   * (could be several, if they're separated by white space).
   */
  public Set<VerticalLineSegment> rightSegments = new HashSet<VerticalLineSegment>();
  
  /**
   * Length of this line segment.
   */
  public int length() { return yBottom - yTop + 1; }
  
  /**
   * When traversing line segments recursively, this attribute
   * allows us to make sure we only touch this line segment once.
   */
  public boolean touched = false;
  
  @Override
  public int compareTo(VerticalLineSegment line2) {
    if (this.x<line2.x)
      return -1;
    else if (this.x>line2.x)
      return 1;
    else if (this.yTop<line2.yTop)
      return -1;
    else if (this.yTop>line2.yTop)
      return 1;
    else
      return 0;
  }

  @Override
  public int hashCode() {
    String hash = x + "|" + yTop;
    return hash.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj instanceof VerticalLineSegment) {
      VerticalLineSegment line2 = (VerticalLineSegment) obj;
      return (line2.x==this.x && line2.yTop==this.yTop);
    }
    return false;
  }

  @Override
  public String toString() {
    return "VerticalLineSegment [x=" + x + ", yTop=" + yTop + ", yBottom="
        + yBottom + "]";
  }
  
  
}