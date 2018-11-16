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
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.Rectangle;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.talismane.machineLearning.ClassificationSolution;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.GeometricMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.Solution;
import com.joliciel.talismane.utils.CountedOutcome;

/**
 * A sequence of letter guesses associated with a given sequence of shapes, and
 * the attached score. There will be exactly one element in this sequence per
 * underlying shape, including possibly empty strings.
 * 
 * @author Assaf Urieli
 *
 */
public class LetterSequence implements Comparable<LetterSequence>, ClassificationSolution {
  // notice no dash in punctation, as these are always considered to be
  // attached
  // to the letters
  // private static final Pattern PUNCTUATION =
  // Pattern.compile("[\\p{Punct}&&[^\\-]]+",
  // Pattern.UNICODE_CHARACTER_CLASS);
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

  private final JochreSession jochreSession;

  public LetterSequence(ShapeSequence underlyingShapeSequence, JochreSession jochreSession) {
    this.jochreSession = jochreSession;
    this.setUnderlyingShapeSequence(underlyingShapeSequence);
  }

  /**
   * Create a letter sequence with space to one additional letter at the end
   * of an existing history.
   */
  public LetterSequence(LetterSequence history) {
    this.jochreSession = history.getJochreSession();
    this.letters.addAll(history.getLetters());
    this.decisions.addAll(history.getDecisions());
    this.setUnderlyingShapeSequence(history.getUnderlyingShapeSequence());
  }

  /**
   * Combine two sequences into one.
   */
  public LetterSequence(LetterSequence sequence1, LetterSequence sequence2) {
    jochreSession = sequence1 != null ? sequence1.getJochreSession() : sequence2.getJochreSession();
    if (sequence1 != null) {
      this.letters.addAll(sequence1.getLetters());
      this.decisions.addAll(sequence1.getDecisions());
      if (sequence1.getEndOfLineHyphenIndex() >= 0)
        this.setEndOfLineHyphenIndex(sequence1.getEndOfLineHyphenIndex());
    }
    if (sequence2 != null) {
      this.letters.addAll(sequence2.getLetters());
      this.decisions.addAll(sequence2.getDecisions());
      if (sequence2.getEndOfLineHyphenIndex() >= 0) {
        if (sequence1 != null)
          this.setEndOfLineHyphenIndex(sequence1.getLetters().size() + sequence2.getEndOfLineHyphenIndex());
        else
          this.setEndOfLineHyphenIndex(sequence2.getEndOfLineHyphenIndex());
      }
    }

    if (sequence1 != null && sequence2 != null) {
      GroupOfShapes group1 = sequence1.getUnderlyingShapeSequence().get(0).getShape().getGroup();
      GroupOfShapes group2 = sequence2.getUnderlyingShapeSequence().get(0).getShape().getGroup();
      if (!group1.equals(group2)) {
        this.setSplit(true);
        groupSequences = new ArrayList<LetterSequence>();
        groupSequences.add(sequence1);
        groupSequences.add(sequence2);
      }
    }

    ShapeSequence shapeSequence = new ShapeSequence(sequence1 == null ? null : sequence1.getUnderlyingShapeSequence(),
        sequence2 == null ? null : sequence2.getUnderlyingShapeSequence());
    this.setUnderlyingShapeSequence(shapeSequence);
  }

  public LetterSequence(ShapeSequence shapeSequence, List<String> letters, JochreSession jochreSession) {
    this.jochreSession = jochreSession;
    this.setUnderlyingShapeSequence(shapeSequence);
    this.letters = letters;
  }

  /**
   * Get this sequence's score based on individual letter scores.
   */
  @Override
  @SuppressWarnings("unchecked")

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
  public int compareTo(LetterSequence o) {
    if (this.equals(o))
      return 0;
    if (this.getScore() < o.getScore()) {
      return 1;
    } else if (this.getScore() > o.getScore()) {
      return -1;
    } else {
      return 1;
    }
  }

  @Override
  public synchronized String toString() {
    if (string == null) {
      string = "Sequence: " + this.getGuessedSequence();
    }
    return string;
  }

