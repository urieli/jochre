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
package com.joliciel.jochre.graphics;

import java.awt.image.BufferedImage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.jochre.utils.dao.ImageUtils;
import com.joliciel.talismane.utils.DaoUtils;

final class GraphicsDaoJdbc implements GraphicsDao {
	private static final Logger LOG = LoggerFactory.getLogger(GraphicsDaoJdbc.class);
	GraphicsServiceInternal graphicsServiceInternal;
	private DataSource dataSource;

	private static final String SELECT_IMAGE = "image_id, image_name, image_width, image_height, image_black_threshold"
			+ ", image_page_id, image_index, image_sep_threshold, image_black_limit, image_white_limit"
			+ ", image_white_gap_fill_factor, image_imgstatus_id, image_owner_id";
	private static final String SELECT_PARAGRAPH = "paragraph_id, paragraph_image_id, paragraph_index";
	private static final String SELECT_ROW = "row_id, row_paragraph_id, row_index, row_image, row_height";
	private static final String SELECT_GROUP = "group_id, group_row_id, group_index, group_hard_hyphen"
			+ ", group_broken_word, group_segment_problem, group_skip";
	private static final String SELECT_SHAPE = "shape_id, shape_top, shape_left, shape_bottom, shape_right"
			+ ", shape_cap_line, shape_mean_line, shape_base_line, shape_pixels, shape_letter, shape_group_id, shape_index" + ", shape_original_guess";

	@Override
	public Shape loadShape(int shapeId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_SHAPE + " FROM ocr_shape WHERE shape_id=:shape_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("shape_id", shapeId);

		LOG.debug(sql);
		logParameters(paramSource);
		Shape shape = null;
		try {
			shape = jt.queryForObject(sql, paramSource, new ShapeMapper(this.getGraphicsServiceInternal()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return shape;
	}

	@Override
	public List<Shape> findShapes(GroupOfShapes group) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_SHAPE + " FROM ocr_shape WHERE shape_group_id=:shape_group_id" + " ORDER BY shape_index";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("shape_group_id", group.getId());

		LOG.debug(sql);
		logParameters(paramSource);

		List<Shape> shapes = jt.query(sql, paramSource, new ShapeMapper(this.getGraphicsServiceInternal()));

		return shapes;
	}

	@Override
	public List<Shape> findShapes(RowOfShapes row) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_SHAPE + " FROM ocr_shape" + " INNER JOIN ocr_group ON shape_group_id = group_id" + " WHERE group_row_id = :group_row_id"
				+ " ORDER BY group_index, shape_index";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("group_row_id", row.getId());

		LOG.debug(sql);
		logParameters(paramSource);

		List<Shape> shapes = jt.query(sql, paramSource, new ShapeMapper(this.getGraphicsServiceInternal()));

		return shapes;
	}

	@Override
	public List<Shape> findShapesToSplit(Locale locale) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_SHAPE + ", count(split_id) as the_count FROM ocr_shape" + " LEFT JOIN ocr_split on shape_id = split_shape_id"
				+ " LEFT JOIN ocr_group ON shape_group_id = group_id" + " LEFT JOIN ocr_row ON group_row_id = row_id"
				+ " LEFT JOIN ocr_paragraph ON row_paragraph_id = paragraph_id" + " LEFT JOIN ocr_image ON paragraph_image_id = image_id"
				+ " WHERE length(shape_letter)>1" + " AND shape_letter not like '%|'" + " AND shape_letter not like '|%'"
				+ " AND shape_letter not in (:dual_character_letters)" + " AND image_imgstatus_id in (:image_imgstatus_id)" + " GROUP BY " + SELECT_SHAPE
				+ " ORDER BY the_count, shape_letter, shape_id";

		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		Linguistics linguistics = JochreSession.getInstance().getLinguistics();

		paramSource.addValue("dual_character_letters", linguistics.getDualCharacterLetters());
		List<Integer> imageStatusList = new ArrayList<Integer>();
		imageStatusList.add(ImageStatus.TRAINING_VALIDATED.getId());
		imageStatusList.add(ImageStatus.TRAINING_HELD_OUT.getId());
		imageStatusList.add(ImageStatus.TRAINING_TEST.getId());
		paramSource.addValue("image_imgstatus_id", imageStatusList);

		LOG.debug(sql);
		logParameters(paramSource);
		List<Shape> shapes = jt.query(sql, paramSource, new ShapeMapper(this.getGraphicsServiceInternal()));

		return shapes;
	}

