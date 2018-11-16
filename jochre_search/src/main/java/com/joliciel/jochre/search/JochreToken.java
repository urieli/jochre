package com.joliciel.jochre.search;

import java.awt.Rectangle;
import java.util.List;

public interface JochreToken {  
  /**
   * Alternative indexing possibilities for the current token.
   */
  public List<String> getContentStrings();
  public void setContentStrings(List<String> contentStrings);
  
  /**
   * The index of this token within the current row.
   */
  public int getIndex();

  /**
   * The starting position of this token in the current document.
   */
  public int getSpanStart();
  public void setSpanStart(int spanStart);
  
  /**
   * The position just after the last character in this token.
   */
  public int getSpanEnd();
  public void setSpanEnd(int spanEnd);
  
  /**
   * The main rectangle containing this token.
   */
  public Rectangle getRectangle();
  
  /**
   * The secondary rectangle, when this token is spread across two rows.
   */
  public Rectangle getSecondaryRectangle();
  public void setSecondaryRectangle(Rectangle secondaryRectangle);
  
  public int getPageIndex();
  public int getParagraphIndex();
  public int getRowIndex();
  
  /**
   * Does this string represent punctuation?
   */
  public boolean isPunctuation();
}
