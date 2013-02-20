package com.joliciel.jochre.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.databind.AnnotateDataBinder;
import org.zkoss.zkplus.databind.BindingListModelArray;
import org.zkoss.zkplus.databind.BindingListModelList;
import org.zkoss.zul.AbstractTreeModel;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.TreeModel;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Window;

import com.joliciel.jochre.JochreServiceLocator;
import com.joliciel.jochre.doc.Author;
import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.doc.DocumentService;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.lexicon.LocaleSpecificLexiconService;
import com.joliciel.jochre.output.TextFormat;
import com.joliciel.jochre.output.OutputService;
import com.joliciel.jochre.output.OutputServiceLocator;
import com.joliciel.jochre.security.User;
import com.joliciel.jochre.security.UserRole;
import com.joliciel.talismane.utils.LogUtils;

public class DocumentController extends GenericForwardComposer<Window> {
	private static final Log LOG = LogFactory.getLog(DocumentController.class);

	private static final long serialVersionUID = -6051038316789525658L;
	
	public static final String HEBREW_ACCENTS = "\u0591\u0592\u0593\u0594\u0595\u0596\u0597\u0598\u0599\u059A\u059B\u059C\u059D\u059E\u059F\u05A0\u05A1\u05A2\u05A3\u05A4\u05A5\u05A6\u05A7\u05A8\u05A9\u05AA\u05AB\u05AC\u05AD\u05AE\u05AF\u05B0\u05B1\u05B2\u05B3\u05B4\u05B5\u05B6\u05B7\u05B8\u05B9\u05BA\u05BB\u05BC\u05BD\u05BF\u05C1\u05C2\u05C4\u05C5\u05C7";
	private JochreServiceLocator locator = null;
	private DocumentService documentService;
	private JochreDocument currentDoc;
	private JochrePage currentPage;
	private JochreImage currentImage;
	private User currentUser;
	
	AnnotateDataBinder binder;

	Window winJochreHome;
	Tree docTree;
	Button btnLoadImage;
	Button btnDownloadImageText;
	Button btnDownloadDocText;
	Button btnUpdateDoc;
	Button btnAddPages;
	Button btnNewDoc;
	Button btnAnalyseDoc;
	Button btnDeleteImage;
	Label lblDocName;
	Label lblImageIndex;
	Label lblDocId;
	Label lblImageId;
	Label lblDocNameLocal;
	Label lblPublisher;
	Label lblCity;
	Label lblYear;
	Label lblFileName;
	Label lblImageStatus;
	Label lblReference;
	Label lblImageOwner;
	Listbox lstAuthors;
	
	int docId = 0;
	int imageId = 0;
	
	Locale locale = null;
	Properties jochreProperties = null;
	
	public DocumentController() {
	}
	
	public void doAfterCompose(Window window) throws Exception {
		super.doAfterCompose(window);
		String pageTitle = Labels.getLabel("docs.title");
		winJochreHome.getPage().setTitle(pageTitle);

		Session session = Sessions.getCurrent();
		currentUser = (User) session.getAttribute(LoginController.SESSION_JOCHRE_USER);
		if (currentUser==null)
			Executions.sendRedirect("login.zul");
		
        locator = JochreServiceLocator.getInstance();

    	String resourcePath = "/jdbc-jochreWeb.properties";
    	LOG.debug("resource path: " + resourcePath);
        locator.setDataSourceProperties(this.getClass().getResourceAsStream(resourcePath));
        documentService = locator.getDocumentServiceLocator().getDocumentService();
        
		HttpServletRequest request = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		String docIdStr = request.getParameter("docId");
		if (docIdStr!=null)
			docId = Integer.parseInt(docIdStr);
		String imageIdStr = request.getParameter("imageId");
		if (imageIdStr!=null)
			imageId = Integer.parseInt(imageIdStr);
		
		String jochrePropertiesPath = "/jochre.properties";
		jochreProperties = new Properties();
		jochreProperties.load(this.getClass().getResourceAsStream(jochrePropertiesPath));

		String lexiconServiceClassName = jochreProperties.getProperty("lexiconService");
		LOG.debug("lexiconServiceClassName: " + lexiconServiceClassName);
		@SuppressWarnings("rawtypes")
		Class lexiconServiceClass = Class.forName(lexiconServiceClassName);
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Constructor constructor =
			lexiconServiceClass.getConstructor(new Class[]{});
		LocaleSpecificLexiconService localeSpecificLexiconService = (LocaleSpecificLexiconService) constructor.newInstance();

		locale = localeSpecificLexiconService.getLocale();
		
		binder = new AnnotateDataBinder(window);
		binder.loadAll();
	}
	
