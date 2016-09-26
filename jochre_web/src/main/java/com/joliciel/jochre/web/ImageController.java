package com.joliciel.jochre.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.image.Images;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.databind.AnnotateDataBinder;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Script;
import org.zkoss.zul.Separator;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Span;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Window;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.graphics.GraphicsDao;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.security.SecurityDao;
import com.joliciel.jochre.security.User;
import com.joliciel.jochre.security.UserRole;
import com.typesafe.config.ConfigFactory;

public class ImageController extends GenericForwardComposer<Window> {
	private static final long serialVersionUID = 5620794383603025597L;

	private static final Logger LOG = LoggerFactory.getLogger(ImageController.class);

	public static final String HEBREW_ACCENTS = "\u0591\u0592\u0593\u0594\u0595\u0596\u0597\u0598\u0599\u059A\u059B\u059C\u059D\u059E\u059F\u05A0\u05A1\u05A2\u05A3\u05A4\u05A5\u05A6\u05A7\u05A8\u05A9\u05AA\u05AB\u05AC\u05AD\u05AE\u05AF\u05B0\u05B1\u05B2\u05B3\u05B4\u05B5\u05B6\u05B7\u05B8\u05B9\u05BA\u05BB\u05BC\u05BD\u05BF\u05C1\u05C2\u05C4\u05C5\u05C7";

	private final JochreSession jochreSession = new JochreSession(ConfigFactory.load());
	private JochreImage currentImage;
	private int imageId;
	private int docId;
	private User currentUser;
	private boolean currentImageOwner;
	private Map<RowOfShapes, Textbox> currentTextBoxes;

	AnnotateDataBinder binder;

	Window winJochreImage;
	Tree docTree;
	Grid rowGrid;
	Button btnSave;
	Button btnSave2;
	Button btnSaveAndExit;
	Button btnSaveAndExit2;
	Script hebrewAccentsSpan;
	Combobox cmbStatus;
	Label lblImageStatus;
	Combobox cmbOwner;
	Label lblOwner;

	public ImageController() {
	}

