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

class RowOfShapesFirstShapeTopComparator implements Comparator<RowOfShapes> {

	@Override
	public int compare(RowOfShapes row1, RowOfShapes row2) {
		if (row1.equals(row2))
			return 0;
		int distance = row1.getShapes().iterator().next().getTop() - row2.getShapes().iterator().next().getTop();
		if (distance<0)
			return -1;
		if (distance>0)
			return 1;
		return 1;
	}

}
