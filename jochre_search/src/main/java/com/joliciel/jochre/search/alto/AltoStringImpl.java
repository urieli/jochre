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
package com.joliciel.jochre.search.alto;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

class AltoStringImpl implements AltoString {
	private static Pattern whiteSpacePattern = Pattern.compile("[\\s\ufeff]+", Pattern.UNICODE_CHARACTER_CLASS);
	private static Pattern punctuationPattern = Pattern.compile("\\p{Punct}+", Pattern.UNICODE_CHARACTER_CLASS);
	private String content;
	private Rectangle rectangle;
	private Rectangle secondaryRectangle;
	private AltoTextLine textLine;
	private List<String> alternatives = new ArrayList<String>();
	private List<String> contentStrings = null;
	private double confidence;
	private boolean hyphen = false;
	private boolean hyphenStart = false;
	private boolean hyphenEnd = false;
	private String hyphenatedContent = null;
	private int index = -1;
	private int spanStart = -1;
	private int spanEnd = -1;
	private boolean whiteSpace = false;
	private boolean punctuation = false;
	
	public AltoStringImpl(AltoTextLine textLine, String content, int left, int top, int width, int height) {
		super();
		this.textLine = textLine;
		this.content = content;
		this.rectangle = new Rectangle(left, top, width, height);
		this.index = this.textLine.getStrings().size();
		this.textLine.getStrings().add(this);
		if (whiteSpacePattern.matcher(content).matches()||content.length()==0&&width>0)
			this.whiteSpace = true;
		if (punctuationPattern.matcher(content).matches())
			this.punctuation = true;
	}
	
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public List<String> getAlternatives() {
		return alternatives;
	}
	
	public void setAlternatives(List<String> alternatives) {
		this.alternatives = alternatives;
	}

	public AltoTextLine getTextLine() {
		return textLine;
	}
	public double getConfidence() {
		return confidence;
	}
	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}
	
	public boolean isHyphen() {
		return hyphen;
	}
	public void setHyphen(boolean hyphen) {
		this.hyphen = hyphen;
	}

	public boolean isHyphenStart() {
		return hyphenStart;
	}
	public void setHyphenStart(boolean hyphenStart) {
		this.hyphenStart = hyphenStart;
	}
	public boolean isHyphenEnd() {
		return hyphenEnd;
	}
	public void setHyphenEnd(boolean hyphenEnd) {
		this.hyphenEnd = hyphenEnd;
	}
	public String getHyphenatedContent() {
		return hyphenatedContent;
	}
	public void setHyphenatedContent(String hyphenatedContent) {
		this.hyphenatedContent = hyphenatedContent;
	}

	@Override
	public Rectangle getRectangle() {
		return rectangle;
	}

	public void setRectangle(Rectangle rectangle) {
		this.rectangle = rectangle;
	}

	public Rectangle getSecondaryRectangle() {
		return secondaryRectangle;
	}

	public void setSecondaryRectangle(Rectangle secondaryRectangle) {
		this.secondaryRectangle = secondaryRectangle;
	}

	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public int getSpanStart() {
		return spanStart;
	}

	@Override
	public void setSpanStart(int spanStart) {
		this.spanStart = spanStart;
	}

	@Override
	public int getSpanEnd() {
		return spanEnd;
	}

	@Override
	public void setSpanEnd(int spanEnd) {
		this.spanEnd = spanEnd;
	}

	@Override
	public String toString() {
		return "AltoString [content=" + content + ", index=" + index
				+ ", spanStart=" + spanStart + ", spanEnd=" + spanEnd + "]";
	}

	public boolean isWhiteSpace() {
		return whiteSpace;
	}

	public boolean isPunctuation() {
		return punctuation;
	}

	@Override
	public int getPageIndex() {
		return this.getTextLine().getTextBlock().getPage().getPageIndex();
	}

	@Override
	public int getParagraphIndex() {
		return this.getTextLine().getTextBlock().getIndex();
	}

	@Override
	public int getRowIndex() {
		return this.getTextLine().getIndex();
	}

	@Override
	public List<String> getContentStrings() {
		if (contentStrings==null) {
			contentStrings = new ArrayList<String>();
			contentStrings.add(this.content);
			contentStrings.addAll(this.alternatives);
		}
		return contentStrings;
	}

	public void setContentStrings(List<String> contentStrings) {
		this.contentStrings = contentStrings;
	}

	@Override
	public boolean mergeWithNext() {
		AltoString nextString = null;
		AltoTextLine nextRow = null;
		boolean endOfRowHyphen = false;
		if (this.index<this.textLine.getStrings().size()-1) {
			nextString = this.textLine.getStrings().get(this.index+1);
			if (nextString.isWhiteSpace())
				return false;
			this.textLine.getStrings().remove(this.index+1);
		}
		if (nextString==null && index==this.textLine.getStrings().size()-1 && this.textLine.getIndex()+1<this.getTextLine().getTextBlock().getTextLines().size()) {
			nextRow = this.getTextLine().getTextBlock().getTextLines().get(this.textLine.getIndex()+1);
			if (nextRow.getStrings().size()>0) {
				nextString = nextRow.getStrings().get(0);
				if (nextString.isWhiteSpace())
					return false;
				nextRow.getStrings().remove(0);
			}
			if (this.content.endsWith("-"))
				endOfRowHyphen = true;
		}
		
		// merge the rectangles
		if (nextString.getRowIndex()==this.getRowIndex()) {
			this.getRectangle().add(nextString.getRectangle());
		} else {
			if (this.secondaryRectangle!=null) {
				this.secondaryRectangle.add(nextString.getRectangle());
			} else {
				this.secondaryRectangle = nextString.getRectangle();
			}
		}
		this.setSpanEnd(nextString.getSpanEnd());
		
		Set<String> alternatives = new TreeSet<String>();
		for (String contentA : this.getContentStrings()) {
			for (String contentB : nextString.getContentStrings()) {
				alternatives.add(contentA + contentB);
				if (endOfRowHyphen && contentA.endsWith("-")) {
					alternatives.add(contentA.substring(0, contentA.length()-1) + contentB);
				} else if (contentA.endsWith("'")) {
					alternatives.add(contentA.substring(0, contentA.length()-1) + contentB);
				} else if (contentA.endsWith("\"")) {
					alternatives.add(contentA.substring(0, contentA.length()-1) + contentB);
				}
			}
		}
		
		if (endOfRowHyphen)
			this.setContent(this.getContent().substring(0, this.getContent().length()-1) + nextString.getContent());
		else
			this.setContent(this.getContent() + nextString.getContent());
		
		alternatives.remove(this.getContent());
		this.setAlternatives(new ArrayList<String>(alternatives));
		this.setContentStrings(null);
		
		this.textLine.recalculate();
		if (nextRow!=null)
			nextRow.recalculate();
		
		return true;
	}

}
