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
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.talismane.utils.CountedOutcome;

class MostLikelyWordChooserImpl implements MostLikelyWordChooser {
	private static final Logger LOG = LoggerFactory.getLogger(MostLikelyWordChooserImpl.class);
	private double additiveSmoothing = 0.75;
	private double frequencyLogBase = 100.0;
	private boolean frequencyAdjusted = false;

	private Map<Integer, Double> frequencyLogs = new HashMap<Integer, Double>();
	private LexiconServiceInternal lexiconServiceInternal = null;
	private LetterGuesserService letterGuesserService = null;
	private WordSplitter wordSplitter;
	private Lexicon lexicon;
	private Set<String> midWordPunctuation = new HashSet<String>();
	private Set<String> startWordPunctuation = new HashSet<String>();
	private Set<String> endWordPunctuation = new HashSet<String>(Arrays.asList("'"));

	private final JochreSession jochreSession;

	public MostLikelyWordChooserImpl(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
	}

	@Override
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
				LetterSequence combinedSequence = this.getLetterGuesserService().getLetterSequence(sequenceWithDash, letterSequence);
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
			LetterSequence separateSequence = this.letterGuesserService.getLetterSequence(bestHoldoverSequence, bestNextRowSequence);
			int minFrequency = bestHoldoverSequence.getFrequency() < bestNextRowSequence.getFrequency() ? bestHoldoverSequence.getFrequency()
					: bestNextRowSequence.getFrequency();
			double freqLog = this.getFrequencyAdjustment(minFrequency);
			double separateAdjustedScore = separateSequence.getScore() * freqLog;
			separateSequence.setAdjustedScore(separateAdjustedScore);
			if (LOG.isDebugEnabled())
				LOG.debug("Best separate: " + separateSequence.toString() + ". Score: " + separateSequence.getScore() + ". Freq: " + minFrequency + ". Adjusted: "
						+ freqLog + ". Adjusted score: " + separateSequence.getAdjustedScore());

			if (bestCombinedSequence.getAdjustedScore() > separateAdjustedScore) {
				if (LOG.isDebugEnabled())
					LOG.debug("Using combined sequence");
				bestSequence = bestCombinedSequence;
			} else {
				if (LOG.isDebugEnabled())
					LOG.debug("Using separate sequences");
				bestSequence = this.getLetterGuesserService().getLetterSequence(bestHoldoverSequence, bestNextRowSequence);
			}
			if (LOG.isDebugEnabled())
				LOG.debug("Best with holdover: " + bestSequence.toString());
		}

		return bestSequence;
	}

	@Override
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
					freqLog = additiveSmoothing / 10.0;
				else if (minFrequency == 0)
					freqLog = additiveSmoothing;
				else
					freqLog = 1 + (Math.log(minFreq) / Math.log(frequencyLogBase));

				this.frequencyLogs.put(minFrequency, freqLog);
			}
			return freqLog;
		} else {
			if (minFrequency < 0)
				return additiveSmoothing / 10.0;
			else if (minFrequency == 0)
				return additiveSmoothing;
			else
				return 1;
		}
	}

	@Override
	public int getFrequency(LetterSequence letterSequence) {
		return this.getFrequency(letterSequence, true);
	}

	@Override
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
						LetterSequence prevCurrentSequence = letterGuesserService.getLetterSequence(prevSequence, subsequence);
						LetterSequence currentNextSequence = letterGuesserService.getLetterSequence(subsequence, nextSequence);
						LetterSequence prevCurrentNextSequence = letterGuesserService.getLetterSequence(prevCurrentSequence, nextSequence);

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

									LetterSequence prevNextSequence = letterGuesserService.getLetterSequence(prevCurrentSequence, nextSequence);
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

	public LexiconServiceInternal getLexiconServiceInternal() {
		return lexiconServiceInternal;
	}

	public void setLexiconServiceInternal(LexiconServiceInternal lexiconServiceInternal) {
		this.lexiconServiceInternal = lexiconServiceInternal;
	}

	@Override
	public WordSplitter getWordSplitter() {
		return wordSplitter;
	}

	@Override
	public void setWordSplitter(WordSplitter wordSplitter) {
		this.wordSplitter = wordSplitter;
	}

	public LetterGuesserService getLetterGuesserService() {
		return letterGuesserService;
	}

	public void setLetterGuesserService(LetterGuesserService letterGuesserService) {
		this.letterGuesserService = letterGuesserService;
	}

	@Override
	public double getFrequencyLogBase() {
		return frequencyLogBase;
	}

	@Override
	public void setFrequencyLogBase(double frequencyLogBase) {
		this.frequencyLogBase = frequencyLogBase;
	}

	@Override
	public double getAdditiveSmoothing() {
		return additiveSmoothing;
	}

	@Override
	public void setAdditiveSmoothing(double additiveSmoothing) {
		this.additiveSmoothing = additiveSmoothing;
	}

	@Override
	public Lexicon getLexicon() {
		return lexicon;
	}

	@Override
	public void setLexicon(Lexicon lexicon) {
		this.lexicon = lexicon;
	}

	@Override
	public boolean isFrequencyAdjusted() {
		return frequencyAdjusted;
	}

	@Override
	public void setFrequencyAdjusted(boolean frequencyAdjusted) {
		this.frequencyAdjusted = frequencyAdjusted;
	}
}
