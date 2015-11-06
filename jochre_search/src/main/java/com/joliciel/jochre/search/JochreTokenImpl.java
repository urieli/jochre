package com.joliciel.jochre.search;

import java.util.ArrayList;
import java.util.List;

class JochreTokenImpl implements JochreToken {
	private int left, top, width, height;
	private List<String> contentStrings = null;
	private List<String> hyphenatedContentStrings = null;
	private int index = -1;
	private int pageIndex = -1;
	private int paragraphIndex = -1;
	private int rowIndex = -1;
	private int spanStart = -1;
	private int spanEnd = -1;
	
	public JochreTokenImpl(JochreToken jochreToken) {
		this.left = jochreToken.getLeft();
		this.top = jochreToken.getTop();
		this.width = jochreToken.getWidth();
		this.height = jochreToken.getHeight();
		this.index = jochreToken.getIndex();
		this.spanStart = jochreToken.getSpanStart();
		this.spanEnd = jochreToken.getSpanEnd();
		this.contentStrings = new ArrayList<String>(jochreToken.getContentStrings());
		this.hyphenatedContentStrings = new ArrayList<String>(jochreToken.getHyphenatedContentStrings());
		this.pageIndex = jochreToken.getPageIndex();
		this.paragraphIndex = jochreToken.getParagraphIndex();
		this.rowIndex = jochreToken.getRowIndex();
	}
	
	public JochreTokenImpl(String text) {
		this.contentStrings = new ArrayList<String>(1);
		contentStrings.add(text);
		this.hyphenatedContentStrings = new ArrayList<String>(0);
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

	public List<String> getContentStrings() {
		return contentStrings;
	}

	public List<String> getHyphenatedContentStrings() {
		return hyphenatedContentStrings;
	}

	public void setContentStrings(List<String> contentStrings) {
		this.contentStrings = contentStrings;
	}

	public void setHyphenatedContentStrings(List<String> hyphenatedContentStrings) {
		this.hyphenatedContentStrings = hyphenatedContentStrings;
	}

	public int getIndex() {
		return index;
	}

	public int getPageIndex() {
		return pageIndex;
	}

	public int getParagraphIndex() {
		return paragraphIndex;
	}

	public int getRowIndex() {
		return rowIndex;
	}

	public int getSpanStart() {
		return spanStart;
	}

	public int getSpanEnd() {
		return spanEnd;
	}

	public void setSpanStart(int spanStart) {
		this.spanStart = spanStart;
	}

	public void setSpanEnd(int spanEnd) {
		this.spanEnd = spanEnd;
	}

}
