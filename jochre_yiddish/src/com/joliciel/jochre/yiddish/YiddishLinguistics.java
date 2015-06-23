package com.joliciel.jochre.yiddish;

import java.util.Set;
import java.util.TreeSet;

import com.joliciel.jochre.lang.Linguistics;

public class YiddishLinguistics implements Linguistics {
	private Set<String> dualCharacterLetters = null;
	private Set<String> validLetters = null;
	private Set<Character> validCharacters = null;
	private Set<Character> diacritics = null;
	private Set<Character> singleCharacterLetters = null;
	private Set<Character> punctuation = null;

	@Override
	public Set<String> getValidLetters() {
		if (validLetters==null) {
			validLetters = new TreeSet<String>();
			for (Character c : this.getSingleCharacterLetters()) {
				validLetters.add("" + c);
			}
			validLetters.addAll(this.getDualCharacterLetters());
		}
		return validLetters;	}

	@Override
	public Set<Character> getValidCharacters() {
		if (validCharacters==null) {
			validCharacters = new TreeSet<Character>();
			validCharacters.addAll(this.getSingleCharacterLetters());
			validCharacters.addAll(this.getDiacritics());
		}
		return validCharacters;
	}

	@Override
	public Set<String> getDualCharacterLetters() {
		if (dualCharacterLetters==null) {
			dualCharacterLetters = new TreeSet<String>();
			String[] dualCharacterLetterArray = new String[] {"אָ","אַ","בּ","פּ","וּ","פֿ","שׁ","וֹ","יִ","ײַ","''","כֿ","תּ","אֶ","כּ",",,","בֿ","עֵ","אִ","שׂ","נָ","מְ","הֶ","מַ","בָּ","לִ","נִ","עֶ","כֶ","יי","וו","אֵ","וי","יו","וּן","ון","זו","זי","יז","ין","ינ","נוּ","נו","ני"};
			for (String letter : dualCharacterLetterArray) {
				dualCharacterLetters.add(letter);
			}
		}
		return dualCharacterLetters;
	}
	
	private Set<Character> getSingleCharacterLetters() {
		if (singleCharacterLetters==null) {
			singleCharacterLetters = new TreeSet<Character>();
			char[] validCharacterArray = new char[] {'א','ב','ג','ד','ה','ו','ז','ח','ט','י','כ','ך','ל','מ','ם','נ','ן','ס','ע','פ','ף','צ','ץ','ק','ר','ש','ת',
					'0','1','2','3','4','5','6','7','8','9',',','.','\'','!','?',')','(','*',';',':','-','—','%','/','\\'};
			for (char letter : validCharacterArray) {
				singleCharacterLetters.add(letter);
			}
		}

		return singleCharacterLetters;
	}
	
	private Set<Character> getDiacritics() {
		if (diacritics==null) {
			diacritics = new TreeSet<Character>();
			char[] diacriticArray = new char[] {'ֱ','ֲ','ֳ','ִ','ֵ','ֶ','ַ','ָ','ֻ','ּ','ֽ','ֿ','ׁ','ׂ','ׄ','ְ'};
			for (char letter : diacriticArray) {
				diacritics.add(letter);
			}
		}
		return diacritics;
	}

	@Override
	public Set<Character> getPunctuation() {
		if (punctuation==null) {
			punctuation = new TreeSet<Character>();
			char[] punctuationArray = new char[] {',','.','\'','!','?',')','(','*',';',':','-','—','%','/','\\','„','“','\'','"'};
			for (char letter : punctuationArray) {
				punctuation.add(letter);
			}
		}
		return punctuation;
	}

	@Override
	public boolean isLeftToRight() {
		return false;
	}

	@Override
	public String standardiseWord(String originalWord) {
		String word = originalWord;
		// double-character fixes
		word = word.replaceAll("וו", "װ");
		word = word.replaceAll("וי", "ױ");
		word = word.replaceAll("ױִ", "ויִ");
		
		// systematic replacements, including redundent melupm vov and khirik yud
		word = word.replaceAll("װוּ", "װוּּ");
		word = word.replaceAll("וּװ", "וּּװ");
		word = word.replaceAll("וּי", "וּּי");
		word = word.replaceAll("וּ", "ו");
		word = word.replaceAll("עיִ", "עיִִ");
		word = word.replaceAll("אַיִ", "אַיִִ");
		word = word.replaceAll("אָיִ", "אָיִִ");
		word = word.replaceAll("ויִ", "ויִִ");
		word = word.replaceAll("וּיִ", "וּיִִ");
		word = word.replaceAll("יִי", "יִִי");
		word = word.replaceAll("ייִ", "ייִִ");
		word = word.replaceAll("יִ", "י");
		word = word.replaceAll("עֶ", "ע");
		word = word.replaceAll("עֵ", "ע");
		word = word.replaceAll("אֵ", "ע");
		word = word.replaceAll("אֶ", "ע");
		word = word.replaceAll("שׁ", "ש");
		word = word.replaceAll("וֹ", "ו");
		word = word.replaceAll("\\Aת([^ּ])", "תּ$1");
		
		// more double-character fixes
		word = word.replaceAll("(.)יי", "$1ײ");
		word = word.replaceAll("ייַ", "ײַ");
		word = word.replaceAll("“", "\"");
		word = word.replaceAll("''", "\"");
		word = word.replaceAll(",,", "„");
		if (word.startsWith("יי"))
			word = "ייִ" + word.substring(2);

		// silent 
		if (word.equals("װאו")) word = "װוּ";
		word = word.replaceAll("װאו([^ּ])", "װוּ$1");
		word = word.replaceAll("ואװ", "וּװ");
		word = word.replaceAll("װאױ", "װױ");
		word = word.replaceAll("אַא", "אַ");

		// silent ה
		word = word.replaceAll("טהו", "טו");
		word = word.replaceAll("טהאָ", "טאָ");

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
