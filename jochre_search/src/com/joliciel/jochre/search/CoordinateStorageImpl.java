///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
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
package com.joliciel.jochre.search;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

class CoordinateStorageImpl implements CoordinateStorage, Serializable {
	private static final long serialVersionUID = 1L;
	private Map<Integer, List<Rectangle>> coordinates = new HashMap<Integer, List<Rectangle>>();
	private SortedSet<Integer> offsets = new TreeSet<Integer>();
	
	@Override
	public List<Rectangle> getRectangles(int offset) {
		return coordinates.get(offset);
	}

	@Override
	public void setRectangles(int offset, List<Rectangle> rectangles) {
		coordinates.put(offset, rectangles);
		offsets.add(offset);
	}

	public List<Rectangle> getNearestRectangles(int offset) {
		int nearestOffset = -1;
		SortedSet<Integer> tailSet = offsets.tailSet(offset);
		if (tailSet.size()>0) {
			nearestOffset = tailSet.first();
		} else {
			SortedSet<Integer> headSet = offsets.headSet(offset);
			nearestOffset = headSet.last();
		}
		if (nearestOffset>=0) {
			return this.getRectangles(nearestOffset);
		}
		return null;
	}
}
