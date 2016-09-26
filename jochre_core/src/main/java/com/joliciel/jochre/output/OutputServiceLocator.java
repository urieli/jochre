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

import com.joliciel.jochre.JochreServiceLocator;
import com.joliciel.jochre.JochreSession;

public class OutputServiceLocator {
	OutputServiceImpl outputService = null;

	private JochreSession jochreSession;
	private JochreServiceLocator jochreServiceLocator;

	public OutputServiceLocator(JochreServiceLocator jochreServiceLocator, JochreSession jochreSession) {
		this.jochreServiceLocator = jochreServiceLocator;
		this.jochreSession = jochreSession;
	}

	public OutputService getTextService() {
		if (outputService == null) {
			outputService = new OutputServiceImpl();
		}
		return outputService;
	}

	public JochreServiceLocator getJochreServiceLocator() {
		return jochreServiceLocator;
	}

	public JochreSession getJochreSession() {
		return jochreSession;
	}

	public void setJochreSession(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
	}

}
