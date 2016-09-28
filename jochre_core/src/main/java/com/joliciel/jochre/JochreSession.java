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
package com.joliciel.jochre;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.jochre.lexicon.DefaultLexiconWrapper;
import com.joliciel.jochre.lexicon.FakeLexicon;
import com.joliciel.jochre.lexicon.Lexicon;
import com.joliciel.jochre.lexicon.LexiconMerger;
import com.joliciel.jochre.lexicon.TextFileLexicon;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.ObjectCache;
import com.joliciel.talismane.utils.SimpleObjectCache;
import com.typesafe.config.Config;

/**
 * A class storing session-wide reference data.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreSession {
	private static final Logger LOG = LoggerFactory.getLogger(JochreSession.class);
	private final Locale locale;
	private final Linguistics linguistics;
	private final double junkConfidenceThreshold;
	private final Config config;
	private final ObjectCache objectCache;
	private final Charset encoding;
	private final Charset csvEncoding;
	private Lexicon lexicon;

	/**
	 * 
	 * @param config
	 *            the configuration for this session
	 */
	public JochreSession(Config config) {
		this.config = config;
		Config jochreConfig = config.getConfig("jochre");

		this.locale = Locale.forLanguageTag(jochreConfig.getString("locale"));
		this.objectCache = new SimpleObjectCache();

		if (jochreConfig.hasPath("encoding"))
			encoding = Charset.forName(jochreConfig.getString("encoding"));
		else
			encoding = Charset.defaultCharset();

		String csvSeparator = jochreConfig.getString("csv.separator");
		CSVFormatter.setGlobalCsvSeparator(csvSeparator);

		if (jochreConfig.hasPath("csv.encoding"))
			csvEncoding = Charset.forName(jochreConfig.getString("csv.encoding"));
		else
			csvEncoding = Charset.defaultCharset();

		if (jochreConfig.hasPath("csv.locale")) {
			String csvLocaleString = jochreConfig.getString("csv.locale");
			Locale csvLocale = Locale.forLanguageTag(csvLocaleString);
			CSVFormatter.setGlobalLocale(csvLocale);
		}

		if (jochreConfig.hasPath("lexicon")) {
			String lexiconPath = jochreConfig.getString("lexicon");
			File lexiconDir = new File(lexiconPath);
			Lexicon myLexicon = this.readLexicon(lexiconDir);
			this.lexicon = new DefaultLexiconWrapper(myLexicon, this.locale);
		} else {
			this.lexicon = new FakeLexicon();
		}

		String linguisticsClassName = jochreConfig.getString("linguistics.class");
		LOG.debug("linguisticsClassName: " + linguisticsClassName);
		try {
			@SuppressWarnings("rawtypes")
			Class linguisticsClass = Class.forName(linguisticsClassName);
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Constructor constructor = linguisticsClass.getConstructor(new Class[] {});
			this.linguistics = (Linguistics) constructor.newInstance();
		} catch (ReflectiveOperationException e) {
			LOG.error("Unable to load class " + linguisticsClassName);
			throw new RuntimeException("Unable to load class " + linguisticsClassName, e);
		}
		this.linguistics.setJochreSession(this);

		this.junkConfidenceThreshold = jochreConfig.getDouble("image-analyser.junk-threshold");
	}

	public Locale getLocale() {
		return locale;
	}

	public Linguistics getLinguistics() {
		return linguistics;
	}

	/**
	 * The average confidence below which a paragraph is considered to be junk,
	 * when considering all of its letters.
	 */
	public double getJunkConfidenceThreshold() {
		return junkConfidenceThreshold;
	}

	public Config getConfig() {
		return config;
	}

	public ObjectCache getObjectCache() {
		return objectCache;
	}

	public Charset getEncoding() {
		return encoding;
	}

	public Charset getCsvEncoding() {
		return csvEncoding;
	}

	protected Lexicon readLexicon(File lexiconDir) {
		Lexicon myLexicon = null;

		if (lexiconDir.isDirectory()) {
			LexiconMerger lexiconMerger = new LexiconMerger();
			File[] lexiconFiles = lexiconDir.listFiles();
			for (File lexiconFile : lexiconFiles) {
				if (lexiconFile.getName().endsWith(".txt")) {
					TextFileLexicon textFileLexicon = new TextFileLexicon(lexiconFile, this.encoding);
					lexiconMerger.addLexicon(textFileLexicon);
				} else {
					TextFileLexicon textFileLexicon = TextFileLexicon.deserialize(lexiconFile);
					lexiconMerger.addLexicon(textFileLexicon);
				}
			}

			myLexicon = lexiconMerger;
		} else {
			if (lexiconDir.getName().endsWith(".txt")) {
				TextFileLexicon textFileLexicon = new TextFileLexicon(lexiconDir, this.encoding);
				myLexicon = textFileLexicon;
			} else {
				TextFileLexicon textFileLexicon = TextFileLexicon.deserialize(lexiconDir);
				myLexicon = textFileLexicon;
			}
		}
		return myLexicon;
	}

	public Lexicon getLexicon() {
		return this.lexicon;
	}

	public void setLexicon(Lexicon lexicon) {
		this.lexicon = lexicon;
	}

}
