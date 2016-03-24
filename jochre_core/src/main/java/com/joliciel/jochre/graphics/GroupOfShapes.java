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
package com.joliciel.jochre.graphics;

import java.util.List;

import com.joliciel.jochre.Entity;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.talismane.utils.CountedOutcome;

/**
 * A group of shapes within a row, corresponding a single orthographic word.
 * @author Assaf Urieli
 *
 */
public interface GroupOfShapes extends Entity, Rectangle {
	/**
	 * The shapes contained on this group.
	 */
	public List<Shape> getShapes();
	
	/**
	 * Add a shape to this group's shapes.
	 */
	public void addShape(Shape shape);
	
	/**
	 * The index of this group, from 0 (first word on row, left-most on left-to-right languages) to n.
	 */
	public int getIndex();
	public void setIndex(int index);
	
	public int getRowId();
	
	/**
	 * The Row containing this group.
	 */
	public RowOfShapes getRow();
	

	/**
	 * The leftmost x coordinate of this group (based on the shapes it contains).
	 */
	public int getLeft();
	
	/**
	 * The leftmost y coordinate of this group (based on the shapes it contains).
	 */
	public int getTop();

	/**
	 * The rightmost x coordinate of this group (based on the shapes it contains).
	 */
	public int getRight();
	
	/**
	 * The bottom-most y coordinate of this group (based on the shapes it contains).
	 */
	public int getBottom();
	
	/**
	 * The letters of the shapes comprising this group combined into a single word.
	 */
	public String getWord();	
	
	/**
	 * Returns the word excluding opening and closing punctuation.
	 */
	public String getWordForIndex();
	
	/**
	 * Recalculate the various statistical measurements for this group.
	 * Should be called after the group has had any shapes added or removed.
	 */
	public void recalculate();
	
	
	/**
	 * Returns the meanline for this row
	 * in the form {left, top, right, bottom}
	 */
	public int[] getMeanLine();
	
	/**
	 * Returns the baseline for this row
	 * in the form {left, top, right, bottom}
	 */
	public int[] getBaseLine();

	/**
	 * Distance from base-line to mean-line.
	 */
	public abstract int getXHeight();

	/**
	 * If the group is at the end of a row and ends with a hyphen,
	 * is this hyphen a hard hyphen (would be in the word whether or not it was at the end of a row)
	 * or a soft hyphen (would disappear if the word was in the middle of a row).
	 */
	public abstract boolean isHardHyphen();
	public abstract void setHardHyphen(boolean hardHyphen);
	
	/**
	 * Set to true if this group represents a broken word, which is only partially
	 * formed by the shapes visible on the page.
	 * Broken words will not be added to the internal glossary.
	 */
	public boolean isBrokenWord();
	public void setBrokenWord(boolean brokenWord);
	
	/**
	 * Does this group border with a segmentation problem that needs to be reviewed.
	 */
	public boolean isSegmentationProblem();
	public void setSegmentationProblem(boolean segmentationProblem);
	
	/**
	 * Should this group be skipped for training and evaluation?
	 */
	public boolean isSkip();
	public void setSkip(boolean skip);
	
	/**
	 * The frequency of the word represented by this letter sequence.
	 */
	public int getFrequency();
	
	/**
	 * A list of shapes, as corrected for merged shapes, split shapes and specks.
	 */
	public List<Shape> getCorrectedShapes();
	
	/**
	 * Returns the best letter sequence for this group, if any, or null if none.
	 */
	public LetterSequence getBestLetterSequence();
	public void setBestLetterSequence(LetterSequence bestLetterSequence);
	
	/**
	 * Returns the subsequences of the best letter sequence.
	 */
	public List<LetterSequence> getSubsequences();

	/**
	 * Whether or not the best letter sequence for this group is split across two lines.
	 */
	public boolean isSplit();
	
	/**
	 * The word frequencies for the best letter sequence.
	 */
	public List<CountedOutcome<String>> getWordFrequencies();
	
	/**
	 * The confidence in the current word guess, in a scale from 0 to 1.
	 */
	public double getConfidence();
	
	/**
	 * A rectangle defining the space preceding this group, or null if first group.
	 */
	public Rectangle getPrecedingSpace();
}
