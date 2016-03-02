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

import java.util.Date;

/**
 * A user's suggestion for a given ocr'd word.
 * @author Assaf Urieli
 *
 */
public interface FeedbackSuggestion {
	/**
	 * The unique internal id for this suggestion.
	 * @return
	 */
	int getId();
	
	/**
	 * The user who made this suggestion.
	 * @return
	 */
	FeedbackUser getUser();
	int getUserId();
	
	/**
	 * The word for which the suggestion was made.
	 * @return
	 */
	FeedbackWord getWord();
	int getWordId();
	
	/**
	 * The font which the user indicated for this suggestion.
	 * @return
	 */
	FeedbackFont getFont();
	int getFontId();
	
	/**
	 * The language which the user indicated for this suggestion.
	 * @return
	 */
	FeedbackLanguage getLanguage();
	int getLanguageId();
	
	/**
	 * The suggested text.
	 * @return
	 */
	String getText();
	
	/**
	 * The text previous to the suggestion.
	 * @return
	 */
	String getPreviousText();
	
	/**
	 * The date when the suggestion was made.
	 * @return
	 */
	Date getCreateDate();
	
	/**
	 * Has this suggestion been applied to the index yet?
	 * @return
	 */
	boolean isApplied();
	public void setApplied(boolean applied);
	
	/**
	 * Should this suggestion be ignored?
	 * @return
	 */
	boolean isIgnored();
	public void setIgnored(boolean ignored);
	
	void save();
}
