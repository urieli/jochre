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

import com.joliciel.jochre.search.JochreSearchConstants;
import com.joliciel.jochre.search.JochreToken;

public class AltoString implements JochreToken {
  /**
   * Style indicating emphasis through individual character separation.
   */
  public static final String SEP_EMPH_STYLE = "SEP_EMPH";

  private static Pattern whiteSpacePattern = Pattern.compile("[\\s\ufeff]+", Pattern.UNICODE_CHARACTER_CLASS);
  private static Pattern punctuationPattern = Pattern.compile("\\p{Punct}+", Pattern.UNICODE_CHARACTER_CLASS);
  private String content;
  private Rectangle rectangle;
  private Rectangle secondaryRectangle;
  private AltoTextLine textLine;
  private Set<String> alternatives = new HashSet<>();
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

  public AltoString(AltoTextLine textLine, String content, int left, int top, int width, int height) {
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

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
    String contentNoPunct = punctuationPattern.matcher(content).replaceAll("");
    if (contentNoPunct.length() > 0 && !content.equals(contentNoPunct))
      this.alternatives.add(contentNoPunct);
  }

  /**
   * Alternative possibilities for the current string.
   */

  public Set<String> getAlternatives() {
    return alternatives;
  }

  public void setAlternatives(Set<String> alternatives) {
    this.alternatives = alternatives;
  }

  public AltoTextLine getTextLine() {
    return textLine;
  }

  /**
   * The confidence assigned by the OCR model to this string, from 0 to 1.
   */

  public double getConfidence() {
    return confidence;
  }

  public void setConfidence(double confidence) {
    this.confidence = confidence;
  }

  /**
   * Is this string a hyphen within a hyphenated word?
   */

  public boolean isHyphen() {
    return hyphen;
  }

  public void setHyphen(boolean hyphen) {
    this.hyphen = hyphen;
  }

  /**
   * Is this string the first half of a hyphenated word which crosses an
   * end-of-line.
   */

  public boolean isHyphenStart() {
    return hyphenStart;
  }

  public void setHyphenStart(boolean hyphenStart) {
    this.hyphenStart = hyphenStart;
  }

  /**
   * Is this string the second half of a hyphenated word which corsses an
   * end-of-line.
   */

  public boolean isHyphenEnd() {
    return hyphenEnd;
  }

  public void setHyphenEnd(boolean hyphenEnd) {
    this.hyphenEnd = hyphenEnd;
  }

  public String getHyphenatedContent() {
    return hyphenatedContent;
  }

  /**
   * The full hyphenated word, or null if string is not part of hyphenated word.
   */

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

  @Override
  public Rectangle getSecondaryRectangle() {
    return secondaryRectangle;
  }

  @Override
  public void setSecondaryRectangle(Rectangle secondaryRectangle) {
    this.secondaryRectangle = secondaryRectangle;
  }

  /**
   * The index of this string within the current TextLine.
   */

  @Override
  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  /**
   * The starting position of this string's character span, where 0 is considered
   * to be the start of the current page.
   */

  @Override
  public int getSpanStart() {
    return spanStart;
  }

  @Override
  public void setSpanStart(int spanStart) {
    this.spanStart = spanStart;
  }

  /**
   * The position just after the last character in this string.
   */

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

  /**
   * Does this string represent white space?
   */

  public boolean isWhiteSpace() {
    return whiteSpace;
  }

  /**
   * Does this string represent punctuation?
   */

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
      contentStrings = new ArrayList<>();
      contentStrings.add(this.content.replace(JochreSearchConstants.INDEX_NEWLINE, ""));
      this.alternatives.remove(this.content);
      contentStrings.addAll(this.alternatives);
    }
    return contentStrings;
  }

  @Override
  public void setContentStrings(List<String> contentStrings) {
    this.contentStrings = contentStrings;
  }

  /**
   * Merge a given AltoString with the next string in the line or the first string
   * on the following line (if it's the last in the line), removing any
   * intervening whitespaces.
   * 
   * @return true if merge occurred, false otherwise (whitespace)
   */

  public boolean mergeWithNext() {
    AltoString nextString = null;
    AltoTextLine nextRow = null;
    boolean endOfRowHyphen = false;

    // remove any whitespace immediately following this string
    while (this.index < this.textLine.getStrings().size() - 1) {
      nextString = this.textLine.getStrings().get(this.index + 1);
      this.textLine.getStrings().remove(this.index + 1);
      if (!nextString.isWhiteSpace())
        break;
    }
    if (nextString != null && nextString.isWhiteSpace())
      nextString = null;

    // if it's the last word in the line, find the first word in the next
    // line
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

    // we now have the two strings to merge
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

    Set<String> alternatives = new TreeSet<>();
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
      this.setContent(this.getContent() + JochreSearchConstants.INDEX_NEWLINE + nextString.getContent());
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

  public String getStyle() {
    return style;
  }

  public void setStyle(String style) {
    this.style = style;
  }
}
