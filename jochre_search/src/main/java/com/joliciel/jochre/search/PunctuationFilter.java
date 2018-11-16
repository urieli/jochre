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

import java.io.IOException;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Filters out any tokens that contain only punctuation.
 * 
 * @author Assaf Urieli
 *
 */
public class PunctuationFilter extends FilteringTokenFilter {
  private final CharArraySet punctuation = StopFilter.makeStopSet(".", ",", ";", ":", "?", "!", "|", "(", ")", "[", "]", "{", "}", "-", "\"", "'", "*", "„",
      "“", "_", "‒", "—", "―");
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  public PunctuationFilter(TokenStream in) {
    super(in);
  }

  @Override
  protected boolean accept() throws IOException {
    return !punctuation.contains(termAtt.buffer(), 0, termAtt.length());
  }

}
