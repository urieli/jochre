package com.joliciel.jochre.utils.pdf;

import java.awt.image.BufferedImage;

public interface PdfImageObserver {
  /**
   * Visit a single image.
   */
  void visitImage(BufferedImage image, String imageName, int pageIndex, int imageIndex);
}
