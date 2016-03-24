package com.joliciel.jochre.web;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zkoss.image.Images;
import org.zkoss.util.resource.Labels;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.databind.AnnotateDataBinder;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Window;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import com.joliciel.jochre.JochreServiceLocator;
import com.joliciel.jochre.boundaries.Split;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.security.User;
import com.joliciel.talismane.utils.LogUtils;

public class SplitController extends GenericForwardComposer<Window> {
	private static final Log LOG = LogFactory.getLog(SplitController.class);

	private static final long serialVersionUID = -6051038316789525658L;
	
	public static final String HEBREW_ACCENTS = "\u0591\u0592\u0593\u0594\u0595\u0596\u0597\u0598\u0599\u059A\u059B\u059C\u059D\u059E\u059F\u05A0\u05A1\u05A2\u05A3\u05A4\u05A5\u05A6\u05A7\u05A8\u05A9\u05AA\u05AB\u05AC\u05AD\u05AE\u05AF\u05B0\u05B1\u05B2\u05B3\u05B4\u05B5\u05B6\u05B7\u05B8\u05B9\u05BA\u05BB\u05BC\u05BD\u05BF\u05C1\u05C2\u05C4\u05C5\u05C7";
	private JochreServiceLocator locator = null;
	private GraphicsService graphicsService;

	private User currentUser;
	
	AnnotateDataBinder binder;

	Window winSplits;
	Grid splitGrid;
	Rows splitGridRows;
	Button btnSave;
	Button btnDone;
	List<Shape> shapesToSplit = null;
	
	public SplitController() {
	}
	
	public void doAfterCompose(Window window) throws Exception {
		super.doAfterCompose(window);
		String pageTitle = Labels.getLabel("splits.title");
		winSplits.getPage().setTitle(pageTitle);

		Session session = Sessions.getCurrent();
		currentUser = (User) session.getAttribute(LoginController.SESSION_JOCHRE_USER);
		if (currentUser==null)
			Executions.sendRedirect("login.zul");
		
        locator = JochreServiceLocator.getInstance();

    	String resourcePath = "/jdbc-jochreWeb.properties";
    	LOG.debug("resource path: " + resourcePath);
        locator.setDataSourceProperties(this.getClass().getResourceAsStream(resourcePath));
        graphicsService = locator.getGraphicsServiceLocator().getGraphicsService();
 		
		binder = new AnnotateDataBinder(window);
		binder.loadAll();
		
		shapesToSplit = graphicsService.findShapesToSplit(JochreProperties.getInstance().getLocale());
		for (Shape shape : shapesToSplit) {
			Row shapeRow = new Row();

			Image shapeImage = new Image();
			org.zkoss.image.Image convertedImage = Images.encode("shape_" + shape.getId() + ".png", shape.getImage());
			shapeImage.setContent(convertedImage);
			shapeImage.setWidth(shape.getImage().getWidth() + "px");
			shapeImage.setHeight((shape.getImage().getHeight()) + "px");
			

			Cell shapeCell = new Cell();
			Div shapeDiv = new Div();
			shapeDiv.setStyle("position: relative;");
			shapeCell.appendChild(shapeDiv);
			int shapeWidth = shape.getImage().getWidth();
			shapeCell.setWidth((shapeWidth + 100) + "px");
			
			shapeImage.addEventListener("onClick", new ShapeImageOnClickEventListener(shape, shapeDiv));

			
			shapeDiv.appendChild(shapeImage);
			
			for (Split split : shape.getSplits()) {
				Div verticalLine = new Div();
				verticalLine.setWidth("1px");
				verticalLine.setHeight("100px");
				verticalLine.setStyle("position: absolute; top:0px; left: "+ split.getPosition() + "px;background-color:RED;");
				verticalLine.setId("shape" + shape.getId() + "split" + split.getPosition());
				verticalLine.addEventListener("onClick", new SplitOnClickEventListener(shape, shapeDiv, verticalLine, split));
				shapeDiv.appendChild(verticalLine);
			}
			shapeRow.appendChild(shapeCell);
			
			Cell letterCell = new Cell();
			Label letterLabel = new Label();
			letterLabel.setValue(shape.getLetter());
			letterCell.appendChild(letterLabel);
			letterCell.setWidth("100px");
			shapeRow.appendChild(letterCell);
			
			Cell detailsCell = new Cell();
			Label detailsLabel = new Label();
			JochrePage page = shape.getGroup().getRow().getParagraph().getImage().getPage();
			JochreDocument doc = page.getDocument();
			detailsLabel.setValue(doc.getName() + ", page " + page.getIndex());
			detailsCell.appendChild(detailsLabel);
			shapeRow.appendChild(detailsCell);
			
			
			splitGridRows.appendChild(shapeRow);
		}
		
	}
	
