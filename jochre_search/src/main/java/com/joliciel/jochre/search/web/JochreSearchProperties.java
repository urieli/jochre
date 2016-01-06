///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
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
package com.joliciel.jochre.search.web;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.utils.LogUtils;

/**
 * Any properties read from the config file.
 * @author Assaf Urieli
 *
 */
public class JochreSearchProperties {
	private static final Log LOG = LogFactory.getLog(JochreSearchProperties.class);
	private static JochreSearchProperties instance;
	private Properties properties;
	@SuppressWarnings("unused")
	private ServletContext servletContext;
	
	public static JochreSearchProperties getInstance(ServletContext servletContext) {
		if (instance==null) {
			instance = new JochreSearchProperties(servletContext);
		}
		return instance;
	}
	
	public static void purgeInstance() {
		LOG.info("purgeInstance");
		instance = null;
	}
	
	private JochreSearchProperties(ServletContext servletContext) {
		try {
			this.servletContext = servletContext;
			String cfhPropertiesPath = "/WEB-INF/jochre.properties";
			String realPath = servletContext.getRealPath(cfhPropertiesPath);
			LOG.info("Loading new JochreSearchProperties from " + realPath);
			properties = new Properties();
			FileInputStream inputStream = new FileInputStream(realPath);
			properties.load(inputStream);
			for (Object keyObj : properties.keySet()) {
				String key = (String) keyObj;
				LOG.info(key + "=" + properties.getProperty(key));
			}
		
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new JochreException(ioe);
		}
	
	}

	public Properties getProperties() {
		return properties;
	}
	
	public String getIndexDirPath() {
		return this.properties.getProperty("index.dir");	
	}
	
	public String getContentDirPath() {
		return this.properties.getProperty("content.dir");	
	}

}
