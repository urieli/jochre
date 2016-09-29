package com.joliciel.jochre.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Scanner;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;

public class JochreProperties {
	private static final Logger LOG = LoggerFactory.getLogger(JochreProperties.class);
	private static JochreProperties instance;
	private Properties properties;

	public static JochreProperties getInstance() {
		if (instance == null) {
			instance = new JochreProperties();
		}
		return instance;
	}

	private JochreProperties() {
		try {
			String jochrePropertiesPath = "/jochre.properties";
			properties = new Properties();
			properties.load(this.getClass().getResourceAsStream(jochrePropertiesPath));
		} catch (IOException e) {
			LOG.error("Failure in JochreProperties$construct", e);
			throw new RuntimeException(e);
		}

	}

	public Properties getProperties() {
		return properties;
	}

	public static String getWelcomeText() {
		try {
			ServletContext servletContext = Executions.getCurrent().getDesktop().getWebApp().getServletContext();
			String welcomeTextPath = servletContext.getRealPath("/resources/welcome.txt");
			LOG.debug("Resource path: " + welcomeTextPath);
			File welcomeTextFile = new File(welcomeTextPath);
			Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(welcomeTextFile), "UTF-8")));

			StringBuilder sb = new StringBuilder();
			while (scanner.hasNextLine()) {
				sb.append(scanner.nextLine() + "\n");
			}
			scanner.close();
			String welcomeText = sb.toString();
			LOG.debug("welcomeText length: " + welcomeText.length());

			return welcomeText;
		} catch (IOException e) {
			LOG.error("Failure in JochreProperties$getWelcomeText", e);
			throw new RuntimeException(e);
		}
	}
}
