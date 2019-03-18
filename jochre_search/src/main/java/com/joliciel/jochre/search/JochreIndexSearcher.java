package com.joliciel.jochre.search;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A reusable index searcher tied to a given index directory.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreIndexSearcher {
  private static final Logger LOG = LoggerFactory.getLogger(JochreIndexSearcher.class);

  private final IndexSearcher indexSearcher;
  private final int maxDocs;

  public JochreIndexSearcher(IndexSearcher indexSearcher, String configId) {
    this.indexSearcher = indexSearcher;
    JochreSearchConfig config = JochreSearchConfig.getInstance(configId);
    this.maxDocs = config.getMaxResults();
  }

  /**
   * Return paginated results for a query.
   * 
   * @param jochreQuery
   *          the query to run
   * @param pageNumber
   *          the page number to return
   * @param resultsPerPage
   *          results per page
   * @return a pair giving the TopDocs corresponding to the paginated results, and
   *         the total hits
   * @throws IOException
   */
  public Pair<TopDocs, Integer> search(JochreQuery jochreQuery, int pageNumber, int resultsPerPage) throws IOException {
    TopDocsCollector<? extends ScoreDoc> topDocsCollector = null;
    switch (jochreQuery.getSortBy()) {
    case Score:
      topDocsCollector = TopScoreDocCollector.create(this.maxDocs);
    case Year:
      topDocsCollector = TopFieldCollector.create(new Sort(new SortedNumericSortField(JochreIndexField.yearSort.name(),
          SortField.Type.INT, !jochreQuery.isSortAscending())), this.maxDocs, false, false, false, true);
    }

    indexSearcher.search(jochreQuery.getLuceneQuery(), topDocsCollector);
    TopDocs topDocs = topDocsCollector.topDocs(pageNumber * resultsPerPage, resultsPerPage);
    int totalHits = topDocsCollector.getTotalHits();

    if (LOG.isTraceEnabled()) {
      LOG.trace("Search results: ");
      for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
        Document doc = indexSearcher.doc(scoreDoc.doc);
        String extId = doc.get(JochreIndexField.id.name());
        LOG.trace(extId + "(docId " + scoreDoc.doc + "): " + scoreDoc.score);
      }
    }
    return Pair.of(topDocs, totalHits);
  }

  /**
   * Find all documents with an exact value for a given field.
   */
  public TopDocs search(JochreIndexField field, String value) throws IOException {
    TermQuery termQuery = new TermQuery(new Term(field.name(), value));
    TopDocs topDocs = indexSearcher.search(termQuery, this.maxDocs);
    return topDocs;
  }

  /**
   * Load a Lucene document by it's internal Lucene id.
   * 
   * @param docId
   *          the internal Lucene doc id.
   */
  public Document loadDocument(int docId) throws IOException {
    return indexSearcher.doc(docId);
  }

  /**
   * Find all documents corresponding to a given name.
   * 
   * @return a Map of lucene id and lucene document
   * @throws IOException
   */
  public Map<Integer, Document> findDocuments(String name) throws IOException {
    return this.findDocumentsInternal(name, -1);
  }

  /**
   * Find the documents for a given name and index.
   * 
   * @throws IOException
   */
  public Map<Integer, Document> findDocument(String name, int index) throws IOException {
    return this.findDocumentsInternal(name, index);
  }

  private Map<Integer, Document> findDocumentsInternal(String name, int index) throws IOException {
    Map<Integer, Document> docs = new LinkedHashMap<>();
    BooleanQuery.Builder builder = new Builder();
    TermQuery termQuery = new TermQuery(new Term(JochreIndexField.name.name(), name));
    builder.add(termQuery, Occur.MUST);
    if (index >= 0) {
      Query indexQuery = new TermQuery(new Term(JochreIndexField.sectionNumber.name(), "" + index));
      builder.add(indexQuery, Occur.MUST);
    }
    Query query = builder.build();
    LOG.debug(query.toString());
    TopDocs topDocs = indexSearcher.search(query, 200);
    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
      Document doc = indexSearcher.doc(scoreDoc.doc);
      docs.put(scoreDoc.doc, doc);
      LOG.debug("Found doc " + scoreDoc.doc + ", name: " + doc.get(JochreIndexField.name.name()) + ", section: "
          + doc.get(JochreIndexField.sectionNumber.name()));
    }
    return docs;
  }
}
