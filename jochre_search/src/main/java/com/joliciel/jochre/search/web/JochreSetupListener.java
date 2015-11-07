package com.joliciel.jochre.search.web;

import javax.imageio.ImageIO;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class JochreSetupListener implements ServletContextListener {

	public JochreSetupListener() {
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		ImageIO.scanForPlugins();
	}

}
