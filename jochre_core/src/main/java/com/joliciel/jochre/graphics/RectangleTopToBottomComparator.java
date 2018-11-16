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

import java.util.Comparator;

/**
 * Arranges a series of rectangles from top to bottom,
 * and left to right only if top and bottom coordinates are identical.
 * @author Assaf Urieli
 *
 */
class RectangleTopToBottomComparator implements Comparator<Rectangle> {

  @Override
  public int compare(Rectangle rect1, Rectangle rect2) {
    if (rect1.equals(rect2))
      return 0;
    if (rect1.getTop()!=rect2.getTop())
      return (rect1.getTop()-rect2.getTop());
    if (rect1.getBottom()!=rect2.getBottom())
      return (rect1.getBottom()-rect2.getBottom());
    if (rect1.getLeft()!=rect2.getLeft())
      return (rect1.getLeft() - rect2.getLeft());
    if (rect1.getRight()!=rect2.getRight())
      return (rect1.getRight() - rect2.getRight());
    return 1;
  }

}
