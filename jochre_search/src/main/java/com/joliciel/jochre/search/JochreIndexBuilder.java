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

import java.io.File;

public interface JochreIndexBuilder {
	/**
	 * Update the index by scanning all of the sub-directories of this contentDir for updates.
	 * The sub-directory path is considered to uniquely identify a work.
	 * Sub-directory contents are described in {@link JochreIndexDirectory}.
	 * A work will only be updated if the date of it's text layer is later than the previous index date (stored in the index),
	 * or if forceUpdate=true. If the work is updated, any previous documents with the same path are first deleted.
	 * Multiple Lucene documents can be created from a single work, if {@link #getWordsPerDoc()}&gt;0.
	 * @param contentDir the parent directory to scan
	 * @param forceUpdate if true, all docs will be updated regardless of last update date
	 */
	public void updateIndex(File contentDir, boolean forceUpdate);
	
	/**
	 * Add or update a single directory to the index.
	 * @param documentDir the directory containing the document set
	 * @param startPage the first page to process, or all if -1
	 * @param endPage the last page to process, or all if -1
	 */
	public void updateDocument(File documentDir, int startPage, int endPage);
	
	/**
	 * Delete a single work from the index. The directory path is considered to identify the work.
	 * @param documentDir the directory containing the document set
	 */
	public void deleteDocument(File documentDir);

	/**
	 * The approximate number of words to include in each Lucene document -
	 * although the document will always include entire pages.
	 * We split documents by an arbitrary number of parts, since
	 * we cannot hope to recognise chapter headings.
	 * If &lt;= 0, all pages will go in single document.
	 * Default is 3000.
	 * @return
	 */
	public int getWordsPerDoc();
	public void setWordsPerDoc(int pagesPerDoc);
}