	public List<JochreDocument> getAllDocuments() {
		LOG.debug("getAllDocuments");
		
		List<JochreDocument> docs = documentService.findDocuments();
		return docs;
	}
	
	public TreeModel<DocumentTreeNode> getDocumentTree() {
		List<JochreDocument> docs = documentService.findDocuments();
		DocumentTreeModel documentTree = new DocumentTreeModel(docs);
		if (docId>0) {
			DocumentTreeNodeDoc theDocNode = null;
			for (DocumentTreeNodeDoc docNode : documentTree.getDocNodes()) {
				if (docNode.getDocument().getId()==docId) {
					theDocNode = docNode;
				}
			}
			docId = 0;
			if (theDocNode!=null) {
				Collection<DocumentTreeNode> openNodes = new ArrayList<DocumentTreeNode>();
				openNodes.add(theDocNode);
				documentTree.setOpenObjects(openNodes);
				currentDoc = theDocNode.getDocument();
				
				if (imageId>0) {
					theDocNode.getChildren();
					DocumentTreeNodeImage theImageNode = null;
					for (DocumentTreeNodeImage imageNode : theDocNode.getImageNodes()) {
						if (imageNode.getImage().getId()==imageId) {
							theImageNode = imageNode;
							break;
						}
					}
					imageId = 0;
					if (theImageNode!=null) {
						Collection<DocumentTreeNode> selection = new ArrayList<DocumentTreeNode>();
						selection.add(theImageNode);
						documentTree.setSelection(selection);
						
						currentImage = theImageNode.getImage();
					}
				}
			}
			this.reloadDoc();
			this.reloadImage();
		}
		return documentTree;
	}
	
	class DocumentTreeModel extends AbstractTreeModel<DocumentTreeNode> {
		private static final long serialVersionUID = -3202100671930767808L;
		private List<DocumentTreeNodeDoc> docNodes;
		public DocumentTreeModel(List<JochreDocument> docs) {
			super(new DocumentTreeNodeRoot());
			docNodes = new ArrayList<DocumentController.DocumentTreeNodeDoc>();
			for (JochreDocument doc : docs) {
				DocumentTreeNodeDoc docNode = new DocumentTreeNodeDoc(doc);
				docNodes.add(docNode);
			}
		}

		@Override
		public boolean isLeaf(DocumentTreeNode node) {
			if (node instanceof DocumentTreeNodeImage)
				return true;
			return false;
		}

		@Override
		public DocumentTreeNode getChild(DocumentTreeNode parent, int index) {
			if (parent instanceof DocumentTreeNodeRoot) {
				return docNodes.get(index);
			}
			if (parent instanceof DocumentTreeNodeDoc) {
				DocumentTreeNodeDoc docNode = (DocumentTreeNodeDoc) parent;
				return docNode.getChildren().get(index);
			}
			return null;
		}

		@Override
		public int getChildCount(DocumentTreeNode parent) {
			if (parent instanceof DocumentTreeNodeRoot) {
				return docNodes.size();
			}
			if (parent instanceof DocumentTreeNodeDoc) {
				DocumentTreeNodeDoc docNode = (DocumentTreeNodeDoc) parent;
				return docNode.getChildren().size();
			}
			return 0;
		}

		public List<DocumentTreeNodeDoc> getDocNodes() {
			return docNodes;
		}
	}
	
	interface DocumentTreeNode {
		
	}
	
	class DocumentTreeNodeRoot implements DocumentTreeNode {
		public String toString() {
			return "ROOT";
		}
	}
	
	class DocumentTreeNodeDoc implements DocumentTreeNode {
		private JochreDocument doc;
		private List<DocumentTreeNodeImage> imageNodes;
		public DocumentTreeNodeDoc(JochreDocument doc) {
			this.doc = doc;
		}
		
		@Override
		public String toString() {
			return doc.getName();
		}
		
		public List<DocumentTreeNodeImage> getChildren() {
			if (imageNodes==null) {
				imageNodes = new ArrayList<DocumentTreeNodeImage>();
				for (JochrePage page : doc.getPages()) {
					if (page.getImages().size()==0) {
						DocumentTreeNodeImage imageNode = new DocumentTreeNodeImage(page);
						imageNodes.add(imageNode);
					} else {
						for (JochreImage image : page.getImages()) {
							DocumentTreeNodeImage imageNode = new DocumentTreeNodeImage(image);
							imageNodes.add(imageNode);
						}
					} // have images?
				}
			}
			return imageNodes;
		}
		
