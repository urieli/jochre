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
 * A row on a particular OCR page for which the user has given feedback.
 * The row image is stored (instead of a word image), since this enables us to better calculate the baseline and other row-level
 * characteristics.
 * @author Assaf Urieli
 *
 */
public interface FeedbackRow {
	/**
	 * The unique internal id for this row.
	 */
	public int getId();
	
	/**
	 * The document containing this row.
	 */
	public FeedbackDocument getDocument();
	public int getDocumentId();
	
	/**
	 * The page index on which this row is found.
	 */
	public int getPageIndex();
	
	/**
	 * This row's rectangle within the page.
	 */
	public Rectangle getRectangle();
	
	/**
	 * This row's image.
	 */
	public BufferedImage getImage();
}
