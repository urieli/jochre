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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

class AltoStringImpl implements AltoString {
	private static Pattern whiteSpacePattern = Pattern.compile("[\\s\ufeff]+", Pattern.UNICODE_CHARACTER_CLASS);
	private String content;
	private int left, top, width, height;
	private AltoTextLine textLine;
	private List<String> alternatives = new ArrayList<String>();
	private double confidence;
	private boolean hyphenStart = false;
	private boolean hyphenEnd = false;
	private String hyphenatedContent = null;
	private int index = -1;
	private int spanStart = -1;
	private int spanEnd = -1;
	private boolean whiteSpace = false;
	
	public AltoStringImpl(AltoTextLine textLine, String content, int left, int top, int width, int height) {
		super();
		this.textLine = textLine;
		this.content = content;
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
		this.index = this.textLine.getStrings().size();
		this.textLine.getStrings().add(this);
		if (whiteSpacePattern.matcher(content).matches())
			this.whiteSpace = true;
	}
	
	public String getContent() {
		return content;
	}

	public int getLeft() {
		return left;
	}

	public int getTop() {
		return top;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public List<String> getAlternatives() {
		return alternatives;
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

	public int getIndex() {
		return index;
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

}
