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
package com.joliciel.jochre.text;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.Bidi;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.descriptive.moment.Mean;

import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.utils.LogUtils;

/**
 * This Text Getter is specific to Yiddish - may need other ones for other languages.
 * @author Assaf Urieli
 *
 */
class TextGetterImpl implements TextGetter {
	private static final Log LOG = LogFactory.getLog(TextGetterImpl.class);

	@Override
	public void getText(JochreImage image, Writer writer, TextFormat textFormat) {
		try {
			double minRatioBiggerFont = 1.15;
			double maxRatioSmallerFont = 0.85;
			
			double meanXHeight = 0;
			if (textFormat.equals(TextFormat.XHTML)) {
				Mean xHeightMean = new Mean();
				for (Paragraph paragraph : image.getParagraphs()) {
					for (RowOfShapes row : paragraph.getRows()) {
						for (GroupOfShapes group : row.getGroups()) {
							for (Shape shape : group.getShapes()) {
								xHeightMean.increment(shape.getXHeight());
							}
						}
					}
				}
				meanXHeight = xHeightMean.getResult();
			}
			for (Paragraph paragraph : image.getParagraphs()) {
				if (textFormat.equals(TextFormat.XHTML))
					writer.append("<P>");
				
				Map<Integer, Boolean> fontSizeChanges = new TreeMap<Integer, Boolean>();
				int currentFontSize = 0;
				StringBuilder paragraphText = new StringBuilder();
				
				for (RowOfShapes row : paragraph.getRows()) {
					for (GroupOfShapes group : row.getGroups()) {
						if (textFormat.equals(TextFormat.XHTML)) {
							double ratio = (double) group.getXHeight() /  meanXHeight;
							if (ratio>=minRatioBiggerFont) {
								if (currentFontSize<=0)
									fontSizeChanges.put(paragraphText.length(), true);
								currentFontSize = 1;
							} else if (ratio<=maxRatioSmallerFont) {
								if (currentFontSize>=0)
									fontSizeChanges.put(paragraphText.length(), false);
								currentFontSize = -1;
							} else if (currentFontSize!=0) {
								if (currentFontSize>0)
									fontSizeChanges.put(paragraphText.length(), false);
								else if (currentFontSize<0)
									fontSizeChanges.put(paragraphText.length(), true);
								currentFontSize = 0;
							}
						}
						StringBuilder sb = new StringBuilder();
						StringBuilder currentSequence = new StringBuilder();
						for (Shape shape : group.getShapes()) {
							String letter = shape.getLetter();
							
							if (letter.startsWith("|")) {
								// beginning of a gehakte letter
								currentSequence.append(shape.getLetter());
								continue;
							} else if (letter.endsWith("|")) {
								// end of a gehakte letter
								if (currentSequence.length()>0&&currentSequence.charAt(0)=='|') {
									String letter1 = currentSequence.toString().substring(1);
									String letter2 = letter.substring(0, letter.length()-1);
									if (letter1.equals(letter2)) {
										letter = letter1;
									} else {
										letter = currentSequence.toString() + letter;
									}
									currentSequence = new StringBuilder();
								}
							}
							
							
							if (letter.equals(",")) {
								// could be ",," = "„"
								if (currentSequence.length()>0&&currentSequence.charAt(0)==',') {
									sb.append("„");
									currentSequence = new StringBuilder();
								} else {
									currentSequence.append(shape.getLetter());
								}
							} else if (letter.equals("'")) {
								// could be "''" = "“"
								if (currentSequence.length()>0&&currentSequence.charAt(0)=='\'') {
									sb.append("“");
									currentSequence = new StringBuilder();
								} else {
									currentSequence.append(shape.getLetter());
								}
							} else if (letter.equals("-")) {
								if (shape.getIndex()==group.getShapes().size()-1
										&& group.getIndex()==row.getGroups().size()-1
										&& row.getIndex()!=paragraph.getRows().size()-1) {
									// do nothing - dash at the end of the line
									// we'll assume for now these dashes are always supposed to disappear
									// though of course they could be used in the place of a real mid-word dash
								} else {
									sb.append(shape.getLetter());									
								}
							} else {
								sb.append(currentSequence);
								currentSequence = new StringBuilder();
								sb.append(letter);
							}
						} // next shape
						sb.append(currentSequence);

						paragraphText.append(sb);
						paragraphText.append(' ');
					} // next group
				} // next row
				String paragraphStr = paragraphText.toString();
				
				Writer currentWriter = writer;
				boolean haveFontSizes = fontSizeChanges.size()>0;
				if (haveFontSizes) {
					currentWriter = new StringWriter();
				}
				
				if (image.getPage().getDocument().isLeftToRight()) {
					currentWriter.append(paragraphText);					
				} else {
					this.appendBidiText(paragraphStr, currentWriter);
				}
				
				if (haveFontSizes) {
					currentFontSize = 0;

					String text = currentWriter.toString();
					int currentIndex = 0;
					
					for (int fontSizeChange : fontSizeChanges.keySet()) {
						boolean isBigger = fontSizeChanges.get(fontSizeChange);
						writer.append(text.substring(currentIndex, fontSizeChange));
						if (isBigger) {
							if (currentFontSize==0) {
								writer.append("<BIG>"); 
								currentFontSize++;
							} else if (currentFontSize<0) {
								writer.append("</SMALL>");
								currentFontSize++;
							}
						} else {
							if (currentFontSize==0) {
								writer.append("<SMALL>");
								currentFontSize--;
							} else if (currentFontSize>0) {
								writer.append("</BIG>");
								currentFontSize--;
							}
						}
						currentIndex = fontSizeChange;
					}
					writer.append(text.substring(currentIndex));
					
					if (currentFontSize>0) {
						writer.append("</BIG>");
					} else if (currentFontSize<0) {
						writer.append("</SMALL>");
					}
				} // haveFontSizes?

				if (textFormat.equals(TextFormat.XHTML))
					writer.append("</P>");
				else
					writer.append('\n');
				writer.flush();
			}
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	void appendBidiText(String text, Writer writer) {
		try {
			// assumption here is that if text is marked as left-to-right
			// it should be reversed.
	        Bidi bidi = new Bidi(text, Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
	
	        // From Bidi API:
	        // If there are multiple runs of text, information about the runs can be accessed by indexing
	        // to get the start, limit, and level of a run.
	        // The level represents both the direction and the 'nesting level' of a directional run.
	        // Odd levels are right-to-left, while even levels are left-to-right.
	        // So for example level 0 represents left-to-right text, while level 1 represents right-to-left text,
	        // and level 2 represents left-to-right text embedded in a right-to-left run. 
	        for (int i = 0; i < bidi.getRunCount(); ++i) {
	
	            int start = bidi.getRunStart(i);
	            int limit = bidi.getRunLimit(i);
	            int level = bidi.getRunLevel(i);
	            String str = text.substring(start, limit);
	            if (level % 2 == 1) {
	            	writer.append(str);
	            } else {
	            	StringBuilder reverseString = new StringBuilder();
	            	for (int j = str.length()-1; j>=0; j--)
	            		reverseString.append(str.charAt(j));
	            	writer.append(reverseString.toString());
	            }
	        }	
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

}
