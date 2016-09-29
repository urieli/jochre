package com.joliciel.jochre.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.joliciel.jochre.security.User;

public class ProfileController extends GenericForwardComposer<Div> {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(ProfileController.class);

	Window winProfile;
	Label lblUsername;
	Textbox txtPassword;
	Textbox txtPassword2;
	Textbox txtFirstName;
	Textbox txtLastName;
	Label lblPwdError;

	public ProfileController() {
	}

	@Override
	public void doAfterCompose(Div div) throws Exception {
		super.doAfterCompose(div);
		div.setAttribute("controller", this);
		String pageTitle = Labels.getLabel("profile.title");
		winProfile.getPage().setTitle(pageTitle);

		Session session = Sessions.getCurrent();
		User user = (User) session.getAttribute(LoginController.SESSION_JOCHRE_USER);
		if (user == null) {
			Executions.sendRedirect("login.zul");
			return;
		}

		lblUsername.setValue(user.getUsername());
		txtFirstName.setText(user.getFirstName());
		txtLastName.setText(user.getLastName());

	}

	public void onClick$btnSave(Event event) {
		LOG.debug("onClick$btnsave");
		try {
			Session session = Sessions.getCurrent();
			User user = (User) session.getAttribute(LoginController.SESSION_JOCHRE_USER);
			if (txtPassword.getText().length() > 0) {
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
			LOG.error("Failure in onClick$btnsave", e);
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
