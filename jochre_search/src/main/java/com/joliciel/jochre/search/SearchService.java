///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
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
package com.joliciel.jochre.search;

import java.io.File;
import java.util.Locale;

import com.joliciel.jochre.search.lexicon.Lexicon;

public interface SearchService {
	public JochreIndexDocument getJochreIndexDocument(JochreIndexSearcher indexSearcher, int docId);
	public JochreIndexSearcher getJochreIndexSearcher();
	public void purge();
	public void purgeSearcher();
	
	public JochreQuery getJochreQuery();
	public JochreIndexBuilder getJochreIndexBuilder();
	
	public SearchStatusHolder getSearchStatusHolder();
	
	public Locale getLocale();
	public void setLocale(Locale locale);
	
	public Lexicon getLexicon();
	public void setLexicon(Lexicon lexicon);
	
	/**
	 * Is the current locale left-to-right?
	 */
	public boolean isLeftToRight();
	
	public abstract void setContentDir(File contentDir);
	public abstract File getContentDir();
	public abstract void setIndexDir(File indexDir);
	public abstract File getIndexDir();
}
