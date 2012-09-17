package com.joliciel.jochre.analyser;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.jochre.lexicon.Lexicon;
import com.joliciel.jochre.lexicon.WordSplitter;

public class ErrorLogger implements LetterGuessObserver {
	JochreImage currentImage = null;
		
	Writer errorWriter = null;
	Lexicon lexicon = null;
	WordSplitter wordSplitter = null;
	boolean currentImageWritten = false;
	
	@Override
	public void onImageStart(JochreImage jochreImage) {
		currentImage = jochreImage;
		currentImageWritten = false;
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
			if (!letterSequence.getRealWord().equals(letterSequence.getGuessedWord())) {
				if (!currentImageWritten) {
					errorWriter.write("\n" + currentImage.getPage().getDocument().getName() + ", " + currentImage.getPage().getIndex() + ", id: " + currentImage.getId() + "\n");
					currentImageWritten = true;
				}
				int realWordFrequency = Integer.MAX_VALUE;
				int guessedWordFrequency = letterSequence.getFrequency();
				if (wordSplitter!=null) {
					List<String> words = this.getWordSplitter().splitText(letterSequence.getRealWord());
					
					for (String word : words) {
						int frequency = this.lexicon.getFrequency(word);
						if (frequency < realWordFrequency)
							realWordFrequency = frequency;
					}
				}

				errorWriter.write("Guess: " + letterSequence.getGuessedSequence() + ". Freq: " + guessedWordFrequency + "\n");
				errorWriter.write("Real:  " + letterSequence.getRealSequence() + ". Freq: " + realWordFrequency + "\n");

				errorWriter.flush();
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	@Override
	public void onImageEnd() {
	}

	@Override
	public void onFinish() {
	}

	public Lexicon getLexicon() {
		return lexicon;
	}

	public void setLexicon(Lexicon lexicon) {
		this.lexicon = lexicon;
	}

	public WordSplitter getWordSplitter() {
		return wordSplitter;
	}

	public void setWordSplitter(WordSplitter wordSplitter) {
		this.wordSplitter = wordSplitter;
	}

	public Writer getErrorWriter() {
		return errorWriter;
	}

	public void setErrorWriter(Writer errorWriter) {
		this.errorWriter = errorWriter;
	}

}
