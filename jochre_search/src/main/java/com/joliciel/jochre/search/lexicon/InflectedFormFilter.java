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
package com.joliciel.jochre.search.lexicon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

public class InflectedFormFilter extends TokenFilter {
  private static final Logger LOG = LoggerFactory.getLogger(InflectedFormFilter.class);

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
  public final boolean incrementToken() throws IOException {
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
