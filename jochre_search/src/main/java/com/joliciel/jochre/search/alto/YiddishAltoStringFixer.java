package com.joliciel.jochre.search.alto;

import java.util.Set;
import java.util.TreeSet;

class YiddishAltoStringFixer implements AltoStringFixer {
	private Set<String> dualCharacterLetters = null;

	public YiddishAltoStringFixer() {
	}

	@Override
	public void fix(AltoTextBlock block) {
		for (int i=0; i<block.getTextLines().size(); i++) {
			AltoTextLine row = block.getTextLines().get(i);
			// look for acronyms (with quotes inside).
			for (int j=1; j<row.getStrings().size()-1; j++) {
				AltoString prevString = row.getStrings().get(j-1);
				AltoString string = row.getStrings().get(j);
				AltoString nextString = row.getStrings().get(j+1);
				if (string.getContent().equals("\"") & !prevString.isWhiteSpace() && !prevString.isPunctuation()
						&& !nextString.isWhiteSpace() && !nextString.isPunctuation()) {
					// quote in the middle of two alphabetic strings, must be an acronym
					// merge prev string with double quote
					prevString.mergeWithNext();
					// merge prev string with next string
					prevString.mergeWithNext();
					
					// since the current string and next string were removed,
					// we decrement j by 1, placing it on the word which followed the
					// next string
					j--;
				}
			}
			
			// always merge apostrophes with previous if previous is one letter or immediately followed by word
			for (int j=1; j<row.getStrings().size(); j++) {
				AltoString prevString = row.getStrings().get(j-1);
				AltoString string = row.getStrings().get(j);
				AltoString nextString = null;
				if (j+1<row.getStrings().size()) {
					nextString = row.getStrings().get(j+1);
				}
				String stringContent = string.getContent();
				String prevStringContent = prevString.getContent();
				
				if (stringContent.equals("'") & !prevString.isWhiteSpace() && !prevString.isPunctuation()) {
					if (prevStringContent.length()==1||this.getDualCharacterLetters().contains(prevStringContent))
						prevString.mergeWithNext();
					else if (nextString!=null && !nextString.isWhiteSpace() && !nextString.isPunctuation()) {
						prevString.mergeWithNext();
					}
				} 
			}
			
			// merge apostrophes with next if inside a word
			for (int j=0; j<row.getStrings().size()-1; j++) {
				AltoString string = row.getStrings().get(j);
				AltoString nextString = row.getStrings().get(j+1);
				boolean afterWhiteSpaceOrPunctuation = j==0 || row.getStrings().get(j-1).isWhiteSpace() || row.getStrings().get(j-1).isPunctuation();
				String stringContent = string.getContent();
				
				if (!string.isPunctuation() && stringContent.length()>1 && stringContent.endsWith("'")) {
					if (afterWhiteSpaceOrPunctuation && stringContent.equals("ס'")||stringContent.equals("מ'")||stringContent.equals("ר'")||stringContent.equals("כ'")) {
						// do nothing
					} else if (!nextString.isWhiteSpace() && !nextString.isPunctuation()) {
						// merge
						string.mergeWithNext();
						j--;
					}
				}
			}
		}
	}

	private Set<String> getDualCharacterLetters() {
		if (dualCharacterLetters==null) {
			dualCharacterLetters = new TreeSet<String>();
			String[] dualCharacterLetterArray = new String[] {"אָ","אַ","בּ","פּ","וּ","פֿ","שׁ","וֹ","יִ","ײַ","כֿ","תּ","אֶ","כּ",",,","בֿ","עֵ","אִ","שׂ","נָ","מְ","הֶ","מַ","בָּ","לִ","נִ","עֶ","כֶ","יי","וו","אֵ","וי"};
			for (String letter : dualCharacterLetterArray) {
				dualCharacterLetters.add(letter);
			}
		}
		return dualCharacterLetters;
	}
}
