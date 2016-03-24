package com.joliciel.jochre.utils.text;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.regex.Pattern;

public class DiacriticRemover {
	private static Pattern diacriticPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	
	public static String apply(String text) {
		return diacriticPattern.matcher(Normalizer.normalize(text, Form.NFD)).replaceAll("");
	}

}
