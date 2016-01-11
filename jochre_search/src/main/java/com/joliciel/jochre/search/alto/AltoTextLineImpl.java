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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

class AltoTextLineImpl implements AltoTextLine {
	private List<AltoString> strings = new ArrayList<AltoString>();
	private Rectangle rectangle;
	private AltoTextBlock textBlock;
	private int wordCount = -1;
	private int index = -1;
	
	public AltoTextLineImpl(AltoTextBlock textBlock, int left, int top, int width, int height) {
		super();
		this.textBlock = textBlock;
		this.rectangle = new Rectangle(left, top, width, height);
		this.index = this.textBlock.getTextLines().size();
		this.textBlock.getTextLines().add(this);
	}

	@Override
	public List<AltoString> getStrings() {
		return strings;
	}
	
	@Override
	public Rectangle getRectangle() {
		return rectangle;
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
	
	public void recalculate() {
		int i=0;
		for (AltoString string : this.strings) {
			string.setIndex(i++);
		}
	}

	@Override
	public String toString() {
		return "AltoTextLineImpl [index=" + index + ", strings=" + strings
				+ "]";
	}
	
	
}
