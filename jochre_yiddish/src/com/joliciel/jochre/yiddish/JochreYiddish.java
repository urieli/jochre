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
package com.joliciel.jochre.yiddish;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.Jochre;
import com.joliciel.jochre.lexicon.Lexicon;
import com.joliciel.jochre.lexicon.LexiconMerger;
import com.joliciel.jochre.lexicon.LocaleSpecificLexiconService;
import com.joliciel.jochre.lexicon.TextFileLexicon;
import com.joliciel.jochre.lexicon.WordSplitter;

public class JochreYiddish extends Jochre implements LocaleSpecificLexiconService {
	public JochreYiddish() {
		super(new Locale("yi"));
	}

	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(JochreYiddish.class);

	WordSplitter yiddishWordSplitter = null;
	Lexicon yiddishLexicon = null;
	
	public static void main(String[] args) throws Exception {
		Map<String, String> argMap = new HashMap<String, String>();
		
		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			argMap.put(argName, argValue);
		}
		
		JochreYiddish jochre = new JochreYiddish();
		jochre.execute(argMap);
	}

	@Override
	public Lexicon getLexicon() {
		if (yiddishLexicon == null && this.getLexiconDirPath()!=null && this.getLexiconDirPath().length()>0) {
			LexiconMerger lexiconMerger = new LexiconMerger();
			File lexiconDir = new File(this.getLexiconDirPath());
		
			File[] lexiconFiles = lexiconDir.listFiles();
			for (File lexiconFile : lexiconFiles) {
				TextFileLexicon lexicon = TextFileLexicon.deserialize(lexiconFile);
				lexiconMerger.addLexicon(lexicon);
			}
	
			yiddishLexicon = new YiddishWordFrequencyFinder(lexiconMerger);
		}
		return yiddishLexicon;
	}

	@Override
	public WordSplitter getWordSplitter() {
		if (yiddishWordSplitter==null) {
			yiddishWordSplitter = new YiddishWordSplitter();
		}
		return yiddishWordSplitter;
	}

}
