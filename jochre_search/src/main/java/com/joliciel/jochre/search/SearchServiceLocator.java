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

import java.util.Locale;

import com.joliciel.jochre.search.alto.AltoServiceLocator;
import com.joliciel.jochre.search.lexicon.LexiconServiceLocator;

public class SearchServiceLocator {
	private static SearchServiceLocator instance;
	private SearchServiceImpl searchService;
	private Locale locale;
	
	private SearchServiceLocator(Locale locale) {
		this.locale = locale;
	}
	
	public static SearchServiceLocator getInstance(Locale locale) {
		if (instance==null) {
			instance = new SearchServiceLocator(locale);
		}
		return instance;
	}
	
	public SearchService getSearchService() {
		if (searchService==null) {
			searchService = new SearchServiceImpl();
			searchService.setAltoService(AltoServiceLocator.getInstance(this).getAltoService());
			searchService.setLexiconService(LexiconServiceLocator.getInstance(this).getLexiconService());
			searchService.setLocale(locale);
		}
		return searchService;
	}
}
