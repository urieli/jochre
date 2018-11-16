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

/**
 * A single search query executed by a user.
 * 
 * @author Assaf Urieli
 *
 */
public class FeedbackQuery {
  private int id;
  private String user;
  private String ip;
  private Date date;
  private int resultCount;
  private Map<FeedbackCriterion, String> clauses = new HashMap<>();

  private final FeedbackDAO feedbackDAO;

  public FeedbackQuery(String user, String ip, FeedbackDAO feedbackDAO) {
    this(feedbackDAO);
    this.user = user;
    this.ip = ip;
  }

  FeedbackQuery(FeedbackDAO feedbackDAO) {
    this.feedbackDAO = feedbackDAO;
  }

  public int getId() {
    return id;
  }

  void setId(int id) {
    this.id = id;
  }

  public String getUser() {
    return user;
  }

  void setUser(String user) {
    this.user = user;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public Date getDate() {
    return date;
  }

  void setDate(Date date) {
    this.date = date;
  }

  public int getResultCount() {
    return resultCount;
  }

  public void setResultCount(int resultCount) {
    this.resultCount = resultCount;
  }

  public Map<FeedbackCriterion, String> getClauses() {
    return clauses;
  }

  public void addClause(FeedbackCriterion criterion, String text) {
    this.clauses.put(criterion, text);
  }

  boolean isNew() {
    return id == 0;
  }

  public void save() {
    this.feedbackDAO.saveQuery(this);
  }

}
