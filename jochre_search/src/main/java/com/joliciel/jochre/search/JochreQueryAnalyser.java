package com.joliciel.jochre.search;

import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.pattern.PatternTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Logger LOG = LoggerFactory.getLogger(JochreQueryAnalyser.class);

  private final Lexicon lexicon;
  private final boolean expandInflections;
  private final TextNormaliser textNormaliser;
  private final JochreSearchConfig config;
  private final Pattern tokenSplitPattern;

  public JochreQueryAnalyser(String configId, boolean expandInflections) {
    this.textNormaliser = TextNormaliser.getInstance(configId);
    this.config = JochreSearchConfig.getInstance(configId);
    this.lexicon = config.getLexicon();
    this.expandInflections = expandInflections;
    String queryTokenPattern = config.getConfig().getString("query-token-pattern");
    if (LOG.isDebugEnabled())
      LOG.debug("queryTokenPattern: " + queryTokenPattern);
    this.tokenSplitPattern = Pattern.compile(queryTokenPattern, Pattern.UNICODE_CHARACTER_CLASS);
  }

  @Override
  protected TokenStreamComponents createComponents(final String fieldName) {
    Tokenizer source = new PatternTokenizer(this.tokenSplitPattern, -1);
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

    result = new PunctuationFilter(result);

    if (lexicon != null && expandInflections)
      result = new InflectedFormFilter(result, lexicon);

    return new TokenStreamComponents(source, result);
  }
}
