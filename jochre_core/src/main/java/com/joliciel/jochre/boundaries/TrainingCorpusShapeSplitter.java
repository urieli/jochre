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
package com.joliciel.jochre.boundaries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeLeftToRightComparator;
import com.joliciel.jochre.graphics.ShapeRightToLeftComparator;
import com.joliciel.jochre.utils.JochreException;

/**
 * Splits a shape from the training corpus based on its annotations.
 * 
 * @author Assaf Urieli
 *
 */
class TrainingCorpusShapeSplitter implements ShapeSplitter {
	private BoundaryServiceInternal boundaryServiceInternal;

	private final JochreSession jochreSession;

	public TrainingCorpusShapeSplitter(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
	}

	@Override
	public List<ShapeSequence> split(Shape shape) {
		List<ShapeSequence> shapeSequences = new ArrayList<ShapeSequence>();
		ShapeSequence shapeSequence = boundaryServiceInternal.getEmptyShapeSequence();
		shapeSequences.add(shapeSequence);

		Set<String> nonSplittableLetters = jochreSession.getLinguistics().getDualCharacterLetters();
		String testLetter = shape.getLetter().replace("|", "");
		if (testLetter.length() == 1 || nonSplittableLetters.contains(testLetter)) {
			shapeSequence.addShape(shape);
		} else {
			int lastLeft = 0;
			Comparator<Shape> shapeComparator = null;
			if (jochreSession.getLinguistics().isLeftToRight())
				shapeComparator = new ShapeLeftToRightComparator();
			else
				shapeComparator = new ShapeRightToLeftComparator();
			TreeSet<Shape> splitShapes = new TreeSet<Shape>(shapeComparator);
			for (Split split : shape.getSplits()) {
				Shape newShape = shape.getJochreImage().getShape(shape.getLeft() + lastLeft, shape.getTop(), shape.getLeft() + split.getPosition(), shape.getBottom());
				lastLeft = split.getPosition() + 1;
				splitShapes.add(newShape);
			}
			Shape lastShape = shape.getJochreImage().getShape(shape.getLeft() + lastLeft, shape.getTop(), shape.getRight(), shape.getBottom());
			splitShapes.add(lastShape);

			List<String> splitLetters = new ArrayList<String>();
			char lastChar = 0;
			boolean haveSplitLetter = false;
			for (int i = 0; i < shape.getLetter().length(); i++) {
				char c = shape.getLetter().charAt(i);
				if (c == '|')
					haveSplitLetter = true;
				if (lastChar != 0) {
					String doubleChar = "" + lastChar + c;
					if (nonSplittableLetters.contains(doubleChar)) {
						splitLetters.add(doubleChar);
						lastChar = 0;
					} else {
						splitLetters.add("" + lastChar);
						lastChar = c;
					}
				} else {
					lastChar = c;
				}
			}
			if (lastChar != 0)
				splitLetters.add("" + lastChar);

			if (splitLetters.size() == 0)
				splitLetters.add("");

			// Need to take into account possibility of split letter at start or end
			// of shape
			// there's an inherent ambiguity to the current encoding - a|b can either
			// mean end a split a or start a split b
			if (haveSplitLetter) {
				int i = 0;
				List<String> newSplitLetters = new ArrayList<String>();
				boolean inSplit = false;
				for (String letter : splitLetters) {
					if (letter.equals("|")) {
						if (i == 1 && i == splitLetters.size() - 2) {
							// smack in the middle - ambiguous split mark
							Shape previousShape = null;
							Shape nextShape = null;
							String previousLetter = splitLetters.get(0);
							String nextLetter = splitLetters.get(2);
							if (shape.getIndex() > 0) {
								previousShape = shape.getGroup().getShapes().get(shape.getIndex() - 1);
							}
							if (shape.getIndex() < shape.getGroup().getShapes().size() - 1) {
								nextShape = shape.getGroup().getShapes().get(shape.getIndex() + 1);
							}
							boolean backwardsSplit = true;
							if (previousShape != null && previousShape.getLetter().equals("|" + previousLetter)) {
								backwardsSplit = true;
							} else if (nextShape != null && nextShape.getLetter().equals(nextLetter + "|")) {
								backwardsSplit = false;
							} else if (previousShape != null && previousShape.getLetter().length() == 0) {
								backwardsSplit = true;
							} else if (nextShape != null && nextShape.getLetter().length() == 0) {
								backwardsSplit = false;
							} else {
								throw new JochreException("Impossible split for shape " + shape.getId() + ": " + previousLetter + "|" + nextLetter);
							}
							if (backwardsSplit) {
								// start split
								String letterWithSplit = newSplitLetters.get(0) + "|";
								newSplitLetters.remove(0);
								newSplitLetters.add(letterWithSplit);
							} else {
								inSplit = true;
							}
						} else if (i == 1) {
							// start split
							String letterWithSplit = newSplitLetters.get(0) + "|";
							newSplitLetters.remove(0);
							newSplitLetters.add(letterWithSplit);
						} else if (i == splitLetters.size() - 2) {
							// end split
							inSplit = true;
						} else {
							throw new JochreException("Impossible split location for shape " + shape.getId() + ": " + shape.getLetter());
						}
					} else if (inSplit) {
						newSplitLetters.add("|" + letter);
						inSplit = false;
					} else {
						newSplitLetters.add(letter);
					}
					i++;
				}

				splitLetters = newSplitLetters;
			}

			if (splitLetters.size() != splitShapes.size()) {
				throw new JochreException("Cannot have more shapes than letters in shape " + shape.getId() + ": " + shape.getLetter() + ", " + splitLetters);
			}
			int i = 0;
			for (Shape splitShape : splitShapes) {
				shapeSequence.addShape(splitShape, shape);
				splitShape.setLetter(splitLetters.get(i++));
			}
		}
		return shapeSequences;
	}

	public BoundaryServiceInternal getBoundaryServiceInternal() {
		return boundaryServiceInternal;
	}

	public void setBoundaryServiceInternal(BoundaryServiceInternal boundaryServiceInternal) {
		this.boundaryServiceInternal = boundaryServiceInternal;
	}
}
