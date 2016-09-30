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
package com.joliciel.jochre.lexicon;

import java.util.Iterator;

/**
 * An interface giving a frequency of occurrence for each word. If it's a
 * corpus-based lexicon, it can give a real occurrence frequency. If it's a
 * dictionary-based lexicon, it can give 0 or 1.
 * 
 * @author Assaf Urieli
 *
 */
public interface Lexicon {
	/**
	 * Frequency of occurrence for a given word. If the word break certain
	 * locale-specific constraints (e.g. letters of a certain type at impossible
	 * places), can return a frequency of -1.
	 */
	public int getFrequency(String word);

	/**
	 * Return all words in this lexicon.
	 */
	public Iterator<String> getWords();
}
