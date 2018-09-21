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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.joliciel.jochre.search.JochrePayload;

/**
 * A single highlighted term within a document.
 * 
 * @author Assaf Urieli
 *
 */
public class HighlightTerm implements Comparable<HighlightTerm> {
	private static final Logger LOG = LoggerFactory.getLogger(HighlightTerm.class);
	private int startOffset;
	private int endOffset;
	private double weight;
	private int docId;
	private String field;
	private JochrePayload payload;
	private int position = -1;
	private boolean inPhrase = false;

	public HighlightTerm(int docId, String field, int startOffset, int endOffset, JochrePayload payload) {
		super();
		this.docId = docId;
		this.field = field;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.payload = payload;
	}

	/**
	 * The document id containing this term.
	 */
	public int getDocId() {
		return docId;
	}

	/**
	 * The field containing this term.
	 */
	public String getField() {
		return field;
	}

	/**
	 * The start offset of this term.
	 */
	public int getStartOffset() {
		return startOffset;
	}

	/**
	 * The end offset of this term.
	 */
	public int getEndOffset() {
		return endOffset;
	}

	/**
	 * This term's weight.
	 */
	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	@Override
	public int compareTo(HighlightTerm o) {
		if (this == o)
			return 0;

		if (this.getDocId() != o.getDocId())
			return this.getDocId() - o.getDocId();

		if (!this.getField().equals(o.getField()))
			return this.getField().compareTo(o.getField());

		if (this.startOffset != o.getStartOffset())
			return this.startOffset - o.getStartOffset();

		if (this.endOffset != o.getEndOffset())
			return this.endOffset - o.getEndOffset();

		return 0;
	}

	@Override
	public String toString() {
		return "HighlightTerm [startOffset=" + startOffset + ", endOffset=" + endOffset + ", weight=" + weight + ", docId=" + docId + ", field=" + field
				+ ", position=" + position + "]";
	}

	public void toJson(JsonGenerator jsonGen, DecimalFormat df) {
		try {
			jsonGen.writeStartObject();
			jsonGen.writeStringField("field", this.getField());
			jsonGen.writeNumberField("start", this.getStartOffset());
			jsonGen.writeNumberField("end", this.getEndOffset());
			Rectangle rect = this.getPayload().getRectangle();
			jsonGen.writeNumberField("left", rect.x);
			jsonGen.writeNumberField("top", rect.y);
			jsonGen.writeNumberField("width", rect.width);
			jsonGen.writeNumberField("height", rect.height);
			Rectangle secondaryRect = this.getPayload().getSecondaryRectangle();
			if (secondaryRect != null) {
				jsonGen.writeNumberField("left2", secondaryRect.x);
				jsonGen.writeNumberField("top2", secondaryRect.y);
				jsonGen.writeNumberField("width2", secondaryRect.width);
				jsonGen.writeNumberField("height2", secondaryRect.height);
			}
			jsonGen.writeNumberField("pageIndex", this.getPayload().getPageIndex());
			jsonGen.writeNumberField("paragraphIndex", this.getPayload().getParagraphIndex());
			jsonGen.writeNumberField("rowIndex", this.getPayload().getRowIndex());
			double roundedWeight = df.parse(df.format(this.getWeight())).doubleValue();
			jsonGen.writeNumberField("weight", roundedWeight);
			jsonGen.writeEndObject();

			jsonGen.flush();
		} catch (java.text.ParseException e) {
			LOG.error("Failed write highlightTerm to JSON in docId " + docId, e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LOG.error("Failed write highlightTerm to JSON in docId " + docId, e);
			throw new RuntimeException(e);
		}
	}

	public JochrePayload getPayload() {
		return payload;
	}

	/**
	 * The term's position within the search index, starts at 0 for each document
	 * field.
	 */
	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	/**
	 * Is the current highlight term inside a phrase, that is, between quotes in the
	 * search query.
	 */
	public boolean isInPhrase() {
		return inPhrase;
	}

	public void setInPhrase(boolean inPhrase) {
		this.inPhrase = inPhrase;
	}

	/**
	 * Do the two terms overlap on any character indexes.
	 */
	public boolean hasOverlap(HighlightTerm otherTerm) {
		// note: if this.startOffset==otherTerm.endOffset, there's no overlap,
		// since the end offset is AFTER the term.
		return this.getDocId() == otherTerm.getDocId() && ((this.startOffset < otherTerm.getEndOffset() && this.endOffset > otherTerm.getStartOffset())
				|| (otherTerm.getStartOffset() < this.endOffset && otherTerm.getEndOffset() > this.startOffset));
	}

	/**
	 * If any highlight terms overlap, combine them into a single term that spans
	 * all overlaps.
	 */
	public static Set<HighlightTerm> combineOverlaps(Set<HighlightTerm> terms) {
		Set<HighlightTerm> fixedTerms = new TreeSet<>();
		HighlightTerm previousTerm = null;
		List<TreeSet<HighlightTerm>> setsToCombine = new ArrayList<>();
		TreeSet<HighlightTerm> combinedSet = null;
		Set<HighlightTerm> termsToCombine = new TreeSet<>();
		int endOffset = -1;
		for (HighlightTerm term : terms) {
			if (previousTerm != null) {
				if (term.getDocId() == previousTerm.getDocId() && term.getStartOffset() < endOffset) {
					if (combinedSet == null) {
						combinedSet = new TreeSet<>();
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

		if (termsToCombine.size() > 0) {
			fixedTerms.addAll(terms);
			fixedTerms.removeAll(termsToCombine);

			for (TreeSet<HighlightTerm> setToCombine : setsToCombine) {
				HighlightTerm firstTerm = setToCombine.first();
				HighlightTerm lastTerm = setToCombine.last();
				HighlightTerm combinedTerm = new HighlightTerm(firstTerm.getDocId(), firstTerm.getField(), firstTerm.getStartOffset(), lastTerm.getEndOffset(),
						firstTerm.getPayload());
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
