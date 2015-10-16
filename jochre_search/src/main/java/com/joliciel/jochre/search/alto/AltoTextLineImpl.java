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

import com.joliciel.jochre.search.Rectangle;

class AltoTextLineImpl implements AltoTextLine {
	private List<AltoString> strings = new ArrayList<AltoString>();
	private int left, top, width, height;
	private AltoTextBlock textBlock;
	private int wordCount = -1;
	private int index = -1;
	private Rectangle rectangle = null;
	
	public AltoTextLineImpl(AltoTextBlock textBlock, int left, int top, int width, int height) {
		super();
		this.textBlock = textBlock;
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
		this.index = this.textBlock.getTextLines().size();
		this.textBlock.getTextLines().add(this);
	}

	@Override
	public List<AltoString> getStrings() {
		return strings;
	}

	public int getLeft() {
		return left;
	}

	public int getTop() {
		return top;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public AltoTextBlock getTextBlock() {
		return textBlock;
	}

	@Override
	public int wordCount() {
		if (wordCount<0) {
			for (AltoString string : this.strings) {
				if (!string.isWhiteSpace())
					wordCount++;
			}
		}
		return wordCount;
	}
	
	public int getIndex() {
		return index;
	}

	@Override
	public Rectangle getRectangle() {
		if (rectangle==null) {
			rectangle = new Rectangle(left, top, left + width -1, top + height -1);
		}
		return rectangle;
	}
}
