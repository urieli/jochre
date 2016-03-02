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

/**
 * A unique user of JochreSearch, identified by his username.
 * Note that it is client code responsibility to manage usernames and accounts.
 * @author Assaf Urieli
 *
 */
public interface FeedbackUser {
	/**
	 * The user's unique internal id.
	 * @return
	 */
	public int getId();
	
	/**
	 * The user's unique user name.
	 * @return
	 */
	public String getUserName();
}
