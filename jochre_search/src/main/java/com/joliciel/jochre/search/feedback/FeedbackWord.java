package com.joliciel.jochre.search.feedback;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.joliciel.jochre.search.JochreIndexWord;

/**
 * A word for which a user has given feedback.
 * 
 * @author Assaf Urieli
 *
 */
public class FeedbackWord {
	private int id;
	private FeedbackRow row;
	private int rowId;
	private Rectangle rectangle;
	private FeedbackRow secondRow;
	private int secondRowId;
	private Rectangle secondRectangle;
	private String initialGuess;
	private BufferedImage image;

	private final FeedbackDAO feedbackDAO;

	public static FeedbackWord findOrCreateWord(JochreIndexWord jochreWord, FeedbackDAO feedbackDAO) {
		FeedbackDocument doc = FeedbackDocument.findOrCreateDocument(jochreWord.getDocument().getPath(), feedbackDAO);
		FeedbackWord word = feedbackDAO.findWord(doc, jochreWord.getPageIndex(), jochreWord.getRectangle());
		if (word == null) {
			word = new FeedbackWord(jochreWord, doc, feedbackDAO);
			word.save();
		}
		return word;
	}

	FeedbackWord(JochreIndexWord jochreWord, FeedbackDocument doc, FeedbackDAO feedbackDAO) {
		this(feedbackDAO);
		this.setRectangle(jochreWord.getRectangle());
		this.setImage(jochreWord.getImage());
		this.setInitialGuess(jochreWord.getText());
		FeedbackRow row = FeedbackRow.findOrCreateRow(doc, jochreWord.getPageIndex(), jochreWord.getRowRectangle(), jochreWord.getRowImage(), feedbackDAO);
		this.setRow(row);
		if (jochreWord.getSecondRectangle() != null) {
			this.setSecondRectangle(jochreWord.getSecondRectangle());
			FeedbackRow row2 = FeedbackRow.findOrCreateRow(doc, jochreWord.getPageIndex(), jochreWord.getSecondRectangle(), jochreWord.getSecondRowImage(),
					feedbackDAO);
			this.setSecondRow(row2);
		}
	}

	FeedbackWord(FeedbackDAO feedbackDAO) {
		this.feedbackDAO = feedbackDAO;
	}

	/**
	 * The unique internal id for this word.
	 */
	public int getId() {
		return id;
	}

	void setId(int id) {
		this.id = id;
	}

	/**
	 * The row on which this word is found.
	 */
	public FeedbackRow getRow() {
		if (this.row == null && this.rowId != 0)
			this.row = this.feedbackDAO.loadRow(this.rowId);
		return row;
	}

	void setRow(FeedbackRow row) {
		this.row = row;
		if (row != null)
			this.rowId = row.getId();
	}

	public int getRowId() {
		return rowId;
	}

	void setRowId(int rowId) {
		this.rowId = rowId;
	}

	/**
	 * The word's rectangle within the page containing it.
	 */
	public Rectangle getRectangle() {
		return rectangle;
	}

	void setRectangle(Rectangle rectangle) {
		this.rectangle = rectangle;
	}

	/**
	 * The row containing the second half of a hyphenated word.
	 */
	public FeedbackRow getSecondRow() {
		if (this.secondRow == null && this.secondRowId != 0)
			this.secondRow = this.feedbackDAO.loadRow(this.secondRowId);
		return secondRow;
	}

	void setSecondRow(FeedbackRow secondRow) {
		this.secondRow = secondRow;
		if (secondRow != null)
			this.secondRowId = secondRow.getId();
	}

	public int getSecondRowId() {
		return secondRowId;
	}

	void setSecondRowId(int secondRowId) {
		this.secondRowId = secondRowId;
	}

	/**
	 * The rectangle containing the 2nd half of a hyphenated word, within the page
	 * containing it.
	 */
	public Rectangle getSecondRectangle() {
		return secondRectangle;
	}

	void setSecondRectangle(Rectangle secondRectangle) {
		this.secondRectangle = secondRectangle;
	}

	/**
	 * The initial guess for this word.
	 */
	public String getInitialGuess() {
		return initialGuess;
	}

	void setInitialGuess(String initialGuess) {
		this.initialGuess = initialGuess;
	}

	/**
	 * This word's image - in the case of a hyphenated word, this combines both
	 * halves. If separate images are required, they can be acquired via the row
	 * images and rectangles.
	 */
	public BufferedImage getImage() {
		return image;
	}

	void setImage(BufferedImage image) {
		this.image = image;
	}

	boolean isNew() {
		return this.id == 0;
	}

	void save() {
		feedbackDAO.saveWord(this);
	}

}