	protected static final class ShapeMapper implements RowMapper<Shape> {
		private GraphicsServiceInternal graphicsService;

		protected ShapeMapper(GraphicsServiceInternal graphicsService) {
			this.graphicsService = graphicsService;
		}

		@Override
		public Shape mapRow(ResultSet rs, int rowNum) throws SQLException {
			ShapeInternal shape = graphicsService.getEmptyShapeInternal();
			// shape_id, shape_top, shape_left, shape_bottom, shape_right
			// shape_cap_line, shape_mean_line, shape_base_line, shape_pixels,
			// shape_letter, shape_group_id, shape_index
			shape.setId(rs.getInt("shape_id"));
			shape.setTop(rs.getInt("shape_top"));
			shape.setLeft(rs.getInt("shape_left"));
			shape.setBottom(rs.getInt("shape_bottom"));
			shape.setRight(rs.getInt("shape_right"));
			shape.setCapLine(rs.getInt("shape_cap_line"));
			shape.setMeanLine(rs.getInt("shape_mean_line"));
			shape.setBaseLine(rs.getInt("shape_base_line"));
			shape.setIndex(rs.getInt("shape_index"));
			shape.setGroupId(rs.getInt("shape_group_id"));

			BufferedImage image = ImageUtils.getImage(rs, "shape_pixels");
			if (image != null)
				shape.setImage(image);

			shape.setLetter(rs.getString("shape_letter"));

			shape.setOriginalGuess(rs.getString("shape_original_guess"));

			shape.setDirty(false);
			return shape;
		}
	}

	@Override
	public JochreImage loadJochreImage(int imageId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_IMAGE + " FROM ocr_image WHERE image_id=:image_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("image_id", imageId);

		LOG.debug(sql);
		logParameters(paramSource);
		JochreImage image = null;
		try {
			image = jt.queryForObject(sql, paramSource, new JochreImageMapper(this.getGraphicsServiceInternal()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return image;
	}

	@Override
	public void loadOriginalImage(JochreImageInternal jochreImage) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT image_image FROM ocr_image WHERE image_id=:image_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("image_id", jochreImage.getId());

		LOG.debug(sql);
		logParameters(paramSource);

		BufferedImage image = ImageUtils.getImage(jt, sql, paramSource, "image_image");
		jochreImage.setOriginalImageDB(image);
	}

	@Override
	public void saveOriginalImage(JochreImage jochreImage) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("image_id", jochreImage.getId());

		ImageUtils.storeImage(paramSource, "image_image", jochreImage.getOriginalImage());

		String sql = "UPDATE ocr_image SET image_image = :image_image" + " WHERE image_id = :image_id";

		LOG.debug(sql);
		logParameters(paramSource);
		jt.update(sql, paramSource);
	}

	@Override
	public List<JochreImage> findImages(JochrePage jochrePage) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_IMAGE + " FROM ocr_image WHERE image_page_id=:image_page_id" + " ORDER BY image_index";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("image_page_id", jochrePage.getId());

		LOG.debug(sql);
		logParameters(paramSource);
		List<JochreImage> images = jt.query(sql, paramSource, new JochreImageMapper(this.getGraphicsServiceInternal()));

		return images;
	}

	@Override
	public List<JochreImage> findImages(ImageStatus[] imageStatuses) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_IMAGE + " FROM ocr_image" + " INNER JOIN ocr_page ON page_id=image_page_id"
				+ " WHERE image_imgstatus_id in (:image_imgstatus_id)" + " ORDER BY page_doc_id, page_index, image_index, image_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		List<Integer> imageStatusList = new ArrayList<Integer>();
		for (ImageStatus imageStatus : imageStatuses)
			imageStatusList.add(imageStatus.getId());
		paramSource.addValue("image_imgstatus_id", imageStatusList);

