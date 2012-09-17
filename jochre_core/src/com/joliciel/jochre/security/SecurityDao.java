///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.jochre.security;

import java.util.List;

import javax.sql.DataSource;

interface SecurityDao {
	/**
	 * Return all users in the system.
	 * @return
	 */
	List<User> findUsers();

	User loadUser(int userId);
	User findUser(String username);
	void saveUserInternal(UserInternal user);
	
	Parameters loadParameters(int parametersId);
	void saveParametersInternal(ParametersInternal parameters);

	public abstract void setDataSource(DataSource dataSource);
	public abstract DataSource getDataSource();
	public abstract void setSecurityServiceInternal(SecurityServiceInternal securityServiceInternal);
	public abstract SecurityServiceInternal getSecurityServiceInternal();
}
