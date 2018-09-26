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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.lexicon.TextNormaliser;

/**
 * A single query sent to the searcher.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreQuery {
	private static final Logger LOG = LoggerFactory.getLogger(JochreQuery.class);
	private int decimalPlaces = 4;
	private int maxDocs = 20;
	private String queryString = null;
	private String authorQueryString = null;
	private String titleQueryString = null;
	private Query luceneQuery = null;
	private Query luceneTextQuery = null;
	private int[] docIds = null;
	private boolean expandInflections = true;
	private final JochreSearchConfig config;

	public JochreQuery(JochreSearchConfig config) {
		this.config = config;
	}

	/**
	 * The number of decimal places to display for each score. Default is 4.
	 */
	public int getDecimalPlaces() {
		return decimalPlaces;
	}

	public void setDecimalPlaces(int decimalPlaces) {
		if (this.decimalPlaces != decimalPlaces) {
			this.decimalPlaces = decimalPlaces;

		}
	}

	/**
	 * The maximum number of docs to return. Default is 20.
	 */
	public int getMaxDocs() {
		return maxDocs;
	}

	public void setMaxDocs(int maxDocs) {
		this.maxDocs = maxDocs;
	}

	/**
	 * The actual query string, as interpreted by a Lucene query parser.
	 */
	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public int[] getDocIds() {
		return docIds;
	}

	public void setDocIds(int[] docIds) {
		this.docIds = docIds;
	}

	public String getAuthorQueryString() {
		return authorQueryString;
	}

	public void setAuthorQueryString(String authorQueryString) {
		this.authorQueryString = authorQueryString;
	}

	public String getTitleQueryString() {
		return titleQueryString;
	}

	public void setTitleQueryString(String titleQueryString) {
		this.titleQueryString = titleQueryString;
	}

	/**
	 * A Lucene query corresponding to the text querystring of this Jochre query,
	 * needed for highlighting.
	 */
	public Query getLuceneTextQuery() {
		try {
			if (luceneTextQuery == null) {
				LOG.debug("Parsing query: " + this.getQueryString());
				LOG.debug("Max docs: " + this.getMaxDocs());
				LOG.debug("expandInflections: " + expandInflections);
				JochreQueryAnalyser analyzer = new JochreQueryAnalyser(config, expandInflections);
				QueryParser queryParser = new QueryParser(JochreIndexField.text.name(), analyzer);
				String queryString = this.getQueryString();
				TextNormaliser textNormaliser = TextNormaliser.getTextNormaliser(config.getLocale());
				if (textNormaliser != null)
					queryString = textNormaliser.normalise(queryString);
				luceneTextQuery = queryParser.parse(queryString);
			}
			LOG.debug(luceneTextQuery.toString());
			return luceneTextQuery;
		} catch (ParseException e) {
			LOG.error("Failed to parse lucene text query: " + this.getQueryString(), e);
			throw new JochreQueryParseException(e.getMessage());
		}
	}

	/**
	 * A Lucene query corresponding to this Jochre query.
	 */
	public Query getLuceneQuery() {
		try {
			Analyzer jochreAnalyzer = new JochreMetaDataAnalyser();
			if (luceneQuery == null) {
				Builder builder = new Builder();
				builder.add(this.getLuceneTextQuery(), Occur.MUST);
				List<Query> additionalQueries = new ArrayList<>();
				if (this.authorQueryString != null) {
					MultiFieldQueryParser authorParser = new MultiFieldQueryParser(
							new String[] { JochreIndexField.author.name(), JochreIndexField.authorLang.name() }, jochreAnalyzer);
					String queryString = authorQueryString;
					LOG.debug("authorQueryString: " + authorQueryString);
					TextNormaliser textNormaliser = TextNormaliser.getTextNormaliser(config.getLocale());
					if (textNormaliser != null)
						queryString = textNormaliser.normalise(queryString);

					Query authorQuery = authorParser.parse(queryString);
					additionalQueries.add(authorQuery);
				}
				if (this.titleQueryString != null) {
					MultiFieldQueryParser titleParser = new MultiFieldQueryParser(
							new String[] { JochreIndexField.title.name(), JochreIndexField.titleLang.name() }, jochreAnalyzer);
					String queryString = titleQueryString;
					LOG.debug("titleQueryString: " + titleQueryString);
					TextNormaliser textNormaliser = TextNormaliser.getTextNormaliser(config.getLocale());
					if (textNormaliser != null)
						queryString = textNormaliser.normalise(queryString);

					Query titleQuery = titleParser.parse(queryString);
					additionalQueries.add(titleQuery);
				}

				for (Query additionalQuery : additionalQueries) {
					// In most cases, the queryClause will return a single
					// boolean clause
					// in this case, we can simplify things
					BooleanClause booleanClause = null;
					if (additionalQuery instanceof BooleanQuery) {
						BooleanQuery clauseBooleanQuery = (BooleanQuery) additionalQuery;
						if (clauseBooleanQuery.clauses().size() == 1) {
							booleanClause = clauseBooleanQuery.clauses().get(0);
						}
					}
					if (booleanClause == null)
						builder.add(additionalQuery, Occur.MUST);
					else
						builder.add(booleanClause);
				}
				luceneQuery = builder.build();
				LOG.debug(luceneQuery.toString());
			}
			return luceneQuery;
		} catch (ParseException e) {
			LOG.error("Failed to parse author query: " + this.getAuthorQueryString() + ", or title query: " + this.getTitleQueryString(), e);
			throw new JochreQueryParseException(e.getMessage());
		}
	}

	/**
	 * Should the query be expanded to inflected froms stemming from the query's
	 * lemmas.
	 */
	public boolean isExpandInflections() {
		return expandInflections;
	}

	public void setExpandInflections(boolean expandInflections) {
		this.expandInflections = expandInflections;
	}

}
