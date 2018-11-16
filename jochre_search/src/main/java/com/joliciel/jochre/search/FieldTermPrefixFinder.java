package com.joliciel.jochre.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CompiledAutomaton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.lexicon.TextNormaliser;

/**
 * Finds the top-N most frequent terms in a set of fields matching a certain
 * prefix. Frequency is the number of documents in which the term appears.
 * Results are sorted alphabetically.
 * 
 * @author Assaf Urieli
 *
 */
public class FieldTermPrefixFinder {
  private static final Logger LOG = LoggerFactory.getLogger(FieldTermPrefixFinder.class);
  final private List<String> results;

  public FieldTermPrefixFinder(IndexSearcher indexSearcher, String field, String prefix, int maxResults, JochreSearchConfig config) throws IOException {
    TextNormaliser textNormaliser = TextNormaliser.getInstance(config);
    if (textNormaliser != null) {
      prefix = textNormaliser.normalise(prefix);
    }
    Automaton prefixAut = PrefixQuery.toAutomaton(new Term(field, prefix).bytes());
    CompiledAutomaton automaton = new CompiledAutomaton(prefixAut, null, true, Integer.MAX_VALUE, true);

    IndexReader reader = indexSearcher.getIndexReader();

    IndexReaderContext readerContext = reader.getContext();
    List<LeafReaderContext> leaves = readerContext.leaves();

    Map<BytesRef, Integer> counter = new HashMap<>();
    Map<BytesRef, LeafReaderContext> leafMap = new HashMap<>();

    for (LeafReaderContext leaf : leaves) {
      LeafReader leafReader = leaf.reader();
      Terms terms = leafReader.terms(field);
      if (terms != null) {
        TermsEnum termsEnum = automaton.getTermsEnum(terms);
        BytesRef bytesRef = null;
        while ((bytesRef = termsEnum.next()) != null) {
          if (!counter.containsKey(bytesRef)) {
            Term term = new Term(field, bytesRef);
            int count = reader.docFreq(term);
            BytesRef copy = BytesRef.deepCopyOf(bytesRef);
            counter.put(copy, count);
            leafMap.put(copy, leaf);
          }
        }
      }
    }

    // Sort by descending frequency
    List<BytesRef> list = new ArrayList<>(counter.keySet());
    Collections.sort(list, new Comparator<BytesRef>() {
      @Override
      public int compare(BytesRef x, BytesRef y) {
        return counter.get(y) - counter.get(x);
      }
    });

    // Limit to top N
    List<BytesRef> maxSizeList = list;
    if (maxResults > 0 && list.size() > maxResults)
      maxSizeList = list.subList(0, maxResults);

    // new we need to read the text actually stored in the documents,
    // not the text normalised into a search term
    // which might be lowercase or not have accents
    Set<String> fieldsToLoad = new HashSet<>();
    fieldsToLoad.add(field);

    List<String> prettyResults = new ArrayList<>(maxSizeList.size());
    for (BytesRef bytesRef : maxSizeList) {
      if (LOG.isDebugEnabled())
        LOG.debug("Searching for term: " + bytesRef.utf8ToString());

      LeafReaderContext leaf = leafMap.get(bytesRef);
      LeafReader leafReader = leaf.reader();
      Terms terms = leafReader.terms(field);
      TermsEnum termsEnum = terms.iterator();
      if (!termsEnum.seekExact(bytesRef)) {
        // term not found
        LOG.debug("Term not found");
        prettyResults.add(bytesRef.utf8ToString());
        continue;
      }
      PostingsEnum docPosEnum = termsEnum.postings(null);
      int relativeId = docPosEnum.nextDoc();
      if (relativeId != PostingsEnum.NO_MORE_DOCS) {
        int nextId = leaf.docBase + relativeId;
        Document doc = reader.document(nextId, fieldsToLoad);
        prettyResults.add(doc.get(field));
        if (LOG.isDebugEnabled())
          LOG.debug("Term found in document " + nextId + ": " + doc.get(field));
      } else {
        LOG.debug("Term not found");
        prettyResults.add(bytesRef.utf8ToString());
      }
    }

    // sort alphabetically
    Collections.sort(prettyResults);
    results = prettyResults;
  }

  public List<String> getResults() {
    return results;
  }

}
