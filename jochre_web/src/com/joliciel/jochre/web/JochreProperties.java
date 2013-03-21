package com.joliciel.jochre.web;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zkoss.zk.ui.Executions;

import com.joliciel.jochre.lexicon.LocaleSpecificLexiconService;
import com.joliciel.talismane.utils.LogUtils;

public class JochreProperties {
	private static final Log LOG = LogFactory.getLog(JochreProperties.class);
	private static JochreProperties instance;
	private Properties properties;
	private LocaleSpecificLexiconService lexiconService;
	
	public static JochreProperties getInstance() {
		if (instance==null) {
			instance = new JochreProperties();
		}
		return instance;
	}
	
	private JochreProperties() {
		try {
			String jochrePropertiesPath = "/jochre.properties";
			properties = new Properties();
			properties.load(this.getClass().getResourceAsStream(jochrePropertiesPath));
		
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	
	}

	public Properties getProperties() {
		return properties;
	}
	
	public LocaleSpecificLexiconService getLexiconService() {
		try {
			if (lexiconService==null) {
				ServletContext servletContext = (ServletContext) Executions.getCurrent().getDesktop().getWebApp().getServletContext();
				
				String lexiconServiceClassName = this.properties.getProperty("lexiconService");
				LOG.debug("lexiconServiceClassName: " + lexiconServiceClassName);
				@SuppressWarnings("rawtypes")
				Class lexiconServiceClass = Class.forName(lexiconServiceClassName);
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Constructor constructor =
					lexiconServiceClass.getConstructor(new Class[]{});
				lexiconService = (LocaleSpecificLexiconService) constructor.newInstance();
		
				String lexiconDirPath = this.properties.getProperty("lexiconDirPath");
				LOG.debug("lexiconDirPath: " + lexiconDirPath);
				String lexiconDirRealPath = servletContext.getRealPath(lexiconDirPath);
		
				lexiconService.setLexiconPath(lexiconDirRealPath);	
			}
			return lexiconService;
		} catch (ClassNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} 
	}
	
	public Locale getLocale() {
		return this.getLexiconService().getLocale();
	}
	
	public File getLetterModelFile() {
		ServletContext servletContext = (ServletContext) Executions.getCurrent().getDesktop().getWebApp().getServletContext();
		String letterModelPath = properties.getProperty("letterModelPath");
		LOG.debug("letterModelPath: " + letterModelPath);
		File letterModelFile = null;
		if (letterModelPath!=null) {
			String letterModelRealPath = servletContext.getRealPath(letterModelPath);
			letterModelFile = new File(letterModelRealPath);
		}
		return letterModelFile;
	}
	
	public File getSplitModelFile() {
		ServletContext servletContext = (ServletContext) Executions.getCurrent().getDesktop().getWebApp().getServletContext();
		String splitModelPath = properties.getProperty("splitModelPath");
		File splitModelFile = null;
		LOG.debug("splitModelPath: " + splitModelPath);
		if (splitModelPath!=null) {
			String splitModelRealPath = servletContext.getRealPath(splitModelPath);
			splitModelFile = new File(splitModelRealPath);
		}
		return splitModelFile;
	}
	
	public File getMergeModelFile() {
		ServletContext servletContext = (ServletContext) Executions.getCurrent().getDesktop().getWebApp().getServletContext();
		String mergeModelPath = properties.getProperty("mergeModelPath");
		LOG.debug("mergeModelPath: " + mergeModelPath);
		File mergeModelFile = null;
		if (mergeModelPath!=null) {
			String mergeModelRealPath = servletContext.getRealPath(mergeModelPath);
			mergeModelFile = new File(mergeModelRealPath);
		}
		return mergeModelFile;
	}
	
	public String getWelcomeText() {
		try {
			ServletContext servletContext = (ServletContext) Executions.getCurrent().getDesktop().getWebApp().getServletContext();
			String welcomeTextPath = servletContext.getRealPath("/resources/welcome.txt");
			LOG.debug("Resource path: " + welcomeTextPath);
			File welcomeTextFile = new File(welcomeTextPath);
			Scanner scanner = new Scanner(welcomeTextFile, "UTF-8");
			StringBuilder sb = new StringBuilder();
			while (scanner.hasNextLine()) {
				sb.append(scanner.nextLine() + "\n");
			}
			String welcomeText = sb.toString();
			LOG.debug("welcomeText length: " + welcomeText.length());

			return welcomeText;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
}