  /**
   * The real word behind this letter sequence, in cases where it's from the
   * training/test corpus.
   */
  public String getRealWord() {
    if (realWord == null) {
      realWord = this.getRealSequence();

      realWord = realWord.replace("[", "");
      realWord = realWord.replace("]", "");

      // split letters are joined back together
      realWord = realWord.replaceAll("\\|(.)\\1\\|", "$1");
      realWord = realWord.replaceAll("\\|(..)\\1\\|", "$1");
      realWord = realWord.replaceAll("\\|(...)\\1\\|", "$1");

      realWord = jochreSession.getLinguistics().standardiseWord(realWord);
    }
    return realWord;
  }

  /**
   * The guessed word.
   */
  public String getGuessedWord() {
    if (guessedWord == null) {
      guessedWord = this.getGuessedSequence();

      guessedWord = guessedWord.replace("[", "");
      guessedWord = guessedWord.replace("]", "");

      // split letters are joined back together
      guessedWord = guessedWord.replaceAll("\\|(.)\\1\\|", "$1");
      guessedWord = guessedWord.replaceAll("\\|(..)\\1\\|", "$1");
      guessedWord = guessedWord.replaceAll("\\|(...)\\1\\|", "$1");

      guessedWord = jochreSession.getLinguistics().standardiseWord(guessedWord);
    }
    return guessedWord;
  }

