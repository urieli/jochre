package com.joliciel.jochre.search.feedback;

public class FeedbackLanguageImpl implements FeedbackLanguageInternal {
	private int id;
	private String code;
	private FeedbackServiceInternal feedbackService;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
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
	public void save() {
		this.feedbackService.saveLanguageInternal(this);
	}
}
