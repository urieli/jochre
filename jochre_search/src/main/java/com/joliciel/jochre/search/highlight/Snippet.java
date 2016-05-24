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
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.joliciel.jochre.search.JochreIndexDocument;
import com.joliciel.jochre.search.JochrePayload;

/**
 * A single snippet within a document, used for displaying the document.
 */
public class Snippet implements Comparable<Snippet> {
	private static final Logger LOG = LoggerFactory.getLogger(Snippet.class);
	private int docId;
	private String field;
	private int startOffset;
	private int endOffset;
	private boolean scoreCalculated = false;
	private double score;
	private List<HighlightTerm> highlightTerms = new ArrayList<HighlightTerm>();
	private Rectangle rect = null;
	private int pageIndex = -1;
	private String text;
	private int startRowIndex = -1;
	private int endRowIndex = -1;

	public Snippet(int docId, String field, int startOffset, int endOffset, int pageIndex) {
		super();
		this.docId = docId;
		this.field = field;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.pageIndex = pageIndex;
	}

	public Snippet(String json) {
		try {
			Reader reader = new StringReader(json);
			JsonFactory jsonFactory = new JsonFactory();
			JsonParser jsonParser = jsonFactory.createParser(reader);
			jsonParser.nextToken();
			this.read(jsonParser);
		} catch (IOException e) {
			LOG.error("Failed to parse json: " + json, e);
			throw new RuntimeException(e);
		}
	}

	public Snippet(JsonParser jsonParser) {
		this.read(jsonParser);
	}

