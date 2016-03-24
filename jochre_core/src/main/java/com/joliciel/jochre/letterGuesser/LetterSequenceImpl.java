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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.Rectangle;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.GeometricMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.Solution;
import com.joliciel.talismane.utils.CountedOutcome;

/**
 * 
 * @author Assaf Urieli
 *
 */
final class LetterSequenceImpl implements Comparable<LetterSequenceImpl>, LetterSequence {
	// notice no dash in punctation, as these are always considered to be attached to the letters
//    private static final Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}&&[^\\-]]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}]+", Pattern.UNICODE_CHARACTER_CLASS);

	private double score = 0;
	private double adjustedScore = 0;
	private boolean scoreCalculated = false;
	private String string = null;
	private String realWord = null;
	private String guessedWord = null;
	private String realSequence = null;
	private String guessedSequence = null;
	private boolean split = false;
	
	private int endOfLineHyphenIndex = -1;
	private ShapeSequence underlyingShapeSequence;
	private int frequency = 0;
	private List<CountedOutcome<String>> wordFrequencies = new ArrayList<CountedOutcome<String>>();
	
	private List<Decision> decisions = new ArrayList<Decision>();
	private List<Solution> underlyingSolutions = new ArrayList<Solution>();
	@SuppressWarnings("rawtypes")
	private ScoringStrategy scoringStrategy = new GeometricMeanScoringStrategy();
	
	private List<LetterSequence> subsequences;
	private List<LetterSequence> groupSequences;
	private LetterSequence hyphenSubsequence = null;
	private List<String> letters = new ArrayList<String>();
	private boolean punctation = false;
	private boolean softHyphen = false;
	private String hyphenatedString = null;
	
	private BoundaryService boundaryService;
	private LetterGuesserServiceInternal letterGuesserService;
	
	public LetterSequenceImpl(ShapeSequence underlyingShapeSequence) {
		super();
		this.setUnderlyingShapeSequence(underlyingShapeSequence);
	}
	
	/**
	 * Create a letter sequence with space to one additional letter at the end
	 * of an existing history.
	 */
	public LetterSequenceImpl(LetterSequence history) {
		this.letters.addAll(history.getLetters());
		this.decisions.addAll(history.getDecisions());
		this.setUnderlyingShapeSequence(history.getUnderlyingShapeSequence());
	}
	
	/**
	 * Combine two sequences into one.
	 */
	public LetterSequenceImpl(LetterSequence sequence1, LetterSequence sequence2, BoundaryService boundaryService) {
		if (sequence1!=null) {
			this.letters.addAll(sequence1.getLetters());
			this.decisions.addAll(sequence1.getDecisions());
			if (sequence1.getEndOfLineHyphenIndex()>=0)
				this.setEndOfLineHyphenIndex(sequence1.getEndOfLineHyphenIndex());
		}
		if (sequence2!=null) {
			this.letters.addAll(sequence2.getLetters());
			this.decisions.addAll(sequence2.getDecisions());
			if (sequence2.getEndOfLineHyphenIndex()>=0) {
				if (sequence1!=null)
					this.setEndOfLineHyphenIndex(sequence1.getLetters().size() + sequence2.getEndOfLineHyphenIndex());
				else
					this.setEndOfLineHyphenIndex(sequence2.getEndOfLineHyphenIndex());
			}
		}
		
		if (sequence1!=null && sequence2!=null) {
			GroupOfShapes group1 = sequence1.getUnderlyingShapeSequence().get(0).getShape().getGroup();
			GroupOfShapes group2 = sequence2.getUnderlyingShapeSequence().get(0).getShape().getGroup();
			if (!group1.equals(group2)) {
				this.setSplit(true);
				groupSequences = new ArrayList<LetterSequence>();
				groupSequences.add(sequence1);
				groupSequences.add(sequence2);
			}
		}
		
		this.boundaryService = boundaryService;
		
		ShapeSequence shapeSequence = this.boundaryService.getShapeSequence(sequence1==null ? null : sequence1.getUnderlyingShapeSequence(), sequence2==null ? null : sequence2.getUnderlyingShapeSequence());
		this.setUnderlyingShapeSequence(shapeSequence);
	}
	
	public LetterSequenceImpl(ShapeSequence shapeSequence, List<String> letters) {
		this.setUnderlyingShapeSequence(shapeSequence);
		this.letters = letters;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.jochre.training.LetterSequence#getScore()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public double getScore() {
		if (!scoreCalculated) {
			score = this.getScoringStrategy().calculateScore(this);
			scoreCalculated = true;
		}
		return score;
	}
	
	public void setScore(double score) {
		this.score = score;
		scoreCalculated = true;
	}

