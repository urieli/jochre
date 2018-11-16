///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.jochre.lexicon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A fake lexicon, which returns a frequency of 1 for every word queried.
 * 
 * @author Assaf Urieli
 *
 */
public class FakeLexicon implements Lexicon {

  @Override
  public int getFrequency(String word) {
    return 1;
  }

  @Override
  public Iterator<String> getWords() {
    List<String> words = new ArrayList<String>();
    return words.iterator();
  }

}
