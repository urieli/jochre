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
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.joliciel.talismane.utils.DaoUtils;


class SecurityDaoJdbc implements SecurityDao {
	private static final Log LOG = LogFactory.getLog(SecurityDaoJdbc.class);
	SecurityServiceInternal securityServiceInternal;
	private DataSource dataSource;
	
	private static final String SELECT_USER = "user_id, user_username, user_password" +
		", user_first_name, user_last_name, user_role, user_failed_logins, user_logins";
	private static final String SELECT_PARAM = "param_id, param_last_failed_login" +
		", param_captcha_interval";

	@Override
	public User loadUser(int userId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_USER + " FROM ocr_user WHERE user_id=:user_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("user_id", userId);

		LOG.info(sql);
		logParameters(paramSource);
		User user = null;
		try {
			user = (User)  jt.queryForObject(sql, paramSource, new UserMapper(this.getSecurityServiceInternal()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return user;
	}

	@Override
	public User findUser(String username) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_USER + " FROM ocr_user WHERE user_username=:user_username";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("user_username", username);

		LOG.info(sql);
		logParameters(paramSource);
		User user = null;
		try {
			user = (User)  jt.queryForObject(sql, paramSource, new UserMapper(this.getSecurityServiceInternal()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return user;
	}
	

	@Override
	public List<User> findUsers() {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_USER + " FROM ocr_user" +
				" ORDER BY user_last_name, user_first_name";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		LOG.info(sql);
		logParameters(paramSource);
		List<User> users =  jt.query(sql, paramSource, new UserMapper(this.getSecurityServiceInternal()));
		return users;
	}


	protected static final class UserMapper implements RowMapper<User> {
		private SecurityServiceInternal securityService;

		protected UserMapper(SecurityServiceInternal securityService) {
			this.securityService = securityService;
		}
		
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public User mapRow(SqlRowSet rs) {
			UserInternal user = securityService.getEmptyUser();
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

	@Override
	public void saveUserInternal(UserInternal user) {
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

		if (user.isNew()) {
			sql = "SELECT nextval('ocr_user_id_seq')";
			LOG.info(sql);
			int userId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("user_id", userId);

			sql = "INSERT INTO ocr_user (user_id, user_username, user_password" +
			", user_first_name, user_last_name, user_role, user_failed_logins, user_logins) " +
			"VALUES (:user_id, :user_username, :user_password" +
			", :user_first_name, :user_last_name, :user_role, :user_failed_logins, :user_logins)";

			LOG.info(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			user.setId(userId);
		} else {
			paramSource.addValue("user_id", user.getId());

			sql = "UPDATE ocr_user" +
			" SET user_username = :user_username" +
			", user_password = :user_password" +
			", user_first_name = :user_first_name" +
			", user_last_name = :user_last_name" +
			", user_role = :user_role" +
			", user_failed_logins = :user_failed_logins" +
			", user_logins = :user_logins" +
			" WHERE user_id = :user_id";

			LOG.info(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);
		}

	}
	

	@Override
	public Parameters loadParameters(int parametersId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_PARAM + " FROM ocr_param WHERE param_id=:param_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("param_id", parametersId);

		LOG.info(sql);
		logParameters(paramSource);
		Parameters parameters = null;
		try {
			parameters = (Parameters)  jt.queryForObject(sql, paramSource, new ParametersMapper(this.getSecurityServiceInternal()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return parameters;
	}

	protected static final class ParametersMapper implements RowMapper<Parameters> {
		private SecurityServiceInternal securityService;

		protected ParametersMapper(SecurityServiceInternal securityService) {
			this.securityService = securityService;
		}
		
        public Parameters mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public Parameters mapRow(SqlRowSet rs) {
			ParametersInternal parameters = securityService.getEmptyParameters();
			parameters.setId(rs.getInt("param_id"));
			parameters.setLastFailedLoginAttempt(rs.getDate("param_last_failed_login"));
			parameters.setCaptachaIntervalSeconds(rs.getInt("param_captcha_interval"));
			return parameters;
        }
	}

	@Override
	public void saveParametersInternal(ParametersInternal parameters) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		paramSource.addValue("param_last_failed_login", parameters.getLastFailedLoginAttempt());
		paramSource.addValue("param_captcha_interval", parameters.getCaptachaIntervalSeconds());
		String sql = null;

		paramSource.addValue("param_id", parameters.getId());

		sql = "UPDATE ocr_param" +
			" SET param_last_failed_login = :param_last_failed_login" +
			", param_captcha_interval = :param_captcha_interval" +
			" WHERE param_id = :param_id";

		LOG.info(sql);
		logParameters(paramSource);
		jt.update(sql, paramSource);

	}

	@Override
	public SecurityServiceInternal getSecurityServiceInternal() {
		return securityServiceInternal;
	}

	@Override
	public void setSecurityServiceInternal(
			SecurityServiceInternal securityServiceInternal) {
		this.securityServiceInternal = securityServiceInternal;
	}

	@Override
	public DataSource getDataSource() {
		return dataSource;
	}

	@Override
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

    public static void logParameters(MapSqlParameterSource paramSource) {
       DaoUtils.LogParameters(paramSource.getValues());
    }
}
