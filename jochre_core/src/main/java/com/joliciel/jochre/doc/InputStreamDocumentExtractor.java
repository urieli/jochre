///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.jochre.doc;

import com.joliciel.talismane.utils.Monitorable;
import com.joliciel.talismane.utils.MultiTaskProgressMonitor;
import com.joliciel.talismane.utils.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;

/**
 * An interface for extracting a JochreDocument from an image input stream.
 * 
 * @author Assaf Urieli
 *
 */
public class InputStreamDocumentExtractor implements Monitorable, Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(InputStreamDocumentExtractor.class);
  private final SourceFileProcessor documentProcessor;
  private MultiTaskProgressMonitor currentMonitor;
  private final InputStream imageInputStream;
  private final String fileName;
  private int pageNumber = 1;

  public InputStreamDocumentExtractor(InputStream imageInputStream, String fileName, SourceFileProcessor documentProcessor) {
    this.documentProcessor = documentProcessor;
    this.imageInputStream = imageInputStream;
    this.fileName = fileName;
  }

  @Override
  public void run() {
    this.extractDocument();
  }

  public JochreDocument extractDocument() {
    LOG.debug("InputStreamDocumentExtractor.extractDocument");
    try {


      JochreDocument doc = this.documentProcessor.onDocumentStart();
      doc.setTotalPageCount(1);

      int currentPageNumber = this.pageNumber;

      JochrePage page = this.documentProcessor.onPageStart(currentPageNumber++);

      BufferedImage image = ImageIO.read(this.imageInputStream);
      String imageName = this.fileName;

      if (currentMonitor != null && documentProcessor instanceof Monitorable) {
        ProgressMonitor monitor = ((Monitorable) documentProcessor).monitorTask();
        currentMonitor.startTask(monitor, 1.0);
      }

      documentProcessor.onImageFound(page, image, imageName, 0);
      if (currentMonitor != null && documentProcessor instanceof Monitorable) {
        currentMonitor.endTask();
      }

      this.documentProcessor.onPageComplete(page);

      this.documentProcessor.onDocumentComplete(doc);
      this.documentProcessor.onAnalysisComplete();

      if (currentMonitor != null)
        currentMonitor.setFinished(true);
      return doc;
    } catch (Exception e) {
      LOG.debug("Exception occurred. Have monitor? " + currentMonitor);
      if (currentMonitor != null)
        currentMonitor.setException(e);
      LOG.error("Exception while processing document", e);
      throw new RuntimeException(e);
    } finally {
      LOG.debug("Exit InputStreamDocumentExtractor.extractDocument");
    }
  }

  @Override
  public ProgressMonitor monitorTask() {
    currentMonitor = new MultiTaskProgressMonitor();

    return currentMonitor;
  }

  /**
   * The page number to assign to this image.
   */
  public int getPageNumber() {
    return pageNumber;
  }

  public void setPageNumber(int pageNumber) {
    this.pageNumber = pageNumber;
  }

}
