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

import java.util.Set;

import com.joliciel.jochre.search.JochreIndexSearcher;
import com.joliciel.jochre.search.JochreQuery;

public interface HighlightService {
	HighlightManager getHighlightManager(JochreIndexSearcher indexSearcher);

	Highlighter getHighlighter(JochreQuery query, JochreIndexSearcher indexSearcher);
	
	SnippetFinder getSnippetFinder(JochreIndexSearcher indexSearcher);
	
	/**
	 * If any highlight terms overlap, combine them into a single term that spans all overlaps.
	 * @param terms
	 * @return
	 */
	public Set<HighlightTerm> combineOverlaps(Set<HighlightTerm> terms);
}