	@Override
	public void doAfterCompose(Window window) throws Exception {
		super.doAfterCompose(window);

		Session session = Sessions.getCurrent();
		currentUser = (User) session.getAttribute(LoginController.SESSION_JOCHRE_USER);
		if (currentUser == null)
			Executions.sendRedirect("login.zul");

		// comp.setVariable(comp.getId() + "Ctrl", this, true);

		hebrewAccentsSpan.setContent("var hebrewAccents=\"" + HEBREW_ACCENTS + "\";");
		rowGrid.setRowRenderer(new ImageGridRowRenderer());

		HttpServletRequest request = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		imageId = Integer.parseInt(request.getParameter("imageId"));
		GraphicsDao graphicsDao = GraphicsDao.getInstance(jochreSession);
		currentImage = graphicsDao.loadJochreImage(imageId);
		docId = currentImage.getPage().getDocumentId();

		currentImageOwner = (currentUser.getRole().equals(UserRole.ADMIN) || currentImage.getOwner().equals(currentUser));

		if (!currentImageOwner) {
			btnSave.setVisible(false);
			btnSave2.setVisible(false);
			btnSaveAndExit.setVisible(false);
			btnSaveAndExit2.setVisible(false);
			cmbStatus.setVisible(false);
			lblImageStatus.setVisible(true);
		} else {
			btnSave.setVisible(true);
			btnSave2.setVisible(true);
			btnSaveAndExit.setVisible(true);
			btnSaveAndExit2.setVisible(true);
			cmbStatus.setVisible(true);
			lblImageStatus.setVisible(false);
		}

		if (currentUser.getRole().equals(UserRole.ADMIN)) {
			cmbOwner.setVisible(true);
			lblOwner.setVisible(false);
			SecurityDao securityDao = SecurityDao.getInstance(jochreSession);
			List<User> users = securityDao.findUsers();

			List<Comboitem> cmbOwnerItems = cmbOwner.getItems();
			Comboitem selectedUser = null;
			for (User user : users) {
				Comboitem item = new Comboitem(user.getFullName());
				item.setValue(user);
				if (currentImage.getOwner().equals(user))
					selectedUser = item;
				cmbOwnerItems.add(item);
			}
			cmbOwner.setSelectedItem(selectedUser);
		} else {
			cmbOwner.setVisible(false);
			lblOwner.setVisible(true);
			lblOwner.setValue(currentImage.getOwner().getFullName());
		}

		String pageTitle = Labels.getLabel("image.title");
		winJochreImage.getPage().setTitle(pageTitle);

		String windowTitle = Labels.getLabel("image.winJochreImage.title",
				new Object[] { currentImage.getPage().getDocument().getName(), currentImage.getPage().getIndex() });
		winJochreImage.setTitle(windowTitle);

		List<Comboitem> cmbStatusItems = cmbStatus.getItems();
		Comboitem selectedItem = null;

		List<ImageStatus> imageStatuses = new ArrayList<ImageStatus>();
		if (currentUser.getRole().equals(UserRole.ADMIN)) {
			for (ImageStatus imageStatus : ImageStatus.values()) {
				imageStatuses.add(imageStatus);
			}
		} else if (currentImage.getImageStatus().equals(ImageStatus.AUTO_NEW) || currentImage.getImageStatus().equals(ImageStatus.AUTO_VALIDATED)) {
			imageStatuses.add(ImageStatus.AUTO_NEW);
			imageStatuses.add(ImageStatus.AUTO_VALIDATED);
		} else {
			// a bit dangerous - leaving the image as "training" and allowing
			// modifications, but oh well!
			imageStatuses.add(currentImage.getImageStatus());
		}
		for (ImageStatus imageStatus : imageStatuses) {
			String statusLabel = Labels.getLabel("ImageStatus." + imageStatus.getCode());
			Comboitem item = new Comboitem(statusLabel);
			item.setValue(imageStatus.getId());
			if (currentImage.getImageStatus().equals(imageStatus))
				selectedItem = item;
			cmbStatusItems.add(item);
		}
		cmbStatus.setSelectedItem(selectedItem);

		lblImageStatus.setValue(Labels.getLabel("ImageStatus." + currentImage.getImageStatus().getCode()));

		reloadRowGrid();

		binder = new AnnotateDataBinder(window);
		binder.loadAll();
	}

	void reloadRowGrid() {
		LOG.trace("reloadRowGrid");

		List<RowOfShapes> imageRows = new ArrayList<RowOfShapes>();
		for (Paragraph paragraph : currentImage.getParagraphs()) {
			for (RowOfShapes row : paragraph.getRows()) {
				imageRows.add(row);
			}
		}

		currentTextBoxes = new HashMap<RowOfShapes, Textbox>();
		rowGrid.setModel(new SimpleListModel<RowOfShapes>(imageRows));
	}

	class ImageGridRowRenderer implements RowRenderer<RowOfShapes> {

