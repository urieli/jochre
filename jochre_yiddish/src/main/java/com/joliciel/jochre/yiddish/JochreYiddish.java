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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import com.joliciel.jochre.Jochre;
import com.joliciel.jochre.utils.JochreLogUtils;
import com.joliciel.talismane.utils.StringUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class JochreYiddish {
	public static void main(String[] args) throws Exception {
		Map<String, String> argMap = StringUtils.convertArgs(args);

		String logConfigPath = argMap.get("logConfigFile");
		if (logConfigPath != null) {
			argMap.remove("logConfigFile");
			JochreLogUtils.configureLogging(logConfigPath);
		}

		String command = argMap.get("command");
		if (command != null && (command.equals("metadata") || command.equals("booklist"))) {
			String inDirPath = null;
			boolean forceUpdate = false;
			String outFilePath = null;

			argMap.remove("command");

			for (Entry<String, String> argMapEntry : argMap.entrySet()) {
				String argName = argMapEntry.getKey();
				String argValue = argMapEntry.getValue();
				if (argName.equals("inDir"))
					inDirPath = argValue;
				else if (argName.equals("outFile"))
					outFilePath = argValue;
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

			if (command.equals("metadata")) {

				fetcher.setForceUpdate(forceUpdate);
				fetcher.fetchMetaData(inDir);
			} else {
				if (outFilePath == null)
					throw new RuntimeException("For command " + command + ", outFile is required");

				File outFile = new File(outFilePath);
				Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, false), "UTF-8"));
				fetcher.buildBookHtml(inDir, writer);
			}
		} else {
			Config config = ConfigFactory.load();
			Jochre jochre = new Jochre(config, argMap);
			jochre.execute(argMap);
		}
	}
}
