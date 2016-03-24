package com.joliciel.jochre.web;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.SimpleConstraint;
import org.zkoss.zul.Window;
import org.zkoss.zul.Textbox;

import com.joliciel.jochre.JochreServiceLocator;
import com.joliciel.jochre.security.User;
import com.joliciel.talismane.utils.LogUtils;

public class ProfileController extends GenericForwardComposer<Div> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1664468221173319777L;


	private static final Log LOG = LogFactory.getLog(ProfileController.class);
	
	private JochreServiceLocator locator = null;

	Window winProfile;
	Label lblUsername;
	Textbox txtPassword;
	Textbox txtPassword2;
	Textbox txtFirstName;
	Textbox txtLastName;
	Label lblPwdError;
	
	
	public ProfileController() {
	}
	
	public void doAfterCompose(Div div) throws Exception {
		super.doAfterCompose(div);
		div.setAttribute("controller", this);
		String pageTitle = Labels.getLabel("profile.title");
		winProfile.getPage().setTitle(pageTitle);

		Session session = Sessions.getCurrent();
		User user = (User) session.getAttribute(LoginController.SESSION_JOCHRE_USER);
		if (user==null) {
			Executions.sendRedirect("login.zul");
			return;
		}
		
        locator = JochreServiceLocator.getInstance();
    	String resourcePath = "/jdbc-jochreWeb.properties";
    	LOG.debug("resource path: " + resourcePath);
        locator.setDataSourceProperties(this.getClass().getResourceAsStream(resourcePath));
        
        lblUsername.setValue(user.getUsername());
        txtFirstName.setText(user.getFirstName());
        txtLastName.setText(user.getLastName());

	}
	
    public void onClick$btnSave(Event event) {
    	LOG.debug("onClick$btnsave");
    	try {
			Session session = Sessions.getCurrent();
			User user = (User) session.getAttribute(LoginController.SESSION_JOCHRE_USER);
			if (txtPassword.getText().length()>0) {
				if (!txtPassword.getText().equals(txtPassword2.getText())) {
					lblPwdError.setVisible(true);
					return;
				}
				user.setPassword(txtPassword.getText());
			}
			user.setFirstName(txtFirstName.getText());
			user.setLastName(txtLastName.getText());
			user.save();
			
			Messagebox.show(Labels.getLabel("button.saveComplete"));
	
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
    }
    
    public void onClick$btnCancel(Event event) {
       	LOG.debug("onClick$btnCancel");
		Executions.sendRedirect("docs.zul");
    }
    
    public Constraint getNoEmpty() {
    	Constraint noEmpty = SimpleConstraint.getInstance("no empty");
    	return noEmpty;
    }
}
