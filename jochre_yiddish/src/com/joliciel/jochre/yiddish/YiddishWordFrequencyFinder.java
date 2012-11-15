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

import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.lexicon.Lexicon;

/**
 * Given a Yiddish word in an unknown orthographic standard,
 * attempts to generate all of the possible variants of this word in standard YIVO orthography,
 * and returns the frequency of the most frequent one, as indicated by the base lexicon.
 * @author Assaf Urieli
 *
 */
public class YiddishWordFrequencyFinder implements Lexicon {
	private static final Log LOG = LogFactory.getLog(YiddishWordFrequencyFinder.class);
	Lexicon baseLexicon;
	
	// notice there is a dash in the punctuation
    private static final String PUNCTUATION = ":,.?!;*()[]{}<>—\\\"'«»|/%“„-";
	
	public YiddishWordFrequencyFinder(Lexicon baseLexicon) {
		super();
		this.baseLexicon = baseLexicon;
	}

	@Override
	public int getFrequency(String initialWord) {
		String word = YiddishWordSplitter.standardiseWord(initialWord);
		if (LOG.isTraceEnabled()) {
			LOG.trace("getFrequency for: " + initialWord + ", standardised to: " + word);
		}

		// systematic replacements for non-hebraic words
		Set<String> variants = new TreeSet<String>();
		
		// in case it's a hebraic word, we keep the initial word in the mix
		variants.add(word);
		
		// silent ה
		variants = this.addVariants(variants, "(.)עה", "$1ע");
		variants = this.addVariants(variants, "(.)יה", "$1י");
		variants = this.addVariants(variants, "(.)אַה", "$1אַ");
		variants = this.addVariants(variants, "אָה", "אָ");
		variants = this.addVariants(variants, "(.)וה", "$1ו");

		// silent א
		variants = this.addVariants(variants, "(.)יא", "$1י");
		
		// diminutives with על
		variants = this.addVariants(variants, "(.)על\\z", "$1ל");
		
		// the vowel י spelled יע
		variants = this.addVariants(variants, "(.)יע(.)", "$1י$2");

		// accusative ען instead of ן
		variants = this.addVariants(variants, "(.)ען\\z", "$1ן");
		
		// ח instead of כ
		variants = this.addVariants(variants, "ח(.)", "$1כ");
		
		// double letters
		variants = this.addVariants(variants, "סס", "ס");
		variants = this.addVariants(variants, "פּפּ", "פּ");
		variants = this.addVariants(variants, "פּפּ", "פּ");
		variants = this.addVariants(variants, "פֿפֿ", "פֿ");
		variants = this.addVariants(variants, "ננ", "נ");
		variants = this.addVariants(variants, "ממ", "מ");
		variants = this.addVariants(variants, "לל", "ל");

		// בּ instead of ב
		variants = this.addVariants(variants, "א([^ַָ])", "אַ$1");
		variants = this.addVariants(variants, "א([^ַָ])", "אָ$1");
		variants = this.addVariants(variants, "יִ", "י");
		variants = this.addVariants(variants, "פ([^ּֿ])", "פֿ$1");
		variants = this.addVariants(variants, "פ([^ּֿ])", "פּ$1");
		variants = this.addVariants(variants, "ב([^ּֿ])", "בֿ$1");
		variants = this.addVariants(variants, "ב([^ּֿ])", "בּ$1");
		variants = this.addVariants(variants, "וּ", "ו");
		
		// niqqud
		variants = this.addVariants(variants, "בּ", "ב");
		variants = this.addVariants(variants, "כֿ", "כ");

		// other typical variants
		variants = this.addVariants(variants, "דט", "ט");
		variants = this.addVariants(variants, "\\Aפֿער(.)", "פֿאַר$1");
		variants = this.addVariants(variants, "\\Aפער(.)", "פֿאַר$1");
		variants = this.addVariants(variants, "\\Aבע(.)", "באַ$1");
		variants = this.addVariants(variants, "\\Aבּע(.)", "באַ$1");

		variants = this.addVariants(variants, "ײ", "ײַ");
		variants = this.addVariants(variants, "(.)דיג\\z", "$1דיק");
		variants = this.addVariants(variants, "(.)דיגער\\z", "$1דיקער");
		variants = this.addVariants(variants, "(.)דיגע\\z", "$1דיקע");
		variants = this.addVariants(variants, "(.)דיגן\\z", "$1דיקן");

		int maxFrequency = 0;
		
		for (String variant : variants) {
			int frequency = this.baseLexicon.getFrequency(variant);
			// only count multiple occurrences if it's the exact spelling
			if (!variant.equals(initialWord)&&frequency>1)
				frequency = 1;
			if (LOG.isTraceEnabled()) {
				if (frequency > 0) {
					LOG.trace(variant + ": " + frequency);
				}
			}
			if (frequency > maxFrequency)
				maxFrequency = frequency;
		}
		
		if (maxFrequency==0) {
			// check whether word is impossible
			if (!isWordPossible(initialWord))
				maxFrequency = -1;
		}
		return maxFrequency;
	}

	boolean isWordPossible(String word) {
		boolean possible = true;
		char lastChar = ' ';
		// cannot have "langer" letters in the middle of a word
		for (int i=0;i<word.length(); i++) {
			char c = word.charAt(i);
			if (lastChar=='ם'||lastChar=='ן'||lastChar=='ך'||lastChar=='ף'||lastChar=='ץ') {
				if (PUNCTUATION.indexOf(c)<0) {
					possible = false;
					break;
				}
			}
			lastChar = c;
		}
		return possible;
	}
	
	Set<String> addVariants(Set<String> variants, String regex, String replacement) {
		Set<String> newVariants = new TreeSet<String>();
		
		for (String variant : variants) {
			newVariants.add(variant);
			newVariants.add(variant.replaceAll(regex, replacement));
		}
		return newVariants;
	}

	/**
	 * A lexicon for getting the base frequencies of any derived variant.
	 * @return
	 */
	public Lexicon getBaseLexicon() {
		return baseLexicon;
	}

	public void setBaseLexicon(Lexicon baseLexicon) {
		this.baseLexicon = baseLexicon;
	}
	
	
}
