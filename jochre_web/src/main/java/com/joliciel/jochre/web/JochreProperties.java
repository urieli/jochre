package com.joliciel.jochre.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.joliciel.jochre.JochreSession;
import com.typesafe.config.ConfigFactory;

public class JochreProperties {
  private static final Logger LOG = LoggerFactory.getLogger(JochreProperties.class);
  private static JochreProperties instance;
  private final JochreSession jochreSession;

  public static JochreProperties getInstance() {
    if (instance == null) {
      instance = new JochreProperties();
    }
    return instance;
  }

  private JochreProperties() {
    try {
      LOG.info("config.file: " + System.getProperty("config.file"));
      jochreSession = new JochreSession(ConfigFactory.load());
    } catch (ReflectiveOperationException e) {
      LOG.error("Failure in JochreProperties$construct", e);
      throw new RuntimeException(e);
    }
  }

  public JochreSession getJochreSession() {
    return jochreSession;
  }
}
