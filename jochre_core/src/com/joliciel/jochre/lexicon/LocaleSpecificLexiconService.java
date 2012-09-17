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

import java.util.Locale;

/**
 * A lexicon service for a specific locale.
 * @author Assaf Urieli
 *
 */
public interface LocaleSpecificLexiconService {
	/**
	 * The locale for this lexicon service.
	 * @return
	 */
	public Locale getLocale();
	
	/**
	 * A lexicon for use with this locale.
	 * @return
	 */
	public Lexicon getLexicon();
	
	/**
	 * A word-splitter for use with this locale.
	 * @return
	 */
	public WordSplitter getWordSplitter();
	
	/**
	 * The path to a directory containing lexical resources.
	 * @return
	 */
	public String getLexiconDirPath();
	public void setLexiconDirPath(String lexiconDirPath);
}
