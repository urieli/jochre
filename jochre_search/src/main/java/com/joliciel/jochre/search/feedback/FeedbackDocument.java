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

/**
 * A representation of a document which has been indexed.
 * Although a document can be broken up into multiple sub-documents, these could potentially change from one index run to the next,
 * while the top-level path will not. Hence we don't store the document sub-index, only it's path.
 * @author Assaf Urieli
 *
 */
public interface FeedbackDocument {
	/**
	 * The document's unique internal id.
	 * @return
	 */
	public int getId();
	
	/**
	 * The document's path, which identifies it outside of the index.
	 * @return
	 */
	public String getPath();
}
