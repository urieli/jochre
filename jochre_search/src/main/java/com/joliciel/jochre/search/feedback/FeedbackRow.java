package com.joliciel.jochre.search.feedback;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * A row on a particular OCR page for which the user has given feedback. The row
 * image is stored (instead of a word image), since this enables us to better
 * calculate the baseline and other row-level characteristics.
 * 
 * @author Assaf Urieli
 *
 */
public class FeedbackRow {
	private int id;
	private FeedbackDocument document;
	private int documentId;
	private int pageIndex;
	private Rectangle rectangle;
	private BufferedImage image;

	private final FeedbackDAO feedbackDAO;

	public static FeedbackRow findOrCreateRow(FeedbackDocument doc, int pageIndex, Rectangle rectangle, BufferedImage rowImage, FeedbackDAO feedbackDAO) {
		FeedbackRow row = feedbackDAO.findRow(doc, pageIndex, rectangle);
		if (row == null) {
			row = new FeedbackRow(doc, pageIndex, rectangle, rowImage, feedbackDAO);
			row.save();
		}
		return row;
	}

	FeedbackRow(FeedbackDocument doc, int pageIndex, Rectangle rectangle, BufferedImage rowImage, FeedbackDAO feedbackDAO) {
		this(feedbackDAO);
		this.setDocument(document);
		this.setPageIndex(pageIndex);
		this.setRectangle(rectangle);
		this.setImage(rowImage);
	}

	FeedbackRow(FeedbackDAO feedbackDAO) {
		this.feedbackDAO = feedbackDAO;
	}

	/**
	 * The unique internal id for this row.
	 */
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/**
	 * The document containing this row.
	 */
	public FeedbackDocument getDocument() {
		if (this.document == null && this.documentId != 0)
			this.document = this.feedbackDAO.loadDocument(this.documentId);
		return document;
	}

	void setDocument(FeedbackDocument document) {
		this.document = document;
		if (document != null) {
			this.documentId = document.getId();
		}
	}

	public int getDocumentId() {
		return documentId;
	}

	void setDocumentId(int documentId) {
		this.documentId = documentId;
	}

	/**
	 * The page index on which this row is found.
	 */
	public int getPageIndex() {
		return pageIndex;
	}

	void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
	}

	/**
	 * This row's rectangle within the page.
	 */
	public Rectangle getRectangle() {
		return rectangle;
	}

	void setRectangle(Rectangle rectangle) {
		this.rectangle = rectangle;
	}

	/**
	 * This row's image.
	 */
	public BufferedImage getImage() {
		return image;
	}

	void setImage(BufferedImage image) {
		this.image = image;
	}

	boolean isNew() {
		return id == 0;
	}

	void save() {
		feedbackDAO.saveRow(this);
	}
}
