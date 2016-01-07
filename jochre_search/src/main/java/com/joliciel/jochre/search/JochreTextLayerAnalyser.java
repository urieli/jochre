///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Assaf Urieli
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.jochre.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

import com.joliciel.jochre.search.lexicon.TextNormaliser;
import com.joliciel.jochre.search.lexicon.TextNormalisingFilter;

/**
 * An analyser using a TokenExtractor to passed to JochreTokeniser, used when the actual
 * tokenising occurred prior to analysis (e.g. in the case of the OCR text layer).
 * @author Assaf Urieli
 *
 */
class JochreTextLayerAnalyser extends Analyzer {
	private static final Log LOG = LogFactory.getLog(JochreTextLayerAnalyser.class);
	private TokenExtractor tokenExtractor;
	private TextNormaliser textNormaliser;
	
	private SearchServiceInternal searchService;
	
	public JochreTextLayerAnalyser(TokenExtractor tokenExtractor) {
		super(Analyzer.PER_FIELD_REUSE_STRATEGY);
		this.tokenExtractor = tokenExtractor;
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		if (LOG.isTraceEnabled())
			LOG.trace("Analysing field " + fieldName);

		Tokenizer source = searchService.getJochreTokeniser(tokenExtractor, fieldName);	
		TokenStream result = source;
		if (textNormaliser!=null)
			result = new TextNormalisingFilter(result, textNormaliser);
		result = new PunctuationFilter(result);
		return new TokenStreamComponents(source, result);
	}

	public SearchServiceInternal getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchServiceInternal searchService) {
		this.searchService = searchService;
	}
	

	public TextNormaliser getTextNormaliser() {
		return textNormaliser;
	}

	public void setTextNormaliser(TextNormaliser textNormaliser) {
		this.textNormaliser = textNormaliser;
	}
}
