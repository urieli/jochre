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
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
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
  public enum SortBy {
    Score, Year
  }

  private static final Logger LOG = LoggerFactory.getLogger(JochreQuery.class);
  private final String queryString;
  private final List<String> authors;
  private final boolean authorInclude;
  private final String titleQueryString;
  private Query luceneQuery = null;
  private Query luceneTextQuery = null;
  private int[] docIds = null;
  private final boolean expandInflections;
  private final JochreSearchConfig config;
  private final SortBy sortBy;
  private final boolean sortAscending;
  private final Integer fromYear;
  private final Integer toYear;
  private final String reference;

  public JochreQuery(JochreSearchConfig config, String queryString) {
    this(config, queryString, new ArrayList<>(), true, "", null, null, true, null);
  }

  public JochreQuery(JochreSearchConfig config, String queryString, List<String> authors, boolean authorInclude,
      String titleQueryString, Integer fromYear, Integer toYear, boolean expandInflections, String reference) {
    this(config, queryString, authors, authorInclude, titleQueryString, fromYear, toYear, expandInflections,
        SortBy.Score, true, reference);
  }

  public JochreQuery(JochreSearchConfig config, String queryString, List<String> authors, boolean authorInclude,
      String titleQueryString, Integer fromYear, Integer toYear, boolean expandInflections, SortBy sortBy,
      boolean sortAscending, String reference) {
    this.config = config;
    this.queryString = queryString;
    this.authors = authors;
    this.authorInclude = authorInclude;
    this.titleQueryString = titleQueryString;
    this.fromYear = fromYear;
    this.toYear = toYear;
    this.expandInflections = expandInflections;
    this.sortBy = sortBy;
    this.sortAscending = sortAscending;
    this.reference = reference;
  }

  /**
   * The actual query string, as interpreted by a Lucene query parser.
   */
  public String getQueryString() {
    return queryString;
  }

  /**
   * A list of authors for inclusion or exclusion.
   */
  public List<String> getAuthors() {
    return authors;
  }

  /**
   * If true and {@link #getAuthors()} is not empty, the query is limited to
   * authors in the list. If false, the query excludes authors in the list.
   */
  public boolean isAuthorInclude() {
    return authorInclude;
  }

  public int[] getDocIds() {
    return docIds;
  }

  public void setDocIds(int[] docIds) {
    this.docIds = docIds;
  }

  public String getTitleQueryString() {
    return titleQueryString;
  }

  /**
   * A Lucene query corresponding to the text querystring of this Jochre query,
   * needed for highlighting.
   */
  public Query getLuceneTextQuery() {
    try {
      if (luceneTextQuery == null) {
        if (this.queryString == null || this.queryString.length() == 0) {
          throw new JochreSearchException("Cannot get a text query without a querystring.");
        }
        LOG.debug("Parsing query: " + this.getQueryString());
        LOG.debug("expandInflections: " + expandInflections);
        JochreQueryAnalyser analyzer = new JochreQueryAnalyser(config, expandInflections);
        QueryParser queryParser = new QueryParser(JochreIndexField.text.name(), analyzer);
        String queryString = this.getQueryString();
        TextNormaliser textNormaliser = TextNormaliser.getInstance(config);
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
      Analyzer jochreAnalyzer = new JochreMetaDataAnalyser(config);
      if (luceneQuery == null) {
        Builder builder = new Builder();
        boolean hasQuery = false;
        if (this.queryString != null && this.queryString.length() > 0) {
          builder.add(this.getLuceneTextQuery(), Occur.MUST);
          hasQuery = true;
        }
        if (this.authors.size() > 0) {
          Builder authorBuilder = new Builder();
          TextNormaliser textNormaliser = TextNormaliser.getInstance(config);

          for (String author : authors) {
            String authorString = author;
            if (textNormaliser != null)
              authorString = textNormaliser.normalise(author);
            TermQuery termQuery = new TermQuery(new Term(JochreIndexField.author.name(), authorString));
            authorBuilder.add(new BooleanClause(termQuery, Occur.SHOULD));
            TermQuery termQuery2 = new TermQuery(new Term(JochreIndexField.authorEnglish.name(), authorString));
            authorBuilder.add(new BooleanClause(termQuery2, Occur.SHOULD));
          }

          BooleanQuery authorQuery = authorBuilder.build();
          BooleanClause authorClause = new BooleanClause(authorQuery, this.authorInclude ? Occur.MUST : Occur.MUST_NOT);
          builder.add(authorClause);
          hasQuery = true;
        }
        if (this.titleQueryString != null && this.titleQueryString.length() > 0) {
          MultiFieldQueryParser titleParser = new MultiFieldQueryParser(
              new String[] { JochreIndexField.titleEnglish.name(), JochreIndexField.title.name() }, jochreAnalyzer);
          String queryString = titleQueryString;
          LOG.debug("titleQueryString: " + titleQueryString);
          TextNormaliser textNormaliser = TextNormaliser.getInstance(config);
          if (textNormaliser != null)
            queryString = textNormaliser.normalise(queryString);

          Query titleQuery = titleParser.parse(queryString);
          builder.add(titleQuery, Occur.MUST);
          hasQuery = true;
        }
        if (this.fromYear != null || this.toYear != null) {
          Query yearQuery = IntPoint.newRangeQuery(JochreIndexField.year.name(),
              this.fromYear == null ? Integer.MIN_VALUE : this.fromYear.intValue(),
              this.toYear == null ? Integer.MAX_VALUE : this.toYear.intValue());
          builder.add(new BooleanClause(yearQuery, Occur.MUST));
          hasQuery = true;
        }
        if (this.reference != null && this.reference.length() > 0) {
          TermQuery refQuery = new TermQuery(new Term(JochreIndexField.id.name(), reference));
          builder.add(new BooleanClause(refQuery, Occur.MUST));
          hasQuery = true;
        }
        if (hasQuery)
          luceneQuery = builder.build();
        else
          luceneQuery = new MatchNoDocsQuery();
        if (LOG.isDebugEnabled())
          LOG.debug(luceneQuery.toString());
      }
      return luceneQuery;
    } catch (ParseException e) {
      LOG.error("Failed to parse title query: " + this.getTitleQueryString(), e);
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

  public Integer getFromYear() {
    return fromYear;
  }

  public Integer getToYear() {
    return toYear;
  }

  public SortBy getSortBy() {
    return sortBy;
  }

  public boolean isSortAscending() {
    return sortAscending;
  }

  public String getReference() {
    return reference;
  }

  /**
   * Can this query return highlights inside the books.
   */
  public boolean hasHighlights() {
    return this.queryString != null && this.queryString.length() > 0;
  }
}
