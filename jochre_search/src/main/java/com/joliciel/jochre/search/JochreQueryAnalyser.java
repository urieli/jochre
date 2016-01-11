package com.joliciel.jochre.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

import com.joliciel.jochre.search.lexicon.InflectedFormFilter;
import com.joliciel.jochre.search.lexicon.Lexicon;
import com.joliciel.jochre.search.lexicon.TextNormaliser;
import com.joliciel.jochre.search.lexicon.TextNormalisingFilter;

/**
 * The analyser used to analyse user queries.
 * @author Assaf Urieli
 *
 */
class JochreQueryAnalyser extends Analyzer {
	TextNormaliser textNormaliser;
	Lexicon lexicon;
	SearchServiceInternal searchService;
	
	public JochreQueryAnalyser() {
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		Tokenizer source = new WhitespaceTokenizer();
		TokenStream result = source;
		if (textNormaliser!=null)
			result = new TextNormalisingFilter(result, textNormaliser);
		
		TokenFilter queryTokenFilter = searchService.getQueryTokenFilter(result);
		if (queryTokenFilter!=null)
			result = queryTokenFilter;
		
		if (lexicon!=null)
			result = new InflectedFormFilter(result, lexicon);
		result = new PunctuationFilter(result);
		return new TokenStreamComponents(source, result);
	}

	public TextNormaliser getTextNormaliser() {
		return textNormaliser;
	}

	public void setTextNormaliser(TextNormaliser textNormaliser) {
		this.textNormaliser = textNormaliser;
	}

	public Lexicon getLexicon() {
		return lexicon;
	}

	public void setLexicon(Lexicon lexicon) {
		this.lexicon = lexicon;
	}

	public SearchServiceInternal getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchServiceInternal searchService) {
		this.searchService = searchService;
	}
	
	
}
