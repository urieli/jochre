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
package com.joliciel.jochre.yiddish;

import java.util.List;
import java.util.StringTokenizer;
import java.util.ArrayList;

import com.joliciel.jochre.lexicon.WordSplitter;

public class YiddishWordSplitter implements WordSplitter {
	// notice no dash in the punctuation
    private static final String PUNCTUATION = ":,.?!;*()[]{}<>—\\\"'«»|/%“„";

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
		wordText = wordText.replaceAll("\\|(.){2}\\|", "$1");
		wordText = wordText.replaceAll("\\|(..){2}\\|", "$1");
		wordText = wordText.replaceAll("\\|(...){2}\\|", "$1");
		
		// replace multiple underscores by a single underscore
		wordText = wordText.replaceAll("_++", "_");
		
		// fix other punctuation
		wordText = wordText.replaceAll("''", "\"");
		wordText = wordText.replaceAll(",,", "„");
		
		StringTokenizer tokenizer = new StringTokenizer(wordText, PUNCTUATION, true);

		String previousWord = "";
		String currentWord = null;
		String previousToken = "";
		boolean prevWasPunctuation = false;
		boolean singleQuoteFound = false;
		boolean doubleQuoteFound = false;
		
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			boolean isPunctuation = (PUNCTUATION.contains(token));
			if (token.equals("'")&&!prevWasPunctuation&&previousWord!=null) {
				// Yiddish allows a single quote inside a word
				singleQuoteFound = true;
				currentWord = previousWord;
			} else if (token.equals("\"")&&!prevWasPunctuation&&previousWord!=null) {
				// Yiddish marks abbreviations by a double quote
				doubleQuoteFound = true;
				currentWord = previousWord;
			} else if (prevWasPunctuation) {
				if (isPunctuation) {
					if (previousToken.equals("'")) {
						// previous item was single quote
						this.addResult(results, previousWord);
						currentWord = "'" + token;
						singleQuoteFound = false;
					} else if (previousToken.equals("\"")) {
						// previous item was double quote
						this.addResult(results, previousWord);
						currentWord = "\"" + token;
						doubleQuoteFound = false;
					} else if (previousToken.equals("—")) {
						// always separate out long dashes
						this.addResult(results, previousWord);
						currentWord = token;
					} else {
						// combine punctuation marks together
						currentWord = previousWord + token;
					}
				} else if (singleQuoteFound) {
					// single quote in the middle of a word
					currentWord = previousWord + "'" + token;
					singleQuoteFound = false;
				} else if (doubleQuoteFound) {
					// double quote in the middle of a word
					currentWord = previousWord + "\"" + token;
					doubleQuoteFound = false;
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
		if (singleQuoteFound)
			this.addResult(results, "'");
		if (doubleQuoteFound)
			this.addResult(results, "\"");
		return results;
	}

	private void addResult(List<String> results, String word) {
		if (word!=null && word.length()>0) {
			word = YiddishWordSplitter.standardiseWord(word);
			results.add(word);
		}
	}
	
	public static String standardiseWord(String originalWord) {
		String word = originalWord;
		// systematic replacements, including redundent melupm vov and khirik yud
		word = word.replaceAll("וּ", "ו");
		word = word.replaceAll("יִ", "י");
		word = word.replaceAll("עֶ", "ע");
		word = word.replaceAll("עֵ", "ע");
		word = word.replaceAll("אֵ", "ע");
		word = word.replaceAll("אֶ", "ע");
		word = word.replaceAll("שׁ", "ש");
		word = word.replaceAll("וֹ", "ו");
		word = word.replaceAll("\\Aת([^ּ])", "תּ$1");

		// silent 
		if (word.equals("וואו")) word = "וווּ";
		word = word.replaceAll("וואו([^ּ])", "וווּ$1");
		word = word.replaceAll("ואוו", "וּוו");
		word = word.replaceAll("וואוי", "וווי");
		word = word.replaceAll("אַא", "אַ");

		// silent ה
		word = word.replaceAll("טהו", "טו");
		word = word.replaceAll("טהאָ", "טאָ");

		// double-character fixes
		word = word.replaceAll("וי", "ױ");
		word = word.replaceAll("וו", "װ");
		word = word.replaceAll("(.)יי", "$1ײ");
		word = word.replaceAll("ייַ", "ײַ");
		if (word.startsWith("יי"))
			word = "ייִ" + word.substring(2);
		word = word.replaceAll("“", "\"");
		word = word.replaceAll("''", "\"");
		word = word.replaceAll(",,", "„");

		// apostrophes all over the place (except at the end)
		word = word.replaceAll("(.)'(.)", "$1$2");

		// adjectives with דיג  instread of דיק
		word = word.replaceAll("(.)דיג\\z", "$1דיק");
		word = word.replaceAll("(.)דיגן\\z", "$1דיקן");

//		word = YiddishWordSplitter.getEndForm(word);
		return word;
	}
	
	public static String getEndForm(String form) {
		String endForm = form;
		if (endForm.endsWith("מ")) {
			endForm = endForm.substring(0, endForm.length()-1) + "ם";
		} else if (endForm.endsWith("נ")) {
			endForm = endForm.substring(0, endForm.length()-1) + "ן";
		} else if (endForm.endsWith("צ")) {
			endForm = endForm.substring(0, endForm.length()-1) + "ץ";
		} else if (endForm.endsWith("פֿ")) {
			endForm = endForm.substring(0, endForm.length()-1) + "ף";
		} else if (endForm.endsWith("כ")) {
			endForm = endForm.substring(0, endForm.length()-1) + "ך";
		}
		
		return endForm;
		
	}
}
