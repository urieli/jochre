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

import javax.sql.DataSource;

import com.joliciel.jochre.JochreServiceLocator;

public class SecurityServiceLocator {
	SecurityServiceImpl securityService = null;
	SecurityDaoJdbc securityDao = null;
	private DataSource dataSource;
	private JochreServiceLocator jochreServiceLocator;
	
	public SecurityServiceLocator(JochreServiceLocator jochreServiceLocator, DataSource dataSource) {
		this.jochreServiceLocator = jochreServiceLocator;
		this.dataSource = dataSource;
	}
	
	public SecurityService getSecurityService() {
		if (securityService==null) {
			securityService = new SecurityServiceImpl(this);
			securityService.setSecurityDao(this.getSecurityDao());
			securityService.setObjectCache(this.jochreServiceLocator.getObjectCache());
		}
		return securityService;
	}
	
	private SecurityDao getSecurityDao() {
		if (this.securityDao == null) {
			this.securityDao = new SecurityDaoJdbc();
			this.securityDao.setDataSource(dataSource);
		}
		return securityDao;
	}

	public JochreServiceLocator getJochreServiceLocator() {
		return jochreServiceLocator;
	}
	
}
