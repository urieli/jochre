package com.joliciel.jochre.search.lexicon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

public class InflectedFormFilter extends TokenFilter {
	private static final Log LOG = LogFactory.getLog(InflectedFormFilter.class);

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

	private Lexicon lexicon;
	private List<String> inflectedForms = null;
	private int index = -1;
	
	public InflectedFormFilter(TokenStream input, Lexicon lexicon) {
		super(input);
		this.lexicon = lexicon;
	}

	@Override
	public boolean incrementToken() throws IOException {
	    if (inflectedForms==null) {
		    if (!input.incrementToken())
		        return false;
		    String term = new String(termAtt.buffer(), 0, termAtt.length());
		    LOG.debug("term: " + term);
	    	Set<String> lemmas = this.lexicon.getLemmas(term);
	    	Set<String> inflectedForms = new TreeSet<>();
	    	inflectedForms.add(term);
	    	if (lemmas!=null) {
	    		for (String lemma : lemmas) {
	    			Set<String> myInflectedForms = this.lexicon.getWords(lemma);
	    			LOG.debug("lemma: " + lemma + ", words: " + myInflectedForms);
	    			inflectedForms.addAll(myInflectedForms);
	    		}
	    	}
	    	this.inflectedForms = new ArrayList<>(inflectedForms);
	    	index = 0;
	    }
	    if (index < inflectedForms.size()) {
	    	String inflectedForm = inflectedForms.get(index);
	    	termAtt.copyBuffer(inflectedForm.toCharArray(), 0, inflectedForm.length());
	    	if (index==0)
	    		posIncrAtt.setPositionIncrement(1);
	    	else
	    		posIncrAtt.setPositionIncrement(0);

	    	index++;
	    	if (index==inflectedForms.size())
	    		inflectedForms = null;
	    	return true;
	    } 

		return false;
	}

}