		LOG.debug(sql);
		logParameters(paramSource);
		List<JochreImage> images = jt.query(sql, paramSource, new JochreImageMapper(this.getGraphicsServiceInternal()));

		return images;
	}

	protected static final class JochreImageMapper implements RowMapper<JochreImage> {
		private GraphicsServiceInternal graphicsService;

		protected JochreImageMapper(GraphicsServiceInternal graphicsService) {
			this.graphicsService = graphicsService;
		}

		@Override
		public JochreImage mapRow(ResultSet rs, int rowNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public JochreImage mapRow(SqlRowSet rs) {
			JochreImageInternal image = graphicsService.getEmptyJochreImageInternal();
			// image_id, image_name, image_width, image_height, image_black_threshold
			image.setId(rs.getInt("image_id"));
			image.setName(rs.getString("image_name"));
			image.setWidth(rs.getInt("image_width"));
			image.setHeight(rs.getInt("image_height"));
			image.setBlackThreshold(rs.getInt("image_black_threshold"));
			image.setSeparationThreshold(rs.getInt("image_sep_threshold"));
			image.setBlackLimit(rs.getInt("image_black_limit"));
			image.setWhiteLimit(rs.getInt("image_white_limit"));
			image.setWhiteGapFillFactor(rs.getInt("image_white_gap_fill_factor"));
			image.setPageId(rs.getInt("image_page_id"));
			image.setIndex(rs.getInt("image_index"));

			if (rs.getObject("image_owner_id") != null)
				image.setOwnerId(rs.getInt("image_owner_id"));

			image.setImageStatus(ImageStatus.forId(rs.getInt("image_imgstatus_id")));

			return image;
		}
	}

	@Override
	public RowOfShapes loadRowOfShapes(int rowId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_ROW + " FROM ocr_row WHERE row_id=:row_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("row_id", rowId);

		LOG.debug(sql);
		logParameters(paramSource);
		RowOfShapes row = null;
		try {
			row = jt.queryForObject(sql, paramSource, new RowOfShapesMapper(this.getGraphicsServiceInternal()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return row;
	}

	@Override
	public List<RowOfShapes> findRows(Paragraph paragraph) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_ROW + " FROM ocr_row WHERE row_paragraph_id=:row_paragraph_id" + " ORDER BY row_index";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("row_paragraph_id", paragraph.getId());

		LOG.debug(sql);
		logParameters(paramSource);

		List<RowOfShapes> rows = jt.query(sql, paramSource, new RowOfShapesMapper(this.getGraphicsServiceInternal()));

		return rows;
	}

	protected static final class RowOfShapesMapper implements RowMapper<RowOfShapes> {
		private GraphicsServiceInternal graphicsService;

		protected RowOfShapesMapper(GraphicsServiceInternal graphicsService) {
			this.graphicsService = graphicsService;
		}

		@Override
		public RowOfShapes mapRow(ResultSet rs, int rowNum) throws SQLException {
			RowOfShapesInternal row = graphicsService.getEmptyRowOfShapesInternal();
			// row_id, row_image_id, row_index
			row.setId(rs.getInt("row_id"));
			row.setParagraphId(rs.getInt("row_paragraph_id"));
			row.setIndex(rs.getInt("row_index"));
			row.setXHeight(rs.getInt("row_height"));

			BufferedImage image = ImageUtils.getImage(rs, "row_image");
			if (image != null)
				row.setImage(image);

			return row;
		}
	}

	@Override
	public GroupOfShapes loadGroupOfShapes(int groupId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_GROUP + " FROM ocr_group WHERE group_id=:group_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("group_id", groupId);

		LOG.debug(sql);
		logParameters(paramSource);
		GroupOfShapes group = null;
		try {
			group = jt.queryForObject(sql, paramSource, new GroupOfShapesMapper(this.getGraphicsServiceInternal()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return group;
	}

	@Override
	public List<GroupOfShapes> findGroups(RowOfShapes row) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_GROUP + " FROM ocr_group WHERE group_row_id=:group_row_id" + " ORDER BY group_index";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("group_row_id", row.getId());

		LOG.debug(sql);
		logParameters(paramSource);

		List<GroupOfShapes> groups = jt.query(sql, paramSource, new GroupOfShapesMapper(this.getGraphicsServiceInternal()));

		return groups;
	}

	@Override
	public List<GroupOfShapes> findGroupsForMerge() {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_GROUP + " FROM ocr_group WHERE group_id IN" + " (SELECT shape_group_id FROM ocr_shape WHERE shape_letter LIKE '%|%')"
				+ " ORDER BY group_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		LOG.debug(sql);
		logParameters(paramSource);

		List<GroupOfShapes> groups = jt.query(sql, paramSource, new GroupOfShapesMapper(this.getGraphicsServiceInternal()));

		return groups;
	}

	protected static final class GroupOfShapesMapper implements RowMapper<GroupOfShapes> {
		private GraphicsServiceInternal graphicsService;

		protected GroupOfShapesMapper(GraphicsServiceInternal graphicsService) {
			this.graphicsService = graphicsService;
		}

		@Override
		public GroupOfShapes mapRow(ResultSet rs, int groupNum) throws SQLException {
			return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
		}

		public GroupOfShapes mapRow(SqlRowSet rs) {
			GroupOfShapesInternal group = graphicsService.getEmptyGroupOfShapesInternal();
			// group_id, group_row_id, group_index
			group.setId(rs.getInt("group_id"));
			group.setRowId(rs.getInt("group_row_id"));
			group.setIndex(rs.getInt("group_index"));
			group.setHardHyphen(rs.getBoolean("group_hard_hyphen"));
			group.setBrokenWord(rs.getBoolean("group_broken_word"));
			group.setSegmentationProblem(rs.getBoolean("group_segment_problem"));
			group.setSkip(rs.getBoolean("group_skip"));

			return group;
		}
	}

	@Override
	public void saveShape(Shape shape) {
		// note: update will not update the pixels (not strictly required).
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		ShapeInternal iShape = (ShapeInternal) shape;

		paramSource.addValue("shape_top", shape.getTop());
		paramSource.addValue("shape_left", shape.getLeft());
		paramSource.addValue("shape_bottom", shape.getBottom());
		paramSource.addValue("shape_right", shape.getRight());
		paramSource.addValue("shape_cap_line", shape.getCapLine());
		paramSource.addValue("shape_mean_line", shape.getMeanLine());
		paramSource.addValue("shape_base_line", shape.getBaseLine());
		paramSource.addValue("shape_letter", shape.getLetter());
		paramSource.addValue("shape_original_guess", shape.getOriginalGuess());
		paramSource.addValue("shape_group_id", shape.getGroupId());
		paramSource.addValue("shape_index", shape.getIndex());
		String sql = null;

		if (shape.getId() == 0) {
			sql = "SELECT nextval('ocr_shape_id_seq')";
			LOG.debug(sql);
			int shapeId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("shape_id", shapeId);

			ImageUtils.storeImage(paramSource, "shape_pixels", shape.getImage());

			sql = "INSERT INTO ocr_shape (shape_id, shape_top, shape_left, shape_bottom, shape_right"
					+ ", shape_cap_line, shape_mean_line, shape_base_line, shape_pixels, shape_letter, shape_group_id" + ", shape_index, shape_original_guess) "
					+ "VALUES (:shape_id, :shape_top, :shape_left, :shape_bottom, :shape_right"
					+ ", :shape_cap_line, :shape_mean_line, :shape_base_line, :shape_pixels, :shape_letter, :shape_group_id" + ", :shape_index, :shape_original_guess)";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			iShape.setId(shapeId);
		} else {
			paramSource.addValue("shape_id", shape.getId());

			sql = "UPDATE ocr_shape" + " SET shape_top = :shape_top" + ", shape_left = :shape_left" + ", shape_bottom = :shape_bottom"
					+ ", shape_right = :shape_right" + ", shape_cap_line = :shape_cap_line" + ", shape_mean_line = :shape_mean_line"
					+ ", shape_base_line = :shape_base_line" + ", shape_letter = :shape_letter" + ", shape_group_id = :shape_group_id" + ", shape_index = :shape_index "
					+ ", shape_original_guess = :shape_original_guess " + " WHERE shape_id = :shape_id";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);
		}
	}

	@Override
	public void deleteContiguousShapeInternal(ShapeInternal shape) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("shape_id", shape.getId());

		String sql = "DELETE FROM ocr_shape WHERE shape_id = :shape_id";
		LOG.debug(sql);
		logParameters(paramSource);
		jt.update(sql, paramSource);
	}

	@Override
	public void saveJochreImage(JochreImage image) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		JochreImageInternal iImage = (JochreImageInternal) image;

		paramSource.addValue("image_name", image.getName());
		paramSource.addValue("image_width", image.getWidth());
		paramSource.addValue("image_height", image.getHeight());
		paramSource.addValue("image_black_threshold", image.getBlackThreshold());
		paramSource.addValue("image_sep_threshold", image.getSeparationThreshold());
		paramSource.addValue("image_black_limit", image.getBlackLimit());
		paramSource.addValue("image_white_limit", image.getWhiteLimit());
		paramSource.addValue("image_white_gap_fill_factor", image.getWhiteGapFillFactor());
		paramSource.addValue("image_page_id", image.getPageId());
		paramSource.addValue("image_index", image.getIndex());
		paramSource.addValue("image_imgstatus_id", image.getImageStatus().getId());
		paramSource.addValue("image_owner_id", image.getOwnerId());

		String sql = null;

		if (image.getId() == 0) {
			sql = "SELECT nextval('ocr_image_id_seq')";
			LOG.debug(sql);
			int imageId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("image_id", imageId);

			sql = "INSERT INTO ocr_image (image_id, image_name, image_width, image_height, image_black_threshold,"
					+ " image_page_id, image_index, image_sep_threshold, image_black_limit, image_white_limit,"
					+ " image_white_gap_fill_factor, image_imgstatus_id, image_owner_id) "
					+ "VALUES (:image_id, :image_name, :image_width, :image_height, :image_black_threshold,"
					+ " :image_page_id, :image_index, :image_sep_threshold, :image_black_limit, :image_white_limit,"
					+ " :image_white_gap_fill_factor, :image_imgstatus_id, :image_owner_id)";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			iImage.setId(imageId);
		} else {
			paramSource.addValue("image_id", image.getId());

			sql = "UPDATE ocr_image" + " SET image_name = :image_name" + ", image_width = :image_width" + ", image_height = :image_height"
					+ ", image_black_threshold = :image_black_threshold" + ", image_sep_threshold = :image_sep_threshold" + ", image_black_limit = :image_black_limit"
					+ ", image_white_limit = :image_white_limit" + ", image_white_gap_fill_factor = :image_white_gap_fill_factor" + ", image_page_id = :image_page_id"
					+ ", image_index = :image_index" + ", image_imgstatus_id = :image_imgstatus_id" + ", image_owner_id = :image_owner_id"
					+ " WHERE image_id = :image_id";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);
		}
	}

	@Override
	public void deleteJochreImage(JochreImage image) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("image_id", image.getId());
		String sql = null;

		sql = "delete from ocr_split where split_shape_id in (" + " select shape_id from ocr_shape" + " inner join ocr_group on shape_group_id = group_id"
				+ " inner join ocr_row on group_row_id = row_id" + " inner join ocr_paragraph on row_paragraph_id = paragraph_id"
				+ " WHERE paragraph_image_id = :image_id)";

		LOG.debug(sql);
		logParameters(paramSource);
		jt.update(sql, paramSource);

		sql = "delete from ocr_shape where shape_group_id in (" + " select group_id from ocr_group" + " inner join ocr_row on group_row_id = row_id"
				+ " inner join ocr_paragraph on row_paragraph_id = paragraph_id" + " WHERE paragraph_image_id = :image_id)";

		LOG.debug(sql);
		logParameters(paramSource);
		jt.update(sql, paramSource);

		sql = "delete from ocr_group where group_row_id in (" + " select row_id from ocr_row" + " inner join ocr_paragraph on row_paragraph_id = paragraph_id"
				+ " WHERE paragraph_image_id = :image_id)";

		LOG.debug(sql);
		logParameters(paramSource);
		jt.update(sql, paramSource);

		sql = "delete from ocr_row where row_paragraph_id in (" + " select paragraph_id from ocr_paragraph" + " WHERE paragraph_image_id = :image_id)";

		LOG.debug(sql);
		logParameters(paramSource);
		jt.update(sql, paramSource);

		sql = "delete from ocr_paragraph" + " where paragraph_image_id = :image_id";

		LOG.debug(sql);
		logParameters(paramSource);
		jt.update(sql, paramSource);

		sql = "delete from ocr_image" + " WHERE image_id = :image_id";

		LOG.debug(sql);
		logParameters(paramSource);
		jt.update(sql, paramSource);

	}

	@Override
	public void saveRowOfShapes(RowOfShapes row) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		RowOfShapesInternal iRow = (RowOfShapesInternal) row;

		paramSource.addValue("row_paragraph_id", row.getParagraphId());
		paramSource.addValue("row_index", row.getIndex());
		paramSource.addValue("row_height", row.getXHeight());
		String sql = null;

		if (row.getId() == 0) {
			sql = "SELECT nextval('ocr_row_id_seq')";
			LOG.debug(sql);
			int rowId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("row_id", rowId);

			ImageUtils.storeImage(paramSource, "row_image", row.getImage());

			sql = "INSERT INTO ocr_row (row_id, row_paragraph_id, row_index, row_image, row_height) "
					+ "VALUES (:row_id, :row_paragraph_id, :row_index, :row_image, :row_height)";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			iRow.clearMemory();
			iRow.setId(rowId);
		} else {
			paramSource.addValue("row_id", row.getId());

			sql = "UPDATE ocr_row" + " SET row_paragraph_id = :row_paragraph_id" + ", row_index = :row_index" + ", row_height = :row_height"
					+ " WHERE row_id = :row_id";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);
		}
	}

	@Override
	public void saveGroupOfShapes(GroupOfShapes group) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		GroupOfShapesInternal iGroup = (GroupOfShapesInternal) group;

		paramSource.addValue("group_row_id", group.getRowId());
		paramSource.addValue("group_index", group.getIndex());
		paramSource.addValue("group_hard_hyphen", group.isHardHyphen());
		paramSource.addValue("group_broken_word", group.isBrokenWord());
		paramSource.addValue("group_segment_problem", group.isSegmentationProblem());
		paramSource.addValue("group_skip", group.isSkip());
		String sql = null;

		if (group.getId() == 0) {
			sql = "SELECT nextval('ocr_group_id_seq')";
			LOG.debug(sql);
			int groupId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("group_id", groupId);

			sql = "INSERT INTO ocr_group (group_id, group_row_id, group_index, group_hard_hyphen, group_broken_word, group_segment_problem, group_skip) "
					+ "VALUES (:group_id, :group_row_id, :group_index, :group_hard_hyphen, :group_broken_word, :group_segment_problem, :group_skip)";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			iGroup.setId(groupId);
		} else {
			paramSource.addValue("group_id", group.getId());

			sql = "UPDATE ocr_group" + " SET group_row_id = :group_row_id" + ", group_index = :group_index" + ", group_hard_hyphen = :group_hard_hyphen"
					+ ", group_broken_word = :group_broken_word" + ", group_segment_problem = :group_segment_problem" + ", group_skip = :group_skip"
					+ " WHERE group_id = :group_id";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);
		}
	}

	@Override
	public Paragraph loadParagraph(int paragraphId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_PARAGRAPH + " FROM ocr_paragraph WHERE paragraph_id=:paragraph_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("paragraph_id", paragraphId);

		LOG.debug(sql);
		logParameters(paramSource);
		Paragraph paragraph = null;
		try {
			paragraph = jt.queryForObject(sql, paramSource, new ParagraphMapper(this.getGraphicsServiceInternal()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return paragraph;
	}

	@Override
	public List<Paragraph> findParagraphs(JochreImage jochreImage) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_PARAGRAPH + " FROM ocr_paragraph WHERE paragraph_image_id=:paragraph_image_id" + " ORDER BY paragraph_index";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("paragraph_image_id", jochreImage.getId());

		LOG.debug(sql);
		logParameters(paramSource);
		List<Paragraph> paragraphs = jt.query(sql, paramSource, new ParagraphMapper(this.getGraphicsServiceInternal()));

		return paragraphs;
	}

	protected static final class ParagraphMapper implements RowMapper<Paragraph> {
		private GraphicsServiceInternal graphicsService;

		protected ParagraphMapper(GraphicsServiceInternal graphicsService) {
			this.graphicsService = graphicsService;
		}

		@Override
		public Paragraph mapRow(ResultSet rs, int rowNum) throws SQLException {
			ParagraphInternal paragraph = graphicsService.getEmptyParagraphInternal();
			paragraph.setId(rs.getInt("paragraph_id"));
			paragraph.setImageId(rs.getInt("paragraph_image_id"));
			paragraph.setIndex(rs.getInt("paragraph_index"));
			return paragraph;
		}
	}

	@Override
	public void saveParagraph(Paragraph paragraph) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		ParagraphInternal iParagraph = (ParagraphInternal) paragraph;

		paramSource.addValue("paragraph_image_id", paragraph.getImageId());
		paramSource.addValue("paragraph_index", paragraph.getIndex());
		String sql = null;

		if (paragraph.getId() == 0) {
			sql = "SELECT nextval('ocr_paragraph_id_seq')";
			LOG.debug(sql);
			int paragraphId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("paragraph_id", paragraphId);

			sql = "INSERT INTO ocr_paragraph (paragraph_id, paragraph_image_id, paragraph_index) " + "VALUES (:paragraph_id, :paragraph_image_id, :paragraph_index)";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			iParagraph.setId(paragraphId);
		} else {
			paramSource.addValue("paragraph_id", paragraph.getId());

			sql = "UPDATE ocr_paragraph" + " SET paragraph_image_id = :paragraph_image_id" + ", paragraph_index = :paragraph_index"
					+ " WHERE paragraph_id = :paragraph_id";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);
		}
	}

	@Override
	public int getShapeCount(JochreImage jochreImage) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT count(*) FROM ocr_shape" + " INNER JOIN ocr_group ON shape_group_id = group_id" + " INNER JOIN ocr_row ON group_row_id = row_id"
				+ " INNER JOIN ocr_paragraph ON row_paragraph_id = paragraph_id" + " INNER JOIN ocr_image ON paragraph_image_id = image_id"
				+ " WHERE image_id = :image_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("image_id", jochreImage.getId());

		LOG.debug(sql);
		logParameters(paramSource);
		int count = jt.queryForObject(sql, paramSource, Integer.class);
		return count;
	}

	@Override
	public List<Integer> findShapeIds(String letter) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT shape_id FROM ocr_shape" + " INNER JOIN ocr_group ON shape_group_id = group_id" + " INNER JOIN ocr_row ON group_row_id = row_id"
				+ " INNER JOIN ocr_paragraph ON row_paragraph_id = paragraph_id" + " INNER JOIN ocr_image ON paragraph_image_id = image_id"
				+ " WHERE image_imgstatus_id = :image_imgstatus_id" + " AND shape_letter = :shape_letter" + " ORDER BY shape_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("image_imgstatus_id", ImageStatus.TRAINING_VALIDATED.getId());
		paramSource.addValue("shape_letter", letter);

		LOG.debug(sql);
		logParameters(paramSource);

		List<Integer> shapeIds = jt.queryForList(sql, paramSource, Integer.class);
		return shapeIds;
	}

	@Override
	public GraphicsServiceInternal getGraphicsServiceInternal() {
		return graphicsServiceInternal;
	}

	@Override
	public void setGraphicsServiceInternal(GraphicsServiceInternal graphicsServiceInternal) {
		this.graphicsServiceInternal = graphicsServiceInternal;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static void logParameters(MapSqlParameterSource paramSource) {
		DaoUtils.LogParameters(paramSource.getValues());
	}
}