		public JochreDocument getDocument() {
			return this.doc;
		}

		public List<DocumentTreeNodeImage> getImageNodes() {
			return imageNodes;
		}
	}
	
	class DocumentTreeNodeImage implements DocumentTreeNode {
		private JochrePage page;
		private JochreImage image;
		public DocumentTreeNodeImage(JochrePage page) {
			this.page = page;
		}
		public DocumentTreeNodeImage(JochreImage image) {
			this.image = image;
			this.page = image.getPage();
		}
		
		@Override
		public String toString() {
			String label = null;
			if (this.image!=null) {
				label = Labels.getLabel("docs.documents.page", new Object[] {image.getPage().getIndex(), Labels.getLabel("ImageStatus." + image.getImageStatus().getCode())});
				if (image.getPage().getImages().size()>1)
					label = Labels.getLabel("docs.documents.image", new Object[] {image.getPage().getIndex(), image.getIndex() + 1, Labels.getLabel("ImageStatus." + image.getImageStatus().getCode())});
			} else {
				label = Labels.getLabel("docs.documents.page", new Object[] {page.getIndex(), "No image"});

			}
			return label;
		}
		public JochrePage getPage() {
			return this.page;
		}
		
		public JochreImage getImage() {
			return this.image;
		}
	}
	
    public void onSelect$docTree(Event event) {
    	try {
			LOG.debug("onSelect$docTree");
	        Treeitem treeItem = docTree.getSelectedItem();
	        Object node = treeItem.getValue();
	
	        LOG.debug(node.getClass().getSimpleName());
	        if (node instanceof DocumentTreeNodeImage) {
	        	DocumentTreeNodeImage imageNode = (DocumentTreeNodeImage) node;
	        	currentImage = imageNode.getImage();
	        	currentPage = imageNode.getPage();
	        	currentDoc = imageNode.getPage().getDocument();
	        	if (currentImage!=null) {
	        		LOG.debug("DocumentTreeNodeImage, image id = " + currentImage.getId() + ", doc id = " + currentDoc.getId());
	        	} else {
	        		LOG.debug("DocumentTreeNodeImage, page id = " + currentPage.getId() + ", doc id = " + currentDoc.getId());
	        	}
	        } else if (node instanceof DocumentTreeNodeDoc) {
	        	DocumentTreeNodeDoc docNode = (DocumentTreeNodeDoc) node;
	        	currentDoc = docNode.getDocument();
	        	currentPage = null;
	        	currentImage = null;
	        	LOG.debug("DocumentTreeNodeDoc, doc id = " + currentDoc.getId());
	        }
	        
	        reloadDoc();
	        reloadImage();

    	} catch (Exception e) {
    		LogUtils.logError(LOG, e);
    		throw new RuntimeException(e);
    	}
    }
    
    public void reloadImage() {
    	if (currentImage!=null) {
        	lblImageIndex.setValue("" + currentImage.getPage().getIndex());	        	
        	btnLoadImage.setDisabled(false);
        	btnDownloadImageText.setDisabled(false);
        	if (currentUser.getRole().equals(UserRole.ADMIN))
        		btnDeleteImage.setDisabled(false);
           	lblImageId.setValue("" + currentImage.getId());
        	lblImageStatus.setValue(Labels.getLabel("ImageStatus." + currentImage.getImageStatus().getCode()));
        	lblImageOwner.setValue(currentImage.getOwner().getFirstName() + " " + currentImage.getOwner().getLastName());
    	} else if (currentPage!=null) {
        	lblImageIndex.setValue("" + currentPage.getIndex());	        	
        	btnLoadImage.setDisabled(true);
        	btnDownloadImageText.setDisabled(true);
        	if (currentUser.getRole().equals(UserRole.ADMIN))
        		btnDeleteImage.setDisabled(false);
           	lblImageId.setValue("" + currentPage.getId());
        	lblImageStatus.setValue("No image");
        	lblImageOwner.setValue("No image");
    	} else {
        	lblImageIndex.setValue(Labels.getLabel("docs.imageDetails.noImageSelected"));
        	btnLoadImage.setDisabled(true);
        	btnDownloadImageText.setDisabled(true);
        	btnDeleteImage.setDisabled(true);
        	lblImageId.setValue("");
        	lblImageStatus.setValue("");
        	lblImageOwner.setValue("");
        }	
    }
    
