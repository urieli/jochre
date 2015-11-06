package com.joliciel.jochre.search;

import java.util.List;

public interface JochreToken {	
	/**
	 * Alternative indexing possibilities for the current token.
	 * @return
	 */
	public List<String> getContentStrings();
	public void setContentStrings(List<String> contentStrings);
	
	/**
	 * Any hyphenated content strings for the current token.
	 * @return
	 */
	public List<String> getHyphenatedContentStrings();
	public void setHyphenatedContentStrings(List<String> hyphenatedContentStrings);
	
	/**
	 * The index of this token within the current row.
	 * @return
	 */
	public int getIndex();

	/**
	 * The starting position of this token in the current document.
	 * @return
	 */
	public int getSpanStart();
	public void setSpanStart(int spanStart);
	
	/**
	 * The position just after the last character in this token.
	 * @return
	 */
	public int getSpanEnd();
	public void setSpanEnd(int spanEnd);
	
	public int getLeft();
	public int getTop();
	public int getWidth();
	public int getHeight();
	public int getPageIndex();
	public int getParagraphIndex();
	public int getRowIndex();
}