		@Override
		public void render(Row gridRow, RowOfShapes row, int index) throws Exception {
			try {
				// gridRow.setWidth("740px");
				LOG.trace("Rendering paragraph " + row.getParagraph().getIndex() + ", row " + row.getIndex());
				boolean leftToRight = (row.getParagraph().getImage().getPage().getDocument().isLeftToRight());
				if (!leftToRight) {
					gridRow.setAlign("right");
				}
				Div imageDiv = new Div();
				imageDiv.setStyle("position:relative; left:0px;");

				Image gridRowImage = new Image();
				gridRowImage.setContent(row.getImage());
				gridRowImage.setId("RowImage" + row.getId());

				int imageWidth = RowOfShapes.ROW_IMAGE_WIDTH;
				int imageHeight = (int) ((row.getImage().getHeight()) * ((double) RowOfShapes.ROW_IMAGE_WIDTH / (double) row.getImage().getWidth()));
				gridRowImage.setWidth(imageWidth + "px");
				gridRowImage.setHeight(imageHeight + "px");
				// gridRowImage.setStyle("position: absolute; left: 0px;");
				gridRowImage.setStyle("position: relative;  left: 0px;");
				gridRowImage.addEventListener("onClick", new RowImageOnClickEventListener(row));

				imageDiv.appendChild(gridRowImage);

				// get the string to display in the textbox
				StringBuilder sb = new StringBuilder();
				StringBuilder emptyLetterBuilder = new StringBuilder();
				for (GroupOfShapes group : row.getGroups()) {
					for (Shape shape : group.getShapes()) {
						String letter = shape.getLetter();
						if (letter == null)
							letter = "";
						String newLetter = ImageController.getLetterForDisplay(letter);
						if (letter.length() == 0)
							emptyLetterBuilder.append(newLetter);
						else {
							if (emptyLetterBuilder.length() > 0) {
								sb.append(emptyLetterBuilder.toString());
								emptyLetterBuilder = new StringBuilder();
							}
							sb.append(newLetter);
						}
					}
					emptyLetterBuilder.append(" ");
				}

				Div letterDiv = this.getLetterDiv(row, true);
				imageDiv.appendChild(letterDiv);
				if (!row.getParagraph().getImage().getImageStatus().equals(ImageStatus.AUTO_NEW)) {
					Div letterDivOrig = this.getLetterDiv(row, false);
					imageDiv.appendChild(letterDivOrig);
				}

				Textbox gridRowText = new Textbox();
				gridRowText.setWidth("90%");
				// gridRowText.setStyle("position: absolute; left: 0px; top: " +
				// (imageHeight+35) + "px;");
				// gridRowText.setStyle("position: relative;");
				gridRowText.setId("RowText" + row.getId());
				if (!leftToRight)
					gridRowText.setSclass("rightToLeft");
				gridRowText.setText(sb.toString());

				if (!currentImageOwner) {
					gridRowText.setVisible(false);
					gridRowText.setReadonly(true);
				}

				// write an action which updates all of the spans for this line
				// to the current letters onkeyup!
				StringBuilder onkeyupAction = new StringBuilder();

				onkeyupAction.append("updateLetters(this");
				for (GroupOfShapes group : row.getGroups()) {
					onkeyupAction.append(",[");
					boolean firstShape = true;
					for (Shape shape : group.getShapes()) {
						String label = "LetterBox_" + shape.getId();
						if (!firstShape)
							onkeyupAction.append(",");
						onkeyupAction.append("this.$f('" + label + "')");
						firstShape = false;
					}
					onkeyupAction.append("]");
				}
				onkeyupAction.append(");");

				gridRowText.setWidgetListener("onKeyUp", onkeyupAction.toString());
				imageDiv.appendChild(gridRowText);

				currentTextBoxes.put(row, gridRowText);

				Separator separator = new Separator("horizontal");
				separator.setBar(true);
				separator.setSpacing("10px");

				imageDiv.appendChild(separator);

				gridRow.appendChild(imageDiv);

				gridRow.setZclass("z-grid-body");
				gridRow.setHeight((imageHeight + 75) + "px");

				LetterLabelUpdater updater = new LetterLabelUpdater(row);
				updater.updateLetterLabels();
			} catch (Exception e) {
				LOG.error("Failure in ImageGridRowRenderer$render", e);
				throw new RuntimeException(e);
			}
		}

