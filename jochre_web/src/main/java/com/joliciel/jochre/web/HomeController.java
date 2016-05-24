package com.joliciel.jochre.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Html;
import org.zkoss.zul.Window;

public class HomeController extends GenericForwardComposer<Window> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1664468221173319777L;

	private static final Logger LOG = LoggerFactory.getLogger(HomeController.class);

	Window winJochreHome;
	Button btnLogin;
	Html htmlContent;

	public HomeController() {
	}

	@Override
	public void doAfterCompose(Window comp) throws Exception {
		LOG.debug("HomeController.doAfterCompose");
		super.doAfterCompose(comp);
		htmlContent.setContent(JochreProperties.getInstance().getWelcomeText());
	}

	public void onClick$btnLogin(Event event) {
		try {
			LOG.debug("onClick$btnLogin");

			Executions.sendRedirect("login.zul");
		} catch (Exception e) {
			LOG.error("Failure in onClick$btnLogin", e);
			throw new RuntimeException(e);
		}
	}
}
