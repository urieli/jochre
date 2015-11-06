///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Assaf Urieli
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

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;

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
	private List<JochreToken> tokens;
	private int currentIndex = 0;
	private JochreToken currentToken;
	private List<String> currentAlternatives = null;
	private int currentAlternativeIndex = 0;
	private int hyphenatedContentIndex = -1;
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

			if (LOG.isTraceEnabled()) {
				LOG.trace(currentToken.toString());
			}
			currentAlternatives = currentToken.getContentStrings();
			hyphenatedContentIndex = currentAlternatives.size();
			currentAlternatives.addAll(currentToken.getHyphenatedContentStrings());
			
			currentAlternativeIndex = 0;
		}

		if (currentToken!=null) {
			String content = currentAlternatives.get(currentAlternativeIndex++);

			// add the term itself
			termAtt.append(content);
			
			if (currentAlternativeIndex>=hyphenatedContentIndex) {
				posLengthAtt.setPositionLength(2);
			} else {
				posLengthAtt.setPositionLength(1);
			}
			
			if (currentAlternativeIndex<currentAlternatives.size()-1) {
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
