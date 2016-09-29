package com.joliciel.jochre.occitan;

import java.util.HashMap;
import java.util.Map;

import com.joliciel.jochre.Jochre;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class JochreOccitan extends Jochre {

	public JochreOccitan(Config config) throws ReflectiveOperationException {
		super(config);
	}

	public static void main(String[] args) throws Exception {
		Map<String, String> argMap = new HashMap<String, String>();

		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos + 1);
			argMap.put(argName, argValue);
		}

		Config config = ConfigFactory.load();

		JochreOccitan jochre = new JochreOccitan(config);
		jochre.execute(argMap);
	}
}