    class ShapeImageOnClickEventListener implements EventListener<MouseEvent> {
    	private Shape shape;
    	private Div shapeDiv;
    	public ShapeImageOnClickEventListener(Shape shape, Div shapeDiv) {
    		this.shape = shape;
    		this.shapeDiv = shapeDiv;
    	}

    	@Override
		public void onEvent(MouseEvent mouseEvent) throws Exception {
			try {
				int x = mouseEvent.getX();
				
				Split splitToRemove = null;
				for (Split split : shape.getSplits()) {
					int diff = Math.abs(split.getPosition() - x);
					if (diff<=5) {
						splitToRemove = split;
						break;
					}
				}
				
				if (splitToRemove!=null){
					LOG.debug("Removing split at position: " + splitToRemove.getPosition());
					shape.deleteSplit(splitToRemove.getPosition());
					for (Object child : shapeDiv.getChildren()) {
						Component component = (Component) child;
						if (component.getId().equals("shape" + shape.getId() + "split" + splitToRemove.getPosition())) {
							shapeDiv.removeChild(component);
							break;
						}
					}
				} else {
				
					Split newSplit = shape.addSplit(x);
					LOG.debug("Adding split at position: " + x);
					Div verticalLine = new Div();
					verticalLine.setWidth("1px");
					verticalLine.setHeight("100px");
					verticalLine.setStyle("position: absolute; top:0px; left: "+ x + "px;background-color:RED;");
					verticalLine.setId("shape" + shape.getId() + "split" + x);
					verticalLine.addEventListener("onClick", new SplitOnClickEventListener(shape, shapeDiv, verticalLine, newSplit));
					shapeDiv.appendChild(verticalLine);
				}
			} catch (Exception e) {
				LogUtils.logError(LOG, e);
				throw e;
			}
    	}
    }

    class SplitOnClickEventListener implements EventListener<MouseEvent> {
    	private Shape shape;
    	private Div shapeDiv;
    	private Div verticalLine;
    	private Split split;
    	public SplitOnClickEventListener(Shape shape, Div shapeDiv, Div verticalLine, Split split) {
    		this.shape = shape;
    		this.shapeDiv = shapeDiv;
    		this.verticalLine = verticalLine;
    		this.split = split;
    	}

		@Override
		public void onEvent(MouseEvent event) throws Exception {
			try {
				LOG.debug("Removing split at position: " + verticalLine.getLeft());
				shapeDiv.removeChild(verticalLine);
				shape.deleteSplit(split.getPosition());
			} catch (Exception e) {
				LogUtils.logError(LOG, e);
				throw e;
			}
		}
    	
    }
    public void onClick$btnSave(Event event) {
    	try {
			LOG.debug("onClick$btnSave");
			for (Shape shape : shapesToSplit)
				shape.save();
			
			Messagebox.show(Labels.getLabel("button.saveComplete"));

    	} catch (Exception e) {
    		LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
    	}
    }
    
    public void onClick$btnDone(Event event) {
    	try {
			LOG.debug("onClick$btnDone");
			Executions.sendRedirect("docs.zul");
    	} catch (Exception e) {
    		LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
    	}
    }
}
