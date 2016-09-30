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
package com.joliciel.jochre.training;

import java.awt.Rectangle;

/**
 * A single word, associated with one (or two) rectangles, depending on whether
 * it is hyphenated at the end of a row or simple, the containing rows and their
 * images, and the text.
 * 
 * @author Assaf Urieli
 *
 */
public interface TrainingWord {
	/**
	 * The row on which this word is found.
	 */
	public TrainingRow getRow();

	/**
	 * The word's rectangle within the page containing it.
	 */
	public Rectangle getRectangle();

	/**
	 * The row containing the second half of a hyphenated word.
	 */
	public TrainingRow getSecondRow();

	/**
	 * The rectangle containing the 2nd half of a hyphenated word, within the
	 * page containing it.
	 */
	public Rectangle getSecondRectangle();

	/**
	 * The font which the user indicated for this word.
	 */
	String getFont();

	/**
	 * The language which the user indicated for this word.
	 */
	String getLanguage();

	/**
	 * The text.
	 */
	String getText();

	/**
	 * The text for the second half in the case of a hyphenated word.
	 * 
	 * @return
	 */
	String getSecondText();
}
