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

interface SearchServiceInternal extends SearchService {
	public SearchLetter newLetter(SearchWord word, String text, int left, int top, int right, int bottom);
	public SearchWord newWord(String text, int left, int top, int right, int bottom);
	public SearchRow newRow(int left, int top, int right, int bottom);
	public SearchParagraph newParagraph(int left, int top, int right, int bottom);
	public SearchPage newPage(String fileNameBase, int width, int height);
	public SearchDocument newDocument();
	public JochreXmlReader getJochreXmlReader(SearchDocument doc);
	public CoordinateStorage getCoordinateStorage();
}
