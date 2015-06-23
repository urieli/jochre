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
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.joliciel.jochre.search.JochreIndexDocument;
import com.joliciel.jochre.search.Rectangle;
import com.joliciel.jochre.search.SearchService;
import com.joliciel.talismane.utils.LogUtils;


class HighlightManagerImpl implements HighlightManager {
	private static final Log LOG = LogFactory.getLog(HighlightManagerImpl.class);

	private IndexSearcher indexSearcher;
	private int decimalPlaces = 2;
	private DecimalFormatSymbols enSymbols = new DecimalFormatSymbols(Locale.US);
	private DecimalFormat df = new DecimalFormat("0.00", enSymbols);
	private SnippetFinder snippetFinder;
	private HighlightTermDecorator decorator;
	private boolean includeText = false;
	private boolean includeGraphics = false;

	private double minWeight = 0;
	private int titleSnippetCount = 1;
	private int snippetCount = 3;
	private int snippetSize = 80;
	
	private SearchService searchService;
	private HighlightService highlightService;
	
	public HighlightManagerImpl(IndexSearcher indexSearcher) {
		super();
		this.indexSearcher = indexSearcher;
	}

	@Override
	public void highlight(Highlighter highlighter, Set<Integer> docIds, Set<String> fields, Writer out) {
		try {
			Map<Integer,NavigableSet<HighlightTerm>> termMap = highlighter.highlight(docIds, fields);
			JsonFactory jsonFactory = new JsonFactory();
			JsonGenerator jsonGen = jsonFactory.createGenerator(out);

			jsonGen.writeStartObject();
			
			for (int docId : docIds) {
				Document doc = indexSearcher.doc(docId);
				jsonGen.writeObjectFieldStart(doc.get("id"));
				jsonGen.writeStringField("path", doc.get("path"));
				jsonGen.writeNumberField("docId", docId);
				
				jsonGen.writeArrayFieldStart("terms");
				for (HighlightTerm term : termMap.get(docId)) {
					fields.add(term.getField());
					term.toJson(jsonGen, df);
				}
				jsonGen.writeEndArray();
				
				if (includeText) {
					for (String field : fields) {
						jsonGen.writeObjectFieldStart("field" + field);
						Set<HighlightTerm> terms = termMap.get(docId);
						jsonGen.writeStringField("contents", this.displayHighlights(docId, field, terms));
						jsonGen.writeEndObject();
					}
				}

				
				jsonGen.writeEndObject();
			}

			jsonGen.writeEndObject();
			jsonGen.flush();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	@Override
	public Map<Integer,List<Snippet>> findSnippets(Set<Integer> docIds, Set<String> fields, Map<Integer, NavigableSet<HighlightTerm>> termMap,
			int maxSnippets, int snippetSize) {
		SnippetFinder snippetFinder = this.getSnippetFinder();

		Map<Integer,List<Snippet>> snippetMap = new HashMap<Integer, List<Snippet>>();
		
		for (int docId : docIds) {
			Set<HighlightTerm> terms = termMap.get(docId);
			List<Snippet> snippets = snippetFinder.findSnippets(docId, fields, terms, maxSnippets, snippetSize);
			snippetMap.put(docId, snippets);
		}
		return snippetMap;
	}
	
	@Override
	public void findSnippets(Highlighter highlighter, Set<Integer> docIds, Set<String> fields, Writer out) {
		try {
			Map<Integer,NavigableSet<HighlightTerm>> termMap = highlighter.highlight(docIds, fields);
			Map<Integer,List<Snippet>> snippetMap = this.findSnippets(docIds, fields, termMap, this.getSnippetCount(), this.getSnippetSize());
			
			JsonFactory jsonFactory = new JsonFactory();
			JsonGenerator jsonGen = jsonFactory.createGenerator(out);

			jsonGen.writeStartObject();
			
			for (int docId : docIds) {
				Document doc = indexSearcher.doc(docId);
				jsonGen.writeObjectFieldStart(doc.get("name"));
				jsonGen.writeStringField("path", doc.get("path"));
				jsonGen.writeNumberField("docId", docId);

				jsonGen.writeArrayFieldStart("snippets");
				for (Snippet snippet : snippetMap.get(docId)) {
					snippet.toJson(jsonGen, df);
				}
				jsonGen.writeEndArray();

				if (includeText) {
					jsonGen.writeArrayFieldStart("snippetText");
					for (Snippet snippet : snippetMap.get(docId)) {
						jsonGen.writeStartObject();
						jsonGen.writeStringField("snippet", this.displaySnippet(docId, snippet));
						jsonGen.writeEndObject();
					}
					jsonGen.writeEndArray();
				}
				
				if (includeGraphics) {
					jsonGen.writeArrayFieldStart("snippetGraphics");
					for (Snippet snippet : snippetMap.get(docId)) {
						jsonGen.writeStartObject();
						ImageSnippet imageSnippet = this.getImageSnippet(snippet);
						jsonGen.writeNumberField("left", imageSnippet.getRectangle().getLeft());
						jsonGen.writeNumberField("top", imageSnippet.getRectangle().getTop());
						jsonGen.writeNumberField("right", imageSnippet.getRectangle().getRight());
						jsonGen.writeNumberField("bottom", imageSnippet.getRectangle().getBottom());
						
						jsonGen.writeArrayFieldStart("highlights");
						for (Rectangle highlight : imageSnippet.getHighlights()) {
							jsonGen.writeStartObject();
							jsonGen.writeNumberField("left", highlight.getLeft());
							jsonGen.writeNumberField("top", highlight.getTop());
							jsonGen.writeNumberField("right", highlight.getRight());
							jsonGen.writeNumberField("bottom", highlight.getBottom());
							jsonGen.writeEndObject();
						}
						jsonGen.writeEndArray();
						jsonGen.writeEndObject();
					}
					jsonGen.writeEndArray();
				}
				jsonGen.writeEndObject();
			} // next doc

			jsonGen.writeEndObject();
			jsonGen.flush();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	@Override
	public String displayHighlights(int docId, String field, Set<HighlightTerm> terms) {
		JochreIndexDocument jochreDoc = searchService.getJochreIndexDocument(indexSearcher, docId);
		String content = jochreDoc.getContents();
		if (LOG.isTraceEnabled()) {
			LOG.trace("Displaying highlights for doc " + docId + ", field " + field);
		}
		StringBuilder sb = new StringBuilder();

		int currentPos = 0;

		for (HighlightTerm term : terms) {
			if (field.equals(term.getField())) {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Adding term: " + term.getStartOffset() + ", " + term.getEndOffset());
				}
				sb.append(content.substring(currentPos, term.getStartOffset()));
				String termText = content.substring(term.getStartOffset(), term.getEndOffset());
				if (term.getWeight()>=minWeight)
					sb.append(this.getDecorator().decorate(termText));
				else
					sb.append(termText);
				currentPos = term.getEndOffset();
			}
		}
		sb.append(content.substring(currentPos, content.length()));
		
		if (LOG.isTraceEnabled()) {
			LOG.trace(sb.toString());
		}
		return sb.toString();
	}
	
	@Override
	public String displaySnippet(int docId, Snippet snippet) {
		JochreIndexDocument jochreDoc = searchService.getJochreIndexDocument(indexSearcher, docId);
		String content = jochreDoc.getContents();

		if (LOG.isTraceEnabled()) {
			LOG.trace("Displaying snippet for doc " + docId + ", snippet " + snippet.getStartOffset() + ", " + snippet.getEndOffset());
		}

		StringBuilder sb = new StringBuilder();
		int currentPos = snippet.getStartOffset();

		for (HighlightTerm term : snippet.getHighlightTerms()) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Adding term: " + term.getStartOffset() + ", " + term.getEndOffset());
			}
			sb.append(content.substring(currentPos, term.getStartOffset()));
			String termText = content.substring(term.getStartOffset(), term.getEndOffset());
			if (term.getWeight()>=minWeight)
				sb.append(this.getDecorator().decorate(termText));
			else
				sb.append(termText);
			currentPos = term.getEndOffset();
		}
		sb.append(content.substring(currentPos, snippet.getEndOffset()));
		
		if (LOG.isTraceEnabled()) {
			LOG.trace(sb.toString());
		}

		return sb.toString();
	}

	@Override
	public int getDecimalPlaces() {
		return decimalPlaces;
	}

	@Override
	public void setDecimalPlaces(int decimalPlaces) {
		if (this.decimalPlaces!=decimalPlaces) {
			this.decimalPlaces = decimalPlaces;
			String dfFormat = "0.";
			for (int i = 0; i<decimalPlaces;i++) {
				dfFormat += "0";
			}
			df = new DecimalFormat(dfFormat, enSymbols);
		}
	}

	@Override
	public SnippetFinder getSnippetFinder() {
		if (snippetFinder==null) {
			snippetFinder = highlightService.getSnippetFinder(indexSearcher);
		}
		return snippetFinder;
	}

	@Override
	public void setSnippetFinder(SnippetFinder snippetFinder) {
		this.snippetFinder = snippetFinder;
	}

	public HighlightTermDecorator getDecorator() {
		if (decorator==null) {
			decorator = new WrappingDecorator("<b>", "</b>");
		}
		return decorator;
	}

	public void setDecorator(HighlightTermDecorator decorator) {
		this.decorator = decorator;
	}

	public boolean isIncludeText() {
		return includeText;
	}

	public void setIncludeText(boolean includeText) {
		this.includeText = includeText;
	}

	public boolean isIncludeGraphics() {
		return includeGraphics;
	}

	public void setIncludeGraphics(boolean includeGraphics) {
		this.includeGraphics = includeGraphics;
	}

	public double getMinWeight() {
		return minWeight;
	}

	public void setMinWeight(double minWeight) {
		this.minWeight = minWeight;
	}

	public int getTitleSnippetCount() {
		return titleSnippetCount;
	}

	public void setTitleSnippetCount(int titleSnippetCount) {
		this.titleSnippetCount = titleSnippetCount;
	}

	public int getSnippetCount() {
		return snippetCount;
	}

	public void setSnippetCount(int snippetCount) {
		this.snippetCount = snippetCount;
	}

	public int getSnippetSize() {
		return snippetSize;
	}

	public void setSnippetSize(int snippetSize) {
		this.snippetSize = snippetSize;
	}

	@Override
	public ImageSnippet getImageSnippet(Snippet snippet) {
		JochreIndexDocument jochreDoc = searchService.getJochreIndexDocument(indexSearcher, snippet.getDocId());
		ImageSnippet imageSnippet = new ImageSnippet(jochreDoc, snippet);
		return imageSnippet;
	}

	public SearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public HighlightService getHighlightService() {
		return highlightService;
	}

	public void setHighlightService(HighlightService highlightService) {
		this.highlightService = highlightService;
	}
	
	
}