		private Div getLetterDiv(RowOfShapes row, boolean realLetter) {
			// Now comes the fun part: a bunch of span tags to show how the
			// letters align!
			Div letterDiv = new Div();
			int imageWidth = RowOfShapes.ROW_IMAGE_WIDTH;
			boolean leftToRight = (row.getParagraph().getImage().getPage().getDocument().isLeftToRight());

			letterDiv.setWidth(imageWidth + "px");
			letterDiv.setHeight("30px");
			// letterDiv.setStyle("position: absolute; left: 0px; top: " +
			// (imageHeight+5) + "px;");
			letterDiv.setStyle("position: relative; ");
			for (Shape shape : row.getShapes()) {
				Span letterSpan = new Span();
				letterSpan.setWidth("30px");
				Label letterLabel = new Label();
				if (realLetter)
					letterLabel.setId("LetterBox_" + shape.getId());
				else
					letterLabel.setId("LetterBoxOrig_" + shape.getId());
				if (leftToRight) {
					int left = shape.getLeft();
					double scale = (double) imageWidth / (double) row.getParagraph().getImage().getWidth();
					left = (int) Math.floor(scale * left);
					letterSpan.setStyle("position: absolute;  left: " + left + "px;");
				} else {
					int right = shape.getRight();
					double scale = (double) imageWidth / (double) row.getParagraph().getImage().getWidth();
					right = (int) Math.floor(scale * right);
					letterSpan.setStyle("position: absolute;  left: " + (right - 30) + "px;");
					letterSpan.setSclass("rightToLeft");
					letterLabel.setSclass("rightToLeft");
				}

				if (realLetter) {
					letterLabel.setStyle("color:#7E2217; background-color:white;");
				} else {
					letterLabel.setValue(shape.getOriginalGuess());
					if (!(shape.getLetter().equals(shape.getOriginalGuess())))
						letterLabel.setStyle("color:red; background-color:yellow;");
					else
						letterLabel.setStyle("color:darkblue; background-color:white;");
				}
				letterSpan.appendChild(letterLabel);
				letterDiv.appendChild(letterSpan);

			}
			return letterDiv;
		}

	}

	class RowImageOnClickEventListener implements EventListener<MouseEvent> {
		private RowOfShapes row;

		public RowImageOnClickEventListener(RowOfShapes row) {
			this.row = row;
		}

