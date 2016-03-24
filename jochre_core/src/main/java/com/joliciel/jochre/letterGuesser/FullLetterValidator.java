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
package com.joliciel.jochre.letterGuesser;

import java.util.Locale;

import com.joliciel.jochre.JochreSession;

/**
 * Returns true only if the entire letter is valid. Empty strings are allowed.
 * @author Assaf Urieli
 *
 */
public class FullLetterValidator implements LetterValidator {
	private Locale locale;
	
	public FullLetterValidator(Locale locale) {
		super();
		this.locale = locale;
	}

	@Override
	public boolean validate(String letter) {
		return (letter.length()==0||JochreSession.getInstance().getLinguistics().getValidLetters().contains(letter));
	}

	public Locale getLocale() {
		return locale;
	}

}
