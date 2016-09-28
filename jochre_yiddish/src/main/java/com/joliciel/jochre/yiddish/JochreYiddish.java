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
import java.util.Map;
import java.util.Map.Entry;

import com.joliciel.jochre.Jochre;
import com.joliciel.jochre.utils.JochreLogUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class JochreYiddish extends Jochre {
	public JochreYiddish(Config config) {
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

		String logConfigPath = argMap.get("logConfigFile");
		argMap.remove("logConfigFile");
		JochreLogUtils.configureLogging(logConfigPath);

		String command = argMap.get("command");
		if (command != null && command.equals("metadata")) {
			String inDirPath = null;
			boolean forceUpdate = false;

			argMap.remove("command");

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

			if (inDirPath == null)
				throw new RuntimeException("For command " + command + ", inDir is required");

			File inDir = new File(inDirPath);
			if (!inDir.exists() || !inDir.isDirectory()) {
				throw new RuntimeException("inDir does not exist or is not a directory: " + inDir.getAbsolutePath());
			}

			YiddishMetaFetcher fetcher = new YiddishMetaFetcher();
			fetcher.setForceUpdate(forceUpdate);
			fetcher.fetchMetaData(inDir);
		} else {
			Config config = ConfigFactory.load();
			JochreYiddish jochre = new JochreYiddish(config);
			jochre.execute(argMap);
		}
	}
}
