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
package com.joliciel.jochre.letterGuesser;

import java.util.List;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.talismane.machineLearning.Solution;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * A sequence of weighted outcomes with an attached score.
 * @author Assaf Urieli
 *
 */
public interface LetterSequence extends List<Letter>, Solution<Letter> {

	/**
	 * Get the sum of logs of the weights.
	 * @return
	 */
	public double getScore();
	
	/**
	 * A dash that needs to be skipped in certain circumstances,
	 * as it represents a dash a the end of a row for combining a sequence
	 * with the first sequence of the next row.
	 * @return
	 */
	public int getDashToSkip();
	public void setDashToSkip(int dashToSkip);

	/**
	 * The score, after adjustments to account for external factors
	 * (such as corresponding word frequency in training corpus).
	 */
	public abstract double getAdjustedScore();
	public abstract void setAdjustedScore(double adjustedScore);
	
	/**
	 * The sequence of shapes underlying this letter sequence.
	 * @return
	 */
	public ShapeSequence getUnderlyingShapeSequence();
	public void setUnderlyingShapeSequence(ShapeSequence shapeSequence);
	
	/**
	 * Get the next shape in the underlying shape sequence
	 * (beyond what has already been guessed by this letter sequence.
	 * @return
	 */
	public ShapeInSequence getNextShape();
	
	/**
	 * The frequency of the word represented by this letter sequence.
	 * @return
	 */
	public int getFrequency();
	public void setFrequency(int frequency);
	
	/**
	 * After breaking this letter sequence up into individual words,
	 * gives the frequency for each word as found in the lexicon.
	 * @return
	 */
	public List<WeightedOutcome<String>> getWordFrequencies();
	
	/**
	 * The guessed word.
	 * @return
	 */
	public String getGuessedWord();
	
	/**
	 * The real word behind this letter sequence, in cases where it's
	 * from the training/test corpus.
	 * @return
	 */
	public String getRealWord();
	
	/**
	 * A string representation of the real sequence behind this letter sequence
	 * (including split letters and inkspots).
	 * @return
	 */
	public String getRealSequence();
	
	/**
	 * A string representation of the guessed sequence behind this letter sequence
	 * (including split letters and inkspots).
	 * @return
	 */
	public String getGuessedSequence();
	
	/**
	 * The first group of shapes underlying this letter sequence.
	 * @return
	 */
	public GroupOfShapes getFirstGroup();
}