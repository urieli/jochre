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
package com.joliciel.jochre.search.alto;

import java.util.ArrayList;
import java.util.List;

class AltoTextBlockImpl implements AltoTextBlock {
	private List<AltoTextLine> rows = new ArrayList<AltoTextLine>();
	private int left, top, right, bottom;
	private AltoPage page;
	private int wordCount = -1;
	private int index = -1;
	
	public AltoTextBlockImpl(AltoPage page, int left, int top, int right, int bottom) {
		super();
		this.page = page;
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
		this.index = this.page.getTextBlocks().size();
		this.page.getTextBlocks().add(this);
	}

	@Override
	public List<AltoTextLine> getTextLines() {
		return rows;
	}

	public int getLeft() {
		return left;
	}

	public int getTop() {
		return top;
	}

	public int getWidth() {
		return right;
	}

	public int getHeight() {
		return bottom;
	}

	public AltoPage getPage() {
		return page;
	}

	@Override
	public int wordCount() {
		if (wordCount<0) {
			wordCount = 0;
			for (AltoTextLine row : this.getTextLines()) {
				wordCount += row.wordCount();
			}
		}
		return wordCount;
	}
	
	public int getIndex() {
		return index;
	}
}