		@Override
		public void onEvent(MouseEvent mouseEvent) throws Exception {
			try {
				int x = mouseEvent.getX();
				int imageWidth = RowOfShapes.ROW_IMAGE_WIDTH;
				double scale = (double) imageWidth / (double) row.getParagraph().getImage().getWidth();
				int clickPos = (int) Math.floor(x / scale);
				GroupOfShapes clickedGroup = null;
				int groupIndex = 0;
				for (GroupOfShapes group : row.getGroups()) {
					if (group.getLeft() <= clickPos && group.getRight() >= clickPos) {
						clickedGroup = group;
						break;
					}
					groupIndex++;
				}
				if (clickedGroup != null) {
					boolean leftToRight = (row.getParagraph().getImage().getPage().getDocument().isLeftToRight());

					List<List<String>> letterGroups = getLetterGroups(row);
					List<String> letterGroup = null;
					if (groupIndex < letterGroups.size())
						letterGroup = letterGroups.get(groupIndex);

					StringBuilder rowTextBuilder = new StringBuilder();
					boolean foundLetterGroup = false;
					int j = 0;
					for (List<String> oneGroup : letterGroups) {
						if (j == groupIndex) {
							foundLetterGroup = true;
							rowTextBuilder.append(FixTextWindowController.ROW_TEXT_PLACE_HOLDER);
						} else {
							for (String letter : oneGroup)
								rowTextBuilder.append(letter);
						}
						rowTextBuilder.append(" ");
						j++;
					}
					if (!foundLetterGroup)
						rowTextBuilder.append(FixTextWindowController.ROW_TEXT_PLACE_HOLDER);

					Window winFixWord = (Window) Path.getComponent("//pgImage/winFixText");
					winFixWord.setAttribute(FixTextWindowController.ATTR_ROW_TEXT, rowTextBuilder.toString());

					Textbox rowTextBox = currentTextBoxes.get(row);
					winFixWord.setAttribute(FixTextWindowController.ATTR_ROW_TEXTBOX, rowTextBox);

					LetterLabelUpdater updater = new LetterLabelUpdater(row);
					winFixWord.setAttribute(FixTextWindowController.ATTR_LETTER_UPDATER, updater);

					winFixWord.setAttribute(FixTextWindowController.ATTR_GROUP, clickedGroup);

					winFixWord.setTitle(Labels.getLabel("image.title") + ": " + clickedGroup.getWord());

					Checkbox chkSkip = (Checkbox) winFixWord.getFellow("chkSkip");
					chkSkip.setChecked(clickedGroup.isSkip());

					Checkbox chkHardHyphen = (Checkbox) winFixWord.getFellow("chkHardHyphen");
					chkHardHyphen.setChecked(clickedGroup.isHardHyphen());

					Checkbox chkBrokenWord = (Checkbox) winFixWord.getFellow("chkBrokenWord");
					chkBrokenWord.setChecked(clickedGroup.isBrokenWord());

					Checkbox chkSegmentProblem = (Checkbox) winFixWord.getFellow("chkSegmentProblem");
					chkSegmentProblem.setChecked(clickedGroup.isSegmentationProblem());

					Grid letterGrid = (Grid) winFixWord.getFellow("letterGrid");
					if (!leftToRight)
						letterGrid.setSclass("rightToLeft");
					LOG.trace(letterGrid.getId());
					Rows letterGridRows = (Rows) winFixWord.getFellow("letterGridRows");
					if (!leftToRight)
						letterGridRows.setSclass("rightToLeft");
					LOG.trace(letterGridRows.getId());

					Row shapeRow = (Row) winFixWord.getFellow("shapeRow");
					LOG.trace(shapeRow.getId());
					shapeRow.getChildren().clear();
					shapeRow.setHeight((clickedGroup.getBottom() - clickedGroup.getTop()) + "px");
					if (!leftToRight)
						shapeRow.setSclass("rightToLeft");

					Row letterBoxRow = (Row) winFixWord.getFellow("letterBoxRow");
					letterBoxRow.getChildren().clear();
					if (!leftToRight)
						letterBoxRow.setSclass("rightToLeft");

					Row arrowRow = (Row) winFixWord.getFellow("arrowRow");
					arrowRow.getChildren().clear();
					if (!leftToRight)
						arrowRow.setSclass("rightToLeft");

					int totalWidth = 0;
					for (int i = 0; i < clickedGroup.getShapes().size(); i++) {
						Shape shape = clickedGroup.getShapes().get(i);

						Image shapeImage = new Image();
						org.zkoss.image.Image convertedImage = Images.encode("shape_" + shape.getId() + ".png", shape.getImage());
						shapeImage.setContent(convertedImage);
						int topOffset = (shape.getTop() - clickedGroup.getTop());
						shapeImage.setStyle("position: relative; top: " + topOffset + "px;");
						shapeImage.setWidth(shape.getImage().getWidth() + "px");
						shapeImage.setHeight((shape.getImage().getHeight()) + "px");
						Cell shapeCell = new Cell();
						int shapeWidth = shape.getImage().getWidth();
						if (shapeWidth < 20)
							shapeWidth = 20;

						shapeCell.setWidth(shapeWidth + "px");

						shapeCell.appendChild(shapeImage);
						shapeRow.appendChild(shapeCell);
						Textbox letterBox = new Textbox();
						letterBox.setId("FixTextLetterBox_" + shape.getId());
						if (letterGroup != null) {
							String letter = "";
							if (i < letterGroup.size())
								letter = letterGroup.get(i);
							if (letter.startsWith("[") && letter.endsWith("]"))
								letter = letter.substring(1, letter.length() - 1);
							letterBox.setText(letter);
						}
						totalWidth += shapeWidth;
						letterBox.setWidth(shapeWidth + "px");
						if (!leftToRight)
							letterBox.setSclass("rightToLeft");

						Cell letterBoxCell = new Cell();
						letterBoxCell.setWidth(shapeWidth + "px");
						letterBoxCell.appendChild(letterBox);
						letterBoxRow.appendChild(letterBoxCell);

						// add arrows for pushing letters right & left
						Hbox hbox = new Hbox();
						if (!leftToRight)
							hbox.setSclass("rightToLeft");
						Cell arrowCell = new Cell();
						arrowCell.setWidth(shapeWidth + "px");
						arrowCell.appendChild(hbox);
						arrowRow.appendChild(arrowCell);
						Image arrowPushForward = new Image();
						StringBuilder textBoxArray = new StringBuilder();
						textBoxArray.append("[");
						boolean firstShape = true;
						for (Shape otherShape : clickedGroup.getShapes()) {
							String label = "FixTextLetterBox_" + otherShape.getId();
							if (!firstShape)
								textBoxArray.append(",");
							textBoxArray.append("this.$f('" + label + "')");
							firstShape = false;
						}
						textBoxArray.append("]");
						arrowPushForward.setWidgetListener("onClick", "pushLetters(1, " + i + "," + textBoxArray.toString() + ");");

						Image arrowPushBack = new Image("images/arrowRight.gif");
						arrowPushBack.setWidgetListener("onClick", "pushLetters(-1, " + i + "," + textBoxArray.toString() + ");");
						if (i != 0)
							hbox.appendChild(arrowPushBack);
						if (i != clickedGroup.getShapes().size() - 1)
							hbox.appendChild(arrowPushForward);
						if (leftToRight) {
							arrowPushForward.setSrc("images/arrowRight.gif");
							arrowPushBack.setSrc("images/arrowLeft.gif");
						} else {
							arrowPushForward.setSrc("images/arrowLeft.gif");
							arrowPushBack.setSrc("images/arrowRight.gif");
						}
					}

					totalWidth = (int) (totalWidth * 1.6);
					letterGrid.setWidth(totalWidth + "px");
					int windowWidth = totalWidth + 60;
					winFixWord.setWidth(windowWidth + "px");
					shapeRow.setHeight((clickedGroup.getBottom() - clickedGroup.getTop()) + "px");
					winFixWord.setVisible(true);

					winFixWord.doModal();
				}
			} catch (Exception e) {
				LOG.error("Failure in RowImageOnClickEventListener$listen", e);
				throw new RuntimeException(e);
			}
		}

	}

