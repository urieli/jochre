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
import java.util.*;

import com.joliciel.jochre.utils.graphics.ImageUtils;
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
public class PdfImageVisitor {
  private static final Logger LOG = LoggerFactory.getLogger(PdfImageVisitor.class);
  private PDDocument pdfDocument = null;
  private File pdfFile;
  private Map<String, String> fields = new TreeMap<>();
  private boolean docClosed = false;
  private boolean stopOnError = false;
  private final List<PdfImageObserver> imageObservers = new ArrayList<>();
  private final Set<Integer> pages;

  public PdfImageVisitor(File pdfFile) {
    this(pdfFile, new HashSet<>());
  }
  
  public PdfImageVisitor(File pdfFile, Set<Integer> pages) {
    try {
      this.pdfFile = pdfFile;
      this.pages = pages;

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
   */
  final public void visitImages() {
    try {
      int i = 0;
      for (PDPage pdfPage : pdfDocument.getPages()) {
        i++;
        if (!pages.isEmpty() && !pages.contains(i))
          continue;

        LOG.info("Decoding page " + i + " (out of " + pdfDocument.getNumberOfPages() + ")");
        
        int rotation = pdfPage.getRotation();
        
        try {
          ImageLocationExtractor imageLocationExtractor = new ImageLocationExtractor();
          ImageCollector imageCollector = new ImageCollector();
          imageLocationExtractor.addObserver(imageCollector);
          imageLocationExtractor.processPage(pdfPage);
          PDResources resources = pdfPage.getResources();
          
          Set<Integer> combineWithNext = new HashSet<>();
          for (int j=1; j<imageCollector.getImages().size(); j++) {
            // if two subsequent images are overlaid, combine them
            PdfImageWithLocation image1 = imageCollector.getImages().get(j-1);
            PdfImageWithLocation image2 = imageCollector.getImages().get(j);
            float intersection = Float.max(0f, 
                Float.min(image2.scaledLeft + image2.scaledWidth, image1.scaledLeft + image1.scaledWidth)
                - Float.max(image2.scaledLeft, image1.scaledLeft))
                * Float.max(0f,
                    Float.min(image2.scaledTop + image2.scaledHeight, image1.scaledTop + image1.scaledHeight)
                        - Float.max(image2.scaledTop, image1.scaledTop));
            float image1Area = image1.scaledWidth * image1.scaledHeight;
            float image2Area = image2.scaledWidth * image2.scaledHeight;
            float union = image1Area + image2Area - intersection;
            float ratio = intersection / union;
            
            // For simplicity, we assume images are either entirely overlaid, or not overlaid at all
            if (ratio > 0.99f) {
              combineWithNext.add(j-1);
            }
          }
          
          BufferedImage previousImage = null;
          for (int j=0; j<imageCollector.getImages().size(); j++) {
            PdfImageWithLocation imageWithLocation = imageCollector.getImages().get(j);
            PDImageXObject pdfImage = imageWithLocation.getImage();
            LOG.debug("Found image of type " + pdfImage.getSuffix());
            BufferedImage image = pdfImage.getImage();
            if (image == null) {
              throw new PdfImageExtractionException("Something went wrong: unable to extract image " + j
                  + " in file  " + pdfFile.getAbsolutePath() + ", page " + i);
            }
            
            if (combineWithNext.contains(j-1)) {
              assert previousImage != null;
              image = ImageUtils.overlayImages(previousImage, image);
            }
            
            if (rotation!=0) {
              LOG.debug("Page rotation:" + rotation);
              image = ImageUtils.rotate(image, rotation);
            }
            
            if (combineWithNext.contains(j)) {
              previousImage = image;
            } else {
              for (PdfImageObserver imageObserver : imageObservers) {
                imageObserver.visitImage(image, imageWithLocation.getName(), i, j);
              }
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

  private static class PdfImageWithLocation {
    private final PDImageXObject image;
    private final String name;
    private final float scaledLeft;
    private final float scaledTop;
    private final float scaledWidth;
    private final float scaledHeight;

    public PdfImageWithLocation(PDImageXObject image, String name, float scaledLeft, float scaledTop, float scaledWidth, float scaledHeight) {
      this.image = image;
      this.name = name;
      this.scaledLeft = scaledLeft;
      this.scaledTop = scaledTop;
      this.scaledWidth = scaledWidth;
      this.scaledHeight = scaledHeight;
    }

    public PDImageXObject getImage() {
      return image;
    }

    public String getName() {
      return name;
    }
  }
  private static class ImageCollector implements ImageLocationExtractor.ImageLocationObserver {
    private final List<PdfImageWithLocation> images = new ArrayList<>();
    
    @Override
    public void onImageFound(PDImageXObject image, String name, float scaledLeft, float scaledTop, float scaledWidth, float scaledHeight) {
      images.add(new PdfImageWithLocation(image, name, scaledLeft, scaledTop, scaledWidth, scaledHeight));
    }

    public List<PdfImageWithLocation> getImages() {
      return images;
    }
  }

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

  public Set<Integer> getPages() {
    return pages;
  }

  public void addImageObserver(PdfImageObserver imageObserver) {
    this.imageObservers.add(imageObserver);
  }
}
