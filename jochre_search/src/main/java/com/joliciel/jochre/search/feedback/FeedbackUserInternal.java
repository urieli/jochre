package com.joliciel.jochre.search.feedback;

interface FeedbackUserInternal extends FeedbackUser {

	public abstract void setUserName(String userName);

	public abstract void setId(int id);

	public abstract boolean isNew();

	public abstract void save();

}
