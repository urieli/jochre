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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.joliciel.jochre.search.JochrePayload;
import com.joliciel.talismane.utils.LogUtils;

/**
 * A single highlighted term within a document.
 * @author Assaf Urieli
 *
 */
public class HighlightTerm implements Comparable<HighlightTerm> {
	private static final Log LOG = LogFactory.getLog(HighlightTerm.class);
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
	 * @return
	 */
	public int getDocId() {
		return docId;
	}
	
	/**
	 * The field containing this term.
	 * @return
	 */
	public String getField() {
		return field;
	}

	/**
	 * The start offset of this term.
	 * @return
	 */
	public int getStartOffset() {
		return startOffset;
	}
	
	/**
	 * The end offset of this term.
	 * @return
	 */
	public int getEndOffset() {
		return endOffset;
	}
	
	/**
	 * This term's weight.
	 * @return
	 */
	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	@Override
	public int compareTo(HighlightTerm o) {
		if (this==o)
			return 0;
		
		if (this.getDocId()!=o.getDocId())
			return this.getDocId() - o.getDocId();
		
		if (!this.getField().equals(o.getField()))
			return this.getField().compareTo(o.getField());

		if (this.startOffset!=o.getStartOffset())
			return this.startOffset - o.getStartOffset();
		
		if (this.endOffset!=o.getEndOffset())
			return this.endOffset - o.getEndOffset();
		
		return 0;
	}

	@Override
	public String toString() {
		return "HighlightTerm [startOffset=" + startOffset + ", endOffset="
				+ endOffset + ", weight=" + weight + ", docId=" + docId
				+ ", field=" + field + ", position=" + position + "]";
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
			if (secondaryRect!=null) {
				jsonGen.writeNumberField("left2", rect.x);
				jsonGen.writeNumberField("top2", rect.y);
				jsonGen.writeNumberField("width2", rect.width);
				jsonGen.writeNumberField("height2", rect.height);
			}
			jsonGen.writeNumberField("pageIndex", this.getPayload().getPageIndex());
			jsonGen.writeNumberField("textBlockIndex", this.getPayload().getTextBlockIndex());
			jsonGen.writeNumberField("textLineIndex", this.getPayload().getTextLineIndex());
			double roundedWeight = df.parse(df.format(this.getWeight())).doubleValue();
			jsonGen.writeNumberField("weight", roundedWeight);
			jsonGen.writeEndObject();

			jsonGen.flush();
		} catch (java.text.ParseException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	public JochrePayload getPayload() {
		return payload;
	}

	/**
	 * The term's position within the search index,
	 * starts at 0 for each document field.
	 * @return
	 */
	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}
	
	/**
	 * Is the current highlight term inside a phrase,
	 * that is, between quotes in the search query.
	 * @return
	 */
	public boolean isInPhrase() {
		return inPhrase;
	}

	public void setInPhrase(boolean inPhrase) {
		this.inPhrase = inPhrase;
	}

	/**
	 * Do the two terms overlap on any character indexes.
	 * @param otherTerm
	 * @return
	 */
	public boolean hasOverlap(HighlightTerm otherTerm) {
		// note: if this.startOffset==otherTerm.endOffset, there's no overlap, since the end offset is AFTER the term.
		return this.getDocId()==otherTerm.getDocId()
				&& ((this.startOffset<otherTerm.getEndOffset() && this.endOffset>otherTerm.getStartOffset())
						|| (otherTerm.getStartOffset()<this.endOffset && otherTerm.getEndOffset()>this.startOffset));
	}
}
