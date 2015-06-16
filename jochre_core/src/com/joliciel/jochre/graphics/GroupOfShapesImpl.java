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
import java.util.ArrayList;

import com.joliciel.jochre.EntityImpl;
import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.talismane.utils.CountedOutcome;

class GroupOfShapesImpl extends EntityImpl implements
		GroupOfShapesInternal {
	
	private GraphicsServiceInternal graphicsService;

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
	
	public List<Shape> getShapes() {
		if (shapes==null) {
			if (this.isNew())
				shapes = new ArrayList<Shape>();
			else {
				shapes = this.graphicsService.findShapes(this);
				for (Shape shape : shapes) {
					((ShapeInternal) shape).setGroup(this);
				}
			}
		}
		return shapes;
	}
	


	@Override
	public void addShapes(List<Shape> shapesToAdd) {
		if (this.shapes==null) {
			this.shapes = new ArrayList<Shape>();
			for (Shape shape : shapesToAdd) {
				if (shape.getGroupId()==this.getId()) {
					this.shapes.add(shape);
					((ShapeInternal) shape).setGroup(this);
				}
			}
		}
	}

	@Override
	public void addShape(Shape shape) {
		this.getShapes().add(shape);
		shape.setGroup(this);
	}



	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		if (this.index!=index) {
			this.index = index;
			dirty = true;
		}
	}



	public int getRowId() {
		return rowId;
	}



	public void setRowId(int rowId) {
		if (this.rowId!=rowId) {
			this.rowId = rowId;
			dirty = true;
		}
	}



	public RowOfShapes getRow() {
		if (this.row==null && this.rowId!=0) {
			this.row = this.graphicsService.loadRowOfShapes(this.rowId);
		}
		return row;
	}



	public void setRow(RowOfShapes row) {
		this.row = row;
		if (row!=null)
			this.setRowId(row.getId());
		else
			this.setRowId(0);
	}



	@Override
	public void saveInternal() {
		if (this.row!=null && this.rowId==0)
			this.setRowId(this.row.getId());

		if (this.dirty)
			this.graphicsService.saveGroupOfShapes(this);
		
		if (this.shapes!=null) {
			int index = 0;
			for (Shape shape : this.shapes) {
				shape.setGroup(this);
				shape.setIndex(index++);
				shape.save();
			}
		}	
	}

	public GraphicsServiceInternal getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsServiceInternal graphicsService) {
		this.graphicsService = graphicsService;
	}

	@Override
	public int getLeft() {
		this.findCoordinates();
		return this.left;
	}

	@Override
	public int getTop() {
		this.findCoordinates();
		return this.top;
	}

	@Override
	public int getRight() {
		this.findCoordinates();
		return this.right;
	}

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

	@Override
	public String getWord() {
		StringBuilder sb = new StringBuilder();
		for (Shape shape : this.getCorrectedShapes()) {
			if (shape.getLetter()!=null)
				sb.append(shape.getLetter());
		}
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		if (this.isNew())
			return super.hashCode();
		else
			return ((Integer)this.getId()).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this.isNew()) {
			return super.equals(obj);
		} else {
			GroupOfShapes otherGroup = (GroupOfShapes) obj;
			return (this.getId()==otherGroup.getId());
		}
	}
	
	@Override
	public void recalculate() {
		this.coordinatesFound = false;
		this.meanLine = null;
		this.baseLine = null;
	}

	@Override
	public String toString() {
		return "Group " + this.getIndex() + ", left(" + this.getLeft() + ")"
		+ ", top(" + this.getTop() + ")"
		+ ", right(" + this.getRight() + ")"
		+ ", bot(" + this.getBottom() + ")"
		+ " [id=" + this.getId() + "]";
	}
	
	@Override
	public int[] getMeanLine() {
		this.getGuideLines();
		return this.meanLine;
	}

	@Override
	public int[] getBaseLine() {
		this.getGuideLines();
		return this.baseLine;
	}
	
	private void getGuideLines() {
		if (this.meanLine==null) {
			Shape leftMostShape = null;
			Shape rightMostShape = null;
			for (Shape shape : this.getShapes()) {
				if (leftMostShape==null) {
					leftMostShape = shape;
					rightMostShape = shape;
				} else {
					if (shape.getLeft()<leftMostShape.getLeft())
						leftMostShape = shape;
					if (shape.getRight()>rightMostShape.getRight())
						rightMostShape = shape;
				}				
			}
			int[] meanLine = { leftMostShape.getLeft(), leftMostShape.getTop() + leftMostShape.getMeanLine(),
					rightMostShape.getRight(), rightMostShape.getTop() + rightMostShape.getMeanLine()};
			int[] baseLine = {leftMostShape.getLeft(), leftMostShape.getTop() + leftMostShape.getBaseLine(),
					rightMostShape.getRight(), rightMostShape.getTop() + rightMostShape.getBaseLine()};
			
			this.meanLine = meanLine;
			this.baseLine = baseLine;
		}
	}



	@Override
	public int getXHeight() {
		if (xHeight<0) {
			Shape firstShape = this.getShapes().get(0);
			xHeight = firstShape.getBaseLine() - firstShape.getMeanLine();
		}
		return xHeight;
	}

	@Override
	public boolean isHardHyphen() {
		return hardHyphen;
	}

	@Override
	public void setHardHyphen(boolean hardHyphen) {
		if (this.hardHyphen!=hardHyphen) {
			this.hardHyphen = hardHyphen;
			dirty = true;
		}
	}

	public boolean isBrokenWord() {
		return brokenWord;
	}

	public void setBrokenWord(boolean brokenWord) {
		if (this.brokenWord!=brokenWord) {
			this.brokenWord = brokenWord;
			dirty = true;
		}
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public boolean isSegmentationProblem() {
		return segmentationProblem;
	}

	public void setSegmentationProblem(boolean segmentationProblem) {
		if (this.segmentationProblem!=segmentationProblem) {
			this.segmentationProblem = segmentationProblem;
			dirty = true;
		}		
	}
	
	public boolean isSkip() {
		return skip;
	}

	public void setSkip(boolean skip) {
		if (this.skip!=skip) {
			this.skip = skip;
			dirty = true;
		}		
	}	

	public int getFrequency() {
		if (this.bestLetterSequence!=null)
			return this.bestLetterSequence.getFrequency();
		return -1;
	}

	@Override
	public boolean isSplit() {
		if (this.bestLetterSequence!=null)
			return this.bestLetterSequence.isSplit();
		return false;
	}
	
	public List<Shape> getCorrectedShapes() {
		if (this.correctedShapes==null) {
			correctedShapes = new ArrayList<Shape>(shapes.size());
			List<Shape> splitShapes = null;
			boolean haveSplitShape = false;
			int mergedTop = 0;
			int mergedBottom = 0;
			int mergedLeft = 0;
			int mergedRight = 0;
			String lastLetter = "";
			for (Shape shape : shapes) {
				if (shape.getLetter().length()==0) {
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
							if (currentSequence.length()>0&&currentSequence.charAt(0)=='|') {
								String letter1 = currentSequence.toString().substring(1);
								String letter2 = letter.substring(0, letter.length()-1);
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
						if (shape.getTop()<mergedTop)
							mergedTop = shape.getTop();
						if (shape.getLeft()<mergedLeft)
							mergedLeft = shape.getLeft();
						if (shape.getBottom()>mergedBottom)
							mergedBottom = shape.getBottom();
						if (shape.getRight()>mergedRight)
							mergedRight = shape.getRight();
					}
				} else if ((shape.getLetter().equals(",") && lastLetter.equals(","))
						|| (shape.getLetter().equals("'") && lastLetter.equals("'"))){
					//TODO: specific to Yiddish, need to generalise
					mergedTop = shape.getTop();
					mergedBottom = shape.getBottom();
					mergedLeft = shape.getLeft();
					mergedRight = shape.getRight();
					Shape lastShape = correctedShapes.remove(correctedShapes.size()-1);
					if (lastShape.getTop()<mergedTop)
						mergedTop = lastShape.getTop();
					if (lastShape.getLeft()<mergedLeft)
						mergedLeft = lastShape.getLeft();
					if (lastShape.getBottom()>mergedBottom)
						mergedBottom = lastShape.getBottom();
					if (lastShape.getRight()>mergedRight)
						mergedRight = lastShape.getRight();
					Shape mergedShape = shape.getJochreImage().getShape(mergedLeft, mergedTop, mergedRight, mergedBottom);
					if (lastLetter.equals(","))
						mergedShape.setLetter("„");
					else
						mergedShape.setLetter("“");
					mergedShape.setConfidence(shape.getConfidence() * lastShape.getConfidence());
					correctedShapes.add(mergedShape);
				} else if (shape.getLetter().equals(",,")) {
					//TODO: specific to Yiddish, need to generalise
					Shape newShape = shape.getJochreImage().getShape(shape.getLeft(), shape.getTop(), shape.getRight(), shape.getBottom());
					newShape.setLetter("„");
					newShape.setConfidence(shape.getConfidence());
					correctedShapes.add(newShape);
				} else if (shape.getLetter().equals("''")) {
					//TODO: specific to Yiddish, need to generalise
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
		if (junk==null) {
			if (this.getFrequency()<=0 || this.getShapes().size()<=1) {
				double averageConfidence = 0;
				if (this.getShapes().size()>0) {
					for (Shape shape : this.getShapes()) {
						averageConfidence += shape.getConfidence();
					}
					averageConfidence = averageConfidence / this.getShapes().size();
				}
				JochreSession jochreSession = JochreSession.getInstance();
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



	@Override
	public LetterSequence getBestLetterSequence() {
		return this.bestLetterSequence;
	}
	public void setBestLetterSequence(LetterSequence bestLetterSequence) {
		this.bestLetterSequence = bestLetterSequence;
	}

	@Override
	public String getWordForIndex() {
		JochreSession jochreSession = JochreSession.getInstance();
		Linguistics linguistics = Linguistics.getInstance(jochreSession.getLocale());
		String word = this.getWord();
		int wordStart = 0;
		for (int i=0; i<word.length(); i++) {
			wordStart = i;
			char c = word.charAt(i);
			if (linguistics.getPunctuation().contains(c)) {
				continue;
			}
			break;
		}
		int wordEnd = word.length()-1;
		for (int i=word.length()-1; i>=0; i--) {
			wordEnd = i;
			char c = word.charAt(i);
			if (linguistics.getPunctuation().contains(c)) {
				continue;
			}
			break;
		}
		wordEnd+=1;
		if (wordStart>wordEnd)
			wordStart=wordEnd;
		String wordForIndex = word.substring(wordStart, wordEnd);
		return wordForIndex;
	}

	public List<CountedOutcome<String>> getWordFrequencies() {
		if (bestLetterSequence!=null)
			return bestLetterSequence.getWordFrequencies();
		return null;
	}

	@Override
	public double getConfidence() {
		if (bestLetterSequence!=null)
			return bestLetterSequence.getAdjustedScore();
		return 0;
	}
	
	@Override
	public int getWidth() {
		return right-left+1;
	}
	
	@Override
	public int getHeight() {
		return bottom-top+1;
	}

	@Override
	public Rectangle getPrecedingSpace() {
		Rectangle prevSpace = null;
		if (this.index>0) {
			GroupOfShapes prevGroup = this.getRow().getGroups().get(this.index-1);
			int top = this.top < prevGroup.getTop() ? this.top : prevGroup.getTop();
			int bottom = this.bottom > prevGroup.getBottom() ? this.bottom : prevGroup.getBottom();
			if (this.getRow().getParagraph().getImage().isLeftToRight()) {
				prevSpace = new RectangleImpl(prevGroup.getRight()+1, top, this.getLeft()-1, bottom);
			} else {
				prevSpace = new RectangleImpl(this.getRight()+1, top, prevGroup.getLeft()-1, bottom);
			}
		}
		return prevSpace;
	}



	@Override
	public List<LetterSequence> getSubsequences() {
		LetterSequence bestLetterSequence = this.getBestLetterSequence();
		List<LetterSequence> subsequences = new ArrayList<LetterSequence>();
		if (bestLetterSequence!=null)
			subsequences = bestLetterSequence.getSubsequences();
		return subsequences;
	}
}
