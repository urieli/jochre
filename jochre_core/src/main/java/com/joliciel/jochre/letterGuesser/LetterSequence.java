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

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.Rectangle;
import com.joliciel.talismane.machineLearning.ClassificationSolution;
import com.joliciel.talismane.utils.CountedOutcome;

/**
 * A sequence of letter guesses associated with a given sequence of shapes, and
 * the attached score. There will be exactly one element in this sequence per
 * underlying shape, including possibly empty strings.
 * 
 * @author Assaf Urieli
 *
 */
public interface LetterSequence extends ClassificationSolution {
	/**
	 * The letters in this sequence.
	 */
	public List<String> getLetters();

	/**
	 * Get the sum of logs of the weights.
	 */
	@Override
	public double getScore();

	public void setScore(double score);

	/**
	 * The index of the shape representing a hyphen at the end of the line,
	 * possibly a soft-hyphen (visible in word-wrapping only).
	 */
	public int getEndOfLineHyphenIndex();

	public void setEndOfLineHyphenIndex(int endOfLineHyphenIndex);

	/**
	 * The score, after adjustments to account for external factors (such as
	 * corresponding word frequency in training corpus).
	 */
	public abstract double getAdjustedScore();

	public abstract void setAdjustedScore(double adjustedScore);

	/**
	 * The sequence of shapes underlying this letter sequence.
	 */
	public ShapeSequence getUnderlyingShapeSequence();

	public void setUnderlyingShapeSequence(ShapeSequence shapeSequence);

	/**
	 * Get the next shape in the underlying shape sequence (beyond what has
	 * already been guessed by this letter sequence.
	 */
	public ShapeInSequence getNextShape();

	/**
	 * The frequency of the word represented by this letter sequence.
	 */
	public int getFrequency();

	public void setFrequency(int frequency);

	/**
	 * After breaking this letter sequence up into individual words, gives the
	 * frequency for each word as found in the lexicon.
	 */
	public List<CountedOutcome<String>> getWordFrequencies();

	public void setWordFrequencies(List<CountedOutcome<String>> wordFrequencies);

	/**
	 * The guessed word.
	 */
	public String getGuessedWord();

	/**
	 * The real word behind this letter sequence, in cases where it's from the
	 * training/test corpus.
	 */
	public String getRealWord();

	/**
	 * A string representation of the real sequence behind this letter sequence
	 * (including split letters and inkspots).
	 */
	public String getRealSequence();

	/**
	 * A string representation of the guessed sequence behind this letter sequence
	 * (including split letters and inkspots).
	 */
	public String getGuessedSequence();

	/**
	 * A list of groups of shapes underlying this letter sequence.
	 */
	public List<GroupOfShapes> getGroups();

	/**
	 * Whether or not this letter sequence is split across two lines.
	 */
	public boolean isSplit();

	/**
	 * Does this letter sequence contain a soft hyphen (only visible because of
	 * word wrapping and end-of-line)?
	 */
	public boolean isSoftHyphen();

	public void setSoftHyphen(boolean softHyphen);

	/**
	 * If this sequence contains any punctuation, returns individual sequences
	 * representing letters and punctuation. Otherwise, returns the original
	 * sequence.
	 */
	public List<LetterSequence> getSubsequences();

	public void setSubsequences(List<LetterSequence> subsequences);

	/**
	 * Whether or not this letter sequence represents punctuation (in the case of
	 * a subsequence).
	 */
	public boolean isPunctation();

	public void setPunctation(boolean punctation);

	/**
	 * Return the rectangle enclosing this letter sequence in a particular group.
	 */
	public Rectangle getRectangleInGroup(GroupOfShapes group);

	/**
	 * For a letter sequence covering two groups, split this letter sequence into
	 * one sequence per group.
	 */
	List<LetterSequence> splitByGroup();

	/**
	 * A subsequence representing a hyphen at the end-of-line (either soft or
	 * hard).
	 */
	LetterSequence getHyphenSubsequence();

	public void setHyphenSubsequence(LetterSequence hyphenSubsequence);

	/**
	 * The most likely hyphenated string in the case of a hyphenated letter
	 * sequence.
	 */
	String getHyphenatedString();

	public void setHyphenatedString(String hyphenatedString);

	public JochreSession getJochreSession();
}