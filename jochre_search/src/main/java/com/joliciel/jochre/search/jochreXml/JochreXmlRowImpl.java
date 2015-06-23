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

class JochreXmlRowImpl implements JochreXmlRow {
	private List<JochreXmlWord> words = new ArrayList<JochreXmlWord>();
	private int left, top, right, bottom;
	private JochreXmlParagraph paragraph;
	private int wordCount = -1;
	
	public JochreXmlRowImpl(JochreXmlParagraph paragraph, int left, int top, int right, int bottom) {
		super();
		this.paragraph = paragraph;
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
	}

	@Override
	public List<JochreXmlWord> getWords() {
		return words;
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

	public JochreXmlParagraph getParagraph() {
		return paragraph;
	}

	@Override
	public int wordCount() {
		if (wordCount<0) {
			wordCount = this.getWords().size();
		}
		return wordCount;
	}
	
	
}
