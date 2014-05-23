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

/**
 * A single highlighted term within a document.
 * @author Assaf Urieli
 *
 */
public class HighlightTerm implements Comparable<HighlightTerm> {
	private int startOffset;
	private int endOffset;
	private double weight;
	private int docId;
	private String field;
	private int pageIndex;

	public HighlightTerm(int docId, String field, int startOffset, int endOffset, int pageIndex) {
		super();
		this.docId = docId;
		this.field = field;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.pageIndex = pageIndex;
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

	public int getPageIndex() {
		return pageIndex;
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
		
		return this.hashCode() - o.hashCode();
	}

	@Override
	public String toString() {
		return "HighlightTerm [startOffset=" + startOffset + ", endOffset="
				+ endOffset + ", weight=" + weight + ", docId=" + docId
				+ ", field=" + field + ", pageIndex=" + pageIndex + "]";
	}
	
	
}
