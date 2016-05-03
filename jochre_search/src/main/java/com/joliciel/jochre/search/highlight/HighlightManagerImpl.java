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

import java.awt.Rectangle;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.joliciel.jochre.search.JochreIndexDocument;
import com.joliciel.jochre.search.JochreIndexField;
import com.joliciel.jochre.search.JochreIndexSearcher;
import com.joliciel.jochre.search.JochreSearchConstants;
import com.joliciel.jochre.search.SearchService;
import com.joliciel.jochre.utils.Either;
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.utils.LogUtils;

class HighlightManagerImpl implements HighlightManager {
	private static final Log LOG = LogFactory.getLog(HighlightManagerImpl.class);

	private JochreIndexSearcher indexSearcher;
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

	public HighlightManagerImpl(JochreIndexSearcher indexSearcher) {
		super();
		this.indexSearcher = indexSearcher;
	}

	@Override
	public void highlight(Highlighter highlighter, Set<Integer> docIds, Set<String> fields, Writer out) {
		try {
			Map<Integer, NavigableSet<HighlightTerm>> termMap = highlighter.highlight(docIds, fields);
			JsonFactory jsonFactory = new JsonFactory();
			JsonGenerator jsonGen = jsonFactory.createGenerator(out);

			jsonGen.writeStartObject();

			for (int docId : docIds) {
				Document doc = indexSearcher.getIndexSearcher().doc(docId);
				jsonGen.writeObjectFieldStart(docId + "");
				jsonGen.writeStringField(JochreIndexField.path.name(), doc.get(JochreIndexField.path.name()));
				jsonGen.writeStringField(JochreIndexField.name.name(), doc.get(JochreIndexField.name.name()));
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
			throw new JochreException(ioe);
		}
	}

	@Override
	public Map<Integer, Either<List<Snippet>, Exception>> findSnippets(Set<Integer> docIds, Set<String> fields,
			Map<Integer, NavigableSet<HighlightTerm>> termMap, int maxSnippets, int snippetSize) {
		SnippetFinder snippetFinder = this.getSnippetFinder();

		Map<Integer, Either<List<Snippet>, Exception>> snippetMap = new HashMap<Integer, Either<List<Snippet>, Exception>>();

		for (int docId : docIds) {
			Set<HighlightTerm> terms = termMap.get(docId);
			Either<List<Snippet>, Exception> result = null;
			try {
				List<Snippet> snippets = snippetFinder.findSnippets(docId, fields, terms, maxSnippets);
				for (Snippet snippet : snippets) {
					snippet.generateText(this);
				}
				result = Either.ofLeft(snippets);
			} catch (Exception e) {
				result = Either.ofRight(e);
			}
			snippetMap.put(docId, result);
		}
		return snippetMap;
	}

	@Override
	public void findSnippets(Highlighter highlighter, Set<Integer> docIds, Set<String> fields, Writer out) {
		try {
			Map<Integer, NavigableSet<HighlightTerm>> termMap = highlighter.highlight(docIds, fields);
			Map<Integer, Either<List<Snippet>, Exception>> snippetMap = this.findSnippets(docIds, fields, termMap, this.getSnippetCount(),
					this.getSnippetSize());

			JsonFactory jsonFactory = new JsonFactory();
			JsonGenerator jsonGen = jsonFactory.createGenerator(out);

			jsonGen.writeStartObject();

			for (int docId : docIds) {
				Document doc = indexSearcher.getIndexSearcher().doc(docId);
				jsonGen.writeObjectFieldStart(docId + "");
				jsonGen.writeStringField(JochreIndexField.path.name(), doc.get(JochreIndexField.path.name()));
				jsonGen.writeStringField(JochreIndexField.name.name(), doc.get(JochreIndexField.name.name()));
				jsonGen.writeNumberField("docId", docId);

				Either<List<Snippet>, Exception> result = snippetMap.get(docId);

				if (result.isLeft()) {
					List<Snippet> snippets = result.getLeft();

					jsonGen.writeArrayFieldStart("snippets");
					for (Snippet snippet : snippets) {
						snippet.toJson(jsonGen, df, this);
					}
					jsonGen.writeEndArray();

					if (includeGraphics) {
						jsonGen.writeArrayFieldStart("snippetGraphics");
						for (Snippet snippet : snippets) {
							jsonGen.writeStartObject();
							ImageSnippet imageSnippet = this.getImageSnippet(snippet);
							jsonGen.writeNumberField("left", imageSnippet.getRectangle().x);
							jsonGen.writeNumberField("top", imageSnippet.getRectangle().y);
							jsonGen.writeNumberField("width", imageSnippet.getRectangle().width);
							jsonGen.writeNumberField("height", imageSnippet.getRectangle().height);

							jsonGen.writeArrayFieldStart("highlights");
							for (Rectangle highlight : imageSnippet.getHighlights()) {
								jsonGen.writeStartObject();
								jsonGen.writeNumberField("left", highlight.x);
								jsonGen.writeNumberField("top", highlight.y);
								jsonGen.writeNumberField("width", highlight.width);
								jsonGen.writeNumberField("height", highlight.height);
								jsonGen.writeEndObject();
							}
							jsonGen.writeEndArray();
							jsonGen.writeEndObject();
						}
						jsonGen.writeEndArray();
					} // include graphics ?
				} else {
					// exception
					Exception e = result.getRight();
					jsonGen.writeObjectFieldStart("snippetError");
					jsonGen.writeStringField("message", e.getMessage());
					jsonGen.writeStringField("stackTrace", LogUtils.getErrorString(e).replaceAll("[\r\n]+", "<br/>"));
					jsonGen.writeEndObject();
				}
				jsonGen.writeEndObject();
			} // next doc

			jsonGen.writeEndObject();
			jsonGen.flush();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new JochreException(ioe);
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
				if (term.getWeight() >= minWeight)
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
	public String displaySnippet(Snippet snippet) {
		JochreIndexDocument jochreDoc = searchService.getJochreIndexDocument(indexSearcher, snippet.getDocId());
		String content = jochreDoc.getContents();

		if (LOG.isTraceEnabled())
			LOG.trace("Displaying snippet for doc " + snippet.getDocId() + ", snippet " + snippet);

		StringBuilder sb = new StringBuilder();
		int currentPos = snippet.getStartOffset();

		sb.append("<span offset=\"" + currentPos + "\">");
		for (HighlightTerm term : snippet.getHighlightTerms()) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Adding term: " + term.getStartOffset() + ", " + term.getEndOffset());
			}
			String substring = content.substring(currentPos, term.getStartOffset());
			substring = this.addLineBreaks(substring, currentPos);
			sb.append(substring);
			currentPos = term.getStartOffset();

			if (term.getWeight() >= minWeight) {
				String termText = content.substring(term.getStartOffset(), term.getEndOffset());
				sb.append("</span>");
				// note: an end-of-line hyphenated word can contain newlines
				termText = this.addLineBreaks(termText, currentPos);
				termText = "<span offset=\"" + term.getStartOffset() + "\">" + termText + "</span>";
				sb.append(this.getDecorator().decorate(termText));
				currentPos = term.getEndOffset();
				sb.append("<span offset=\"" + currentPos + "\">");
			}
		}
		sb.append(this.addLineBreaks(content.substring(currentPos, snippet.getEndOffset()), currentPos));
		sb.append("</span>");

		if (LOG.isTraceEnabled()) {
			LOG.trace(sb.toString());
		}

		return sb.toString();
	}

	private static final Pattern NEWLINE_PATTERN = Pattern.compile("[" + JochreSearchConstants.INDEX_NEWLINE + JochreSearchConstants.INDEX_PARAGRAPH + "]");

	private String addLineBreaks(String text, int currentPos) {
		StringBuilder sb = new StringBuilder();
		int innerPos = 0;
		Matcher matcher = NEWLINE_PATTERN.matcher(text);
		while (matcher.find()) {
			sb.append(text.substring(innerPos, matcher.start()));
			sb.append("</span>");
			sb.append("<br/>");
			innerPos = matcher.end();
			sb.append("<span offset=\"" + (currentPos + innerPos) + "\">");
			if (LOG.isTraceEnabled())
				LOG.trace("Added linebreak at " + (currentPos + innerPos));
		}
		if (innerPos < text.length())
			sb.append(text.substring(innerPos));
		text = sb.toString();
		return text;
	}

	@Override
	public int getDecimalPlaces() {
		return decimalPlaces;
	}

	@Override
	public void setDecimalPlaces(int decimalPlaces) {
		if (this.decimalPlaces != decimalPlaces) {
			this.decimalPlaces = decimalPlaces;
			String dfFormat = "0.";
			for (int i = 0; i < decimalPlaces; i++) {
				dfFormat += "0";
			}
			df = new DecimalFormat(dfFormat, enSymbols);
		}
	}

	@Override
	public SnippetFinder getSnippetFinder() {
		if (snippetFinder == null) {
			snippetFinder = highlightService.getSnippetFinder(indexSearcher);
		}
		return snippetFinder;
	}

	@Override
	public void setSnippetFinder(SnippetFinder snippetFinder) {
		this.snippetFinder = snippetFinder;
	}

	@Override
	public HighlightTermDecorator getDecorator() {
		if (decorator == null) {
			decorator = new WrappingDecorator("<b>", "</b>");
		}
		return decorator;
	}

	@Override
	public void setDecorator(HighlightTermDecorator decorator) {
		this.decorator = decorator;
	}

	@Override
	public boolean isIncludeText() {
		return includeText;
	}

	@Override
	public void setIncludeText(boolean includeText) {
		this.includeText = includeText;
	}

	@Override
	public boolean isIncludeGraphics() {
		return includeGraphics;
	}

	@Override
	public void setIncludeGraphics(boolean includeGraphics) {
		this.includeGraphics = includeGraphics;
	}

	@Override
	public double getMinWeight() {
		return minWeight;
	}

	@Override
	public void setMinWeight(double minWeight) {
		this.minWeight = minWeight;
	}

	@Override
	public int getTitleSnippetCount() {
		return titleSnippetCount;
	}

	@Override
	public void setTitleSnippetCount(int titleSnippetCount) {
		this.titleSnippetCount = titleSnippetCount;
	}

	@Override
	public int getSnippetCount() {
		return snippetCount;
	}

	@Override
	public void setSnippetCount(int snippetCount) {
		this.snippetCount = snippetCount;
	}

	@Override
	public int getSnippetSize() {
		return snippetSize;
	}

	@Override
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
