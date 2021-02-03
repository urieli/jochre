package com.joliciel.jochre.pdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map.Entry;
import java.util.Set;

import com.joliciel.jochre.utils.pdf.PdfImageObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.doc.SourceFileProcessor;
import com.joliciel.jochre.utils.pdf.PdfImageVisitor;
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
public class PdfDocumentProcessor extends PdfImageVisitor implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(PdfDocumentProcessor.class);

  private final SourceFileProcessor documentProcessor;
  MultiTaskProgressMonitor currentMonitor;

  /**
   * @param pages
   *          Pages to process, empty set means all pages
   * @param documentProcessor
   *          a processor for the document being created (to allow processing as
   *          we go).
   */
  public PdfDocumentProcessor(File pdfFile, Set<Integer> pages, SourceFileProcessor documentProcessor) {
    super(pdfFile, pages);
    this.documentProcessor = documentProcessor;
    this.addImageObserver(new JochreImageVisitor());
  }

  @Override
  public void run() {
    this.process();
  }

  /**
   * Visit the images and return the JochreDocument containing them.
   */
  public JochreDocument process() {
    try {
      LOG.debug("PdfImageVisitorImpl.visitImages");
      if (this.currentMonitor != null)
        currentMonitor.setCurrentAction("imageMonitor.extractingNextImage");

      JochreDocument jochreDocument = this.documentProcessor.onDocumentStart();
      jochreDocument.setTotalPageCount(this.getPageCount());

      for (Entry<String, String> field : this.getFields().entrySet()) {
        jochreDocument.getFields().put(field.getKey(), field.getValue());
      }

      this.visitImages();

      JochrePage finalPage = jochreDocument.getCurrentPage();
      if (finalPage != null) {
        documentProcessor.onPageComplete(finalPage);
      }

      this.documentProcessor.onDocumentComplete(jochreDocument);
      this.documentProcessor.onAnalysisComplete();
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

  private final class JochreImageVisitor implements PdfImageObserver {
    @Override
    public void visitImage(BufferedImage image, String imageName, int pageIndex, int imageIndex) {
      LOG.debug("visitImage " + imageName + ", " + pageIndex + ", " + imageIndex);
      if (currentMonitor != null)
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
        double percentAllotted = (1 / (double) (getPages().size() + 1));
        currentMonitor.startTask(monitor, percentAllotted);
      }

      String prettyName = getPdfFile().getName();
      if (prettyName.indexOf('.') >= 0)
        prettyName = prettyName.substring(0, prettyName.indexOf('.'));
      prettyName += "_" + pageIndex;
      prettyName += "_" + imageIndex;

      documentProcessor.onImageFound(currentPage, image, prettyName, imageIndex);
      if (currentMonitor != null && documentProcessor instanceof Monitorable) {
        currentMonitor.endTask();
      }

      if (currentMonitor != null)
        currentMonitor.setCurrentAction("imageMonitor.extractingNextImage");
    }
  }

  public ProgressMonitor monitorTask() {
    currentMonitor = new MultiTaskProgressMonitor();
    return currentMonitor;
  }

}
