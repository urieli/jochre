package com.joliciel.jochre.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.DefaultTreeModel;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelArray;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.TreeModel;
import org.zkoss.zul.TreeNode;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Window;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.doc.Author;
import com.joliciel.jochre.doc.DocumentDao;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.security.User;
import com.joliciel.jochre.security.UserRole;

public class DocumentController extends SelectorComposer<Window> {
	private static final Logger LOG = LoggerFactory.getLogger(DocumentController.class);

	private static final long serialVersionUID = 1L;

	public static final String HEBREW_ACCENTS = "\u0591\u0592\u0593\u0594\u0595\u0596\u0597\u0598\u0599\u059A\u059B\u059C\u059D\u059E\u059F\u05A0\u05A1\u05A2\u05A3\u05A4\u05A5\u05A6\u05A7\u05A8\u05A9\u05AA\u05AB\u05AC\u05AD\u05AE\u05AF\u05B0\u05B1\u05B2\u05B3\u05B4\u05B5\u05B6\u05B7\u05B8\u05B9\u05BA\u05BB\u05BC\u05BD\u05BF\u05C1\u05C2\u05C4\u05C5\u05C7";
	private final JochreSession jochreSession;
	private JochreDocument currentDoc;
	private JochreImage currentImage;
	private User currentUser;

	@Wire
	Window winJochreHome;
	@Wire
	Tree docTree;
	@Wire
	Button btnLoadImage;
	@Wire
	Button btnDownloadImageText;
	@Wire
	Button btnDownloadDocText;
	@Wire
	Button btnUpdateDoc;
	@Wire
	Button btnAddPages;
	@Wire
	Button btnNewDoc;
	@Wire
	Button btnAnalyseDoc;
	@Wire
	Button btnDeleteImage;
	@Wire
	Label lblDocName;
	@Wire
	Label lblImageIndex;
	@Wire
	Label lblDocId;
	@Wire
	Label lblImageId;
	@Wire
	Label lblDocNameLocal;
	@Wire
	Label lblPublisher;
	@Wire
	Label lblCity;
	@Wire
	Label lblYear;
	@Wire
	Label lblFileName;
	@Wire
	Label lblImageStatus;
	@Wire
	Label lblReference;
	@Wire
	Label lblImageOwner;
	@Wire
	Listbox lstAuthors;

	int docId = 0;
	int imageId = 0;

	public DocumentController() throws ReflectiveOperationException {
		jochreSession = JochreProperties.getInstance().getJochreSession();
	}

	@Override
	public void doAfterCompose(Window window) throws Exception {
		LOG.debug("doAfterCompose");
		super.doAfterCompose(window);
		String pageTitle = Labels.getLabel("docs.title");
		winJochreHome.getPage().setTitle(pageTitle);

		Session session = Sessions.getCurrent();
		currentUser = (User) session.getAttribute(LoginController.SESSION_JOCHRE_USER);
		if (currentUser == null)
			Executions.sendRedirect("login.zul");

		HttpServletRequest request = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		String docIdStr = request.getParameter("docId");
		if (docIdStr != null)
			docId = Integer.parseInt(docIdStr);
		String imageIdStr = request.getParameter("imageId");
		if (imageIdStr != null)
			imageId = Integer.parseInt(imageIdStr);

		docTree.setModel(this.getDocumentTree());
		lstAuthors.setModel(new ListModelArray<Author>(new Author[] {}, true));
		lstAuthors.setItemRenderer(new AuthorListItemRenderer());
	}

	public TreeModel<TreeNode<DocOrImage>> getDocumentTree() {
		LOG.debug("getDocumentTree2");
		DocumentDao documentDao = DocumentDao.getInstance(jochreSession);
		List<JochreDocument> docs = documentDao.findDocuments();
		List<TreeNode<DocOrImage>> docNodeList = new ArrayList<>();
		for (JochreDocument doc : docs) {
			List<TreeNode<DocOrImage>> imageNodeList = new ArrayList<>();
			for (JochrePage page : doc.getPages()) {
				LOG.debug("page " + page.getIndex());
				for (JochreImage image : page.getImages()) {
					DefaultTreeNode<DocOrImage> imageNode = new DefaultTreeNode<DocumentController.DocOrImage>(new DocOrImage(image));
					imageNodeList.add(imageNode);
					if (imageId == image.getId())
						currentImage = image;
				} // have images?
			}

			boolean open = docId == doc.getId();
			if (open)
				currentDoc = doc;
			DefaultTreeNode<DocOrImage> docNode = new DirectoryTreeNode<DocumentController.DocOrImage>(new DocOrImage(doc), imageNodeList, open);
			docNodeList.add(docNode);

		}
		DefaultTreeNode<DocOrImage> root = new DefaultTreeNode<DocumentController.DocOrImage>(null, docNodeList);

		TreeModel<TreeNode<DocOrImage>> docTree = new DefaultTreeModel<>(root);

		this.reloadDoc();
		this.reloadImage();

		return docTree;
	}

