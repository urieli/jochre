package com.joliciel.jochre.search;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.lexicon.Lexicon;
import com.typesafe.config.Config;
import com.zaxxer.hikari.HikariDataSource;

public class JochreSearchConfig {
	private static final Logger LOG = LoggerFactory.getLogger(JochreSearchConfig.class);
	private static final Set<String> RTL = new HashSet<>(Arrays.asList(new String[] { "ar", "dv", "fa", "ha", "he", "iw", "ji", "ps", "ur", "yi" }));

	private final Config config;
	private final String configId;
	private final Locale locale;
	private final File contentDir;

	public JochreSearchConfig(String configId, Config config) {
		this.configId = configId;
		this.config = config.getConfig("jochre.search." + configId);
		this.locale = Locale.forLanguageTag(this.config.getString("locale"));
		this.contentDir = new File(this.config.getString("content-dir"));
	}

	public boolean isLeftToRight() {
		return !RTL.contains(locale.getLanguage());
	}

	public TokenFilter getQueryTokenFilter(TokenStream input) {
		TokenFilter tokenFilter = null;
		if (config.hasPath("query-token-filter.class")) {
			try {
				String className = config.getString("query-token-filter.class");

				@SuppressWarnings("unchecked")
				Class<? extends TokenFilter> clazz = (Class<? extends TokenFilter>) Class.forName(className);
				Constructor<? extends TokenFilter> cons = clazz.getConstructor(TokenStream.class);

				tokenFilter = cons.newInstance(input);
			} catch (ReflectiveOperationException e) {
				LOG.error("Unable to construct TokenFilter", e);
				throw new RuntimeException(e);
			}
		}
		return tokenFilter;
	}

	public File getContentDir() {
		return contentDir;
	}

	public File getLexiconFile() {
		String lexiconFilePath = config.getString("lexicon");
		File lexiconFile = new File(lexiconFilePath);
		return lexiconFile;
	}

	public Lexicon getLexicon() {
		Lexicon lexicon = null;
		if (config.hasPath("lexicon")) {
			File lexiconFile = this.getLexiconFile();
			lexicon = Lexicon.deserializeLexicon(lexiconFile);
		}
		return lexicon;
	}

	public Locale getLocale() {
		return locale;
	}

	public Config getConfig() {
		return config;
	}

	public String getConfigId() {
		return configId;
	}

	public boolean hasDatabase() {
		return config.hasPath("jdbc.url");
	}

	public DataSource getDataSource() {
		Config jdbcConfig = config.getConfig("jdbc");
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
		dataSource.setPoolName("HikariPool-" + configId);

		return dataSource;
	}

}
