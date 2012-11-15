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

/**
 * A service for retrieving implementations of the output package.
 * @author Assaf Urieli
 * 
 */
public interface OutputService {
	/**
	 * See getTextGetter(Writer, TextFormat, Lexicon), except that all end-of-row hyphens
	 * are considered to be soft hyphens (and removed).
	 * @author Assaf Urieli
	 *
	 */
	public DocumentObserver getTextGetter(Writer writer, TextFormat textFormat);
	
	/**
	 * Converts Jochre's analysis to human-readable text, either in plain or xhtml format.
	 * Note that the current implementation has some Yiddish-specific rules (around Yiddish-style
	 * double-quotes) which will need to be generalised.
	 * @param writer the writer where the text should be written.
	 * @param textFormat plain or xhtml
	 * @param lexicon a lexicon for deciding whether an end-of-row hyphen is a hard hyphen or not
	 * @return
	 */
	public DocumentObserver getTextGetter(Writer writer, TextFormat textFormat, Lexicon lexicon);
	
	/**
	 * Outputs Jochre's analysis to an XML format as per the XML spec indicated by http://finereader.abbyy.com/
	 * @param writer
	 * @return
	 */
	public DocumentObserver getAbbyyFineReader8Exporter(Writer writer);
}