	private void read(JsonParser jsonParser) {
		try {
			if (jsonParser.getCurrentToken() != JsonToken.START_OBJECT)
				throw new RuntimeException("Expected START_OBJECT, but was " + jsonParser.getCurrentToken() + " at " + jsonParser.getCurrentLocation());
			while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
				String fieldName = jsonParser.getCurrentName();

				if (fieldName.equals("docId")) {
					this.docId = jsonParser.nextIntValue(0);
				} else if (fieldName.equals("field")) {
					this.field = jsonParser.nextTextValue();
				} else if (fieldName.equals("start")) {
					this.startOffset = jsonParser.nextIntValue(0);
				} else if (fieldName.equals("end")) {
					this.endOffset = jsonParser.nextIntValue(0);
				} else if (fieldName.equals("score")) {
					jsonParser.nextValue();
					this.score = jsonParser.getDoubleValue();
					this.scoreCalculated = true;
				} else if (fieldName.equals("pageIndex")) {
					this.pageIndex = jsonParser.nextIntValue(-1);
				} else if (fieldName.equals("startRowIndex")) {
					this.startRowIndex = jsonParser.nextIntValue(-1);
				} else if (fieldName.equals("endRowIndex")) {
					this.endRowIndex = jsonParser.nextIntValue(-1);
				} else if (fieldName.equals("text")) {
					this.text = jsonParser.nextTextValue();
				} else if (fieldName.equals("terms")) {
					if (jsonParser.nextToken() != JsonToken.START_ARRAY)
						throw new RuntimeException("Expected START_ARRAY, but was " + jsonParser.getCurrentToken() + " at " + jsonParser.getCurrentLocation());
					while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
						if (jsonParser.getCurrentToken() != JsonToken.START_OBJECT)
							throw new RuntimeException(
									"Expected START_OBJECT, but was " + jsonParser.getCurrentToken() + " at " + jsonParser.getCurrentLocation());
						int termDocId = docId;
						String termField = field;
						int termStart = 0;
						int termEnd = 0;
						int pageIndex = 0;
						int paragraphIndex = 0;
						int rowIndex = 0;
						int left = 0;
						int top = 0;
						int width = 0;
						int height = 0;
						int left2 = -1;
						int top2 = -1;
						int width2 = -1;
						int height2 = -1;

						double weight = 0.0;
						while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
							String termFieldName = jsonParser.getCurrentName();
							if (termFieldName.equals("docId")) {
								termDocId = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("field")) {
								termField = jsonParser.nextTextValue();
							} else if (termFieldName.equals("start")) {
								termStart = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("end")) {
								termEnd = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("pageIndex")) {
								pageIndex = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("paragraphIndex")) {
								paragraphIndex = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("rowIndex")) {
								rowIndex = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("left")) {
								left = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("top")) {
								top = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("width")) {
								width = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("height")) {
								height = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("left2")) {
								left2 = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("top2")) {
								top2 = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("width2")) {
								width2 = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("height2")) {
								height2 = jsonParser.nextIntValue(0);
							} else if (termFieldName.equals("weight")) {
								jsonParser.nextValue();
								weight = jsonParser.getDoubleValue();
							} else {
								throw new RuntimeException("Unexpected term field name: " + termFieldName + " at " + jsonParser.getCurrentLocation());
							}
						}
						Rectangle rect = new Rectangle(left, top, width, height);
						Rectangle secondaryRect = null;
						if (left2 >= 0)
							secondaryRect = new Rectangle(left2, top2, width2, height2);
						JochrePayload payload = new JochrePayload(rect, secondaryRect, pageIndex, paragraphIndex, rowIndex);
						HighlightTerm highlightTerm = new HighlightTerm(termDocId, termField, termStart, termEnd, payload);
						highlightTerm.setWeight(weight);
						this.highlightTerms.add(highlightTerm);
					}
				} else {
					throw new RuntimeException("Unexpected field name: " + fieldName + " at " + jsonParser.getCurrentLocation());
				}
			}
		} catch (JsonParseException e) {
			LOG.error("Failed to parse json", e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LOG.error("Failed to parse json", e);
			throw new RuntimeException(e);
		}
	}

	public String toJson() {
		try {
			DecimalFormatSymbols enSymbols = new DecimalFormatSymbols(Locale.US);
			DecimalFormat df = new DecimalFormat("0.00", enSymbols);
			StringWriter writer = new StringWriter();
			JsonFactory jsonFactory = new JsonFactory();
			JsonGenerator jsonGen = jsonFactory.createGenerator(writer);
			this.toJson(jsonGen, df);
			writer.flush();
			String json = writer.toString();
			return json;
		} catch (IOException e) {
			LOG.error("Failed to write json", e);
			throw new RuntimeException(e);
		}
	}

	public void toJson(JsonGenerator jsonGen, DecimalFormat df) {
		this.toJson(jsonGen, df, null);
	}

	public void toJson(JsonGenerator jsonGen, DecimalFormat df, HighlightManager highlightManager) {
		try {
			jsonGen.writeStartObject();
			jsonGen.writeNumberField("docId", docId);
			jsonGen.writeStringField("field", this.getField());
			jsonGen.writeNumberField("start", this.getStartOffset());
			jsonGen.writeNumberField("end", this.getEndOffset());
			double roundedScore = df.parse(df.format(this.getScore())).doubleValue();
			jsonGen.writeNumberField("score", roundedScore);
			jsonGen.writeNumberField("pageIndex", this.getPageIndex());
			jsonGen.writeNumberField("startRowIndex", this.getStartRowIndex());
			jsonGen.writeNumberField("endRowIndex", this.getEndRowIndex());
			jsonGen.writeArrayFieldStart("terms");
			for (HighlightTerm term : this.getHighlightTerms()) {
				term.toJson(jsonGen, df);
			}
			jsonGen.writeEndArray(); // terms

			if (this.text != null) {
				jsonGen.writeStringField("text", text);
			} else if (highlightManager != null) {
				String text = highlightManager.displaySnippet(this);
				jsonGen.writeStringField("text", text);
			}

			jsonGen.writeEndObject();
			jsonGen.flush();
		} catch (java.text.ParseException e) {
			LOG.error("Failed to parse json", e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LOG.error("Failed to parse json", e);
			throw new RuntimeException(e);
		}
	}

	public int getStartOffset() {
		return startOffset;
	}

	public int getEndOffset() {
		return endOffset;
	}

	public void setStartOffset(int startOffset) {
		this.startOffset = startOffset;
	}

	public void setEndOffset(int endOffset) {
		this.endOffset = endOffset;
	}

	public int getDocId() {
		return docId;
	}

	public String getField() {
		return field;
	}

	public double getScore() {
		if (!scoreCalculated) {
			score = 0;
			for (HighlightTerm term : this.highlightTerms) {
				score += term.getWeight();
			}
			scoreCalculated = true;
		}
		return score;
	}

	public boolean hasOverlap(Snippet otherSnippet) {
		return this.getDocId() == otherSnippet.getDocId() && this.getField().equals(otherSnippet.getField())
				&& ((this.startOffset < otherSnippet.getEndOffset() && this.endOffset >= otherSnippet.getStartOffset())
						|| (otherSnippet.getStartOffset() < this.endOffset && otherSnippet.getEndOffset() >= this.startOffset));
	}

	public void merge(Snippet otherSnippet) {
		if (this.startOffset < otherSnippet.getStartOffset() && this.endOffset < otherSnippet.getEndOffset()) {
			this.endOffset = otherSnippet.endOffset;
		} else if (this.startOffset > otherSnippet.getStartOffset() && this.endOffset > otherSnippet.getEndOffset()) {
			this.startOffset = otherSnippet.getStartOffset();
		}
		Set<HighlightTerm> newTerms = new TreeSet<HighlightTerm>(this.highlightTerms);
		newTerms.addAll(otherSnippet.getHighlightTerms());
		this.highlightTerms = new ArrayList<HighlightTerm>(newTerms);
		this.scoreCalculated = false;
	}

	@Override
	public int compareTo(Snippet o) {
		if (this == o)
			return 0;

		if (this.getScore() != o.getScore())
			return o.getScore() > this.getScore() ? 1 : -1;

		if (this.getDocId() != o.getDocId())
			return this.getDocId() - o.getDocId();

		if (!this.getField().equals(o.getField()))
			return this.getField().compareTo(o.getField());

		if (this.getPageIndex() != o.getPageIndex())
			return this.getPageIndex() - o.getPageIndex();

		if (this.startOffset != o.getStartOffset())
			return this.startOffset - o.getStartOffset();

		if (this.endOffset != o.getEndOffset())
			return this.endOffset - o.getEndOffset();

		return o.getEndOffset() - this.endOffset;
	}

	/**
	 * The highlight terms contained in this snippet.
	 */
	public List<HighlightTerm> getHighlightTerms() {
		return highlightTerms;
	}

	public void setHighlightTerms(List<HighlightTerm> highlightTerms) {
		this.highlightTerms = highlightTerms;
	}

	public int getPageIndex() {
		if (pageIndex < 0 && this.highlightTerms.size() > 0)
			pageIndex = this.highlightTerms.get(0).getPayload().getPageIndex();
		return pageIndex;
	}

	public Rectangle getRectangle(JochreIndexDocument jochreDoc) {
		if (this.rect == null) {
			int startRowIndex = this.startRowIndex;
			int endRowIndex = this.endRowIndex;
			if (startRowIndex < 0 || endRowIndex < 0) {
				if (this.highlightTerms.size() > 0) {
					startRowIndex = this.highlightTerms.get(0).getPayload().getRowIndex() - 1;
					endRowIndex = this.highlightTerms.get(this.highlightTerms.size() - 1).getPayload().getRowIndex();
					if (this.highlightTerms.get(this.highlightTerms.size() - 1).getPayload().getSecondaryRectangle() != null)
						endRowIndex += 2;
					else
						endRowIndex += 1;
					if (startRowIndex < 0)
						startRowIndex = 0;
					if (endRowIndex >= jochreDoc.getRowCount(pageIndex))
						endRowIndex = jochreDoc.getRowCount(pageIndex) - 1;
				} else {
					startRowIndex = 0;
					endRowIndex = 0;
				}
			}

			LOG.debug("Getting rectangle for snippet with terms " + this.highlightTerms.toString());
			if (LOG.isTraceEnabled()) {
				for (HighlightTerm term : this.highlightTerms) {
					LOG.trace(term.toString() + ", " + term.getPayload().toString());
				}
			}
			LOG.debug("pageIndex: " + pageIndex + ", startRowIndex: " + startRowIndex + ", endRowIndex: " + endRowIndex);
			rect = new Rectangle(jochreDoc.getRowRectangle(pageIndex, startRowIndex));
			LOG.debug("Start row rect: " + rect);
			for (int i = startRowIndex + 1; i <= endRowIndex; i++) {
				Rectangle otherRect = jochreDoc.getRowRectangle(pageIndex, i);
				LOG.debug("Expanding by end row rect: " + otherRect);
				rect.add(otherRect);
			}
		}
		return rect;
	}

	@Override
	public String toString() {
		return "Snippet [docId=" + docId + ", field=" + field + ", startOffset=" + startOffset + ", endOffset=" + endOffset + ", score=" + score
				+ ", highlightTerms=" + highlightTerms + ", pageIndex=" + pageIndex + ", startRowIndex=" + startRowIndex + ", endRowIndex=" + endRowIndex + "]";
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void generateText(HighlightManager highlightManager) {
		this.text = highlightManager.displaySnippet(this);
	}

	public int getStartRowIndex() {
		return startRowIndex;
	}

	public void setStartRowIndex(int startRowIndex) {
		this.startRowIndex = startRowIndex;
	}

	public int getEndRowIndex() {
		return endRowIndex;
	}

	public void setEndRowIndex(int endRowIndex) {
		this.endRowIndex = endRowIndex;
	}
}
