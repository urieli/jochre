package com.joliciel.jochre.web;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.web.ImageController.LetterLabelUpdater;

public class FixTextWindowController extends GenericForwardComposer<Window> {
  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LoggerFactory.getLogger(FixTextWindowController.class);

  static final String ATTR_ROW_TEXT = "RowTextAttribute";
  static final String ATTR_ROW_TEXTBOX = "RowTextBoxAttribute";
  static final String ATTR_LETTER_UPDATER = "LetterUpdaterAttribute";
  static final String ATTR_GROUP = "GroupAttribute";
  static final String ROW_TEXT_PLACE_HOLDER = "[[[xxxxxxx]]]";

  @Wire
  Window winFixText;
  @Wire
  Button btnOK;
  @Wire
  Button btnCancel;
  @Wire
  Row shapeRow;
  @Wire
  Row letterBoxRow;
  @Wire
  Checkbox chkSkip;
  @Wire
  Checkbox chkHardHyphen;
  @Wire
  Checkbox chkBrokenWord;
  @Wire
  Checkbox chkSegmentProblem;

  public FixTextWindowController() {
  }

  @Override
  public void doAfterCompose(Window window) throws Exception {
    super.doAfterCompose(window);
  }

  @Listen("onClick = #btnOK")
  public void onClick$btnOK(Event event) {
    // TODO: replacing all occurrences of the same word on the line instead
    // of just the current one
    LOG.debug("onClick$btnOK");
    winFixText.setVisible(false);
    String rowText = (String) winFixText.getAttribute(FixTextWindowController.ATTR_ROW_TEXT);
    Textbox rowTextBox = (Textbox) winFixText.getAttribute(FixTextWindowController.ATTR_ROW_TEXTBOX);
    LetterLabelUpdater updater = (LetterLabelUpdater) winFixText.getAttribute(FixTextWindowController.ATTR_LETTER_UPDATER);
    GroupOfShapes group = (GroupOfShapes) winFixText.getAttribute(FixTextWindowController.ATTR_GROUP);
    group.setSkip(chkSkip.isChecked());
    group.setHardHyphen(chkHardHyphen.isChecked());
    group.setBrokenWord(chkBrokenWord.isChecked());
    group.setSegmentationProblem(chkSegmentProblem.isChecked());
    group.save();

    List<Textbox> letterBoxes = new ArrayList<Textbox>();
    for (Object child : letterBoxRow.getChildren()) {
      if (child instanceof Textbox) {
        letterBoxes.add((Textbox) child);
      }
    }
    StringBuilder sb = new StringBuilder();
    // for (Textbox letterBox : letterBoxes) {
    for (Shape shape : group.getShapes()) {
      Textbox letterBox = (Textbox) letterBoxRow.getFellow("FixTextLetterBox_" + shape.getId());
      String letter = letterBox.getText();
      String newLetter = ImageController.getLetterForDisplay(letter);
      LOG.debug("Letter: " + letter + ", newLetter: " + newLetter);
      sb.append(newLetter);
    }
    LOG.debug(sb.toString());

    String newText = rowText.replace(FixTextWindowController.ROW_TEXT_PLACE_HOLDER, sb.toString());
    rowTextBox.setText(newText);

    updater.updateLetterLabels();

  }

  @Listen("onClick = #btnCancel")
  public void onClick$btnCancel(Event event) {
    winFixText.setVisible(false);
  }

}