    public void reloadDoc() {
        if (currentDoc==null) {
        	lblDocName.setValue(Labels.getLabel("docs.documentDetails.noDocumentSelected"));	        	
        	lblDocId.setValue("");	        	
        	lblDocNameLocal.setValue("");
        	lblPublisher.setValue("");
        	lblCity.setValue("");
        	lblYear.setValue("");
        	lblFileName.setValue("");
        	lblReference.setValue("");
        	btnDownloadDocText.setDisabled(true);
        	btnUpdateDoc.setDisabled(true);
        	btnAddPages.setDisabled(true);
        	lstAuthors.setModel(new BindingListModelArray<Author>(new Author[] {}, true));
    	} else {
        	lblDocName.setValue(currentDoc.getName());	        	
        	lblDocId.setValue("" + currentDoc.getId());
        	lblDocNameLocal.setValue(currentDoc.getNameLocal());
        	lblPublisher.setValue(currentDoc.getPublisher());
        	lblCity.setValue(currentDoc.getCity());
        	lblYear.setValue("" + currentDoc.getYear());
        	lblFileName.setValue(currentDoc.getFileName());
        	lblReference.setValue(currentDoc.getReference());
        	btnDownloadDocText.setDisabled(false);
        	btnUpdateDoc.setDisabled(false);
        	btnAddPages.setDisabled(false);
        	lstAuthors.setModel(new BindingListModelList<Author>(currentDoc.getAuthors(), true));
        }	
    }
    
    public void onClick$btnLoadImage(Event event) {
       	try {
			LOG.debug("onClick$btnLoadImage");
	        
			Executions.sendRedirect("image.zul?imageId=" + currentImage.getId());
			docTree.invalidate();
   	} catch (Exception e) {
    		LogUtils.logError(LOG, e);
    		throw new RuntimeException(e);
    	}
	}
    
    public void onClick$btnDownloadImageTextOld(Event event) {
    	LOG.debug("onClick$btnDownloadImageText");
    	try {
    		if (currentImage!=null) {
		    	ServletContext servletContext = (ServletContext) Sessions.getCurrent().getWebApp().getServletContext();
				String realPath = servletContext.getRealPath("/temp/");
				File directory = new File(realPath);
				LOG.debug(realPath);
		    	File file = File.createTempFile("text", ".txt", directory);
		    	LOG.debug(file.getName());
		    	OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		    	
		    	OutputServiceLocator textServiceLocator = locator.getTextServiceLocator();
		    	OutputService textService = textServiceLocator.getTextService();
		    	DocumentObserver textGetter = textService.getTextGetter(out, TextFormat.PLAIN);
		    	textGetter.onImageComplete(currentImage);
		    	out.flush();
		    	out.close();
		    	
		    	Executions.sendRedirect("/temp/" + file.getName());
    		}
	    	
    	} catch (Exception e) {
    		LogUtils.logError(LOG, e);
    		throw new RuntimeException(e);
    	}
    }
    
    public void onClick$btnAnalyseDoc(Event event) {
    	LOG.debug("onClick$btnAnalyseDoc");
    	try {
		   Executions.sendRedirect("text.zul");
			docTree.invalidate();
	    	
    	} catch (Exception e) {
    		LogUtils.logError(LOG, e);
    		throw new RuntimeException(e);
    	}   	
    }

    public void onClick$btnDownloadDocText(Event event) {
    	LOG.debug("onClick$btnDownloadDocText");
    	try {
    		if (currentDoc!=null) {
		    	Executions.sendRedirect("text.zul?docId=" + currentDoc.getId());
    		}
			docTree.invalidate();
	    	
    	} catch (Exception e) {
    		LogUtils.logError(LOG, e);
    		throw new RuntimeException(e);
    	}   	
    }

    public void onClick$btnAddPages(Event event) {
    	LOG.debug("onClick$btnAddPages");
    	try {
    		if (currentDoc!=null) {
		    	Executions.sendRedirect("text.zul?docId=" + currentDoc.getId() + "&addPages=true");
    		}
			docTree.invalidate();
	    	
    	} catch (Exception e) {
    		LogUtils.logError(LOG, e);
    		throw new RuntimeException(e);
    	}   	
    }

    public void onClick$btnDownloadImageText(Event event) {
    	LOG.debug("onClick$btnDownloadImageText");
    	try {
    		if (currentImage!=null) {
		    	
		    	Executions.sendRedirect("text.zul?imageId=" + currentImage.getId());
    		}
	    	
    	} catch (Exception e) {
    		LogUtils.logError(LOG, e);
    		throw new RuntimeException(e);
    	}
    }
    
