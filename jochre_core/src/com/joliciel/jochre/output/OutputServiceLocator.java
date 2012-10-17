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
package com.joliciel.jochre.output;

import javax.sql.DataSource;

import com.joliciel.jochre.JochreServiceLocator;

public class OutputServiceLocator {
	OutputServiceImpl outputService = null;

	private DataSource dataSource;
	private JochreServiceLocator jochreServiceLocator;
	
	public OutputServiceLocator(JochreServiceLocator jochreServiceLocator, DataSource dataSource) {
		this.jochreServiceLocator = jochreServiceLocator;
		this.dataSource = dataSource;
	}
	
	public OutputService getTextService() {
		if (outputService==null) {
			outputService = new OutputServiceImpl();
		}
		return outputService;
	}


	public JochreServiceLocator getJochreServiceLocator() {
		return jochreServiceLocator;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	
}
