package com.joliciel.jochre.pdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.doc.SourceFileProcessor;
import com.joliciel.jochre.utils.pdf.AbstractPdfImageVisitor;
import com.joliciel.talismane.utils.Monitorable;
import com.joliciel.talismane.utils.MultiTaskProgressMonitor;
import com.joliciel.talismane.utils.ProgressMonitor;

/**
 * Visits a set of images from a pdf document, and returns them as a
 * JochreDocument.
 * 
 * @author Assaf Urieli
 *
 */
public class PdfImageVisitor extends AbstractPdfImageVisitor implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(PdfImageVisitor.class);

	private final SourceFileProcessor documentProcessor;
	private final int firstPage;
	private final int lastPage;
	MultiTaskProgressMonitor currentMonitor;

	/**
	 * @param firstPage
	 *            a value of -1 means no first page
	 * @param lastPage
	 *            a value of -1 means no last page
	 * @param documentProcessor
	 *            a processor for the document being created (to allow
	 *            processing as we go).
	 */
	public PdfImageVisitor(File pdfFile, int firstPage, int lastPage, SourceFileProcessor documentProcessor) {
		super(pdfFile);
		this.documentProcessor = documentProcessor;
		this.firstPage = firstPage;
		this.lastPage = lastPage;
	}

	@Override
	public void run() {
		this.visitImages();
	}

	/**
	 * Visit the images and return the JochreDocument containing them.
	 */
	public JochreDocument visitImages() {
		try {
			LOG.debug("PdfImageVisitorImpl.visitImages");
			if (this.currentMonitor != null)
				currentMonitor.setCurrentAction("imageMonitor.extractingNextImage");

			JochreDocument jochreDocument = this.documentProcessor.onDocumentStart();
			jochreDocument.setTotalPageCount(this.getPageCount());

			for (Entry<String, String> field : this.getFields().entrySet()) {
				jochreDocument.getFields().put(field.getKey(), field.getValue());
			}

			this.visitImages(firstPage, lastPage);

			JochrePage finalPage = jochreDocument.getCurrentPage();
			if (finalPage != null) {
				documentProcessor.onPageComplete(finalPage);
			}

			this.documentProcessor.onDocumentComplete(jochreDocument);
			if (this.currentMonitor != null) {
				currentMonitor.setFinished(true);
			}
			return jochreDocument;
		} catch (Exception e) {
			if (this.currentMonitor != null)
				this.currentMonitor.setException(e);
			LOG.error("Failed processing in " + this.getClass().getSimpleName(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void visitImage(BufferedImage image, String imageName, int pageIndex, int imageIndex) {
		LOG.debug("visitImage " + imageName + ", " + pageIndex + ", " + imageIndex);
		if (this.currentMonitor != null)
			currentMonitor.setCurrentAction("");

		JochrePage currentPage = documentProcessor.getDocument().getCurrentPage();
		if (currentPage == null || currentPage.getIndex() != pageIndex) {
			if (currentPage != null) {
				documentProcessor.onPageComplete(currentPage);
			}
			currentPage = documentProcessor.onPageStart(pageIndex);

		}

		if (currentMonitor != null && documentProcessor instanceof Monitorable) {
			ProgressMonitor monitor = ((Monitorable) documentProcessor).monitorTask();
			double percentAllotted = (1 / (double) ((lastPage - firstPage) + 1));
			currentMonitor.startTask(monitor, percentAllotted);
		}

		String prettyName = this.getPdfFile().getName();
		if (prettyName.indexOf('.') >= 0)
			prettyName = prettyName.substring(0, prettyName.indexOf('.'));
		prettyName += "_" + pageIndex;
		prettyName += "_" + imageIndex;

		documentProcessor.onImageFound(currentPage, image, prettyName, imageIndex);
		if (currentMonitor != null && documentProcessor instanceof Monitorable) {
			currentMonitor.endTask();
		}

		if (this.currentMonitor != null)
			currentMonitor.setCurrentAction("imageMonitor.extractingNextImage");
	}

	public ProgressMonitor monitorTask() {
		currentMonitor = new MultiTaskProgressMonitor();
		return currentMonitor;
	}

}
