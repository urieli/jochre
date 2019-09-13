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
package com.joliciel.jochre.utils.pdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.utils.JochreException;

/**
 * A base class for visiting the images in a Pdf document one at a time.
 * 
 * @author Assaf Urieli
 *
 */
public abstract class AbstractPdfImageVisitor {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractPdfImageVisitor.class);
  private PDDocument pdfDocument = null;
  private File pdfFile;
  private Map<String, String> fields = new TreeMap<>();
  private boolean docClosed = false;
  private boolean stopOnError = false;

  public AbstractPdfImageVisitor(File pdfFile) {
    try {
      this.pdfFile = pdfFile;

      pdfDocument = PDDocument.load(pdfFile);
      PDDocumentInformation info = pdfDocument.getDocumentInformation();
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
      fields.put("PageCount", "" + pdfDocument.getNumberOfPages());
      if (info.getTitle() != null)
        fields.put("Title", info.getTitle());
      if (info.getAuthor() != null)
        fields.put("Author", info.getAuthor());
      if (info.getSubject() != null)
        fields.put("Subject", info.getSubject());
      if (info.getKeywords() != null)
        fields.put("Keywords", info.getKeywords());
      if (info.getCreator() != null)
        fields.put("Creator", info.getCreator());
      if (info.getProducer() != null)
        fields.put("Producer", info.getProducer());
      if (info.getCreationDate() != null)
        fields.put("CreateDate", dateFormat.format(info.getCreationDate().getTime()));
      if (info.getModificationDate() != null)
        fields.put("ModificationDate", dateFormat.format(info.getModificationDate().getTime()));
      for (String metaDataField : info.getMetadataKeys()) {
        fields.put(metaDataField, info.getCustomMetadataValue(metaDataField));
      }

    } catch (FileNotFoundException fnfe) {
      throw new RuntimeException(fnfe);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Visit all of the images in a pdf file.
   * 
   * @param firstPage
   *          a value of -1 means no first page
   * @param lastPage
   *          a value of -1 means no last page
   */
  final protected void visitImages(Set<Integer> pages) {
    try {
      int i = 0;
      for (PDPage pdfPage : pdfDocument.getPages()) {
        i++;
        if (!pages.isEmpty() && !pages.contains(i))
          continue;

        LOG.info("Decoding page " + i + " (out of " + pdfDocument.getNumberOfPages() + ")");

        try {
          PDResources resources = pdfPage.getResources();
          Iterator<COSName> pdxObjects = resources.getXObjectNames().iterator();
          int j = 0;
          while (pdxObjects.hasNext()) {
            COSName cosName = pdxObjects.next();

            PDXObject pdxObject = resources.getXObject(cosName);
            if (pdxObject instanceof PDImageXObject) {
              PDImageXObject pdfImage = (PDImageXObject) pdxObject;
              BufferedImage image = pdfImage.getImage();
              if (image == null) {
                throw new PdfImageExtractionException("Something went wrong: unable to extract image " + j
                    + " in file  " + pdfFile.getAbsolutePath() + ", page " + i);
              }
              this.visitImage(image, cosName.getName(), i, j);
              j++;
            }
          }
        } catch (PdfImageExtractionException e) {
          LOG.error("Error in file  " + pdfFile.getAbsolutePath() + ", page " + i, e);
          if (stopOnError)
            throw e;
        } catch (IOException e) {
          LOG.error("Error in file  " + pdfFile.getAbsolutePath() + ", page " + i, e);
          if (stopOnError)
            throw new RuntimeException(e);
        } catch (JochreException e) {
          LOG.error("Error in file  " + pdfFile.getAbsolutePath() + ", page " + i, e);
          if (stopOnError)
            throw e;
        }
      } // next page
    } finally {
      this.close();
    }
  }

  /**
   * Visit a single image.
   */
  protected abstract void visitImage(BufferedImage image, String imageName, int pageIndex, int imageIndex);

  public int getPageCount() {
    return pdfDocument.getNumberOfPages();
  }

  public File getPdfFile() {
    return pdfFile;
  }

  public Map<String, String> getFields() {
    return fields;
  }

  public void close() {
    try {
      if (!docClosed) {
        pdfDocument.close();
        docClosed = true;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Should processing stop if an error is encountered extracting an image from a
   * given page.
   */
  public boolean isStopOnError() {
    return stopOnError;
  }

  public void setStopOnError(boolean stopOnError) {
    this.stopOnError = stopOnError;
  }

}
