package com.joliciel.jochre.search.feedback;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.joliciel.jochre.search.JochreIndexField;
import com.joliciel.jochre.search.JochreSearchConfig;
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.jochre.utils.dao.ImageUtils;
import com.joliciel.talismane.utils.DaoUtils;

class FeedbackDAO {
  private static final Logger LOG = LoggerFactory.getLogger(FeedbackDAO.class);
  private static Map<String, FeedbackDAO> instances = new HashMap<>();

  public static FeedbackDAO getInstance(String configId) {
    FeedbackDAO feedbackDAO = instances.get(configId);
    if (feedbackDAO == null) {
      feedbackDAO = new FeedbackDAO(configId);
      instances.put(configId, feedbackDAO);
    }
    return feedbackDAO;
  }

  private final String configId;
  private final DataSource dataSource;

  private static final String SELECT_DOCUMENT = "doc_id, doc_path";
  private static final String SELECT_ROW = "row_id, row_doc_id, row_page_index, row_x, row_y, row_width, row_height, row_image";
  private static final String SELECT_WORD = "word_id, word_row_id, word_x, word_y, word_width, word_height"
      + ", word_2nd_x, word_2nd_y, word_2nd_width, word_2nd_height, word_2nd_row_id, word_initial_guess, word_image";
  private static final String SELECT_SUGGESTION = "suggest_id, suggest_user_id, suggest_word_id, suggest_font_id, suggest_language_id, suggest_create_date"
      + ", suggest_text, suggest_previous_text, suggest_ignore, suggest_ip_id, user_username, language_code, font_code, ip_address";
  private static final String SELECT_QUERY = "query_id, query_user_id, query_ip_id, query_date, query_results, user_username, ip_address";
  private static final String SELECT_CLAUSE = "clause_query_id, clause_criterion_id, clause_text, criterion_name";
  private static final String SELECT_CORRECTION = "cor_id, cor_doc_id, cor_field_id, cor_user_id, cor_value, cor_value_before, cor_date"
      + ", cor_apply_everywhere, cor_ip_id, cor_ignore, cor_sent, cor_documents, user_username, ip_address";

  private Map<Integer, JochreIndexField> idFieldMap = new HashMap<>();
  private Map<JochreIndexField, Integer> fieldIdMap = new HashMap<>();

  private FeedbackDAO(String configId) {
    this.configId = configId;
    JochreSearchConfig config = JochreSearchConfig.getInstance(configId);
    this.dataSource = config.getDataSource();
    this.loadCriteria();
    this.loadFields();
  }

  FeedbackWord loadWord(int wordId) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT " + SELECT_WORD + " FROM joc_word WHERE word_id=:word_id";
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    paramSource.addValue("word_id", wordId);

