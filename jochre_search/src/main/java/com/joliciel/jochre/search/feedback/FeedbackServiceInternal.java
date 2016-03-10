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
	public FeedbackDocument findOrCreateDocument(String path);
	public FeedbackRow findOrCreateRow(FeedbackDocument doc, int pageIndex, Rectangle rectangle, BufferedImage rowImage);
	public FeedbackWord findOrCreateWord(JochreIndexWord jochreWord);
	public FeedbackWordInternal getEmptyFeedbackWordInternal();
	public FeedbackDocumentInternal getEmptyFeedbackDocumentInternal();
	public FeedbackRowInternal getEmptyFeedbackRowInternal();
	public FeedbackSuggestionInternal getEmptyFeedbackSuggestionInternal();
	public void saveSuggestionInternal(FeedbackSuggestionInternal suggestion);

	public void saveRowInternal(FeedbackRowInternal row);
	public void saveWordInternal(FeedbackWordInternal word);
	public void saveDocumentInternal(FeedbackDocumentInternal doc);

	public FeedbackRow loadRow(int rowId);
	public FeedbackDocument loadDocument(int docId);
	public FeedbackWord loadWord(int wordId);
	public FeedbackQueryInternal getEmptyFeedbackQueryInternal();
	public void saveQueryInternal(FeedbackQueryInternal query);
}
