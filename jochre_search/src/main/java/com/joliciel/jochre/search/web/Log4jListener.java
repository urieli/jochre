package com.joliciel.jochre.search.web;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.joliciel.talismane.utils.LogUtils;

public class Log4jListener implements ServletContextListener {
	private static final Log LOG = LogFactory.getLog(Log4jListener.class);

	@Override
	public void contextDestroyed(ServletContextEvent event) {
	}
	
	@Override
	public void contextInitialized(ServletContextEvent event) {
		System.out.println("Log4jListener.contextInitialized");
		System.out.flush();
		ServletContext context = event.getServletContext();
		reloadLog4jProperties(context);
	}

	public static void reloadLog4jProperties(ServletContext context) {
		try {
			Properties log4jProperties = new Properties();
			log4jProperties.load(context.getResourceAsStream("/WEB-INF/log4j.properties"));
			PropertyConfigurator.configure(log4jProperties);
			LOG.info("log4j reloaded");
		} catch (IOException e) {
			System.out.println("Exception reading log4j.properties");
			System.out.println(LogUtils.getErrorString(e));
			System.out.flush();
		}
	}
}
