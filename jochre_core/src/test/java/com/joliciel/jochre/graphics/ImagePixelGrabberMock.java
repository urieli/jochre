package com.joliciel.jochre.graphics;

import com.joliciel.jochre.utils.graphics.ImagePixelGrabber;

public class ImagePixelGrabberMock implements ImagePixelGrabber {
  private int[] pixels;
  private int width;
  private int height;
  
  public ImagePixelGrabberMock(int[] pixels, int width, int height) {
    this.pixels = pixels;
    this.width = width;
    this.height = height;
  }
  
  @Override
  public Object getPixels() {
    return null;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public int getNumColors() {
    return 0;
  }

  @Override
  public int getRed(int pixel) {
    return 0;
  }

  @Override
  public int getGreen(int pixel) {
    return 0;
  }

  @Override
  public int getBlue(int pixel) {
    return 0;
  }

  @Override
  public int getPixelBrightness(int x, int y) {
    return pixels[y*width + x];
  }

}
