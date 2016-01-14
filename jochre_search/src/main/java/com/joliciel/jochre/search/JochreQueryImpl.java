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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import com.joliciel.jochre.search.lexicon.LexiconService;
import com.joliciel.jochre.search.lexicon.TextNormaliser;
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.utils.LogUtils;

class JochreQueryImpl implements JochreQuery {
	private static final Log LOG = LogFactory.getLog(JochreQueryImpl.class);
	private int decimalPlaces = 4;
	private int maxDocs = 20;
	private String queryString = null;
	private String authorQueryString = null;
	private String titleQueryString = null;
	private Query luceneQuery = null;
	private Query luceneTextQuery = null;
	private int[] docIds = null;
	private boolean expandInflections = true;
	
	private SearchServiceInternal searchService;
	private LexiconService lexiconService;

	public JochreQueryImpl() {}
	
	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#getDecimalPlaces()
	 */
	@Override
	public int getDecimalPlaces() {
		return decimalPlaces;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#setDecimalPlaces(int)
	 */
	@Override
	public void setDecimalPlaces(int decimalPlaces) {
		if (this.decimalPlaces!=decimalPlaces) {
			this.decimalPlaces = decimalPlaces;

		}
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#getMaxDocs()
	 */
	@Override
	public int getMaxDocs() {
		return maxDocs;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#setMaxDocs(int)
	 */
	@Override
	public void setMaxDocs(int maxDocs) {
		this.maxDocs = maxDocs;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#getQueryString()
	 */
	@Override
	public String getQueryString() {
		return queryString;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#setQueryString(java.lang.String)
	 */
	@Override
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public int[] getDocIds() {
		return docIds;
	}

	public void setDocIds(int[] docIds) {
		this.docIds = docIds;
	}
	
	@Override
	public String getAuthorQueryString() {
		return authorQueryString;
	}

	@Override
	public void setAuthorQueryString(String authorQueryString) {
		this.authorQueryString = authorQueryString;
	}

	@Override
	public String getTitleQueryString() {
		return titleQueryString;
	}

	@Override
	public void setTitleQueryString(String titleQueryString) {
		this.titleQueryString = titleQueryString;
	}

	public Query getLuceneTextQuery() {
		try {
			if (luceneTextQuery==null) {
				LOG.debug("Parsing query: " + this.getQueryString());
				LOG.debug("Max docs: " + this.getMaxDocs());
				JochreQueryAnalyser analyzer = searchService.getJochreQueryAnalyzer();
				analyzer.setExpandInflections(expandInflections);
				QueryParser queryParser = new QueryParser(JochreIndexField.text.name(), analyzer);
				String queryString = this.getQueryString();
				TextNormaliser textNormaliser = lexiconService.getTextNormaliser(searchService.getLocale());
				if (textNormaliser!=null)
					queryString = textNormaliser.normalise(queryString);
				luceneTextQuery = queryParser.parse(queryString);
			}
			LOG.info(luceneTextQuery.toString());
			return luceneTextQuery;
		} catch (ParseException pe) {
			LogUtils.logError(LOG, pe);
			throw new JochreException(pe);
		}
	}
	
	@Override
	public Query getLuceneQuery() {
		try {
			Analyzer jochreAnalyzer = searchService.getJochreMetaDataAnalyzer();
			if (luceneQuery==null) {
				Builder builder = new Builder();
				builder.add(this.getLuceneTextQuery(), Occur.MUST);
				List<Query> additionalQueries = new ArrayList<>(); 
				if (this.authorQueryString!=null) {
					MultiFieldQueryParser authorParser = new MultiFieldQueryParser(new String[] {JochreIndexField.author.name(),  JochreIndexField.authorLang.name()}, jochreAnalyzer);
					String queryString = authorQueryString;
					TextNormaliser textNormaliser = lexiconService.getTextNormaliser(searchService.getLocale());
					if (textNormaliser!=null)
						queryString = textNormaliser.normalise(queryString);

					Query authorQuery = authorParser.parse(queryString);
					additionalQueries.add(authorQuery);
				}
				if (this.titleQueryString!=null) {
					MultiFieldQueryParser titleParser = new MultiFieldQueryParser(new String[] {JochreIndexField.title.name(),  JochreIndexField.titleLang.name()}, jochreAnalyzer);
					String queryString = titleQueryString;
					TextNormaliser textNormaliser = lexiconService.getTextNormaliser(searchService.getLocale());
					if (textNormaliser!=null)
						queryString = textNormaliser.normalise(queryString);

					Query titleQuery = titleParser.parse(queryString);
					additionalQueries.add(titleQuery);
				}
				
				for (Query additionalQuery : additionalQueries) {
					// In most cases, the queryClause will return a single boolean clause
					// in this case, we can simplify things
					BooleanClause booleanClause = null;
					if (additionalQuery instanceof BooleanQuery) {
						BooleanQuery clauseBooleanQuery = (BooleanQuery) additionalQuery;
						if (clauseBooleanQuery.clauses().size()==1) {
							booleanClause = clauseBooleanQuery.clauses().get(0);
						}
					}
					if (booleanClause==null)
						builder.add(additionalQuery, Occur.MUST);
					else
						builder.add(booleanClause);
				}
				luceneQuery = builder.build();
				LOG.info(luceneQuery.toString());
			}
			return luceneQuery;
		} catch (ParseException pe) {
			LogUtils.logError(LOG, pe);
			throw new JochreException(pe);
		}
	}

	public SearchServiceInternal getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchServiceInternal searchService) {
		this.searchService = searchService;
	}

	public LexiconService getLexiconService() {
		return lexiconService;
	}

	public void setLexiconService(LexiconService lexiconService) {
		this.lexiconService = lexiconService;
	}

	public boolean isExpandInflections() {
		return expandInflections;
	}

	public void setExpandInflections(boolean expandInflections) {
		this.expandInflections = expandInflections;
	}

	
}
