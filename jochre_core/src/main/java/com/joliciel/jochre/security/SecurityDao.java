///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.jochre.security;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.joliciel.jochre.EntityNotFoundException;
import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.utils.dao.DaoConfig;
import com.joliciel.talismane.utils.DaoUtils;

public class SecurityDao {
	private static final Logger LOG = LoggerFactory.getLogger(SecurityDao.class);
	private final DataSource dataSource;

	private final JochreSession jochreSession;

	public static Map<String, SecurityDao> instances = new HashMap<>();

	public static SecurityDao getInstance(JochreSession jochreSession) {
		String key = DaoConfig.getKey(jochreSession.getConfig().getConfig("jochre.jdbc"));
		SecurityDao instance = instances.get(key);
		if (instance == null) {
			instance = new SecurityDao(jochreSession);
			instances.put(key, instance);
		}
		return instance;
	}

	private SecurityDao(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.dataSource = DaoConfig.getDataSource(jochreSession.getConfig().getConfig("jochre.jdbc"));
	}

	private static final String SELECT_USER = "user_id, user_username, user_password"
			+ ", user_first_name, user_last_name, user_role, user_failed_logins, user_logins";
	private static final String SELECT_PARAM = "param_id, param_last_failed_login" + ", param_captcha_interval";

	public User loadUser(int userId) {
		User user = this.jochreSession.getObjectCache().getEntity(User.class, userId);
		if (user == null) {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
			String sql = "SELECT " + SELECT_USER + " FROM ocr_user WHERE user_id=:user_id";
			MapSqlParameterSource paramSource = new MapSqlParameterSource();
			paramSource.addValue("user_id", userId);

			LOG.info(sql);
			logParameters(paramSource);
			try {
				user = jt.queryForObject(sql, paramSource, new UserMapper());
			} catch (EmptyResultDataAccessException ex) {
				throw new EntityNotFoundException("No User found for user id " + userId);
			}

			this.jochreSession.getObjectCache().putEntity(User.class, userId, user);
		}
		return user;

	}

	public User findUser(String username) {
		User user = this.jochreSession.getObjectCache().getEntity(User.class, username);
		if (user == null) {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
			String sql = "SELECT " + SELECT_USER + " FROM ocr_user WHERE user_username=:user_username";
			MapSqlParameterSource paramSource = new MapSqlParameterSource();
			paramSource.addValue("user_username", username);

			LOG.info(sql);
			logParameters(paramSource);

			try {
				user = jt.queryForObject(sql, paramSource, new UserMapper());
			} catch (EmptyResultDataAccessException ex) {
				throw new EntityNotFoundException("No User found for username " + username);
			}

			this.jochreSession.getObjectCache().putEntity(User.class, username, user);
		}
		return user;
	}

	/**
	 * Return all users in the system.
	 */
	public List<User> findUsers() {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_USER + " FROM ocr_user" + " ORDER BY user_last_name, user_first_name";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		LOG.info(sql);
		logParameters(paramSource);
		List<User> users = jt.query(sql, paramSource, new UserMapper());
		return users;
	}

	private final class UserMapper implements RowMapper<User> {
		UserMapper() {
		}

		@Override
		public User mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public User mapRow(SqlRowSet rs) {
			User user = new User(jochreSession);
			user.setId(rs.getInt("user_id"));
			user.setUsername(rs.getString("user_username"));
			user.setPassword(rs.getString("user_password"));
			user.setFirstName(rs.getString("user_first_name"));
			user.setLastName(rs.getString("user_last_name"));
			user.setRole(UserRole.forId(rs.getInt("user_role")));
			user.setFailedLoginCount(rs.getInt("user_failed_logins"));
			user.setLoginCount(rs.getInt("user_logins"));
			return user;
		}
	}

	void saveUserInternal(User user) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		paramSource.addValue("user_username", user.getUsername());
		paramSource.addValue("user_password", user.getPassword());
		paramSource.addValue("user_first_name", user.getFirstName());
		paramSource.addValue("user_last_name", user.getLastName());
		paramSource.addValue("user_role", user.getRole().getId());
		paramSource.addValue("user_failed_logins", user.getFailedLoginCount());
		paramSource.addValue("user_logins", user.getLoginCount());
		String sql = null;

		if (user.getId() == 0) {
			sql = "SELECT nextval('ocr_user_id_seq')";
			LOG.info(sql);
			int userId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("user_id", userId);

			sql = "INSERT INTO ocr_user (user_id, user_username, user_password"
					+ ", user_first_name, user_last_name, user_role, user_failed_logins, user_logins) " + "VALUES (:user_id, :user_username, :user_password"
					+ ", :user_first_name, :user_last_name, :user_role, :user_failed_logins, :user_logins)";

			LOG.info(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			user.setId(userId);
		} else {
			paramSource.addValue("user_id", user.getId());

			sql = "UPDATE ocr_user" + " SET user_username = :user_username" + ", user_password = :user_password" + ", user_first_name = :user_first_name"
					+ ", user_last_name = :user_last_name" + ", user_role = :user_role" + ", user_failed_logins = :user_failed_logins"
					+ ", user_logins = :user_logins" + " WHERE user_id = :user_id";

			LOG.info(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);
		}

	}

	public Parameters loadParameters() {
		int parametersId = 1;
		Parameters parameters = this.jochreSession.getObjectCache().getEntity(Parameters.class, parametersId);
		if (parameters == null) {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
			String sql = "SELECT " + SELECT_PARAM + " FROM ocr_param WHERE param_id=:param_id";
			MapSqlParameterSource paramSource = new MapSqlParameterSource();
			paramSource.addValue("param_id", parametersId);

			LOG.info(sql);
			logParameters(paramSource);

			try {
				parameters = jt.queryForObject(sql, paramSource, new ParametersMapper());
			} catch (EmptyResultDataAccessException ex) {
				throw new EntityNotFoundException("No Parameters found for parameters id " + parametersId);
			}

			this.jochreSession.getObjectCache().putEntity(Parameters.class, parametersId, parameters);
		}
		return parameters;

	}

	private final class ParametersMapper implements RowMapper<Parameters> {
		protected ParametersMapper() {
		}

		@Override
		public Parameters mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public Parameters mapRow(SqlRowSet rs) {
			Parameters parameters = new Parameters(jochreSession);
			parameters.setId(rs.getInt("param_id"));
			parameters.setLastFailedLoginAttempt(rs.getDate("param_last_failed_login"));
			parameters.setCaptachaIntervalSeconds(rs.getInt("param_captcha_interval"));
			return parameters;
		}
	}

	void saveParametersInternal(Parameters parameters) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		paramSource.addValue("param_last_failed_login", parameters.getLastFailedLoginAttempt());
		paramSource.addValue("param_captcha_interval", parameters.getCaptachaIntervalSeconds());
		String sql = null;

		paramSource.addValue("param_id", parameters.getId());

		sql = "UPDATE ocr_param" + " SET param_last_failed_login = :param_last_failed_login" + ", param_captcha_interval = :param_captcha_interval"
				+ " WHERE param_id = :param_id";

		LOG.info(sql);
		logParameters(paramSource);
		jt.update(sql, paramSource);

	}

	private DataSource getDataSource() {
		return dataSource;
	}

	private static void logParameters(MapSqlParameterSource paramSource) {
		DaoUtils.LogParameters(paramSource.getValues());
	}
}
