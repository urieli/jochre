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
package com.joliciel.jochre.doc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochreDocumentInternal;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.doc.JochrePageInternal;
import com.joliciel.talismane.utils.DaoUtils;

public final class DocumentDaoJdbc implements DocumentDao {
	private static final Log LOG = LogFactory.getLog(DocumentDaoJdbc.class);
	DocumentServiceInternal documentServiceInternal;
	private DataSource dataSource;

	private static final String SELECT_DOCUMENT = "doc_id, doc_filename, doc_name, doc_locale, doc_owner_id" +
			", doc_name_local, doc_publisher, doc_city, doc_year, doc_reference";
	private static final String SELECT_PAGE = "page_id, page_doc_id, page_index";
	private static final String SELECT_AUTHOR = "author_id, author_first_name, author_last_name, author_first_name_local, author_last_name_local";


	@Override
	public JochrePage loadJochrePage(int jochrePageId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_PAGE + " FROM ocr_page WHERE page_id=:page_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("page_id", jochrePageId);

		LOG.info(sql);
		logParameters(paramSource);
		JochrePage jochrePage = null;
		try {
			jochrePage = (JochrePage)  jt.queryForObject(sql, paramSource, new JochrePageMapper(this.getDocumentServiceInternal()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return jochrePage;
	}
	
	@Override
	public List<JochrePage> findJochrePages(JochreDocument jochreDocument) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_PAGE + " FROM ocr_page WHERE page_doc_id=:page_doc_id" +
        		" ORDER BY page_index";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("page_doc_id", jochreDocument.getId());
       
        LOG.info(sql);
        logParameters(paramSource);
        @SuppressWarnings("unchecked")
        List<JochrePage> jochrePages = jt.query(sql, paramSource, new JochrePageMapper(this.getDocumentServiceInternal()));
       
        return jochrePages;
	}

	protected static final class JochrePageMapper implements RowMapper {
		private DocumentServiceInternal graphicsService;

		protected JochrePageMapper(DocumentServiceInternal graphicsService) {
			this.graphicsService = graphicsService;
		}
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			JochrePageInternal jochrePage = graphicsService.getEmptyJochrePageInternal();
			jochrePage.setId(rs.getInt("page_id"));
			jochrePage.setDocumentId(rs.getInt("page_doc_id"));
			jochrePage.setIndex(rs.getInt("page_index"));
			return jochrePage;
		}
	}
	
	@Override
	public void saveJochrePage(JochrePage jochrePage) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		JochrePageInternal iJochrePage = (JochrePageInternal) jochrePage;

		paramSource.addValue("page_doc_id", jochrePage.getDocumentId());
		paramSource.addValue("page_index", jochrePage.getIndex());
		String sql = null;

		if (jochrePage.isNew()) {
			sql = "SELECT nextval('ocr_page_id_seq')";
			LOG.info(sql);
			int jochrePageId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("page_id", jochrePageId);

			sql = "INSERT INTO ocr_page (page_id, page_doc_id, page_index) " +
			"VALUES (:page_id, :page_doc_id, :page_index)";

			LOG.info(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			iJochrePage.setId(jochrePageId);
		} else {
			paramSource.addValue("page_id", jochrePage.getId());

			sql = "UPDATE ocr_page" +
			" SET page_doc_id = :page_doc_id" +
			", page_index = :page_index" +
			" WHERE page_id = :page_id";

			LOG.info(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);
		}
	}
	
	
	
	@Override
	public void deleteJochrePage(JochrePage page) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("page_id", page.getId());
		String sql = null;
		
		sql = "delete from ocr_page" +
			" WHERE page_id = :page_id";

		LOG.info(sql);
		logParameters(paramSource);
		jt.update(sql, paramSource);
	}

