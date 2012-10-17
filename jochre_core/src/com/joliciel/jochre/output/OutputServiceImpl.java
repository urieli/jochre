///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.jochre.output;

import java.io.Writer;

import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.lexicon.Lexicon;

class OutputServiceImpl implements OutputService {
	@Override
	public DocumentObserver getTextGetter(Writer writer, TextFormat textFormat) {
		return this.getTextGetter(writer, textFormat, null);
	}

	@Override
	public DocumentObserver getTextGetter(Writer writer, TextFormat textFormat,
			Lexicon lexicon) {
		TextGetterImpl textGetter =  new TextGetterImpl(writer, textFormat, lexicon);
		return textGetter;
	}

	@Override
	public DocumentObserver getAbbyyFineReader8Exporter(Writer writer) {
		AbbyyFineReader8Exporter exporter = new AbbyyFineReader8Exporter(writer);
		return exporter;
	}

}
