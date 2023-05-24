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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.typesafe.config.Config;
import com.zaxxer.hikari.HikariDataSource;

/**
 * For configurating a datasource from a TypeSafe.Config
 * 
 * @author Assaf Urieli
 *
 */
public class DaoConfig {
  public static Map<String, DataSource> dataSources = new HashMap<>();

  /**
   * Return a unique key defining the datasource in this config.
   *
   */
  public static String getKey(Config jdbcConfig) {

    if (!jdbcConfig.hasPath("url"))
      return null;
    String driverClass = jdbcConfig.getString("driver-class-name");
    String url = jdbcConfig.getString("url");
    String user = jdbcConfig.getString("username");
    String key = driverClass + "|" + url + "|" + user;
    return key;
  }

  /**
   * Get a datasource from the jochre.jdbc key in the configuration file.
   */
  public static DataSource getDataSource(Config jdbcConfig) {
    String key = getKey(jdbcConfig);
    if (key == null)
      return null;

    if (dataSources.containsKey(key))
      return dataSources.get(key);

    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setDriverClassName(jdbcConfig.getString("driver-class-name"));
    dataSource.setJdbcUrl(jdbcConfig.getString("url"));
    dataSource.setUsername(jdbcConfig.getString("username"));
    dataSource.setPassword(jdbcConfig.getString("password"));
    dataSource.setConnectionTimeout(jdbcConfig.getDuration("checkout-timeout").toMillis());
    dataSource.setMaximumPoolSize(jdbcConfig.getInt("max-pool-size"));
    dataSource.setIdleTimeout(jdbcConfig.getDuration("idle-timeout").toMillis());
    dataSource.setMinimumIdle(jdbcConfig.getInt("min-idle"));
    dataSource.setMaxLifetime(jdbcConfig.getDuration("max-lifetime").toMillis());
    dataSource.setPoolName("HikariPool-" + key);

    dataSources.put(key, dataSource);
    return dataSource;
  }
}
