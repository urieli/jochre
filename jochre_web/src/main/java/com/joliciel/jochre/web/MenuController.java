package com.joliciel.jochre.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Toolbar;

import com.joliciel.jochre.security.User;

public class MenuController extends GenericForwardComposer<Panel> {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(MenuController.class);

	@Wire
	Panel panelMenu;
	@Wire
	Label lblName;
	@Wire
	Label lblCorpusName;
	@Wire
	Toolbar toolbar1;

	public MenuController() {
	}

	@Override
	public void doAfterCompose(Panel panel) throws Exception {
		super.doAfterCompose(panel);
		LOG.debug("MenuController");
		Session session = Sessions.getCurrent();
		User currentUser = (User) session.getAttribute(LoginController.SESSION_JOCHRE_USER);
		if (currentUser != null) {
			lblName.setValue(currentUser.getFirstName() + " " + currentUser.getLastName());
			toolbar1.setVisible(true);
		} else {
			lblName.setValue("");
			toolbar1.setVisible(false);
		}
		lblCorpusName.setValue(JochreProperties.getInstance().getProperties().getProperty("corpusName"));
	}

	@Listen("onClick = #btnLogout")
	public void onClick$btnLogout(Event event) {
		LOG.debug("onClick$btnLogout");
		Session session = Sessions.getCurrent();
		session.removeAttribute(LoginController.SESSION_JOCHRE_USER);
		Executions.sendRedirect("index.zul");
	}
}
