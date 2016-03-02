package com.joliciel.jochre.search.feedback;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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

import com.joliciel.jochre.utils.dao.ImageUtils;
import com.joliciel.talismane.utils.DaoUtils;
import com.joliciel.talismane.utils.LogUtils;

class FeedbackDAO {
	private static final Log LOG = LogFactory.getLog(FeedbackDAO.class);
	private DataSource dataSource;
	private FeedbackServiceInternal feedbackService;

    private static final String SELECT_DOCUMENT = "doc_id, doc_path";
    private static final String SELECT_USER = "user_id, user_username";
    private static final String SELECT_LANGUAGE = "language_id, language_code";
    private static final String SELECT_FONT = "font_id, font_code";
    private static final String SELECT_ROW = "row_id, row_doc_id, row_page_index, row_x, row_y, row_width, row_height, row_image";
    private static final String SELECT_WORD = "word_id, word_row_id, word_x, word_y, word_width, word_height"
    		+ ", word_2nd_x, word_2nd_y, word_2nd_width, word_2nd_height, word_2nd_row_id, word_initial_guess, word_image";
    private static final String SELECT_SUGGESTION = "suggest_id, suggest_user_id, suggest_word_id, suggest_font_id, suggest_language_id, suggest_create_date"
    		+ ", suggest_text, suggest_previous_text, suggest_applied, suggest_ignore";

