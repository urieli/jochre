package com.joliciel.jochre.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

import com.joliciel.jochre.search.lexicon.InflectedFormFilter;
import com.joliciel.jochre.search.lexicon.Lexicon;

/**
 * The analyser used to analyse user queries.
 * @author Assaf Urieli
 *
 */
class JochreQueryAnalyser extends Analyzer {
	private Lexicon lexicon;
	private SearchServiceInternal searchService;
	private boolean expandInflections = true;
	
	public JochreQueryAnalyser() {
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		Tokenizer source = new WhitespaceTokenizer();
		TokenStream result = source;
		
		TokenFilter queryTokenFilter = searchService.getQueryTokenFilter(result);
		if (queryTokenFilter!=null)
			result = queryTokenFilter;
		
		if (lexicon!=null && expandInflections)
			result = new InflectedFormFilter(result, lexicon);
		result = new PunctuationFilter(result);
		return new TokenStreamComponents(source, result);
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

	public boolean isExpandInflections() {
		return expandInflections;
	}

	public void setExpandInflections(boolean expandInflections) {
		this.expandInflections = expandInflections;
	}
	
	
}
