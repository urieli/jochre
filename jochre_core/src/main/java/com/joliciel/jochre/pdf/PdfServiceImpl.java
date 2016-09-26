package com.joliciel.jochre.pdf;

import java.io.File;

import com.joliciel.jochre.doc.SourceFileProcessor;

class PdfServiceImpl implements PdfService {
	PdfImageSaver pdfImageSaver = null;

	@Override
	/**
	 * @param firstPage
	 *          a value of -1 means no first page
	 * @param lastPage
	 *          a value of -1 means no last page
	 * @param documentProcessor
	 *          a processor for the document being created (to allow processing as
	 *          we go).
	 */
	public PdfImageVisitor getPdfImageVisitor(File pdfFile, int firstPage, int lastPage, SourceFileProcessor documentProcessor) {
		PdfImageVisitorImpl pdfImageVisitor = new PdfImageVisitorImpl(pdfFile, firstPage, lastPage, documentProcessor);
		return pdfImageVisitor;
	}

	@Override
	public PdfImageSaver getPdfImageSaver(File pdfFile) {
		if (pdfImageSaver == null) {
			pdfImageSaver = new PdfImageSaverImpl(pdfFile);
		}
		return pdfImageSaver;
	}

}