	@Override
	public int compareTo(LetterSequenceImpl o) {
		if (this.equals(o))
			return 0;
		if (this.getScore()<o.getScore()) {
			return 1;
		} else if (this.getScore()>o.getScore()) {
			return -1;
		} else {
			return 1;
		}
	}

	@Override
	public synchronized String toString() {
		if (string==null) {
			string = "Sequence: " + this.getGuessedSequence();
		}
		return string;
	}
	

	@Override
	public String getRealWord() {
		if (realWord==null) {
			realWord = this.getRealSequence();
			
			realWord = realWord.replace("[", "");
			realWord = realWord.replace("]", "");
			
			// split letters are joined back together
			realWord = realWord.replaceAll("\\|(.)\\1\\|", "$1");
			realWord = realWord.replaceAll("\\|(..)\\1\\|", "$1");
			realWord = realWord.replaceAll("\\|(...)\\1\\|", "$1");
			
			realWord = JochreSession.getInstance().getLinguistics().standardiseWord(realWord);
		}
		return realWord;
	}
	

	@Override
	public String getGuessedWord() {
		if (guessedWord==null) {
			guessedWord = this.getGuessedSequence();
			
			guessedWord = guessedWord.replace("[", "");
			guessedWord = guessedWord.replace("]", "");
			
			// split letters are joined back together
			guessedWord = guessedWord.replaceAll("\\|(.)\\1\\|", "$1");
			guessedWord = guessedWord.replaceAll("\\|(..)\\1\\|", "$1");
			guessedWord = guessedWord.replaceAll("\\|(...)\\1\\|", "$1");

			guessedWord = JochreSession.getInstance().getLinguistics().standardiseWord(guessedWord);
		}
		return guessedWord;
	}

	@Override
	public String getRealSequence() {
		if (realSequence==null) {
			Linguistics linguistics = JochreSession.getInstance().getLinguistics();
			StringBuilder realWordBuilder = new StringBuilder();
			Shape lastShape = null;
			for (ShapeInSequence shapeInSequence : this.getUnderlyingShapeSequence()) {
				for (Shape originalShape : shapeInSequence.getOriginalShapes()) {
					if (!originalShape.equals(lastShape)) {
						String letter = originalShape.getLetter();
						if (letter.length()==0)
							realWordBuilder.append("[]");
						else if (letter.length()>1 && !linguistics.getDualCharacterLetters().contains(letter))
							realWordBuilder.append("[" + letter + "]");
						else
							realWordBuilder.append(letter);
					}
					lastShape = originalShape;
				}
			}
			realSequence = realWordBuilder.toString();
		}
		return realSequence;
	}

	@Override
	public String getGuessedSequence() {
		if (guessedSequence==null) {
			Linguistics linguistics = JochreSession.getInstance().getLinguistics();
			StringBuilder builder = new StringBuilder();
			for (int i=0; i<letters.size(); i++) {
				String letter = letters.get(i);
				if (i==this.endOfLineHyphenIndex) {
					if (this.softHyphen) {
						continue;
					}
				}
				
				if (letter.length()==0)
					builder.append("[]");
				else if (letter.length()>1 && !linguistics.getDualCharacterLetters().contains(letter))
					builder.append("[" + letter + "]");
				else
					builder.append(letter);
			}
			guessedSequence = builder.toString();
		}
		return guessedSequence;
	}

	public int getEndOfLineHyphenIndex() {
		return endOfLineHyphenIndex;
	}

	public void setEndOfLineHyphenIndex(int dashToSkip) {
		this.endOfLineHyphenIndex = dashToSkip;
	}

	@Override
	public double getAdjustedScore() {
		return adjustedScore;
	}
	
	@Override
	public void setAdjustedScore(double adjustedScore) {
		this.adjustedScore = adjustedScore;
	}

	public ShapeSequence getUnderlyingShapeSequence() {
		return underlyingShapeSequence;
	}

	public void setUnderlyingShapeSequence(ShapeSequence underlyingShapeSequence) {
		this.underlyingShapeSequence = underlyingShapeSequence;
		this.underlyingSolutions.add(underlyingShapeSequence);
	}

	@Override
	public ShapeInSequence getNextShape() {
		if (this.underlyingShapeSequence.size()<=this.letters.size())
			return null;
		else
			return this.underlyingShapeSequence.get(this.letters.size());
	}

	public BoundaryService getBoundaryService() {
		return boundaryService;
	}

	public void setBoundaryService(BoundaryService boundaryService) {
		this.boundaryService = boundaryService;
	}

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	@Override
	public List<Decision> getDecisions() {
		return this.decisions;
	}

	@Override
	public List<Solution> getUnderlyingSolutions() {
		return this.underlyingSolutions;
	}

	@Override
	public void addDecision(Decision decision) {
		this.decisions.add(decision);
	}

