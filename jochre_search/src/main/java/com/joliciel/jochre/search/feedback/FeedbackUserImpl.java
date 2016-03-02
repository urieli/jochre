package com.joliciel.jochre.search.feedback;

class FeedbackUserImpl implements FeedbackUserInternal {
	private int id;
	private String userName;
	private FeedbackServiceInternal feedbackService;
	public int getId() {
		return id;
	}
	@Override
	public void setId(int id) {
		this.id = id;
	}
	public String getUserName() {
		return userName;
	}
	@Override
	public void setUserName(String userName) {
		this.userName = userName;
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
	public void save() {
		this.feedbackService.saveUserInternal(this);
	}
}
