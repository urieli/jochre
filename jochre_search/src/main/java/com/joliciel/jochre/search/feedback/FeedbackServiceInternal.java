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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.joliciel.jochre.search.JochreIndexWord;

interface FeedbackServiceInternal extends FeedbackService {
	FeedbackDocument findOrCreateDocument(String path);
	FeedbackFont findOrCreateFont(String code);
	FeedbackLanguage findOrCreateLanguage(String code);
	FeedbackUser findOrCreateuser(String userName);
	FeedbackRow findOrCreateRow(FeedbackDocument doc, int pageIndex, Rectangle rectangle, BufferedImage rowImage);
	FeedbackWord findOrCreateWord(JochreIndexWord jochreWord);
	FeedbackWordInternal getEmptyFeedbackWordInternal();
	FeedbackUserInternal getEmptyFeedbackUserInternal();
	FeedbackFontInternal getEmptyFeedbackFontInternal();
	FeedbackLanguageInternal getEmptyFeedbackLanguageInternal();
	FeedbackDocumentInternal getEmptyFeedbackDocumentInternal();
	FeedbackRowInternal getEmptyFeedbackRowInternal();
	FeedbackSuggestionInternal getEmptyFeedbackSuggestionInternal();
	void saveSuggestionInternal(FeedbackSuggestionInternal suggestion);
	public abstract void saveLanguageInternal(FeedbackLanguageInternal language);
	public abstract void saveFontInternal(FeedbackFontInternal font);
	public abstract void saveUserInternal(FeedbackUserInternal user);
	public abstract void saveRowInternal(FeedbackRowInternal row);
	public abstract void saveWordInternal(FeedbackWordInternal word);
	public abstract void saveDocumentInternal(FeedbackDocumentInternal doc);
	FeedbackUser loadUser(int userId);
	public abstract FeedbackLanguage loadLanguage(int languageId);
	public abstract FeedbackFont loadFont(int fontId);
	public abstract FeedbackRow loadRow(int rowId);
	public abstract FeedbackDocument loadDocument(int docId);
	public abstract FeedbackWord loadWord(int wordId);
}
