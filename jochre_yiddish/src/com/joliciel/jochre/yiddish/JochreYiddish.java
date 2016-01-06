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
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.joliciel.jochre.Jochre;
import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.lexicon.FakeLexicon;
import com.joliciel.jochre.lexicon.Lexicon;
import com.joliciel.jochre.lexicon.LocaleSpecificLexiconService;
import com.joliciel.jochre.lexicon.WordSplitter;

public class JochreYiddish extends Jochre implements LocaleSpecificLexiconService {
	public JochreYiddish() {
		super();
		this.setLocale(new Locale("yi"));
		JochreSession.getInstance().setLinguistics(new YiddishLinguistics());
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
		
		String logConfigPath = argMap.get("logConfigFile");
		if (logConfigPath!=null) {
			argMap.remove("logConfigFile");
			Properties props = new Properties();
			props.load(new FileInputStream(logConfigPath));
			PropertyConfigurator.configure(props);
		}
		
		String command = argMap.get("command");
		if (command!=null && command.equals("metadata")) {
			String inDirPath = null;
			boolean forceUpdate = false;
			
			for (Entry<String, String> argMapEntry : argMap.entrySet()) {
				String argName = argMapEntry.getKey();
				String argValue = argMapEntry.getValue();
				if (argName.equals("inDir"))
					inDirPath = argValue;
				else if (argName.equals("forceUpdate"))
					forceUpdate = argValue.equals("true");
				else
					throw new RuntimeException("Unknown argument: " + argName);
			}
			
			if (inDirPath==null)
				throw new RuntimeException("For command " + command + ", inDir is required");
			
			File inDir = new File(inDirPath);
			if (!inDir.exists() || !inDir.isDirectory()) {
				throw new RuntimeException("inDir does not exist or is not a directory: " + inDir.getAbsolutePath());
			}
			
			YiddishMetaFetcher fetcher = new YiddishMetaFetcher();
			fetcher.setForceUpdate(forceUpdate);
			fetcher.fetchMetaData(inDir);
		} else {
			JochreYiddish jochre = new JochreYiddish();
			jochre.execute(argMap);
		}
	}

	@Override
	public Lexicon getLexicon() {
		if (yiddishLexicon == null) {
			if (this.getLexiconPath()!=null && this.getLexiconPath().length()>0) {
				File lexiconDir = new File(this.getLexiconPath());
				Lexicon myLexicon = this.readLexicon(lexiconDir);
				yiddishLexicon = new YiddishWordFrequencyFinder(myLexicon);
			} else {
				yiddishLexicon = new FakeLexicon();
			}
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
