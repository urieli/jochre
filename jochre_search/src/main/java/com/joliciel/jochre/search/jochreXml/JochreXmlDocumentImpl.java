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
package com.joliciel.jochre.search.jochreXml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JochreXmlDocumentImpl implements JochreXmlDocument {
	private File directory;
	private List<JochreXmlImage> images = new ArrayList<JochreXmlImage>();
	private int wordCount = -1;
	
	@Override
	public File getDirectory() {
		return directory;
	}

	@Override
	public List<JochreXmlImage> getImages() {
		return images;
	}


	@Override
	public int wordCount() {
		if (wordCount<0) {
			wordCount = 0;
			for (JochreXmlImage image : this.getImages()) {
				wordCount += image.wordCount();
			}
		}
		return wordCount;
	}
}
