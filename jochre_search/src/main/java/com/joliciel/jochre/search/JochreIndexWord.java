///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Assaf Urieli
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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * A single word found within a {@link JochreIndexDocument} at a given offset, used to retrieve the word's
 * attributes such as its text, its rectangle or its image.
 * @author Assaf Urieli
 *
 */
public interface JochreIndexWord {
	
	/**
	 * The word's start offset.
	 * @return
	 */
	public int getStartOffset();
	
	/**
	 * The document containing this word.
	 * @return
	 */
	public JochreIndexDocument getDocument();
	
	/**
	 * The rectangle surrounding this word within the page.
	 * @return
	 */
	public Rectangle getRectangle();
	
	/**
	 * The second rectangle for this word, when it is a hyphenated word split across two rows.
	 * @return
	 */
	public Rectangle getSecondRectangle();
	
	/**
	 * The word's text.
	 * @return
	 */
	public String getText();
	
	/**
	 * The word's image - if it is a hyphenated word, the image includes both halves.
	 * @return
	 */
	public BufferedImage getImage();
	
	/**
	 * The rectangle of the row containing this word, within the page.
	 * @return
	 */
	public Rectangle getRowRectangle();
	
	/**
	 * The image of the row containing this word.
	 * @return
	 */
	public BufferedImage getRowImage();
	
	/**
	 * This 2nd row's rectangle within the page, when the word is a hyphenated word split across two rows.
	 * @return
	 */
	public Rectangle getSecondRowRectangle();
	
	/**
	 * This 2nd row's image, when the word is a hyphenated word split across two rows.
	 * @return
	 */
	public BufferedImage getSecondRowImage();
	
	/**
	 * Page index for this word.
	 * @return
	 */
	public int getPageIndex();
}
