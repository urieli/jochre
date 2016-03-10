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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class FeedbackQueryImpl implements FeedbackQueryInternal {
	private int id;
	private String user;
	private String ip;
	private Date date;
	private int resultCount;
	private Map<FeedbackCriterion, String> clauses = new HashMap<FeedbackCriterion, String>();
	
	private FeedbackServiceInternal feedbackService;
	
	@Override
	public int getId() {
		return id;
	}
	@Override
	public void setId(int id) {
		this.id = id;
	}
	@Override
	public String getUser() {
		return user;
	}
	@Override
	public void setUser(String user) {
		this.user = user;
	}
	@Override
	public String getIp() {
		return ip;
	}
	@Override
	public void setIp(String ip) {
		this.ip = ip;
	}
	@Override
	public Date getDate() {
		return date;
	}
	@Override
	public void setDate(Date date) {
		this.date = date;
	}
	
	@Override
	public int getResultCount() {
		return resultCount;
	}
	@Override
	public void setResultCount(int resultCount) {
		this.resultCount = resultCount;
	}
	@Override
	public Map<FeedbackCriterion, String> getClauses() {
		return clauses;
	}
	
	@Override
	public void addClause(FeedbackCriterion criterion, String text) {
		this.clauses.put(criterion, text);
	}
	public FeedbackServiceInternal getFeedbackService() {
		return feedbackService;
	}
	public void setFeedbackService(FeedbackServiceInternal feedbackService) {
		this.feedbackService = feedbackService;
	}
	@Override
	public boolean isNew() {
		return id==0;
	}
	@Override
	public void save() {
		this.feedbackService.saveQueryInternal(this);
	}

}
