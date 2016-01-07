///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
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

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.search.IndexSearcher;

import com.joliciel.jochre.search.alto.AltoPage;
import com.joliciel.jochre.search.alto.AltoService;
import com.joliciel.jochre.search.lexicon.Lexicon;
import com.joliciel.jochre.search.lexicon.LexiconService;

class SearchServiceImpl implements SearchServiceInternal {
	private AltoService altoService;
	private LexiconService lexiconService;
	private JochreIndexSearcher searcher;
	private SearchStatusHolder searchStatusHolder;
	private Locale locale;
	private Lexicon lexicon;
	
	@Override
	public JochreIndexBuilder getJochreIndexBuilder(File indexDir) {
		return this.getJochreIndexBuilder(indexDir, null);
	}

	@Override
	public JochreIndexBuilder getJochreIndexBuilder(File indexDir,
			File contentDir) {
		JochreIndexBuilderImpl builder = new JochreIndexBuilderImpl(indexDir, contentDir);
		builder.setSearchService(this);
		builder.setAltoService(altoService);
		return builder;
	}

	@Override
	public JochreQuery getJochreQuery() {
		JochreQueryImpl query = new JochreQueryImpl();
		query.setSearchService(this);
		return query;
	}
	
	@Override
	public JochreIndexSearcher getJochreIndexSearcher(File indexDir) {
		if (this.searcher==null) {
			JochreIndexSearcherImpl searcher = new JochreIndexSearcherImpl(indexDir);
			searcher.setSearchService(this);
			this.searcher = searcher;
		}
		return this.searcher;
	}
	
	public void purge() {
		this.searcher = null;
		this.lexicon = null;
	}
	
	@Override
	public JochreIndexDocument getJochreIndexDocument(
			IndexSearcher indexSearcher, int docId) {
		JochreIndexDocumentImpl doc = new JochreIndexDocumentImpl(indexSearcher, docId);
		return doc;
	}
	
	@Override
	public JochreIndexDirectory getJochreIndexDirectory(File dir) {
		JochreIndexDirectoryImpl directory = new JochreIndexDirectoryImpl(dir);
		return directory;
	}

	@Override
	public JochreIndexDocument newJochreIndexDocument(JochreIndexDirectory directory,
			int index, List<AltoPage> currentPages) {
		JochreIndexDocumentImpl doc = new JochreIndexDocumentImpl(directory, index, currentPages);
		return doc;
	}
	@Override
	public Tokenizer getJochreTokeniser(TokenExtractor tokenExtractor,
			String fieldName) {
		JochreTokeniser jochreTokeniser = new JochreTokeniser(tokenExtractor, fieldName);
		jochreTokeniser.setSearchService(this);
		return jochreTokeniser;
	}
	
	public Analyzer getJochreTextLayerAnalyzer(TokenExtractor tokenExtractor) {
		JochreTextLayerAnalyser analyser = new JochreTextLayerAnalyser(tokenExtractor);
		analyser.setSearchService(this);
		analyser.setTextNormaliser(this.lexiconService.getTextNormaliser(locale));
		return analyser;
	}
	
	public Analyzer getJochreMetaDataAnalyzer() {
		JochreMetaDataAnalyser analyser = new JochreMetaDataAnalyser();
		analyser.setTextNormaliser(this.lexiconService.getTextNormaliser(locale));
		return analyser;
	}
	
	public Analyzer getJochreQueryAnalyzer() {
		JochreQueryAnalyser analyser = new JochreQueryAnalyser();
		analyser.setTextNormaliser(this.lexiconService.getTextNormaliser(locale));
		analyser.setLexicon(lexicon);
		return analyser;
	}
	
	public AltoService getAltoService() {
		return altoService;
	}
	public void setAltoService(AltoService altoService) {
		this.altoService = altoService;
	}

	@Override
	public JochreToken getJochreToken(JochreToken jochreToken) {
		JochreTokenImpl token = new JochreTokenImpl(jochreToken);
		return token;
	}

	@Override
	public JochreToken getJochreToken(String text) {
		JochreTokenImpl token = new JochreTokenImpl(text);
		return token;
	}

	@Override
	public SearchStatusHolder getSearchStatusHolder() {
		if (searchStatusHolder==null) {
			searchStatusHolder = new SearchStatusHolder();
		}
		return searchStatusHolder;
	}

	public LexiconService getLexiconService() {
		return lexiconService;
	}

	public void setLexiconService(LexiconService lexiconService) {
		this.lexiconService = lexiconService;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public Lexicon getLexicon() {
		return lexicon;
	}

	public void setLexicon(Lexicon lexicon) {
		this.lexicon = lexicon;
	}
}
