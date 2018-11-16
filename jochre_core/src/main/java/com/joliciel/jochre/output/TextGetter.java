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
package com.joliciel.jochre.output;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.Bidi;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.lexicon.Lexicon;

/**
 * Converts Jochre's analysis to human-readable text, either in plain or xhtml
 * format. Note that the current implementation has some Yiddish-specific rules
 * (around Yiddish-style double-quotes) which will need to be generalised.
 * 
 * @author Assaf Urieli
 *
 */
public class TextGetter extends AbstractExporter implements DocumentObserver {
  public enum TextFormat {
    PLAIN,
    XHTML;
  }

  private static final Logger LOG = LoggerFactory.getLogger(TextGetter.class);

  private TextFormat textFormat = TextFormat.PLAIN;
  private Lexicon lexicon;

  public TextGetter(Writer writer, TextFormat textFormat) {
    this(writer, textFormat, null);
  }

  public TextGetter(Writer writer, TextFormat textFormat, Lexicon lexicon) {
    super(writer);
    this.writer = writer;
    this.textFormat = textFormat;
    this.lexicon = lexicon;
  }

  public TextGetter(File outputDir, TextFormat textFormat, Lexicon lexicon) {
    super(outputDir, textFormat == TextFormat.PLAIN ? ".txt" : ".htm");
    this.textFormat = textFormat;
    this.lexicon = lexicon;
  }

