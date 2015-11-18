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

class AltoTextBlockImpl implements AltoTextBlock {
	private List<AltoTextLine> rows = new ArrayList<AltoTextLine>();
	private Rectangle rectangle;
	private AltoPage page;
	private int wordCount = -1;
	private int index = -1;
	
	public AltoTextBlockImpl(AltoPage page, int left, int top, int width, int height) {
		super();
		this.page = page;
		this.rectangle = new Rectangle(left, top, width, height);
		this.index = this.page.getTextBlocks().size();
		this.page.getTextBlocks().add(this);
	}

	@Override
	public List<AltoTextLine> getTextLines() {
		return rows;
	}
	
	@Override
	public Rectangle getRectangle() {
		return rectangle;
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

	@Override
	public void joinHyphens() {
		for (int i=0; i<this.rows.size(); i++) {
			AltoTextLine row = this.rows.get(i);
			for (int j=0; j<row.getStrings().size(); j++) {
				AltoString string = row.getStrings().get(j);
				if (string.isHyphenStart() && string.getHyphenatedContent()!=null) {
					if (i+1<this.rows.size()) {
						if (j+1<row.getStrings().size()) {
							AltoString hyphen = row.getStrings().get(j+1);
							if (hyphen.isHyphen()) {
								string.getRectangle().add(hyphen.getRectangle());
								row.getStrings().remove(j+1);
								j++;
							}
						}
						AltoTextLine nextRow = this.rows.get(i+1);
						if (nextRow.getStrings().size()>0) {
							AltoString nextString = nextRow.getStrings().get(0);
							
							List<String> alternatives = new ArrayList<String>();
							for (String contentA : string.getContentStrings()) {
								for (String contentB : nextString.getContentStrings()) {
									String alternative = contentA + "-" + contentB;
									if (!string.getHyphenatedContent().equals(alternative))
										alternatives.add(contentA + "-" + contentB);
								}
							}
							string.setAlternatives(alternatives);
							string.setContent(string.getHyphenatedContent());
							string.setContentStrings(null);
							
							string.setSpanEnd(nextString.getSpanEnd());
							string.setSecondaryRectangle(nextString.getRectangle());
							nextRow.getStrings().remove(0);
						}
					}

				}
			}
		}
	}
}
