package com.joliciel.jochre.web;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.databind.AnnotateDataBinder;
import org.zkoss.zkplus.databind.BindingListModelList;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.doc.Author;
import com.joliciel.jochre.doc.DocumentDao;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.security.User;
import com.typesafe.config.ConfigFactory;

public class UpdateDocumentController extends GenericForwardComposer<Window> {
	private static final Logger LOG = LoggerFactory.getLogger(UpdateDocumentController.class);

	private static final long serialVersionUID = 1L;
	static final String ATTR_DOC = "JochreDoc";
	static final String ATTR_DOC_CONTROLLER = "DocController";

	private final JochreSession jochreSession;
	private JochreDocument currentDoc;
	private Author currentAuthor;

	AnnotateDataBinder binder;

	Window winUpdateDocument;
	Textbox txtDocName;
	Textbox txtDocNameLocal;
	Textbox txtPublisher;
	Textbox txtCity;
	Textbox txtYear;
	Textbox txtReference;
	Textbox txtAuthorFirstName;
	Textbox txtAuthorLastName;
	Textbox txtAuthorFirstNameLocal;
	Textbox txtAuthorLastNameLocal;
	Button btnAddNewAuthor;
	Button btnSave;
	Button btnCancel;
	Button btnAddAuthor;

	Listbox lstAuthors;
	Combobox cmbAuthors;

	public UpdateDocumentController() throws ReflectiveOperationException {
		jochreSession = new JochreSession(ConfigFactory.load());
	}

	@Override
	public void doAfterCompose(Window window) throws Exception {
		super.doAfterCompose(window);

		Session session = Sessions.getCurrent();
		User user = (User) session.getAttribute(LoginController.SESSION_JOCHRE_USER);
		if (user == null)
			Executions.sendRedirect("login.zul");

		DocumentDao documentDao = DocumentDao.getInstance(jochreSession);
		List<Author> authors = documentDao.findAuthors();

		List<Comboitem> authorItems = cmbAuthors.getItems();
		for (Author author : authors) {
			Comboitem item = new Comboitem(author.getFullName());
			item.setValue(author.getId());
			authorItems.add(item);
		}
		// comp.setVariable(comp.getId() + "Ctrl", this, true);
		binder = new AnnotateDataBinder(window);
		binder.loadAll();

		winUpdateDocument.addEventListener("onModalOpen", new UpdateDocumentControllerModalListener());
	}

	public void onClick$btnAddAuthor(Event event) {
		LOG.debug("onClick$btnAddAuthor");
		try {
			int authorId = 0;
			Comboitem selectedItem = cmbAuthors.getSelectedItem();
			if (selectedItem != null)
				authorId = (Integer) selectedItem.getValue();
			if (authorId != 0) {
				LOG.debug("authorId: " + authorId);
				DocumentDao documentDao = DocumentDao.getInstance(jochreSession);

				Author author = documentDao.loadAuthor(authorId);
				if (!this.currentDoc.getAuthors().contains(author)) {
					this.currentDoc.getAuthors().add(author);

					BindingListModelList<Author> model = new BindingListModelList<Author>(this.currentDoc.getAuthors(), true);
					lstAuthors.setModel(model);
				}
			}
		} catch (Exception e) {
			LOG.error("Failure in onClick$btnAddAuthor", e);
			throw new RuntimeException(e);
		}
	}

	public void onClick$btnCancelAuthor(Event event) {
		LOG.debug("onClick$btnCancelAuthor()");
		try {
			currentAuthor = null;
			txtAuthorFirstName.setValue("");
			txtAuthorLastName.setValue("");
			txtAuthorFirstNameLocal.setValue("");
			txtAuthorLastNameLocal.setValue("");
			btnAddNewAuthor.setLabel(Labels.getLabel("docs.documentDetails.addAuthor"));

		} catch (Exception e) {
			LOG.error("Failure in onClick$btnCancelAuthor", e);
			throw new RuntimeException(e);
		}
	}

	public void onClick$btnUpdateAuthor(Event event) {
		LOG.debug("onClick$btnUpdateAuthor()");
		try {
			if (lstAuthors.getSelectedItem() == null) {
				return;
			}
			currentAuthor = (Author) lstAuthors.getSelectedItem().getValue();
			txtAuthorFirstName.setValue(currentAuthor.getFirstName());
			txtAuthorLastName.setValue(currentAuthor.getLastName());
			txtAuthorFirstNameLocal.setValue(currentAuthor.getFirstNameLocal());
			txtAuthorLastNameLocal.setValue(currentAuthor.getLastNameLocal());
			btnAddNewAuthor.setLabel(Labels.getLabel("button.update"));

		} catch (Exception e) {
			LOG.error("Failure in onClick$btnUpdateAuthor", e);
			throw new RuntimeException(e);
		}
	}

