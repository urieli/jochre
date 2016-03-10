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
package com.joliciel.jochre.search;

import java.io.File;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

import com.joliciel.jochre.search.alto.AltoPage;

interface SearchServiceInternal extends SearchService {
	public JochreIndexDirectory getJochreIndexDirectory(File dir);
	public JochreIndexDocument newJochreIndexDocument(JochreIndexDirectory directory, int index, List<AltoPage> currentPages);
	public Tokenizer getJochreTokeniser(TokenExtractor tokenExtractor,
			String fieldName);
	
	/**
	 * An analyser using a {@link TokenExtractor} to passed to {@link #getJochreTokeniser(TokenExtractor, String)}, used when the actual
	 * tokenising occurred prior to analysis (e.g. in the case of the OCR text layer).
	 * @param tokenExtractor
	 * @return
	 */
	public Analyzer getJochreTextLayerAnalyzer(TokenExtractor tokenExtractor);
	
	public Analyzer getJochreMetaDataAnalyzer();
	
	public JochreQueryAnalyser getJochreQueryAnalyzer();
	
	public JochreToken getJochreToken(JochreToken jochreToken);
	public JochreToken getJochreToken(String text);
	
	
	/**
	 * Get a TokenFilter to be used on queries in the current locale, or null if none avaialble.
	 * @param input
	 * @return
	 */
	public TokenFilter getQueryTokenFilter(TokenStream input);
	
	/**
	 * Get a JochreIndexWord at a particular offset of a document.
	 * @param doc
	 * @param startOffset
	 * @return
	 */
	public JochreIndexWord getWord(JochreIndexDocument doc, int startOffset);
}