	public void onClick$btnSaveAndExit(Event event) {
		try {
			LOG.debug("onClick$btnExitWithoutSave");
			this.save();
			Executions.sendRedirect("docs.zul?docId=" + docId + "&imageId=" + imageId);
		} catch (Exception e) {
			LOG.error("Failure in onClick$btnExitWithoutSave", e);
			throw new RuntimeException(e);
		}
	}

	public void onClick$btnSaveAndExit2(Event event) {
		this.onClick$btnSaveAndExit(event);
	}

	public void onClick$btnSave(Event event) {
		LOG.debug("onClick$btnSave");
		this.save();
	}

	void save() {
		try {
			Comboitem selectedItem = cmbStatus.getSelectedItem();
			ImageStatus imageStatus = ImageStatus.forId((Integer) selectedItem.getValue());
			currentImage.setImageStatus(imageStatus);
			if (currentUser.getRole().equals(UserRole.ADMIN)) {
				User owner = (User) cmbOwner.getSelectedItem().getValue();
				currentImage.setOwner(owner);
			}
			GraphicsDao graphicsDao = GraphicsDao.getInstance(jochreSession);
			graphicsDao.saveJochreImage(currentImage);
			for (Paragraph paragraph : currentImage.getParagraphs()) {
				LOG.trace("Paragraph " + paragraph.getIndex() + ", " + paragraph.getRows().size() + " rows");
				for (RowOfShapes row : paragraph.getRows()) {
					List<List<String>> letterGroups = this.getLetterGroups(row);
					LOG.trace("Row " + row.getIndex() + ", " + row.getGroups().size() + " groups, " + letterGroups.size() + " letter groups");
					Iterator<List<String>> iLetterGroups = letterGroups.iterator();
					for (GroupOfShapes group : row.getGroups()) {
						LOG.trace("Group " + group.getIndex() + " text : " + group.getWord());
						boolean hasChange = false;
						List<String> letters = null;
						if (iLetterGroups.hasNext())
							letters = iLetterGroups.next();
						else
							letters = new ArrayList<String>();

						LOG.trace("Found " + letters.size() + " letters in text");
						Iterator<String> iLetters = letters.iterator();
						for (Shape shape : group.getShapes()) {
							String currentLetter = shape.getLetter();
							if (currentLetter == null)
								currentLetter = "";
							String newLetter = "";
							if (iLetters.hasNext())
								newLetter = iLetters.next();
							if (newLetter.startsWith("[") && newLetter.endsWith("]")) {
								newLetter = newLetter.substring(1, newLetter.length() - 1);
							}
							LOG.trace("currentLetter:  " + currentLetter + ", newLetter: " + newLetter);
							if (!currentLetter.equals(newLetter)) {
								LOG.trace("newLetter: " + newLetter);
								shape.setLetter(newLetter);
								shape.save();
								hasChange = true;
							}
						}

						if (hasChange)
							LOG.trace("Group text after : " + group.getWord());
					} // next group
				} // next row
			} // next paragraph

			Messagebox.show(Labels.getLabel("button.saveComplete"));

		} catch (Exception e) {
			LOG.error("Failure in save", e);
			throw new RuntimeException(e);
		}
	}

