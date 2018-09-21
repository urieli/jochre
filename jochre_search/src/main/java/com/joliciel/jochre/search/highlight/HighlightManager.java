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

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.joliciel.jochre.search.JochreIndexDocument;
import com.joliciel.jochre.search.JochreIndexField;
import com.joliciel.jochre.search.JochreSearchConfig;
import com.joliciel.jochre.search.JochreSearchConstants;
import com.joliciel.jochre.utils.Either;
import com.joliciel.talismane.utils.LogUtils;

public class HighlightManager {
	private static final Logger LOG = LoggerFactory.getLogger(HighlightManager.class);

	private final IndexSearcher indexSearcher;
	private final JochreSearchConfig config;

	private int decimalPlaces = 2;
	private DecimalFormatSymbols enSymbols = new DecimalFormatSymbols(Locale.US);
	private DecimalFormat df = new DecimalFormat("0.00", enSymbols);
	private boolean includeText = false;
	private boolean includeGraphics = false;
	private double minWeight = 0;
	private int snippetCount;

	public HighlightManager(IndexSearcher indexSearcher, JochreSearchConfig config) {
		this.config = config;
		this.indexSearcher = indexSearcher;
		this.snippetCount = config.getConfig().getInt("snippet-finder.snippet-count");
	}

	/**
	 * Write a JSON output of terms to highlight for a given set of docIds.
	 * 
	 * @param highlighter
	 *            The highlighter to be used to find the terms to highlight.
	 * @param docIds
	 *            The documents to highlight.
	 * @param out
	 *            The writer where the JSON should be written.
	 * @throws IOException
	 */

	public void highlight(Highlighter highlighter, Set<Integer> docIds, Set<String> fields, Writer out) throws IOException {
		Map<Integer, NavigableSet<HighlightTerm>> termMap = highlighter.highlight(docIds, fields);
		JsonFactory jsonFactory = new JsonFactory();
		JsonGenerator jsonGen = jsonFactory.createGenerator(out);

		jsonGen.writeStartObject();

		for (int docId : docIds) {
			Document doc = indexSearcher.doc(docId);
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
	}

	/**
	 * Find highlight snippets for a set of documents. Each document either returns
	 * a list of snippets or an exception.
	 * 
	 * @param docIds
	 *            The documents to highlight and snip.
	 * @param termMap
	 *            The previously retrieved terms.
	 * @param maxSnippets
	 *            The maximum number of snippets.
	 */
	public Map<Integer, Either<List<Snippet>, Exception>> findSnippets(Set<Integer> docIds, Set<String> fields,
			Map<Integer, NavigableSet<HighlightTerm>> termMap, int maxSnippets) {
		SnippetFinder snippetFinder = SnippetFinder.getInstance(config);

		Map<Integer, Either<List<Snippet>, Exception>> snippetMap = new HashMap<>();

		for (int docId : docIds) {
			Set<HighlightTerm> terms = termMap.get(docId);
			Either<List<Snippet>, Exception> result = null;
			try {
				List<Snippet> snippets = snippetFinder.findSnippets(indexSearcher, docId, fields, terms, maxSnippets);
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

	/**
	 * Write a JSON output of snippets for a given set of docIds.
	 * 
	 * @param highlighter
	 *            The highlighter to be used to find the terms to highlight.
	 * @param docIds
	 *            The documents to highlight and snip.
	 * @param out
	 *            The writer where the JSON should be written.
	 * @throws IOException
	 */
	public void findSnippets(Highlighter highlighter, Set<Integer> docIds, Set<String> fields, Writer out) throws IOException {
		Map<Integer, NavigableSet<HighlightTerm>> termMap = highlighter.highlight(docIds, fields);
		Map<Integer, Either<List<Snippet>, Exception>> snippetMap = this.findSnippets(docIds, fields, termMap, this.snippetCount);

		JsonFactory jsonFactory = new JsonFactory();
		JsonGenerator jsonGen = jsonFactory.createGenerator(out);

		jsonGen.writeStartObject();

		for (int docId : docIds) {
			Document doc = indexSearcher.doc(docId);
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
	}

	/**
	 * Get a textual representation of a document with terms highlighted.
	 * 
	 * @throws IOException
	 * 
	 */
	public String displayHighlights(int docId, String field, Set<HighlightTerm> terms) throws IOException {
		JochreIndexDocument jochreDoc = new JochreIndexDocument(indexSearcher, docId, config);
		String content = jochreDoc.getContents();
		if (LOG.isTraceEnabled()) {
			LOG.trace("Displaying highlights for doc " + docId + ", field " + field);
		}

		HighlightTermDecorator decorator = HighlightTermDecorator.getInstance(config);

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
					sb.append(decorator.decorate(termText));
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

	/**
	 * Get a textual representation of the snippet.
	 * 
	 * @throws IOException
	 */
	public String displaySnippet(Snippet snippet) throws IOException {
		JochreIndexDocument jochreDoc = new JochreIndexDocument(indexSearcher, snippet.getDocId(), config);
		String content = jochreDoc.getContents();

		if (LOG.isTraceEnabled())
			LOG.trace("Displaying snippet for doc " + snippet.getDocId() + ", snippet " + snippet);

		StringBuilder sb = new StringBuilder();
		int currentPos = snippet.getStartOffset();

		HighlightTermDecorator decorator = HighlightTermDecorator.getInstance(config);
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
				sb.append(decorator.decorate(termText));
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

	/**
	 * How many decimal places to write in the JSON.
	 */
	public int getDecimalPlaces() {
		return decimalPlaces;
	}

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

	/**
	 * Whether or not the text should be included in the JSON output, along with the
	 * offsets.
	 */
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

	/**
	 * Maximum snippets to return.
	 */
	public int getSnippetCount() {
		return snippetCount;
	}

	public void setSnippetCount(int snippetCount) {
		this.snippetCount = snippetCount;
	}

	/**
	 * The minimum weight to display a highlighted term.
	 */
	public double getMinWeight() {
		return minWeight;
	}

	public void setMinWeight(double minWeight) {
		this.minWeight = minWeight;
	}

	public ImageSnippet getImageSnippet(Snippet snippet) throws IOException {
		JochreIndexDocument jochreDoc = new JochreIndexDocument(indexSearcher, snippet.getDocId(), config);
		ImageSnippet imageSnippet = new ImageSnippet(jochreDoc, snippet);
		return imageSnippet;
	}

}
