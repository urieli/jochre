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

import com.joliciel.jochre.utils.pdf.PdfImageObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.utils.JochreException;

import javax.imageio.ImageIO;

/**
 * Saves a set of images extracted from a pdf document.
 * 
 * @author Assaf Urieli
 *
 */
public class PdfImageSaver implements PdfImageObserver {
  private static final Logger LOG = LoggerFactory.getLogger(PdfImageSaver.class);
  private static String SUFFIX = "png";

  private final String baseName;
  private final File outputDir;

  public PdfImageSaver(String baseName, File outputDir) {
    this.baseName = baseName;
    // Create the output directory if it doesn't exist
    this.outputDir = outputDir;

    LOG.debug("Images will be stored to " + outputDir.getPath());
    if (outputDir.exists() == false)
      outputDir.mkdirs();
  }

  @Override
  public void visitImage(BufferedImage image, String imageName, int pageIndex, int imageIndex) {
    String fileName = baseName;
    if (imageIndex > 0) {
      fileName += "_" + String.format("%04d", pageIndex) + "_" + String.format("%02d", imageIndex);
    } else {
      fileName += "_" + String.format("%04d", pageIndex);
    }

    try {
      ImageIO.write(image, SUFFIX, new File(outputDir, fileName + "." + SUFFIX));
    } catch (IOException e) {
      throw new JochreException(e);
    }
  }

}
