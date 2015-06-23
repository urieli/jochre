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
package com.joliciel.jochre.lang;

import java.util.Set;

/**
 * A interface containing various information about specifics for the current language.
 * (valid characters, valid letters, text direction, etc.).
 * @author Assaf Urieli
 *
 */
public interface Linguistics {
	public Set<String> getValidLetters();
	
	public Set<Character> getValidCharacters();
	
	public Set<String> getDualCharacterLetters();
	
	public Set<Character> getPunctuation();
	
	public boolean isLeftToRight();
	
	public String standardiseWord(String originalWord);
}
