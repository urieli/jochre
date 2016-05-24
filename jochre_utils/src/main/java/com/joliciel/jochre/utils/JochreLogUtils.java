package com.joliciel.jochre.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class JochreLogUtils {

	/**
	 * If logConfigPath is not null, use it to configure logging. Otherwise, use
	 * the default configuration file.
	 */
	public static void configureLogging(String logConfigPath) {
		try {
			if (logConfigPath != null) {
				File slf4jFile = new File(logConfigPath);
				if (slf4jFile.exists()) {
					try (InputStream stream = new BufferedInputStream(new FileInputStream(slf4jFile))) {
						JochreLogUtils.configureLogging(stream);
					}
				} else {
					throw new JochreException("missing logConfigFile: " + slf4jFile.getCanonicalPath());
				}
			} else {
				try (InputStream stream = JochreLogUtils.class.getResourceAsStream("/com/joliciel/jochre/utils/resources/default-logback.xml")) {
					JochreLogUtils.configureLogging(stream);
				}
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println(e.getStackTrace().toString());
			throw new RuntimeException(e);
		}
	}

	public static void configureLogging(InputStream stream) {
		try {
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			// Call context.reset() to clear any previous configuration,
			// e.g. default configuration
			loggerContext.reset();

			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(loggerContext);
			configurator.setContext(loggerContext);
			// Call context.reset() to clear any previous configuration,
			// e.g. default configuration
			loggerContext.reset();
			configurator.doConfigure(stream);
		} catch (JoranException e) {
			System.err.println(e.getMessage());
			System.err.println(e.getStackTrace().toString());
			throw new RuntimeException(e);
		}
	}
}