    public void onClick$btnDeleteImage(Event event) {
    	LOG.debug("onClick$btnDeleteImage");
    	try {
    		if (currentDoc != null && currentImage!=null) {
    			Messagebox.show(Labels.getLabel("docs.imageDetails.deleteConfirm"),
    					Labels.getLabel("button.areYouSureTitle"),
    					Messagebox.OK | Messagebox.CANCEL,
    		            Messagebox.QUESTION, new EventListener<Event>() {
							
							@Override
							public void onEvent(Event event) throws Exception {
								if (((Integer) event.getData()).intValue() == Messagebox.OK) {
							    	currentDoc.deleteImage(currentImage);
							    	docTree.setModel(getDocumentTree());
							    	currentImage = null;
							    	reloadImage();
									Messagebox.show(Labels.getLabel("button.deleteComplete"));
								}
							}
						});
    		} else if (currentDoc!=null && currentPage !=null) {
       			Messagebox.show(Labels.getLabel("docs.imageDetails.deleteConfirm"),
    					Labels.getLabel("button.areYouSureTitle"),
    					Messagebox.OK | Messagebox.CANCEL,
    		            Messagebox.QUESTION, new EventListener<Event>() {
							
							@Override
							public void onEvent(Event event) throws Exception {
								if (((Integer) event.getData()).intValue() == Messagebox.OK) {
							    	currentDoc.deletePage(currentPage);
							    	docTree.setModel(getDocumentTree());
							    	currentPage = null;
							    	reloadImage();
									Messagebox.show(Labels.getLabel("button.deleteComplete"));
								}
							}
						});
    		}
	    	
    	} catch (Exception e) {
    		LogUtils.logError(LOG, e);
    		throw new RuntimeException(e);
    	}
    }
    
    public void onClick$btnUpdateDoc(Event event) {
       	try {
			LOG.debug("onClick$btnUpdateDoc");
			Window winUpdateDoc = (Window) Path.getComponent("//pgDocs/winUpdateDocument");
			LOG.debug("Setting UpdateDocumentController.ATTR_DOC to " + currentDoc);
			winUpdateDoc.setAttribute(UpdateDocumentController.ATTR_DOC, currentDoc);
			winUpdateDoc.setAttribute(UpdateDocumentController.ATTR_DOC_CONTROLLER, this);
			Event modalEvent = new Event("onModalOpen", (Window) Path.getComponent("//pgDocs/winUpdateDocument"), currentDoc);
			Events.sendEvent(modalEvent);

			winUpdateDoc.doModal();

			docTree.invalidate();
    	} catch (Exception e) {
    		LogUtils.logError(LOG, e);
    		throw new RuntimeException(e);
    	}
	}
    
    public void onClick$btnNewDoc(Event event) {
    	try {
			LOG.debug("onClick$btnNewDoc");
			Window winUpdateDoc = (Window) Path.getComponent("//pgDocs/winUpdateDocument");
			JochreDocument newDoc = documentService.getEmptyJochreDocument();

			newDoc.setLocale(this.locale);
			newDoc.setOwner(this.currentUser);
			newDoc.setYear(1900);
			winUpdateDoc.setAttribute(UpdateDocumentController.ATTR_DOC, newDoc);
			winUpdateDoc.setAttribute(UpdateDocumentController.ATTR_DOC_CONTROLLER, this);

			Event modalEvent = new Event("onModalOpen", (Window) Path.getComponent("//pgDocs/winUpdateDocument"), newDoc);
			Events.sendEvent(modalEvent);

			winUpdateDoc.doModal();
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
    }
    
    public void selectNewDoc(JochreDocument newDoc) {
    	LOG.debug("selectNewDoc");
    	docTree.setModel(this.getDocumentTree());
    	Treechildren rootChild = docTree.getTreechildren();
    	Collection<Treeitem> treeItems = rootChild.getItems();
    	if (treeItems.size()>0) {
	    	Treeitem root = (Treeitem) treeItems.iterator().next();
			Collection<Treeitem> rootChildren = root.getTreechildren().getItems();
	    	Treeitem theItem = null;
	    	for (Treeitem child : rootChildren) {
	    		DocumentTreeNodeDoc docNode = (DocumentTreeNodeDoc) child.getValue();
	    		if (docNode.getDocument().equals(newDoc)) {
	    			theItem = child;
	    			break;
	    		}
	    	}
	    	if (theItem!=null)
	    		docTree.selectItem(theItem);
    	}
    }

}
