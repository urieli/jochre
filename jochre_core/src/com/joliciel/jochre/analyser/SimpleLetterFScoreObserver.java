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
package com.joliciel.jochre.analyser;

import java.util.List;

import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.jochre.letterGuesser.LetterValidator;
import com.joliciel.jochre.stats.FScoreCalculator;

/**
 * Calculates the f-score based on the letter already assigned to the shape provided,
 * and the best guess.
 * @author Assaf Urieli
 *
 */
public class SimpleLetterFScoreObserver implements FScoreObserver {
	FScoreCalculator<String> fScoreCalculator;
	LetterValidator letterValidator;
	Linguistics linguistics;

	boolean hasError = false;
	boolean stillValid = true;
	boolean currentImageWritten = false;
	JochreImage currentImage = null;
	
	public SimpleLetterFScoreObserver(LetterValidator letterValidator) {
		super();
		this.letterValidator = letterValidator;
		this.linguistics = Linguistics.getInstance(letterValidator.getLocale());
		fScoreCalculator = new FScoreCalculator<String>();
	}

	@Override
	public void onImageStart(JochreImage jochreImage) {
		currentImage = jochreImage;
		currentImageWritten = false;
	}

	@Override
	public void onGuessLetter(ShapeInSequence shapeInSequence, String bestGuess) {
		if (stillValid) {
			Shape shape = shapeInSequence.getShape();
			String realLetter = shape.getLetter();
			if (letterValidator.validate(realLetter)) {
				
				if (realLetter.length()==0)
					realLetter = "■";
				else if (!linguistics.getValidLetters().contains(realLetter)) {
					if (realLetter.contains("|"))
						realLetter = "□" + realLetter;
					else
						realLetter = "■" + realLetter;
				}
				if (bestGuess.length()==0)
					bestGuess = "■";
				else if (!linguistics.getValidLetters().contains(bestGuess))
					if (bestGuess.contains("|"))
						bestGuess = "□" + bestGuess;
					else
						bestGuess = "■" + bestGuess;
				
				fScoreCalculator.increment(realLetter, bestGuess);
				if (!realLetter.equals(bestGuess))
					hasError = true;
			} else {
				stillValid = false;
			}
		}
	}

	public FScoreCalculator<String> getFScoreCalculator() {
		return fScoreCalculator;
	}

	@Override
	public void onFinish() {
	}

	@Override
	public void onImageEnd() {
	}

	@Override
	public void onGuessSequence(LetterSequence letterSequence) {

	}

	@Override
	public void onStartSequence(LetterSequence letterSequence) {
		hasError = false;
		stillValid = true;
	}

	@Override
	public void onBeamSearchEnd(LetterSequence bestSequence,
			List<LetterSequence> finalSequences,
			List<LetterSequence> holdoverSequences) {
	}

}
