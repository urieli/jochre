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

import com.joliciel.jochre.Entity;

/**
 * A User of the Jochre application.
 * @author Assaf Urieli
 *
 */
public interface User extends Entity {
	/**
	 * Attempt to login using a given password.
	 * @return true if succeeded, false if failed.
	 */
	public boolean login(String password);

	public abstract String getUsername();

	public abstract String getLastName();

	public abstract String getFirstName();

	public abstract UserRole getRole();

	public abstract void setLastName(String lastName);

	public abstract void setFirstName(String firstName);

	public abstract void setPassword(String password);

	public abstract int getLoginCount();

	public abstract int getFailedLoginCount();
	public abstract String getFullName();

}
