package com.joliciel.jochre.search.feedback;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

interface FeedbackRowInternal extends FeedbackRow {

	public abstract void setImage(BufferedImage image);

	public abstract void setRectangle(Rectangle rectangle);

	public abstract void setPageIndex(int pageIndex);

	public abstract void setDocumentId(int documentId);

	public abstract void setDocument(FeedbackDocument document);

	public abstract void setId(int id);

	public abstract boolean isNew();

	public abstract void save();

}
