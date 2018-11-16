///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.jochre.lexicon;

import java.util.List;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.graphics.CorpusSelectionCriteria;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreCorpusImageReader;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Shape;

/**
 * Builds a lexicon from all words found in a particular portion of the corpus.
 * 
 * @author Assaf Urieli
 *
 */
public class CorpusLexiconBuilder {
  private final CorpusSelectionCriteria criteria;

  private final JochreSession jochreSession;

  public CorpusLexiconBuilder(CorpusSelectionCriteria criteria, JochreSession jochreSession) {
    this.jochreSession = jochreSession;
    this.criteria = criteria;
  }

  /**
   * Build a lexicon from the training corpus.
   */
  public TextFileLexicon buildLexicon() {
    TextFileLexicon lexicon = new TextFileLexicon();
    JochreCorpusImageReader imageReader = new JochreCorpusImageReader(jochreSession);
    imageReader.setSelectionCriteria(criteria);
    String wordText = "";
    while (imageReader.hasNext()) {
      JochreImage image = imageReader.next();
      for (Paragraph paragraph : image.getParagraphs()) {
        // rows ending in dashes can only be held-over within the same
        // paragraph.
        // to avoid strange things like a page number getting added to
        // the word,
        // if the dash is on the last row of the page.
        String holdoverWord = null;
        for (RowOfShapes row : paragraph.getRows()) {
          for (GroupOfShapes group : row.getGroups()) {
            if (group.isBrokenWord())
              continue;

            wordText = "";
            for (Shape shape : group.getShapes()) {
              if (shape.getLetter() != null)
                wordText += shape.getLetter();
            }

            if (wordText.length() == 0) {
              lexicon.incrementEntry("");
              continue;
            }
            List<String> words = jochreSession.getLinguistics().splitText(wordText);

            int i = 0;
            for (String word : words) {
              if (i == 0) {
                // first word
                if (holdoverWord != null && holdoverWord.length() > 0) {
                  word = holdoverWord + word;
                  holdoverWord = null;
                }
              }
              if (i == words.size() - 1) {
                // last word
                if (group.getIndex() == row.getGroups().size() - 1 && word.endsWith("-")) {
                  // a dash at the end of a line
                  if (group.isHardHyphen())
                    holdoverWord = word;
                  else
                    holdoverWord = word.substring(0, word.length() - 1);
                  word = "";
                }
              }
              lexicon.incrementEntry(word);
              i++;
            }
          }
        }
      }
    }

    // TODO: re-adjust the final probability for numbers
    // Currently all numbers are represented as a string of zeros, to imply
    // the
    // approximately
    // equiprobable nature of any series of numbers
    // However, any single string of numbers should divide the total
    // frequency
    // of the string
    // by the number of possible combinations. Therefore, "0" should divide
    // by
    // 10, "00" by a hundred,
    // etc.

    return lexicon;
  }

  /**
   * Selection criteria for selecting the images to be processed.
   */
  public CorpusSelectionCriteria getCriteria() {
    return criteria;
  }

}