	public class DirectoryTreeNode<T> extends DefaultTreeNode<T> {
		private static final long serialVersionUID = 1L;
		private boolean open = false;

		public DirectoryTreeNode(T data, Collection<TreeNode<T>> children, boolean open) {
			super(data, children);
			this.setOpen(open);
		}

		public DirectoryTreeNode(T data, Collection<TreeNode<T>> children) {
			super(data, children);
		}

		public DirectoryTreeNode(T data) {
			super(data);
		}

		public boolean isOpen() {
			return open;
		}

		public void setOpen(boolean open) {
			this.open = open;
		}
	}

	public static final class DocOrImage {
		private final int id;
		private final String description;
		private final JochreDocument doc;
		private final JochreImage image;

		public DocOrImage(JochreDocument doc) {
			this.doc = doc;
			this.id = doc.getId();
			this.description = doc.getName();
			this.image = null;
		}

		public DocOrImage(JochreImage image) {
			this.doc = null;
			this.image = image;
			this.id = image.getId();
			boolean singleImage = (image.getPage().getImages().size() == 1);

			String label = null;
			label = Labels.getLabel("docs.documents.page",
					new Object[] { image.getPage().getIndex(), Labels.getLabel("ImageStatus." + image.getImageStatus().getCode()) });
			if (!singleImage)
				label = Labels.getLabel("docs.documents.image",
						new Object[] { image.getPage().getIndex(), image.getIndex() + 1, Labels.getLabel("ImageStatus." + image.getImageStatus().getCode()) });

			this.description = label;
		}

		public JochreDocument getDocument() {
			return doc;
		}

		public JochreImage getImage() {
			return image;
		}

		public int getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}

