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
package com.joliciel.jochre.stats;

import java.util.Collection;
import java.util.Comparator;

/**
 * A comparator which orders a set of collections by decreasing cardinality.
 * @author Assaf Urieli
 *
 */
public class CardinalityComparator<T> implements Comparator<Collection<T>> {

  @Override
  public int compare(Collection<T> o1, Collection<T> o2) {
    if (o1.equals(o2))
      return 0;
    if (o1.size()==o2.size())
      return 1;
    return o2.size()-o1.size();
  }

}
