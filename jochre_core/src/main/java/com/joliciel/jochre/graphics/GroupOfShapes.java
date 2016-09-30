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

import java.util.ArrayList;
import java.util.List;

import com.joliciel.jochre.Entity;
import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.talismane.utils.CountedOutcome;

/**
 * A group of shapes within a row, corresponding a single orthographic word.
 * 
 * @author Assaf Urieli
 *
 */
public class GroupOfShapes implements Entity, Rectangle {
	private int id;

	private List<Shape> shapes;
	private List<Shape> correctedShapes;
	private int index;
	private int rowId;
	private RowOfShapes row = null;

	private boolean hardHyphen = false;
	private boolean brokenWord = false;
	private boolean segmentationProblem = false;
	private boolean skip = false;

	private boolean coordinatesFound = false;
	private int left;
	private int top;
	private int right;
	private int bottom;
	private int xHeight = -1;

	private int[] meanLine = null;
	private int[] baseLine = null;

	private boolean dirty = true;

	private LetterSequence bestLetterSequence = null;
	private Boolean junk = null;

	private final JochreSession jochreSession;
	private final GraphicsDao graphicsDao;

	public GroupOfShapes(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.graphicsDao = GraphicsDao.getInstance(jochreSession);
	}

	/**
	 * The shapes contained on this group.
	 */
	public List<Shape> getShapes() {
		if (shapes == null) {
			if (this.id == 0)
				shapes = new ArrayList<Shape>();
			else {
				shapes = this.graphicsDao.findShapes(this);
				for (Shape shape : shapes) {
					shape.setGroup(this);
				}
			}
		}
		return shapes;
	}

	/**
	 * Add any shapes from the list which belong to this group.
	 */
	void addShapes(List<Shape> shapesToAdd) {
		if (this.shapes == null) {
			this.shapes = new ArrayList<Shape>();
			for (Shape shape : shapesToAdd) {
				if (shape.getGroupId() == this.getId()) {
					this.shapes.add(shape);
					shape.setGroup(this);
				}
			}
		}
	}

	/**
	 * Add a shape to this group's shapes.
	 */
	public void addShape(Shape shape) {
		this.getShapes().add(shape);
		shape.setGroup(this);
	}

