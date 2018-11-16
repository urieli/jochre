package com.joliciel.jochre.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import com.joliciel.jochre.search.lexicon.TextNormaliser;
import com.joliciel.jochre.search.lexicon.TextNormalisingFilter;

/**
 * The analyser used to analyse tokenised metadata.
 * 
 * @author Assaf Urieli
 *
 */
class JochreMetaDataAnalyser extends Analyzer {
  private final TextNormaliser textNormaliser;

  public JochreMetaDataAnalyser(JochreSearchConfig config) {
    textNormaliser = TextNormaliser.getInstance(config);
  }

  @Override
  protected TokenStreamComponents createComponents(final String fieldName) {
    Tokenizer source = new StandardTokenizer();
    TokenStream result = source;
    if (textNormaliser != null)
      result = new TextNormalisingFilter(result, textNormaliser);
    else {
      result = new ASCIIFoldingFilter(result);
      result = new LowerCaseFilter(result);
    }
    result = new PunctuationFilter(result);
    return new TokenStreamComponents(source, result);
  }
}
