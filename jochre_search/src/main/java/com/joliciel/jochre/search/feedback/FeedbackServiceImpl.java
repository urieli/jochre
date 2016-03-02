package com.joliciel.jochre.search.feedback;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.jochre.search.JochreIndexDocument;
import com.joliciel.jochre.search.JochreIndexSearcher;
import com.joliciel.jochre.search.JochreIndexWord;
import com.joliciel.jochre.search.SearchService;
import com.joliciel.jochre.utils.JochreException;

class FeedbackServiceImpl implements FeedbackServiceInternal {
	private SearchService searchService;
	private FeedbackDAO feedbackDAO;

	@Override
	public FeedbackSuggestion makeSuggestion(JochreIndexSearcher indexSearcher, int docId, int offset,
			String text, String username, String fontCode,
			String languageCode) {
		if (this.feedbackDAO==null)
			throw new JochreException("Cannot make suggestions without a database to store them.");
		
		JochreIndexDocument jochreDoc = searchService.getJochreIndexDocument(indexSearcher, docId);
		JochreIndexWord jochreWord = jochreDoc.getWord(offset);
		FeedbackWord word = this.findOrCreateWord(jochreWord);
		FeedbackUser user = this.findOrCreateuser(username);
		FeedbackFont font = this.findOrCreateFont(fontCode);
		FeedbackLanguage language = this.findOrCreateLanguage(languageCode);
		FeedbackSuggestionInternal suggestion = this.getEmptyFeedbackSuggestionInternal();
		suggestion.setWord(word);
		suggestion.setUser(user);
		suggestion.setFont(font);
		suggestion.setLanguage(language);
		suggestion.setPreviousText(jochreWord.getText());
		suggestion.setText(text);
		suggestion.save();
		return suggestion;
	}

	public SearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	@Override
	public FeedbackDocument findOrCreateDocument(String path) {
		FeedbackDocument doc = feedbackDAO.findDocument(path);
		if (doc==null) {
			FeedbackDocumentInternal iDoc = this.getEmptyFeedbackDocumentInternal();
			iDoc.setPath(path);
			iDoc.save();
			doc = iDoc;
		}
		return doc;
	}

	@Override
	public FeedbackFont findOrCreateFont(String code) {
		FeedbackFont font = feedbackDAO.findFont(code);
		if (font==null) {
			FeedbackFontInternal iFont = this.getEmptyFeedbackFontInternal();
			iFont.setCode(code);
			iFont.save();
			font = iFont;
		}
		return font;
	}

	@Override
	public FeedbackLanguage findOrCreateLanguage(String code) {
		FeedbackLanguage language = feedbackDAO.findLanguage(code);
		if (language==null) {
			FeedbackLanguageInternal iLanguage = this.getEmptyFeedbackLanguageInternal();
			iLanguage.setCode(code);
			iLanguage.save();
			language = iLanguage;
		}
		return language;
	}

	@Override
	public FeedbackUser findOrCreateuser(String userName) {
		FeedbackUser user = feedbackDAO.findUser(userName);
		if (user==null) {
			FeedbackUserInternal iUser = this.getEmptyFeedbackUserInternal();
			iUser.setUserName(userName);
			iUser.save();
			user = iUser;
		}
		return user;
	}

	@Override
	public FeedbackRow findOrCreateRow(FeedbackDocument doc, int pageIndex,
			Rectangle rectangle, BufferedImage rowImage) {
		FeedbackRow row = feedbackDAO.findRow(doc, pageIndex, rectangle);
		if (row==null) {
			FeedbackRowInternal iRow = this.getEmptyFeedbackRowInternal();
			iRow.setDocument(doc);
			iRow.setPageIndex(pageIndex);
			iRow.setRectangle(rectangle);
			iRow.setImage(rowImage);
			iRow.save();
			row = iRow;
		}
		return row;
	}

	@Override
	public FeedbackWord findOrCreateWord(JochreIndexWord jochreWord) {
		FeedbackDocument doc = this.findOrCreateDocument(jochreWord.getDocument().getPath());
		FeedbackWord word = feedbackDAO.findWord(doc, jochreWord.getPageIndex(), jochreWord.getRectangle());
		if (word==null) {
			FeedbackWordInternal iWord = this.getEmptyFeedbackWordInternal();
			iWord.setRectangle(jochreWord.getRectangle());
			iWord.setImage(jochreWord.getImage());
			iWord.setInitialGuess(jochreWord.getText());
			
			FeedbackRow row = this.findOrCreateRow(doc, jochreWord.getPageIndex(), jochreWord.getRowRectangle(), jochreWord.getRowImage());
			iWord.setRow(row);
			
			if (jochreWord.getSecondRectangle()!=null) {
				iWord.setSecondRectangle(jochreWord.getSecondRectangle());
				FeedbackRow row2 = this.findOrCreateRow(doc, jochreWord.getPageIndex(), jochreWord.getSecondRectangle(), jochreWord.getSecondRowImage());
				iWord.setSecondRow(row2);
			}
			
			iWord.save();
			word = iWord;
		}
		return word;
	}

