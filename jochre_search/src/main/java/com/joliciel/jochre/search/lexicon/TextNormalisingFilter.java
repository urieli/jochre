package com.joliciel.jochre.search.lexicon;

import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class TextNormalisingFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  
  private TextNormaliser textNormaliser;
  
  public TextNormalisingFilter(TokenStream input, TextNormaliser textNormaliser) {
    super(input);
    this.textNormaliser = textNormaliser;
  }

  @Override
  public final boolean incrementToken() throws IOException {
      if (!input.incrementToken())
          return false;
      
      String term = new String(termAtt.buffer(), 0, termAtt.length());
      term = textNormaliser.normalise(term);
      termAtt.copyBuffer(term.toCharArray(), 0, term.length());
    return true;
  }


}
