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

/**
 * A single snippet within a document, used for displaying the document.
 */
public class Snippet implements Comparable<Snippet> {
	private int docId;
	private String field;
	private int startOffset;
	private int endOffset;
	private boolean scoreCalculated = false;
	private double score;
	private List<HighlightTerm> highlightTerms = new ArrayList<HighlightTerm>();
	
	public Snippet(int docId, String field, int startOffset, int endOffset) {
		super();
		this.docId = docId;
		this.field = field;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
	}
	public int getStartOffset() {
		return startOffset;
	}
	public int getEndOffset() {
		return endOffset;
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
		return this.getDocId()==otherSnippet.getDocId() && this.getField().equals(otherSnippet.getField())
				&& ((this.startOffset<otherSnippet.getEndOffset() && this.endOffset>=otherSnippet.getStartOffset())
						|| (otherSnippet.getStartOffset()<this.endOffset && otherSnippet.getEndOffset()>=this.startOffset));
	}
	
	public void merge(Snippet otherSnippet) {
		if (this.startOffset<otherSnippet.getStartOffset() && this.endOffset<otherSnippet.getEndOffset()) {
			this.endOffset = otherSnippet.endOffset;
		} else if (this.startOffset>otherSnippet.getStartOffset() && this.endOffset>otherSnippet.getEndOffset()) {
			this.startOffset = otherSnippet.getStartOffset();
		}
		Set<HighlightTerm> newTerms = new TreeSet<HighlightTerm>(this.highlightTerms);
		newTerms.addAll(otherSnippet.getHighlightTerms());
		this.highlightTerms = new ArrayList<HighlightTerm>(newTerms);
		this.scoreCalculated = false;
	}
	
	@Override
	public int compareTo(Snippet o) {
		if (this==o)
			return 0;
		
		if (this.score!=o.getScore())
			return o.getScore() > this.score ? 1 : -1;
		
		if (this.getDocId()!=o.getDocId())
			return this.getDocId() - o.getDocId();
		
		if (!this.getField().equals(o.getField()))
			return this.getField().compareTo(o.getField());

		if (this.startOffset!=o.getStartOffset())
			return this.startOffset - o.getStartOffset();
		
		if (this.endOffset!=o.getEndOffset())
			return this.endOffset - o.getEndOffset();
		
		return o.getEndOffset() - this.endOffset;
	}
	
	/**
	 * The highlight terms contained in this snippet.
	 * @return
	 */
	public List<HighlightTerm> getHighlightTerms() {
		return highlightTerms;
	}
	public void setHighlightTerms(List<HighlightTerm> highlightTerms) {
		this.highlightTerms = highlightTerms;
	}
	
	
}
