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
import com.joliciel.jochre.doc.JochreDocumentInternal;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.doc.JochrePageInternal;

interface DocumentServiceInternal extends DocumentService {
	public JochrePageInternal getEmptyJochrePageInternal();

	public JochreDocumentInternal getEmptyJochreDocumentInternal();

	public void saveJochrePage(JochrePage page);


	public List<JochrePage> findPages(JochreDocument document);

	public void saveJochreDocument(JochreDocument jochreDocument);

	public AuthorInternal getEmptyAuthorInternal();
	
	public List<? extends Author> findAuthors(JochreDocument doc);
	public void saveAuthor(Author author);
	
	public void replaceAuthors(JochreDocument doc);

	public void deleteJochrePage(JochrePage page);
}
