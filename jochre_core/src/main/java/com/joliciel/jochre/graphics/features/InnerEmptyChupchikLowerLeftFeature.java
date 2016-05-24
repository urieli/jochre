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
package com.joliciel.jochre.graphics.features;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;
import com.joliciel.jochre.graphics.WritableImageGrid;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

/**
 * If we view the empty space at the centre of the shape, does it's lower-left extremety
 * narrow down to a thin chupchik (whether or not it's closed or open).
 * Useful for distinguishing Yiddish Mem from Tes, Shlos mem and Samekh.
 * @author Assaf Urieli
 *
 */
public class InnerEmptyChupchikLowerLeftFeature extends AbstractShapeFeature<Boolean> implements BooleanFeature<ShapeWrapper> {
	private static final Logger LOG = LoggerFactory.getLogger(InnerEmptyChupchikLowerLeftFeature.class);
	private GraphicsService graphicsService;
	
	public InnerEmptyChupchikLowerLeftFeature() {
	}
	
	@Override
	public FeatureResult<Boolean> checkInternal(ShapeWrapper shapeWrapper, RuntimeEnvironment env) {
		Shape shape = shapeWrapper.getShape();
		BitSet bitset = shape.getBlackAndWhiteBitSet(shape.getJochreImage().getBlackThreshold());
		boolean[][] grid = new boolean[shape.getWidth()][shape.getHeight()];
		for (int i = 0; i < shape.getWidth(); i++) {
			for (int j = 0; j<shape.getHeight(); j++) {
				if (bitset.get(j*shape.getWidth()+i))
					grid[i][j] = true;
			}
		}
		int startX = shape.getWidth()/2;
		int startY = shape.getHeight()/2;
		while (startY<shape.getHeight()&&grid[startX][startY]) {
			startY+=1;
		}

		WritableImageGrid mirror = this.graphicsService.getEmptyMirror(shape);
		
		boolean foundChupchikOrOpening = false;
		if (startY<shape.getHeight()) {
			Stack<HorizontalLineSegment> whiteLineStack = new Stack<HorizontalLineSegment>();
			Set<HorizontalLineSegment> whiteLineSet = new TreeSet<HorizontalLineSegment>();
			HorizontalLineSegment startLine = new HorizontalLineSegment(startX, startY);
			whiteLineStack.push(startLine);
			while (!whiteLineStack.empty()) {
				HorizontalLineSegment line = whiteLineStack.pop();
				if (line.y==shape.getHeight()-1) {
					// found opening to the outside world
					if (LOG.isTraceEnabled())
						LOG.trace("Reached edge: found opening");
					foundChupchikOrOpening=true;
					break;
				}
				if (mirror.getPixel(line.xLeft, line.y)==1)
					continue;
				
				// extend this line to the right and left
				for (int i = line.xLeft; i>=0;i--) {
					if (!grid[i][line.y])
						line.xLeft = i;
					else
						break;
				}
				for (int i = line.xRight; i<= startX; i++) {
					if (!grid[i][line.y])
						line.xRight = i;
					else
						break;
				}
				if (LOG.isTraceEnabled())
					LOG.trace(line.toString());
				whiteLineSet.add(line);
				
				for (int i = line.xLeft; i<=line.xRight; i++) {
					mirror.setPixel(i, line.y, 1);
				}
				
				// find lines below and to the left
				if (line.y<shape.getHeight()-1) {
					boolean inLine = false;
					int row = line.y+1;
					int xLeft = 0;
					for (int i = line.xLeft; i<=line.xRight; i++) {
						if (!inLine&& !grid[i][row]) {
							inLine=true;
							xLeft = i;
						} else if (inLine&&grid[i][row]) {
							HorizontalLineSegment newLine = new HorizontalLineSegment(xLeft, row);
							newLine.xRight = i-1;
							whiteLineStack.push(newLine);
							inLine = false;
						}
					}
					if (inLine) {
						HorizontalLineSegment newLine = new HorizontalLineSegment(xLeft, row);
						newLine.xRight = line.xRight;
						whiteLineStack.push(newLine);
					}
				}
			}
			
			if (!foundChupchikOrOpening){
//				if (LOG.isDebugEnabled()) {
//					LOG.trace("List of lines");
//					for (HorizontalLineSegment line : whiteLineSet) {
//						LOG.trace(line.toString());
//					}
//				}
				Iterator<HorizontalLineSegment> iLines = whiteLineSet.iterator();
				HorizontalLineSegment bottomLeftLine = iLines.next();
				double threshold = shape.getWidth()/8;
				if (LOG.isTraceEnabled())
					LOG.trace("Length threshold: " + threshold);
				HorizontalLineSegment nextLine = null;
				List<HorizontalLineSegment> firstFewLines = new ArrayList<HorizontalLineSegment>();
				firstFewLines.add(bottomLeftLine);
				HorizontalLineSegment currentLine = bottomLeftLine;
				while (iLines.hasNext() && firstFewLines.size()<3) {
					 nextLine = iLines.next();
					 if (nextLine.y!=currentLine.y) {
						 firstFewLines.add(nextLine);
						 currentLine = nextLine;
					 }
				}
				boolean mightHaveChupchik = true;
				HorizontalLineSegment prevLine = null;
				for (HorizontalLineSegment line : firstFewLines) {
					if (LOG.isTraceEnabled())
						LOG.trace("Next line left, " + bottomLeftLine.xLeft + ", length: " + bottomLeftLine.length() + ", threshold: " + threshold);
					if (line.length()>threshold) {
						mightHaveChupchik =false;
						break;
					}
					if (prevLine!=null) {
						if (line.xLeft+2<prevLine.xLeft) {
							mightHaveChupchik =false;
							break;						
						}
						if (line.length()+1<prevLine.length()) {
							mightHaveChupchik =false;
							break;						
						}
					}
					prevLine = line;
					threshold = threshold * 1.2;
				}
				if (mightHaveChupchik)
					foundChupchikOrOpening = true;
			}
		}

		FeatureResult<Boolean> outcome = this.generateResult(foundChupchikOrOpening);
		return outcome;
	}

	private class HorizontalLineSegment implements Comparable<HorizontalLineSegment> {
		private HorizontalLineSegment(int x, int y) {
			this.y = y;
			this.xLeft = x;
			this.xRight = x;
		}
		public int y;
		public int xLeft;
		public int xRight;
		public int length() { return xRight - xLeft + 1; }

		
		@Override
		public int compareTo(HorizontalLineSegment line2) {
			// arrange lines from bottom-left upwards
			if (this.y<line2.y)
				return 1;
			else if (this.y>line2.y)
				return -1;
			else if (this.xLeft<line2.xLeft)
				return -1;
			else if (this.xLeft>line2.xLeft)
				return 1;
			else
				return 0;
		}

		@Override
		public int hashCode() {
			String hash = y + "|" + xLeft;
			return hash.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (obj instanceof HorizontalLineSegment) {
				HorizontalLineSegment line2 = (HorizontalLineSegment) obj;
				return (line2.xLeft==this.xLeft && line2.y==this.y);
			}
			return false;
		}


		@Override
		public String toString() {
			return "line, y=" + this.y + ", left=" + this.xLeft + ",right=" + this.xRight;
		}
		
	}

	public GraphicsService getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}
	
	
}
