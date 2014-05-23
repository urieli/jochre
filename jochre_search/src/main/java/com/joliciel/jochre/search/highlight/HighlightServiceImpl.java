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
package com.joliciel.jochre.search.highlight;

import org.apache.lucene.search.IndexSearcher;

import com.joliciel.jochre.search.JochreQuery;
import com.joliciel.jochre.search.SearchService;

class HighlightServiceImpl implements HighlightService {
	private SearchService searchService;
	
	@Override
	public HighlightManager getHighlightManager(IndexSearcher indexSearcher) {
		HighlightManagerImpl manager = new HighlightManagerImpl(indexSearcher);
		manager.setSearchService(this.getSearchService());
		manager.setHighlightService(this);
		return manager;
	}

	@Override
	public Highlighter getHighlighter(JochreQuery query,
			IndexSearcher indexSearcher) {
		LuceneQueryHighlighter highlighter = new LuceneQueryHighlighter(query, indexSearcher);
		highlighter.setSearchService(this.getSearchService());
		return highlighter;
	}

	public SearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	@Override
	public SnippetFinder getSnippetFinder(IndexSearcher indexSearcher) {
		FixedSizeSnippetFinder snippetFinder = new FixedSizeSnippetFinder(indexSearcher);
		snippetFinder.setSearchService(searchService);
		return snippetFinder;
	}
	
	
}
