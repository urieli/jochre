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

import org.apache.lucene.search.Query;

/**
 * A single query sent to the searcher.
 * @author Assaf Urieli
 *
 */

public interface JochreQuery {
	/**
	 * The number of decimal places to display for each score. Default is 4.
	 * @return
	 */
	public int getDecimalPlaces();
	public void setDecimalPlaces(int decimalPlaces);

	/**
	 * The maximum number of docs to return. Default is 20.
	 * @return
	 */
	public int getMaxDocs();
	public void setMaxDocs(int maxDocs);

	/**
	 * The actual query string, as interpreted by a Lucene query parser.
	 * @return
	 */
	public String getQueryString();
	public void setQueryString(String queryString);
	
	/**
	 * A Lucene query corresponding to the text querystring of this Jochre query,
	 * needed for highlighting.
	 * @return
	 */
	public Query getLuceneTextQuery();
	
	/**
	 * A Lucene query corresponding to this Jochre query.
	 * @return
	 */
	public Query getLuceneQuery();
	public abstract void setTitleQueryString(String titleQueryString);
	public abstract String getTitleQueryString();
	public abstract void setAuthorQueryString(String authorQueryString);
	public abstract String getAuthorQueryString();
}