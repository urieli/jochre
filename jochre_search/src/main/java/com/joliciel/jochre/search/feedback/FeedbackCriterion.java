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
  text(1),
  author(2),
  title(3),
  strict(4),
  includeAuthors(5),
  fromYear(6),
  toYear(7),
  sortBy(8),
  sortAscending(9),
  reference(10);

  private final int id;
  private static Map<Integer, FeedbackCriterion> idMap = null;

  FeedbackCriterion(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public static FeedbackCriterion forId(int id) {
    if (idMap == null) {
      idMap = new HashMap<>();
      for (FeedbackCriterion crit : FeedbackCriterion.values()) {
        idMap.put(crit.id, crit);
      }
    }
    FeedbackCriterion criterion = idMap.get(id);
    if (criterion == null)
      throw new JochreSearchException("Unknown criterion for id: " + id);
    return criterion;
  }
}
