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

import java.util.List;
import java.util.Set;

public interface SnippetFinder {
	/**
	 * Find the best n snippets corresponding to a list of highlight terms.
	 * @param doc The Lucene document whose snippets we want
	 * @param highlightTerms The previously retrieved highlight terms for the document.
	 * @param textFieldTypes The text field types whose snippets we want - in case no highlightTerms were provided, we'll build default snippets out of the corresponding text fields.
	 * @param maxSnippets The maximum number of snippets to return.
	 * @return
	 */
	public List<Snippet> findSnippets(int docId, Set<String> fields, Set<HighlightTerm> highlightTerms, int maxSnippets);
	
}
