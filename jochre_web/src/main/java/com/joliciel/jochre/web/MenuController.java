package com.joliciel.jochre.web;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Toolbar;

import com.joliciel.jochre.security.User;

public class MenuController extends GenericForwardComposer<Panel> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1664468221173319777L;


	private static final Log LOG = LogFactory.getLog(MenuController.class);

	Panel panelMenu;
	Label lblName;
	Label lblCorpusName;
	Toolbar toolbar1;
	
	public MenuController() {
	}
	
	public void doAfterCompose(Panel panel) throws Exception {
		super.doAfterCompose(panel);
		LOG.debug("MenuController");
		Session session = Sessions.getCurrent();
		User currentUser = (User) session.getAttribute(LoginController.SESSION_JOCHRE_USER);
		if (currentUser!=null) {
			lblName.setValue(currentUser.getFirstName() + " " + currentUser.getLastName());
			toolbar1.setVisible(true);
		} else {
			lblName.setValue("");
			toolbar1.setVisible(false);
		}
		lblCorpusName.setValue(JochreProperties.getInstance().getProperties().getProperty("corpusName"));
	}
	
    public void onClick$btnLogout(Event event) {
    	LOG.debug("onClick$btnLogout");
		Session session = Sessions.getCurrent();
		session.removeAttribute(LoginController.SESSION_JOCHRE_USER);
		Executions.sendRedirect("index.zul");
    }
}
