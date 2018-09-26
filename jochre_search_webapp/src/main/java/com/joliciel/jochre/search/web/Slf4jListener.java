package com.joliciel.jochre.search.web;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.LoggerFactory;

import com.joliciel.talismane.utils.LogUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class Slf4jListener implements ServletContextListener {
	@Override
	public void contextDestroyed(ServletContextEvent event) {
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		System.out.println("Slf4jListener.contextInitialized");
		System.out.flush();
		ServletContext context = event.getServletContext();
		reloadLogger(context);
	}

	public static void reloadLogger(ServletContext context) {
		Config config = ConfigFactory.load();
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		String logFileLocation = config.getString("jochre.search.webapp.logging");
		File logFile = new File(logFileLocation);
		try {
			if (logFile.exists()) {
				JoranConfigurator configurator = new JoranConfigurator();
				configurator.setContext(loggerContext);
				// Call context.reset() to clear any previous configuration,
				// e.g. default configuration
				loggerContext.reset();
				configurator.doConfigure(logFile);
			}
		} catch (JoranException e) {
			System.out.println("Exception reading log configuration from: " + logFileLocation);
			System.out.println(LogUtils.getErrorString(e));
			System.out.flush();
		}
	}
}
