package com.joliciel.jochre.search.highlight;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.PrefixTermsEnum;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CompiledAutomaton;

import com.joliciel.jochre.search.JochrePayload;
import com.joliciel.jochre.search.JochreQuery;
import com.joliciel.talismane.utils.LogUtils;


class LuceneQueryHighlighter implements Highlighter {
	private static final Log LOG = LogFactory.getLog(LuceneQueryHighlighter.class);

	JochreQuery jochreQuery;
	IndexSearcher indexSearcher;

	public LuceneQueryHighlighter(JochreQuery jochreQuery, IndexSearcher indexSearcher) {
		this.jochreQuery = jochreQuery;
		this.indexSearcher = indexSearcher;
	}

	public Map<Integer, NavigableSet<HighlightTerm>> highlight(Set<Integer> docIds, Set<String> fields) {
		try {			
			IndexReader reader = indexSearcher.getIndexReader();

			IndexReaderContext readerContext = reader.getContext();
			List<AtomicReaderContext> leaves = readerContext.leaves();

			Map<Integer,NavigableSet<HighlightTerm>> termMap = new HashMap<Integer, NavigableSet<HighlightTerm>>();

			for (int docId : docIds) {
				termMap.put(docId, new TreeSet<HighlightTerm>());
			}

			Map<Integer,Document> luceneIdToLuceneDocMap = new HashMap<Integer, Document>();
			Map<Integer,Set<Integer>> myLeaves = new HashMap<Integer, Set<Integer>>();
			for (int docId : docIds) {
				Document luceneDoc = indexSearcher.doc(docId);
				luceneIdToLuceneDocMap.put(docId, luceneDoc);
				int leaf = ReaderUtil.subIndex(docId, leaves);
				Set<Integer> docsPerLeaf = myLeaves.get(leaf);
				if (docsPerLeaf==null) {
					docsPerLeaf = new HashSet<Integer>();
					myLeaves.put(leaf, docsPerLeaf);
				}
				docsPerLeaf.add(docId);

			}
			
			Query query = jochreQuery.getLuceneQuery();
			
			Set<Term> terms = new HashSet<Term>();
			Set<TermPhrase> phrases = new HashSet<TermPhrase>();
			Set<Term> prefixes = new HashSet<Term>();
			Set<Term> wildcardTerms = new HashSet<Term>();
			List<CompiledAutomaton> automatons = new ArrayList<CompiledAutomaton>();
			
			this.extractTerms(query, terms, phrases, prefixes, wildcardTerms);
			
			Map<String,Set<Term>> fieldTerms = new HashMap<String, Set<Term>>();
			for (String field : fields) {
				fieldTerms.put(field, new HashSet<Term>());
			}
			for (Term term : terms) {
				if (fields.contains(term.field())) {
					fieldTerms.get(term.field()).add(term);
				}
			}
			for (TermPhrase phrase : phrases) {
				for (Term term : phrase.getTerms()) {
					if (fields.contains(term.field())) {
						fieldTerms.get(term.field()).add(term);
					}
				}
			}
			for (Term wildcardTerm : wildcardTerms) {
				Automaton automaton = WildcardQuery.toAutomaton(wildcardTerm);
				CompiledAutomaton compiledAutomaton = new CompiledAutomaton(automaton);
				automatons.add(compiledAutomaton);
			}
			
			// add 1 to docCount to ensure even a term that's in all documents gets a very very very low score
			int docFieldCount = 0;
			for (String field : fields) {
				int fieldCount = reader.getDocCount(field);
				docFieldCount += fieldCount;
			}
			double docCountLog = Math.log(docFieldCount+1);
			
			Map<Term,Set<HighlightTerm>> termHighlightMap = new HashMap<Term, Set<HighlightTerm>>();
			for (String field : fields) {
				for (Term term : fieldTerms.get(field)) {
					termHighlightMap.put(term, new TreeSet<HighlightTerm>());
				}
			}
			
			// We store the TF once per term text (BytesRef) rather than per term
			// so as not to weight the same term higher for certain fields than others
			Map<BytesRef,Double> termLogs = new HashMap<BytesRef, Double>();
			
			List<HighlightPassage> allHighlights = new ArrayList<HighlightPassage>();
			
			for (int leaf : myLeaves.keySet()) {
				if (LOG.isTraceEnabled())
					LOG.trace("Searching leaf " + leaf);
				Set<Integer> docsPerLeaf = myLeaves.get(leaf);
				AtomicReaderContext subContext = leaves.get(leaf);
				AtomicReader atomicReader = subContext.reader();
				
				int fieldCounter = 0;
				for (String field : fields) {
					fieldCounter++;
					if (LOG.isTraceEnabled())
						LOG.trace("Field " + fieldCounter + ": " + field);
					
					Terms atomicReaderTerms = atomicReader.terms(field);
					if (atomicReaderTerms == null) {
						continue; // nothing to do
					}
					
					TermsEnum termsEnum = atomicReaderTerms.iterator(TermsEnum.EMPTY);
					
					int termCounter = 0;
					for (Term term : fieldTerms.get(field)) {
						termCounter++;
						if (LOG.isTraceEnabled())
							LOG.trace("Searching for term " + termCounter + ": " + term.bytes().utf8ToString() + " in field " + field);
						
						if (!termsEnum.seekExact(term.bytes())) {
							continue; // term not found
						}
						
						List<HighlightPassage> highlights = this.findHighlights(field, termsEnum, subContext, luceneIdToLuceneDocMap, docsPerLeaf);
						allHighlights.addAll(highlights);
						
					} // next term
					
					for (Term prefix : prefixes) {
						TermsEnum prefixEnum = new PrefixTermsEnum(termsEnum, prefix.bytes());
						BytesRef nextBytesRef = prefixEnum.next();
						while (nextBytesRef!=null) {
							List<HighlightPassage> highlights = this.findHighlights(field, prefixEnum, subContext, luceneIdToLuceneDocMap, docsPerLeaf);
							allHighlights.addAll(highlights);
							nextBytesRef = prefixEnum.next();
						}
					}
					
					for (CompiledAutomaton automaton : automatons) {
						TermsEnum automatonEnum = automaton.getTermsEnum(atomicReaderTerms);
						BytesRef nextBytesRef = automatonEnum.next();
						while (nextBytesRef!=null) {
							List<HighlightPassage> highlights = this.findHighlights(field, automatonEnum, subContext, luceneIdToLuceneDocMap, docsPerLeaf);
							allHighlights.addAll(highlights);
							nextBytesRef = automatonEnum.next();
						}
					}

				} // next field
			} // next index leaf to search
			
			for (HighlightPassage highlight : allHighlights) {	
				double weight = this.weigh(highlight.term, fields, termLogs, docCountLog);
                if (weight>0) {
					HighlightTerm highlightTerm = new HighlightTerm(highlight.docId, highlight.field, highlight.start, highlight.end, highlight.payload);
					highlightTerm.setWeight(weight);
					highlightTerm.setPosition(highlight.position);
					
					Set<HighlightTerm> termHighlightTerms = termHighlightMap.get(highlight.term);
					if (termHighlightTerms!=null)
						termHighlightTerms.add(highlightTerm);
					
					Set<HighlightTerm> highlightTerms = termMap.get(highlight.docId);
					highlightTerms.add(highlightTerm);
                }
			}
			
			this.logHighlightTerms(termMap);
			this.removeIndependentPhraseTerms(fields, fieldTerms, terms, phrases, termHighlightMap, termMap);
			this.logHighlightTerms(termMap);
			
			return termMap;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	private void logHighlightTerms(Map<Integer,? extends Set<HighlightTerm>> docTermMap) {
		if (LOG.isTraceEnabled()) {
			for (int docId : docTermMap.keySet()) {
				LOG.trace("Document: " + docId + ". Terms: " + docTermMap.get(docId));
			}
		}
	}
	
	private List<HighlightPassage> findHighlights(String field, TermsEnum termsEnum, AtomicReaderContext subContext,
			Map<Integer,Document> luceneIdToLuceneDocMap,
			Set<Integer> docsPerLeaf) throws IOException {
		List<HighlightPassage> highlights = new ArrayList<HighlightPassage>();
		
		Term term = new Term(field, BytesRef.deepCopyOf(termsEnum.term()));
		
		DocsAndPositionsEnum docPosEnum = termsEnum.docsAndPositions(null, null, DocsAndPositionsEnum.FLAG_OFFSETS | DocsAndPositionsEnum.FLAG_PAYLOADS);
		int relativeId = docPosEnum.nextDoc();
		while (relativeId!=DocsAndPositionsEnum.NO_MORE_DOCS) {
			int luceneId = subContext.docBase + relativeId;
            if (docsPerLeaf.contains(luceneId)) {
            	//Retrieve the term frequency in the current document
                int freq=docPosEnum.freq();
                
                if (LOG.isTraceEnabled()) {
                	LOG.trace("Found " + freq + " matches for term " + term.toString() + ", luceneId " + luceneId + ", field " + field);
                }
                for(int i=0; i<freq; i++){
                    int position=docPosEnum.nextPosition();
                    int start=docPosEnum.startOffset();
                    int end=docPosEnum.endOffset();

                    if (LOG.isTraceEnabled())
                    	LOG.trace("Found match " + position + " at luceneId " + luceneId + ", field " + field + " start=" + start + ", end=" + end);
                    
                    BytesRef bytesRef = docPosEnum.getPayload();
                	JochrePayload payload = new JochrePayload(bytesRef);
                    
                    HighlightPassage highlight = new HighlightPassage(luceneId, field, term, payload, position, start, end);
                    highlights.add(highlight);
				}
            }
            relativeId = docPosEnum.nextDoc();
		}
		return highlights;
	}
	
	private static final class HighlightPassage {
		int docId;
		String field;
		JochrePayload payload;
		Term term;
		int position;
		int start;
		int end;
		
		public HighlightPassage(int docId, String field, Term term, JochrePayload payload,
				int position, int start, int end) {
			super();
			this.docId = docId;
			this.field = field;
			this.payload = payload;
			this.position = position;
			this.start = start;
			this.end = end;
			this.term = term;
		}
	}
	
	/**
	 * For any terms found only in phrases, remove highlights where the
	 * sequence of highlighted terms in a given text field doesn't match the phrase.
	 * Additionally, assigns {@link HighlightTerm#isInPhrase()} for each highlight term.
	 * @param fields all fields we care about here
	 * @param fieldTerms all terms found in each field
	 * @param terms a set of independent terms (that is, outside of phrases), got from a term query
	 * @param phrases a set of term phrases in the search query
	 * @param termHighlightMap
	 * @param docTermMap
	 * @param textTermMap
	 */
	private void removeIndependentPhraseTerms(Set<String> fields, Map<String,Set<Term>> fieldTerms,
			Set<Term> terms, Set<TermPhrase> phrases,
			Map<Term,Set<HighlightTerm>> termHighlightMap,
			Map<Integer,NavigableSet<HighlightTerm>> termMap) {
		// If there are no phrases, we have nothing to do here
		if (phrases.size()==0)
			return;
		
		// remove any highlight terms that only exist in phrases, if they don't match the phrase
		LOG.trace("Looking for independent terms only found in phrases");
		Map<Term,Set<TermPhrase>> termsInPhrases = new HashMap<Term, Set<TermPhrase>>();
		Set<Term> termsInPhrasesOnly = new HashSet<Term>();
		for (String field : fields) {
			for (Term term : fieldTerms.get(field)) {
				Set<TermPhrase> myPhrases = new HashSet<TermPhrase>();
				for (TermPhrase phrase : phrases) {
					for (Term phraseTerm : phrase.getTerms()) {
						if (term.equals(phraseTerm)) {
							myPhrases.add(phrase);
							break;
						}
					}
				}
				if (myPhrases.size()>0) {
					termsInPhrases.put(term, myPhrases);
					
					if (!terms.contains(term))
						termsInPhrasesOnly.add(term);
				}
			}
		}
		
		if (LOG.isTraceEnabled()) {
			for (Term term : termsInPhrases.keySet()) {
				LOG.trace("Term : " + term + " in phrases: " + termsInPhrases.get(term));
			}
		}
		
		Set<HighlightTerm> highlightsToRemove = new HashSet<HighlightTerm>();
		Set<HighlightTerm> highlightsToKeep = new HashSet<HighlightTerm>();
		for (Term term : termsInPhrases.keySet()) {
			if (LOG.isTraceEnabled()) LOG.trace("Searching for matches on term: " + term);
			boolean inPhrasesOnly = termsInPhrasesOnly.contains(term);
			for (HighlightTerm highlightTerm : termHighlightMap.get(term)) {
				if (LOG.isTraceEnabled()) LOG.trace(highlightTerm.toString());
				if (highlightsToKeep.contains(highlightTerm)) {
					if (LOG.isTraceEnabled()) LOG.trace("Already matched, skipping.");
					continue;
				}
				boolean foundPhraseMatch = false;
				NavigableSet<HighlightTerm> textTerms = termMap.get(highlightTerm.getDocId());
				for (TermPhrase termPhrase : termsInPhrases.get(term)) {
					if (LOG.isTraceEnabled()) LOG.trace(termPhrase.toString());
					boolean phraseMatch = true;
					int termIndex = termPhrase.getIndex(term);
					int termPos = termPhrase.getPosition(term);
					int phraseLength = termPhrase.getTerms().length;
					
					// Construct all possible descendingLists from the current term, taking into account synonyms and overlaps
					Iterator<HighlightTerm> iHighlightTerms = textTerms.headSet(highlightTerm, false).descendingIterator();
					int requiredSize = termPos + termPhrase.getSlop();
					List<List<HighlightTerm>> descendingLists = this.findHighlightNeighbors(highlightTerm, iHighlightTerms, requiredSize);
					
					// Construct all possible ascendingLists from the current term, taking into account synonyms and overlaps
					iHighlightTerms = textTerms.tailSet(highlightTerm, false).iterator();
					requiredSize = (phraseLength-termPos)-1 + termPhrase.getSlop();
					List<List<HighlightTerm>> ascendingLists = this.findHighlightNeighbors(highlightTerm, iHighlightTerms, requiredSize);
					
					if (LOG.isTraceEnabled()) {
						for (List<HighlightTerm> descendingList : descendingLists)
							LOG.trace("descendingList: " + descendingList);
						for (List<HighlightTerm> ascendingList : ascendingLists)
							LOG.trace("ascendingList: " + ascendingList);
					}
					
					// start from term and move to end
					Set<HighlightTerm> matchedTerms = new HashSet<HighlightTerm>();
					matchedTerms.add(highlightTerm);
					HighlightTerm baseTerm = highlightTerm;
					int baseTermPos = termPos;
					for (int i=termIndex+1; i<phraseLength; i++) {
						Term oneTerm = termPhrase.getTerms()[i];
						int oneTermPos = termPhrase.getPositions()[i];
						
						if (LOG.isTraceEnabled()) {
							LOG.trace("Checking " + term + " at pos " + termPos + " against " + oneTerm + " at position " + oneTermPos);
							LOG.trace("baseTerm: " + baseTerm);
						}
						
						HighlightTerm matchedTerm = null;
						for (List<HighlightTerm> ascendingList : ascendingLists) {
							for (HighlightTerm highlight : ascendingList) {
								if (termHighlightMap.get(oneTerm).contains(highlight)) {
									if (LOG.isTraceEnabled())
										LOG.trace("Found at " + highlight);
									if (checkSlop(baseTerm,baseTermPos,highlight,oneTermPos,termPhrase.getSlop())) {
										matchedTerm = highlight;
										break;
									}
								}
							}
							if (matchedTerm!=null) break;
						}
						if (matchedTerm==null && termPhrase.getSlop()>0) {
							for (List<HighlightTerm> descendingList : descendingLists) {
								for (HighlightTerm highlight : descendingList) {
									if (termHighlightMap.get(oneTerm).contains(highlight)) {
										if (LOG.isTraceEnabled())
											LOG.trace("Found at " + highlight);
										if (checkSlop(baseTerm,baseTermPos,highlight,oneTermPos,termPhrase.getSlop())) {
											matchedTerm = highlight;
											break;
										}
									}
								}
								if (matchedTerm!=null) break;
							}
						}
						if (matchedTerm==null) {
							phraseMatch=false;
							break;
						}
						matchedTerms.add(matchedTerm);
						baseTerm = matchedTerm;
						baseTermPos = oneTermPos;
					} // next term in term phrase
					
					if (!phraseMatch)
						continue;
					
					baseTerm = highlightTerm;
					baseTermPos = termPos;
					for (int i=termIndex-1; i>=0; i--) {
						Term oneTerm = termPhrase.getTerms()[i];
						int oneTermPos = termPhrase.getPositions()[i];
						
						if (LOG.isTraceEnabled()) {
							LOG.trace("Checking " + term + " at pos " + termPos + " against " + oneTerm + " at position " + oneTermPos);
							LOG.trace("baseTerm: " + baseTerm);
						}

						HighlightTerm matchedTerm = null;
						for (List<HighlightTerm> descendingList : descendingLists) {
							for (HighlightTerm highlight : descendingList) {
								if (termHighlightMap.get(oneTerm).contains(highlight)) {
									if (LOG.isTraceEnabled())
										LOG.trace("Found at " + highlight);
									if (checkSlop(highlight,oneTermPos,baseTerm,baseTermPos,termPhrase.getSlop())) {
										matchedTerm = highlight;
										break;
									}
								}
							}
							if (matchedTerm!=null) break;
						}
						if (matchedTerm==null && termPhrase.getSlop()>0) {
							for (List<HighlightTerm> ascendingList : ascendingLists) {
								for (HighlightTerm highlight : ascendingList) {
									if (termHighlightMap.get(oneTerm).contains(highlight)) {
										if (LOG.isTraceEnabled())
											LOG.trace("Found at " + highlight);
										if (checkSlop(highlight,oneTermPos,baseTerm,baseTermPos,termPhrase.getSlop())) {
											matchedTerm = highlight;
											break;
										}
									}
								}
								if (matchedTerm!=null) break;
							}
						}
						if (matchedTerm==null) {
							phraseMatch=false;
							break;
						}
						matchedTerms.add(matchedTerm);
						baseTerm = matchedTerm;
						baseTermPos = oneTermPos;
					} // next term in term phrase
					
					// found a matching phrase
					if (phraseMatch) {
						if (LOG.isTraceEnabled()) LOG.trace("Found phrase match: " + matchedTerms);
						highlightsToKeep.addAll(matchedTerms);
						for (HighlightTerm matchedTerm : matchedTerms) {
							matchedTerm.setInPhrase(true);
						}
						foundPhraseMatch = true;
						break;
					}
				} // next term phrase containing this term
				if (!foundPhraseMatch && inPhrasesOnly) {
					if (LOG.isTraceEnabled()) LOG.trace("No phrase match, removing highlight: " + highlightTerm);
					highlightsToRemove.add(highlightTerm);
				}
			} // next highlight term corresponding to a term only contained in phrases
		} // next term only contained in phrases
		
		// remove any highlights that were only in phrases and for which no phrase match was found
		for (HighlightTerm highlight : highlightsToRemove) {
			termMap.get(highlight.getDocId()).remove(highlight);
		}
		
	}
	
	/**
	 * Return all possible descending or ascending lists (depending on the direction of the iterator)
	 * starting at a given highlight term (but excluding it)
	 * and all having a given requiredSize.
	 * If we run across any overlapping terms, we create multiple lists, one per overlap.
	 * @param highlightTerm
	 * @param iHighlightTerms
	 * @param requiredSize
	 * @return
	 */
	private List<List<HighlightTerm>> findHighlightNeighbors(HighlightTerm highlightTerm, Iterator<HighlightTerm> iHighlightTerms, int requiredSize) {
		List<List<HighlightTerm>> descendingLists = new ArrayList<List<HighlightTerm>>();
		descendingLists.add(new ArrayList<HighlightTerm>());
		List<List<HighlightTerm>> finalLists = new ArrayList<List<HighlightTerm>>();

		while (iHighlightTerms.hasNext()) {
			// terminating condition: all descendingLists have the required size
			for (List<HighlightTerm> descendingList : descendingLists) {
				if (descendingList.size()>=requiredSize) {
					finalLists.add(descendingList);
				}
			}
			descendingLists.removeAll(finalLists);
			if (descendingLists.size()==0) {
				break;
			}
			
			List<List<HighlightTerm>> newLists = new ArrayList<List<HighlightTerm>>();
			HighlightTerm prevTerm = iHighlightTerms.next();
			if (prevTerm.hasOverlap(highlightTerm)) {
				// if this term surrounds the highlightTerm, we don't want it.
				continue;
			}
			for (List<HighlightTerm> descendingList : descendingLists) {
				boolean foundOverlap = false;
				for (int i=0; i<descendingList.size(); i++) {
					HighlightTerm oneTerm = descendingList.get(i);
					if (prevTerm.hasOverlap(oneTerm)) {
						// we have an overlap - need to add two new lists, one with the overlap, and one without it
						List<HighlightTerm> newList = new ArrayList<HighlightTerm>(i);
						for (int j=0; j<i; j++) newList.add(descendingList.get(j));
						newList.add(prevTerm);
						newLists.add(newList);
						List<HighlightTerm> newList2 = new ArrayList<HighlightTerm>(descendingList);
						newLists.add(newList2);
						foundOverlap = true;
						break;
					}
				}
				if (!foundOverlap) {
					List<HighlightTerm> newList = new ArrayList<HighlightTerm>(descendingList);
					newList.add(prevTerm);
					newLists.add(newList);
				}
			}
			descendingLists = newLists;
		}
		
		for (List<HighlightTerm> descendingList : descendingLists) {
			finalLists.add(descendingList);
		}
		return finalLists;
	}
	
	/**
	 * For two terms both of which appear in an enclosing phrase,
	 * based on the real distance between baseTerm and otherTerm in the text,
	 * check if the difference between the real distance and the required distance is covered by the slop.
	 * @param baseTerm the base term
	 * @param baseTermRequiredPos the base term's position in the enclosing phrase
	 * @param otherTerm the other term
	 * @param otherTermRequiredPos the other term's position in the enclosing phrase
	 * @param slop how much slop is allowed in the difference between the two.
	 * @return
	 */
	private boolean checkSlop(HighlightTerm baseTerm, int baseTermRequiredPos, HighlightTerm otherTerm, int otherTermRequiredPos, int slop) {
		boolean valid = true;
		int requiredDistance = otherTermRequiredPos - baseTermRequiredPos;
		int distance = otherTerm.getPosition()-baseTerm.getPosition();
		int difference = Math.abs(requiredDistance-distance);
		if (sameSign(requiredDistance, distance)) {	
			// if the required distance is 1, but distance is 1, then slop >= 0
			// if the required distance is 2, but distance is 1, then slop >= 1
			// if the required distance is 1, but distance is 2, then slop >= 1
			// if the required distance is 1, but distance is 3, then slop >= 2
			
			// do nothing
		} else {
			// order inversion takes one edit distance

			// if the required distance is 1, but distance is -1, then slop >= 1
			// if the required distance is -1, but distance is 1, then slop >= 1
			// if the required distance is 2, but distance is -1, then slop >= 2
			// if the required distance is 1, but distance is -2, then slop >= 2
			
			difference -= 1;
		}
		valid = difference <= slop;

		if (!valid && LOG.isTraceEnabled()) {
			LOG.trace("requiredDistance: " + requiredDistance + ", distance: " + distance + ", difference: " + difference + ", baseTerm: " + baseTerm + ", otherTerm: " + otherTerm);
		}
		return valid;
	}
	
	private boolean sameSign(int x, int y) {
		return ((x<0) == (y<0)); 
	}
	
	/**
	 * A term is weighed as follows:
	 * Term frequency = sum, for each field, of the document count containing this term
	 * Doc frequency = sum, for each field, of document count containing at least one term in this field
	 * IDF = log(docFreq) - log(termFreq).
	 * @param term
	 * @param fields
	 * @param termLogs
	 * @param docCountLog
	 * @return
	 * @throws IOException
	 */
	private double weigh(Term term, Set<String> fields, Map<BytesRef,Double> termLogs, double docCountLog) throws IOException {
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
			if (termCountLog==Double.NEGATIVE_INFINITY)
				termCountLog = 0;
			idf = docCountLog - termCountLog;
			BytesRef bytesRef = BytesRef.deepCopyOf(term.bytes());
			termLogs.put(bytesRef, idf);
		}
		
		return idf;
	}
	
	private void extractTerms(Query query, Set<Term> terms, Set<TermPhrase> phrases, Set<Term> prefixes, Set<Term> wildcardTerms) {
		if (query instanceof PhraseQuery) {
			TermPhrase termPhrase = new TermPhrase(((PhraseQuery) query).getTerms(), ((PhraseQuery) query).getPositions(), ((PhraseQuery) query).getSlop());
			phrases.add(termPhrase);
		} else if (query instanceof PrefixQuery) {
			prefixes.add(((PrefixQuery) query).getPrefix());
		} else if (query instanceof TermQuery) {
			terms.add(((TermQuery) query).getTerm());
		} else if (query instanceof WildcardQuery) {
			wildcardTerms.add(((WildcardQuery) query).getTerm());
		} else if (query instanceof BooleanQuery) {
			for (BooleanClause clause : ((BooleanQuery) query).getClauses()) {
				if (clause.getOccur() != Occur.MUST_NOT) {
					this.extractTerms(clause.getQuery(), terms, phrases, prefixes, wildcardTerms);
				}
			}
		}
	}
	
	private static final class TermPhrase {
		Term[] terms;
		int[] positions;
		
		int slop;
		
		public TermPhrase(Term[] terms, int[] positions, int slop) {
			super();
			this.terms = terms;
			this.positions = positions;
			this.slop = slop;
		}
		
		public Term[] getTerms() {
			return terms;
		}

		public int getSlop() {
			return slop;
		}
		
		public int getIndex(Term term) {
			int i=-1;
			for (i=0; i<terms.length; i++) {
				if (terms[i].equals(term)) {
					break;
				}
			}
			return i;
		}
		
		public int getPosition(Term term) {
			int i=this.getIndex(term);
			return this.positions[i];
		}

		public int[] getPositions() {
			return positions;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + slop;
			result = prime * result + Arrays.hashCode(terms);
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TermPhrase other = (TermPhrase) obj;
			if (slop != other.slop)
				return false;
			if (!Arrays.equals(terms, other.terms))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "TermPhrase [terms=" + Arrays.toString(terms) + ", slop="
					+ slop + "]";
		}
		
		
	}
	
}
