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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;

import com.joliciel.jochre.search.JochreIndexDocument;
import com.joliciel.jochre.search.SearchService;
import com.joliciel.talismane.utils.LogUtils;


class FixedSizeSnippetFinder implements SnippetFinder {
	private static final Log LOG = LogFactory.getLog(FixedSizeSnippetFinder.class);
	IndexSearcher indexSearcher;
	
	private SearchService searchService;
	
	public FixedSizeSnippetFinder(IndexSearcher indexSearcher) {
		super();
		this.indexSearcher = indexSearcher;
	}


	@Override
	public List<Snippet> findSnippets(int docId, Set<String> fields, Set<HighlightTerm> highlightTerms, int maxSnippets, int snippetSize) {
		try {
			Document doc = indexSearcher.doc(docId);
			JochreIndexDocument jochreDoc = searchService.getJochreIndexDocument(indexSearcher, docId);
			// find best snippet for each term		
			PriorityQueue<Snippet> heap = new PriorityQueue<Snippet>();
			
			int i=-1;
			for (HighlightTerm term : highlightTerms) {
				i++;
				String content = jochreDoc.getContents();
				if (term.getStartOffset()>=content.length()) {
					String title = doc.get("title");
					String startPage = doc.get("startPage");
					String endPage = doc.get("endPage");
					if (content.length()>100) {
						LOG.debug("Content start: " + content.substring(0, 100));
						LOG.debug("Content end: " + content.substring(content.length()-100));
					} else {
						LOG.debug("Content: " + content);
					}
					throw new RuntimeException(term.toString() + " cannot fit into contents for doc " + title + ", pages " + startPage + " to " + endPage + ", length: " + content.length());
				}
				List<HighlightTerm> snippetTerms = new ArrayList<HighlightTerm>();
				snippetTerms.add(term);
				int j=-1;
				boolean foundTextField = false;
				for (HighlightTerm otherTerm : highlightTerms) {
					j++;
					if (j<=i)
						continue;
					if (!otherTerm.getField().equals(term.getField())) {
						if (foundTextField)
							break;
						else
							continue;
					}
					foundTextField = true;
					
					if (otherTerm.getStartOffset()<term.getStartOffset()+snippetSize) {
						snippetTerms.add(otherTerm);
					} else {
						break;
					}
				}
				HighlightTerm lastTerm = snippetTerms.get(snippetTerms.size()-1);
				int middle = (term.getStartOffset() + lastTerm.getEndOffset()) / 2;
				int start = middle - (snippetSize / 2);
				int end = middle + (snippetSize / 2);
				if (start > term.getStartOffset())
					start = term.getStartOffset();
				if (end < lastTerm.getEndOffset())
					end = lastTerm.getEndOffset();
				
				if (start<0)
					start=0;
				if (end > content.length())
					end = content.length();
				
				for (int k=start; k>=0; k--) {
					if (Character.isWhitespace(content.charAt(k))) {
						start = k+1;
						break;
					}
				}
				for (int k=end; k<content.length(); k++) {
					if (Character.isWhitespace(content.charAt(k))) {
						end = k;
						break;
					}
				}
				Snippet snippet = new Snippet(docId, term.getField(), start, end);
				snippet.setHighlightTerms(snippetTerms);
				heap.add(snippet);
			}
			
			// if we have no snippets, add one per field type
			if (heap.isEmpty()) {
				String content = jochreDoc.getContents();
				int end = snippetSize * maxSnippets;
				if (end>content.length()) end = content.length();
				for (int k=end; k<content.length(); k++) {
					if (Character.isWhitespace(content.charAt(k))) {
						end = k;
						break;
					}
				}
				Snippet snippet = new Snippet(docId, fields.iterator().next(), 0, end);
				heap.add(snippet);
			}
			
			List<Snippet> snippets = new ArrayList<Snippet>(maxSnippets);
			while (snippets.size()<maxSnippets && !heap.isEmpty()) {
				Snippet snippet = heap.poll();
				boolean hasOverlap = false;
				for (Snippet otherSnippet : snippets) {
					if (otherSnippet.hasOverlap(snippet))
						hasOverlap = true;
				}
				if (!hasOverlap)
					snippets.add(snippet);
			}
			return snippets;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public SearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

}
