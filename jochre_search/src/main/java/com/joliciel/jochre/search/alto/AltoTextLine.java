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
package com.joliciel.jochre.search.alto;

import java.awt.Rectangle;
import java.util.List;

/**
 * A text line (row) in an Alto file.
 * Note that row indexing starts at 0 at the page level, not the textblock level,
 * and continues until the end of the page.
 * This vastly simplifies finding the previous/next row on the page when displaying.
 * @author Assaf Urieli
 *
 */
public interface AltoTextLine {
	public List<AltoString> getStrings();
	public Rectangle getRectangle();
	public AltoTextBlock getTextBlock();
	public int wordCount();
	public int getIndex();
	
	/**
	 * Recalculate indexes after merging or other manipulation of contained strings.
	 */
	public void recalculate();
}
