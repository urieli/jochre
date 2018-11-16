package com.joliciel.jochre.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Html;
import org.zkoss.zul.Window;

public class HomeController extends GenericForwardComposer<Window> {
  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LoggerFactory.getLogger(HomeController.class);

  @Wire
  Window winJochreHome;
  @Wire
  Button btnLogin;
  @Wire
  Html htmlContent;

  public HomeController() {
  }

  @Override
  public void doAfterCompose(Window comp) throws Exception {
    LOG.debug("HomeController.doAfterCompose");
    super.doAfterCompose(comp);
    htmlContent.setContent(JochreProperties.getInstance().getJochreSession().getConfig().getConfig("jochre.web").getString("welcome-text"));
  }

  @Listen("onClick = #btnLogin")
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
