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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.letterGuesser.Letter;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;

class MostLikelyWordChooserImpl implements MostLikelyWordChooser {
	private static final Log LOG = LogFactory.getLog(MostLikelyWordChooserImpl.class);
	private double additiveSmoothing = 0.5;
	private double frequencyLogBase = 2.0;
	
	Map<Integer, Double> frequencyLogs = new HashMap<Integer, Double>();
	LexiconServiceInternal lexiconServiceInternal = null;
	LetterGuesserService letterGuesserService = null;
	WordSplitter wordSplitter;
	Lexicon lexicon;
	Writer unknownWordWriter;
	
	public LetterSequence chooseMostLikelyWord(
			List<LetterSequence> heap, List<LetterSequence> holdoverHeap, int n) {
		LetterSequence bestSequence = null;
		
		List<LetterSequence> holdoverWithDash = new ArrayList<LetterSequence>(n);
		List<LetterSequence> holdoverWithoutDash = new ArrayList<LetterSequence>(n);
		
		int i = 0;
		for (LetterSequence holdoverSequence : holdoverHeap) {
			if (i>=n)
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
			for (int j = sequenceWithDash.size()-1; j>=0; j--) {
				Letter outcome = sequenceWithDash.get(j);
				if (outcome.getString().equals("-")) {
					sequenceWithDash.setDashToSkip(j);
					break;
				}
			}
			for (LetterSequence letterSequence : heap) {
				LetterSequence combinedSequence = this.getLetterGuesserService().getLetterSequence(sequenceWithDash, letterSequence);
				combinedHeap.add(combinedSequence);
			}
		}
		
		List<LetterSequence> combinedSequences = new ArrayList<LetterSequence>();
		for (i=0;i<n;i++) {
			if (combinedHeap.isEmpty())
				break;
			combinedSequences.add(combinedHeap.poll());
		}
		
		if (holdoverWithoutDash.size()==0) {
			// all holdovers end with a dash
			// therefore we must combine the two sequences
			bestSequence = this.chooseMostLikelyWord(combinedSequences, n);
			
		} else {
			// some holdovers end with a dash, others don't
			// need to compare combined sequences with individual sequences
			LetterSequence bestCombinedSequence = this.chooseMostLikelyWord(combinedSequences, n);
			
			// Originally we only included sequences without dashes here
			// However, this falsifies the results towards those without a dash
			// especially in the case where sequence 1 or sequence 2 is also a common word (e.g. der in Yiddish)
//			PriorityQueue<LetterSequence> holdoverHeapWithoutDash = new PriorityQueue<LetterSequence>(holdoverWithoutDash);
//			LetterSequence bestHoldoverSequenceWithoutDash = this.chooseMostLikelyWord(holdoverHeapWithoutDash, n);
			// Changed it to the following:
			LetterSequence bestHoldoverSequence = this.chooseMostLikelyWord(holdoverHeap, n);
			LetterSequence bestNextRowSequence = this.chooseMostLikelyWord(heap, n);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Best combined: " + bestCombinedSequence.toString() + ". Adjusted score: " + bestCombinedSequence.getAdjustedScore());
				LOG.debug("Best seq1 separate: " + bestHoldoverSequence.toString() + ". Adjusted score: " + bestHoldoverSequence.getAdjustedScore());
				LOG.debug("Best seq2 separate: " + bestNextRowSequence.toString() + ". Adjusted score: " + bestNextRowSequence.getAdjustedScore());
			}
			
			// Now, to compare the best combined with the best separate scores, we need to get a geometric mean of the shapes
			// in the best separate ones, and adjust for the lowest frequency word
			LetterSequence separateSequence = this.letterGuesserService.getLetterSequence(bestHoldoverSequence, bestNextRowSequence);
			int minFrequency = bestHoldoverSequence.getFrequency() < bestNextRowSequence.getFrequency() ? bestHoldoverSequence.getFrequency() : bestNextRowSequence.getFrequency();
			double freqLog = this.getFrequencyAdjustment(minFrequency);
			double separateAdjustedScore = separateSequence.getScore() * freqLog + additiveSmoothing;
			separateSequence.setAdjustedScore(separateAdjustedScore);
			if (LOG.isDebugEnabled())
				LOG.debug("Best separate: " + separateSequence.toString() + ". Score: " + separateSequence.getScore() + ". Freq: " + minFrequency + ". Adjusted: " + freqLog + ". Adjusted score: " + separateSequence.getAdjustedScore());
				
			if (bestCombinedSequence.getAdjustedScore()>separateAdjustedScore) {
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
	public LetterSequence chooseMostLikelyWord(
			List<LetterSequence> heap, int n) {
		int i = 0;
		double bestScore = Double.NEGATIVE_INFINITY;
		LetterSequence bestSequence = null;
		int bestSequencyFrequency = 0;
		
		List<LetterSequence> sequences = new ArrayList<LetterSequence>(n);
		for (LetterSequence sequence : heap) {
			if (i>=n)
				break;
			sequences.add(sequence);
			
			StringBuilder sb = new StringBuilder();
			int j = 0;
			for (Letter outcome : sequence) {
				// we skip a dash which separates two rows if required
				if (outcome!=null && j!=sequence.getDashToSkip())
					sb.append(outcome.getString());
				j++;
			}
			String wordText = sb.toString();
			int minFrequency = Integer.MAX_VALUE;

			List<String> words = this.getWordSplitter().splitText(wordText);
			
			for (String word : words) {
				int frequency = this.lexicon.getFrequency(word);
				if (frequency < minFrequency)
					minFrequency = frequency;
			}
			
			sequence.setFrequency(minFrequency);
			double freqLog = this.getFrequencyAdjustment(minFrequency);
			
			double adjustedScore = sequence.getScore() * freqLog;
			sequence.setAdjustedScore(adjustedScore);
			if (LOG.isDebugEnabled())
				LOG.debug(sequence.toString() + ". Score: " + sequence.getScore() + ". Freq: " + minFrequency + ". Adjusted: " + freqLog + ". Adjusted score: " + adjustedScore);
			if (adjustedScore>bestScore) {
				bestSequence = sequence;
				bestScore = adjustedScore;
				bestSequencyFrequency = minFrequency;
			}
			i++;
		}
		if (LOG.isDebugEnabled())
			LOG.debug("Best: " + bestSequence.toString());
		
		if (bestSequencyFrequency==0 && this.unknownWordWriter!=null) {
			try {
				this.unknownWordWriter.write(bestSequence.toString() + "\n");
				this.unknownWordWriter.flush();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return bestSequence;
	}
	
	double getFrequencyAdjustment(int minFrequency) {
		Double freqLogObj = this.frequencyLogs.get(minFrequency);
		double freqLog = 0;
		if (freqLogObj!=null)
			freqLog = freqLogObj.doubleValue();
		else {
			// The base is log2 of the frequency + 1
			// 0 = log2(1) = 0
			// 1 = log2(2) = 1
			// 3 = log2(4) = 2
			// 7 = log2(8) = 3
			// etc.
			// To this we add additive smoothing to allow for unknown words
			double minFreq = minFrequency;
			if (minFreq<0)
				minFreq = -0.9;
			freqLog = (Math.log(minFreq + 1.0) / Math.log(frequencyLogBase)) + additiveSmoothing;
			this.frequencyLogs.put(minFrequency, freqLog);
		}
		return freqLog;
	}


	public LexiconServiceInternal getLexiconServiceInternal() {
		return lexiconServiceInternal;
	}

	public void setLexiconServiceInternal(
			LexiconServiceInternal lexiconServiceInternal) {
		this.lexiconServiceInternal = lexiconServiceInternal;
	}

	public WordSplitter getWordSplitter() {
		return wordSplitter;
	}

	public void setWordSplitter(WordSplitter wordSplitter) {
		this.wordSplitter = wordSplitter;
	}

	public LetterGuesserService getLetterGuesserService() {
		return letterGuesserService;
	}

	public void setLetterGuesserService(LetterGuesserService letterGuesserService) {
		this.letterGuesserService = letterGuesserService;
	}

	public double getFrequencyLogBase() {
		return frequencyLogBase;
	}

	public void setFrequencyLogBase(double frequencyLogBase) {
		this.frequencyLogBase = frequencyLogBase;
	}

	public double getAdditiveSmoothing() {
		return additiveSmoothing;
	}

	public void setAdditiveSmoothing(double additiveSmoothing) {
		this.additiveSmoothing = additiveSmoothing;
	}

	public Lexicon getLexicon() {
		return lexicon;
	}

	public void setLexicon(Lexicon lexicon) {
		this.lexicon = lexicon;
	}

	public Writer getUnknownWordWriter() {
		return unknownWordWriter;
	}

	public void setUnknownWordWriter(Writer unknownWordWriter) {
		this.unknownWordWriter = unknownWordWriter;
	}

}
