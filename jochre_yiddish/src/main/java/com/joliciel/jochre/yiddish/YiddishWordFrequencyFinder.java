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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.lexicon.Lexicon;
import com.joliciel.talismane.utils.CountedOutcome;

/**
 * Given a Yiddish word in an unknown orthographic standard, attempts to
 * generate all of the possible variants of this word in standard YIVO
 * orthography, and returns the frequency of the most frequent one, as indicated
 * by the base lexicon.
 * 
 * @author Assaf Urieli
 *
 */
public class YiddishWordFrequencyFinder implements Lexicon {
	private static final Logger LOG = LoggerFactory.getLogger(YiddishWordFrequencyFinder.class);
	private static final Pattern NUMBER = Pattern.compile("\\d+");
	private final Lexicon baseLexicon;
	private final JochreSession jochreSession;

	public YiddishWordFrequencyFinder(Lexicon baseLexicon, JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.baseLexicon = baseLexicon;
	}

	@Override
	public int getFrequency(String word) {
		List<CountedOutcome<String>> frequencies = this.getFrequencies(word);
		if (frequencies.size() == 0)
			return 0;
		else
			return frequencies.get(0).getCount();
	}

	@Override
	public List<CountedOutcome<String>> getFrequencies(String initialWord) {
		List<CountedOutcome<String>> results = new ArrayList<CountedOutcome<String>>();
		String word = jochreSession.getLinguistics().standardiseWord(initialWord);
		if (LOG.isTraceEnabled()) {
			LOG.trace("getFrequency for: " + initialWord + ", standardised to: " + word);
		}

		if (NUMBER.matcher(word).matches()) {
			if (LOG.isTraceEnabled()) {
				LOG.trace(word + " is a number, setting freq to 1");
			}
			results.add(new CountedOutcome<String>(word, 1));
			return results;
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
		variants = this.addVariants(variants, "כ", "כּ");
		variants = this.addVariants(variants, "ב", "בֿ");
		variants = this.addVariants(variants, "בּ", "ב");
		variants = this.addVariants(variants, "כֿ", "כ");
		variants = this.addVariants(variants, "פ", "פֿ");
		variants = this.addVariants(variants, "פּ", "פ");
		variants = this.addVariants(variants, "װו", "װוּ");

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

		Set<CountedOutcome<String>> orderedResults = new TreeSet<CountedOutcome<String>>();
		for (String variant : variants) {
			int frequency = this.baseLexicon.getFrequency(variant);
			if (frequency > 0) {
				orderedResults.add(new CountedOutcome<String>(variant, frequency));
			}
			// only count multiple occurrences if it's the exact spelling
			if (!variant.equals(initialWord) && frequency > 1)
				frequency = 1;
			if (LOG.isTraceEnabled()) {
				if (frequency > 0) {
					LOG.trace(variant + ": " + frequency);
				}
			}
		}
		results.addAll(orderedResults);

		return results;
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
	 */
	public Lexicon getBaseLexicon() {
		return baseLexicon;
	}

	@Override
	public Iterator<String> getWords() {
		return this.baseLexicon.getWords();
	}

}
