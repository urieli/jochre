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

public class AltoPageImpl implements AltoPage {
	private AltoDocument document;
	private List<AltoTextBlock> textBlocks = new ArrayList<AltoTextBlock>();
	private List<AltoTextLine> textLines = new ArrayList<AltoTextLine>();
	private int pageIndex;
	private int width;
	private int height;
	private int wordCount = -1;
	private double confidence = 0;
	
	public AltoPageImpl(AltoDocument doc, int pageIndex, int width, int height) {
		super();
		this.document = doc;
		this.pageIndex = pageIndex;
		this.width = width;
		this.height = height;
		this.document.getPages().add(this);
	}

	@Override
	public List<AltoTextBlock> getTextBlocks() {
		return textBlocks;
	}

	public List<AltoTextLine> getTextLines() {
		return textLines;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getIndex() {
		return pageIndex;
	}


	public AltoDocument getDocument() {
		return document;
	}

	@Override
	public int wordCount() {
		if (wordCount<0) {
			wordCount = 0;
			for (AltoTextBlock block : this.getTextBlocks()) {
				wordCount += block.wordCount();
			}
		}
		return wordCount;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}
	
	
}
