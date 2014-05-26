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

class SearchParagraphImpl implements SearchParagraph {
	private List<SearchRow> rows = new ArrayList<SearchRow>();
	private int left, top, right, bottom;
	private SearchImage page;
	
	public SearchParagraphImpl(SearchImage page, int left, int top, int right, int bottom) {
		super();
		this.page = page;
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
	}

	@Override
	public List<SearchRow> getRows() {
		return rows;
	}

	public int getLeft() {
		return left;
	}

	public int getTop() {
		return top;
	}

	public int getRight() {
		return right;
	}

	public int getBottom() {
		return bottom;
	}

	public SearchImage getPage() {
		return page;
	}
	
	
}
