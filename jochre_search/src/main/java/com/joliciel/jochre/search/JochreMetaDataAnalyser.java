package com.joliciel.jochre.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import com.joliciel.jochre.search.lexicon.TextNormaliser;
import com.joliciel.jochre.search.lexicon.TextNormalisingFilter;

/**
 * The analyser used to analyse tokenised metadata.
 * @author Assaf Urieli
 *
 */
class JochreMetaDataAnalyser extends Analyzer {
	TextNormaliser textNormaliser;

	public JochreMetaDataAnalyser() {
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		Tokenizer source = new StandardTokenizer();
		TokenStream result = source;
		if (textNormaliser!=null)
			result = new TextNormalisingFilter(result, textNormaliser);
		result = new PunctuationFilter(result);
		return new TokenStreamComponents(source, result);
	}

	public TextNormaliser getTextNormaliser() {
		return textNormaliser;
	}

	public void setTextNormaliser(TextNormaliser textNormaliser) {
		this.textNormaliser = textNormaliser;
	}
}