	@SuppressWarnings("rawtypes")
	public ScoringStrategy getScoringStrategy() {
		return scoringStrategy;
	}

	public void setScoringStrategy(@SuppressWarnings("rawtypes") ScoringStrategy scoringStrategy) {
		this.scoringStrategy = scoringStrategy;
	}

	public List<CountedOutcome<String>> getWordFrequencies() {
		return wordFrequencies;
	}
	
	public void setWordFrequencies(List<CountedOutcome<String>> wordFrequencies) {
		this.wordFrequencies = wordFrequencies;
	}

	@Override
	public List<GroupOfShapes> getGroups() {
		List<GroupOfShapes> groups = new ArrayList<GroupOfShapes>();

		GroupOfShapes currentGroup = this.getUnderlyingShapeSequence().get(0).getShape().getGroup();
		groups.add(currentGroup);
		for (ShapeInSequence shapeInSequence : this.getUnderlyingShapeSequence()) {
			if (!shapeInSequence.getShape().getGroup().equals(currentGroup)) {
				currentGroup = shapeInSequence.getShape().getGroup();
				groups.add(currentGroup);
			}
		}
		return groups;
	}

	@Override
	public boolean isSplit() {
		return split;
	}

	public void setSplit(boolean split) {
		this.split = split;
	}

	@Override
	public List<LetterSequence> getSubsequences() {
		if (subsequences==null) {
			subsequences = new ArrayList<LetterSequence>();
			List<String> currentLetters = new ArrayList<String>();
			ShapeSequence currentShapes = this.boundaryService.getEmptyShapeSequence();
			boolean inPunctuation = false;
			boolean expectEndOfLineHyphen = false;
			
			for (int i=0; i<this.letters.size(); i++) {
				String letter = this.letters.get(i);
				ShapeInSequence shape = this.underlyingShapeSequence.get(i);
				
				if (i==this.getEndOfLineHyphenIndex())
					expectEndOfLineHyphen = true;
				
				if (PUNCTUATION.matcher(letter).matches()) {
					if (!inPunctuation && currentLetters.size()>0) {
						LetterSequence subsequence = this.getSubsequence(currentShapes, currentLetters);
						subsequences.add(subsequence);
						currentLetters = new ArrayList<String>();
						currentShapes = this.boundaryService.getEmptyShapeSequence();
					}
					inPunctuation = true;
				} else {
					if (inPunctuation && currentLetters.size()>0) {
						LetterSequence subsequence = this.getSubsequence(currentShapes, currentLetters);
						subsequence.setPunctation(true);
						if (expectEndOfLineHyphen) {
							this.setHyphenSubsequence(subsequence);
						}
						
						subsequences.add(subsequence);
						currentLetters = new ArrayList<String>();
						currentShapes = this.boundaryService.getEmptyShapeSequence();
					}
					inPunctuation = false;
				}
				currentLetters.add(letter);
				currentShapes.addShape(shape.getShape());
			}
			if (currentLetters.size()>0) {
				LetterSequence subsequence = this.getSubsequence(currentShapes, currentLetters);
				subsequence.setPunctation(inPunctuation);
				if (inPunctuation && expectEndOfLineHyphen)
					this.setHyphenSubsequence(subsequence);
				subsequences.add(subsequence);
			}
		}
		return subsequences;
	}
	
	
	
	public void setSubsequences(List<LetterSequence> subsequences) {
		this.subsequences = subsequences;
	}

	private LetterSequence getSubsequence(ShapeSequence shapeSequence, List<String> letters) {
		LetterSequence subsequence = this.letterGuesserService.getLetterSequence(shapeSequence, letters);
		return subsequence;
	}

	public LetterGuesserServiceInternal getLetterGuesserService() {
		return letterGuesserService;
	}

	public void setLetterGuesserService(
			LetterGuesserServiceInternal letterGuesserService) {
		this.letterGuesserService = letterGuesserService;
	}

	public boolean isPunctation() {
		return punctation;
	}

	public void setPunctation(boolean punctation) {
		this.punctation = punctation;
	}

	public List<String> getLetters() {
		return letters;
	}

	@Override
	public Rectangle getRectangleInGroup(GroupOfShapes group) {
		return this.getUnderlyingShapeSequence().getRectangleInGroup(group);
	}

