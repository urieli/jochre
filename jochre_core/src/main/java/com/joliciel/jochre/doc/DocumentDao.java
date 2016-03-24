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
package com.joliciel.jochre.doc;

import java.util.List;

import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;

interface DocumentDao {
	
	public DocumentServiceInternal getDocumentServiceInternal();

	public void setDocumentServiceInternal(
			DocumentServiceInternal documentServiceInternal);
	
	public void saveJochrePage(JochrePage jochrePage);
	public List<JochrePage> findJochrePages(JochreDocument jochreDocument);
	public JochrePage loadJochrePage(int jochrePageId);
	public JochreDocument loadJochreDocument(int jochreDocumentId);
	public JochreDocument loadJochreDocument(String name);
	
	public void saveJochreDocument(JochreDocument jochreDocument);
	
	/**
	 * Find all existing documents in the database.
	 */
	public List<JochreDocument> findDocuments();
	
	public List<? extends Author> findAuthors(JochreDocument doc);
	public Author loadAuthor(int authorId);
	public void saveAuthor(Author author);
	
	public void replaceAuthors(JochreDocument doc);
	
	/**
	 * Find all existing authors in the database.
	 */
	public List<Author> findAuthors();


	public void deleteJochrePage(JochrePage page);
}
