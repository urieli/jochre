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
import java.util.TreeSet;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds white areas of a certain minimum vertical and horizontal dimension
 * given a set of black areas already known in the area.
 * @author Assaf Urieli
 *
 */
class WhiteAreaFinder {
	private static final Logger LOG = LoggerFactory.getLogger(WhiteAreaFinder.class);
	public List<Rectangle> getWhiteAreas(ImageGrid imageGrid, int blackThreshold, int left, int top, int right, int bottom,
			double minWhiteAreaWidth, double minWhiteAreaHeight) {
		return this.getWhiteAreas(imageGrid, blackThreshold, null, left, top, right, bottom, minWhiteAreaWidth, minWhiteAreaHeight);
	}

	public List<Rectangle> getWhiteAreas(List<? extends Rectangle> shapes, int left, int top, int right, int bottom,
			double minWhiteAreaWidth, double minWhiteAreaHeight) {
		return this.getWhiteAreas(null, 0, shapes, left, top, right, bottom, minWhiteAreaWidth, minWhiteAreaHeight);
	}
	
	List<Rectangle> getWhiteAreas(ImageGrid imageGrid, int blackThreshold, List<? extends Rectangle> blackAreas, int left, int top, int right, int bottom,
			double minWhiteAreaWidth, double minWhiteAreaHeight) {	
		// figure out white areas based on shapes, not isPixelBlack
		List<Rectangle> whiteAreas = new ArrayList<Rectangle>();
		List<RectangleImpl> openWhiteAreas = new ArrayList<RectangleImpl>();
		TreeSet<Rectangle> blackAreasToConsider = new TreeSet<Rectangle>(new RectangleTopToBottomComparator());
		if (blackAreas!=null)
			blackAreasToConsider.addAll(blackAreas);
		
		// stop "false" column separators from appearing at the top row, below a series of titles
		// these can be recognise because they radically narrow what is already a long strip
		// Now - doing this by getting horizontal boxes first
//		double minRatioForExtension = 0.5;
		
		for (int y = top; y<=bottom; y++) {
			// get all white horizontal lines on current row
			List<WhiteLine> whiteLines = new ArrayList<WhiteLine>();
			boolean[] pixels = new boolean[right+1];
			List<Rectangle> blackAreasToRemove = new ArrayList<Rectangle>();
			for (Rectangle blackArea : blackAreasToConsider) {
				if (blackArea.getBottom()<y) 
					blackAreasToRemove.add(blackArea);
				if (blackArea.getTop()>y)
					break;
				if (blackArea.getBottom()>=y) {
					int maxLeft = blackArea.getLeft() > left ? blackArea.getLeft() : left;
					int minRight = blackArea.getRight() < right ? blackArea.getRight() : right;
					for (int i =maxLeft; i<=minRight; i++) {
						pixels[i] = true;
					}
				}
			}
			blackAreasToConsider.removeAll(blackAreasToRemove);
			boolean inWhite = false;
			int startWhite = 0;
			for (int x = left; x<=right; x++) {
				boolean isBlack = pixels[x];
				// alternate method to a list of rectangles: get black pixel directly
				if (imageGrid!=null) {
					isBlack = imageGrid.isPixelBlack(x, y, blackThreshold);
				}
				if (!inWhite && !isBlack) {
					startWhite = x;
					inWhite = true;
				} else if (inWhite && isBlack) {
					WhiteLine whiteLine = new WhiteLine(startWhite, x-1);
					whiteLines.add(whiteLine);
					inWhite = false;
				}
			}
			if (inWhite) {
				WhiteLine whiteLine = new WhiteLine(startWhite, right);
				whiteLines.add(whiteLine);
			}
			
			// check if the white horizontal lines extend existing rectangles
			List<RectangleImpl> currentWhiteAreas = new ArrayList<RectangleImpl>();
			for (WhiteLine whiteLine : whiteLines) {
				for (RectangleImpl whiteArea : openWhiteAreas) {
					int maxLeft = whiteLine.start >= whiteArea.getLeft() ? whiteLine.start : whiteArea.getLeft();
					int minRight = whiteLine.end <= whiteArea.getRight() ? whiteLine.end : whiteArea.getRight();
					if (minRight - maxLeft >= minWhiteAreaWidth) {
						// there is an overlap that's wide enough, add it
						// but first check the ratio is above the minimum
//						if ((double)(minRight - maxLeft) / (double) (whiteArea.getRight() - whiteArea.getLeft()) > minRatioForExtension) {
							RectangleImpl newWhiteArea = new RectangleImpl(maxLeft, whiteArea.getTop(), minRight, y);
							currentWhiteAreas.add(newWhiteArea);
//						}
					}
				}
				RectangleImpl whiteLineArea = new RectangleImpl(whiteLine.start, y, whiteLine.end, y);
				currentWhiteAreas.add(whiteLineArea);
			}
			currentWhiteAreas.addAll(openWhiteAreas);
			
			// get rid of white areas with full overlap
			List<RectangleImpl> whiteAreasToDelete = new ArrayList<RectangleImpl>();
			for (int i=0; i<currentWhiteAreas.size()-1; i++) {
				RectangleImpl whiteArea1 = currentWhiteAreas.get(i);
				for (int j=i+1; j<currentWhiteAreas.size(); j++) {
					RectangleImpl whiteArea2 = currentWhiteAreas.get(j);
					if (whiteArea1.getLeft()>=whiteArea2.getLeft()&&whiteArea1.getTop()>=whiteArea2.getTop()
							&& whiteArea1.getRight()<=whiteArea2.getRight()&&whiteArea1.getBottom()<=whiteArea2.getBottom()) {
						whiteAreasToDelete.add(whiteArea1);
					} else if (whiteArea2.getLeft()>=whiteArea1.getLeft()&&whiteArea2.getTop()>=whiteArea1.getTop()
							&& whiteArea2.getRight()<=whiteArea1.getRight()&&whiteArea2.getBottom()<=whiteArea1.getBottom()) {
						whiteAreasToDelete.add(whiteArea2);
					}
				}
			}
			currentWhiteAreas.removeAll(whiteAreasToDelete);
			
			openWhiteAreas = new ArrayList<RectangleImpl>();
			for (RectangleImpl whiteArea : currentWhiteAreas) {
				if (whiteArea.getBottom()<y && (whiteArea.getBottom()-whiteArea.getTop()>=minWhiteAreaHeight)) {
					LOG.debug("Adding " + whiteArea.toString());
					whiteAreas.add(whiteArea);
				} else if (whiteArea.getBottom()==y) {
					openWhiteAreas.add(whiteArea);
				}
			}
		}
		for (RectangleImpl whiteArea : openWhiteAreas) {
			if (whiteArea.getBottom()-whiteArea.getTop()>=minWhiteAreaHeight) {
				LOG.debug("Adding " + whiteArea.toString());
				whiteAreas.add(whiteArea);
			}
		}
		return whiteAreas;
	}

	private static class WhiteLine {
		public WhiteLine(int start, int end) {
			this.start = start;
			this.end = end;
		}
		public int start;
		public int end;
	}
}
