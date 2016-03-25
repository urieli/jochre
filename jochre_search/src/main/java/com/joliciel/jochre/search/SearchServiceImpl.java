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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

import com.joliciel.jochre.search.alto.AltoPage;
import com.joliciel.jochre.search.alto.AltoService;
import com.joliciel.jochre.search.feedback.FeedbackService;
import com.joliciel.jochre.search.lexicon.Lexicon;
import com.joliciel.jochre.search.lexicon.LexiconService;

class SearchServiceImpl implements SearchServiceInternal {
	private static final Log LOG = LogFactory.getLog(SearchServiceImpl.class);

	private AltoService altoService;
	private LexiconService lexiconService;
	private FeedbackService feedbackService;

	private JochreIndexSearcher searcher;
	private SearchStatusHolder searchStatusHolder;
	private Locale locale;
	private Lexicon lexicon;
	private File indexDir;
	private File contentDir;

	private static final Set<String> RTL = new HashSet<String>(Arrays.asList(new String[] { "ar", "dv", "fa", "ha", "he", "iw", "ji", "ps", "ur", "yi" }));

	public SearchServiceImpl(Locale locale, File indexDir, File contentDir) {
		super();
		this.locale = locale;
		this.indexDir = indexDir;
		this.contentDir = contentDir;
	}

	@Override
	public JochreIndexBuilder getJochreIndexBuilder() {
		if (indexDir == null)
			throw new JochreSearchException("indexDir not set");
		if (contentDir == null)
			throw new JochreSearchException("contentDir not set");
		JochreIndexBuilderImpl builder = new JochreIndexBuilderImpl(indexDir, contentDir);
		builder.setSearchService(this);
		builder.setAltoService(altoService);
		builder.setFeedbackService(feedbackService);
		return builder;
	}

	@Override
	public JochreQuery getJochreQuery() {
		JochreQueryImpl query = new JochreQueryImpl();
		query.setSearchService(this);
		query.setLexiconService(this.getLexiconService());
		return query;
	}

	@Override
	public synchronized JochreIndexSearcher getJochreIndexSearcher() {
		if (this.searcher == null)
			this.searcher = this.buildSearcher();
		return this.searcher;
	}

	@Override
	public void purge() {
		this.lexicon = null;
		this.purgeSearcher();
	}

	@Override
	public synchronized void purgeSearcher() {
		JochreIndexSearcher searcher = this.buildSearcher();
		this.searcher = searcher;
	}

	private JochreIndexSearcher buildSearcher() {
		JochreIndexSearcherImpl searcher = new JochreIndexSearcherImpl(indexDir, contentDir);
		searcher.setSearchService(this);
		return searcher;
	}

	@Override
	public JochreIndexDocument getJochreIndexDocument(JochreIndexSearcher indexSearcher, int docId) {
		JochreIndexDocumentImpl doc = new JochreIndexDocumentImpl(indexSearcher, docId);
		doc.setSearchService(this);
		return doc;
	}

	@Override
	public JochreIndexDirectory getJochreIndexDirectory(File dir) {
		JochreIndexDirectoryImpl directory = new JochreIndexDirectoryImpl(contentDir, dir);
		return directory;
	}

	@Override
	public JochreIndexDocument newJochreIndexDocument(JochreIndexDirectory directory, int index, List<AltoPage> currentPages) {
		JochreIndexDocumentImpl doc = new JochreIndexDocumentImpl(directory, index, currentPages);
		doc.setSearchService(this);
		return doc;
	}

	@Override
	public Tokenizer getJochreTokeniser(TokenExtractor tokenExtractor, String fieldName) {
		JochreTokeniser jochreTokeniser = new JochreTokeniser(tokenExtractor, fieldName);
		jochreTokeniser.setSearchService(this);
		return jochreTokeniser;
	}

	@Override
	public Analyzer getJochreTextLayerAnalyzer(TokenExtractor tokenExtractor) {
		JochreTextLayerAnalyser analyser = new JochreTextLayerAnalyser(tokenExtractor);
		analyser.setSearchService(this);
		analyser.setTextNormaliser(this.lexiconService.getTextNormaliser(locale));
		return analyser;
	}

	@Override
	public Analyzer getJochreMetaDataAnalyzer() {
		JochreMetaDataAnalyser analyser = new JochreMetaDataAnalyser();
		analyser.setTextNormaliser(this.lexiconService.getTextNormaliser(locale));
		return analyser;
	}

	@Override
	public JochreQueryAnalyser getJochreQueryAnalyzer() {
		JochreQueryAnalyser analyser = new JochreQueryAnalyser();
		analyser.setSearchService(this);
		analyser.setLexicon(lexicon);
		analyser.setTextNormaliser(this.lexiconService.getTextNormaliser(locale));
		return analyser;
	}

	public AltoService getAltoService() {
		return altoService;
	}

	public void setAltoService(AltoService altoService) {
		this.altoService = altoService;
	}

	@Override
	public SearchStatusHolder getSearchStatusHolder() {
		if (searchStatusHolder == null) {
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

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public boolean isLeftToRight() {
		return !RTL.contains(this.getLocale().getLanguage());
	}

	@Override
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	@Override
	public Lexicon getLexicon() {
		return lexicon;
	}

	@Override
	public void setLexicon(Lexicon lexicon) {
		this.lexicon = lexicon;
	}

	@Override
	public TokenFilter getQueryTokenFilter(TokenStream input) {
		TokenFilter tokenFilter = null;
		if (locale.getLanguage().equals("yi") || locale.getLanguage().equals("ji")) {
			tokenFilter = new YiddishQueryTokenFilter(input);
		} else if (locale.getLanguage().equals("oc")) {
			tokenFilter = new OccitanQueryTokenFilter(input);
		}
		if (LOG.isDebugEnabled())
			LOG.debug("queryTokenFilter: " + tokenFilter);
		return tokenFilter;
	}

	@Override
	public JochreIndexWord getWord(JochreIndexDocument doc, int startOffset) {
		JochreIndexWordImpl word = new JochreIndexWordImpl(doc, startOffset);
		word.setSearchService(this);
		return word;
	}

	public FeedbackService getFeedbackService() {
		return feedbackService;
	}

	public void setFeedbackService(FeedbackService feedbackService) {
		this.feedbackService = feedbackService;
	}

	@Override
	public File getIndexDir() {
		return indexDir;
	}

	@Override
	public void setIndexDir(File indexDir) {
		this.indexDir = indexDir;
	}

	@Override
	public File getContentDir() {
		return contentDir;
	}

	@Override
	public void setContentDir(File contentDir) {
		this.contentDir = contentDir;
	}
}
