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

class RowOfShapesColumnComparator implements Comparator<RowOfShapes> {
	private boolean leftToRight = true;
	public RowOfShapesColumnComparator(boolean leftToRight) {
		this.leftToRight = leftToRight;
	}
	
	@Override
	public int compare(RowOfShapes row1, RowOfShapes row2) {
		if (row1.equals(row2))
			return 0;
		
		if (leftToRight) {
			if (row1.getRight()<row2.getLeft()) {
				return -1;
			} else if (row2.getRight()<row1.getLeft()) {
				return 1;
			} else if (row1.getTop()<row2.getTop()) {
				return -1;
			} else {
				return 1;
			}
		} else {
			if (row1.getLeft()>row2.getRight()) {
				return -1;
			} else if (row2.getLeft()>row1.getRight()) {
				return 1;
			} else if (row1.getTop()<row2.getTop()) {
				return -1;
			} else {
				return 1;
			}
			
		}
	}

}
