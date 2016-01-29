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
import com.joliciel.jochre.utils.JochreException;

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
	public DocumentObserver getTextGetter(File outDir, TextFormat textFormat,
			Lexicon lexicon) {
		TextGetterImpl textGetter =  new TextGetterImpl(outDir, textFormat, lexicon);
		return textGetter;
	}

	@Override
	public DocumentObserver getJochrePageByPageExporter(File outputDir,
			String baseName) {
		JochrePageByPageExporter exporter = new JochrePageByPageExporter(outputDir, baseName);
		return exporter;
	}

	public DocumentObserver getExporter(File outputDir, ExportFormat exportFormat) {
		return this.getExporter(outputDir, null, exportFormat);
	}

	@Override
	public DocumentObserver getExporter(Writer writer, ExportFormat exportFormat) {
		return this.getExporter(null, writer, exportFormat);
	}
	
	public DocumentObserver getExporter(File outputDir, Writer writer, ExportFormat exportFormat) {
		DocumentObserver exporter = null;
		switch (exportFormat) {
		case Abbyy: {
			if (outputDir==null) {
				AbbyyFineReader8Exporter myExporter = new AbbyyFineReader8Exporter(writer);
				exporter = myExporter;
			} else {
				AbbyyFineReader8Exporter myExporter = new AbbyyFineReader8Exporter(outputDir);
				exporter = myExporter;
			}
			break;
		}
		case Alto: {
			if (outputDir==null) {
				AltoXMLExporter myExporter = new AltoXMLExporter(writer);
				exporter = myExporter;
			} else {
				AltoXMLExporter myExporter = new AltoXMLExporter(outputDir);
				exporter = myExporter;
			}
			break;
		}
		case Jochre: {
			if (outputDir==null) {
				JochreXMLExporter myExporter = new JochreXMLExporter(writer);
				exporter = myExporter;
			} else {
				JochreXMLExporter myExporter = new JochreXMLExporter(outputDir);
				exporter = myExporter;
			}
			break;
		}
		case GuessedText: {
			if (outputDir==null) {
				TextExporter myExporter = new TextExporter(writer);
				exporter = myExporter;
			} else {
				TextExporter myExporter = new TextExporter(outputDir);
				exporter = myExporter;
			}
			break;
		}
		default:
			throw new JochreException("Export format currently unsupported: " + exportFormat);
		}
		return exporter;
	}


}
