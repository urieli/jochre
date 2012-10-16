package com.joliciel.jochre.web;


import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zkforge.bwcaptcha.Captcha;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.joliciel.jochre.EntityNotFoundException;
import com.joliciel.jochre.JochreServiceLocator;
import com.joliciel.jochre.security.Parameters;
import com.joliciel.jochre.security.SecurityService;
import com.joliciel.jochre.security.User;
import com.joliciel.talismane.utils.LogUtils;

public class LoginController extends GenericForwardComposer<Window> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1664468221173319777L;


	private static final Log LOG = LogFactory.getLog(LoginController.class);
	public static String SESSION_JOCHRE_USER = "SESSION_JOCHRE_USER";
	private JochreServiceLocator locator = null;
	private SecurityService securityService = null;

	Window winLogin;
	Button btnLogin;
	Textbox txtUserName;
	Textbox txtPassword;
	Captcha captcha;
	Textbox txtCaptcha;
	Row rowCaptcha;
	Row rowCaptchaTextbox;
	Label lblBadCaptcha;
	Label lblError;
	Button btnCaptcha;
	
	public LoginController() {
	}
	
	public void doAfterCompose(Window window) throws Exception {
		super.doAfterCompose(window);
		String pageTitle = Labels.getLabel("login.title");
		winLogin.getPage().setTitle(pageTitle);

		Session session = Sessions.getCurrent();
		session.removeAttribute(SESSION_JOCHRE_USER);
		
        locator = JochreServiceLocator.getInstance();

    	String resourcePath = "/jdbc-jochreWeb.properties";
        locator.setDataSourceProperties(this.getClass().getResourceAsStream(resourcePath));
		securityService = locator.getSecurityServiceLocator().getSecurityService();

		HttpServletRequest request = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		String failed = request.getParameter("failed");
		if (failed==null)
			lblError.setVisible(false);
		else
			lblError.setVisible(true);
		
		Parameters parameters = securityService.loadParameters();
		Date lastFailedLoginAttempt = parameters.getLastFailedLoginAttempt();
		int captchaIntervalSeconds = parameters.getCaptachaIntervalSeconds();
		Date now = new Date();
		long diff = now.getTime() - lastFailedLoginAttempt.getTime();
		LOG.debug("time since last failed login: " + diff);
		if (diff < captchaIntervalSeconds * 1000) {
			LOG.debug("Showing captcha, interval = " + captchaIntervalSeconds);
			rowCaptcha.setVisible(true);
			rowCaptchaTextbox.setVisible(true);
		} else {
			rowCaptcha.setVisible(false);
			rowCaptchaTextbox.setVisible(false);
		}
	}

    public void onClick$btnCaptcha(Event event) {
    	captcha.randomValue();
    }

    public void onClick$btnLogin(Event event) {
       	try {
			LOG.debug("onClick$btnLogin");
			
			if (rowCaptcha.isVisible()) {
				String captchaText = this.txtCaptcha.getValue();
				if (!captcha.getValue().equalsIgnoreCase(captchaText)) {
					LOG.debug("Bad captcha");
					lblBadCaptcha.setVisible(true);
					captcha.randomValue();
					txtCaptcha.setValue("");
					return;
				} else {
					lblBadCaptcha.setVisible(false);
				}
			}
			
			Session session = Sessions.getCurrent();
			
			User user = null;
			try {
				user = securityService.findUser(txtUserName.getValue());
			} catch (EntityNotFoundException enfe) {
				LOG.debug("Unknown user: " + txtUserName.getValue());
				lblError.setVisible(true);
				captcha.randomValue();
				txtCaptcha.setValue("");
			}
			
			if (user!=null) {
				boolean success = user.login(txtPassword.getValue());
				if (!success) {
					LOG.debug("Login failed");
					lblError.setVisible(true);
					captcha.randomValue();
					txtCaptcha.setValue("");
					
					Parameters parameters = securityService.loadParameters();
					Date lastFailedLoginAttempt = parameters.getLastFailedLoginAttempt();
					int captchaIntervalSeconds = parameters.getCaptachaIntervalSeconds();
					Date now = new Date();
					long diff = now.getTime() - lastFailedLoginAttempt.getTime();
					LOG.debug("time since last failed login: " + diff);
					if (diff < captchaIntervalSeconds * 1000) {
						LOG.debug("Showing captcha, interval = " + captchaIntervalSeconds);
						rowCaptcha.setVisible(true);
						rowCaptchaTextbox.setVisible(true);
					} else {
						rowCaptcha.setVisible(false);
						rowCaptchaTextbox.setVisible(false);
					}
				} else {
					LOG.debug("Login success");
					session.setAttribute(SESSION_JOCHRE_USER, user);
					Executions.sendRedirect("docs.zul");
				}
			}
    	} catch (Exception e) {
    		LogUtils.logError(LOG, e);
    		throw new RuntimeException(e);
    	}
	}
}
