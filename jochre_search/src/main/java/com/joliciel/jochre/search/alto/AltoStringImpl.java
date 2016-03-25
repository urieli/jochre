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
import java.util.HashSet;
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
	private Set<String> alternatives = new HashSet<String>();
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
	private String style = null;

	private AltoServiceInternal altoService;

	public AltoStringImpl(AltoTextLine textLine, String content, int left, int top, int width, int height) {
		super();
		this.textLine = textLine;
		this.content = content;
		this.rectangle = new Rectangle(left, top, width, height);
		this.index = this.textLine.getStrings().size();
		this.textLine.getStrings().add(this);
		if (whiteSpacePattern.matcher(content).matches() || content.length() == 0 && width > 0)
			this.whiteSpace = true;
		if (punctuationPattern.matcher(content).matches())
			this.punctuation = true;
	}

	@Override
	public String getContent() {
		return content;
	}

	@Override
	public void setContent(String content) {
		this.content = content;
		String contentNoPunct = punctuationPattern.matcher(content).replaceAll("");
		if (contentNoPunct.length() > 0 && !content.equals(contentNoPunct))
			this.alternatives.add(contentNoPunct);
	}

	@Override
	public Set<String> getAlternatives() {
		return alternatives;
	}

	@Override
	public void setAlternatives(Set<String> alternatives) {
		this.alternatives = alternatives;
	}

	@Override
	public AltoTextLine getTextLine() {
		return textLine;
	}

	@Override
	public double getConfidence() {
		return confidence;
	}

	@Override
	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	@Override
	public boolean isHyphen() {
		return hyphen;
	}

	@Override
	public void setHyphen(boolean hyphen) {
		this.hyphen = hyphen;
	}

	@Override
	public boolean isHyphenStart() {
		return hyphenStart;
	}

	@Override
	public void setHyphenStart(boolean hyphenStart) {
		this.hyphenStart = hyphenStart;
	}

	@Override
	public boolean isHyphenEnd() {
		return hyphenEnd;
	}

	@Override
	public void setHyphenEnd(boolean hyphenEnd) {
		this.hyphenEnd = hyphenEnd;
	}

	@Override
	public String getHyphenatedContent() {
		return hyphenatedContent;
	}

	@Override
	public void setHyphenatedContent(String hyphenatedContent) {
		this.hyphenatedContent = hyphenatedContent;
	}

	@Override
	public Rectangle getRectangle() {
		return rectangle;
	}

	@Override
	public void setRectangle(Rectangle rectangle) {
		this.rectangle = rectangle;
	}

	@Override
	public Rectangle getSecondaryRectangle() {
		return secondaryRectangle;
	}

	@Override
	public void setSecondaryRectangle(Rectangle secondaryRectangle) {
		this.secondaryRectangle = secondaryRectangle;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
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
		return "AltoString [content=" + content + ", index=" + index + ", spanStart=" + spanStart + ", spanEnd=" + spanEnd + "]";
	}

	@Override
	public boolean isWhiteSpace() {
		return whiteSpace;
	}

	@Override
	public boolean isPunctuation() {
		return punctuation;
	}

	@Override
	public int getPageIndex() {
		return this.getTextLine().getTextBlock().getPage().getIndex();
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
		if (contentStrings == null) {
			contentStrings = new ArrayList<String>();
			contentStrings.add(this.content);
			this.alternatives.remove(this.content);
			contentStrings.addAll(this.alternatives);
		}
		return contentStrings;
	}

	@Override
	public void setContentStrings(List<String> contentStrings) {
		this.contentStrings = contentStrings;
	}

	@Override
	public boolean mergeWithNext() {
		AltoString nextString = null;
		AltoTextLine nextRow = null;
		boolean endOfRowHyphen = false;
		while (this.index < this.textLine.getStrings().size() - 1) {
			nextString = this.textLine.getStrings().get(this.index + 1);
			this.textLine.getStrings().remove(this.index + 1);
			if (!nextString.isWhiteSpace())
				break;
		}
		if (nextString != null && nextString.isWhiteSpace())
			nextString = null;
		if (nextString == null && index == this.textLine.getStrings().size() - 1
				&& this.textLine.getIndex() + 1 < this.getTextLine().getTextBlock().getPage().getTextLines().size()) {
			nextRow = this.getTextLine().getTextBlock().getPage().getTextLines().get(this.textLine.getIndex() + 1);
			if (this.getTextLine().getTextBlock().getIndex() != nextRow.getTextBlock().getIndex())
				return false;

			while (nextRow.getStrings().size() > 0) {
				nextString = nextRow.getStrings().get(0);
				nextRow.getStrings().remove(0);
				if (!nextString.isWhiteSpace())
					break;
			}
			if (this.content.endsWith("-"))
				endOfRowHyphen = true;
		}
		if (nextString == null)
			return false;

		// merge the rectangles
		if (nextString.getRowIndex() == this.getRowIndex()) {
			this.getRectangle().add(nextString.getRectangle());
		} else {
			if (this.secondaryRectangle != null) {
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
					alternatives.add(contentA.substring(0, contentA.length() - 1) + contentB);
				} else if (contentA.endsWith("'")) {
					alternatives.add(contentA.substring(0, contentA.length() - 1) + contentB);
				} else if (contentA.endsWith("\"")) {
					alternatives.add(contentA.substring(0, contentA.length() - 1) + contentB);
				}
			}
		}

		if (endOfRowHyphen) {
			AltoStringFixer altoStringFixer = this.altoService.getAltoStringFixer();
			if (altoStringFixer != null)
				this.setContent(altoStringFixer.getHyphenatedContent(this.getContent(), nextString.getContent()));
			else
				this.setContent(this.getContent().substring(0, this.getContent().length() - 1) + nextString.getContent());
		} else
			this.setContent(this.getContent() + nextString.getContent());

		alternatives.remove(this.getContent());
		this.setAlternatives(alternatives);
		this.setContentStrings(null);

		this.textLine.recalculate();
		if (nextRow != null)
			nextRow.recalculate();

		return true;
	}

	@Override
	public String getStyle() {
		return style;
	}

	@Override
	public void setStyle(String style) {
		this.style = style;
	}

	public AltoServiceInternal getAltoService() {
		return altoService;
	}

	public void setAltoService(AltoServiceInternal altoService) {
		this.altoService = altoService;
	}
}
