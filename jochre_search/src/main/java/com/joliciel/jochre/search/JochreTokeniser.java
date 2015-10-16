package com.joliciel.jochre.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;

import com.joliciel.jochre.search.alto.AltoPage;
import com.joliciel.jochre.search.alto.AltoString;

/**
 * A very simple "dummy" tokeniser wrapping a TokenExtractor, which is in charge of the actual tokenisation.
 * @author Assaf Urieli
 *
 */
class JochreTokeniser extends Tokenizer {
	private static final Log LOG = LogFactory.getLog(JochreTokeniser.class);
	
	private SearchServiceInternal searchService;
	
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
	private final PositionLengthAttribute posLengthAtt = addAttribute(PositionLengthAttribute.class);
	private final PayloadAttribute payloadAtt = addAttribute(PayloadAttribute.class);

	private TokenExtractor tokenExtractor;
	private List<AltoString> tokens;
	private int currentIndex = 0;
	private AltoString currentToken;
	private AltoPage currentPage;
	private List<String> currentAlternatives = null;
	private int currentAlternativeIndex = 0;
	private String fieldName;
	
	/**
	 * Constructor including the tokenExtractor, the current fieldName being analysed (for Lucene fields)
	 * and a Reader containing the input.
	 * @param tokenExtractor a place to get the tokens
	 * @param input the text field contents, ignored since we've already analysed them
	 * @return
	 */
	protected JochreTokeniser(TokenExtractor tokenExtractor, String fieldName) {
		super();
		this.tokenExtractor = tokenExtractor;
		this.fieldName = fieldName;
	}

	@Override
	public boolean incrementToken() throws IOException {
		clearAttributes();
		if (this.tokens==null) {
			this.tokens = this.tokenExtractor.findTokens(fieldName, this.input);
			this.currentIndex = 0;
		}
		
		if (currentToken!=null && currentAlternativeIndex>=currentAlternatives.size()) {
			currentToken = null;
		}
		
		if (currentToken==null && currentIndex<tokens.size()) {
			currentToken = tokens.get(currentIndex++);
			if (!currentToken.getTextLine().getTextBlock().getPage().equals(currentPage)) {
				// new page
				currentPage = currentToken.getTextLine().getTextBlock().getPage();
				if (LOG.isTraceEnabled()) {
					LOG.trace("New page: " + currentPage.getPageIndex());
				}

			}
			if (LOG.isTraceEnabled()) {
				LOG.trace(currentToken.toString());
			}
			currentAlternatives = new ArrayList<String>();
			if (currentToken.isHyphenStart() && currentToken.getHyphenatedContent()!=null) {
				currentAlternatives.add(currentToken.getHyphenatedContent());
			}
			currentAlternatives.add(currentToken.getContent());
			for (String alternative : currentToken.getAlternatives()) {
				currentAlternatives.add(alternative);
			}
			currentAlternativeIndex = 0;
		}

		if (currentToken!=null) {
			String content = currentAlternatives.get(currentAlternativeIndex++);

			// add the term itself
			termAtt.append(content);
			
			if (currentToken.isHyphenStart() && currentToken.getHyphenatedContent()!=null && currentAlternativeIndex==0) {
				posLengthAtt.setPositionLength(2);
			} else {
				posLengthAtt.setPositionLength(1);
			}
			
			if (currentAlternativeIndex<currentToken.getAlternatives().size()-1) {
				posIncrAtt.setPositionIncrement(0);
			} else {
				posIncrAtt.setPositionIncrement(1);
			}
			
			offsetAtt.setOffset(currentToken.getSpanStart(), currentToken.getSpanEnd());
			
			// store the coordinates in the payload
			JochrePayload payload = new JochrePayload(currentToken);
			payloadAtt.setPayload(payload.getBytesRef());

			return true;
		} else {
			tokens = null;
			return false;
		}
	}

	public SearchServiceInternal getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchServiceInternal searchService) {
		this.searchService = searchService;
	}
	
	
}
