package com.joliciel.jochre.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;

import com.joliciel.jochre.search.lexicon.TextNormaliser;
import com.joliciel.jochre.search.lexicon.TextNormalisingFilter;

/**
 * An analyser used to analyse metadata containing a single untokenised keyword.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreKeywordAnalyser extends Analyzer {
	private final TextNormaliser textNormaliser;

	public JochreKeywordAnalyser(JochreSearchConfig config) {
		textNormaliser = TextNormaliser.getInstance(config);
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		Tokenizer source = new KeywordTokenizer();
		TokenStream result = source;
		if (textNormaliser != null)
			result = new TextNormalisingFilter(result, textNormaliser);
		else {
			result = new ASCIIFoldingFilter(result);
			result = new LowerCaseFilter(result);
		}
		return new TokenStreamComponents(source, result);
	}

}