  /**
   * A string representation of the real sequence behind this letter sequence
   * (including split letters and inkspots).
   */
  public String getRealSequence() {
    if (realSequence == null) {
      Linguistics linguistics = jochreSession.getLinguistics();
      StringBuilder realWordBuilder = new StringBuilder();
      Shape lastShape = null;
      for (ShapeInSequence shapeInSequence : this.getUnderlyingShapeSequence()) {
        for (Shape originalShape : shapeInSequence.getOriginalShapes()) {
          if (!originalShape.equals(lastShape)) {
            String letter = originalShape.getLetter();
            if (letter.length() == 0)
              realWordBuilder.append("[]");
            else if (letter.length() > 1 && !linguistics.getDualCharacterLetters().contains(letter))
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

  /**
   * A string representation of the guessed sequence behind this letter
   * sequence (including split letters and inkspots).
   */
  public String getGuessedSequence() {
    if (guessedSequence == null) {
      Linguistics linguistics = jochreSession.getLinguistics();
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < letters.size(); i++) {
        String letter = letters.get(i);
        if (i == this.endOfLineHyphenIndex) {
          if (this.softHyphen) {
            continue;
          }
        }

        if (letter.length() == 0)
          builder.append("[]");
        else if (letter.length() > 1 && !linguistics.getDualCharacterLetters().contains(letter))
          builder.append("[" + letter + "]");
        else
          builder.append(letter);
      }
      guessedSequence = builder.toString();
    }
    return guessedSequence;
  }

  /**
   * The index of the shape representing a hyphen at the end of the line,
   * possibly a soft-hyphen (visible in word-wrapping only).
   */
  public int getEndOfLineHyphenIndex() {
    return endOfLineHyphenIndex;
  }

  public void setEndOfLineHyphenIndex(int dashToSkip) {
    this.endOfLineHyphenIndex = dashToSkip;
  }

  /**
   * The score, after adjustments to account for external factors (such as
   * corresponding word frequency in training corpus).
   */
  public double getAdjustedScore() {
    return adjustedScore;
  }

  public void setAdjustedScore(double adjustedScore) {
    this.adjustedScore = adjustedScore;
  }

  /**
   * The sequence of shapes underlying this letter sequence.
   */
  public ShapeSequence getUnderlyingShapeSequence() {
    return underlyingShapeSequence;
  }

  public void setUnderlyingShapeSequence(ShapeSequence underlyingShapeSequence) {
    this.underlyingShapeSequence = underlyingShapeSequence;
    this.underlyingSolutions.add(underlyingShapeSequence);
  }

  /**
   * Get the next shape in the underlying shape sequence (beyond what has
   * already been guessed by this letter sequence.
   */
  public ShapeInSequence getNextShape() {
    if (this.underlyingShapeSequence.size() <= this.letters.size())
      return null;
    else
      return this.underlyingShapeSequence.get(this.letters.size());
  }

  /**
   * The frequency of the word represented by this letter sequence.
   */
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

  @Override
  @SuppressWarnings("rawtypes")
  public ScoringStrategy getScoringStrategy() {
    return scoringStrategy;
  }

  @Override
  public void setScoringStrategy(@SuppressWarnings("rawtypes") ScoringStrategy scoringStrategy) {
    this.scoringStrategy = scoringStrategy;
  }

  /**
   * After breaking this letter sequence up into individual words, gives the
   * frequency for each word as found in the lexicon.
   */
  public List<CountedOutcome<String>> getWordFrequencies() {
    return wordFrequencies;
  }

  public void setWordFrequencies(List<CountedOutcome<String>> wordFrequencies) {
    this.wordFrequencies = wordFrequencies;
  }

  /**
   * A list of groups of shapes underlying this letter sequence.
   */
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

  /**
   * Whether or not this letter sequence is split across two lines.
   */
  public boolean isSplit() {
    return split;
  }

  public void setSplit(boolean split) {
    this.split = split;
  }

  /**
   * If this sequence contains any punctuation, returns individual sequences
   * representing letters and punctuation. Otherwise, returns the original
   * sequence.
   */
  public List<LetterSequence> getSubsequences() {
    if (subsequences == null) {
      subsequences = new ArrayList<LetterSequence>();
      List<String> currentLetters = new ArrayList<String>();
      ShapeSequence currentShapes = new ShapeSequence();
      boolean inPunctuation = false;
      boolean expectEndOfLineHyphen = false;

      for (int i = 0; i < this.letters.size(); i++) {
        String letter = this.letters.get(i);
        ShapeInSequence shape = this.underlyingShapeSequence.get(i);

        if (i == this.getEndOfLineHyphenIndex())
          expectEndOfLineHyphen = true;

        if (PUNCTUATION.matcher(letter).matches()) {
          if (!inPunctuation && currentLetters.size() > 0) {
            LetterSequence subsequence = this.getSubsequence(currentShapes, currentLetters);
            subsequences.add(subsequence);
            currentLetters = new ArrayList<String>();
            currentShapes = new ShapeSequence();
          }
          inPunctuation = true;
        } else {
          if (inPunctuation && currentLetters.size() > 0) {
            LetterSequence subsequence = this.getSubsequence(currentShapes, currentLetters);
            subsequence.setPunctation(true);
            if (expectEndOfLineHyphen) {
              this.setHyphenSubsequence(subsequence);
            }

            subsequences.add(subsequence);
            currentLetters = new ArrayList<String>();
            currentShapes = new ShapeSequence();
          }
          inPunctuation = false;
        }
        currentLetters.add(letter);
        currentShapes.addShape(shape.getShape());
      }
      if (currentLetters.size() > 0) {
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
    LetterSequence subsequence = new LetterSequence(shapeSequence, letters, jochreSession);
    return subsequence;
  }

  /**
   * Whether or not this letter sequence represents punctuation (in the case
   * of a subsequence).
   */
  public boolean isPunctation() {
    return punctation;
  }

  public void setPunctation(boolean punctation) {
    this.punctation = punctation;
  }

  /**
   * The letters in this sequence.
   */

  public List<String> getLetters() {
    return letters;
  }

  /**
   * Return the rectangle enclosing this letter sequence in a particular
   * group.
   */
  public Rectangle getRectangleInGroup(GroupOfShapes group) {
    return this.getUnderlyingShapeSequence().getRectangleInGroup(group);
  }

  /**
   * For a letter sequence covering two groups, split this letter sequence
   * into one sequence per group.
   */
  public List<LetterSequence> splitByGroup() {
    List<LetterSequence> letterSequences = new ArrayList<LetterSequence>();
    if (this.isSplit()) {

      Map<GroupOfShapes, LetterSequence> groupToLetterSequenceMap = new HashMap<GroupOfShapes, LetterSequence>();

      if (groupSequences != null) {
        letterSequences = groupSequences;
        for (LetterSequence letterSequence : letterSequences) {
          groupToLetterSequenceMap.put(letterSequence.getGroups().get(0), letterSequence);
        }
      } else {
        List<String> currentLetters = new ArrayList<String>();
        ShapeSequence currentShapes = new ShapeSequence();
        GroupOfShapes currentGroup = this.getGroups().get(0);

        for (int i = 0; i < this.letters.size(); i++) {
          String letter = this.letters.get(i);
          Shape shape = this.underlyingShapeSequence.get(i).getShape();
          if (!currentGroup.equals(shape.getGroup())) {
            LetterSequence letterSequence = new LetterSequence(currentShapes, currentLetters, jochreSession);
            letterSequence.setScore(this.getScore());
            letterSequence.setAdjustedScore(this.getAdjustedScore());
            groupToLetterSequenceMap.put(currentGroup, letterSequence);
            letterSequences.add(letterSequence);
            currentLetters = new ArrayList<String>();
            currentShapes = new ShapeSequence();
            currentGroup = shape.getGroup();
          }
          currentShapes.addShape(shape);
          currentLetters.add(letter);
        }
        if (currentLetters.size() > 0) {
          LetterSequence letterSequence = new LetterSequence(currentShapes, currentLetters, jochreSession);
          letterSequence.setScore(this.getScore());
          letterSequence.setAdjustedScore(this.getAdjustedScore());
          groupToLetterSequenceMap.put(currentGroup, letterSequence);
          letterSequences.add(letterSequence);
        }
      }

      GroupOfShapes currentGroup = this.getGroups().get(0);

      List<LetterSequence> newSubsequences = new ArrayList<LetterSequence>();
      for (LetterSequence subsequence : this.getSubsequences()) {
        if (subsequence.getHyphenSubsequence() != null) {
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
            if (oneSubsequence.getWordFrequencies().size() > 0) {
              currentSequence.getWordFrequencies().add(oneSubsequence.getWordFrequencies().get(0));
            }
          }
          currentSubsequences = new ArrayList<LetterSequence>();
          currentGroup = subsequence.getGroups().get(0);
        }
        currentSubsequences.add(subsequence);
      }
      if (currentSubsequences.size() > 0) {
        LetterSequence currentSequence = groupToLetterSequenceMap.get(currentGroup);
        currentSequence.setSubsequences(currentSubsequences);
        for (LetterSequence oneSubsequence : currentSubsequences) {
          if (oneSubsequence.getWordFrequencies().size() > 0) {
            currentSequence.getWordFrequencies().add(oneSubsequence.getWordFrequencies().get(0));
          }
        }
      }

      if (this.getHyphenSubsequence() != null)
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

  /**
   * A subsequence representing a hyphen at the end-of-line (either soft or
   * hard).
   */
  public LetterSequence getHyphenSubsequence() {
    return hyphenSubsequence;
  }

  public void setHyphenSubsequence(LetterSequence hyphenSubsequence) {
    this.hyphenSubsequence = hyphenSubsequence;
    hyphenSubsequence.setEndOfLineHyphenIndex(0);
  }

  /**
   * Does this letter sequence contain a soft hyphen (only visible because of
   * word wrapping and end-of-line)?
   */
  public boolean isSoftHyphen() {
    return softHyphen;
  }

  public void setSoftHyphen(boolean softHyphen) {
    this.guessedSequence = null;
    this.guessedWord = null;
    this.softHyphen = softHyphen;
  }

  /**
   * The most likely hyphenated string in the case of a hyphenated letter
   * sequence.
   */
  public String getHyphenatedString() {
    return hyphenatedString;
  }

  public void setHyphenatedString(String hyphenatedString) {
    this.hyphenatedString = hyphenatedString;
  }

  JochreSession getJochreSession() {
    return jochreSession;
  }

}
