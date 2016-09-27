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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.talismane.utils.CountedOutcome;
import com.typesafe.config.Config;

/**
 * Chooses the most likely letter sequence from the top n letter sequences of a
 * heap.
 * 
 * @author Assaf Urieli
 *
 */
public class MostLikelyWordChooser {
	private static final Logger LOG = LoggerFactory.getLogger(MostLikelyWordChooser.class);
	private double unknownWordFactor = 0.75;
	private double frequencyLogBase = 100.0;
	private boolean frequencyAdjusted = false;

	private final Map<Integer, Double> frequencyLogs = new HashMap<Integer, Double>();
	private final WordSplitter wordSplitter;
	private final Lexicon lexicon;
	private Set<String> midWordPunctuation = new HashSet<String>();
	private Set<String> startWordPunctuation = new HashSet<String>();
	private Set<String> endWordPunctuation = new HashSet<String>(Arrays.asList("'"));

	private final JochreSession jochreSession;

	public MostLikelyWordChooser(Lexicon lexicon, WordSplitter wordSplitter, JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.lexicon = lexicon;
		this.wordSplitter = wordSplitter;

		Config wordChooserConfig = jochreSession.getConfig().getConfig("jochre.word-chooser");
		unknownWordFactor = wordChooserConfig.getDouble("unknown-word-factor");
		frequencyAdjusted = wordChooserConfig.getBoolean("frequency-adjusted");
		frequencyLogBase = wordChooserConfig.getDouble("frequency-log-base");
	}

	/**
	 * Choose the most likely letter sequence from two heaps, one representing
	 * the holdover from the previous row (some of which end with a dash), and
	 * one representing the first heap on the current row.
	 * 
	 * @param heap
	 *            the current row's first heap
	 * @param holdoverHeap
	 *            the previous row's holdover heap, at least some of whose
	 *            sequences end with a dash
	 * @param n
	 *            the number of sequences to consider in each heap
	 * @return a letter sequence covering both heaps, either as a combined word
	 *         (with a dash in the middle) or as two separate words
	 */
	public LetterSequence chooseMostLikelyWord(List<LetterSequence> heap, List<LetterSequence> holdoverHeap, int n) {
		LetterSequence bestSequence = null;

		List<LetterSequence> holdoverWithDash = new ArrayList<LetterSequence>(n);
		List<LetterSequence> holdoverWithoutDash = new ArrayList<LetterSequence>(n);

		int i = 0;
		for (LetterSequence holdoverSequence : holdoverHeap) {
			if (i >= n)
				break;
			if (holdoverSequence.toString().endsWith("-"))
				holdoverWithDash.add(holdoverSequence);
			else
				holdoverWithoutDash.add(holdoverSequence);
			i++;
		}

		PriorityQueue<LetterSequence> combinedHeap = new PriorityQueue<LetterSequence>();
		for (LetterSequence sequenceWithDash : holdoverWithDash) {
			// find the dash that needs to be skipped at the end of sequence 1
			for (int j = sequenceWithDash.getLetters().size() - 1; j >= 0; j--) {
				String outcome = sequenceWithDash.getLetters().get(j);
				if (outcome.equals("-")) {
					sequenceWithDash.setEndOfLineHyphenIndex(j);
					break;
				}
			}
			for (LetterSequence letterSequence : heap) {
				LetterSequence combinedSequence = new LetterSequence(sequenceWithDash, letterSequence);
				combinedHeap.add(combinedSequence);
			}
		}

		List<LetterSequence> combinedSequences = new ArrayList<LetterSequence>();
		for (i = 0; i < n; i++) {
			if (combinedHeap.isEmpty())
				break;
			combinedSequences.add(combinedHeap.poll());
		}

		if (holdoverWithoutDash.size() == 0) {
			// all holdovers end with a dash
			// therefore we must combine the two sequences
			bestSequence = this.chooseMostLikelyWord(combinedSequences, n);

		} else {
			// some holdovers end with a dash, others don't
			// need to compare combined sequences with individual sequences
			LetterSequence bestCombinedSequence = this.chooseMostLikelyWord(combinedSequences, n);

			// Originally we only included sequences without dashes here
			// However, this falsifies the results towards those without a dash
			// especially in the case where sequence 1 or sequence 2 is also a
			// common word (e.g. der in Yiddish)
			// PriorityQueue<LetterSequence> holdoverHeapWithoutDash = new
			// PriorityQueue<LetterSequence>(holdoverWithoutDash);
			// LetterSequence bestHoldoverSequenceWithoutDash =
			// this.chooseMostLikelyWord(holdoverHeapWithoutDash, n);
			// Changed it to the following:
			LetterSequence bestHoldoverSequence = this.chooseMostLikelyWord(holdoverHeap, n);
			LetterSequence bestNextRowSequence = this.chooseMostLikelyWord(heap, n);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Best combined: " + bestCombinedSequence.toString() + ". Adjusted score: " + bestCombinedSequence.getAdjustedScore());
				LOG.debug("Best seq1 separate: " + bestHoldoverSequence.toString() + ". Adjusted score: " + bestHoldoverSequence.getAdjustedScore());
				LOG.debug("Best seq2 separate: " + bestNextRowSequence.toString() + ". Adjusted score: " + bestNextRowSequence.getAdjustedScore());
			}

			// Now, to compare the best combined with the best separate scores,
			// we need to get a geometric mean of the shapes
			// in the best separate ones, and adjust for the lowest frequency
			// word
			LetterSequence separateSequence = new LetterSequence(bestHoldoverSequence, bestNextRowSequence);
			int minFrequency = bestHoldoverSequence.getFrequency() < bestNextRowSequence.getFrequency() ? bestHoldoverSequence.getFrequency()
					: bestNextRowSequence.getFrequency();
			double freqLog = this.getFrequencyAdjustment(minFrequency);
			double separateAdjustedScore = separateSequence.getScore() * freqLog;
			separateSequence.setAdjustedScore(separateAdjustedScore);
			if (LOG.isDebugEnabled())
				LOG.debug("Best separate: " + separateSequence.toString() + ". Score: " + separateSequence.getScore() + ". Freq: " + minFrequency
						+ ". Adjusted: " + freqLog + ". Adjusted score: " + separateSequence.getAdjustedScore());

			if (bestCombinedSequence.getAdjustedScore() > separateAdjustedScore) {
				if (LOG.isDebugEnabled())
					LOG.debug("Using combined sequence");
				bestSequence = bestCombinedSequence;
			} else {
				if (LOG.isDebugEnabled())
					LOG.debug("Using separate sequences");
				bestSequence = new LetterSequence(bestHoldoverSequence, bestNextRowSequence);
			}
			if (LOG.isDebugEnabled())
				LOG.debug("Best with holdover: " + bestSequence.toString());
		}