	public void onClick$btnDeleteAuthor(Event event) {
		LOG.debug("onClick$btnDeleteAuthor()");
		try {
			if (lstAuthors.getSelectedItem() == null) {
				return;
			}
			Author author = (Author) lstAuthors.getSelectedItem().getValue();
			this.currentDoc.getAuthors().remove(author);

			BindingListModelList<Author> model = new BindingListModelList<Author>(this.currentDoc.getAuthors(), true);
			lstAuthors.setModel(model);

		} catch (Exception e) {
			LOG.error("Failure in onClick$btnDeleteAuthor", e);
			throw new RuntimeException(e);
		}
	}

	public void onClick$btnAddNewAuthor(Event event) {
		LOG.debug("onClick$btnAddNewAuthor");
		try {
			Author author = currentAuthor;
			boolean isNew = false;
			if (author == null) {
				author = new Author(jochreSession);
				isNew = true;
			}
			author.setFirstName(txtAuthorFirstName.getValue());
			author.setLastName(txtAuthorLastName.getValue());
			author.setFirstNameLocal(txtAuthorFirstNameLocal.getValue());
			author.setLastNameLocal(txtAuthorLastNameLocal.getValue());
			author.save();

			currentAuthor = null;
			txtAuthorFirstName.setValue("");
			txtAuthorLastName.setValue("");
			txtAuthorFirstNameLocal.setValue("");
			txtAuthorLastNameLocal.setValue("");
			btnAddNewAuthor.setLabel(Labels.getLabel("docs.documentDetails.addAuthor"));

			if (isNew) {
				DocumentDao documentDao = DocumentDao.getInstance(jochreSession);
				List<Author> authors = documentDao.findAuthors();

				List<Comboitem> authorItems = cmbAuthors.getItems();
				authorItems.clear();
				for (Author oneAuthor : authors) {
					Comboitem item = new Comboitem(oneAuthor.getFullName());
					item.setValue(oneAuthor.getId());
					authorItems.add(item);
					if (oneAuthor.getId() == author.getId())
						cmbAuthors.setSelectedItem(item);
				}
			} else {
				BindingListModelList<Author> model = new BindingListModelList<Author>(this.currentDoc.getAuthors(), true);
				lstAuthors.setModel(model);
			}

		} catch (Exception e) {
			LOG.error("Failure in onClick$btnAddNewAuthor", e);
			throw new RuntimeException(e);
		}
	}

	public void onClick$btnSave(Event event) {
		LOG.debug("onClick$btnSave");
		try {
			this.currentDoc.setName(txtDocName.getValue());
			this.currentDoc.setNameLocal(txtDocNameLocal.getValue());
			this.currentDoc.setPublisher(txtPublisher.getValue());
			this.currentDoc.setCity(txtCity.getValue());
			this.currentDoc.setYear(Integer.parseInt(txtYear.getValue()));
			this.currentDoc.setReference(txtReference.getValue());

			boolean isNew = this.currentDoc.getId() == 0;
			this.currentDoc.save();

			Messagebox.show(Labels.getLabel("button.saveComplete"));

			DocumentController docController = (DocumentController) winUpdateDocument.getAttribute(UpdateDocumentController.ATTR_DOC_CONTROLLER);
			if (isNew)
				docController.selectNewDoc(this.currentDoc);
			else
				docController.reloadDoc();

			winUpdateDocument.setVisible(false);
		} catch (Exception e) {
			LOG.error("Failure in onClick$btnSave", e);
			throw new RuntimeException(e);
		}
	}

	public void onClick$btnCancel(Event event) {
		winUpdateDocument.setVisible(false);
	}

	class UpdateDocumentControllerModalListener implements EventListener<Event> {

		@Override
		public void onEvent(Event event) throws Exception {
			LOG.debug("UpdateDocumentControllerModalListener:onModalOpen");
			currentDoc = (JochreDocument) event.getData();
			LOG.debug("currentDoc: " + currentDoc);
			if (currentDoc == null)
				currentDoc = new JochreDocument(jochreSession);
			txtDocName.setValue(currentDoc.getName());
			txtDocNameLocal.setValue(currentDoc.getNameLocal());
			txtPublisher.setValue(currentDoc.getPublisher());
			txtCity.setValue(currentDoc.getCity());
			txtYear.setValue("" + currentDoc.getYear());
			txtReference.setValue(currentDoc.getReference());
			lstAuthors.setModel(new BindingListModelList<Author>(currentDoc.getAuthors(), true));
		}

	}

}