	@Override
	public JochreDocument loadJochreDocument(int jochreDocumentId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_DOCUMENT + " FROM ocr_document WHERE doc_id=:doc_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("doc_id", jochreDocumentId);

		LOG.info(sql);
		logParameters(paramSource);
		JochreDocument jochreDocument = null;
		try {
			jochreDocument = (JochreDocument)  jt.queryForObject(sql, paramSource, new JochreDocumentMapper(this.getDocumentServiceInternal()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return jochreDocument;
	}


	@Override
	public List<JochreDocument> findDocuments() {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_DOCUMENT + " FROM ocr_document ORDER BY doc_name";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		LOG.info(sql);
		logParameters(paramSource);
		@SuppressWarnings("unchecked")
		List<JochreDocument> documents =  jt.query(sql, paramSource, new JochreDocumentMapper(this.getDocumentServiceInternal()));

		return documents;
	}
	
	protected static final class JochreDocumentMapper implements RowMapper {
		private DocumentServiceInternal graphicsService;

		protected JochreDocumentMapper(DocumentServiceInternal graphicsService) {
			this.graphicsService = graphicsService;
		}
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			JochreDocumentInternal jochreDocument = graphicsService.getEmptyJochreDocumentInternal();
			jochreDocument.setId(rs.getInt("doc_id"));
			jochreDocument.setFileName(rs.getString("doc_filename"));
			jochreDocument.setName(rs.getString("doc_name"));
			jochreDocument.setOwnerId(rs.getInt("doc_owner_id"));
            if (rs.getObject("doc_name_local")!=null)
            	jochreDocument.setNameLocal(rs.getString("doc_name_local"));
            if (rs.getObject("doc_publisher")!=null)
            	jochreDocument.setPublisher(rs.getString("doc_publisher"));
            if (rs.getObject("doc_city")!=null)
            	jochreDocument.setCity(rs.getString("doc_city"));
            if (rs.getObject("doc_year")!=null)
            	jochreDocument.setYear(rs.getInt("doc_year"));
            if (rs.getObject("doc_reference")!=null)
            	jochreDocument.setReference(rs.getString("doc_reference"));
			
			Locale locale = new Locale(rs.getString("doc_locale"));
			jochreDocument.setLocale(locale);
			return jochreDocument;
		}
	}
	
	@Override
	public void saveJochreDocument(JochreDocument jochreDocument) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		JochreDocumentInternal iJochreDocument = (JochreDocumentInternal) jochreDocument;

		paramSource.addValue("doc_filename", jochreDocument.getFileName());
		paramSource.addValue("doc_name", jochreDocument.getName());
		paramSource.addValue("doc_owner_id", jochreDocument.getOwnerId());
		paramSource.addValue("doc_locale", jochreDocument.getLocale().getLanguage());
		paramSource.addValue("doc_name_local", jochreDocument.getNameLocal());
		paramSource.addValue("doc_publisher", jochreDocument.getPublisher());
		paramSource.addValue("doc_city", jochreDocument.getCity());
		paramSource.addValue("doc_year", jochreDocument.getYear());
		paramSource.addValue("doc_reference", jochreDocument.getReference());
		
		String sql = null;

		if (jochreDocument.isNew()) {
			sql = "SELECT nextval('ocr_doc_id_seq')";
			LOG.info(sql);
			int jochreDocumentId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("doc_id", jochreDocumentId);

			sql = "INSERT INTO ocr_document (doc_id, doc_filename, doc_name, doc_locale, doc_owner_id" +
					", doc_name_local, doc_publisher, doc_city, doc_year, doc_reference) " +
					"VALUES (:doc_id, :doc_filename, :doc_name, :doc_locale, :doc_owner_id" +
					", :doc_name_local, :doc_publisher, :doc_city, :doc_year, :doc_reference)";

			LOG.info(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			iJochreDocument.setId(jochreDocumentId);
		} else {
			paramSource.addValue("doc_id", jochreDocument.getId());

			sql = "UPDATE ocr_document" +
			" SET doc_filename = :doc_filename" +
			", doc_name = :doc_name" +
			", doc_locale = :doc_locale" +
			", doc_owner_id = :doc_owner_id" +
			", doc_name_local = :doc_name_local" +
			", doc_publisher = :doc_publisher" +
			", doc_city = :doc_city" +
			", doc_year = :doc_year" +
			", doc_reference = :doc_reference" +
			" WHERE doc_id = :doc_id";

			LOG.info(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);
		}
	}
	

	@Override
	public Author loadAuthor(int authorId) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		String sql = "SELECT " + SELECT_AUTHOR + " FROM ocr_author WHERE author_id=:author_id";
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("author_id", authorId);

		LOG.info(sql);
		logParameters(paramSource);
		Author author = null;
		try {
			author = (Author)  jt.queryForObject(sql, paramSource, new AuthorMapper(this.getDocumentServiceInternal()));
		} catch (EmptyResultDataAccessException ex) {
			ex.hashCode();
		}
		return author;
	}
	
	@Override
	public List<Author> findAuthors() {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
	    String sql = "SELECT " + SELECT_AUTHOR + " FROM ocr_author" +
	    	" ORDER BY author_last_name, author_first_name";
	    MapSqlParameterSource paramSource = new MapSqlParameterSource();
	   
	    LOG.info(sql);
	    logParameters(paramSource);
	    @SuppressWarnings("unchecked")
	    List<Author> authors = jt.query(sql, paramSource, new AuthorMapper(this.getDocumentServiceInternal()));
	   
	    return authors;
     }

	@Override
	public List<Author> findAuthors(JochreDocument jochreDocument) {
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
        String sql = "SELECT " + SELECT_AUTHOR + " FROM ocr_author" +
        		" INNER JOIN ocr_doc_author_map ON docauthor_author_id = author_id" +
        		" WHERE docauthor_doc_id=:docauthor_doc_id" +
        		" ORDER BY author_last_name, author_first_name";
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("docauthor_doc_id", jochreDocument.getId());
       
        LOG.info(sql);
        logParameters(paramSource);
        @SuppressWarnings("unchecked")
        List<Author> authors = jt.query(sql, paramSource, new AuthorMapper(this.getDocumentServiceInternal()));
       
        return authors;
	}

	protected static final class AuthorMapper implements RowMapper {
		private DocumentServiceInternal graphicsService;

		protected AuthorMapper(DocumentServiceInternal graphicsService) {
			this.graphicsService = graphicsService;
		}
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			AuthorInternal author = graphicsService.getEmptyAuthorInternal();
			author.setId(rs.getInt("author_id"));
			author.setFirstName(rs.getString("author_first_name"));
			author.setLastName(rs.getString("author_last_name"));
			author.setFirstNameLocal(rs.getString("author_first_name_local"));
			author.setLastNameLocal(rs.getString("author_last_name_local"));
			return author;
		}
	}
	

