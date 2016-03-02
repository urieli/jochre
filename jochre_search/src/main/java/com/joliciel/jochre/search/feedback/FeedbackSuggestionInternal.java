package com.joliciel.jochre.search.feedback;

import java.util.Date;

interface FeedbackSuggestionInternal extends FeedbackSuggestion {

	public void setCreateDate(Date createDate);

	public void setPreviousText(String previousText);

	public void setText(String text);

	public void setLanguageId(int languageId);

	public void setLanguage(FeedbackLanguage language);

	public void setFont(FeedbackFont font);

	public void setWordId(int wordId);

	public void setWord(FeedbackWord word);

	public void setUserId(int userId);

	public void setUser(FeedbackUser user);

	public void setId(int id);

	public void setFontId(int fontId);

	public boolean isNew();

}
