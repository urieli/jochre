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
import java.util.List;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.GeometricMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.Solution;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * 
 * @author Assaf Urieli
 *
 */
final class LetterSequenceImpl extends ArrayList<String> implements Comparable<LetterSequenceImpl>, LetterSequence {
	private static final long serialVersionUID = 7846011721688179506L;

	private double score = 0;
	private double adjustedScore = 0;
	private boolean scoreCalculated = false;
	private String string = null;
	private String realWord = null;
	private String guessedWord = null;
	private String realSequence = null;
	private String guessedSequence = null;
	private boolean split = false;
	
	private int dashToSkip = -1;
	private ShapeSequence underlyingShapeSequence;
	private BoundaryService boundaryService;
	private int frequency = 0;
	private List<WeightedOutcome<String>> wordFrequencies = new ArrayList<WeightedOutcome<String>>();
	
	private List<Decision> decisions = new ArrayList<Decision>();
	private List<Solution> underlyingSolutions = new ArrayList<Solution>();
	@SuppressWarnings("rawtypes")
	private ScoringStrategy scoringStrategy = new GeometricMeanScoringStrategy();
	
	public LetterSequenceImpl(ShapeSequence underlyingShapeSequence) {
		super();
		this.setUnderlyingShapeSequence(underlyingShapeSequence);
	}
	
	public LetterSequenceImpl(ShapeSequence underlyingShapeSequence, int initialCapacity) {
		super(initialCapacity);
		this.setUnderlyingShapeSequence(underlyingShapeSequence);
	}
	
	/**
	 * Create a letter sequence with space to one additional letter at the end
	 * of an existing history.
	 * @param history
	 */
	public LetterSequenceImpl(LetterSequence history) {
		super(history.size()+1);
		this.addAll(history);
		this.decisions.addAll(history.getDecisions());
		this.setUnderlyingShapeSequence(history.getUnderlyingShapeSequence());
	}
	
	/**
	 * Combine two sequences into one.
	 * @param sequence1
	 * @param sequence2
	 */
	public LetterSequenceImpl(LetterSequence sequence1, LetterSequence sequence2, BoundaryService boundaryService) {
		super(sequence1.size() + sequence2.size());
		this.setSplit(true);
		this.addAll(sequence1);
		this.addAll(sequence2);
		this.decisions.addAll(sequence1.getDecisions());
		this.decisions.addAll(sequence2.getDecisions());
		
		this.setDashToSkip(sequence1.getDashToSkip());
		this.boundaryService = boundaryService;
		
		ShapeSequence shapeSequence = this.boundaryService.getShapeSequence(sequence1.getUnderlyingShapeSequence(), sequence2.getUnderlyingShapeSequence());
		this.setUnderlyingShapeSequence(shapeSequence);
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
			realWord = realWord.replaceAll("\\|(.){2}\\|", "$1");
			realWord = realWord.replaceAll("\\|(..){2}\\|", "$1");
			realWord = realWord.replaceAll("\\|(...){2}\\|", "$1");

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
			guessedWord = guessedWord.replaceAll("\\|(.){2}\\|", "$1");
			guessedWord = guessedWord.replaceAll("\\|(..){2}\\|", "$1");
			guessedWord = guessedWord.replaceAll("\\|(...){2}\\|", "$1");

		}
		return guessedWord;
	}

	@Override
	public String getRealSequence() {
		if (realSequence==null) {
			JochreSession jochreSession = JochreSession.getInstance();
			Linguistics linguistics = Linguistics.getInstance(jochreSession.getLocale());
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
			JochreSession jochreSession = JochreSession.getInstance();
			Linguistics linguistics = Linguistics.getInstance(jochreSession.getLocale());
			StringBuilder builder = new StringBuilder();
			for (String letter : this) {
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

	public int getDashToSkip() {
		return dashToSkip;
	}

	public void setDashToSkip(int dashToSkip) {
		this.dashToSkip = dashToSkip;
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
		if (this.underlyingShapeSequence.size()<=this.size())
			return null;
		else
			return this.underlyingShapeSequence.get(this.size());
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

	public List<WeightedOutcome<String>> getWordFrequencies() {
		return wordFrequencies;
	}

	@Override
	public GroupOfShapes getFirstGroup() {
		return this.getUnderlyingShapeSequence().get(0).getShape().getGroup();
	}

	@Override
	public boolean isSplit() {
		return split;
	}

	public void setSplit(boolean split) {
		this.split = split;
	}


}
