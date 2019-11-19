package com.joliciel.jochre.search.highlight;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.FuzzyTermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Matches;
import org.apache.lucene.search.MatchesIterator;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CompiledAutomaton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochrePayload;
import com.joliciel.jochre.search.JochreQuery;

/**
 * Finds the actual terms to be highlighted for a given query and a given set of
 * documents.
 * 
 * @author Assaf Urieli
 *
 */
public class LuceneQueryHighlighter implements Highlighter {
  private static final Logger LOG = LoggerFactory.getLogger(LuceneQueryHighlighter.class);

  private final JochreQuery jochreQuery;
  private final IndexSearcher indexSearcher;
  private final double docCountLog;
  private final Set<String> fields;

  // We store the TF once per term text (BytesRef) rather than per term
  // so as not to weight the same term higher for certain fields than others
  private final Map<BytesRef, Double> termLogs = new HashMap<>();

  public LuceneQueryHighlighter(JochreQuery jochreQuery, IndexSearcher indexSearcher, Set<String> fields)
      throws IOException {
    this.jochreQuery = jochreQuery;
    this.indexSearcher = indexSearcher;
    this.fields = fields;
    // add 1 to docCount to ensure even a term that's in all documents
    // gets a very very very low score
    int docFieldCount = 0;
    IndexReader reader = indexSearcher.getIndexReader();
    for (String field : fields) {
      int fieldCount = reader.getDocCount(field);
      docFieldCount += fieldCount;
    }
    docCountLog = Math.log(docFieldCount + 1);
  }

  @Override
  public Map<Integer, NavigableSet<HighlightTerm>> highlight(Set<Integer> docIds) {
    try {
      IndexReader reader = indexSearcher.getIndexReader();

      IndexReaderContext readerContext = reader.getContext();
      List<LeafReaderContext> leaves = readerContext.leaves();

      Map<Integer, NavigableSet<HighlightTerm>> termMap = new HashMap<>();
      Map<Integer, Map<String, List<HighlightPassage>>> docPassages = new HashMap<>();

      for (int docId : docIds) {
        termMap.put(docId, new TreeSet<HighlightTerm>());

        Map<String, List<HighlightPassage>> fieldPassages = new HashMap<>();
        docPassages.put(docId, fieldPassages);
        for (String field : fields) {
          fieldPassages.put(field, new ArrayList<>());
        }
      }

      Map<Integer, Document> luceneIdToLuceneDocMap = new HashMap<>();
      Map<Integer, Set<Integer>> myLeaves = new HashMap<>();
      for (int docId : docIds) {
        Document luceneDoc = indexSearcher.doc(docId);
        luceneIdToLuceneDocMap.put(docId, luceneDoc);
        int leaf = ReaderUtil.subIndex(docId, leaves);
        Set<Integer> docsPerLeaf = myLeaves.get(leaf);
        if (docsPerLeaf == null) {
          docsPerLeaf = new HashSet<>();
          myLeaves.put(leaf, docsPerLeaf);
        }
        docsPerLeaf.add(docId);
      }

      Query query = jochreQuery.getLuceneTextQuery();

      List<Weight> weights = new ArrayList<>();
      List<TermsEnumExtractor> extractors = new ArrayList<>();
      this.extractWeights(query, weights, extractors);

      for (int leaf : myLeaves.keySet()) {
        if (LOG.isTraceEnabled())
          LOG.trace("Searching leaf " + leaf);
        Set<Integer> docsPerLeaf = myLeaves.get(leaf);
        LeafReaderContext subContext = leaves.get(leaf);
        LeafReader atomicReader = subContext.reader();

        for (int docId : docsPerLeaf) {
          Map<String, List<HighlightPassage>> fieldPassages = docPassages.get(docId);
          for (Weight weight : weights) {
            Set<Term> terms = new HashSet<>();
            weight.extractTerms(terms);
            Matches matches = weight.matches(subContext, docId - subContext.docBase);
            if (matches != null) {
              for (String field : fields) {
                List<HighlightPassage> passages = fieldPassages.get(field);
                MatchesIterator iMatches = matches.getMatches(field);
                while (iMatches.next()) {
                  HighlightPassage passage = new HighlightPassage(iMatches.startOffset(), iMatches.endOffset(), terms);
                  passages.add(passage);
                }
              }
            }
          }
        }

        for (String field : fields) {
          if (LOG.isTraceEnabled())
            LOG.trace("Field : " + field);

          Terms atomicReaderTerms = atomicReader.terms(field);
          if (atomicReaderTerms == null) {
            continue; // nothing to do
          }

          for (TermsEnumExtractor extractor : extractors) {
            if (LOG.isTraceEnabled())
              LOG.trace("Matching extractor " + (extractor.getTerm() == null ? "" : extractor.getTerm().utf8ToString())
                  + " in field " + field);
            TermsEnum extractorEnum = extractor.getTermsEnum(atomicReaderTerms);
            BytesRef nextBytesRef = extractorEnum.next();
            while (nextBytesRef != null) {
              List<HighlightTerm> highlights = this.findHighlights(field, extractorEnum, subContext,
                  luceneIdToLuceneDocMap, docsPerLeaf);
              for (HighlightTerm highlightTerm : highlights) {
                termMap.get(highlightTerm.getDocId()).add(highlightTerm);
              }
              nextBytesRef = extractorEnum.next();
            }
          }

          for (int docId : docsPerLeaf) {
            List<HighlightPassage> fieldPassages = docPassages.get(docId).get(field);
            if (fieldPassages != null) {
              for (HighlightPassage passage : fieldPassages) {
                if (LOG.isTraceEnabled())
                  LOG.trace("Checking passage: " + passage);
                int termCounter = 0;
                TermsEnum termsEnum = atomicReaderTerms.iterator();
                for (Term term : passage.terms) {
                  termCounter++;
                  if (LOG.isTraceEnabled())
                    LOG.trace("Searching for term " + termCounter + ": " + term.bytes().utf8ToString() + " in field "
                        + field);

                  if (!termsEnum.seekExact(term.bytes())) {
                    continue; // term not found
                  }

                  Set<Integer> singleDocIdSet = new HashSet<>();
                  singleDocIdSet.add(docId);
                  List<HighlightTerm> highlights = this.findHighlights(field, termsEnum, subContext,
                      luceneIdToLuceneDocMap, singleDocIdSet);
                  for (HighlightTerm highlightTerm : highlights) {
                    if (highlightTerm.getStartOffset() >= passage.start
                        && highlightTerm.getEndOffset() <= passage.end) {
                      if (LOG.isTraceEnabled())
                        LOG.trace("Adding term: " + highlightTerm);
                      termMap.get(highlightTerm.getDocId()).add(highlightTerm);
                    } else {
                      if (LOG.isTraceEnabled())
                        LOG.trace("Term out of range: " + highlightTerm);
                    }
                  }
                } // next term
              }
            }
          }

        } // next field
      } // next leaf

      this.logHighlightTerms(termMap);
      return termMap;
    } catch (IOException e) {
      LOG.error("Failed find lucene highlights in docIds " + docIds, e);
      throw new RuntimeException(e);
    }
  }

