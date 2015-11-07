package com.joliciel.jochre.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

public class JochreWhitespaceAnalyser extends Analyzer {

	public JochreWhitespaceAnalyser() {
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		Tokenizer source = new WhitespaceTokenizer();
		TokenStream result = new YiddishNormalisingFilter(source);
		result = new PunctuationFilter(result);
		return new TokenStreamComponents(source, result);
	}

}
