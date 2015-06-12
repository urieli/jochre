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

import java.io.File;
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
	public DocumentObserver getJochrePageByPageExporter(File outputDir,
			String baseName) {
		JochrePageByPageExporter exporter = new JochrePageByPageExporter(outputDir, baseName);
		return exporter;
	}

	@Override
	public DocumentObserver getExporter(Writer writer, ExportFormat exportFormat) {
		DocumentObserver exporter = null;
		switch (exportFormat) {
		case Abbyy: {
			AbbyyFineReader8Exporter myExporter = new AbbyyFineReader8Exporter(writer);
			exporter = myExporter;
			break;
		}
		case Alto: {
			AltoXMLExporter myExporter = new AltoXMLExporter(writer);
			exporter = myExporter;
			break;
		}
		case Jochre: {
			JochreXMLExporter myExporter = new JochreXMLExporter(writer);
			exporter = myExporter;
			break;
		}
		}
		return exporter;
	}

}