	public FeedbackDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public FeedbackWord loadWord(int wordId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_WORD + " FROM joc_word WHERE word_id=:word_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("word_id", wordId);

		LOG.debug(sql);
		logParameters(paramSource);
		FeedbackWord word = null;
		try {
			word =  jt.queryForObject(sql, paramSource, new WordMapper(this.getFeedbackService()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return word;
	}
	
	public FeedbackWord findWord(FeedbackDocument doc, int pageIndex,
			Rectangle rectangle) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_WORD + " FROM joc_word"
				+ " INNER JOIN joc_row ON word_row_id = row_id"
				+ " WHERE row_doc_id=:row_doc_id"
				+ " AND row_page_index=:row_page_index"
				+ " AND word_x=:word_x"
				+ " AND word_y=:word_y";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("row_doc_id", doc.getId());
		paramSource.addValue("row_page_index", pageIndex);
		paramSource.addValue("word_x", rectangle.x);
		paramSource.addValue("word_y", rectangle.y);

		LOG.debug(sql);
		logParameters(paramSource);
		FeedbackWord word = null;
		try {
			word =  jt.queryForObject(sql, paramSource, new WordMapper(this.getFeedbackService()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return word;
	}
	
	public void saveWord(FeedbackWordInternal word) {
		// note: no update
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		//word_id, word_row_id, word_x, word_y, word_width, word_height
		//word_2nd_x, word_2nd_y, word_2nd_width, word_2nd_height, word_2nd_row_id, word_initial_guess, word_image
		paramSource.addValue("word_row_id", word.getRowId());
		paramSource.addValue("word_x", word.getRectangle().x);
		paramSource.addValue("word_y", word.getRectangle().y);
		paramSource.addValue("word_width", word.getRectangle().width);
		paramSource.addValue("word_height", word.getRectangle().height);
		if (word.getSecondRectangle()!=null) {
			paramSource.addValue("word_2nd_x", word.getSecondRectangle().x);
			paramSource.addValue("word_2nd_y", word.getSecondRectangle().y);
			paramSource.addValue("word_2nd_width", word.getSecondRectangle().width);
			paramSource.addValue("word_2nd_height", word.getSecondRectangle().height);
		} else {
			paramSource.addValue("word_2nd_x", null);
			paramSource.addValue("word_2nd_y", null);
			paramSource.addValue("word_2nd_width", null);
			paramSource.addValue("word_2nd_height", null);
		}
		paramSource.addValue("word_2nd_row_id", word.getSecondRowId()==0 ? null : word.getSecondRowId());
		paramSource.addValue("word_initial_guess", word.getInitialGuess());
		String sql = null;

		if (word.isNew()) {
			sql = "SELECT nextval('joc_word_word_id_seq')";
			LOG.debug(sql);
			int wordId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("word_id", wordId);
			
			ImageUtils.storeImage(paramSource, "word_image", word.getImage());

			sql = "INSERT INTO joc_word (word_id, word_row_id, word_x, word_y, word_width, word_height"
					+ ", word_2nd_x, word_2nd_y, word_2nd_width, word_2nd_height, word_2nd_row_id, word_initial_guess, word_image) "
					+ " VALUES (:word_id, :word_row_id, :word_x, :word_y, :word_width, :word_height"
					+ ", :word_2nd_x, :word_2nd_y, :word_2nd_width, :word_2nd_height, :word_2nd_row_id, :word_initial_guess, :word_image)";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			word.setId(wordId);
		}
	}
	
	protected static final class WordMapper implements RowMapper<FeedbackWord> {
		private FeedbackServiceInternal feedbackService;

		protected WordMapper(FeedbackServiceInternal feedbackService) {
			this.feedbackService = feedbackService;
		}
		
		public FeedbackWord mapRow(ResultSet rs, int rowNum) throws SQLException {
           return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public FeedbackWordInternal mapRow(SqlRowSet rs) throws SQLException {
			FeedbackWordInternal word = feedbackService.getEmptyFeedbackWordInternal();
			// word_id, word_row_id, word_x, word_y, word_width, word_height
    		// word_2nd_x, word_2nd_y, word_2nd_width, word_2nd_height, word_2nd_row_id, word_initial_guess
			word.setId(rs.getInt("word_id"));
			word.setRowId(rs.getInt("word_row_id"));
			int x = rs.getInt("word_x");
			int y = rs.getInt("word_y");
			int width = rs.getInt("word_width");
			int height = rs.getInt("word_height");
			Rectangle rect = new Rectangle(x, y, width, height);
			word.setRectangle(rect);
			
			rs.getInt("word_2nd_x");
			if (!rs.wasNull()) {
				int x2 = rs.getInt("word_2nd_x");
				int y2 = rs.getInt("word_2nd_y");
				int width2 = rs.getInt("word_2nd_width");
				int height2 = rs.getInt("word_2nd_height");
				Rectangle rect2 = new Rectangle(x2, y2, width2, height2);
				word.setSecondRectangle(rect2);
			}

			BufferedImage image = ImageUtils.getImage(rs, "word_image");
			if (image!=null) word.setImage(image);
			
			return word;
		}
	}
	

	public FeedbackRow loadRow(int rowId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_ROW + " FROM joc_row WHERE row_id=:row_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("row_id", rowId);

		LOG.debug(sql);
		logParameters(paramSource);
		FeedbackRow row = null;
		try {
			row =  jt.queryForObject(sql, paramSource, new FeedbackRowMapper(this.getFeedbackService()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return row;
	}
	
	public FeedbackRow findRow(FeedbackDocument doc, int pageIndex,
			Rectangle rectangle) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_ROW + " FROM joc_row WHERE row_doc_id=:row_doc_id"
				+ " AND row_page_index=:row_page_index"
				+ " AND row_x=:row_x"
				+ " AND row_y=:row_y";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("row_doc_id", doc.getId());
		paramSource.addValue("row_page_index", pageIndex);
		paramSource.addValue("row_x", rectangle.x);
		paramSource.addValue("row_y", rectangle.y);

		LOG.debug(sql);
		logParameters(paramSource);
		FeedbackRow row = null;
		try {
			row =  jt.queryForObject(sql, paramSource, new FeedbackRowMapper(this.getFeedbackService()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return row;
	}
	
	public void saveRow(FeedbackRowInternal row) {
		// note: no update
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		// row_id, row_doc_id, row_page_index, row_x, row_y, row_width, row_height, row_image
		paramSource.addValue("row_doc_id", row.getDocumentId());
		paramSource.addValue("row_page_index", row.getPageIndex());
		paramSource.addValue("row_x", row.getRectangle().x);
		paramSource.addValue("row_y", row.getRectangle().y);
		paramSource.addValue("row_width", row.getRectangle().width);
		paramSource.addValue("row_height", row.getRectangle().height);

		String sql = null;

		if (row.isNew()) {
			sql = "SELECT nextval('joc_row_row_id_seq')";
			LOG.debug(sql);
			int rowId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("row_id", rowId);
			
			ImageUtils.storeImage(paramSource, "row_image", row.getImage());

			sql = "INSERT INTO joc_row (row_id, row_doc_id, row_page_index, row_x, row_y, row_width, row_height, row_image) "
					+ " VALUES (:row_id, :row_doc_id, :row_page_index, :row_x, :row_y, :row_width, :row_height, :row_image)";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			row.setId(rowId);
		}
	}
	
	protected static final class FeedbackRowMapper implements RowMapper<FeedbackRow> {
		private FeedbackServiceInternal feedbackService;

		protected FeedbackRowMapper(FeedbackServiceInternal feedbackService) {
			this.feedbackService = feedbackService;
		}
		
		public FeedbackRow mapRow(ResultSet rs, int rowNum) throws SQLException {
           return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public FeedbackRowInternal mapRow(SqlRowSet rs) throws SQLException {
			FeedbackRowInternal row = feedbackService.getEmptyFeedbackRowInternal();
			// row_id, row_doc_id, row_page_index, row_x, row_y, row_width, row_height, row_image
			row.setId(rs.getInt("row_id"));
			row.setDocumentId(rs.getInt("row_doc_id"));
			row.setPageIndex(rs.getInt("row_page_index"));
			
			int x = rs.getInt("row_x");
			int y = rs.getInt("row_y");
			int width = rs.getInt("row_width");
			int height = rs.getInt("row_height");
			Rectangle rect = new Rectangle(x, y, width, height);
			row.setRectangle(rect);
			
			BufferedImage image = ImageUtils.getImage(rs, "row_image");
			if (image!=null) row.setImage(image);
			
			return row;
		}
	}
	
	public FeedbackSuggestion loadSuggestion(int suggestionId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_SUGGESTION + " FROM joc_suggestion WHERE suggestion_id=:suggestion_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("suggestion_id", suggestionId);

		LOG.debug(sql);
		logParameters(paramSource);
		FeedbackSuggestion suggestion = null;
		try {
			suggestion =  jt.queryForObject(sql, paramSource, new SuggestionMapper(this.getFeedbackService()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return suggestion;
	}
	
	public List<FeedbackSuggestion> findUnappliedSuggestions() {
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
			String sql = "SELECT " + SELECT_SUGGESTION + ", " + SELECT_WORD + ", " + SELECT_ROW + ", " + SELECT_DOCUMENT + " FROM joc_suggestion"
					+ " INNER JOIN joc_word ON suggest_word_id = word_id"
					+ " INNER JOIN joc_row ON word_row_id = row_id"
					+ " INNER JOIN joc_document ON row_doc_id = doc_id"
					+ " WHERE suggest_applied=:false"
					+ " AND suggest_ignore=:false"
					+ " ORDER BY suggest_id";
			MapSqlParameterSource paramSource = new MapSqlParameterSource();
			paramSource.addValue("false", false);
	
			LOG.info(sql);
			logParameters(paramSource);
			
			SqlRowSet rs = jt.queryForRowSet(sql, paramSource);
			SuggestionMapper suggestionMapper = new SuggestionMapper(this.getFeedbackService());
			WordMapper wordMapper = new WordMapper(this.getFeedbackService());
			FeedbackRowMapper rowMapper = new FeedbackRowMapper(this.getFeedbackService());
			DocumentMapper docMapper = new DocumentMapper(this.getFeedbackService());
			List<FeedbackSuggestion> suggestions = new ArrayList<>();
			while (rs.next()) {
				FeedbackSuggestionInternal suggestion = suggestionMapper.mapRow(rs);
				FeedbackWordInternal word = wordMapper.mapRow(rs);
				FeedbackRowInternal row = rowMapper.mapRow(rs);
				FeedbackDocument document = docMapper.mapRow(rs);
				suggestion.setWord(word);
				word.setRow(row);
				row.setDocument(document);
				suggestions.add(suggestion);
			}
	
			return suggestions;
		} catch (SQLException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public List<FeedbackSuggestion> findSuggestions(FeedbackDocument doc, int pageIndex) {
		try {
			NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
			String sql = "SELECT " + SELECT_SUGGESTION + ", " + SELECT_WORD + ", " + SELECT_ROW + ", " + SELECT_DOCUMENT + " FROM joc_suggestion"
					+ " INNER JOIN joc_word ON suggest_word_id = word_id"
					+ " INNER JOIN joc_row ON word_row_id = row_id"
					+ " INNER JOIN joc_document ON row_doc_id = doc_id"
					+ " WHERE row_doc_id=:row_doc_id"
					+ " AND row_page_index=:row_page_index"
					+ " AND suggest_ignore=:false"
					+ " ORDER BY suggest_id";
			MapSqlParameterSource paramSource = new MapSqlParameterSource();
			paramSource.addValue("row_doc_id", doc.getId());
			paramSource.addValue("row_page_index", pageIndex);
			paramSource.addValue("false", false);
	
			LOG.info(sql);
			logParameters(paramSource);
			
			SqlRowSet rs = jt.queryForRowSet(sql, paramSource);
			SuggestionMapper suggestionMapper = new SuggestionMapper(this.getFeedbackService());
			WordMapper wordMapper = new WordMapper(this.getFeedbackService());
			FeedbackRowMapper rowMapper = new FeedbackRowMapper(this.getFeedbackService());
			DocumentMapper docMapper = new DocumentMapper(this.getFeedbackService());
			List<FeedbackSuggestion> suggestions = new ArrayList<>();
			while (rs.next()) {
				FeedbackSuggestionInternal suggestion = suggestionMapper.mapRow(rs);
				FeedbackWordInternal word = wordMapper.mapRow(rs);
				FeedbackRowInternal row = rowMapper.mapRow(rs);
				FeedbackDocument document = docMapper.mapRow(rs);
				suggestion.setWord(word);
				word.setRow(row);
				row.setDocument(document);
				suggestions.add(suggestion);
			}
	
			return suggestions;
		} catch (SQLException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	public void saveSuggestion(FeedbackSuggestionInternal suggestion) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		// suggest_id, suggest_user_id, suggest_word_id, suggest_font_id, suggest_language_id, suggest_create_date
		// suggest_text, suggest_previous_text, suggest_applied, suggest_ignore
		paramSource.addValue("suggest_id", suggestion.getId());
		paramSource.addValue("suggest_word_id", suggestion.getWordId());
		paramSource.addValue("suggest_user_id", suggestion.getUserId());
		paramSource.addValue("suggest_font_id", suggestion.getFontId());
		paramSource.addValue("suggest_language_id", suggestion.getLanguageId());
		paramSource.addValue("suggest_text", suggestion.getText());
		paramSource.addValue("suggest_previous_text", suggestion.getPreviousText());
		paramSource.addValue("suggest_applied", suggestion.isApplied());
		paramSource.addValue("suggest_ignore", suggestion.isIgnored());

		String sql = null;

		if (suggestion.isNew()) {
			sql = "SELECT nextval('joc_suggestion_suggest_id_seq')";
			LOG.debug(sql);
			int suggestionId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("suggest_id", suggestionId);

			sql = "INSERT INTO joc_suggestion (suggest_id, suggest_user_id, suggest_word_id, suggest_font_id, suggest_language_id, suggest_create_date"
					+ ", suggest_text, suggest_previous_text, suggest_applied, suggest_ignore) "
					+ " VALUES (:suggest_id, :suggest_user_id, :suggest_word_id, :suggest_font_id, :suggest_language_id, current_timestamp"
					+ ", :suggest_text, :suggest_previous_text, :suggest_applied, :suggest_ignore)";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			suggestion.setId(suggestionId);
		} else {
			paramSource.addValue("suggest_id", suggestion.getId());
			
			sql = "UPDATE joc_suggestion"
					+ " SET suggest_applied=:suggest_applied"
					+ ", suggest_ignore=:suggest_ignore"
					+ " WHERE suggest_id=:suggest_id";
			
			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);
		}
	}
	
	protected static final class SuggestionMapper implements RowMapper<FeedbackSuggestion> {
		private FeedbackServiceInternal feedbackService;

		protected SuggestionMapper(FeedbackServiceInternal feedbackService) {
			this.feedbackService = feedbackService;
		}
		
		public FeedbackSuggestion mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public FeedbackSuggestionInternal mapRow(SqlRowSet rs) {
			FeedbackSuggestionInternal suggestion = feedbackService.getEmptyFeedbackSuggestionInternal();
			// suggest_id, suggest_user_id, suggest_word_id, suggest_font_id, suggest_language_id, suggest_create_date
    		// suggest_text, suggest_previous_text, suggest_applied, suggest_ignore
			suggestion.setId(rs.getInt("suggest_id"));
			suggestion.setUserId(rs.getInt("suggest_user_id"));
			suggestion.setWordId(rs.getInt("suggest_word_id"));
			suggestion.setFontId(rs.getInt("suggest_font_id"));
			suggestion.setLanguageId(rs.getInt("suggest_language_id"));
			suggestion.setCreateDate(rs.getTimestamp("suggest_create_date"));
			suggestion.setText(rs.getString("suggest_text"));
			suggestion.setPreviousText(rs.getString("suggest_previous_text"));
			suggestion.setApplied(rs.getBoolean("suggest_applied"));
			suggestion.setIgnored(rs.getBoolean("suggest_ignore"));
			
			return suggestion;
		}
	}
	
	public FeedbackUser loadUser(int userId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_USER + " FROM joc_user WHERE user_id=:user_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("user_id", userId);

		LOG.debug(sql);
		logParameters(paramSource);
		FeedbackUser user = null;
		try {
			user =  jt.queryForObject(sql, paramSource, new UserMapper(this.getFeedbackService()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return user;
	}
	
	public FeedbackUser findUser(String userName) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_USER + " FROM joc_user WHERE user_username=:user_username";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("user_username", userName);

		LOG.debug(sql);
		logParameters(paramSource);
		FeedbackUser user = null;
		try {
			user =  jt.queryForObject(sql, paramSource, new UserMapper(this.getFeedbackService()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return user;
	}
	
	public void saveUser(FeedbackUserInternal user) {
		// note: no update
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		// user_id, user_username
		paramSource.addValue("user_username", user.getUserName());

		String sql = null;

		if (user.isNew()) {
			sql = "SELECT nextval('joc_user_user_id_seq')";
			LOG.debug(sql);
			int userId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("user_id", userId);

			sql = "INSERT INTO joc_user (user_id, user_username) "
					+ " VALUES (:user_id, :user_username)";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			user.setId(userId);
		}
	}
	
	protected static final class UserMapper implements RowMapper<FeedbackUser> {
		private FeedbackServiceInternal feedbackService;

		protected UserMapper(FeedbackServiceInternal feedbackService) {
			this.feedbackService = feedbackService;
		}
		
		public FeedbackUser mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public FeedbackUserInternal mapRow(SqlRowSet rs) {
			FeedbackUserInternal user = feedbackService.getEmptyFeedbackUserInternal();
			// user_id, user_username
			user.setId(rs.getInt("user_id"));
			user.setUserName(rs.getString("user_username"));
			
			return user;
		}
	}
	
	public FeedbackFont loadFont(int fontId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_FONT + " FROM joc_font WHERE font_id=:font_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("font_id", fontId);

		LOG.debug(sql);
		logParameters(paramSource);
		FeedbackFont font = null;
		try {
			font =  jt.queryForObject(sql, paramSource, new FontMapper(this.getFeedbackService()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return font;
	}
	
	public FeedbackFont findFont(String code) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_FONT + " FROM joc_font WHERE font_code=:font_code";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("font_code", code);

		LOG.debug(sql);
		logParameters(paramSource);
		FeedbackFont font = null;
		try {
			font =  jt.queryForObject(sql, paramSource, new FontMapper(this.getFeedbackService()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return font;
	}
	
	public void saveFont(FeedbackFontInternal font) {
		// note: no update
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		// font_id, font_code
		paramSource.addValue("font_code", font.getCode());

		String sql = null;

		if (font.isNew()) {
			sql = "SELECT nextval('joc_font_font_id_seq')";
			LOG.debug(sql);
			int fontId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("font_id", fontId);

			sql = "INSERT INTO joc_font (font_id, font_code) "
					+ " VALUES (:font_id, :font_code)";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			font.setId(fontId);
		}
	}
	
	protected static final class FontMapper implements RowMapper<FeedbackFont> {
		private FeedbackServiceInternal feedbackService;

		protected FontMapper(FeedbackServiceInternal feedbackService) {
			this.feedbackService = feedbackService;
		}
		
		public FeedbackFont mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public FeedbackFontInternal mapRow(SqlRowSet rs) {
			FeedbackFontInternal font = feedbackService.getEmptyFeedbackFontInternal();
			// font_id, font_code
			font.setId(rs.getInt("font_id"));
			font.setCode(rs.getString("font_code"));
			
			return font;
		}
	}
	
	public FeedbackLanguage loadLanguage(int languageId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_LANGUAGE + " FROM joc_language WHERE language_id=:language_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("language_id", languageId);

		LOG.debug(sql);
		logParameters(paramSource);
		FeedbackLanguage language = null;
		try {
			language =  jt.queryForObject(sql, paramSource, new LanguageMapper(this.getFeedbackService()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return language;
	}
	
	public FeedbackLanguage findLanguage(String code) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_LANGUAGE + " FROM joc_language WHERE language_code=:language_code";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("language_code", code);

		LOG.debug(sql);
		logParameters(paramSource);
		FeedbackLanguage language = null;
		try {
			language =  jt.queryForObject(sql, paramSource, new LanguageMapper(this.getFeedbackService()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return language;
	}
	
	public void saveLanguage(FeedbackLanguageInternal language) {
		// note: no update
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		// language_id, language_code
		paramSource.addValue("language_code", language.getCode());

		String sql = null;

		if (language.isNew()) {
			sql = "SELECT nextval('joc_language_language_id_seq')";
			LOG.debug(sql);
			int languageId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("language_id", languageId);

			sql = "INSERT INTO joc_language (language_id, language_code) "
					+ " VALUES (:language_id, :language_code)";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			language.setId(languageId);
		}
	}
	
	protected static final class LanguageMapper implements RowMapper<FeedbackLanguage> {
		private FeedbackServiceInternal feedbackService;

		protected LanguageMapper(FeedbackServiceInternal feedbackService) {
			this.feedbackService = feedbackService;
		}
		
		public FeedbackLanguage mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public FeedbackLanguageInternal mapRow(SqlRowSet rs) {
			FeedbackLanguageInternal language = feedbackService.getEmptyFeedbackLanguageInternal();
			// language_id, language_code
			language.setId(rs.getInt("language_id"));
			language.setCode(rs.getString("language_code"));
			
			return language;
		}
	}
	

	public FeedbackDocument loadDocument(int documentId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_DOCUMENT + " FROM joc_document WHERE doc_id=:doc_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("doc_id", documentId);

		LOG.debug(sql);
		logParameters(paramSource);
		FeedbackDocument document = null;
		try {
			document =  jt.queryForObject(sql, paramSource, new DocumentMapper(this.getFeedbackService()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return document;
	}
	
	public FeedbackDocument findDocument(String path) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_DOCUMENT + " FROM joc_document WHERE doc_path=:doc_path";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("doc_path", path);

		LOG.debug(sql);
		logParameters(paramSource);
		FeedbackDocument doc = null;
		try {
			doc =  jt.queryForObject(sql, paramSource, new DocumentMapper(this.getFeedbackService()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return doc;
	}
	
	public void saveDocument(FeedbackDocumentInternal doc) {
		// note: no update
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		// doc_id, doc_code
		paramSource.addValue("doc_path", doc.getPath());

		String sql = null;

		if (doc.isNew()) {
			sql = "SELECT nextval('joc_document_doc_id_seq')";
			LOG.debug(sql);
			int docId = jt.queryForObject(sql, paramSource, Integer.class);
			paramSource.addValue("doc_id", docId);

			sql = "INSERT INTO joc_document (doc_id, doc_path) "
					+ " VALUES (:doc_id, :doc_path)";

			LOG.debug(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			doc.setId(docId);
		}
	}
	
	protected static final class DocumentMapper implements RowMapper<FeedbackDocument> {
		private FeedbackServiceInternal feedbackService;

		protected DocumentMapper(FeedbackServiceInternal feedbackService) {
			this.feedbackService = feedbackService;
		}
		
		public FeedbackDocument mapRow(ResultSet rs, int rowNum) throws SQLException {
            return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
        }

        public FeedbackDocumentInternal mapRow(SqlRowSet rs) {
			FeedbackDocumentInternal document = feedbackService.getEmptyFeedbackDocumentInternal();
			// doc_id, doc_path
			document.setId(rs.getInt("doc_id"));
			document.setPath(rs.getString("doc_path"));
			
			return document;
		}
	}
	
    public static void logParameters(MapSqlParameterSource paramSource) {
        DaoUtils.LogParameters(paramSource.getValues());
     }

	public FeedbackServiceInternal getFeedbackService() {
		return feedbackService;
	}

	public void setFeedbackService(FeedbackServiceInternal feedbackService) {
		this.feedbackService = feedbackService;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
}