	/**
	 * The index of this group, from 0 (first word on row, left-most on
	 * left-to-right languages) to n.
	 */
	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		if (this.index != index) {
			this.index = index;
			dirty = true;
		}
	}

	public int getRowId() {
		return rowId;
	}

	void setRowId(int rowId) {
		if (this.rowId != rowId) {
			this.rowId = rowId;
			dirty = true;
		}
	}

	/**
	 * The Row containing this group.
	 */
	public RowOfShapes getRow() {
		if (this.row == null && this.rowId != 0) {
			this.row = this.graphicsDao.loadRowOfShapes(this.rowId);
		}
		return row;
	}

	void setRow(RowOfShapes row) {
		this.row = row;
		if (row != null)
			this.setRowId(row.getId());
		else
			this.setRowId(0);
	}

	@Override
	public void save() {
		if (this.row != null && this.rowId == 0)
			this.setRowId(this.row.getId());

		if (this.dirty)
			this.graphicsDao.saveGroupOfShapes(this);

		if (this.shapes != null) {
			int index = 0;
			for (Shape shape : this.shapes) {
				shape.setGroup(this);
				shape.setIndex(index++);
				shape.save();
			}
		}
	}

	/**
	 * The leftmost x coordinate of this group (based on the shapes it contains).
	 */
	@Override
	public int getLeft() {
		this.findCoordinates();
		return this.left;
	}

	/**
	 * The topmost y coordinate of this group (based on the shapes it contains).
	 */
	@Override
	public int getTop() {
		this.findCoordinates();
		return this.top;
	}

	/**
	 * The rightmost x coordinate of this group (based on the shapes it contains).
	 */
	@Override
	public int getRight() {
		this.findCoordinates();
		return this.right;
	}

	/**
	 * The bottom-most y coordinate of this group (based on the shapes it
	 * contains).
	 */
	@Override
	public int getBottom() {
		this.findCoordinates();
		return this.bottom;
	}

	private void findCoordinates() {
		if (!coordinatesFound) {
			Shape firstShape = this.getShapes().iterator().next();
			left = firstShape.getLeft();
			top = firstShape.getTop();
			right = firstShape.getRight();
			bottom = firstShape.getBottom();

			for (Shape shape : shapes) {
				if (shape.getLeft() < left)
					left = shape.getLeft();
				if (shape.getTop() < top)
					top = shape.getTop();
				if (shape.getRight() > right)
					right = shape.getRight();
				if (shape.getBottom() > bottom)
					bottom = shape.getBottom();
			}
			coordinatesFound = true;
		}
	}

	/**
	 * The letters of the shapes comprising this group combined into a single
	 * word.
	 */
	public String getWord() {
		StringBuilder sb = new StringBuilder();
		for (Shape shape : this.getCorrectedShapes()) {
			if (shape.getLetter() != null)
				sb.append(shape.getLetter());
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		if (this.id == 0)
			return super.hashCode();
		else
			return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this.id == 0) {
			return super.equals(obj);
		} else {
			GroupOfShapes otherGroup = (GroupOfShapes) obj;
			return (this.getId() == otherGroup.getId());
		}
	}

	/**
	 * Recalculate the various statistical measurements for this group. Should be
	 * called after the group has had any shapes added or removed.
	 */
	public void recalculate() {
		this.coordinatesFound = false;
		this.meanLine = null;
		this.baseLine = null;
	}

	@Override
	public String toString() {
		return "Group " + this.getIndex() + ", left(" + this.getLeft() + ")" + ", top(" + this.getTop() + ")" + ", right(" + this.getRight() + ")" + ", bot("
				+ this.getBottom() + ")" + " [id=" + this.getId() + "]";
	}

	/**
	 * Returns the meanline for this row in the form {left, top, right, bottom}
	 */
	public int[] getMeanLine() {
		this.getGuideLines();
		return this.meanLine;
	}

	/**
	 * Returns the baseline for this row in the form {left, top, right, bottom}
	 */
	public int[] getBaseLine() {
		this.getGuideLines();
		return this.baseLine;
	}

	private void getGuideLines() {
		if (this.meanLine == null) {
			Shape leftMostShape = null;
			Shape rightMostShape = null;
			for (Shape shape : this.getShapes()) {
				if (leftMostShape == null) {
					leftMostShape = shape;
					rightMostShape = shape;
				} else {
					if (shape.getLeft() < leftMostShape.getLeft())
						leftMostShape = shape;
					if (shape.getRight() > rightMostShape.getRight())
						rightMostShape = shape;
				}
			}
			int[] meanLine = { leftMostShape.getLeft(), leftMostShape.getTop() + leftMostShape.getMeanLine(), rightMostShape.getRight(),
					rightMostShape.getTop() + rightMostShape.getMeanLine() };
			int[] baseLine = { leftMostShape.getLeft(), leftMostShape.getTop() + leftMostShape.getBaseLine(), rightMostShape.getRight(),
					rightMostShape.getTop() + rightMostShape.getBaseLine() };

			this.meanLine = meanLine;
			this.baseLine = baseLine;
		}
	}

	/**
	 * Distance from base-line to mean-line.
	 */
	public int getXHeight() {
		if (xHeight < 0) {
			Shape firstShape = this.getShapes().get(0);
			xHeight = firstShape.getBaseLine() - firstShape.getMeanLine();
		}
		return xHeight;
	}

	/**
	 * If the group is at the end of a row and ends with a hyphen, is this hyphen
	 * a hard hyphen (would be in the word whether or not it was at the end of a
	 * row) or a soft hyphen (would disappear if the word was in the middle of a
	 * row).
	 */
	public boolean isHardHyphen() {
		return hardHyphen;
	}

	public void setHardHyphen(boolean hardHyphen) {
		if (this.hardHyphen != hardHyphen) {
			this.hardHyphen = hardHyphen;
			dirty = true;
		}
	}

	/**
	 * Set to true if this group represents a broken word, which is only partially
	 * formed by the shapes visible on the page. Broken words will not be added to
	 * the internal glossary.
	 */
	public boolean isBrokenWord() {
		return brokenWord;
	}

	public void setBrokenWord(boolean brokenWord) {
		if (this.brokenWord != brokenWord) {
			this.brokenWord = brokenWord;
			dirty = true;
		}
	}

	boolean isDirty() {
		return dirty;
	}

	void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	/**
	 * Does this group border with a segmentation problem that needs to be
	 * reviewed.
	 */
	public boolean isSegmentationProblem() {
		return segmentationProblem;
	}

	public void setSegmentationProblem(boolean segmentationProblem) {
		if (this.segmentationProblem != segmentationProblem) {
			this.segmentationProblem = segmentationProblem;
			dirty = true;
		}
	}

	/**
	 * Should this group be skipped for training and evaluation?
	 */
	public boolean isSkip() {
		return skip;
	}

	public void setSkip(boolean skip) {
		if (this.skip != skip) {
			this.skip = skip;
			dirty = true;
		}
	}

	/**
	 * The frequency of the word represented by this letter sequence.
	 */
	public int getFrequency() {
		if (this.bestLetterSequence != null)
			return this.bestLetterSequence.getFrequency();
		return -1;
	}

	/**
	 * Whether or not the best letter sequence for this group is split across two
	 * lines.
	 */
	public boolean isSplit() {
		if (this.bestLetterSequence != null)
			return this.bestLetterSequence.isSplit();
		return false;
	}

	/**
	 * A list of shapes, as corrected for merged shapes, split shapes and specks.
	 */
	public List<Shape> getCorrectedShapes() {
		if (this.correctedShapes == null) {
			correctedShapes = new ArrayList<Shape>(shapes.size());
			List<Shape> splitShapes = null;
			boolean haveSplitShape = false;
			int mergedTop = 0;
			int mergedBottom = 0;
			int mergedLeft = 0;
			int mergedRight = 0;
			String lastLetter = "";
			for (Shape shape : shapes) {
				if (shape.getLetter().length() == 0) {
					// do nothing
				} else if (shape.getLetter().contains("|") && haveSplitShape) {
					// end of a split shape
					Shape mergedShape = shape.getJochreImage().getShape(mergedLeft, mergedTop, mergedRight, mergedBottom);
					StringBuilder currentSequence = new StringBuilder();
					double confidence = 1.0;
					for (Shape splitShape : splitShapes) {
						String letter = splitShape.getLetter();
						confidence = confidence * splitShape.getConfidence();
						if (letter.startsWith("|")) {
							// beginning of a gehakte letter
							currentSequence.append(letter);
							continue;
						} else if (letter.endsWith("|")) {
							// end of a gehakte letter
							if (currentSequence.length() > 0 && currentSequence.charAt(0) == '|') {
								String letter1 = currentSequence.toString().substring(1);
								String letter2 = letter.substring(0, letter.length() - 1);
								if (letter1.equals(letter2)) {
									letter = letter1;
								} else {
									letter = currentSequence.toString() + letter;
								}
								currentSequence = new StringBuilder();
							}
						}
					}
					mergedShape.setLetter(currentSequence.toString());
					mergedShape.setConfidence(confidence);
					correctedShapes.add(mergedShape);
					splitShapes = null;
					haveSplitShape = false;
				} else if (shape.getLetter().contains("|") || haveSplitShape) {
					if (!haveSplitShape) {
						// first shape in split
						haveSplitShape = true;
						splitShapes = new ArrayList<Shape>(2);
						splitShapes.add(shape);
						mergedTop = shape.getTop();
						mergedBottom = shape.getBottom();
						mergedLeft = shape.getLeft();
						mergedRight = shape.getRight();
					} else {
						splitShapes.add(shape);
						if (shape.getTop() < mergedTop)
							mergedTop = shape.getTop();
						if (shape.getLeft() < mergedLeft)
							mergedLeft = shape.getLeft();
						if (shape.getBottom() > mergedBottom)
							mergedBottom = shape.getBottom();
						if (shape.getRight() > mergedRight)
							mergedRight = shape.getRight();
					}
				} else if ((shape.getLetter().equals(",") && lastLetter.equals(",")) || (shape.getLetter().equals("'") && lastLetter.equals("'"))) {
					// TODO: specific to Yiddish, need to generalise
					mergedTop = shape.getTop();
					mergedBottom = shape.getBottom();
					mergedLeft = shape.getLeft();
					mergedRight = shape.getRight();
					Shape lastShape = correctedShapes.remove(correctedShapes.size() - 1);
					if (lastShape.getTop() < mergedTop)
						mergedTop = lastShape.getTop();
					if (lastShape.getLeft() < mergedLeft)
						mergedLeft = lastShape.getLeft();
					if (lastShape.getBottom() > mergedBottom)
						mergedBottom = lastShape.getBottom();
					if (lastShape.getRight() > mergedRight)
						mergedRight = lastShape.getRight();
					Shape mergedShape = shape.getJochreImage().getShape(mergedLeft, mergedTop, mergedRight, mergedBottom);
					if (lastLetter.equals(","))
						mergedShape.setLetter("„");
					else
						mergedShape.setLetter("“");
					mergedShape.setConfidence(shape.getConfidence() * lastShape.getConfidence());
					correctedShapes.add(mergedShape);
				} else if (shape.getLetter().equals(",,")) {
					// TODO: specific to Yiddish, need to generalise
					Shape newShape = shape.getJochreImage().getShape(shape.getLeft(), shape.getTop(), shape.getRight(), shape.getBottom());
					newShape.setLetter("„");
					newShape.setConfidence(shape.getConfidence());
					correctedShapes.add(newShape);
				} else if (shape.getLetter().equals("''")) {
					// TODO: specific to Yiddish, need to generalise
					Shape newShape = shape.getJochreImage().getShape(shape.getLeft(), shape.getTop(), shape.getRight(), shape.getBottom());
					newShape.setLetter("“");
					newShape.setConfidence(shape.getConfidence());
					correctedShapes.add(newShape);
				} else {
					correctedShapes.add(shape);
				}
				lastLetter = shape.getLetter();
			}
		}
		return correctedShapes;
	}

	public boolean isJunk() {
		if (junk == null) {
			if (this.getFrequency() <= 0 || this.getShapes().size() <= 1) {
				double averageConfidence = 0;
				if (this.getShapes().size() > 0) {
					for (Shape shape : this.getShapes()) {
						averageConfidence += shape.getConfidence();
					}
					averageConfidence = averageConfidence / this.getShapes().size();
				}

				if (averageConfidence < jochreSession.getJunkConfidenceThreshold())
					junk = true;
				else
					junk = false;
			} else {
				junk = false;
			}
		}
		return junk;
	}

	/**
	 * Returns the best letter sequence for this group, if any, or null if none.
	 */
	public LetterSequence getBestLetterSequence() {
		return this.bestLetterSequence;
	}

	public void setBestLetterSequence(LetterSequence bestLetterSequence) {
		this.bestLetterSequence = bestLetterSequence;
	}

	/**
	 * Returns the word excluding opening and closing punctuation.
	 */
	public String getWordForIndex() {
		Linguistics linguistics = jochreSession.getLinguistics();
		String word = this.getWord();
		int wordStart = 0;
		for (int i = 0; i < word.length(); i++) {
			wordStart = i;
			char c = word.charAt(i);
			if (linguistics.getPunctuation().contains(c)) {
				continue;
			}
			break;
		}
		int wordEnd = word.length() - 1;
		for (int i = word.length() - 1; i >= 0; i--) {
			wordEnd = i;
			char c = word.charAt(i);
			if (linguistics.getPunctuation().contains(c)) {
				continue;
			}
			break;
		}
		wordEnd += 1;
		if (wordStart > wordEnd)
			wordStart = wordEnd;
		String wordForIndex = word.substring(wordStart, wordEnd);
		return wordForIndex;
	}

	/**
	 * The word frequencies for the best letter sequence.
	 */
	public List<CountedOutcome<String>> getWordFrequencies() {
		if (bestLetterSequence != null)
			return bestLetterSequence.getWordFrequencies();
		return null;
	}

	/**
	 * The confidence in the current word guess, in a scale from 0 to 1.
	 */
	public double getConfidence() {
		if (bestLetterSequence != null)
			return bestLetterSequence.getAdjustedScore();
		return 0;
	}

	@Override
	public int getWidth() {
		return right - left + 1;
	}

	@Override
	public int getHeight() {
		return bottom - top + 1;
	}

	/**
	 * A rectangle defining the space preceding this group, or null if first
	 * group.
	 */
	public Rectangle getPrecedingSpace() {
		Rectangle prevSpace = null;
		if (this.index > 0) {
			GroupOfShapes prevGroup = this.getRow().getGroups().get(this.index - 1);
			int top = this.top < prevGroup.getTop() ? this.top : prevGroup.getTop();
			int bottom = this.bottom > prevGroup.getBottom() ? this.bottom : prevGroup.getBottom();
			if (this.getRow().getParagraph().getImage().isLeftToRight()) {
				prevSpace = new RectangleImpl(prevGroup.getRight() + 1, top, this.getLeft() - 1, bottom);
			} else {
				prevSpace = new RectangleImpl(this.getRight() + 1, top, prevGroup.getLeft() - 1, bottom);
			}
		}
		return prevSpace;
	}

	/**
	 * Returns the subsequences of the best letter sequence.
	 */
	public List<LetterSequence> getSubsequences() {
		LetterSequence bestLetterSequence = this.getBestLetterSequence();
		List<LetterSequence> subsequences = new ArrayList<LetterSequence>();
		if (bestLetterSequence != null)
			subsequences = bestLetterSequence.getSubsequences();
		return subsequences;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

}
