package com.joliciel.jochre.search.web;

import javax.imageio.ImageIO;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochreSearchConfig;
import com.joliciel.jochre.search.JochreSearchManager;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class JochreSetupListener implements ServletContextListener {
  private static final Logger LOG = LoggerFactory.getLogger(JochreSetupListener.class);

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

      Config config = ConfigFactory.load();
      String configId = config.getString("jochre.search.webapp.config-id");
      JochreSearchConfig searchConfig = JochreSearchConfig.getInstance(configId);

      LOG.info("Content dir: " + searchConfig.getContentDir().getAbsolutePath());

      // preload the lexicon
      searchConfig.getLexicon();

      // preload the search manager
      JochreSearchManager.getInstance(configId);
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      LOG.info(this.getClass().getSimpleName() + ".contextInitialized Duration: " + duration);
    }
  }

}
