package com.joliciel.jochre.utils.graphics;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;

public class ImageUtils {
  public static BufferedImage indexedToRGB(BufferedImage indexed) {
    if (indexed.getColorModel() instanceof IndexColorModel) {
      BufferedImage rgb = new BufferedImage(indexed.getWidth(), indexed.getHeight(), BufferedImage.TYPE_INT_RGB);
      rgb.createGraphics().drawImage(indexed, 0, 0, null);
      return rgb;
    } else {
      return indexed;
    }
  }
  public static BufferedImage toGreyscale(BufferedImage colorImage) {
    BufferedImage greyImage = new BufferedImage(colorImage.getWidth(), colorImage.getHeight(),
        BufferedImage.TYPE_BYTE_GRAY);
    Graphics g = greyImage.getGraphics();
    g.drawImage(colorImage, 0, 0, null);
    g.dispose();
    return greyImage;
  }
  
  public static BufferedImage alphaToWhite(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = result.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0,0, width, height);

    graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.75f));
    graphics.drawImage(image, 0, 0, null);
    
    graphics.dispose();
    return result;
  }
  
  public static BufferedImage overlayImages(BufferedImage background, BufferedImage foreground) {
    int width = foreground.getWidth();
    int height = foreground.getHeight();
    BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = result.createGraphics();
    
    graphics.drawImage(background, 0, 0, width, height, null);

    graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1.0f));
    graphics.drawImage(foreground, 0, 0, null);

    graphics.dispose();
    return result;
  }
  
  public static BufferedImage toBlackAndWhite(BufferedImage greyImage, int threshold) {
    assert greyImage.getType() == BufferedImage.TYPE_BYTE_GRAY;

    // Allocate the new image
    BufferedImage blackWhiteImage = new BufferedImage(greyImage.getWidth(), greyImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
    
    //Allocate arrays
    int len = greyImage.getWidth()*greyImage.getHeight();
    byte[] src = new byte[len];
    byte[] dst = new byte[len];

    // Read the src image data into the array
    greyImage.getRaster().getDataElements(0, 0, greyImage.getWidth(), greyImage.getHeight(), src);

    // Convert to B&W
    int j = 0;
    for ( int i=0; i<len; i++ ) {
      dst[i] = src[i] <= threshold ? (byte) 255 : (byte) 0;
    }

    // Set the dst image data
    blackWhiteImage.getRaster().setDataElements(0, 0, greyImage.getWidth(), greyImage.getHeight(), dst);

    return blackWhiteImage;
  }

  public static BufferedImage deepCopy(BufferedImage bi) {
    ColorModel cm = bi.getColorModel();
    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    WritableRaster raster = bi.copyData(null);
    return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
  }
  
  public static BufferedImage rotate(BufferedImage image, int degrees) {
    final double rads = Math.toRadians(degrees);
    final double sin = Math.abs(Math.sin(rads));
    final double cos = Math.abs(Math.cos(rads));
    final int w = (int) Math.floor(image.getWidth() * cos + image.getHeight() * sin);
    final int h = (int) Math.floor(image.getHeight() * cos + image.getWidth() * sin);
    final BufferedImage rotatedImage = new BufferedImage(w, h, image.getType());
    final AffineTransform at = new AffineTransform();
    at.translate(w / 2, h / 2);
    at.rotate(rads,0, 0);
    at.translate(-image.getWidth() / 2, -image.getHeight() / 2);
    final AffineTransformOp rotateOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
    rotateOp.filter(image,rotatedImage);
    return rotatedImage;
  }
}
