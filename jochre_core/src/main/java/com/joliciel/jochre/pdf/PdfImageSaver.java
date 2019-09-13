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
package com.joliciel.jochre.pdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.utils.JochreException;
import com.joliciel.jochre.utils.pdf.AbstractPdfImageVisitor;

/**
 * Saves a set of images extracted from a pdf document.
 * 
 * @author Assaf Urieli
 *
 */
public class PdfImageSaver extends AbstractPdfImageVisitor {
  private static final Logger LOG = LoggerFactory.getLogger(PdfImageSaver.class);
  private static String SUFFIX = "png";

  private final File outputDir;
  private final Set<Integer> pages;

  /**
   * 
   * @param pdfFile
   *          File to read
   * @param outputDirectory
   *          Where to save the pages.
   * @param pages
   *          Pages to process, empty set means all pages
   */
  public PdfImageSaver(File pdfFile, String outputDirectory, Set<Integer> pages) {
    super(pdfFile);
    // Create the output directory if it doesn't exist
    this.outputDir = new File(outputDirectory);

    LOG.debug("Images will be stored to " + outputDirectory);
    if (outputDir.exists() == false)
      outputDir.mkdirs();
    this.pages = pages;
  }

  /**
   * Save the images to the outputDirectory indicated.
   */
  public void saveImages() {
    this.visitImages(pages);
  }

  @Override
  protected void visitImage(BufferedImage image, String imageName, int pageIndex, int imageIndex) {
    String fileName = this.getPdfFile().getName().substring(0, this.getPdfFile().getName().lastIndexOf('.'));
    if (imageIndex > 0) {
      fileName += "_" + String.format("%04d", pageIndex) + "_" + String.format("%02d", imageIndex) + "." + SUFFIX;
    } else {
      fileName += "_" + String.format("%04d", pageIndex) + "." + SUFFIX;
    }
    try {
      ImageIO.write(image, SUFFIX, new File(outputDir, fileName));
    } catch (IOException e) {
      throw new JochreException(e);
    }
  }

}
