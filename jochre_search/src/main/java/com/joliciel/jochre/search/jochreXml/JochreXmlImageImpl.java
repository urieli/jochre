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

import java.util.ArrayList;
import java.util.List;

public class JochreXmlImageImpl implements JochreXmlImage {
	private String fileNameBase;
	private List<JochreXmlParagraph> paragraphs = new ArrayList<JochreXmlParagraph>();
	private int pageIndex;
	private int imageIndex;
	private int width;
	private int height;
	private int wordCount = -1;
	
	public JochreXmlImageImpl(String fileNameBase, int pageIndex, int imageIndex, int width, int height) {
		super();
		this.fileNameBase = fileNameBase;
		this.pageIndex = pageIndex;
		this.imageIndex = imageIndex;
		this.width = width;
		this.height = height;
	}

	@Override
	public String getFileNameBase() {
		return fileNameBase;
	}

	@Override
	public List<JochreXmlParagraph> getParagraphs() {
		return paragraphs;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getPageIndex() {
		return pageIndex;
	}

	public int getImageIndex() {
		return imageIndex;
	}

	@Override
	public int wordCount() {
		if (wordCount<0) {
			wordCount = 0;
			for (JochreXmlParagraph par : this.getParagraphs()) {
				wordCount += par.wordCount();
			}
		}
		return wordCount;
	}
}