	@Override
	public List<LetterSequence> splitByGroup() {
		List<LetterSequence> letterSequences = new ArrayList<LetterSequence>();
		if (this.isSplit()) {
			
			Map<GroupOfShapes,LetterSequence> groupToLetterSequenceMap = new HashMap<GroupOfShapes,LetterSequence>();
			
			if (groupSequences!=null) {
				letterSequences = groupSequences;
				for (LetterSequence letterSequence : letterSequences) {
					groupToLetterSequenceMap.put(letterSequence.getGroups().get(0), letterSequence);
				}
			} else {
				List<String> currentLetters = new ArrayList<String>();
				ShapeSequence currentShapes = this.boundaryService.getEmptyShapeSequence();
				GroupOfShapes currentGroup = this.getGroups().get(0);
	
				for (int i=0; i<this.letters.size(); i++) {
					String letter = this.letters.get(i);
					Shape shape = this.underlyingShapeSequence.get(i).getShape();
					if (!currentGroup.equals(shape.getGroup())) {
						LetterSequence letterSequence = this.letterGuesserService.getLetterSequence(currentShapes, currentLetters);
						letterSequence.setScore(this.getScore());
						letterSequence.setAdjustedScore(this.getAdjustedScore());
						groupToLetterSequenceMap.put(currentGroup, letterSequence);
						letterSequences.add(letterSequence);
						currentLetters = new ArrayList<String>();
						currentShapes = this.boundaryService.getEmptyShapeSequence();
						currentGroup = shape.getGroup();
					}
					currentShapes.addShape(shape);
					currentLetters.add(letter);
				}
				if (currentLetters.size()>0) {
					LetterSequence letterSequence = this.letterGuesserService.getLetterSequence(currentShapes, currentLetters);
					letterSequence.setScore(this.getScore());
					letterSequence.setAdjustedScore(this.getAdjustedScore());
					groupToLetterSequenceMap.put(currentGroup, letterSequence);
					letterSequences.add(letterSequence);
				}
			}
			
			GroupOfShapes currentGroup = this.getGroups().get(0);
			
			List<LetterSequence> newSubsequences = new ArrayList<LetterSequence>();
			for (LetterSequence subsequence : this.getSubsequences()) {
				if (subsequence.getHyphenSubsequence()!=null) {
					// subsequence contains end-of-line hyphen
					// break it up into several subsequences
					List<LetterSequence> subsequencesByGroup = subsequence.getSubsequences();
					LetterSequence firstSubsequence = subsequencesByGroup.get(0);
					firstSubsequence.setHyphenSubsequence(subsequence.getHyphenSubsequence());
					newSubsequences.addAll(subsequencesByGroup);
					for (LetterSequence subsubsequence : subsequencesByGroup) {
						subsubsequence.setHyphenatedString(subsequence.getHyphenatedString());
					}
				} else {
					newSubsequences.add(subsequence);
				}
			}
			
			// assign my subsequences to the correct group
			List<LetterSequence> currentSubsequences = new ArrayList<LetterSequence>();
			for (LetterSequence subsequence : newSubsequences) {
				if (!subsequence.getGroups().get(0).equals(currentGroup)) {
					LetterSequence currentSequence = groupToLetterSequenceMap.get(currentGroup);
					currentSequence.setSubsequences(currentSubsequences);
					for (LetterSequence oneSubsequence : currentSubsequences) {
						if (oneSubsequence.getWordFrequencies().size()>0) {
							currentSequence.getWordFrequencies().add(oneSubsequence.getWordFrequencies().get(0));
						}
					}
					currentSubsequences = new ArrayList<LetterSequence>();
					currentGroup = subsequence.getGroups().get(0);
				}
				currentSubsequences.add(subsequence);
			}
			if (currentSubsequences.size()>0) {
				LetterSequence currentSequence = groupToLetterSequenceMap.get(currentGroup);
				currentSequence.setSubsequences(currentSubsequences);
				for (LetterSequence oneSubsequence : currentSubsequences) {
					if (oneSubsequence.getWordFrequencies().size()>0) {
						currentSequence.getWordFrequencies().add(oneSubsequence.getWordFrequencies().get(0));
					}
				}
			}
			
			if (this.getHyphenSubsequence()!=null)
				letterSequences.get(0).setHyphenSubsequence(this.getHyphenSubsequence());
			
			for (LetterSequence letterSequence : letterSequences) {
				letterSequence.setScore(this.getScore());
				letterSequence.setAdjustedScore(this.getAdjustedScore());
			}
		} else {
			letterSequences.add(this);
		}
		return letterSequences;
	}

	public LetterSequence getHyphenSubsequence() {
		return hyphenSubsequence;
	}

	public void setHyphenSubsequence(LetterSequence hyphenSubsequence) {
		this.hyphenSubsequence = hyphenSubsequence;
		hyphenSubsequence.setEndOfLineHyphenIndex(0);
	}

	public boolean isSoftHyphen() {
		return softHyphen;
	}

	public void setSoftHyphen(boolean softHyphen) {
		this.guessedSequence = null;
		this.guessedWord = null;
		this.softHyphen = softHyphen;
	}

	public String getHyphenatedString() {
		return hyphenatedString;
	}

	public void setHyphenatedString(String hyphenatedString) {
		this.hyphenatedString = hyphenatedString;
	}
	
	
}