	@Override
	public void saveAuthor(Author author) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		AuthorInternal iAuthor = (AuthorInternal) author;

		paramSource.addValue("author_first_name", author.getFirstName());
		paramSource.addValue("author_last_name", author.getLastName());
		paramSource.addValue("author_first_name_local", author.getFirstNameLocal());
		paramSource.addValue("author_last_name_local", author.getLastNameLocal());
		String sql = null;

		if (author.isNew()) {
			sql = "SELECT nextval('ocr_author_id_seq')";
			LOG.info(sql);
			int authorId = jt.queryForInt(sql, paramSource);
			paramSource.addValue("author_id", authorId);

			sql = "INSERT INTO ocr_author (author_id, author_first_name, author_last_name" +
					", author_first_name_local, author_last_name_local) " +
					"VALUES (:author_id, :author_first_name, :author_last_name" +
					", :author_first_name_local, :author_last_name_local)";

			LOG.info(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);

			iAuthor.setId(authorId);
		} else {
			paramSource.addValue("author_id", author.getId());

			sql = "UPDATE ocr_author" +
				" SET author_first_name = :author_first_name" +
				", author_last_name = :author_last_name" +
				", author_first_name_local = :author_first_name_local" +
				", author_last_name_local = :author_last_name_local" +
				" WHERE author_id = :author_id";

			LOG.info(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);
		}
	}
	
	
	@Override
	public void replaceAuthors(JochreDocument doc) {
		NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(this.getDataSource());
		MapSqlParameterSource paramSource = new MapSqlParameterSource();

		paramSource.addValue("docauthor_doc_id", doc.getId());
		String sql = "DELETE FROM ocr_doc_author_map WHERE docauthor_doc_id = :docauthor_doc_id";
		LOG.info(sql);
		logParameters(paramSource);
		jt.update(sql, paramSource);

		for (Author author : doc.getAuthors()) {
			paramSource = new MapSqlParameterSource();

			paramSource.addValue("docauthor_doc_id", doc.getId());
			paramSource.addValue("docauthor_author_id", author.getId());
			
			sql = "INSERT INTO ocr_doc_author_map (docauthor_doc_id, docauthor_author_id)" +
					" VALUES (:docauthor_doc_id, :docauthor_author_id)";
			
			LOG.info(sql);
			logParameters(paramSource);
			jt.update(sql, paramSource);			
		}
	}

	public DocumentServiceInternal getDocumentServiceInternal() {
		return documentServiceInternal;
	}

	public void setDocumentServiceInternal(
			DocumentServiceInternal documentServiceInternal) {
		this.documentServiceInternal = documentServiceInternal;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
    @SuppressWarnings("unchecked")
    public static void logParameters(MapSqlParameterSource paramSource) {
       DaoUtils.LogParameters(paramSource.getValues());
    }
}
