package com.joliciel.jochre.search.web;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.utils.JochreLogUtils;
import com.joliciel.talismane.utils.LogUtils;

public class Slf4jListener implements ServletContextListener {
	private static final Logger LOG = LoggerFactory.getLogger(Slf4jListener.class);

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
			try (InputStream stream = context.getResourceAsStream("/WEB-INF/logback.xml")) {
				JochreLogUtils.configureLogging(stream);
			}
			LOG.info("slf4j reloaded");
		} catch (IOException e) {
			System.out.println("Exception reading log configuration");
			System.out.println(LogUtils.getErrorString(e));
			System.out.flush();
		}
	}
}
