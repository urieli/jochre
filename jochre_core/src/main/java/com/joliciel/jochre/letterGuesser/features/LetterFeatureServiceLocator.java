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
package com.joliciel.jochre.letterGuesser.features;

import com.joliciel.jochre.JochreServiceLocator;

public class LetterFeatureServiceLocator {
	LetterFeatureServiceImpl letterFeatureService = null;
	private final JochreServiceLocator jochreServiceLocator;

	public LetterFeatureServiceLocator(JochreServiceLocator jochreServiceLocator) {
		this.jochreServiceLocator = jochreServiceLocator;
	}

	public LetterFeatureService getLetterFeatureService() {
		if (letterFeatureService == null) {
			letterFeatureService = new LetterFeatureServiceImpl(jochreServiceLocator.getJochreSession());
			letterFeatureService.setGraphicsFeatureService(this.jochreServiceLocator.getGraphicsFeatureServiceLocator().getGraphicsFeatureService());
			letterFeatureService.setLetterGuesserService(this.jochreServiceLocator.getLetterGuesserServiceLocator().getLetterGuesserService());
		}
		return letterFeatureService;
	}

	public JochreServiceLocator getJochreServiceLocator() {
		return jochreServiceLocator;
	}

}
