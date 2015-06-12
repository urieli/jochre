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

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.jochre.JochreException;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.jochre.letterGuesser.LetterValidator;
import com.joliciel.jochre.lexicon.Lexicon;
import com.joliciel.jochre.stats.FScoreCalculator;

/**
 * Calculates f-score based on the original letter, rather than the split/merged letter.
 * @author Assaf Urieli
 *
 */
public class OriginalShapeLetterAssigner implements FScoreObserver {
	FScoreCalculator<String> fScoreCalculator;
	boolean save = false;
	boolean evaluate = false;
	boolean singleLetterMethod = false;
	JochreImage currentImage = null;
	
	String currentLetter = "";
	String currentGuess = "";
	
	Writer errorWriter = null;
	Lexicon lexicon = null;
	boolean stillValid = true;
	boolean hasError = false;
	boolean currentImageWritten = false;
	
	LetterValidator letterValidator;

	public OriginalShapeLetterAssigner() {
		super();
		fScoreCalculator = new FScoreCalculator<String>();
	}

	@Override
	public void onImageStart(JochreImage jochreImage) {
		currentImage = jochreImage;
		currentImageWritten = false;
	}

	@Override
	public void onGuessLetter(ShapeInSequence shapeInSequence, String bestGuess) {
		Shape shape = shapeInSequence.getShape();
		shape.setOriginalGuess(bestGuess);
	}


	@Override
	public void onGuessSequence(LetterSequence letterSequence) {
		stillValid = true;
		ShapeSequence shapeSequence = letterSequence.getUnderlyingShapeSequence();
		Shape previousOriginalShape = null;

		List<ShapeInSequence> subsequenceForPrevOriginalShape = new ArrayList<ShapeInSequence>();
		for (ShapeInSequence shapeInSequence : shapeSequence) {
			// cases that are possible:
			// 1) shapeInSequence is 1-to-1 with an original shape (A from original shape A)
			// 2) shapeInSequence shares an original shape with previous (B from original shape AB)
			// 3) shapeInSequence shares an original shape with next (A from original shape AB)
			// 4) shapeInSequence shares an original shape with previous and next (B from original shape ABC)
			// 5) shapeInSequence has two original shapes (A from original shapes |A A|)
			// 6) shapeInSequence has 3 original shapes (A from original shapes |A * A|)
			// 7) shapeInSequence shares with previous and has 2+ original shapes (A from |A A|B)
			// 8) shapeInSequence shares with next and has 2+ original shapes (B from A|B B|)
			// So, when we reach a new original shape,
			// either it coincides with a previous shape border, or it doesn't
			
			List<Shape> originalShapes = shapeInSequence.getOriginalShapes();
			
			for (Shape nextOriginalShape : originalShapes) {
				if (!nextOriginalShape.equals(previousOriginalShape)) {
					// new original shape, we need to populate the letters of the previous one
					if (previousOriginalShape!=null)
						this.assignLetter(previousOriginalShape, subsequenceForPrevOriginalShape);
					
					previousOriginalShape = nextOriginalShape;
					subsequenceForPrevOriginalShape = new ArrayList<ShapeInSequence>();
				}
				subsequenceForPrevOriginalShape.add(shapeInSequence);
			} // next original shape

		} // next underlying shape sequence shape
		if (previousOriginalShape!=null)
			this.assignLetter(previousOriginalShape, subsequenceForPrevOriginalShape);
		
	}

