package com.joliciel.jochre.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

/**
 * A very basic analyser, which passes a TokenExtractor to the tokeniser.
 * @author Assaf Urieli
 *
 */
class JochreAnalyser extends Analyzer {
	private static final Log LOG = LogFactory.getLog(JochreAnalyser.class);
	TokenExtractor tokenExtractor;
	
	SearchServiceInternal searchService;
	
	public JochreAnalyser(TokenExtractor tokenExtractor) {
		super(Analyzer.PER_FIELD_REUSE_STRATEGY);
		this.tokenExtractor = tokenExtractor;
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		if (LOG.isTraceEnabled())
			LOG.trace("Analysing field " + fieldName);

		Tokenizer source = searchService.getJochreTokeniser(tokenExtractor, fieldName);	
		TokenStream result = source;
		return new TokenStreamComponents(source, result);
	}

	public SearchServiceInternal getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchServiceInternal searchService) {
		this.searchService = searchService;
	}
}
