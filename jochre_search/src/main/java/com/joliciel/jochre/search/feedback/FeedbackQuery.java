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
import java.util.Map;

/**
 * A single search query executed by a user.
 * @author Assaf Urieli
 *
 */
public interface FeedbackQuery {
	public void addClause(FeedbackCriterion criterion, String text);

	public Map<FeedbackCriterion, String> getClauses();

	public Date getDate();

	public String getIp();

	public String getUser();

	public int getId();

	public int getResultCount();

	public void setResultCount(int resultCount);
	
	public void save();
}