	void assignLetter(Shape originalShape, List<ShapeInSequence> subsequenceForOriginalShape) {
		String guessedLetter = "";
		for (ShapeInSequence shapeInSubSequence : subsequenceForOriginalShape) {
			if (shapeInSubSequence.getOriginalShapes().size()==1) {
				// if this subsequence shape has only one original shape,
				// we can go ahead and add the subsequence shape's letter to the original shape
				guessedLetter += shapeInSubSequence.getShape().getOriginalGuess();
			} else {
				// the subsequence shape has multiple original shapes, so its letter has to be
				// split among all of them (these original shapes were joined into a single new shape)
				int j = 0;
				int myIndex = -1;
				for (Shape myOriginalShape : shapeInSubSequence.getOriginalShapes()) {
					if (myOriginalShape.equals(originalShape)) {
						myIndex = j;
						break;
					}
					j++;
				}
				if (myIndex==0) {
					// the original shape starts this subsequence shape
					if (shapeInSubSequence.getShape().getOriginalGuess().length()>0)
						guessedLetter += "|" + shapeInSubSequence.getShape().getOriginalGuess();
				} else if (myIndex==shapeInSubSequence.getOriginalShapes().size()-1) {
					// the original shape ends this subsequence shape
					if (shapeInSubSequence.getShape().getOriginalGuess().length()>0)
						guessedLetter += shapeInSubSequence.getShape().getOriginalGuess() + "|";
				} else {
					// the original shape is in the middle of this subsequence shape
					// nothing to do here, since we leave these blank
				} // if more than one, where is the original shape in this subsequence's original shapes
			} // only one original shape for this subsequence shape, or more?
		} // next shape in subsequence for this original shape
		
		originalShape.setOriginalGuess(guessedLetter);
		if (currentImage.getImageStatus().equals(ImageStatus.AUTO_NEW))
			originalShape.setLetter(guessedLetter);
		if (save)
			originalShape.save();
		
		if (evaluate && stillValid) {
			if (letterValidator==null) {
				throw new JochreException("Cannot evaluate without a letter validator.");
			}
			
			String realLetter = originalShape.getLetter();
			String realLetterForCheck = realLetter.replace("|","");
			if (letterValidator.validate(realLetterForCheck)) {
				if (guessedLetter.startsWith("|")&&guessedLetter.length()==3&&realLetter.equals("" + guessedLetter.charAt(1))) {
					// the guessed letter is the first half of a split dual letter, and is the same as a real letter
					this.incrementFScore(realLetter, realLetter);
				} else if (guessedLetter.endsWith("|")&&guessedLetter.length()==3&&realLetter.equals("" + guessedLetter.charAt(1))) {
					// the guessed letter is the second half of a split dual letter, and is the same as a real letter
					this.incrementFScore(realLetter, realLetter);
				} else if (realLetter.startsWith("|")&&realLetter.length()==3&&guessedLetter.equals("" + realLetter.charAt(1))) {
					// the real letter is the first half of a split dual letter, and we correctly guessed the first letter of the two
					this.incrementFScore(realLetter, realLetter);
				} else if (realLetter.endsWith("|")&&realLetter.length()==3&&guessedLetter.equals("" + realLetter.charAt(1))) {
					// the real letter is the second half of a split dual letter, and we correctly guessed the second letter of the two
					this.incrementFScore(realLetter, realLetter);
				} else {
					this.incrementFScore(realLetter, guessedLetter);
					if (realLetter.equals(guessedLetter))
						hasError = true;
				}
			} else {
				// check if there are any invalid characters
				String prevChar = "";
				for (int i=0;i<realLetterForCheck.length();i++) {
					String nextChar = "" + realLetterForCheck.charAt(i);
					if (letterValidator.validate(nextChar)) {
						// do nothing
					} else if (letterValidator.validate(prevChar + nextChar)) {
						// do nothing
					} else {
						stillValid = false;
						break;
					}
					prevChar = nextChar;
				}
				if (stillValid) {
					this.incrementFScore(realLetter, guessedLetter);
				}
			}
		}
	}
	
