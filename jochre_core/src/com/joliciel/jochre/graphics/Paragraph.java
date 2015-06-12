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

import com.joliciel.jochre.Entity;

public interface Paragraph extends Entity, Rectangle {
	/**
	 * The index of this paragraph, from 0 (top-most in right-to-left or left-to-right languages) to n.
	 * @return
	 */
	public int getIndex();
	public void setIndex(int index);
	public int getImageId();
	
	/**
	 * The JochreImage containing this paragraph.
	 * @return
	 */
	public JochreImage getImage();
	
	public List<RowOfShapes> getRows();
	public void addRow(RowOfShapes row);
	public RowOfShapes newRow();
	
	
	/**
	 * The leftmost x coordinate of this paragraph (based on the rows it contains).
	 */
	public int getLeft();
	
	/**
	 * The leftmost y coordinate of this paragraph (based on the rows it contains).
	 */
	public int getTop();

	/**
	 * The rightmost x coordinate of this paragraph (based on the rows it contains).
	 */
	public int getRight();
	
	/**
	 * The bottom-most y coordinate of this paragraph (based on the rows it contains).
	 */
	public int getBottom();
	
	/**
	 * Does this paragraph contain a vast majority of "junk" analyses
	 * (low confidence letters, inexistent words, etc. not to be exported)
	 * @return
	 */
	public boolean isJunk();
}
