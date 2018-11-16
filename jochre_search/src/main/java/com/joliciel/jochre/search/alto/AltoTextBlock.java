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

public class AltoTextBlock {
  private List<AltoTextLine> rows = new ArrayList<>();
  private Rectangle rectangle;
  private AltoPage page;
  private int wordCount = -1;
  private int index = -1;

  public AltoTextBlock(AltoPage page, int left, int top, int width, int height) {
    super();
    this.page = page;
    this.rectangle = new Rectangle(left, top, width, height);
    this.index = this.page.getTextBlocks().size();
    this.page.getTextBlocks().add(this);
  }

  public List<AltoTextLine> getTextLines() {
    return rows;
  }

  public Rectangle getRectangle() {
    return rectangle;
  }

  public AltoPage getPage() {
    return page;
  }

  public int wordCount() {
    if (wordCount < 0) {
      wordCount = 0;
      for (AltoTextLine row : this.getTextLines()) {
        wordCount += row.wordCount();
      }
    }
    return wordCount;
  }

  public int getIndex() {
    return index;
  }

  /**
   * For any hyphenated words at end-of-line, replace the two parts of the hyphens
   * by a single string, where the secondary rectangle is set to contain the
   * second word.
   */
  public void joinHyphens() {
    for (int i = 0; i < this.rows.size(); i++) {
      AltoTextLine row = this.rows.get(i);
      for (int j = 0; j < row.getStrings().size(); j++) {
        AltoString string = row.getStrings().get(j);
        if (string.isHyphen()) {
          AltoString prevString = null;
          AltoString nextString = null;
          if (j > 0 && !row.getStrings().get(j - 1).isWhiteSpace()) {
            prevString = row.getStrings().get(j - 1);
          }

          if (j + 1 < row.getStrings().size() && !row.getStrings().get(j + 1).isWhiteSpace()) {
            nextString = row.getStrings().get(j + 1);
          }
          AltoTextLine nextRow = null;
          if (nextString == null && j == row.getStrings().size() - 1 && i + 1 < this.rows.size()) {
            nextRow = this.rows.get(i + 1);
            if (nextRow.getStrings().size() > 0 && !nextRow.getStrings().get(0).isWhiteSpace()) {
              nextString = nextRow.getStrings().get(0);
            }
          }

          if (prevString != null && nextString != null) {
            // merge with the hyphen
            prevString.mergeWithNext();

            // merge with the string following the hyphen
            prevString.mergeWithNext();

            // since we removed the hyphen on which we were placed, we need to decrement j,
            // so as to be placed on the word which followed the one after the hyphen, which
            // has now taken its place
            j--;
          }

        }
      }
    }
  }
}
