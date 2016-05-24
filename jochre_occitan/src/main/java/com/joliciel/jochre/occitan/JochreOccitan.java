package com.joliciel.jochre.occitan;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.Jochre;
import com.joliciel.jochre.lexicon.LocaleSpecificLexiconService;

public class JochreOccitan extends Jochre implements LocaleSpecificLexiconService {
	public JochreOccitan() {
		super();
		this.setLocale(Locale.forLanguageTag("oc"));
	}

	public static void main(String[] args) throws Exception {
		Map<String, String> argMap = new HashMap<String, String>();
		
		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			argMap.put(argName, argValue);
		}
		
		JochreOccitan jochre = new JochreOccitan();
		jochre.execute(argMap);
	}
	
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(JochreOccitan.class);

}