	public FeedbackDAO getFeedbackDAO() {
		return feedbackDAO;
	}

	public void setFeedbackDAO(FeedbackDAO feedbackDAO) {
		this.feedbackDAO = feedbackDAO;
		if (feedbackDAO!=null)
			this.feedbackDAO.setFeedbackService(this);
	}

	@Override
	public FeedbackWordInternal getEmptyFeedbackWordInternal() {
		FeedbackWordImpl word = new FeedbackWordImpl();
		word.setFeedbackService(this);
		return word;
	}

	@Override
	public FeedbackUserInternal getEmptyFeedbackUserInternal() {
		FeedbackUserImpl user = new FeedbackUserImpl();
		user.setFeedbackService(this);
		return user;
	}

	@Override
	public FeedbackFontInternal getEmptyFeedbackFontInternal() {
		FeedbackFontImpl font = new FeedbackFontImpl();
		font.setFeedbackService(this);
		return font;
	}

	@Override
	public FeedbackLanguageInternal getEmptyFeedbackLanguageInternal() {
		FeedbackLanguageImpl language = new FeedbackLanguageImpl();
		language.setFeedbackService(this);
		return language;
	}

	@Override
	public FeedbackDocumentInternal getEmptyFeedbackDocumentInternal() {
		FeedbackDocumentImpl doc = new FeedbackDocumentImpl();
		doc.setFeedbackService(this);
		return doc;
	}

	@Override
	public FeedbackRowInternal getEmptyFeedbackRowInternal() {
		FeedbackRowImpl row = new FeedbackRowImpl();
		row.setFeedbackService(this);
		return row;
	}

	@Override
	public FeedbackSuggestionInternal getEmptyFeedbackSuggestionInternal() {
		FeedbackSuggestionImpl suggestion = new FeedbackSuggestionImpl();
		suggestion.setFeedbackService(this);
		return suggestion;
	}

	@Override
	public void saveSuggestionInternal(
			FeedbackSuggestionInternal suggestion) {
		this.feedbackDAO.saveSuggestion(suggestion);
	}
	@Override
	public void saveDocumentInternal(
			FeedbackDocumentInternal doc) {
		this.feedbackDAO.saveDocument(doc);
	}
	
	@Override
	public void saveWordInternal(
			FeedbackWordInternal word) {
		this.feedbackDAO.saveWord(word);
	}
	@Override
	public void saveRowInternal(
			FeedbackRowInternal row) {
		this.feedbackDAO.saveRow(row);
	}
	@Override
	public void saveUserInternal(
			FeedbackUserInternal user) {
		this.feedbackDAO.saveUser(user);
	}
	@Override
	public void saveFontInternal(
			FeedbackFontInternal font) {
		this.feedbackDAO.saveFont(font);
	}
	@Override
	public void saveLanguageInternal(
			FeedbackLanguageInternal language) {
		this.feedbackDAO.saveLanguage(language);
	}

	@Override
	public List<FeedbackSuggestion> findUnappliedSuggestions() {
		if (this.feedbackDAO==null)
			return new ArrayList<>();
		return this.feedbackDAO.findUnappliedSuggestions();
	}

	@Override
	public List<FeedbackSuggestion> findSuggestions(String path, int pageIndex) {
		if (this.feedbackDAO==null)
			return new ArrayList<>();
		
		FeedbackDocument doc = this.feedbackDAO.findDocument(path);
		if (doc==null) {
			return new ArrayList<>();
		}
		return this.feedbackDAO.findSuggestions(doc, pageIndex);
	}

	@Override
	public FeedbackUser loadUser(int userId) {
		return this.feedbackDAO.loadUser(userId);
	}
	@Override
	public FeedbackWord loadWord(int wordId) {
		return this.feedbackDAO.loadWord(wordId);
	}
	@Override
	public FeedbackDocument loadDocument(int docId) {
		return this.feedbackDAO.loadDocument(docId);
	}
	@Override
	public FeedbackRow loadRow(int rowId) {
		return this.feedbackDAO.loadRow(rowId);
	}
	@Override
	public FeedbackFont loadFont(int fontId) {
		return this.feedbackDAO.loadFont(fontId);
	}
	@Override
	public FeedbackLanguage loadLanguage(int languageId) {
		return this.feedbackDAO.loadLanguage(languageId);
	}
}
