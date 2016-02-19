package com.joliciel.jochre.search.web;

import java.io.File;

import javax.imageio.ImageIO;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.search.JochreIndexSearcher;
import com.joliciel.jochre.search.SearchService;
import com.joliciel.jochre.search.SearchServiceLocator;
import com.joliciel.jochre.search.lexicon.Lexicon;
import com.joliciel.jochre.search.lexicon.LexiconService;
import com.joliciel.jochre.search.lexicon.LexiconServiceLocator;

public class JochreSetupListener implements ServletContextListener {
	private static final Log LOG = LogFactory.getLog(JochreSetupListener.class);

	public JochreSetupListener() {
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		long startTime = System.currentTimeMillis();
		try {
			ImageIO.scanForPlugins();
			
			JochreSearchProperties props = JochreSearchProperties.getInstance(servletContextEvent.getServletContext());
			SearchServiceLocator searchServiceLocator = SearchServiceLocator.getInstance(props.getLocale());
			SearchService searchService = searchServiceLocator.getSearchService();
			String lexiconPath = props.getLexiconPath();
			if (lexiconPath!=null && searchService.getLexicon()==null) {
				LOG.debug("Loading lexicon");
				LexiconServiceLocator lexiconServiceLocator = LexiconServiceLocator.getInstance(searchServiceLocator);
				LexiconService lexiconService = lexiconServiceLocator.getLexiconService();
				File lexiconFile = new File(lexiconPath);
				Lexicon lexicon = lexiconService.deserializeLexicon(lexiconFile);
				searchService.setLexicon(lexicon);
			}
			
			LOG.debug("Creating searcher");
			String indexDirPath = props.getIndexDirPath();
			File indexDir = new File(indexDirPath);
			LOG.debug("Index dir: " + indexDir.getAbsolutePath());
			@SuppressWarnings("unused")
			JochreIndexSearcher searcher = searchService.getJochreIndexSearcher(indexDir);
		} finally {
			long duration = System.currentTimeMillis() - startTime;
			LOG.info(this.getClass().getSimpleName() + ".contextInitialized Duration: " + duration);
		}
	}

}
