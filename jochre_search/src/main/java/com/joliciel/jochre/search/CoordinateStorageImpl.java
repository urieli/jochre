///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
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
package com.joliciel.jochre.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class CoordinateStorageImpl implements CoordinateStorage, Serializable {
	private static final Log LOG = LogFactory.getLog(CoordinateStorageImpl.class);
	private static final long serialVersionUID = 6L;
	private Map<Integer, List<Rectangle>> coordinates = new HashMap<Integer, List<Rectangle>>();
	private SortedSet<Integer> wordOffsets = new TreeSet<Integer>();
	private List<Rectangle> rowCoordinates = new ArrayList<Rectangle>();
	private List<Integer> rowOffsets = new ArrayList<Integer>();
	private List<Rectangle> paragraphCoordinates = new ArrayList<Rectangle>();
	private List<Integer> paragraphOffsets = new ArrayList<Integer>();
	private List<Integer> imageOffsets = new ArrayList<Integer>();
	private Map<Integer, String> imageNames = new HashMap<Integer, String>();
	private List<Integer> imagePageIndexes = new ArrayList<Integer>();
	
	@Override
	public List<Rectangle> getRectangles(int offset) {
		return coordinates.get(offset);
	}

	@Override
	public void setRectangles(int offset, List<Rectangle> rectangles) {
		coordinates.put(offset, rectangles);
		wordOffsets.add(offset);
	}

	public List<Rectangle> getNearestRectangles(int offset) {
		int nearestOffset = -1;
		SortedSet<Integer> tailSet = wordOffsets.tailSet(offset);
		if (tailSet.size()>0) {
			nearestOffset = tailSet.first();
		} else {
			SortedSet<Integer> headSet = wordOffsets.headSet(offset);
			nearestOffset = headSet.last();
		}
		if (nearestOffset>=0) {
			return this.getRectangles(nearestOffset);
		}
		return null;
	}
	
	public Rectangle getRowCoordinates(int offset) {
		int rowIndex = this.getRowIndex(offset);
		Rectangle rectangle = rowCoordinates.get(rowIndex);
		return rectangle;
	}
	

	@Override
	public Rectangle getParagraphCoordinates(int offset) {
		int paragraphIndex = this.getParagraphIndex(offset);
		Rectangle rectangle = paragraphCoordinates.get(paragraphIndex);
		return rectangle;
	}

	@Override
	public int getRowIndex(int offset) {
		int rowIndex = 0;
		for (int i=0; i<rowOffsets.size(); i++) {
			int rowStart = rowOffsets.get(i);
			if (rowStart > offset) {
				break;
			}
			rowIndex = i;
		}
		return rowIndex;
	}


	@Override
	public int getParagraphIndex(int offset) {
		int paragraphIndex = 0;
		for (int i=0; i<paragraphOffsets.size(); i++) {
			int paragraphStart = paragraphOffsets.get(i);
			if (paragraphStart > offset) {
				break;
			}
			paragraphIndex = i;
		}
		return paragraphIndex;
	}
	
	@Override
	public int getRowStartOffset(int rowIndex) {
		return rowOffsets.get(rowIndex);
	}
	
	public void addRow(int startOffset, Rectangle rectangle) {
		if (LOG.isDebugEnabled())
			LOG.debug("Adding row " + startOffset + ": " + rectangle.toString());
		this.rowCoordinates.add(rectangle);
		this.rowOffsets.add(startOffset);
	}

	@Override
	public void addParagraph(int startOffset, Rectangle rectangle) {
		if (LOG.isDebugEnabled())
			LOG.debug("Adding paragraph " + startOffset + ": " + rectangle.toString());
		this.paragraphCoordinates.add(rectangle);
		this.paragraphOffsets.add(startOffset);
	}

	public void addImage(int startOffset, String imageName, int pageIndex) {
		if (LOG.isDebugEnabled())
			LOG.debug("Adding page " + imageOffsets.size() + " at offset "+ startOffset + ": " + imageName);
		imageNames.put(imageOffsets.size(), imageName);
		imageOffsets.add(startOffset);
		imagePageIndexes.add(pageIndex);
	}
	
	public int getImageIndex(int offset) {
		int myPageIndex = 0;
		for (int i=0; i<imageOffsets.size(); i++) {
			int pageStart = imageOffsets.get(i);
			if (pageStart > offset) {
				break;
			}
			myPageIndex = i;
		}
		return myPageIndex;
	}
	
	public String getImageName(int imageIndex) {
		return imageNames.get(imageIndex);
	}

	@Override
	public int getImageCount() {
		return imageOffsets.size();
	}

	@Override
	public int getPageIndex(int offset) {
		int imageIndex = this.getImageIndex(offset);
		int pageIndex = imagePageIndexes.get(imageIndex);
		return pageIndex;
	}

	@Override
	public int getImageStartOffset(int imageIndex) {
		return imageOffsets.get(imageIndex);
	}


	@Override
	public int getParagraphStartOffset(int paragraphIndex) {
		return paragraphOffsets.get(paragraphIndex);
	}

}
