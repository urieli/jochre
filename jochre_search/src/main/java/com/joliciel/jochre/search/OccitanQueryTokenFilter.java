///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;

/**
 * Called only when analysing search queries (not documents),
 * to separate on apostrophes.
 * @author Assaf Urieli
 *
 */
class OccitanQueryTokenFilter extends TokenFilter {
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
	private final PositionLengthAttribute posLengthAtt = addAttribute(PositionLengthAttribute.class);
	
	private String leftoverTerm = null;
	private int previousEndOffset = -1;
	
	public OccitanQueryTokenFilter(TokenStream input) {
		super(input);
	}

	@Override
	public final boolean incrementToken() throws IOException {
		if (leftoverTerm!=null) {
			clearAttributes();
			termAtt.copyBuffer(leftoverTerm.toCharArray(), 0, leftoverTerm.length());
			posIncrAtt.setPositionIncrement(1);
			offsetAtt.setOffset(previousEndOffset, previousEndOffset+leftoverTerm.length());
			posLengthAtt.setPositionLength(1);
			leftoverTerm=null;
			return true;
		} else if (input.incrementToken()) {
			String term = new String(termAtt.buffer(), 0, termAtt.length());
			int aposPos = term.indexOf('\'');
			if (aposPos>0 && aposPos<term.length()-1) {
				// need to separate
				int startOffset = offsetAtt.startOffset();
			    clearAttributes();
				posIncrAtt.setPositionIncrement(1);
				posLengthAtt.setPositionLength(1);
				String term1 = term.substring(0, aposPos+1);
				leftoverTerm = term.substring(aposPos+1).replace("'", "");
				termAtt.copyBuffer(term1.toCharArray(), 0, term1.length());
				offsetAtt.setOffset(startOffset, startOffset+term1.length());
				previousEndOffset = offsetAtt.endOffset();
				return true;
			} else {
				return true;
			}
		}
		return false;
	}

}