  @Override
  protected void onDocumentStartInternal(JochreDocument jochreDocument) {
    try {
      if (textFormat.equals(TextFormat.XHTML)) {
        writer.write("<html>\n");
        writer.write("<head>\n");
        writer.write("<meta http-equiv=\"Content-type\" content=\"text/html;charset=UTF-8\" />\n");
        writer.write("<title>" + jochreDocument.getName() + "</title>\n");
        writer.write("</head>\n");
        writer.write("<body>\n");
        writer.flush();
      }
    } catch (IOException e) {
      LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onPageStart(JochrePage jochrePage) {
    if (textFormat.equals(TextFormat.XHTML)) {
      try {
        writer.write("<h3>Page " + (jochrePage.getIndex()) + "</h3>\n");
      } catch (IOException e) {
        LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void onImageStart(JochreImage jochreImage) {
  }

  @Override
  public void onImageComplete(JochreImage image) {
    try {
      double minRatioBiggerFont = 1.15;
      double maxRatioSmallerFont = 0.85;

      double meanXHeight = 0;
      if (textFormat.equals(TextFormat.XHTML)) {
        Mean xHeightMean = new Mean();
        for (Paragraph paragraph : image.getParagraphs()) {
          if (!paragraph.isJunk()) {
            for (RowOfShapes row : paragraph.getRows()) {
              for (GroupOfShapes group : row.getGroups()) {
                for (Shape shape : group.getShapes()) {
                  xHeightMean.increment(shape.getXHeight());
                }
              }
            }
          }
        }
        meanXHeight = xHeightMean.getResult();
      }
      String paragraphString = "<p>";
      if (!image.isLeftToRight())
        paragraphString = "<p dir=\"rtl\">";
      for (Paragraph paragraph : image.getParagraphs()) {
        if (!paragraph.isJunk()) {
          if (textFormat.equals(TextFormat.XHTML))
            writer.append(paragraphString);

          Map<Integer, Boolean> fontSizeChanges = new TreeMap<Integer, Boolean>();
          int currentFontSize = 0;
          StringBuilder paragraphText = new StringBuilder();

          String lastWord = "";
          boolean lastRowEndedWithHyphen = false;
          for (RowOfShapes row : paragraph.getRows()) {
            for (GroupOfShapes group : row.getGroups()) {
              boolean endOfRowHyphen = false;
              if (textFormat.equals(TextFormat.XHTML)) {
                double ratio = group.getXHeight() / meanXHeight;
                if (ratio >= minRatioBiggerFont) {
                  if (currentFontSize <= 0)
                    fontSizeChanges.put(paragraphText.length(), true);
                  currentFontSize = 1;
                } else if (ratio <= maxRatioSmallerFont) {
                  if (currentFontSize >= 0)
                    fontSizeChanges.put(paragraphText.length(), false);
                  currentFontSize = -1;
                } else if (currentFontSize != 0) {
                  if (currentFontSize > 0)
                    fontSizeChanges.put(paragraphText.length(), false);
                  else if (currentFontSize < 0)
                    fontSizeChanges.put(paragraphText.length(), true);
                  currentFontSize = 0;
                }
              }
              StringBuilder sb = new StringBuilder();
              StringBuilder currentSequence = new StringBuilder();
              for (Shape shape : group.getShapes()) {
                String letter = shape.getLetter();

                if (letter.startsWith("|")) {
                  // beginning of a gehakte letter
                  currentSequence.append(shape.getLetter());
                  continue;
                } else if (letter.endsWith("|")) {
                  // end of a gehakte letter
                  if (currentSequence.length() > 0 && currentSequence.charAt(0) == '|') {
                    String letter1 = currentSequence.toString().substring(1);
                    String letter2 = letter.substring(0, letter.length() - 1);
                    if (letter1.equals(letter2)) {
                      letter = letter1;
                    } else {
                      letter = currentSequence.toString() + letter;
                    }
                    currentSequence = new StringBuilder();
                  }
                }

                if (letter.equals(",")) {
                  // TODO: for Yiddish, need a way to
                  // generalise this
                  // could be ",," = "„"
                  if (currentSequence.length() > 0 && currentSequence.charAt(0) == ',') {
                    sb.append("„");
                    currentSequence = new StringBuilder();
                  } else {
                    currentSequence.append(shape.getLetter());
                  }
                } else if (letter.equals("'")) {
                  // TODO: for Yiddish, need a way to
                  // generalise this
                  // could be "''" = "“"
                  if (currentSequence.length() > 0 && currentSequence.charAt(0) == '\'') {
                    sb.append("“");
                    currentSequence = new StringBuilder();
                  } else {
                    currentSequence.append(shape.getLetter());
                  }
                } else if (letter.equals("-")) {
                  if (shape.getIndex() == group.getShapes().size() - 1 && group.getIndex() == row.getGroups().size() - 1
                      && row.getIndex() != paragraph.getRows().size() - 1) {
                    // do nothing - dash at the end of the
                    // line
                    // we'll assume for now these dashes are
                    // always supposed to disappear
                    // though of course they could be used
                    // in the place of a real mid-word dash
                    endOfRowHyphen = true;
                  } else {
                    sb.append(shape.getLetter());
                  }
                } else {
                  sb.append(currentSequence);
                  currentSequence = new StringBuilder();
                  // TODO: for Yiddish, need a way to
                  // generalise this
                  if (letter.equals(",,")) {
                    sb.append("„");
                  } else if (letter.equals("''")) {
                    sb.append("“");
                  } else {
                    sb.append(letter);
                  }
                }
              } // next shape
              sb.append(currentSequence);

              String word = sb.toString();
              if (endOfRowHyphen) {
                lastRowEndedWithHyphen = true;
                endOfRowHyphen = false;
              } else if (lastRowEndedWithHyphen) {
                if (lexicon != null) {
                  String hyphenatedWord = lastWord + "-" + word;
                  int frequency = lexicon.getFrequency(hyphenatedWord);
                  LOG.debug("hyphenatedWord: " + hyphenatedWord + ", Frequency: " + frequency);
                  if (frequency > 0) {
                    paragraphText.append("-");
                  }
                }
                lastRowEndedWithHyphen = false;
              }
              lastWord = word;
              paragraphText.append(word);
              if (!lastRowEndedWithHyphen)
                paragraphText.append(' ');
            } // next group
          } // next row
          String paragraphStr = paragraphText.toString();

          Writer currentWriter = writer;
          boolean haveFontSizes = fontSizeChanges.size() > 0;
          if (haveFontSizes) {
            currentWriter = new StringWriter();
          }

          if (image.getPage().getDocument().isLeftToRight()) {
            currentWriter.append(paragraphText);
          } else {
            this.appendBidiText(paragraphStr, currentWriter);
          }

          if (haveFontSizes) {
            currentFontSize = 0;

            String text = currentWriter.toString();
            int currentIndex = 0;

            for (int fontSizeChange : fontSizeChanges.keySet()) {
              boolean isBigger = fontSizeChanges.get(fontSizeChange);
              writer.append(text.substring(currentIndex, fontSizeChange));
              if (isBigger) {
                if (currentFontSize == 0) {
                  writer.append("<big>");
                  currentFontSize++;
                } else if (currentFontSize < 0) {
                  writer.append("</small>");
                  currentFontSize++;
                }
              } else {
                if (currentFontSize == 0) {
                  writer.append("<small>");
                  currentFontSize--;
                } else if (currentFontSize > 0) {
                  writer.append("</big>");
                  currentFontSize--;
                }
              }
              currentIndex = fontSizeChange;
            }
            writer.append(text.substring(currentIndex));

            if (currentFontSize > 0) {
              writer.append("</big>");
            } else if (currentFontSize < 0) {
              writer.append("</small>");
            }
          } // haveFontSizes?

          if (textFormat.equals(TextFormat.XHTML))
            writer.append("</p>");
          else
            writer.append('\n');
          writer.flush();
        } // paragraph.isJunk()?
      } // next paragraph
    } catch (IOException e) {
      LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onPageComplete(JochrePage jochrePage) {
    if (textFormat.equals(TextFormat.XHTML)) {
      try {
        writer.write("<hr/>\n");
      } catch (IOException e) {
        LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  protected void onDocumentCompleteInternal(JochreDocument jochreDocument) {
    try {
      if (textFormat.equals(TextFormat.XHTML)) {
        writer.write("</body>\n");
        writer.write("</html>\n");
        writer.flush();
      }
    } catch (IOException e) {
      LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
      throw new RuntimeException(e);
    }
  }

  void appendBidiText(String text, Writer writer) {
    try {
      // assumption here is that if text is marked as left-to-right
      // it should be reversed.
      Bidi bidi = new Bidi(text, Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);

      // From Bidi API:
      // If there are multiple runs of text, information about the runs
      // can be accessed by indexing
      // to get the start, limit, and level of a run.
      // The level represents both the direction and the 'nesting level'
      // of a directional run.
      // Odd levels are right-to-left, while even levels are
      // left-to-right.
      // So for example level 0 represents left-to-right text, while level
      // 1 represents right-to-left text,
      // and level 2 represents left-to-right text embedded in a
      // right-to-left run.
      for (int i = 0; i < bidi.getRunCount(); ++i) {

        int start = bidi.getRunStart(i);
        int limit = bidi.getRunLimit(i);
        int level = bidi.getRunLevel(i);
        String str = text.substring(start, limit);
        if (level % 2 == 1) {
          writer.append(str);
        } else {
          StringBuilder reverseString = new StringBuilder();
          for (int j = str.length() - 1; j >= 0; j--)
            reverseString.append(str.charAt(j));
          writer.append(reverseString.toString());
        }
      }
    } catch (IOException e) {
      LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
      throw new RuntimeException(e);
    }
  }
}
