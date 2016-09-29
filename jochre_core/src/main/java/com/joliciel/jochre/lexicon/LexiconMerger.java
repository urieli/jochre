///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
 * Merges lexicons by returning the maximum frequency that any single lexicon
 * gives for a given word.
 * 
 * @author Assaf Urieli
 *
 */
public class LexiconMerger implements Lexicon {
	private List<Lexicon> lexicons = new ArrayList<Lexicon>();

	@Override
	public int getFrequency(String word) {
		int maxFrequency = 0;
		for (Lexicon lexicon : this.lexicons) {
			int frequency = lexicon.getFrequency(word);
			if (frequency > maxFrequency)
				maxFrequency = frequency;
		}
		return maxFrequency;
	}

	public List<Lexicon> getLexicons() {
		return lexicons;
	}

	public void addLexicon(Lexicon lexicon) {
		this.lexicons.add(lexicon);
	}

	@Override
	public Iterator<String> getWords() {
		final List<Lexicon> myLexicons = lexicons;
		if (lexicons.size() == 0) {
			List<String> words = new ArrayList<String>();
			return words.iterator();
		}

		return new Iterator<String>() {
			int i = 0;
			Iterator<String> iterator = lexicons.get(0).getWords();

			@Override
			public boolean hasNext() {
				while (!iterator.hasNext() && i + 1 < lexicons.size()) {
					i++;
					iterator = myLexicons.get(i).getWords();
				}
				return iterator.hasNext();
			}

			@Override
			public String next() {
				return iterator.next();
			}

			@Override
			public void remove() {
			}
		};
	}

}
