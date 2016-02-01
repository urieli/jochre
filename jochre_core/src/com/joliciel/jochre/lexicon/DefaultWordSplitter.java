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

import java.util.List;
import java.util.StringTokenizer;
import java.util.ArrayList;

import com.joliciel.jochre.lexicon.WordSplitter;

public class DefaultWordSplitter implements WordSplitter {
	// notice no dash or single quote in the punctuation
    private static final String PUNCTUATION = ":,.?!;*()[]{}<>—\\\"«»|/%“„";

	@Override
	public List<String> splitText(String wordText) {
		List<String> results = new ArrayList<String>();
		
		if (wordText.length()==0) {
			results.add("");
			return results;
		}
		
		// all numerals are treated identically
		wordText = wordText.replaceAll("[0-9]", "0");
		
		// split letters are joined back together
		wordText = wordText.replaceAll("\\|(.)\\1\\|", "$1");
		wordText = wordText.replaceAll("\\|(..)\\1\\|", "$1");
		wordText = wordText.replaceAll("\\|(...)\\1\\|", "$1");
		
		// replace multiple underscores by a single underscore
		wordText = wordText.replaceAll("_++", "_");
		
		// fix other punctuation
		wordText = wordText.replaceAll("''", "\"");
		
		StringTokenizer tokenizer = new StringTokenizer(wordText, PUNCTUATION, true);

		String previousWord = "";
		String currentWord = null;
		String previousToken = "";
		boolean prevWasPunctuation = false;
		
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			boolean isPunctuation = (PUNCTUATION.contains(token));
			
			if (prevWasPunctuation) {
				if (isPunctuation) {
					if (previousToken.equals("—")) {
						// always separate out long dashes
						this.addResult(results, previousWord);
						currentWord = token;
					} else {
						// combine punctuation marks together
						currentWord = previousWord + token;
					}
				} else {
					this.addResult(results, previousWord);
					currentWord = token;
				}
			} else {
				// the last word wasn't punctuation
				// so it has to be dealt with separately
				this.addResult(results, previousWord);
				currentWord = token;
			}
			
			prevWasPunctuation = isPunctuation;
			previousWord = currentWord;
			previousToken = token;
		}
		this.addResult(results, previousWord);

		return results;
	}

	private void addResult(List<String> results, String word) {
		if (word!=null && word.length()>0) {
			results.add(word);
		}
	}
}
