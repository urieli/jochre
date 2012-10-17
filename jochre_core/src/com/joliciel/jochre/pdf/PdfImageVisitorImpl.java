package com.joliciel.jochre.pdf;

import java.awt.image.BufferedImage;
import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.SourceFileProcessor;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.Monitorable;
import com.joliciel.talismane.utils.MultiTaskProgressMonitor;
import com.joliciel.talismane.utils.ProgressMonitor;

class PdfImageVisitorImpl extends AbstractPdfImageVisitor implements PdfImageVisitor {
	private static final Log LOG = LogFactory.getLog(PdfImageVisitorImpl.class);
	
	GraphicsService graphicsService;
	SourceFileProcessor documentProcessor;
	int firstPage;
	int lastPage;
	MultiTaskProgressMonitor currentMonitor;
	
	/**
	 * @param pdfFile
	 * @param firstPage a value of -1 means no first page
	 * @param lastPage a value of -1 means no last page
	 * @param documentProcessor a processor for the document being created (to allow processing as we go).
	 */
	public PdfImageVisitorImpl(File pdfFile, int firstPage, int lastPage,
			SourceFileProcessor documentProcessor) {
		super(pdfFile);
		this.documentProcessor = documentProcessor;	
		this.firstPage = firstPage;
		this.lastPage = lastPage;
	}
	
	@Override
	public void run() {
		this.visitImages();
	}

	@Override
	public JochreDocument visitImages() {
		try {
			LOG.debug("PdfImageVisitorImpl.visitImages");
			if (this.currentMonitor!=null)
				currentMonitor.setCurrentAction("imageMonitor.extractingNextImage");
			
			JochreDocument jochreDocument = this.documentProcessor.onDocumentStart();
			jochreDocument.setTotalPageCount(this.getPageCount());
			
			this.visitImages(firstPage, lastPage);
			
			JochrePage finalPage = jochreDocument.getCurrentPage();
			if (finalPage!=null) {
				documentProcessor.onPageComplete(finalPage);
			}
			
			this.documentProcessor.onDocumentComplete(jochreDocument);
			if (this.currentMonitor!=null) {
				currentMonitor.setFinished(true);
			}
			return jochreDocument;
		} catch (Exception e) {
			if (this.currentMonitor!=null)
				this.currentMonitor.setException(e);
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	void visitImage(BufferedImage image, String imageName, int pageIndex,
			int imageIndex) {
		LOG.debug("visitImage " + imageName + ", " + pageIndex + ", " + imageIndex);
		if (this.currentMonitor!=null)
			currentMonitor.setCurrentAction("");

		JochrePage currentPage = documentProcessor.getDocument().getCurrentPage();
		if (currentPage==null||currentPage.getIndex()!=pageIndex) {
			if (currentPage!=null) {
				documentProcessor.onPageComplete(currentPage);
			}
			currentPage = documentProcessor.onPageStart(pageIndex);
			
		}

		if (currentMonitor!=null&&documentProcessor instanceof Monitorable) {
			ProgressMonitor monitor = ((Monitorable)documentProcessor).monitorTask();
			double percentAllotted = (1 / (double)((lastPage - firstPage) + 1));
			currentMonitor.startTask(monitor, percentAllotted);
		}
		
		String prettyName = this.getPdfFile().getName();
		if (prettyName.indexOf('.')>=0)
			prettyName = prettyName.substring(0, prettyName.indexOf('.'));
		prettyName += "_" + pageIndex;
		prettyName += "_" + imageIndex;
		
		documentProcessor.onImageFound(currentPage, image, prettyName, imageIndex);
		if (currentMonitor!=null&&documentProcessor instanceof Monitorable) {
			currentMonitor.endTask();
		}
		
		if (this.currentMonitor!=null)
			currentMonitor.setCurrentAction("imageMonitor.extractingNextImage");
	}

	public GraphicsService getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}

	@Override
	public ProgressMonitor monitorTask() {
		currentMonitor = new MultiTaskProgressMonitor();
		return currentMonitor;
	}

    
    
}
