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

/**
 * Decides whether a particular letter is valid or not for training/guessing/evaluating.
 * Useful for keeping out letters from foreign alphabets.
 * @author Assaf Urieli
 *
 */
public interface LetterValidator {
	/**
	 * Return true if this letter is valid for training/guessing/evaluating, false otherwise.
	 * @param letter
	 * @return
	 */
	public boolean validate(String letter);
	
	public Locale getLocale();
}
