package com.joliciel.jochre.search;

import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class YiddishNormalisingFilter extends TokenFilter {
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	
	public YiddishNormalisingFilter(TokenStream input) {
		super(input);
	}

	@Override
	public boolean incrementToken() throws IOException {
	    if (!input.incrementToken())
	        return false;
	    
	    String term = new String(termAtt.buffer(), 0, termAtt.length());
	    term = this.normalise(term);
	    termAtt.copyBuffer(term.toCharArray(), 0, term.length());
		return true;
	}

	private String normalise(String text) {
		// double-character fixes
		text = text.replaceAll("[֑֖֛֢֣֤֥֦֧֪֚֭֮֒֓֔֕֗֘֙֜֝֞֟֠֡֨֩֫֬֯]", "");
		text = text.replaceAll("[ְֱֲֳִֵֶַָֹֺֻּֽֿׁׂׅׄ]", "");
		text = text.replaceAll("װ", "וו");
		text = text.replaceAll("ױ", "וי");
		text = text.replaceAll("[ײײַ]", "יי");
		text = text.replaceAll("[ﭏאָﬞאַאָ]", "א");
		text = text.replaceAll("יִ", "י");
		text = text.replaceAll("וּ", "ו");
		text = text.replaceAll("כֿ", "כ");
		text = text.replaceAll("[בֿבּ]", "ב");
		text = text.replaceAll("[כֿכּ]", "כ");
		text = text.replaceAll("[שׁשׂ]", "ש");
		text = text.replaceAll("[תּ]", "ת");

		return text;
	}
}
