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

import com.joliciel.jochre.JochreSession;

/**
 * A User of the Jochre application.
 * 
 * @author Assaf Urieli
 *
 */
public class User {
  private int id;
  private String username;
  private String password;
  private String firstName;
  private String lastName;
  private int failedLoginCount = 0;
  private int loginCount = 0;
  private UserRole role = UserRole.GUEST;

  private final SecurityDao securityDao;

  public User(JochreSession jochreSession) {
    this.securityDao = SecurityDao.getInstance(jochreSession);
  }

  /**
   * Attempt to login using a given password.
   * 
   * @return true if succeeded, false if failed.
   */
  public boolean login(String password) {
    if (password.equals(this.password)) {
      this.setLoginCount(this.loginCount + 1);
      this.save();
      return true;
    } else {
      this.setFailedLoginCount(this.failedLoginCount + 1);
      this.save();

      Parameters parameters = securityDao.loadParameters();
      parameters.loginFailed();
      parameters.save();
      return false;
    }
  }

  public void save() {
    securityDao.saveUserInternal(this);
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public int getFailedLoginCount() {
    return failedLoginCount;
  }

  void setFailedLoginCount(int failedLoginCount) {
    this.failedLoginCount = failedLoginCount;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    User other = (User) obj;
    if (username == null) {
      if (other.username != null)
        return false;
    } else if (!username.equals(other.username))
      return false;
    return true;
  }

  public int getLoginCount() {
    return loginCount;
  }

  void setLoginCount(int loginCount) {
    this.loginCount = loginCount;
  }

  public String getFullName() {
    return this.firstName + " " + this.lastName;
  }

  public int getId() {
    return id;
  }

  void setId(int id) {
    this.id = id;
  }

}
