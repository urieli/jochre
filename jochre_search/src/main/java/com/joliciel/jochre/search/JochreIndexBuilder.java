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
	 * Each sub-directory name should uniquely identify a document set contained inside it.
	 * The sub-directory can contain a file called "instructions.txt".
	 * This file can contain a single word: "delete" or "skip", in which case the document set
	 * will be deleted from the index or skipped.
	 * Otherwise, the index will be updated if the zip file in the document set has a later modify
	 * date than the one located in the file "indexDate.txt".
	 * @param contentDir the parent directory to scan
	 * @param forceUpdate if true, all docs will be updated regardless of last update date
	 */
	public void updateIndex(File contentDir, boolean forceUpdate);
	
	/**
	 * Add or update a single document set to the index. The directory name should uniquely identify the document set.
	 * @param documentDir the directory containing the document set
	 */
	public void updateDocument(File documentDir);
	
	/**
	 * Delete a single document set from the index. The directory name should uniquely identify the document set.
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