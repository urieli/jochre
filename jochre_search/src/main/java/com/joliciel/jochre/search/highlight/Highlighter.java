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
package com.joliciel.jochre.search.highlight;

import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

/**
 * Returns highlighted terms within a document, based on the method via which
 * the document was retrieved.
 */
public interface Highlighter {
	/**
	 * Find all of the terms to highlight in a given set of doc ids.
	 */
	public Map<Integer, NavigableSet<HighlightTerm>> highlight(Set<Integer> docIds, Set<String> fields);
}