    LOG.debug(sql);
    logParameters(paramSource);
    FeedbackWord word = null;
    try {
      word = jt.queryForObject(sql, paramSource, new WordMapper());
    } catch (EmptyResultDataAccessException ex) {
      throw new EntityNotFoundException(FeedbackWord.class, wordId);
    }
    return word;
  }

  FeedbackWord findWord(FeedbackDocument doc, int pageIndex, Rectangle rectangle) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT " + SELECT_WORD + " FROM joc_word" + " INNER JOIN joc_row ON word_row_id = row_id"
        + " WHERE row_doc_id=:row_doc_id" + " AND row_page_index=:row_page_index" + " AND word_x=:word_x"
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
      word = jt.queryForObject(sql, paramSource, new WordMapper());
    } catch (EmptyResultDataAccessException ex) {
      ex.hashCode();
    }
    return word;
  }

  void saveWord(FeedbackWord word) {
    // note: no update
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    MapSqlParameterSource paramSource = new MapSqlParameterSource();

    // word_id, word_row_id, word_x, word_y, word_width, word_height
    // word_2nd_x, word_2nd_y, word_2nd_width, word_2nd_height,
    // word_2nd_row_id, word_initial_guess, word_image
    paramSource.addValue("word_row_id", word.getRowId());
    paramSource.addValue("word_x", word.getRectangle().x);
    paramSource.addValue("word_y", word.getRectangle().y);
    paramSource.addValue("word_width", word.getRectangle().width);
    paramSource.addValue("word_height", word.getRectangle().height);
    if (word.getSecondRectangle() != null) {
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
    paramSource.addValue("word_2nd_row_id", word.getSecondRowId() == 0 ? null : word.getSecondRowId());
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

  protected final class WordMapper implements RowMapper<FeedbackWord> {
    @Override
    public FeedbackWord mapRow(ResultSet rs, int rowNum) throws SQLException {
      return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
    }

    public FeedbackWord mapRow(SqlRowSet rs) throws SQLException {
      FeedbackWord word = new FeedbackWord(configId);
      // word_id, word_row_id, word_x, word_y, word_width, word_height
      // word_2nd_x, word_2nd_y, word_2nd_width, word_2nd_height,
      // word_2nd_row_id, word_initial_guess
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
      if (image != null)
        word.setImage(image);

      return word;
    }
  }

  FeedbackRow loadRow(int rowId) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT " + SELECT_ROW + " FROM joc_row WHERE row_id=:row_id";
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    paramSource.addValue("row_id", rowId);

    LOG.debug(sql);
    logParameters(paramSource);
    FeedbackRow row = null;
    try {
      row = jt.queryForObject(sql, paramSource, new FeedbackRowMapper());
    } catch (EmptyResultDataAccessException ex) {
      throw new EntityNotFoundException(FeedbackRow.class, rowId);
    }
    return row;
  }

  FeedbackRow findRow(FeedbackDocument doc, int pageIndex, Rectangle rectangle) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT " + SELECT_ROW + " FROM joc_row WHERE row_doc_id=:row_doc_id"
        + " AND row_page_index=:row_page_index" + " AND row_x=:row_x" + " AND row_y=:row_y";
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    paramSource.addValue("row_doc_id", doc.getId());
    paramSource.addValue("row_page_index", pageIndex);
    paramSource.addValue("row_x", rectangle.x);
    paramSource.addValue("row_y", rectangle.y);

    LOG.debug(sql);
    logParameters(paramSource);
    FeedbackRow row = null;
    try {
      row = jt.queryForObject(sql, paramSource, new FeedbackRowMapper());
    } catch (EmptyResultDataAccessException ex) {
      ex.hashCode();
    }
    return row;
  }

  void saveRow(FeedbackRow row) {
    // note: no update
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    MapSqlParameterSource paramSource = new MapSqlParameterSource();

    // row_id, row_doc_id, row_page_index, row_x, row_y, row_width,
    // row_height, row_image
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

  protected final class FeedbackRowMapper implements RowMapper<FeedbackRow> {
    @Override
    public FeedbackRow mapRow(ResultSet rs, int rowNum) throws SQLException {
      return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
    }

    public FeedbackRow mapRow(SqlRowSet rs) throws SQLException {
      FeedbackRow row = new FeedbackRow(configId);
      // row_id, row_doc_id, row_page_index, row_x, row_y, row_width,
      // row_height, row_image
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
      if (image != null)
        row.setImage(image);

      return row;
    }
  }

  FeedbackSuggestion loadSuggestion(int suggestionId) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT " + SELECT_SUGGESTION + " FROM joc_suggestion"
        + " INNER JOIN joc_font ON suggest_font_id = font_id"
        + " INNER JOIN joc_language ON suggest_language_id = language_id"
        + " INNER JOIN joc_user ON suggest_user_id = user_id" + " INNER JOIN joc_ip ON suggest_ip_id = ip_id"
        + " WHERE suggestion_id=:suggestion_id";
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    paramSource.addValue("suggestion_id", suggestionId);

    LOG.debug(sql);
    logParameters(paramSource);
    FeedbackSuggestion suggestion = null;
    try {
      suggestion = jt.queryForObject(sql, paramSource, new SuggestionMapper());
    } catch (EmptyResultDataAccessException ex) {
      throw new EntityNotFoundException(FeedbackSuggestion.class, suggestionId);
    }
    return suggestion;
  }

  List<FeedbackSuggestion> findSuggestions(FeedbackDocument doc, int pageIndex) {
    String sql = null;
    try {
      NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
      sql = "SELECT " + SELECT_SUGGESTION + ", " + SELECT_WORD + ", " + SELECT_ROW + ", " + SELECT_DOCUMENT
          + " FROM joc_suggestion" + " INNER JOIN joc_font ON suggest_font_id = font_id"
          + " INNER JOIN joc_language ON suggest_language_id = language_id"
          + " INNER JOIN joc_user ON suggest_user_id = user_id" + " INNER JOIN joc_ip ON suggest_ip_id = ip_id"
          + " INNER JOIN joc_word ON suggest_word_id = word_id" + " INNER JOIN joc_row ON word_row_id = row_id"
          + " INNER JOIN joc_document ON row_doc_id = doc_id" + " WHERE row_doc_id=:row_doc_id"
          + " AND row_page_index=:row_page_index" + " AND suggest_ignore=:false" + " ORDER BY suggest_id";
      MapSqlParameterSource paramSource = new MapSqlParameterSource();
      paramSource.addValue("row_doc_id", doc.getId());
      paramSource.addValue("row_page_index", pageIndex);
      paramSource.addValue("false", false);

      LOG.info(sql);
      logParameters(paramSource);

      SqlRowSet rs = jt.queryForRowSet(sql, paramSource);
      SuggestionMapper suggestionMapper = new SuggestionMapper();
      WordMapper wordMapper = new WordMapper();
      FeedbackRowMapper rowMapper = new FeedbackRowMapper();
      DocumentMapper docMapper = new DocumentMapper();
      List<FeedbackSuggestion> suggestions = new ArrayList<>();
      while (rs.next()) {
        FeedbackSuggestion suggestion = suggestionMapper.mapRow(rs);
        FeedbackWord word = wordMapper.mapRow(rs);
        FeedbackRow row = rowMapper.mapRow(rs);
        FeedbackDocument document = docMapper.mapRow(rs);
        suggestion.setWord(word);
        word.setRow(row);
        row.setDocument(document);
        suggestions.add(suggestion);
      }

      return suggestions;
    } catch (SQLException e) {
      LOG.error("Failed to run sql: " + sql, e);
      throw new RuntimeException(e);
    }
  }

  Map<Integer, List<FeedbackSuggestion>> findSuggestions(FeedbackDocument doc) {
    String sql = null;
    try {
      NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
      sql = "SELECT " + SELECT_SUGGESTION + ", " + SELECT_WORD + ", " + SELECT_ROW + ", " + SELECT_DOCUMENT
          + " FROM joc_suggestion" + " INNER JOIN joc_font ON suggest_font_id = font_id"
          + " INNER JOIN joc_language ON suggest_language_id = language_id"
          + " INNER JOIN joc_user ON suggest_user_id = user_id" + " INNER JOIN joc_ip ON suggest_ip_id = ip_id"
          + " INNER JOIN joc_word ON suggest_word_id = word_id" + " INNER JOIN joc_row ON word_row_id = row_id"
          + " INNER JOIN joc_document ON row_doc_id = doc_id" + " WHERE row_doc_id=:row_doc_id"
          + " AND suggest_ignore=:false" + " ORDER BY row_page_index, suggest_id";
      MapSqlParameterSource paramSource = new MapSqlParameterSource();
      paramSource.addValue("row_doc_id", doc.getId());
      paramSource.addValue("false", false);

      LOG.info(sql);
      logParameters(paramSource);

      SqlRowSet rs = jt.queryForRowSet(sql, paramSource);
      SuggestionMapper suggestionMapper = new SuggestionMapper();
      WordMapper wordMapper = new WordMapper();
      FeedbackRowMapper rowMapper = new FeedbackRowMapper();
      DocumentMapper docMapper = new DocumentMapper();

      Map<Integer, List<FeedbackSuggestion>> suggestionMap = new HashMap<>();

      int currentPageIndex = -1;
      List<FeedbackSuggestion> suggestions = null;
      while (rs.next()) {
        int pageIndex = rs.getInt("row_page_index");
        if (pageIndex != currentPageIndex) {
          suggestions = new ArrayList<>();
          suggestionMap.put(pageIndex, suggestions);
          currentPageIndex = pageIndex;
        }

        FeedbackSuggestion suggestion = suggestionMapper.mapRow(rs);
        FeedbackWord word = wordMapper.mapRow(rs);
        FeedbackRow row = rowMapper.mapRow(rs);
        FeedbackDocument document = docMapper.mapRow(rs);
        suggestion.setWord(word);
        word.setRow(row);
        row.setDocument(document);
        suggestions.add(suggestion);
      }

      return suggestionMap;
    } catch (SQLException e) {
      LOG.error("Failed to run sql: " + sql, e);
      throw new RuntimeException(e);
    }
  }

  void saveSuggestion(FeedbackSuggestion suggestion) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    MapSqlParameterSource paramSource = new MapSqlParameterSource();

    // suggest_id, suggest_user_id, suggest_word_id, suggest_font_id,
    // suggest_language_id, suggest_create_date
    // suggest_text, suggest_previous_text, suggest_ignore
    paramSource.addValue("suggest_word_id", suggestion.getWordId());
    paramSource.addValue("suggest_text", suggestion.getText());
    paramSource.addValue("suggest_previous_text", suggestion.getPreviousText());
    paramSource.addValue("suggest_ignore", suggestion.isIgnored());

    String sql = null;

    if (suggestion.isNew()) {
      int ipId = this.getIpId(suggestion.getIp());
      paramSource.addValue("suggest_ip_id", ipId);

      int userId = this.getUserId(suggestion.getUser());
      paramSource.addValue("suggest_user_id", userId);

      int fontId = this.getFontId(suggestion.getFont());
      paramSource.addValue("suggest_font_id", fontId);

      int languageId = this.getLanguageId(suggestion.getLanguage());
      paramSource.addValue("suggest_language_id", languageId);

      sql = "SELECT nextval('joc_suggestion_suggest_id_seq')";
      LOG.debug(sql);
      int suggestionId = jt.queryForObject(sql, paramSource, Integer.class);
      paramSource.addValue("suggest_id", suggestionId);

      sql = "INSERT INTO joc_suggestion (suggest_id, suggest_user_id, suggest_word_id, suggest_font_id, suggest_language_id, suggest_create_date"
          + ", suggest_text, suggest_previous_text, suggest_ignore, suggest_ip_id) "
          + " VALUES (:suggest_id, :suggest_user_id, :suggest_word_id, :suggest_font_id, :suggest_language_id, current_timestamp"
          + ", :suggest_text, :suggest_previous_text, :suggest_ignore, :suggest_ip_id)";

      LOG.debug(sql);
      logParameters(paramSource);
      jt.update(sql, paramSource);

      suggestion.setId(suggestionId);
    } else {
      paramSource.addValue("suggest_id", suggestion.getId());

      sql = "UPDATE joc_suggestion";
      sql += ", suggest_ignore=:suggest_ignore";
      sql += " WHERE suggest_id=:suggest_id";

      LOG.debug(sql);
      logParameters(paramSource);
      jt.update(sql, paramSource);
    }
  }

  private int getIpId(String ipAddress) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT ip_id FROM joc_ip WHERE ip_address=:ip_address";
    LOG.trace(sql);
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    paramSource.addValue("ip_address", ipAddress);
    int ipId = 0;
    try {
      ipId = jt.queryForObject(sql, paramSource, Integer.class);
    } catch (EmptyResultDataAccessException e) {
      // do nothing
    }
    if (ipId == 0) {
      sql = "SELECT nextval('joc_ip_ip_id_seq')";
      LOG.debug(sql);
      ipId = jt.queryForObject(sql, paramSource, Integer.class);
      sql = "INSERT INTO joc_ip (ip_id, ip_address) VALUES (:ip_id, :ip_address)";
      paramSource.addValue("ip_id", ipId);
      LOG.debug(sql);
      logParameters(paramSource);
      jt.update(sql, paramSource);
    }
    return ipId;
  }

  private int getFontId(String fontCode) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT font_id FROM joc_font WHERE font_code=:font_code";
    LOG.trace(sql);
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    paramSource.addValue("font_code", fontCode);
    int fontId = 0;
    try {
      fontId = jt.queryForObject(sql, paramSource, Integer.class);
    } catch (EmptyResultDataAccessException e) {
      // do nothing
    }
    if (fontId == 0) {
      sql = "SELECT nextval('joc_font_font_id_seq')";
      LOG.debug(sql);
      fontId = jt.queryForObject(sql, paramSource, Integer.class);
      sql = "INSERT INTO joc_font (font_id, font_code) VALUES (:font_id, :font_code)";
      paramSource.addValue("font_id", fontId);
      LOG.debug(sql);
      logParameters(paramSource);
      jt.update(sql, paramSource);
    }
    return fontId;
  }

  private int getLanguageId(String languageCode) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT language_id FROM joc_language WHERE language_code=:language_code";
    LOG.trace(sql);
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    paramSource.addValue("language_code", languageCode);
    int languageId = 0;
    try {
      languageId = jt.queryForObject(sql, paramSource, Integer.class);
    } catch (EmptyResultDataAccessException e) {
      // do nothing
    }
    if (languageId == 0) {
      sql = "SELECT nextval('joc_language_language_id_seq')";
      LOG.debug(sql);
      languageId = jt.queryForObject(sql, paramSource, Integer.class);
      sql = "INSERT INTO joc_language (language_id, language_code) VALUES (:language_id, :language_code)";
      paramSource.addValue("language_id", languageId);
      LOG.debug(sql);
      logParameters(paramSource);
      jt.update(sql, paramSource);
    }
    return languageId;
  }

  private int getUserId(String userName) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT user_id FROM joc_user WHERE user_username=:user_username";
    LOG.trace(sql);
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    paramSource.addValue("user_username", userName);
    int userId = 0;
    try {
      userId = jt.queryForObject(sql, paramSource, Integer.class);
    } catch (EmptyResultDataAccessException e) {
      // do nothing
    }
    if (userId == 0) {
      sql = "SELECT nextval('joc_user_user_id_seq')";
      LOG.debug(sql);
      userId = jt.queryForObject(sql, paramSource, Integer.class);
      sql = "INSERT INTO joc_user (user_id, user_username) VALUES (:user_id, :user_username)";
      paramSource.addValue("user_id", userId);
      LOG.debug(sql);
      logParameters(paramSource);
      jt.update(sql, paramSource);
    }
    return userId;
  }

  void loadCriteria() {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT criterion_id, criterion_name FROM joc_criterion";
    LOG.debug(sql);
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    SqlRowSet rs = jt.queryForRowSet(sql, paramSource);
    Set<FeedbackCriterion> criteria = new HashSet<>();
    while (rs.next()) {
      int criterionId = rs.getInt("criterion_id");
      String name = rs.getString("criterion_name");
      FeedbackCriterion criterion = FeedbackCriterion.valueOf(name);
      criteria.add(criterion);
      if (criterion.getId() != criterionId) {
        LOG.error(
            "Bad id for criterion: " + criterion.name() + ". Should be: " + criterion.getId() + ". Is: " + criterionId);
      }
    }

    for (FeedbackCriterion criterion : FeedbackCriterion.values()) {
      if (!criteria.contains(criterion)) {
        sql = "INSERT INTO joc_criterion (criterion_id, criterion_name)" + " VALUES (:criterion_id, :criterion_name)";
        paramSource = new MapSqlParameterSource();
        paramSource.addValue("criterion_id", criterion.getId());
        paramSource.addValue("criterion_name", criterion.name());
        LOG.debug(sql);
        logParameters(paramSource);
        jt.update(sql, paramSource);
      }
    }
  }

  void loadFields() {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT field_id, field_name FROM joc_field";
    LOG.debug(sql);
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    SqlRowSet rs = jt.queryForRowSet(sql, paramSource);
    int maxId = 0;
    while (rs.next()) {
      int fieldId = rs.getInt("field_id");
      String name = rs.getString("field_name");
      JochreIndexField field = JochreIndexField.valueOf(name);
      idFieldMap.put(fieldId, field);
      fieldIdMap.put(field, fieldId);
      if (fieldId > maxId)
        maxId = fieldId;
    }
    maxId += 1;

    for (JochreIndexField field : JochreIndexField.values()) {
      if (!fieldIdMap.containsKey(field)) {
        sql = "INSERT INTO joc_field (field_id, field_name)" + " VALUES (:field_id, :field_name)";
        paramSource = new MapSqlParameterSource();
        paramSource.addValue("field_id", maxId);
        paramSource.addValue("field_name", field.name());
        LOG.debug(sql);
        logParameters(paramSource);
        jt.update(sql, paramSource);

        idFieldMap.put(maxId, field);
        fieldIdMap.put(field, maxId);

        maxId += 1;
      }
    }
  }

  protected final class SuggestionMapper implements RowMapper<FeedbackSuggestion> {
    @Override
    public FeedbackSuggestion mapRow(ResultSet rs, int rowNum) throws SQLException {
      return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
    }

    public FeedbackSuggestion mapRow(SqlRowSet rs) {
      FeedbackSuggestion suggestion = new FeedbackSuggestion(configId);
      // suggest_id, suggest_user_id, suggest_word_id, suggest_font_id,
      // suggest_language_id, suggest_create_date
      // suggest_text, suggest_previous_text,
      // suggest_ignore
      suggestion.setId(rs.getInt("suggest_id"));
      suggestion.setUser(rs.getString("user_username"));
      suggestion.setWordId(rs.getInt("suggest_word_id"));
      suggestion.setFont(rs.getString("font_code"));
      suggestion.setLanguage(rs.getString("language_code"));
      suggestion.setCreateDate(rs.getTimestamp("suggest_create_date"));
      suggestion.setText(rs.getString("suggest_text"));
      suggestion.setPreviousText(rs.getString("suggest_previous_text"));
      suggestion.setIgnored(rs.getBoolean("suggest_ignore"));
      suggestion.setIp(rs.getString("ip_address"));

      return suggestion;
    }
  }

  FeedbackQuery loadQuery(int queryId) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT " + SELECT_QUERY + "," + SELECT_CLAUSE + " FROM joc_query"
        + " INNER JOIN joc_user ON query_user_id = user_id" + " INNER JOIN joc_ip ON query_ip_id = ip_id"
        + " INNER JOIN joc_clause ON query_id = clause_query_id" + " WHERE query_id=:query_id";
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    paramSource.addValue("query_id", queryId);

    LOG.debug(sql);
    logParameters(paramSource);
    SqlRowSet rs = jt.queryForRowSet(sql, paramSource);
    QueryMapper queryMapper = new QueryMapper();
    FeedbackQuery query = null;
    while (rs.next()) {
      if (query == null)
        query = queryMapper.mapRow(rs);
      FeedbackCriterion criterion = FeedbackCriterion.forId(rs.getInt("clause_criterion_id"));
      String text = rs.getString("clause_text");
      query.addClause(criterion, text);
    }

    if (query == null) {
      throw new EntityNotFoundException(FeedbackQuery.class, queryId);
    }
    return query;
  }

  void saveQuery(FeedbackQuery query) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    MapSqlParameterSource paramSource = new MapSqlParameterSource();

    // query_id, query_user_id, query_ip_id, query_date, query_results
    // clause_query_id, clause_criterion_id, clause_text, criterion_name
    paramSource.addValue("query_results", query.getResultCount());

    String sql = null;

    if (query.isNew()) {
      int ipId = this.getIpId(query.getIp());
      paramSource.addValue("query_ip_id", ipId);

      int userId = this.getUserId(query.getUser());
      paramSource.addValue("query_user_id", userId);

      sql = "SELECT nextval('joc_query_query_id_seq')";
      LOG.debug(sql);
      int queryId = jt.queryForObject(sql, paramSource, Integer.class);
      paramSource.addValue("query_id", queryId);

      sql = "INSERT INTO joc_query (query_id, query_user_id, query_ip_id, query_date, query_results) "
          + " VALUES (:query_id, :query_user_id, :query_ip_id, current_timestamp, :query_results)";

      LOG.debug(sql);
      logParameters(paramSource);
      jt.update(sql, paramSource);

      query.setId(queryId);

      for (FeedbackCriterion criterion : query.getClauses().keySet()) {
        String text = query.getClauses().get(criterion);
        sql = "INSERT INTO joc_clause (clause_query_id, clause_criterion_id, clause_text) "
            + " VALUES (:clause_query_id, :clause_criterion_id, :clause_text)";
        paramSource = new MapSqlParameterSource();
        paramSource.addValue("clause_query_id", queryId);
        paramSource.addValue("clause_criterion_id", criterion.getId());
        paramSource.addValue("clause_text", text);
        LOG.debug(sql);
        logParameters(paramSource);
        jt.update(sql, paramSource);
      }
    }
  }

  protected final class QueryMapper implements RowMapper<FeedbackQuery> {

    @Override
    public FeedbackQuery mapRow(ResultSet rs, int rowNum) throws SQLException {
      return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
    }

    public FeedbackQuery mapRow(SqlRowSet rs) {
      FeedbackQuery query = new FeedbackQuery(configId);
      // query_id, query_user_id, query_ip_id, query_date, query_results,
      // user_username, ip_address
      query.setId(rs.getInt("query_id"));
      query.setUser(rs.getString("user_username"));
      query.setIp(rs.getString("ip_address"));
      query.setDate(rs.getDate("query_date"));
      query.setResultCount(rs.getInt("query_results"));

      return query;
    }
  }

  FeedbackDocument loadDocument(int documentId) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT " + SELECT_DOCUMENT + " FROM joc_document WHERE doc_id=:doc_id";
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    paramSource.addValue("doc_id", documentId);

    LOG.debug(sql);
    logParameters(paramSource);
    FeedbackDocument document = null;
    try {
      document = jt.queryForObject(sql, paramSource, new DocumentMapper());
    } catch (EmptyResultDataAccessException ex) {
      throw new EntityNotFoundException(FeedbackDocument.class, documentId);
    }
    return document;
  }

  FeedbackDocument findDocument(String path) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT " + SELECT_DOCUMENT + " FROM joc_document WHERE doc_path=:doc_path";
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    paramSource.addValue("doc_path", path);

    LOG.debug(sql);
    logParameters(paramSource);
    FeedbackDocument doc = null;
    try {
      doc = jt.queryForObject(sql, paramSource, new DocumentMapper());
    } catch (EmptyResultDataAccessException ex) {
      ex.hashCode();
    }
    return doc;
  }

  void saveDocument(FeedbackDocument doc) {
    // note: no update
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    MapSqlParameterSource paramSource = new MapSqlParameterSource();

    // doc_id, doc_code
    paramSource.addValue("doc_path", doc.getPath());

    String sql = null;

    if (doc.isNew()) {
      sql = "SELECT nextval('joc_document_doc_id_seq')";
      LOG.debug(sql);
      int docId = jt.queryForObject(sql, paramSource, Integer.class);
      paramSource.addValue("doc_id", docId);

      sql = "INSERT INTO joc_document (doc_id, doc_path) " + " VALUES (:doc_id, :doc_path)";

      LOG.debug(sql);
      logParameters(paramSource);
      jt.update(sql, paramSource);

      doc.setId(docId);
    }
  }

  protected final class DocumentMapper implements RowMapper<FeedbackDocument> {
    @Override
    public FeedbackDocument mapRow(ResultSet rs, int rowNum) throws SQLException {
      return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
    }

    public FeedbackDocument mapRow(SqlRowSet rs) {
      FeedbackDocument document = new FeedbackDocument(configId);
      // doc_id, doc_path
      document.setId(rs.getInt("doc_id"));
      document.setPath(rs.getString("doc_path"));

      return document;
    }
  }

  Correction loadCorrection(int correctionId) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT " + SELECT_CORRECTION + ", " + SELECT_DOCUMENT + " FROM joc_correction";
    sql += " INNER JOIN joc_user ON cor_user_id = user_id";
    sql += " INNER JOIN joc_ip ON cor_ip_id = ip_id";
    sql += " INNER JOIN joc_document ON cor_doc_id = doc_id";
    sql += " WHERE cor_id=:cor_id";
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    paramSource.addValue("cor_id", correctionId);

    LOG.debug(sql);
    logParameters(paramSource);
    Correction correction = null;

    SqlRowSet rs = jt.queryForRowSet(sql, paramSource);

    if (rs.next()) {
      CorrectionMapper correctionMapper = new CorrectionMapper();
      DocumentMapper documentMapper = new DocumentMapper();
      correction = correctionMapper.mapRow(rs);
      FeedbackDocument document = documentMapper.mapRow(rs);
      correction.setDocument(document);
    } else {
      throw new EntityNotFoundException(Correction.class, correctionId);
    }
    return correction;
  }

  /**
   * Return all corrections to apply to a specific document path, including all
   * general corrections, in the order in which they were added.
   */
  Map<JochreIndexField, List<Correction>> findCorrections(String docPath) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    String sql = "SELECT " + SELECT_CORRECTION + ", " + SELECT_DOCUMENT + " FROM joc_correction";
    sql += " INNER JOIN joc_user ON cor_user_id = user_id";
    sql += " INNER JOIN joc_ip ON cor_ip_id = ip_id";
    sql += " INNER JOIN joc_document ON cor_doc_id = doc_id";
    sql += " INNER JOIN joc_field ON cor_field_id = field_id";
    sql += " WHERE (doc_path=:doc_path OR cor_apply_everywhere=:true)";
    sql += " AND cor_ignore=:false";
    sql += " ORDER BY cor_field_id, cor_id ASC";
    MapSqlParameterSource paramSource = new MapSqlParameterSource();
    paramSource.addValue("true", true);
    paramSource.addValue("false", false);
    paramSource.addValue("doc_path", docPath);

    LOG.debug(sql);
    logParameters(paramSource);

    SqlRowSet rs = jt.queryForRowSet(sql, paramSource);

    Map<JochreIndexField, List<Correction>> corrections = new HashMap<>();

    CorrectionMapper correctionMapper = new CorrectionMapper();
    DocumentMapper documentMapper = new DocumentMapper();

    JochreIndexField currentField = null;
    List<Correction> fieldCorrections = null;
    while (rs.next()) {
      Correction correction = correctionMapper.mapRow(rs);
      FeedbackDocument document = documentMapper.mapRow(rs);
      correction.setDocument(document);
      JochreIndexField field = correction.getField();
      if (currentField == null || currentField != field) {
        fieldCorrections = new ArrayList<>();
        corrections.put(field, fieldCorrections);
        currentField = field;
      }
      fieldCorrections.add(correction);
    }
    return corrections;
  }

  void saveCorrection(Correction correction) {
    NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(dataSource);
    MapSqlParameterSource paramSource = new MapSqlParameterSource();

    String sql = null;
    paramSource.addValue("cor_ignore", correction.isIgnore());
    paramSource.addValue("cor_sent", correction.isSent());

    if (correction.isNew()) {
      paramSource.addValue("cor_doc_id", correction.getDocumentId());
      paramSource.addValue("cor_field_id", fieldIdMap.get(correction.getField()));

      int ipId = this.getIpId(correction.getIp());
      paramSource.addValue("cor_ip_id", ipId);

      int userId = this.getUserId(correction.getUser());
      paramSource.addValue("cor_user_id", userId);

      paramSource.addValue("cor_apply_everywhere", correction.isApplyEverywhere());
      paramSource.addValue("cor_value", correction.getValue());
      paramSource.addValue("cor_value_before", correction.getPreviousValue());

      sql = "SELECT nextval('joc_correction_cor_id_seq')";
      LOG.debug(sql);
      int correctionId = jt.queryForObject(sql, paramSource, Integer.class);
      paramSource.addValue("cor_id", correctionId);

      sql = "INSERT INTO joc_correction (cor_id, cor_doc_id, cor_field_id, cor_user_id, cor_value, cor_value_before";
      sql += ", cor_date, cor_apply_everywhere, cor_ip_id, cor_ignore, cor_sent)";
      sql += " VALUES (:cor_id, :cor_doc_id, :cor_field_id, :cor_user_id, :cor_value, :cor_value_before";
      sql += ", current_timestamp, :cor_apply_everywhere, :cor_ip_id, :cor_ignore, :cor_sent)";

      LOG.debug(sql);
      logParameters(paramSource);
      jt.update(sql, paramSource);

      correction.setId(correctionId);
    } else {
      sql = "UPDATE joc_correction";
      sql += " SET cor_ignore = :cor_ignore";
      sql += ", cor_sent = :cor_sent";
      if (correction.getDocuments() != null) {
        sql += ", cor_documents = '{";
        sql += "\"" + String.join("\", \"", correction.getDocuments()) + "\"";
        sql += "}'";
      }
      sql += " WHERE cor_id = :cor_id";

      paramSource.addValue("cor_id", correction.getId());

      LOG.debug(sql);
      logParameters(paramSource);
      jt.update(sql, paramSource);
    }
  }

  protected final class CorrectionMapper implements RowMapper<Correction> {
    @Override
    public Correction mapRow(ResultSet rs, int rowNum) throws SQLException {
      return this.mapRow(new ResultSetWrappingSqlRowSet(rs));
    }

    public Correction mapRow(SqlRowSet rs) {
      // cor_id, cor_doc_id, cor_field_id, cor_user_id, cor_value, cor_value_before,
      // cor_date, cor_apply_everywhere, cor_ip_id

      int id = rs.getInt("cor_id");
      int docId = rs.getInt("cor_doc_id");
      JochreIndexField field = idFieldMap.get(rs.getInt("cor_field_id"));
      String user = rs.getString("user_username");
      String value = rs.getString("cor_value");
      String previousValue = rs.getString("cor_value_before");
      Date createDate = rs.getTimestamp("cor_date");
      boolean applyEverywhere = rs.getBoolean("cor_apply_everywhere");
      String ip = rs.getString("ip_address");

      Correction correction = new Correction(docId, field, user, ip, value, previousValue, applyEverywhere, createDate,
          configId);
      correction.setId(id);

      correction.setIgnore(rs.getBoolean("cor_ignore"));
      correction.setSent(rs.getBoolean("cor_sent"));

      Array docArray = (Array) rs.getObject("cor_documents");
      if (!rs.wasNull()) {
        Object[] documents;
        try {
          documents = (Object[]) docArray.getArray();
        } catch (SQLException e) {
          throw new JochreException("Exception reading document array", e);
        }

        List<String> docList = new ArrayList<>(documents.length);
        for (Object obj : documents) {
          String doc = (String) obj;
          docList.add(doc);
        }
        correction.setDocuments(docList);
      }
      return correction;
    }
  }

  static void logParameters(MapSqlParameterSource paramSource) {
    DaoUtils.LogParameters(paramSource.getValues());
  }

}
