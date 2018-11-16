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

class RowOfShapesVerticalLocationComparator implements Comparator<RowOfShapes> {

  @Override
  public int compare(RowOfShapes row1, RowOfShapes row2) {
    if (row1.equals(row2))
      return 0;
    double row1MidPoint = (double)(row1.getTop() + row1.getBottom()) / 2.0;
    double row2MidPoint = (double)(row2.getTop() + row2.getBottom()) / 2.0;
    
    double distance = row1MidPoint - row2MidPoint;
    if (distance<0)
      return -1;
    if (distance>0)
      return 1;
    return 1;
  }

}
