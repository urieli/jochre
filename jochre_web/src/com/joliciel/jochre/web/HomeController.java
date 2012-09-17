package com.joliciel.jochre.web;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Window;

import com.joliciel.talismane.utils.util.LogUtils;

public class HomeController extends GenericForwardComposer<Window> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1664468221173319777L;


	private static final Log LOG = LogFactory.getLog(HomeController.class);

	Window winJochreHome;
	Button btnLogin;
	
	public HomeController() {
	}
	
	public void doAfterCompose(Window comp) throws Exception {
		super.doAfterCompose(comp);
	}
	
    public void onClick$btnLogin(Event event) {
       	try {
			LOG.debug("onClick$btnLogin");
	        
			Executions.sendRedirect("login.zul");
    	} catch (Exception e) {
    		LogUtils.logError(LOG, e);
    		throw new RuntimeException(e);
    	}
	}
}
