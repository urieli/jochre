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

import java.util.ArrayList;
import java.util.List;

public class SearchPageImpl implements SearchPage {
	private String fileNameBase;
	private List<SearchParagraph> paragraphs = new ArrayList<SearchParagraph>();
	private int pageIndex;
	private int imageIndex;
	private int width;
	private int height;
	
	public SearchPageImpl(String fileNameBase, int pageIndex, int imageIndex, int width, int height) {
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
	public List<SearchParagraph> getParagraphs() {
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

}
