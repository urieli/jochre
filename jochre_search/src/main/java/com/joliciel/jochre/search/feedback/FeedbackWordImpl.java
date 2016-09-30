package com.joliciel.jochre.search.feedback;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

class FeedbackWordImpl implements FeedbackWordInternal {
	private int id;
	private FeedbackRow row;
	private int rowId;
	private Rectangle rectangle;
	private FeedbackRow secondRow;
	private int secondRowId;
	private Rectangle secondRectangle;
	private String initialGuess;
	private BufferedImage image;
	private FeedbackServiceInternal feedbackService;

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public FeedbackRow getRow() {
		if (this.row == null && this.rowId != 0)
			this.row = this.feedbackService.loadRow(this.rowId);
		return row;
	}

	@Override
	public void setRow(FeedbackRow row) {
		this.row = row;
		if (row != null)
			this.rowId = row.getId();
	}

	@Override
	public int getRowId() {
		return rowId;
	}

	@Override
	public void setRowId(int rowId) {
		this.rowId = rowId;
	}

	@Override
	public Rectangle getRectangle() {
		return rectangle;
	}

	@Override
	public void setRectangle(Rectangle rectangle) {
		this.rectangle = rectangle;
	}

	@Override
	public FeedbackRow getSecondRow() {
		if (this.secondRow == null && this.secondRowId != 0)
			this.secondRow = this.feedbackService.loadRow(this.secondRowId);
		return secondRow;
	}

	@Override
	public void setSecondRow(FeedbackRow secondRow) {
		this.secondRow = secondRow;
		if (secondRow != null)
			this.secondRowId = secondRow.getId();
	}

	@Override
	public int getSecondRowId() {
		return secondRowId;
	}

	@Override
	public void setSecondRowId(int secondRowId) {
		this.secondRowId = secondRowId;
	}

	@Override
	public Rectangle getSecondRectangle() {
		return secondRectangle;
	}

	@Override
	public void setSecondRectangle(Rectangle secondRectangle) {
		this.secondRectangle = secondRectangle;
	}

	@Override
	public String getInitialGuess() {
		return initialGuess;
	}

	@Override
	public void setInitialGuess(String initialGuess) {
		this.initialGuess = initialGuess;
	}

	@Override
	public BufferedImage getImage() {
		return image;
	}

	@Override
	public void setImage(BufferedImage image) {
		this.image = image;
	}

	@Override
	public boolean isNew() {
		return this.id == 0;
	}

	public FeedbackServiceInternal getFeedbackService() {
		return feedbackService;
	}

	public void setFeedbackService(FeedbackServiceInternal feedbackService) {
		this.feedbackService = feedbackService;
	}

	@Override
	public void save() {
		this.feedbackService.saveWordInternal(this);
	}

}
