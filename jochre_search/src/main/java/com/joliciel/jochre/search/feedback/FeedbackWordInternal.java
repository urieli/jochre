package com.joliciel.jochre.search.feedback;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

interface FeedbackWordInternal extends FeedbackWord {

	public void setInitialGuess(String initialGuess);

	public void setSecondRectangle(Rectangle secondRectangle);

	public void setSecondRowId(int secondRowId);

	public void setSecondRow(FeedbackRow secondRow);

	public void setRectangle(Rectangle rectangle);

	public void setRowId(int rowId);

	public void setRow(FeedbackRow row);

	public void setId(int id);

	public void setImage(BufferedImage image);
	
	public boolean isNew();

	public void save();

}
