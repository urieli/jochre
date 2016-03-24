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

import com.joliciel.jochre.EntityNotFoundException;
import com.joliciel.talismane.utils.ObjectCache;

class SecurityServiceImpl implements SecurityServiceInternal {
	private ObjectCache objectCache;	
	@SuppressWarnings("unused")
	private SecurityServiceLocator securityServiceLocator;
	private SecurityDao securityDao;
	
	public SecurityServiceImpl(SecurityServiceLocator securityServiceLocator) {
		this.securityServiceLocator = securityServiceLocator;
	
	}
	@Override
	public User loadUser(int userId) {
		User user = (User) this.objectCache.getEntity(User.class, userId);
        if (user==null) {
        	user = this.getSecurityDao().loadUser(userId);
            if (user==null) {
                throw new EntityNotFoundException("No User found for user id " + userId);
            }
            this.objectCache.putEntity(User.class, userId, user);
        }
        return user;
	}

	@Override
	public User findUser(String username) {
		User user = (User) this.objectCache.getEntity(User.class, username);
        if (user==null) {
        	user = this.getSecurityDao().findUser(username);
            if (user==null) {
                throw new EntityNotFoundException("No User found for username " + username);
            }
            this.objectCache.putEntity(User.class, username, user);
        }
        return user;
	}

	@Override
	public void saveUserInternal(UserInternal user) {
		this.getSecurityDao().saveUserInternal(user);
	}

	@Override
	public UserInternal getEmptyUser() {
		UserImpl user = new UserImpl();
		user.setSecurityServiceInternal(this);
		return user;
	}

	@Override
	public List<User> findUsers() {
		return this.getSecurityDao().findUsers();
	}
	
	@Override
	public ParametersInternal getEmptyParameters() {
		ParametersImpl parameters = new ParametersImpl();
		parameters.setSecurityServiceInternal(this);
		return parameters;
	}
	@Override
	public void saveParametersInternal(ParametersInternal parameters) {
		this.getSecurityDao().saveParametersInternal(parameters);
	}
	
	@Override
	public Parameters loadParameters() {
		Parameters parameters = (Parameters) this.objectCache.getEntity(Parameters.class, 1);
        if (parameters==null) {
        	parameters = this.getSecurityDao().loadParameters(1);
            if (parameters==null) {
                throw new EntityNotFoundException("No Parameters found for parameters id " + 1);
            }
            this.objectCache.putEntity(Parameters.class, 1, parameters);
        }
        return parameters;
	}
	
	public SecurityDao getSecurityDao() {
		this.securityDao.setSecurityServiceInternal(this);
		return securityDao;
	}

	public void setSecurityDao(SecurityDao securityDao) {
		this.securityDao = securityDao;
	}
	public ObjectCache getObjectCache() {
		return objectCache;
	}
	public void setObjectCache(ObjectCache objectCache) {
		this.objectCache = objectCache;
	}

}
