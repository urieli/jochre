package com.joliciel.jochre.search.feedback;

import java.util.Date;

class FeedbackSuggestionImpl implements FeedbackSuggestionInternal {
	private int id;
	private FeedbackUser user;
	private int userId;
	private FeedbackWord word;
	private int wordId;
	private FeedbackFont font;
	private int fontId;
	private FeedbackLanguage language;
	private int languageId;
	private String text;
	private String previousText;
	private Date createDate;
	private boolean applied = false;
	private boolean ignored = false;
	
	private FeedbackServiceInternal feedbackService;
	
	public int getId() {
		return id;
	}
	@Override
	public void setId(int id) {
		this.id = id;
	}
	public FeedbackUser getUser() {
		if (this.user==null && this.userId!=0)
			this.user = this.feedbackService.loadUser(this.userId);
		return user;
	}
	@Override
	public void setUser(FeedbackUser user) {
		this.user = user;
		if (user!=null)
			this.userId = user.getId();
	}
	public int getUserId() {
		return userId;
	}
	@Override
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public FeedbackWord getWord() {
		if (this.word==null && this.wordId!=0)
			this.word = this.feedbackService.loadWord(this.wordId);
		return word;
	}
	@Override
	public void setWord(FeedbackWord word) {
		this.word = word;
		if (word!=null)
			this.wordId = word.getId();
	}
	public int getWordId() {
		return wordId;
	}
	@Override
	public void setWordId(int wordId) {
		this.wordId = wordId;
	}
	public FeedbackFont getFont() {
		if (this.font==null && this.fontId!=0)
			this.font = this.feedbackService.loadFont(this.fontId);
		return font;
	}
	@Override
	public void setFont(FeedbackFont font) {
		this.font = font;
		if (font!=null)
			this.fontId = font.getId();
	}
	public int getFontId() {
		return fontId;
	}
	@Override
	public void setFontId(int fontId) {
		this.fontId = fontId;
	}
	public FeedbackLanguage getLanguage() {
		if (this.language==null && this.languageId!=0)
			this.language = this.feedbackService.loadLanguage(this.languageId);
		return language;
	}
	@Override
	public void setLanguage(FeedbackLanguage language) {
		this.language = language;
		if (language!=null)
			this.languageId = language.getId();
	}
	public int getLanguageId() {
		return languageId;
	}
	@Override
	public void setLanguageId(int languageId) {
		this.languageId = languageId;
	}
	public String getText() {
		return text;
	}
	@Override
	public void setText(String text) {
		this.text = text;
	}
	public String getPreviousText() {
		return previousText;
	}
	@Override
	public void setPreviousText(String previousText) {
		this.previousText = previousText;
	}
	public Date getCreateDate() {
		return createDate;
	}
	@Override
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	public boolean isApplied() {
		return applied;
	}
	@Override
	public void setApplied(boolean applied) {
		this.applied = applied;
	}
	public boolean isIgnored() {
		return ignored;
	}
	@Override
	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}
	@Override
	public void save() {
		this.feedbackService.saveSuggestionInternal(this);
	}
	@Override
	public boolean isNew() {
		return id==0;
	}
	public FeedbackServiceInternal getFeedbackService() {
		return feedbackService;
	}
	public void setFeedbackService(FeedbackServiceInternal feedbackService) {
		this.feedbackService = feedbackService;
	}
	
}
