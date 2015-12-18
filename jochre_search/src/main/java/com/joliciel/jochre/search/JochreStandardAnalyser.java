package com.joliciel.jochre.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class JochreStandardAnalyser extends Analyzer {

	public JochreStandardAnalyser() {
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		Tokenizer source = new StandardTokenizer();
		TokenStream result = new YiddishNormalisingFilter(source);
		result = new PunctuationFilter(result);
		return new TokenStreamComponents(source, result);
	}

}
