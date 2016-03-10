package com.joliciel.jochre.search.feedback;

import java.util.Date;

class FeedbackSuggestionImpl implements FeedbackSuggestionInternal {
	private int id;
	private String user;
	private FeedbackWord word;
	private int wordId;
	private String font;
	private String language;
	private String text;
	private String previousText;
	private Date createDate;
	private boolean applied = false;
	private boolean ignored = false;
	private String ip = null;
	
	private FeedbackServiceInternal feedbackService;
	
	public int getId() {
		return id;
	}
	@Override
	public void setId(int id) {
		this.id = id;
	}
	public String getUser() {
		return user;
	}
	@Override
	public void setUser(String user) {
		this.user = user;
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
	public String getFont() {
		return font;
	}
	@Override
	public void setFont(String font) {
		this.font = font;
	}

	public String getLanguage() {
		return language;
	}
	@Override
	public void setLanguage(String language) {
		this.language = language;

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
	@Override
	public String getIp() {
		return ip;
	}
	@Override
	public void setIp(String ip) {
		this.ip = ip;
	}
}