		public boolean isDocument() {
			return doc != null;
		}
	}

	@Listen("onSelect = #docTree")
	public void onSelect$docTree() {
		LOG.debug("onSelect$docTree");
		Treeitem treeItem = docTree.getSelectedItem();
		TreeNode<DocOrImage> node = treeItem.getValue();

		if (node.getData().isDocument()) {
			currentDoc = node.getData().getDocument();
			currentImage = null;
			LOG.debug("DocumentTreeNodeDoc, doc id = " + currentDoc.getId());
		} else {
			currentImage = node.getData().getImage();
			currentDoc = currentImage.getPage().getDocument();
			LOG.debug("Image, image id = " + currentImage.getId() + ", doc id = " + currentDoc.getId());
		}

		reloadDoc();
		reloadImage();
	}

	public void reloadImage() {
		if (currentImage != null) {
			lblImageIndex.setValue("" + currentImage.getPage().getIndex());
			btnLoadImage.setDisabled(false);
			btnDownloadImageText.setDisabled(false);
			if (currentUser.getRole().equals(UserRole.ADMIN))
				btnDeleteImage.setDisabled(false);
			lblImageId.setValue("" + currentImage.getId());
			lblImageStatus.setValue(Labels.getLabel("ImageStatus." + currentImage.getImageStatus().getCode()));
			lblImageOwner.setValue(currentImage.getOwner().getFirstName() + " " + currentImage.getOwner().getLastName());
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
		if (currentDoc == null) {
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
			lstAuthors.setModel(new ListModelArray<Author>(new Author[] {}, true));
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
			lstAuthors.setModel(new ListModelList<Author>(currentDoc.getAuthors(), true));
		}
	}

	@Listen("onClick = #btnLoadImage")
	public void onClick$btnLoadImage() {
		try {
			LOG.debug("onClick$btnLoadImage");

			Executions.sendRedirect("image.zul?imageId=" + currentImage.getId());
			docTree.invalidate();
		} catch (Exception e) {
			LOG.error("Failure in onClick$btnLoadImage", e);
			throw new RuntimeException(e);
		}
	}

	@Listen("onClick = #btnAnalyseDoc")
	public void onClick$btnAnalyseDoc(Event event) {
		LOG.debug("onClick$btnAnalyseDoc");
		try {
			Executions.sendRedirect("text.zul");
			docTree.invalidate();

		} catch (Exception e) {
			LOG.error("Failure in onClick$btnAnalyseDoc", e);
			throw new RuntimeException(e);
		}
	}

	@Listen("onClick = #btnDownloadDocText")
	public void onClick$btnDownloadDocText() {
		LOG.debug("onClick$btnDownloadDocText");
		try {
			if (currentDoc != null) {
				Executions.sendRedirect("text.zul?docId=" + currentDoc.getId());
			}
			docTree.invalidate();

		} catch (Exception e) {
			LOG.error("Failure in onClick$btnDownloadDocText", e);
			throw new RuntimeException(e);
		}
	}

	@Listen("onClick = #btnAddPages")
	public void onClick$btnAddPages() {
		LOG.debug("onClick$btnAddPages");
		try {
			if (currentDoc != null) {
				Executions.sendRedirect("text.zul?docId=" + currentDoc.getId() + "&addPages=true");
			}
			docTree.invalidate();

		} catch (Exception e) {
			LOG.error("Failure in onClick$btnAddPages", e);
			throw new RuntimeException(e);
		}
	}

	@Listen("onClick = #btnDownloadImageText")
	public void onClick$btnDownloadImageText() {
		LOG.debug("onClick$btnDownloadImageText");
		try {
			if (currentImage != null) {

				Executions.sendRedirect("text.zul?imageId=" + currentImage.getId());
			}

		} catch (Exception e) {
			LOG.error("Failure in onClick$btnDownloadImageText", e);
			throw new RuntimeException(e);
		}
	}

	@Listen("onClick = #btnDeleteImage")
	public void onClick$btnDeleteImage() {
		LOG.debug("onClick$btnDeleteImage");
		if (currentDoc != null && currentImage != null) {
			Messagebox.show(Labels.getLabel("docs.imageDetails.deleteConfirm"), Labels.getLabel("button.areYouSureTitle"), Messagebox.OK | Messagebox.CANCEL,
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
		}

	}

	@Listen("onClick = #btnUpdateDoc")
	public void onClick$btnUpdateDoc() {
		LOG.debug("onClick$btnUpdateDoc");
		Window winUpdateDoc = (Window) Path.getComponent("//pgDocs/winUpdateDocument");
		LOG.debug("Setting UpdateDocumentController.ATTR_DOC to " + currentDoc);
		winUpdateDoc.setAttribute(UpdateDocumentController.ATTR_DOC, currentDoc);
		winUpdateDoc.setAttribute(UpdateDocumentController.ATTR_DOC_CONTROLLER, this);
		Event modalEvent = new Event("onModalOpen", Path.getComponent("//pgDocs/winUpdateDocument"), currentDoc);
		Events.sendEvent(modalEvent);

		winUpdateDoc.doModal();

		docTree.invalidate();
	}

	@Listen("onClick = #btnNewDoc")
	public void onClick$btnNewDoc() {
		LOG.debug("onClick$btnNewDoc");
		Window winUpdateDoc = (Window) Path.getComponent("//pgDocs/winUpdateDocument");
		JochreDocument newDoc = new JochreDocument(jochreSession);

		newDoc.setLocale(jochreSession.getLocale());
		newDoc.setOwner(this.currentUser);
		newDoc.setYear(1900);
		winUpdateDoc.setAttribute(UpdateDocumentController.ATTR_DOC, newDoc);
		winUpdateDoc.setAttribute(UpdateDocumentController.ATTR_DOC_CONTROLLER, this);

		Event modalEvent = new Event("onModalOpen", Path.getComponent("//pgDocs/winUpdateDocument"), newDoc);
		Events.sendEvent(modalEvent);

		winUpdateDoc.doModal();
	}

	public void selectNewDoc(JochreDocument newDoc) {
		LOG.debug("selectNewDoc");
		docTree.setModel(this.getDocumentTree());
		Treechildren rootChild = docTree.getTreechildren();
		Collection<Treeitem> treeItems = rootChild.getItems();
		if (treeItems.size() > 0) {
			Treeitem root = treeItems.iterator().next();
			Collection<Treeitem> rootChildren = root.getTreechildren().getItems();
			Treeitem theItem = null;
			for (Treeitem child : rootChildren) {
				TreeNode<DocOrImage> docNode = child.getValue();
				if (docNode.getData().getDocument().equals(newDoc)) {
					theItem = child;
					break;
				}
			}
			if (theItem != null)
				docTree.selectItem(theItem);
		}
	}

	public static final class AuthorListItemRenderer implements ListitemRenderer<Author> {

		@Override
		public void render(Listitem item, Author author, int index) throws Exception {
			Listcell listcell1 = new Listcell(author.getFullName());
			Listcell listcell2 = new Listcell(author.getFullNameLocal());
			item.getChildren().add(listcell1);
			item.getChildren().add(listcell2);
		}

	}
}
