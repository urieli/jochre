///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Assaf Urieli
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
package com.joliciel.jochre.utils.dao;

import java.beans.PropertyVetoException;

import javax.sql.DataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.typesafe.config.Config;

/**
 * For configurating a datasource from a TypeSafe.Config
 * 
 * @author Assaf Urieli
 *
 */
public class DaoConfig {

	/**
	 * Get a datasource from the jochre.jdbc key in the configuration file.
	 */
	public static DataSource getDataSource(Config config) {
		Config dataSourceConfig = config.getConfig("jochre.jdbc");
		ComboPooledDataSource dataSource = new ComboPooledDataSource();
		try {
			dataSource.setDriverClass(dataSourceConfig.getString("driver-class-name"));
		} catch (PropertyVetoException e) {
			throw new RuntimeException(e);
		}

		dataSource.setJdbcUrl(dataSourceConfig.getString("url"));
		dataSource.setUser(dataSourceConfig.getString("username"));
		dataSource.setPassword(dataSourceConfig.getString("password"));
		if (dataSourceConfig.hasPath("checkout-timeout")) {
			dataSource.setCheckoutTimeout(dataSourceConfig.getInt("checkout-timeout"));
		}
		if (dataSourceConfig.hasPath("max-pool-size")) {
			dataSource.setMaxPoolSize(dataSourceConfig.getInt("max-pool-size"));
		}
		if (dataSourceConfig.hasPath("min-pool-size")) {
			dataSource.setMinPoolSize(dataSourceConfig.getInt("min-pool-size"));
		}
		if (dataSourceConfig.hasPath("max-idle-time")) {
			dataSource.setMaxIdleTime(dataSourceConfig.getInt("max-idle-time"));
		}

		int maxIdleTimeExcessConnections = dataSourceConfig.getInt("max-idle-time-excess-connections");
		dataSource.setMaxIdleTimeExcessConnections(maxIdleTimeExcessConnections);
		return dataSource;
	}
}
