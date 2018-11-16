package com.joliciel.jochre.search;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.joliciel.jochre.search.lexicon.Lexicon;

/**
 * Write document contents as simple HTML. There will be an A name tag for each
 * page, with the name "page<i>n</i>".
 * 
 * @author Assaf Urieli
 *
 */
public class DocumentContentHTMLWriter {
  private final Writer writer;
  private final JochreIndexDocument doc;
  private final Lexicon lexicon;

  private static final Pattern NEWLINE_PATTERN = Pattern.compile("[" + JochreSearchConstants.INDEX_NEWLINE + JochreSearchConstants.INDEX_PARAGRAPH + "]");

  public DocumentContentHTMLWriter(Writer writer, JochreIndexDocument doc, JochreSearchConfig config) {
    this.writer = writer;
    this.doc = doc;
    this.lexicon = config.getLexicon();
  }

  public void writeContents() throws IOException {
    Map<Integer, Integer> pageOffsets = new HashMap<>();
    int lastOffset = 0;
    for (int i = this.doc.getStartPage(); i <= this.doc.getEndPage(); i++) {
      int offset = lastOffset;
      try {
        offset = doc.getStartIndex(i, 0);
      } catch (IndexFieldNotFoundException e) {
        // do nothing
      }
      pageOffsets.put(i, offset);
      lastOffset = offset;
    }

    int i = this.doc.getStartPage();
    String contents = doc.getContents();
    StringBuilder sb = new StringBuilder();
    int innerPos = 0;
    Matcher matcher = NEWLINE_PATTERN.matcher(contents);
    while (i <= doc.getEndPage() && innerPos >= pageOffsets.get(i)) {
      sb.append("<hr><a name=\"page" + i + "\"></a>\n");
      i++;
    }
    sb.append("<p>");
    while (matcher.find()) {
      String before = contents.substring(innerPos, matcher.start());
      String newline = contents.substring(matcher.start(), matcher.end());
      if (JochreSearchConstants.INDEX_NEWLINE.equals(newline)) {
        if (before.endsWith("-") && !before.endsWith(" -")) {
          if (lexicon != null) {
            int prevSpace = before.lastIndexOf(' ');
            if (prevSpace < 0)
              prevSpace = 0;
            String prevWord = before.substring(prevSpace);
            int nextSpace = contents.indexOf(' ', matcher.end());
            if (nextSpace < 0)
              nextSpace = contents.length();
            String nextWord = contents.substring(matcher.end(), nextSpace);
            String compositeWord = prevWord + nextWord;
            Set<String> lemmas = lexicon.getLemmas(compositeWord);
            if (lemmas == null || lemmas.isEmpty()) {
              // composite word doesn't exist, assume soft hyphen
              sb.append(before.substring(0, before.length() - 1));
            } else {
              sb.append(before);
            }
          } else {
            // always assume it's a soft hyphen.
            sb.append(before.substring(0, before.length() - 1));
          }
        } else {
          sb.append(before);
          sb.append(" ");
        }
      } else if (JochreSearchConstants.INDEX_PARAGRAPH.equals(newline)) {
        sb.append(before);
        sb.append("</p>\n");
        while (i <= doc.getEndPage() && matcher.end() >= pageOffsets.get(i)) {
          sb.append("<hr><a name=\"page" + i + "\"></a>\n");
          i++;
        }
        sb.append("<p>");
      }
      innerPos = matcher.end();
    }
    if (innerPos < contents.length())
      sb.append(contents.substring(innerPos));
    sb.append("</p>\n");
    while (i <= doc.getEndPage()) {
      sb.append("<hr><a name=\"page" + i + "\"></a>\n");
      i++;
    }
    writer.write(sb.toString());
    writer.flush();
  }

}
