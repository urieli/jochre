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

public class ShapeLeftToRightComparator implements Comparator<Shape> {

  @Override
  public int compare(Shape shape1, Shape shape2) {
    if (shape1.equals(shape2))
      return 0;
    if (shape1.getRight()!=shape2.getRight())
      return (shape1.getRight() - shape2.getRight());
    if (shape1.getTop()!=shape2.getTop())
      return (shape1.getTop()-shape2.getTop());
    if (shape1.getLeft()!=shape2.getLeft())
      return (shape1.getLeft() - shape2.getLeft());
    if (shape1.getBottom()!=shape2.getBottom())
      return (shape1.getBottom()-shape2.getBottom());
    return 1;
  }
  

}
