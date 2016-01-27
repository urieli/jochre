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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
		FullRowSnippetFinder snippetFinder = new FullRowSnippetFinder(indexSearcher);
		snippetFinder.setSearchService(searchService);
		return snippetFinder;
	}
	
	public Set<HighlightTerm> combineOverlaps(Set<HighlightTerm> terms) {
		Set<HighlightTerm> fixedTerms = new TreeSet<HighlightTerm>();
		HighlightTerm previousTerm = null;
		List<TreeSet<HighlightTerm>> setsToCombine = new ArrayList<TreeSet<HighlightTerm>>();
		TreeSet<HighlightTerm> combinedSet = null;
		Set<HighlightTerm> termsToCombine = new TreeSet<HighlightTerm>();
		int endOffset = -1;
		for (HighlightTerm term : terms) {
			if (previousTerm!=null) {
				if (term.getDocId()==previousTerm.getDocId() && term.getStartOffset()<endOffset) {
					if (combinedSet==null) {
						combinedSet = new TreeSet<HighlightTerm>();
						setsToCombine.add(combinedSet);
					}
					combinedSet.add(previousTerm);
					combinedSet.add(term);
					termsToCombine.add(previousTerm);
					termsToCombine.add(term);
					endOffset = previousTerm.getEndOffset() > term.getEndOffset() ? previousTerm.getEndOffset() : term.getEndOffset();
				} else {
					combinedSet = null;
					endOffset = term.getEndOffset();
				}
			} else {
				endOffset = term.getEndOffset();
			}
			previousTerm = term;
		}
		
		if (termsToCombine.size()>0) {
			fixedTerms.addAll(terms);
			fixedTerms.removeAll(termsToCombine);
			
			for (TreeSet<HighlightTerm> setToCombine : setsToCombine) {
				HighlightTerm firstTerm = setToCombine.first();
				HighlightTerm lastTerm = setToCombine.last();
				HighlightTerm combinedTerm = new HighlightTerm(firstTerm.getDocId(), firstTerm.getField(), firstTerm.getStartOffset(), lastTerm.getEndOffset(), firstTerm.getPayload());
				// for now, we simply add their weights together
				for (HighlightTerm termToCombine : setToCombine) {
					combinedTerm.setWeight(combinedTerm.getWeight() + termToCombine.getWeight());
				}
				fixedTerms.add(combinedTerm);
			}
			return fixedTerms;
		} else {
			return terms;
		}
	}
}
