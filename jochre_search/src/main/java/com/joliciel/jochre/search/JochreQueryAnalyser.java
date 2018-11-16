package com.joliciel.jochre.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;

import com.joliciel.jochre.search.lexicon.InflectedFormFilter;
import com.joliciel.jochre.search.lexicon.Lexicon;
import com.joliciel.jochre.search.lexicon.TextNormaliser;
import com.joliciel.jochre.search.lexicon.TextNormalisingFilter;

/**
 * The analyser used to analyse user queries.
 * 
 * @author Assaf Urieli
 *
 */
class JochreQueryAnalyser extends Analyzer {
  private final Lexicon lexicon;
  private final boolean expandInflections;
  private final TextNormaliser textNormaliser;
  private final JochreSearchConfig config;

  public JochreQueryAnalyser(JochreSearchConfig config, boolean expandInflections) {
    this.textNormaliser = TextNormaliser.getInstance(config);
    this.config = config;
    this.lexicon = config.getLexicon();
    this.expandInflections = expandInflections;
  }

  @Override
  protected TokenStreamComponents createComponents(final String fieldName) {
    Tokenizer source = new WhitespaceTokenizer();
    TokenStream result = source;

    if (textNormaliser != null)
      result = new TextNormalisingFilter(result, textNormaliser);
    else {
      result = new ASCIIFoldingFilter(result);
      result = new LowerCaseFilter(result);
    }

    TokenFilter queryTokenFilter = config.getQueryTokenFilter(result);
    if (queryTokenFilter != null)
      result = queryTokenFilter;

    if (lexicon != null && expandInflections)
      result = new InflectedFormFilter(result, lexicon);
    result = new PunctuationFilter(result);
    return new TokenStreamComponents(source, result);
  }
}
