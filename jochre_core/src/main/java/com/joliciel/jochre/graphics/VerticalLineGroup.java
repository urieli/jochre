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

import java.util.List;
import java.util.ArrayList;

/**
 * A group of vertical line segments separating two bridge candidates.
 * @author Assaf Urieli
 *
 */
final class VerticalLineGroup {
  public VerticalLineGroup(Shape shape) {
    this.leftBoundary = shape.getWidth()-1;
    this.rightBoundary = 0;
    this.topBoundary = shape.getHeight()-1;
    this.bottomBoundary = 0;
  }
  /**
   * Total pixel count for this group.
   */
  public int pixelCount = 0;
  
  /**
   * Left-hand boundary for this group.
   */
  public int leftBoundary = -1;
  /**
   * Right-hand boundary for this group.
   */
  public int rightBoundary = -1;
  
  /**
   * Top boundary for this group.
   */
  public int topBoundary = -1;
  
  /**
   * Bottom boundary for this group.
   */
  public int bottomBoundary = -1;
  
  /**
   * Bridge candidates touching this group on the left.
   */
  public List<BridgeCandidate> leftCandidates = new ArrayList<BridgeCandidate>();
  
  /**
   * Bridge candidates touching this group on the right.
   */
  public List<BridgeCandidate> rightCandidates = new ArrayList<BridgeCandidate>();
  
  /**
   * An attribute useful when traversing the groups recursively.
   * Enables us to make sure we only touch this group once.
   */
  public boolean touched = false;
  
}