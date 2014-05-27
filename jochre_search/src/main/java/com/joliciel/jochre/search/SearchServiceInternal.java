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
import java.util.Map;

interface SearchServiceInternal extends SearchService {
	public JochreXmlLetter newLetter(JochreXmlWord word, String text, int left, int top, int right, int bottom);
	public JochreXmlWord newWord(JochreXmlRow row, String text, int left, int top, int right, int bottom);
	public JochreXmlRow newRow(JochreXmlParagraph paragraph, int left, int top, int right, int bottom);
	public JochreXmlParagraph newParagraph(JochreXmlImage page, int left, int top, int right, int bottom);
	public JochreXmlImage newImage(String fileNameBase, int pageIndex, int imageIndex, int width, int height);
	public JochreXmlDocument newDocument();
	public JochreXmlReader getJochreXmlReader(JochreXmlDocument doc);
	public CoordinateStorage getCoordinateStorage();
	public JochreIndexDocument newJochreIndexDocument(File directory, int index, StringBuilder sb, CoordinateStorage coordinateStorage, int startPage, int endPage, Map<String,String> fields);
}
