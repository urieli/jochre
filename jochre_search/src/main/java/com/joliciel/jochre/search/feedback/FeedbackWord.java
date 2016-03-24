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
package com.joliciel.jochre.search.feedback;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * A word for which a user has given feedback.
 * @author Assaf Urieli
 *
 */
public interface FeedbackWord {
	/**
	 * The unique internal id for this word.
	 */
	public int getId();
	
	/**
	 * The row on which this word is found.
	 */
	public FeedbackRow getRow();
	public int getRowId();
	
	/**
	 * The word's rectangle within the page containing it.
	 */
	public Rectangle getRectangle();
	
	/**
	 * The row containing the second half of a hyphenated word.
	 */
	public FeedbackRow getSecondRow();
	public int getSecondRowId();
	
	/**
	 * The rectangle containing the 2nd half of a hyphenated word, within the page containing it.
	 */
	public Rectangle getSecondRectangle();
	
	/**
	 * The initial guess for this word.
	 */
	public String getInitialGuess();
	
	/**
	 * This word's image.
	 */
	public BufferedImage getImage();
}
