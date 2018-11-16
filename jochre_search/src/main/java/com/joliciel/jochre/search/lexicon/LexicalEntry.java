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

import java.io.Serializable;

/**
 * A single lexical entry for a given string.
 * @author Assaf Urieli
 *
 */
public interface LexicalEntry extends Serializable {
  /**
   * The original text of this entry.
   */
  public String getWord();
  public void setWord(String word);
  
  /**
   * The lemma for this lexical entry.
   */
  public String getLemma();
  public void setLemma(String lemma);
  
  /**
   * The original grammatical category of this entry, using the categorisation of the lexicon.
   */
  public String getCategory();
  public void setCategory(String category);
  
  /**
   * The name of the lexicon which contained this entry.
   */
  public String getLexiconName();
  public void setLexiconName(String lexiconName);
}
