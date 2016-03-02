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

import java.util.List;

import com.joliciel.jochre.search.JochreIndexSearcher;

/**
 * A service for retrieving and saving feedback information.
 * @author Assaf Urieli
 *
 */
public interface FeedbackService {
	/**
	 * Make a suggestion for a given word in a JochreDocument.
	 * @param docId The Lucene docId
	 * @param offset The word's offset within the document.
	 * @param suggestion The new suggestion
	 * @param username The user who made the suggestion
	 * @param fontCode The font code for this suggestion
	 * @param languageCode The language code for this suggestion
	 * @return the suggestion created
	 */
	public FeedbackSuggestion makeSuggestion(JochreIndexSearcher indexSearcher, int docId, int offset, String suggestion, String username, String fontCode, String languageCode);

	/**
	 * Find any suggestions which have not yet been applied, in order of creation.
	 * @return
	 */
	public List<FeedbackSuggestion> findUnappliedSuggestions();
	
	/**
	 * Find any suggestions made on a given document path and page index, in order of creation.
	 * @param path
	 * @param pageIndex
	 * @return
	 */
	public List<FeedbackSuggestion> findSuggestions(String path, int pageIndex);
}
