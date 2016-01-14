package com.joliciel.jochre.search.lexicon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.utils.LogUtils;

class LexiconServiceImpl implements LexiconService {
	private static final Log LOG = LogFactory.getLog(LexiconServiceImpl.class);
	
	private Map<Locale,TextNormaliser> textNormaliserMap = new HashMap<Locale, TextNormaliser>();
	
	@Override
	public TextNormaliser getTextNormaliser(Locale locale) {
		TextNormaliser textNormaliser = null;
		if (locale.getLanguage().equals("yi")||
				locale.getLanguage().equals("ji")) {
			textNormaliser = textNormaliserMap.get(locale);
			if (textNormaliser==null) {
				textNormaliser = new YiddishTextNormaliser();
				textNormaliserMap.put(locale, textNormaliser);
			}
		}
		return textNormaliser;
	}

	@Override
	public TextFileLexicon getTextFileLexicon(Locale locale) {
		TextFileLexiconImpl textFileLexicon = new TextFileLexiconImpl(locale);
		textFileLexicon.setLexiconService(this);
		return textFileLexicon;
	}

	@Override
	public Lexicon deserializeLexicon(File lexiconFile) {
		try {
			TextFileLexiconImpl lexicon = null;
			FileInputStream fis = new FileInputStream(lexiconFile);
			ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry ze = null;
		    while ((ze = zis.getNextEntry()) != null) {
		    	LOG.debug(ze.getName());
		    	if (ze.getName().endsWith(".obj")) {
		    		LOG.debug("deserializing " + ze.getName());
					@SuppressWarnings("resource")
					ObjectInputStream in = new ObjectInputStream(zis);
					lexicon = (TextFileLexiconImpl) in.readObject();
					break;
		    	}
		    }
		    zis.close();
		    lexicon.setLexiconService(this);
		    return lexicon;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new JochreException(e);
		} catch (ClassNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new JochreException(e);
		}
	}
}
