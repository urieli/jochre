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
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.search.IndexSearcher;

import com.joliciel.jochre.search.alto.AltoPage;
import com.joliciel.jochre.search.alto.AltoService;

class SearchServiceImpl implements SearchServiceInternal {
	private AltoService altoService;
	private JochreIndexSearcher searcher;
	private SearchStatusHolder searchStatusHolder;
	
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
	public JochreQuery getJochreQuery(Map<String, String> argMap) {
		JochreQueryImpl query = new JochreQueryImpl(argMap);
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
	
	public void purgeSearcher() {
		this.searcher = null;
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
	
	public Analyzer getJochreAnalyser(TokenExtractor tokenExtractor) {
		JochreAnalyser analyser = new JochreAnalyser(tokenExtractor);
		analyser.setSearchService(this);
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

}
