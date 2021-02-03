///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Assaf Urieli
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
package com.joliciel.jochre.search;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.joliciel.jochre.utils.pdf.PdfImageObserver;
import com.joliciel.jochre.utils.pdf.PdfImageVisitor;

public class PdfImageReader {
  private final File pdfFile;

  public PdfImageReader(File pdfFile) {
    this.pdfFile = pdfFile;
  }

  public BufferedImage readImage(int pageNumber) {
    // assuming only one image per PDF page
    Set<Integer> pages = new HashSet<>();
    pages.add(pageNumber);
    PdfImageVisitor pdfImageVisitor = new PdfImageVisitor(this.pdfFile, pages);
    PdfImageReaderInternal imageReader = new PdfImageReaderInternal();
    pdfImageVisitor.addImageObserver(imageReader);
    pdfImageVisitor.visitImages();
    BufferedImage image = imageReader.getImage();
    return image;
  }

  public final static class PdfImageReaderInternal implements PdfImageObserver {
    BufferedImage image = null;

    @Override
    public void visitImage(BufferedImage currentImage, String imageName, int pageIndex, int imageIndex) {
      if (this.image==null) {
        this.image = currentImage;
      } else {
        // If there are multiple images we take the biggest one
        long currentImageSize = currentImage.getHeight() * currentImage.getWidth();
        long imageSize = image.getHeight() * image.getWidth();
        if (currentImageSize > imageSize) {
          this.image = currentImage;
        }
      }
    }

    public BufferedImage getImage() {
      return image;
    }
  }
}
