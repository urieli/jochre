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
package com.joliciel.jochre.boundaries;

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
import com.joliciel.jochre.boundaries.Split;
import com.joliciel.jochre.boundaries.SplitInternal;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.utils.DaoUtils;

final class BoundaryDaoJdbc implements BoundaryDao {
	private static final Log LOG = LogFactory.getLog(BoundaryDaoJdbc.class);
	BoundaryServiceInternal boundaryServiceInternal;
	private DataSource dataSource;

	private static final String SELECT_SPLIT = "split_id, split_shape_id, split_position";

	@Override
	public BoundaryServiceInternal getBoundaryServiceInternal() {
		return boundaryServiceInternal;
	}

	@Override
	public void setBoundaryServiceInternal(
			BoundaryServiceInternal boundaryServiceInternal) {
		this.boundaryServiceInternal = boundaryServiceInternal;
	}

	@Override
	public DataSource getDataSource() {
		return dataSource;
	}

	@Override
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}



	@Override
	public Split loadSplit(int splitId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_SPLIT + " FROM ocr_split WHERE split_id=:split_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("split_id", splitId);

		LOG.debug(sql);
		logParameters(paramSource);
		Split split = null;
		try {
			split = (Split)  jt.queryForObject(sql, paramSource, new SplitMapper(this.getBoundaryServiceInternal()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return split;
	}

	@Override
	public List<Split> findSplits(Shape shape) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_SPLIT + " FROM ocr_split WHERE split_shape_id=:split_shape_id" +
		" ORDER BY split_position";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("split_shape_id", shape.getId());

		LOG.debug(sql);
		logParameters(paramSource);
		@SuppressWarnings("unchecked")
		List<Split> splits = jt.query(sql, paramSource, new SplitMapper(this.getBoundaryServiceInternal()));

		return splits;
	}
	
	protected static final class SplitMapper implements RowMapper {
		private BoundaryServiceInternal boundaryService;

		protected SplitMapper(BoundaryServiceInternal boundaryService) {
			this.boundaryService = boundaryService;
		}
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			SplitInternal split = boundaryService.getEmptySplitInternal();
			// split_id, split_top, split_left, split_bottom, split_right
			// split_cap_line, split_mean_line, split_base_line, split_pixels, split_letter, split_group_id, split_index
			split.setId(rs.getInt("split_id"));
			split.setShapeId(rs.getInt("split_shape_id"));
			split.setPosition(rs.getInt("split_position"));

			split.setDirty(false);
			return split;
		}
	}
	
	@Override
	public void saveSplit(Split split) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		SplitInternal iSplit = (SplitInternal) split;

		paramSource.addValue("split_shape_id", split.getShapeId());
		paramSource.addValue("split_position", split.getPosition());
		String sql = null;

		if (split.isNew()) {
			sql = "SELECT nextval('ocr_split_id_seq')";
			LOG.debug(sql);
			int splitId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("split_id", splitId);

			sql = "INSERT INTO ocr_split (split_id, split_shape_id, split_position) " +
			"VALUES (:split_id, :split_shape_id, :split_position)";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			iSplit.setId(splitId);
		} else {
			paramSource.addValue("split_id", split.getId());

			sql = "UPDATE ocr_split" +
			" SET split_shape_id = :split_shape_id" +
			", split_position = :split_position" +
			" WHERE split_id = :split_id";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);
		}
	}
	@Override
	public void deleteSplit(Split split) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("split_id", split.getId());
		String sql = null;
		
		sql = "delete from ocr_split where split_id = :split_id";

		LOG.debug(sql);
		logParameters(paramSource);
		jt.update(sql, paramSource);
	}
	
	
    @SuppressWarnings("unchecked")
    public static void logParameters(MapSqlParameterSource paramSource) {
       DaoUtils.LogParameters(paramSource.getValues());
    }
}
