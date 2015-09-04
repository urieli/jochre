package com.joliciel.jochre.lang;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultLinguistics implements Linguistics {
	private static final Log LOG = LogFactory.getLog(Linguistics.class);
	private static Map<Locale, Linguistics> instances = new HashMap<Locale, Linguistics>();
	private Locale locale;
	private Set<String> dualCharacterLetters = null;
	private Set<String> validLetters = null;
	private Set<Character> validCharacters = null;
	private Set<Character> punctuation = null;
	
	private boolean leftToRight = false;
	private boolean leftToRightChecked = false;
	
	public static Linguistics getInstance(Locale locale) {
		Linguistics linguistics = instances.get(locale);
		if (linguistics == null) {
			linguistics = new DefaultLinguistics(locale);
			instances.put(locale, linguistics);
		}
		return linguistics;
	}
	
	private DefaultLinguistics(Locale locale) {
		this.locale = locale;
	}
	
	public Set<String> getValidLetters() {
		if (validLetters==null) {
			validLetters = new TreeSet<String>();
		}
		return validLetters;
	}
	
	public Set<Character> getValidCharacters() {
		if (validCharacters==null) {
			validCharacters = new TreeSet<Character>();
		}
		return validCharacters;
	}
	
	public Set<String> getDualCharacterLetters() {
		if (dualCharacterLetters==null) {
			dualCharacterLetters = new TreeSet<String>();
		}
		return dualCharacterLetters;
	}
	
	
	public Set<Character> getPunctuation() {
		if (punctuation==null) {
			punctuation = new TreeSet<Character>();
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

	@Override
	public String standardiseWord(String originalWord) {
		return originalWord;
	}
	
	@Override
	public boolean isWordPossible(String word) {
		if (word.indexOf('|')>=0)
			return false;
		return true;
	}
}