	void incrementFScore(String realLetter, String guessedLetter) {
		if (singleLetterMethod) {
			if (currentLetter.length()>0) {
				currentGuess += guessedLetter;
				if (!realLetter.endsWith("|"))
					return;
			}
			
			if (realLetter.length()<=1||letterValidator.validate(realLetter)) {
				fScoreCalculator.increment(realLetter, guessedLetter);
			} else if (realLetter.startsWith("|")) {
				currentLetter = realLetter.substring(1);
				currentGuess += guessedLetter;
			} else if (realLetter.endsWith("|")) {
				// split letters are joined back together
				currentGuess = currentGuess.replaceAll("\\|(.){2}\\|", "$1");
				currentGuess = currentGuess.replaceAll("\\|(..){2}\\|", "$1");
				currentGuess = currentGuess.replaceAll("\\|(...){2}\\|", "$1");
				fScoreCalculator.increment(currentLetter, currentGuess);
				currentLetter = "";
				currentGuess = "";
			} else {
				List<String> realLetterChars = new ArrayList<String>();
				String prevChar = "";
				for (int i=0;i<realLetter.length();i++) {
					String nextChar = "" + realLetter.charAt(i);
					if (letterValidator.validate(prevChar + nextChar)) {
						realLetterChars.add(prevChar + nextChar);
					} else {
						if (prevChar.length()>0)
							realLetterChars.add(prevChar);
						realLetterChars.add(nextChar);
					}
					prevChar = nextChar;
				}

				List<String> guessedLetterChars = new ArrayList<String>();
				prevChar = "";
				for (int i=0;i<guessedLetter.length();i++) {
					String nextChar = "" + guessedLetter.charAt(i);
					if (letterValidator.validate(prevChar + nextChar)) {
						guessedLetterChars.add(prevChar + nextChar);
					} else {
						if (prevChar.length()>0)
							guessedLetterChars.add(prevChar);
						guessedLetterChars.add(nextChar);
					}
					prevChar = nextChar;
				}
				
				if (realLetterChars.size()==guessedLetterChars.size()) {
					for (int i=0;i<realLetterChars.size();i++) {
						fScoreCalculator.increment(realLetterChars.get(i), guessedLetterChars.get(i));
					}
				} else if (realLetterChars.size()==2) {
					String firstLetter = "";
					String secondLetter = "";
					double midPoint = (double)guessedLetterChars.size() / 2.0;
					for (int i = 0; i<guessedLetterChars.size();i++) {
						if (i<midPoint&&midPoint<i+1) {
							firstLetter += "|" + guessedLetterChars.get(i);
							secondLetter += guessedLetterChars.get(i) + "|";
						} else if (i<midPoint) {
							firstLetter += guessedLetterChars.get(i);
						} else {
							secondLetter += guessedLetterChars.get(i);
						}
					}
					fScoreCalculator.increment(realLetterChars.get(0), firstLetter);
					fScoreCalculator.increment(realLetterChars.get(1), secondLetter);
					
				} else {
					fScoreCalculator.increment(realLetter, guessedLetter);
				}

			}
		} else {
			if (realLetter.length()==0)
				realLetter = "■";
			else if (!letterValidator.validate(realLetter)) {
				if (realLetter.contains("|"))
					realLetter = "□" + realLetter;
				else
					realLetter = "■" + realLetter;
			}
			if (guessedLetter.length()==0)
				guessedLetter = "■";
			else if (!letterValidator.validate(guessedLetter))
				if (guessedLetter.contains("|"))
					guessedLetter = "□" + guessedLetter;
				else
					guessedLetter = "■" + guessedLetter;
			
			fScoreCalculator.increment(realLetter, guessedLetter);
		}
	}
	
	@Override
	public void onImageEnd() {
	}

	@Override
	public void onFinish() {
	}

	@Override
	public FScoreCalculator<String> getFScoreCalculator() {
		return fScoreCalculator;
	}

	public boolean isSave() {
		return save;
	}

	public void setSave(boolean save) {
		this.save = save;
	}

	public boolean isEvaluate() {
		return evaluate;
	}

	public void setEvaluate(boolean evaluate) {
		this.evaluate = evaluate;
	}

	public boolean isSingleLetterMethod() {
		return singleLetterMethod;
	}

	public void setSingleLetterMethod(boolean singleLetterMethod) {
		this.singleLetterMethod = singleLetterMethod;
	}

	@Override
	public void onStartSequence(LetterSequence letterSequence) {
		
	}

	public LetterValidator getLetterValidator() {
		return letterValidator;
	}

	public void setLetterValidator(LetterValidator letterValidator) {
		this.letterValidator = letterValidator;
	}

	@Override
	public void onBeamSearchEnd(LetterSequence bestSequence,
			List<LetterSequence> finalSequences,
			List<LetterSequence> holdoverSequences) {
	}


}