	public void onClick$btnSave2(Event event) {
		this.onClick$btnSave(event);
	}

	public void onClick$btnExitWithoutSave(Event event) {
		try {
			LOG.debug("onClick$btnExitWithoutSave");
			Executions.sendRedirect("docs.zul?docId=" + docId + "&imageId=" + imageId);
		} catch (Exception e) {
			LOG.error("Failure in onClick$btnExitWithoutSave", e);
			throw new RuntimeException(e);
		}
	}

	public void onClick$btnExitWithoutSave2(Event event) {
		this.onClick$btnExitWithoutSave(event);
	}

	class LetterLabelUpdater {
		private RowOfShapes row;

		public LetterLabelUpdater(RowOfShapes row) {
			this.row = row;
		}

		public void updateLetterLabels() {
			try {
				LOG.debug("updateLetterLabels");
				List<List<String>> letterGroups = getLetterGroups(row);
				List<List<Label>> labelGroups = new ArrayList<List<Label>>();
				for (GroupOfShapes group : row.getGroups()) {
					List<Label> labelGroup = new ArrayList<Label>();
					labelGroups.add(labelGroup);
					for (Shape shape : group.getShapes()) {
						String labelId = "LetterBox_" + shape.getId();
						Label label = (Label) winJochreImage.getFellow(labelId);
						labelGroup.add(label);
					}
				}

				int letterGroupIndex = 0;
				for (List<Label> labelGroup : labelGroups) {
					// no more groups to assign
					if (letterGroupIndex >= letterGroups.size()) {
						for (Label label : labelGroup)
							label.setValue("");
						continue;
					}
					List<String> letterGroup = letterGroups.get(letterGroupIndex++);
					if (LOG.isTraceEnabled())
						LOG.trace(letterGroup.toString());
					boolean wrongLength = true;
					if (letterGroup != null) {
						wrongLength = (labelGroup.size() != letterGroup.size());
						if (wrongLength)
							LOG.trace("wrongLength");
					}
					int letterIndex = 0;
					for (Label label : labelGroup) {
						// no more letters in this group
						if (letterIndex >= letterGroup.size()) {
							label.setValue("");
							continue;
						}
						String letter = letterGroup.get(letterIndex++);
						if (letter.startsWith("[") && letter.endsWith("]")) {
							letter = letter.substring(1, letter.length() - 1);
						}
						label.setValue(letter);
						if (wrongLength)
							label.setStyle("color:red; background-color:yellow;");
						else
							label.setStyle("color:black; background-color:white;");

						label.invalidate();
					}
				}
			} catch (Exception e) {
				LOG.error("Failure in LetterLabelUpdater$updateLetterLabels", e);
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Divide the text in a given row's textbox into separate letter groups, which
	 * will then be used to populate labels that are aligned with the letters in
	 * the image.
	 * 
	 * @param row
	 * @return
	 */
	List<List<String>> getLetterGroups(RowOfShapes row) {
		List<List<String>> letterGroups = new ArrayList<List<String>>();
		Textbox rowText = currentTextBoxes.get(row);
		if (rowText == null) {
			LOG.trace("No textbox for this row!");
			return letterGroups;
		} else {
			LOG.trace("Found textbox: " + rowText.getId());
		}
		String text = rowText.getText();

		List<String> letters = new ArrayList<String>();
		for (int i = 0; i < text.length(); i++) {
			String letter = text.substring(i, i + 1);
			if (letter.equals(" ")) {
				if (letters.size() > 0) {
					letterGroups.add(letters);
				}
				letters = new ArrayList<String>();
			} else {
				if (letter.equals("[")) {
					int endIndex = text.indexOf("]", i);
					if (endIndex >= 0) {
						letter = text.substring(i, endIndex + 1);
						LOG.trace("Letter: " + letter);
						i = endIndex;
					}
				} else if (letter.equals("-")) {
					if (i + 1 < text.length()) {
						String nextLetter = text.substring(i + 1, i + 2);
						if (nextLetter.equals("-")) {
							letter = "—";
							i++;
						}
					}
				}
				// LOG.debug("Letter: " + letter);
				if (HEBREW_ACCENTS.indexOf(letter) >= 0 && letters.size() != 0) {
					if (letter.equals("ַ") && letters.size() > 1 && letters.get(letters.size() - 1).equals("י") && letters.get(letters.size() - 2).equals("י")) {
						letters.remove(letters.size() - 1);
						letters.remove(letters.size() - 1);
						letters.add("ײַ");
					} else {
						String lastLetter = letters.get(letters.size() - 1);
						lastLetter += letter;
						letters.remove(letters.size() - 1);
						letters.add(lastLetter);
					}
				} else if (letter.equals("װ")) {
					letters.add("ו");
					letters.add("ו");
				} else if (letter.equals("ױ")) {
					letters.add("ו");
					letters.add("י");
				} else if (letter.equals("ײ")) {
					letters.add("י");
					letters.add("י");
				} else if (letter.equals("„")) {
					letters.add(",");
					letters.add(",");
				} else if (letter.equals("“")) {
					letters.add("'");
					letters.add("'");
				} else {
					letters.add(letter);
				}
			}
		}
		if (letters.size() > 0) {
			letterGroups.add(letters);
		}

		LOG.trace("Found " + letterGroups.size() + " letter groups");
		return letterGroups;
	}

	static String getLetterForDisplay(String letter) {
		String newLetter = letter;
		if (letter.equals("װ"))
			newLetter = "וו";
		else if (letter.equals("ױ"))
			newLetter = "וי";
		else if (letter.equals("ײַ"))
			newLetter = "ײַ";
		else if (letter.equals("ײ"))
			newLetter = "יי";
		else if (letter.equals("„"))
			newLetter = ",,";
		else if (letter.equals("“"))
			newLetter = "''";

		if (letter.length() == 2 && ImageController.HEBREW_ACCENTS.indexOf(letter.substring(1)) > 0) {
			// do nothing
		} else if (letter.length() >= 2) {
			newLetter = "[" + letter + "]";
		} else if (letter.length() == 0) {
			newLetter = "[]";
		}
		return newLetter;
	}
}
