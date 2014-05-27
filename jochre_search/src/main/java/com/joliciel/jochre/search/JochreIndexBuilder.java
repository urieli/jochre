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
	 * @param contentDir
	 */
	public void updateIndex(File contentDir);
	
	/**
	 * Add a single document directory to the index.
	 * @param documentDir
	 */
	public void addDocumentDir(File documentDir);

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