  private void logHighlightTerms(Map<Integer, ? extends Set<HighlightTerm>> docTermMap) {
    if (LOG.isTraceEnabled()) {
      for (int docId : docTermMap.keySet()) {
        LOG.trace("Document: " + docId + ". Terms: " + docTermMap.get(docId));
        for (HighlightTerm term : docTermMap.get(docId)) {
          LOG.trace(term.toString() + ", " + term.getPayload().toString());
        }
      }
    }
  }

  private static final class HighlightPassage {
    final int start;
    final int end;
    final Set<Term> terms;

    public HighlightPassage(int start, int end, Set<Term> terms) {
      this.start = start;
      this.end = end;
      this.terms = terms;
    }

    @Override
    public String toString() {
      return "HighlightPassage [start=" + start + ", end=" + end + ", terms=" + terms + "]";
    }
  }

  private void extractWeights(Query query, List<Weight> weights, List<TermsEnumExtractor> extractors)
      throws IOException {
    if (query instanceof BooleanQuery) {
      for (BooleanClause clause : ((BooleanQuery) query).clauses()) {
        if (clause.getOccur() != Occur.MUST_NOT) {
          this.extractWeights(clause.getQuery(), weights, extractors);
        }
      }
    } else if (query instanceof PrefixQuery) {
      Term prefixTerm = ((PrefixQuery) query).getPrefix();
      Automaton automaton = PrefixQuery.toAutomaton(prefixTerm.bytes());
      CompiledAutomaton compiledAutomaton = new CompiledAutomaton(automaton, null, true, Integer.MAX_VALUE, true);
      extractors.add(new CompiledAutomatonTermsEnumExtractor(compiledAutomaton));
    } else if (query instanceof WildcardQuery) {
      Term wildcardTerm = ((WildcardQuery) query).getTerm();
      Automaton automaton = WildcardQuery.toAutomaton(wildcardTerm);
      CompiledAutomaton compiledAutomaton = new CompiledAutomaton(automaton);
      extractors.add(new CompiledAutomatonTermsEnumExtractor(compiledAutomaton));
    } else if (query instanceof RegexpQuery) {
      RegexpQuery regexpQuery = (RegexpQuery) query;
      Automaton automaton = regexpQuery.getAutomaton();
      if (LOG.isDebugEnabled())
        LOG.debug(automaton.toDot());
      CompiledAutomaton compiledAutomaton = new CompiledAutomaton(automaton);
      extractors.add(new CompiledAutomatonTermsEnumExtractor(compiledAutomaton));
    } else if (query instanceof FuzzyQuery) {
      extractors.add(new FuzzyQueryTermsEnumExtractor((FuzzyQuery) query));
    } else {
      if (LOG.isDebugEnabled())
        LOG.debug("Extracting weight for " + query.getClass().getName());
      weights.add(query.createWeight(indexSearcher, false, 1.0f));
    }
  }

