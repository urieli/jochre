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

import org.apache.lucene.index.IndexWriter;

/**
 * A wrapper for a document in the Lucene sense - a single book could be split into multiple documents,
 * each with a different page range.
 * @author Assaf Urieli
 *
 */
public interface JochreIndexDocument {
	/**
	 * The full string contents of the document.
	 */
	public String getContents();
	
	/**
	 * Save the document to the Lucene index.
	 */
	public void save(IndexWriter indexWriter);
	
	/**
	 * Get the rectangle enclosing a particular row.
	 */
	public Rectangle getRowRectangle(int pageIndex, int rowIndex);
	
	/**
	 * The index of the first page contained in this document.
	 */
	public int getStartPage();
	
	/**
	 * The index of the last page contained in this document.
	 */
	public int getEndPage();
	
	/**
	 * The document's name, used as a prefix for various files in the document's directory.
	 */
	public String getName();
	
	/**
	 * Get the image corresponding to a particular page index.
	 */
	public BufferedImage getImage(int pageIndex);

	/**
	 * Return the content index of the first character on a given row.
	 */
	public int getStartIndex(int pageIndex, int rowIndex);

	/**
	 * Return the content index following the last character on a given row.
	 */
	public int getEndIndex(int pageIndex, int rowIndex);
	
	/**
	 * Get the number of rows on a given page.
	 */
	public int getRowCount(int pageIndex);
	
	/**
	 * Get the word starting at the given offset, or at the maximum offset prior to this one.
	 */
	public JochreIndexWord getWord(int startOffset);
	
	/**
	 * A term lister for the current document.
	 */
	public JochreIndexTermLister getTermLister();
	
	/**
	 * The document's path, relative to the content directory.
	 */
	public String getPath();
	
	/**
	 * The document's section number, which, together with the path,
	 * identifies it uniquely.
	 */
	public int getSectionNumber();
}