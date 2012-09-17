package com.joliciel.jochre.pdf;

import java.io.File;

import com.joliciel.jochre.doc.SourceFileProcessor;
import com.joliciel.jochre.graphics.GraphicsService;

class PdfServiceImpl implements PdfService {
	PdfImageSaver pdfImageSaver = null;
	GraphicsService graphicsService;
	
	@Override
	/**
	 * @param pdfFile
	 * @param firstPage a value of -1 means no first page
	 * @param lastPage a value of -1 means no last page
	 * @param documentProcessor a processor for the document being created (to allow processing as we go).
	 */
	public PdfImageVisitor getPdfImageVisitor(File pdfFile, int firstPage, int lastPage,
			SourceFileProcessor documentProcessor) {
		PdfImageVisitorImpl pdfImageExtractor = new PdfImageVisitorImpl(pdfFile, firstPage, lastPage, documentProcessor);
		pdfImageExtractor.setGraphicsService(this.getGraphicsService());
		return pdfImageExtractor;
	}
	
	@Override
	public PdfImageSaver getPdfImageSaver() {
		if (pdfImageSaver==null) {
			pdfImageSaver = new PdfImageSaverImpl();
		}
		return pdfImageSaver;
	}

	public GraphicsService getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}
	
	
}
