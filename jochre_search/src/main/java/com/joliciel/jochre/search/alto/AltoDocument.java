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

public class AltoDocument {
	private String name;
	private List<AltoPage> pages = new ArrayList<>();
	private int wordCount = -1;

	public AltoDocument(String name) {
		super();
		this.name = name;
	}

	public List<AltoPage> getPages() {
		return pages;
	}

	public int wordCount() {
		if (wordCount < 0) {
			wordCount = 0;
			for (AltoPage page : this.getPages()) {
				wordCount += page.wordCount();
			}
		}
		return wordCount;
	}

	public String getName() {
		return name;
	}

}
