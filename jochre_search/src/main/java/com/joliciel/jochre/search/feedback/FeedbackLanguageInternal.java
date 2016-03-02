package com.joliciel.jochre.search.feedback;

interface FeedbackLanguageInternal extends FeedbackLanguage {
	public void setId(int id);
	public void setCode(String code);
	public boolean isNew();
	public void save();
}
