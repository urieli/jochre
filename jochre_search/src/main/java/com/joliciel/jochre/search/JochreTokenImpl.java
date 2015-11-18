package com.joliciel.jochre.search;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

class JochreTokenImpl implements JochreToken {
	private Rectangle rectangle, secondaryRectangle;
	private List<String> contentStrings = null;
	private int index = -1;
	private int pageIndex = -1;
	private int paragraphIndex = -1;
	private int rowIndex = -1;
	private int spanStart = -1;
	private int spanEnd = -1;
	
	public JochreTokenImpl(JochreToken jochreToken) {
		this.rectangle = new Rectangle(jochreToken.getRectangle());
		if (jochreToken.getSecondaryRectangle()!=null)
			this.secondaryRectangle = new Rectangle(jochreToken.getSecondaryRectangle());
		this.index = jochreToken.getIndex();
		this.spanStart = jochreToken.getSpanStart();
		this.spanEnd = jochreToken.getSpanEnd();
		this.contentStrings = new ArrayList<String>(jochreToken.getContentStrings());
		this.pageIndex = jochreToken.getPageIndex();
		this.paragraphIndex = jochreToken.getParagraphIndex();
		this.rowIndex = jochreToken.getRowIndex();
	}
	
	public JochreTokenImpl(String text) {
		this.contentStrings = new ArrayList<String>(1);
		contentStrings.add(text);
	}

	public Rectangle getSecondaryRectangle() {
		return secondaryRectangle;
	}

	public void setSecondaryRectangle(Rectangle secondaryRectangle) {
		this.secondaryRectangle = secondaryRectangle;
	}

	public Rectangle getRectangle() {
		return rectangle;
	}

	public List<String> getContentStrings() {
		return contentStrings;
	}

	public void setContentStrings(List<String> contentStrings) {
		this.contentStrings = contentStrings;
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
