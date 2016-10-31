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

import com.joliciel.jochre.JochreSession;

/**
 * Returns true if all component characters of this letter are valid (allows for
 * merged letters). Empty strings are allowed.
 * 
 * @author Assaf Urieli
 *
 */
public class ComponentCharacterValidator implements LetterValidator {
	private final JochreSession jochreSession;

	public ComponentCharacterValidator(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
	}

	@Override
	public boolean validate(String letter) {
		if (letter.length() == 0)
			return true;
		if (jochreSession.getLinguistics().isCharacterValidationActive()) {
			for (int i = 0; i < letter.length(); i++) {
				char c = letter.charAt(i);
				if (c == '|')
					continue;
				if (!jochreSession.getLinguistics().getValidCharacters().contains(c)) {
					return false;
				}
			}
		}
		return true;
	}

}
