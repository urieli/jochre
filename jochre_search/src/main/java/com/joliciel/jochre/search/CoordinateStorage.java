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

import java.util.List;

public interface CoordinateStorage {
	/**
	 * Get the rectangles to highlight corresponding to a word
	 * starting at a certain offset from the document start.
	 * Rectangles are located in the image indexed by {@link #getImageIndex(int)}.
	 * @param offset
	 * @return
	 */
	public List<Rectangle> getRectangles(int offset);
	
	/**
	 * Set the rectangles to highlight corresponding to the word starting at a certain offset.
	 * @param offset
	 * @param rectangles
	 */
	public void setRectangles(int offset, List<Rectangle> rectangles);
	public List<Rectangle> getNearestRectangles(int offset);
	
	/**
	 * Add a new image, starting at given textual offset, with a given name,
	 * and located on the page with the given page index.
	 * @param startOffset
	 * @param imageName
	 * @param pageIndex
	 */
	public void addImage(int startOffset, String imageName, int pageIndex);

	/**
	 * Add a paragraph starting at a given offset and defined by a given rectangle.
	 * @param startOffset
	 * @param rectangle
	 */
	public void addParagraph(int startOffset, Rectangle rectangle);
	
	/**
	 * Add a row starting at a given offset and defined by a given rectangle.
	 * @param startOffset
	 * @param rectangle
	 */
	public void addRow(int startOffset, Rectangle rectangle);
			
	/**
	 * Get the index of the image containing the provided offset.
	 * Image indexes are sequential within a given storage.
	 * @param offset
	 * @return
	 */
	public int getImageIndex(int offset);
	
	/**
	 * Get the number of images in this coordinate storage.
	 * @return
	 */
	public int getImageCount();
	
	/**
	 * Get the name of the image corresponding to a given index.
	 * @param imageIndex
	 * @return
	 */
	public String getImageName(int imageIndex);
	
	/**
	 * Get the index of the real page on which the text is found corresponding to the provided offset.
	 * @param offset
	 * @return
	 */
	public int getPageIndex(int offset);
	
	/**
	 * Get the start offset of a given image index.
	 * @param imageIndex
	 * @return
	 */
	public int getImageStartOffset(int imageIndex);
	
	public int getParagraphIndex(int offset);
	public int getParagraphStartOffset(int paragraphIndex);
	public Rectangle getParagraphCoordinates(int offset);
	public int getRowIndex(int offset);
	public int getRowStartOffset(int rowIndex);
	
	/**
	 * Get the coordinates for an entire row containing the provided offset.
	 * @param offset
	 * @return
	 */
	public Rectangle getRowCoordinates(int offset);

}
