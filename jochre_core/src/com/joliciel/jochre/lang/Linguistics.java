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
package com.joliciel.jochre.lang;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class containing various information about specifics for each language
 * (valid characters, valid letters, text direction, etc.).
 * @author Assaf Urieli
 *
 */
public class Linguistics {
	private static final Log LOG = LogFactory.getLog(Linguistics.class);
	private static Map<Locale, Linguistics> instances = new HashMap<Locale, Linguistics>();
	private Locale locale;
	private Set<String> dualCharacterLetters = null;
	private Set<String> validLetters = null;
	private Set<Character> validCharacters = null;
	private Set<Character> diacritics = null;
	private Set<Character> singleCharacterLetters = null;
	private Set<Character> punctuation = null;
	
	private boolean leftToRight = false;
	private boolean leftToRightChecked = false;
	
	public static Linguistics getInstance(Locale locale) {
		Linguistics linguistics = instances.get(locale);
		if (linguistics == null) {
			linguistics = new Linguistics(locale);
			instances.put(locale, linguistics);
		}
		return linguistics;
	}
	
	private Linguistics(Locale locale) {
		this.locale = locale;
	}
	
	public Set<String> getValidLetters() {
		if (validLetters==null) {
			validLetters = new TreeSet<String>();
			if (locale.getLanguage().equals("yi")||locale.getLanguage().equals("ji")) {
				for (Character c : this.getSingleCharacterLetters()) {
					validLetters.add("" + c);
				}
				validLetters.addAll(this.getDualCharacterLetters());
			}
		}
		return validLetters;
	}
	
	public Set<Character> getValidCharacters() {
		if (validCharacters==null) {
			validCharacters = new TreeSet<Character>();
			if (locale.getLanguage().equals("yi")||locale.getLanguage().equals("ji")) {
				validCharacters.addAll(this.getSingleCharacterLetters());
				validCharacters.addAll(this.getDiacritics());
			}
		}
		return validCharacters;
	}
	
	public Set<String> getDualCharacterLetters() {
		if (dualCharacterLetters==null) {
			dualCharacterLetters = new TreeSet<String>();
			if (locale.getLanguage().equals("yi")||locale.getLanguage().equals("ji")) {
				String[] dualCharacterLetterArray = new String[] {"אָ","אַ","בּ","פּ","וּ","פֿ","שׁ","וֹ","יִ","ײַ","''","כֿ","תּ","אֶ","כּ",",,","בֿ","עֵ","אִ","שׂ","נָ","מְ","הֶ","מַ","בָּ","לִ","נִ","עֶ","כֶ","יי","וו","אֵ","וי","יו","וּן","ון","זו","זי","יז","ין","ינ","נוּ","נו","ני"};
				for (String letter : dualCharacterLetterArray) {
					dualCharacterLetters.add(letter);
				}
			}
		}
		return dualCharacterLetters;
	}
	
	private Set<Character> getSingleCharacterLetters() {
		if (singleCharacterLetters==null) {
			singleCharacterLetters = new TreeSet<Character>();
			if (locale.getLanguage().equals("yi")||locale.getLanguage().equals("ji")) {
				char[] validCharacterArray = new char[] {'א','ב','ג','ד','ה','ו','ז','ח','ט','י','כ','ך','ל','מ','ם','נ','ן','ס','ע','פ','ף','צ','ץ','ק','ר','ש','ת',
						'0','1','2','3','4','5','6','7','8','9',',','.','\'','!','?',')','(','*',';',':','-','—','%','/','\\'};
				for (char letter : validCharacterArray) {
					singleCharacterLetters.add(letter);
				}
			}
		}
		return singleCharacterLetters;
	}
	
	private Set<Character> getDiacritics() {
		if (diacritics==null) {
			diacritics = new TreeSet<Character>();
			if (locale.getLanguage().equals("yi")||locale.getLanguage().equals("ji")) {
				char[] diacriticArray = new char[] {'ֱ','ֲ','ֳ','ִ','ֵ','ֶ','ַ','ָ','ֻ','ּ','ֽ','ֿ','ׁ','ׂ','ׄ','ְ'};
				for (char letter : diacriticArray) {
					diacritics.add(letter);
				}
			}
		}
		return diacritics;
	}
	
	public Set<Character> getPunctuation() {
		if (punctuation==null) {
			punctuation = new TreeSet<Character>();
			if (locale.getLanguage().equals("yi")||locale.getLanguage().equals("ji")) {
				char[] punctuationArray = new char[] {',','.','\'','!','?',')','(','*',';',':','-','—','%','/','\\','„','“','\'','"'};
				for (char letter : punctuationArray) {
					punctuation.add(letter);
				}
			}
		}
		return punctuation;
	}
	
	public boolean isLeftToRight() {
		if (!leftToRightChecked) {
			leftToRight = true;
			LOG.debug("Locale language: " + locale.getLanguage());
			if (locale.getLanguage().equals("he")||
					locale.getLanguage().equals("yi")||
					locale.getLanguage().equals("ji")||
					locale.getLanguage().equals("ar")) {
				LOG.debug("Right-to-left");
				leftToRight = false;
			} else {
				LOG.debug("Left-to-right");
			}
			leftToRightChecked = true;
		}
		return leftToRight;
	}
}
