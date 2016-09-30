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
package com.joliciel.jochre.search.feedback;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.SearchServiceLocator;
import com.joliciel.jochre.utils.JochreException;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class FeedbackServiceLocator {
	private static final Logger LOG = LoggerFactory.getLogger(FeedbackServiceLocator.class);

	private static FeedbackServiceLocator instance;
	private FeedbackServiceImpl feedbackService;
	private SearchServiceLocator searchServiceLocator;
	private FeedbackDAO feedbackDAO;
	private DataSource dataSource;

	private FeedbackServiceLocator(SearchServiceLocator searchServiceLocator) {
		this.searchServiceLocator = searchServiceLocator;
	}

	public void setDataSource(DataSource dataSource) {
		this.feedbackDAO = new FeedbackDAO(dataSource);
		this.dataSource = dataSource;
		if (this.feedbackService != null)
			this.feedbackService.setFeedbackDAO(feedbackDAO);
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDatabasePropertiesPath(String databasePropertiesPath) {
		if (this.dataSource == null) {
			try {
				File dataSourcePropsFile = new File(databasePropertiesPath);
				if (!dataSourcePropsFile.exists())
					throw new JochreException("Could not find database properties at: " + databasePropertiesPath);
				FileInputStream dataSourceInputStream = new FileInputStream(dataSourcePropsFile);
				Properties dataSourceProperties = new Properties();
				dataSourceProperties.load(dataSourceInputStream);
				this.setDataSourceProperties(dataSourceProperties);
			} catch (IOException e) {
				LOG.error("Failed to load properties from: " + databasePropertiesPath, e);
				throw new RuntimeException(e);
			}
		}
	}

	public void setDataSourceProperties(Properties dataSourceProperties) {
		if (this.dataSource == null) {
			ComboPooledDataSource dataSource = new ComboPooledDataSource();
			try {
				dataSource.setDriverClass(dataSourceProperties.getProperty("jdbc.driverClassName"));
			} catch (PropertyVetoException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

			dataSource.setJdbcUrl(dataSourceProperties.getProperty("jdbc.url"));
			dataSource.setUser(dataSourceProperties.getProperty("jdbc.username"));
			dataSource.setPassword(dataSourceProperties.getProperty("jdbc.password"));
			if (dataSourceProperties.containsKey("jdbc.checkoutTimeout"))
				dataSource.setCheckoutTimeout(Integer.parseInt(dataSourceProperties.getProperty("jdbc.checkoutTimeout")));
			if (dataSourceProperties.containsKey("jdbc.maxPoolSize"))
				dataSource.setMaxPoolSize(Integer.parseInt(dataSourceProperties.getProperty("jdbc.maxPoolSize")));
			if (dataSourceProperties.containsKey("jdbc.minPoolSize"))
				dataSource.setMinPoolSize(Integer.parseInt(dataSourceProperties.getProperty("jdbc.minPoolSize")));
			if (dataSourceProperties.containsKey("jdbc.maxIdleTime"))
				dataSource.setMaxIdleTime(Integer.parseInt(dataSourceProperties.getProperty("jdbc.maxIdleTime")));

			int maxIdleTimeExcessConnections = 600;
			if (dataSourceProperties.containsKey("jdbc.maxIdleTimeExcessConnections"))
				maxIdleTimeExcessConnections = Integer.parseInt(dataSourceProperties.getProperty("jdbc.maxIdleTimeExcessConnections"));
			dataSource.setMaxIdleTimeExcessConnections(maxIdleTimeExcessConnections);

			this.setDataSource(dataSource);
		}
	}

	public static FeedbackServiceLocator getInstance(SearchServiceLocator searchServiceLocator) {
		if (instance == null) {
			instance = new FeedbackServiceLocator(searchServiceLocator);
		}
		return instance;
	}

	public FeedbackService getFeedbackService() {
		if (feedbackService == null) {
			feedbackService = new FeedbackServiceImpl();
			feedbackService.setSearchService(searchServiceLocator.getSearchService());
			feedbackService.setFeedbackDAO(feedbackDAO);
		}
		return feedbackService;
	}
}
