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

import com.joliciel.jochre.EntityImpl;

class UserImpl extends EntityImpl implements UserInternal {
	private SecurityServiceInternal securityServiceInternal;
	private String username;
	private String password;
	private String firstName;
	private String lastName;
	private int failedLoginCount = 0;
	private int loginCount = 0;
	private UserRole role = UserRole.GUEST;
	
	@Override
	public boolean login(String password) {
		if (password.equals(this.password)) {
			this.setLoginCount(this.loginCount+1);
			this.save();
			return true;
		} else {
			this.setFailedLoginCount(this.failedLoginCount+1);
			this.save();
			
			Parameters parameters = securityServiceInternal.loadParameters();
			parameters.loginFailed();
			parameters.save();
			return false;
		}
	}

	@Override
	public void saveInternal() {
		this.securityServiceInternal.saveUserInternal(this);
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String getFirstName() {
		return firstName;
	}

	@Override
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Override
	public String getLastName() {
		return lastName;
	}

	@Override
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public SecurityServiceInternal getSecurityServiceInternal() {
		return securityServiceInternal;
	}

	public void setSecurityServiceInternal(
			SecurityServiceInternal securityServiceInternal) {
		this.securityServiceInternal = securityServiceInternal;
	}

	@Override
	public int getFailedLoginCount() {
		return failedLoginCount;
	}

	@Override
	public void setFailedLoginCount(int failedLoginCount) {
		this.failedLoginCount = failedLoginCount;
	}

	@Override
	public UserRole getRole() {
		return role;
	}

	@Override
	public void setRole(UserRole role) {
		this.role = role;
	}

	@Override
	public int hashCode() {
		if (this.isNew())
			return super.hashCode();
		else
			return ((Integer)this.getId()).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this.isNew()) {
			return super.equals(obj);
		} else {
			User otherUser = (User) obj;
			return (this.getId()==otherUser.getId());
		}
	}

	@Override
	public int getLoginCount() {
		return loginCount;
	}

	@Override
	public void setLoginCount(int loginCount) {
		this.loginCount = loginCount;
	}

	@Override
	public String getFullName() {
		return this.firstName + " " + this.lastName;
	}

}
