package com.joliciel.jochre.web;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.joliciel.jochre.EntityNotFoundException;
import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.security.Parameters;
import com.joliciel.jochre.security.SecurityDao;
import com.joliciel.jochre.security.User;

public class LoginController extends GenericForwardComposer<Window> {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);
	public static String SESSION_JOCHRE_USER = "SESSION_JOCHRE_USER";

	private final JochreSession jochreSession;

	@Wire
	Window winLogin;
	@Wire
	Button btnLogin;
	@Wire
	Textbox txtUserName;
	@Wire
	Textbox txtPassword;
	@Wire
	Label lblError;

	public LoginController() throws ReflectiveOperationException {
		jochreSession = JochreProperties.getInstance().getJochreSession();
	}

	@Override
	public void doAfterCompose(Window window) throws Exception {
		super.doAfterCompose(window);
		String pageTitle = Labels.getLabel("login.title");
		winLogin.getPage().setTitle(pageTitle);

		Session session = Sessions.getCurrent();
		session.removeAttribute(SESSION_JOCHRE_USER);

		HttpServletRequest request = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		String failed = request.getParameter("failed");
		if (failed == null)
			lblError.setVisible(false);
		else
			lblError.setVisible(true);

		SecurityDao securityDao = SecurityDao.getInstance(jochreSession);
		Parameters parameters = securityDao.loadParameters();
		Date lastFailedLoginAttempt = parameters.getLastFailedLoginAttempt();
		Date now = new Date();
		long diff = now.getTime() - lastFailedLoginAttempt.getTime();
		LOG.debug("time since last failed login: " + diff);
	}

	@Listen("onClick = #btnLogin")
	public void onClick$btnLogin(Event event) {
		try {
			LOG.debug("onClick$btnLogin");

			Session session = Sessions.getCurrent();
			SecurityDao securityDao = SecurityDao.getInstance(jochreSession);

			User user = null;
			try {
				user = securityDao.findUser(txtUserName.getValue());
			} catch (EntityNotFoundException enfe) {
				LOG.debug("Unknown user: " + txtUserName.getValue());
				lblError.setVisible(true);
			}

			if (user != null) {
				boolean success = user.login(txtPassword.getValue());
				if (!success) {
					LOG.debug("Login failed");
					lblError.setVisible(true);

					Parameters parameters = securityDao.loadParameters();
					Date lastFailedLoginAttempt = parameters.getLastFailedLoginAttempt();
					Date now = new Date();
					long diff = now.getTime() - lastFailedLoginAttempt.getTime();
					LOG.debug("time since last failed login: " + diff);
				} else {
					LOG.debug("Login success");
					session.setAttribute(SESSION_JOCHRE_USER, user);
					Executions.sendRedirect("docs.zul");
				}
			}
		} catch (Exception e) {
			LOG.error("Failure in onClick$btnLogin", e);
			throw new RuntimeException(e);
		}
	}
}
