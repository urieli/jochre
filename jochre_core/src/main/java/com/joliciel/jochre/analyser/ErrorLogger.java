package com.joliciel.jochre.analyser;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.jochre.utils.JochreException;

public class ErrorLogger implements LetterGuessObserver {
	JochreImage currentImage = null;

	Writer errorWriter = null;
	boolean currentImageWritten = false;

	private final JochreSession jochreSession;

	public ErrorLogger(JochreSession jochreSession) {
		super();
		this.jochreSession = jochreSession;
	}

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
		try {
			if (!letterSequence.getRealWord().equals(letterSequence.getGuessedWord())) {
				if (!currentImageWritten) {
					errorWriter.write("\n" + currentImage.getPage().getDocument().getName() + ", " + currentImage.getPage().getIndex() + ", id: "
							+ currentImage.getId() + "\n");
					currentImageWritten = true;
				}
				int realWordFrequency = Integer.MAX_VALUE;
				int guessedWordFrequency = letterSequence.getFrequency();

				List<String> words = jochreSession.getLinguistics().splitText(letterSequence.getRealWord());

				for (String word : words) {
					int frequency = jochreSession.getLexicon().getFrequency(word);
					if (frequency < realWordFrequency)
						realWordFrequency = frequency;
				}

				errorWriter.write("Guess: " + letterSequence.getGuessedSequence() + ". Freq: " + guessedWordFrequency + "\n");
				errorWriter.write("Real:  " + letterSequence.getRealSequence() + ". Freq: " + realWordFrequency + "\n");

				errorWriter.flush();
			}
		} catch (IOException ioe) {
			throw new JochreException(ioe);
		}
	}

	@Override
	public void onImageEnd() {
	}

	@Override
	public void onFinish() {
	}

	public Writer getErrorWriter() {
		return errorWriter;
	}

	public void setErrorWriter(Writer errorWriter) {
		this.errorWriter = errorWriter;
	}

	@Override
	public void onBeamSearchEnd(LetterSequence bestSequence, List<LetterSequence> finalSequences, List<LetterSequence> holdoverSequences) {
	}

}
