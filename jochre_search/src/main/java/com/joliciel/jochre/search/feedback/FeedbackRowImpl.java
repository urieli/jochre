package com.joliciel.jochre.search.feedback;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

class FeedbackRowImpl implements FeedbackRowInternal {
	private int id;
	private FeedbackDocument document;
	private int documentId;
	private int pageIndex;
	private Rectangle rectangle;
	private BufferedImage image;
	private FeedbackServiceInternal feedbackService;
	public int getId() {
		return id;
	}
	@Override
	public void setId(int id) {
		this.id = id;
	}
	public FeedbackDocument getDocument() {
		if (this.document==null && this.documentId!=0)
			this.document = this.feedbackService.loadDocument(this.documentId);
		return document;
	}
	@Override
	public void setDocument(FeedbackDocument document) {
		this.document = document;
		if (document!=null) {
			this.documentId = document.getId();
		}
	}
	public int getDocumentId() {
		return documentId;
	}
	@Override
	public void setDocumentId(int documentId) {
		this.documentId = documentId;
	}
	public int getPageIndex() {
		return pageIndex;
	}
	@Override
	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
	}
	public Rectangle getRectangle() {
		return rectangle;
	}
	@Override
	public void setRectangle(Rectangle rectangle) {
		this.rectangle = rectangle;
	}
	public BufferedImage getImage() {
		return image;
	}
	@Override
	public void setImage(BufferedImage image) {
		this.image = image;
	}
	@Override
	public boolean isNew() {
		return id==0;
	}
	public FeedbackServiceInternal getFeedbackService() {
		return feedbackService;
	}
	public void setFeedbackService(FeedbackServiceInternal feedbackService) {
		this.feedbackService = feedbackService;
	}
	@Override
	public void save() {
		this.feedbackService.saveRowInternal(this);
	}
}
