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

import java.util.List;
import com.joliciel.jochre.letterGuesser.LetterSequence;

/**
 * Chooses the most likely letter sequence from the top n letter sequences of a heap.
 * @author Assaf Urieli
 *
 */
public interface MostLikelyWordChooser {
	/**
	 * Choose the most likely letter sequence from the heap.
	 * @param heap
	 * @param n the number of sequences to consider in the heap
	 * @return
	 */
	public LetterSequence chooseMostLikelyWord(List<LetterSequence> heap, int n);
	
	/**
	 * Choose the most likely letter sequence from two heaps,
	 * one representing the holdover from the previous row (some of which end with a dash),
	 * and one representing the first heap on the current row.
	 * @param heap the current row's first heap
	 * @param holdoverHeap the previous row's holdover heap, at least some of whose sequences end with a dash
	 * @param n the number of sequences to consider in each heap
	 * @return a letter sequence covering both heaps, either as a combined word (with a dash in the middle)
	 * or as two separate words
	 */
	public LetterSequence chooseMostLikelyWord(
			List<LetterSequence> heap, List<LetterSequence> holdoverHeap, int n);
	
	public WordSplitter getWordSplitter();
	public void setWordSplitter(WordSplitter wordSplitter);
	
	/**
	 * Should we adjust at all with respect to word frequency,
	 * or should we simply give one score for known words and another for unknown words.
	 * @return
	 */
	public boolean isFrequencyAdjusted();
	public void setFrequencyAdjusted(boolean frequencyAdjusted);
	
	/**
	 * The log base indicating how much more weight to give to a common word than a rare word,
	 * if {@link #isFrequencyAdjusted()} is true.
	 * The score = 1 + (ln(1) / ln(frequencyLogBase));
	 * Default value is 10.0, so that a word with a frequency of 10 has twice the weight of frequency of 1,
	 * 100 has 3 times the weight, etc.
	 * @return
	 */
	public double getFrequencyLogBase();
	public void setFrequencyLogBase(double frequencyLogBase);
	
	/**
	 * Additive smoothing to allow for unknown words
	 * 0 means unknown words will not be allowed
	 * 0.5 means unknown words will be given a multiple of 0.5, while a word with freq 1 will be given 1.5 (3 times as much).
	 * 1.0 = 2 times as much
	 * @return
	 */
	public double getAdditiveSmoothing();
	public void setAdditiveSmoothing(double additiveSmoothing);
	
	/**
	 * The lexicon based on which the choices are made.
	 * @return
	 */
	public Lexicon getLexicon();
	public void setLexicon(Lexicon lexicon);
		
	/**
	 * Same as {@link #getFrequency(LetterSequence)}, but can either apply
	 * to the guessed word or to the real word from the training corpus.
	 * @param letterSequence
	 * @param guessedWord if true, applies to the guessed word
	 * @return
	 */
	public int getFrequency(LetterSequence letterSequence, boolean guessedWord);
	
	/**
	 * For a given LetterSequence, find the lexicon's frequency for the underlying guessed word
	 * (or the minimum word frequency if there are several words separated by punctuation).
	 * @param letterSequence
	 * @return
	 */
	public int getFrequency(LetterSequence letterSequence);
}
