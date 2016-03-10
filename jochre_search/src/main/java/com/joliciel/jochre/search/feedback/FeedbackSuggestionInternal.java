package com.joliciel.jochre.search.feedback;

import java.util.Date;

interface FeedbackSuggestionInternal extends FeedbackSuggestion {

	public void setCreateDate(Date createDate);

	public void setPreviousText(String previousText);

	public void setText(String text);

	public void setLanguage(String language);

	public void setFont(String font);

	public void setWordId(int wordId);

	public void setWord(FeedbackWord word);

	public void setUser(String user);

	public void setId(int id);

	public boolean isNew();
}
