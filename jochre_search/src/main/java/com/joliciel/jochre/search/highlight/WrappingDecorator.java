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
package com.joliciel.jochre.search.highlight;

import com.joliciel.jochre.search.JochreSearchConfig;

public class WrappingDecorator implements HighlightTermDecorator {
	private final String before;
	private final String after;

	public WrappingDecorator(JochreSearchConfig config) {
		this.before = config.getConfig().getString("highlighter.wrapping-decorator.before");
		this.after = config.getConfig().getString("highlighter.wrapping-decorator.after");
	}

	@Override
	public String decorate(String term) {
		return before + term + after;
	}

}
