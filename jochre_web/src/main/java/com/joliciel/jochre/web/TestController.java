package com.joliciel.jochre.web;

import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.databind.AnnotateDataBinder;
import org.zkoss.zul.Button;
import org.zkoss.zul.Window;

public class TestController extends GenericForwardComposer<Window> {
	private static final long serialVersionUID = 5240410997127634394L;
	Button btnTest;
	AnnotateDataBinder binder;
	
	public void doAfterCompose(Window window) throws Exception {
		super.doAfterCompose(window);
		
		btnTest.setWidgetListener("onClick", "testAlert();");
		
		binder = new AnnotateDataBinder(window);
		binder.loadAll();
	}
}