  private interface TermsEnumExtractor {
    public TermsEnum getTermsEnum(Terms terms) throws IOException;

    public BytesRef getTerm();
  }

  private static final class CompiledAutomatonTermsEnumExtractor implements TermsEnumExtractor {
    private CompiledAutomaton compiledAutomaton;

    public CompiledAutomatonTermsEnumExtractor(CompiledAutomaton compiledAutomaton) {
      this.compiledAutomaton = compiledAutomaton;
    }

    @Override
    public TermsEnum getTermsEnum(Terms terms) throws IOException {
      return this.compiledAutomaton.getTermsEnum(terms);
    }

    @Override
    public BytesRef getTerm() {
      return this.compiledAutomaton.term;
    }
  }

  private static final class FuzzyQueryTermsEnumExtractor implements TermsEnumExtractor {
    private FuzzyQuery fuzzyQuery;

    public FuzzyQueryTermsEnumExtractor(FuzzyQuery fuzzyQuery) {
      this.fuzzyQuery = fuzzyQuery;
    }

    @Override
    public TermsEnum getTermsEnum(Terms terms) throws IOException {
      return new FuzzyTermsEnum(terms, new AttributeSource(), fuzzyQuery.getTerm(), fuzzyQuery.getMaxEdits(),
          fuzzyQuery.getPrefixLength(), fuzzyQuery.getTranspositions());
    }

    @Override
    public BytesRef getTerm() {
      return this.fuzzyQuery.getTerm().bytes();
    }
  }

  private List<HighlightTerm> findHighlights(String field, TermsEnum termsEnum, LeafReaderContext subContext,
      Map<Integer, Document> luceneIdToLuceneDocMap, Set<Integer> docsPerLeaf) throws IOException {
    List<HighlightTerm> highlights = new ArrayList<>();

    Term term = new Term(field, BytesRef.deepCopyOf(termsEnum.term()));

    PostingsEnum postingsEnum = termsEnum.postings(null,
        PostingsEnum.OFFSETS | PostingsEnum.POSITIONS | PostingsEnum.PAYLOADS);
    int relativeId = postingsEnum.nextDoc();
    while (relativeId != DocIdSetIterator.NO_MORE_DOCS) {
      int luceneId = subContext.docBase + relativeId;
      if (docsPerLeaf.contains(luceneId)) {
        // Retrieve the term frequency in the current document
        int freq = postingsEnum.freq();

        if (LOG.isTraceEnabled()) {
          LOG.trace(
              "Found " + freq + " matches for term " + term.toString() + ", luceneId " + luceneId + ", field " + field);
        }
        for (int i = 0; i < freq; i++) {
          int position = postingsEnum.nextPosition();
          int start = postingsEnum.startOffset();
          int end = postingsEnum.endOffset();

          if (LOG.isTraceEnabled())
            LOG.trace("Found match " + position + " at luceneId " + luceneId + ", field " + field + " start=" + start
                + ", end=" + end);

          BytesRef bytesRef = postingsEnum.getPayload();
          JochrePayload payload = new JochrePayload(bytesRef);
          if (LOG.isTraceEnabled())
            LOG.trace("Payload: " + payload.toString());
          double weight = this.weigh(term);
          HighlightTerm highlight = new HighlightTerm(luceneId, field, start, end, position, weight, payload);
          highlights.add(highlight);
        }
      }
      relativeId = postingsEnum.nextDoc();
    }
    return highlights;
  }

  /**
   * A term is weighed as follows: Term frequency = sum, for each field, of the
   * document count containing this term Doc frequency = sum, for each field, of
   * document count containing at least one term in this field IDF = log(docFreq)
   * - log(termFreq).
   * 
   * @throws IOException
   */
  private double weigh(Term term) throws IOException {
    double idf = 0;
    if (termLogs.containsKey(term.bytes())) {
      idf = termLogs.get(term.bytes());
    } else {
      IndexReader reader = indexSearcher.getIndexReader();
      int freq = 0;
      for (String field : fields) {
        Term fieldTerm = new Term(field, term.bytes());
        freq += reader.docFreq(fieldTerm);
      }

      double termCountLog = Math.log(freq);
      if (termCountLog == Double.NEGATIVE_INFINITY)
        termCountLog = 0;
      idf = docCountLog - termCountLog;
      BytesRef bytesRef = BytesRef.deepCopyOf(term.bytes());
      termLogs.put(bytesRef, idf);
    }

    return idf;
  }
}
