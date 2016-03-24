///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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
package com.joliciel.jochre.search.lexicon;

import java.io.File;
import java.util.Locale;

public interface LexiconService {
	/**
	 * Return a text normaliser for the current locale, or none if no text normaliser is available for this locale.
	 */
	public TextNormaliser getTextNormaliser(Locale locale);
	
	public TextFileLexicon getTextFileLexicon(Locale locale);
	
	public Lexicon deserializeLexicon(File lexiconFile);
}