		return bestSequence;
	}

	/**
	 * Choose the most likely letter sequence from the heap.
	 * 
	 * @param n
	 *            the number of sequences to consider in the heap
	 */
	public LetterSequence chooseMostLikelyWord(List<LetterSequence> heap, int n) {
		int i = 0;
		double bestScore = Double.NEGATIVE_INFINITY;
		LetterSequence bestSequence = null;

		List<LetterSequence> sequences = new ArrayList<LetterSequence>(n);
		for (LetterSequence sequence : heap) {
			if (i >= n)
				break;
			sequences.add(sequence);

			int minFrequency = this.getFrequency(sequence);

			sequence.setFrequency(minFrequency);
			double freqLog = this.getFrequencyAdjustment(minFrequency);

			double adjustedScore = sequence.getScore() * freqLog;
			sequence.setAdjustedScore(adjustedScore);
			if (LOG.isDebugEnabled())
				LOG.debug(sequence.toString() + ". Score: " + sequence.getScore() + ". Freq: " + minFrequency + ". Adjusted: " + freqLog + ". Adjusted score: "
						+ adjustedScore);
			if (adjustedScore > bestScore) {
				bestSequence = sequence;
				bestScore = adjustedScore;
			}
			i++;
		}
		if (LOG.isDebugEnabled())
			LOG.debug("Best: " + bestSequence.toString());

		return bestSequence;
	}

	double getFrequencyAdjustment(int minFrequency) {
		if (frequencyAdjusted) {
			Double freqLogObj = this.frequencyLogs.get(minFrequency);
			double freqLog = 0;
			if (freqLogObj != null)
				freqLog = freqLogObj.doubleValue();
			else {
				// Assume the base is 2, and additive smoothing = 0.4.
				// -1 = 0.04
				// 0 = 0.4
				// 1 = 1
				// 2 = 1 + log2(2) = 2
				// 4 = 1 + log2(4) = 3
				// etc.
				double minFreq = minFrequency;
				if (minFrequency < 0)
					freqLog = unknownWordFactor / 10.0;
				else if (minFrequency == 0)
					freqLog = unknownWordFactor;
				else
					freqLog = 1 + (Math.log(minFreq) / Math.log(frequencyLogBase));

				this.frequencyLogs.put(minFrequency, freqLog);
			}
			return freqLog;
		} else {
			if (minFrequency < 0)
				return unknownWordFactor / 10.0;
			else if (minFrequency == 0)
				return unknownWordFactor;
			else
				return 1;
		}
	}

	/**
	 * For a given LetterSequence, find the lexicon's frequency for the
	 * underlying guessed word (or the minimum word frequency if there are
	 * several words separated by punctuation).
	 */
	public int getFrequency(LetterSequence letterSequence) {
		return this.getFrequency(letterSequence, true);
	}

	/**
	 * Same as {@link #getFrequency(LetterSequence)}, but can either apply to
	 * the guessed word or to the real word from the training corpus.
	 * 
	 * @param guessedWord
	 *            if true, applies to the guessed word
	 */
	public int getFrequency(LetterSequence letterSequence, boolean guessedWord) {
		int frequency = 0;
		List<LetterSequence> subsequences = letterSequence.getSubsequences();
		List<List<LetterSequence>> possibilities = new ArrayList<List<LetterSequence>>();
		possibilities.add(new ArrayList<LetterSequence>());

		int lastIndex = -1;
		for (int i = 0; i < subsequences.size(); i++) {
			LetterSequence subsequence = subsequences.get(i);
			lastIndex += subsequence.getLetters().size();
			String word = null;
			if (guessedWord)
				word = subsequence.getGuessedWord();
			else
				word = subsequence.getRealWord();

			List<List<LetterSequence>> newPossibilities = new ArrayList<List<LetterSequence>>();
			for (List<LetterSequence> possibility : possibilities) {
				if (possibility.size() > 0) {
					// has this subsequence already been processed ?
					LetterSequence lastSequence = possibility.get(possibility.size() - 1);
					Shape lastShape = lastSequence.getUnderlyingShapeSequence().get(lastSequence.getUnderlyingShapeSequence().size() - 1).getShape();
					Shape myLastShape = subsequence.getUnderlyingShapeSequence().get(subsequence.getUnderlyingShapeSequence().size() - 1).getShape();
					if (lastShape.equals(myLastShape)) {
						newPossibilities.add(possibility);
						continue;
					}
				}
				boolean addWord = true;
				if (subsequence.isPunctation()) {
					if (word.equals("-") || midWordPunctuation.contains(word) || startWordPunctuation.contains(word) || endWordPunctuation.contains(word)) {
						LetterSequence prevSequence = possibility.size() == 0 ? null : possibility.get(possibility.size() - 1);
						LetterSequence nextSequence = i == subsequences.size() - 1 ? null : subsequences.get(i + 1);
						LetterSequence prevCurrentSequence = new LetterSequence(prevSequence, subsequence);
						LetterSequence currentNextSequence = new LetterSequence(subsequence, nextSequence);
						LetterSequence prevCurrentNextSequence = new LetterSequence(prevCurrentSequence, nextSequence);

						if (word.equals("-")) {
							if (prevSequence == null && nextSequence == null) {
								newPossibilities.add(possibility);
							} else if (prevSequence == null) {
								List<LetterSequence> newPoss = new ArrayList<LetterSequence>();
								newPoss.add(subsequence);
								newPoss.add(nextSequence);
								newPossibilities.add(newPoss);
								newPoss = new ArrayList<LetterSequence>();
								newPoss.add(currentNextSequence);
								newPossibilities.add(newPoss);
							} else if (nextSequence == null) {
								List<LetterSequence> newPoss = new ArrayList<LetterSequence>(possibility);
								newPoss.add(subsequence);
								newPossibilities.add(newPoss);
								newPoss = new ArrayList<LetterSequence>(possibility);
								newPoss.remove(newPoss.size() - 1);
								newPoss.add(prevCurrentSequence);
								newPossibilities.add(newPoss);
							} else {
								List<LetterSequence> newPoss = new ArrayList<LetterSequence>(possibility);
								newPoss.add(subsequence);
								newPoss.add(nextSequence);
								newPossibilities.add(newPoss);
								newPoss = new ArrayList<LetterSequence>(possibility);
								newPoss.add(currentNextSequence);
								newPossibilities.add(newPoss);
								newPoss = new ArrayList<LetterSequence>(possibility);
								newPoss.remove(newPoss.size() - 1);
								newPoss.add(prevCurrentSequence);
								newPoss.add(nextSequence);
								newPossibilities.add(newPoss);
								newPoss = new ArrayList<LetterSequence>(possibility);
								newPoss.remove(newPoss.size() - 1);
								newPoss.add(prevCurrentNextSequence);
								newPossibilities.add(newPoss);

								// add skipped dash possibility
								if (lastIndex == letterSequence.getEndOfLineHyphenIndex()) {
									subsequence.setHyphenSubsequence(subsequence);
									prevCurrentNextSequence.setHyphenSubsequence(subsequence);
									prevCurrentSequence.setHyphenSubsequence(subsequence);
									currentNextSequence.setHyphenSubsequence(subsequence);

									LetterSequence prevNextSequence = new LetterSequence(prevCurrentSequence, nextSequence);
									prevNextSequence.setHyphenSubsequence(subsequence);
									prevNextSequence.setSoftHyphen(true);

									newPoss = new ArrayList<LetterSequence>(possibility);
									newPoss.remove(newPoss.size() - 1);
									newPoss.add(prevNextSequence);

									newPossibilities.add(newPoss);
								}
							}

							addWord = false;
						}

						if (midWordPunctuation.contains(word)) {
							if (prevSequence != null && nextSequence != null) {
								List<LetterSequence> newPoss = new ArrayList<LetterSequence>(possibility);
								newPoss.remove(newPoss.size() - 1);
								newPoss.add(prevCurrentNextSequence);
								newPossibilities.add(newPoss);
								addWord = false;
							}
						}

						if (startWordPunctuation.contains(word)) {
							if (nextSequence != null && !subsequences.get(subsequences.size() - 1).getGuessedWord().equals(word)) {
								List<LetterSequence> newPoss = new ArrayList<LetterSequence>(possibility);
								newPoss.add(currentNextSequence);
								newPossibilities.add(newPoss);

								newPoss = new ArrayList<LetterSequence>(possibility);
								newPoss.add(subsequence);
								newPoss.add(nextSequence);
								newPossibilities.add(newPoss);
								addWord = false;
							}
						}

						if (endWordPunctuation.contains(word) && !subsequences.get(0).getGuessedWord().equals(word)) {
							if (prevSequence != null) {
								List<LetterSequence> newPoss = new ArrayList<LetterSequence>(possibility);
								newPoss.remove(newPoss.size() - 1);
								newPoss.add(prevCurrentSequence);
								newPossibilities.add(newPoss);
							}
						}
					}
				}

				if (addWord) {
					possibility.add(subsequence);
					newPossibilities.add(possibility);
				}
			}
			possibilities = newPossibilities;
		}

		TreeMap<Integer, List<List<LetterSequence>>> freqPossibilityMap = new TreeMap<Integer, List<List<LetterSequence>>>();

		for (List<LetterSequence> possibility : possibilities) {
			boolean hasWords = false;
			for (LetterSequence subsequence : possibility) {
				if (!subsequence.isPunctation()) {
					hasWords = true;
					break;
				}
			}
			int minFreq = Integer.MAX_VALUE;
			for (LetterSequence subsequence : possibility) {
				String word = subsequence.getGuessedWord();
				int freq = 0;
				List<CountedOutcome<String>> frequencies = lexicon.getFrequencies(word);

				if (frequencies.size() == 0) {
					// check whether word is impossible
					if (!jochreSession.getLinguistics().isWordPossible(word)) {
						frequencies.add(new CountedOutcome<String>(word, -1));
					}
				}

				if (frequencies != null && frequencies.size() > 0) {
					subsequence.setWordFrequencies(frequencies);
					letterSequence.getWordFrequencies().add(frequencies.get(0));
					freq = frequencies.get(0).getCount();
				} else {
					frequencies = new ArrayList<>();
					frequencies.add(new CountedOutcome<String>(word, 0));
					freq = 0;
					subsequence.setWordFrequencies(frequencies);
					letterSequence.getWordFrequencies().add(frequencies.get(0));
				}

				if (subsequence.isPunctation() && hasWords) {
					continue;
				}
				if (freq < minFreq)
					minFreq = freq;
			}

			List<List<LetterSequence>> possibilitiesAtFreq = freqPossibilityMap.get(minFreq);
			if (possibilitiesAtFreq == null) {
				possibilitiesAtFreq = new ArrayList<List<LetterSequence>>();
				freqPossibilityMap.put(minFreq, possibilitiesAtFreq);
			}
			possibilitiesAtFreq.add(possibility);

		}

		// Out of all of the sub-sequences possibilities giving the max
		// frequency in the lexicon
		// we choose the one containing the single longest word to populate the
		// subsequences for this letter sequence
		// and select its hyphenated content.
		// Thus if both halves of an existing hyphenated word also happen to
		// exist independently as words in the lexicon,
		// we'll still take the longer hyphenated word.
		List<List<LetterSequence>> maxFreqPossibilities = freqPossibilityMap.lastEntry().getValue();
		List<LetterSequence> maxLengthList = null;
		int maxLengthForList = -1;
		for (List<LetterSequence> possibility : maxFreqPossibilities) {
			int maxLength = 0;
			for (LetterSequence subsequence : possibility) {
				String word = subsequence.getGuessedWord();
				if (word.length() > maxLength)
					maxLength = word.length();
			}
			if (maxLength > maxLengthForList) {
				maxLengthList = possibility;
				maxLengthForList = maxLength;
			}
		}

		frequency = freqPossibilityMap.lastEntry().getKey();
		letterSequence.setSubsequences(maxLengthList);

		// construct the hyphenated string out of the subsequences directly
		// surrounding the hyphen
		// making sure to leave out any opening and closing punctuation
		String hyphenatedString = "";
		boolean foundFirstWord = false;
		String punctuationString = "";
		for (LetterSequence subsequence : maxLengthList) {
			if (subsequence.getHyphenSubsequence() != null) {
				letterSequence.setHyphenSubsequence(subsequence.getHyphenSubsequence());
			}
			if (!foundFirstWord && !subsequence.isPunctation())
				foundFirstWord = true;
			if (foundFirstWord && subsequence.isPunctation()) {
				punctuationString += subsequence.getGuessedWord();
			} else if (foundFirstWord) {
				hyphenatedString += punctuationString;
				punctuationString = "";
				hyphenatedString += subsequence.getGuessedWord();
			}
		}
		if (letterSequence.isSplit()) {
			letterSequence.setHyphenatedString(hyphenatedString);
			for (LetterSequence subsequence : maxLengthList) {
				subsequence.setHyphenatedString(hyphenatedString);
			}
		}

		return frequency;
	}

	public WordSplitter getWordSplitter() {
		return wordSplitter;
	}

	/**
	 * The log base indicating how much more weight to give to a common word
	 * than a rare word, if {@link #isFrequencyAdjusted()} is true. The score =
	 * 1 + (ln(1) / ln(frequencyLogBase)); Default value is 10.0, so that a word
	 * with a frequency of 10 has twice the weight of frequency of 1, 100 has 3
	 * times the weight, etc.
	 */
	public double getFrequencyLogBase() {
		return frequencyLogBase;
	}

	public void setFrequencyLogBase(double frequencyLogBase) {
		this.frequencyLogBase = frequencyLogBase;
	}

	/**
	 * An adjustment factor for unknown words<br/>
	 * 0 means unknown words will not be allowed.<br/>
	 * 1 means unknown words have no downwards adjustment with respect to known
	 * words<br/>
	 * other values means the raw score for unknown words is multiplied by this
	 * factor<br/>
	 * 
	 * @return
	 */
	public double getUnknownWordFactor() {
		return unknownWordFactor;
	}

	public void setUnknownWordFactor(double unknownWordFactor) {
		this.unknownWordFactor = unknownWordFactor;
	}

	/**
	 * The lexicon based on which the choices are made.
	 */
	public Lexicon getLexicon() {
		return lexicon;
	}

	/**
	 * Should we adjust at all with respect to word frequency, or should we
	 * simply give one score for known words and another for unknown words.
	 */
	public boolean isFrequencyAdjusted() {
		return frequencyAdjusted;
	}

	public void setFrequencyAdjusted(boolean frequencyAdjusted) {
		this.frequencyAdjusted = frequencyAdjusted;
	}
}
