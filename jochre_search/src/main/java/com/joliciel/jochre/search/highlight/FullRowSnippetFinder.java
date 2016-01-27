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

import com.joliciel.jochre.search.IndexFieldNotFoundException;
import com.joliciel.jochre.search.JochreIndexDocument;
import com.joliciel.jochre.search.JochreIndexField;
import com.joliciel.jochre.search.JochreSearchException;
import com.joliciel.jochre.search.SearchService;
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.utils.LogUtils;

/**
 * Returns snippets which span an entire row, making it possible to match text and graphic snippets.
 * @author Assaf Urieli
 *
 */
class FullRowSnippetFinder implements SnippetFinder {
	private static final Log LOG = LogFactory.getLog(FullRowSnippetFinder.class);
	IndexSearcher indexSearcher;
	
	private SearchService searchService;
	
	public FullRowSnippetFinder(IndexSearcher indexSearcher) {
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
					String title = doc.get(JochreIndexField.title.name());
					String startPage = doc.get(JochreIndexField.startPage.name());
					String endPage = doc.get(JochreIndexField.endPage.name());
					LOG.debug("Content: " + content);
					throw new RuntimeException(term.toString() + " cannot fit into contents for doc " + title + ", pages " + startPage + " to " + endPage + ", length: " + content.length());
				}
				
				int pageIndex = term.getPayload().getPageIndex();
				int blockIndex = term.getPayload().getTextBlockIndex();
				int rowIndex = term.getPayload().getTextLineIndex();
				
				int startRowIndex = rowIndex > 0 ? rowIndex-1 : rowIndex;
				int start = jochreDoc.getStartIndex(pageIndex, blockIndex, startRowIndex);
				int end = -1;
				try {
					// provoke an IndexFileNotFoundException if rowIndex+1 doesn't exist.
					jochreDoc.getStartIndex(pageIndex, blockIndex, rowIndex+1);
					end = jochreDoc.getEndIndex(pageIndex, blockIndex, rowIndex+1);
				} catch (IndexFieldNotFoundException e) {
					end = jochreDoc.getEndIndex(pageIndex, blockIndex, rowIndex);
				}
				
				Snippet snippet = new Snippet(docId, term.getField(), start, end);
				
				List<HighlightTerm> snippetTerms = new ArrayList<HighlightTerm>();
				snippetTerms.add(term);
				int j=-1;
				boolean snippetAlreadyAdded = false;
				for (HighlightTerm otherTerm : highlightTerms) {
					j++;
					if (j==i)
						continue;
					
					if (otherTerm.getStartOffset()>= snippet.getStartOffset() && otherTerm.getEndOffset()<=snippet.getEndOffset()) {
						if (j<i) {
							snippetAlreadyAdded = true;
							break;
						}
						
						snippetTerms.add(otherTerm);
						if (otherTerm.getPayload().getPageIndex()!=term.getPayload().getPageIndex())
							throw new JochreSearchException("otherTerm on wrong page");
						
						if (otherTerm.getPayload().getTextBlockIndex()!=term.getPayload().getTextBlockIndex())
							throw new JochreSearchException("otherTerm on wrong paragraph");
						
						if (otherTerm.getPayload().getTextLineIndex()>term.getPayload().getTextLineIndex()) {
							try {
								// provoke an IndexFileNotFoundException if rowIndex+1 doesn't exist.
								jochreDoc.getStartIndex(pageIndex, blockIndex, otherTerm.getPayload().getTextLineIndex()+1);
								int newEnd = jochreDoc.getEndIndex(pageIndex, blockIndex, otherTerm.getPayload().getTextLineIndex()+1);
								snippet.setEndOffset(newEnd);
							} catch (IndexFieldNotFoundException e) {
								// do nothing
							}
						}
					} // term within current snippet boundaries
				} // next term
				
				snippet.setHighlightTerms(snippetTerms);
				
				if (!snippetAlreadyAdded)
					heap.add(snippet);
			}
			
			// if we have no snippets, add one per field type
			if (heap.isEmpty()) {
				String content = jochreDoc.getContents();
				
				int end = -1;
				
				for (int pageIndex = jochreDoc.getStartPage(); pageIndex<=jochreDoc.getEndPage(); pageIndex++) {
					for (int blockIndex = 0; blockIndex<10; blockIndex++) {
						for (int rowIndex = 0; rowIndex<10; rowIndex++) {
							try {
								jochreDoc.getStartIndex(pageIndex, blockIndex, rowIndex);
								end = jochreDoc.getEndIndex(pageIndex, blockIndex, rowIndex);
								break;
							} catch (IndexFieldNotFoundException e) {
								// do nothing
							}
						}
						if (end>=0)
							break;
					}
					if (end>=0)
						break;
				}
				if (end>content.length()) end = content.length();
				
				Snippet snippet = new Snippet(docId, fields.iterator().next(), 0, end);
				snippet.setPageIndex(jochreDoc.getStartPage());
				if (LOG.isTraceEnabled())
					LOG.trace("Snippet candidate: " + snippet);
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
			
			for (Snippet snippet : snippets) {
				LOG.debug("Added snippet: " + snippet.toJson());
			}
			return snippets;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new JochreException(e);
		}
	}

	public SearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

}
