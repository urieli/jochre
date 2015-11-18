package com.joliciel.jochre.search;

import java.awt.Rectangle;
import java.util.List;

public interface JochreToken {	
	/**
	 * Alternative indexing possibilities for the current token.
	 * @return
	 */
	public List<String> getContentStrings();
	public void setContentStrings(List<String> contentStrings);
	
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
	
	/**
	 * The main rectangle containing this token.
	 * @return
	 */
	public Rectangle getRectangle();
	
	/**
	 * The secondary rectangle, when this token is spread across two rows.
	 * @return
	 */
	public Rectangle getSecondaryRectangle();
	public void setSecondaryRectangle(Rectangle secondaryRectangle);
	
	public int getPageIndex();
	public int getParagraphIndex();
	public int getRowIndex();
}
