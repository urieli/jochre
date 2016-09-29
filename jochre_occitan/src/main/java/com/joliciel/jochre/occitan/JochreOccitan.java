package com.joliciel.jochre.occitan;

import java.util.HashMap;
import java.util.Map;

import com.joliciel.jochre.Jochre;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class JochreOccitan {
	public static void main(String[] args) throws Exception {
		Map<String, String> argMap = new HashMap<String, String>();

		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos + 1);
			argMap.put(argName, argValue);
		}

		Config config = ConfigFactory.load();

		Jochre jochre = new Jochre(config, argMap);
		jochre.execute(argMap);
	}
}
