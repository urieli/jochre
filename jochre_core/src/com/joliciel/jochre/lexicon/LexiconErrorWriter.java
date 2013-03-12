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
package com.joliciel.jochre.lexicon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.analyser.LetterGuessObserver;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.LogUtils;

/**
 * Creates the following outputs:<br/>
 * a 2x2 matrix of known/unknown vs error/correct<br/>
 * a list of words for each square in the matrix<br/>
 * @author Assaf Urieli
 *
 */
public class LexiconErrorWriter implements LetterGuessObserver {
	private static final Log LOG = LogFactory.getLog(LexiconErrorWriter.class);
	private File outputDir;
	private String baseName;
	MostLikelyWordChooser wordChooser;
	
	int knownWordErrorCount;
	int knownWordCorrectCount;
	int unknownWordErrorCount;
	int unknownWordCorrectCount;
	
	Writer knownWordErrorWriter;
	Writer knownWordCorrectWriter;
	Writer unknownWordErrorWriter;
	Writer unknownWordCorrectWriter;
	Writer allWordWriter;
	
	public LexiconErrorWriter(File outputDir, String baseName, MostLikelyWordChooser wordChooser) {
		try {
			this.outputDir = outputDir;
			this.baseName = baseName;
			this.wordChooser = wordChooser;
			
			knownWordErrorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_KE.csv"), false),"UTF8"));
			knownWordCorrectWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_KC.csv"), false),"UTF8"));
			unknownWordErrorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_UE.csv"), false),"UTF8"));
			unknownWordCorrectWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_UC.csv"), false),"UTF8"));
			allWordWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_all.csv"), false),"UTF8"));
	
			String line = CSVFormatter.format("word")
				+ CSVFormatter.format("guess")
				+ CSVFormatter.format("realFreq")
				+ CSVFormatter.format("guessFreq")
				+ CSVFormatter.format("file")
				+ CSVFormatter.format("page")
				+ CSVFormatter.format("par")
				+ CSVFormatter.format("row")
				+ CSVFormatter.format("group")
				+ CSVFormatter.format("id")
				+ "\n";
			
			knownWordErrorWriter.write(line);
			knownWordCorrectWriter.write(line);
			unknownWordErrorWriter.write(line);
			unknownWordCorrectWriter.write(line);
			
			line = CSVFormatter.format("word")
				+ CSVFormatter.format("guess")
				+ CSVFormatter.format("known")
				+ CSVFormatter.format("error")
				+ CSVFormatter.format("realFreq")
				+ CSVFormatter.format("guessFreq")
				+ CSVFormatter.format("file")
				+ CSVFormatter.format("page")
				+ CSVFormatter.format("par")
				+ CSVFormatter.format("row")
				+ CSVFormatter.format("group")
				+ CSVFormatter.format("id")
				+ "\n";
			allWordWriter.write(line);
			
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onImageStart(JochreImage jochreImage) {
		
	}

	@Override
	public void onGuessLetter(ShapeInSequence shapeInSequence, String bestGuess) {
		
	}

	@Override
	public void onStartSequence(LetterSequence letterSequence) {
		
	}

	@Override
	public void onGuessSequence(LetterSequence letterSequence) {
		try  {
			int realFrequency = wordChooser.getFrequency(letterSequence.getRealWord());
			boolean error = !letterSequence.getRealWord().equals(letterSequence.getGuessedWord());
			boolean known = realFrequency>0;
			
			for (int i=0;i<2;i++) {
				Writer writer = null;
				if (i==0) {
					writer = allWordWriter;
				} else {
					if (error&&known) {
						knownWordErrorCount++;
						writer = knownWordErrorWriter;
					} else if (error&&!known) {
						unknownWordErrorCount++;
						writer = unknownWordErrorWriter;
					} else if (!error&&known) {
						knownWordCorrectCount++;
						writer = knownWordCorrectWriter;
					} else if (!error&&!known) {
						unknownWordCorrectCount++;
						writer = unknownWordCorrectWriter;
					}
				}
				
				writer.write(CSVFormatter.format(letterSequence.getRealSequence()));
				writer.write(CSVFormatter.format(letterSequence.getGuessedWord()));
				
				if (i==0) {
					writer.write(CSVFormatter.format(known ? 1 : 0));
					writer.write(CSVFormatter.format(error ? 1 : 0));
				}
				
				writer.write(CSVFormatter.format(realFrequency));
				writer.write(CSVFormatter.format(letterSequence.getFrequency()));
				writer.write(CSVFormatter.format(letterSequence.getFirstGroup().getRow().getParagraph().getImage().getPage().getDocument().getName()));
				writer.write(CSVFormatter.format(letterSequence.getFirstGroup().getRow().getParagraph().getImage().getPage().getIndex()));
				writer.write(CSVFormatter.format(letterSequence.getFirstGroup().getRow().getParagraph().getIndex()));
				writer.write(CSVFormatter.format(letterSequence.getFirstGroup().getRow().getIndex()));
				writer.write(CSVFormatter.format(letterSequence.getFirstGroup().getIndex()));
				writer.write(CSVFormatter.format(letterSequence.getFirstGroup().getId()));
				writer.write("\n");
				writer.flush();
			}
			
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onImageEnd() {
	}

	@Override
	public void onFinish() {
		try  {
			knownWordErrorWriter.close();
			knownWordCorrectWriter.close();
			unknownWordErrorWriter.close();
			unknownWordCorrectWriter.close();
			double totalCount = knownWordCorrectCount+unknownWordCorrectCount+knownWordErrorCount+unknownWordErrorCount;
			Writer statsWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_KEMatrix.csv"), false),"UTF8"));
			statsWriter.write(CSVFormatter.getCsvSeparator() + CSVFormatter.format("known") + CSVFormatter.format("unknown")+ CSVFormatter.format("total") + "\n");
			statsWriter.write(CSVFormatter.format("correct") + CSVFormatter.format(knownWordCorrectCount) + CSVFormatter.format(unknownWordCorrectCount) + CSVFormatter.format(knownWordCorrectCount+unknownWordCorrectCount) + "\n");
			statsWriter.write(CSVFormatter.format("error") + CSVFormatter.format(knownWordErrorCount) + CSVFormatter.format(unknownWordErrorCount) + CSVFormatter.format(knownWordErrorCount+unknownWordErrorCount) + "\n");
			statsWriter.write(CSVFormatter.format("total") + CSVFormatter.format(knownWordCorrectCount+knownWordErrorCount) + CSVFormatter.format(unknownWordCorrectCount+unknownWordErrorCount) + CSVFormatter.format(knownWordCorrectCount+unknownWordCorrectCount+knownWordErrorCount+unknownWordErrorCount) + "\n");
			statsWriter.write(CSVFormatter.format("correct%") + CSVFormatter.format((double)knownWordCorrectCount/totalCount) + CSVFormatter.format((double)unknownWordCorrectCount/totalCount) + CSVFormatter.format((double)(knownWordCorrectCount+unknownWordCorrectCount)/totalCount) + "\n");
			statsWriter.write(CSVFormatter.format("error%") + CSVFormatter.format((double)knownWordErrorCount/totalCount) + CSVFormatter.format((double)unknownWordErrorCount/totalCount) + CSVFormatter.format((double)(knownWordErrorCount+unknownWordErrorCount)/totalCount) + "\n");
			statsWriter.write(CSVFormatter.format("total%") + CSVFormatter.format((double)(knownWordCorrectCount+knownWordErrorCount)/totalCount) + CSVFormatter.format((double)(unknownWordCorrectCount+unknownWordErrorCount)/totalCount) + CSVFormatter.format((double)(knownWordCorrectCount+unknownWordCorrectCount+knownWordErrorCount+unknownWordErrorCount)/totalCount) + "\n");
			statsWriter.flush();
			statsWriter.close();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}

	}
}
