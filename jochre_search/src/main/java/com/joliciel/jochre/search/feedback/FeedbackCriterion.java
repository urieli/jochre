///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Assaf Urieli
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
package com.joliciel.jochre.search.feedback;

import java.util.HashMap;
import java.util.Map;

import com.joliciel.jochre.search.JochreSearchException;

/**
 * A single query criterion type.
 * 
 * @author Assaf Urieli
 *
 */
public enum FeedbackCriterion {
	text,
	author,
	includeAuthors,
	title,
	strict;

	private int id;
	private static Map<Integer, FeedbackCriterion> idMap = new HashMap<>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
		idMap.put(id, this);
	}

	public static FeedbackCriterion forId(int id) {
		FeedbackCriterion criterion = idMap.get(id);
		if (criterion == null)
			throw new JochreSearchException("Unknown criterion for id: " + id);
		return criterion;
	}
}
