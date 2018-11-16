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
package com.joliciel.jochre.graphics.util;

/**
 * An interface for getting individual pixel color/brightness out of an image.
 * @author Assaf Urieli
 *
 */
public interface ImagePixelGrabber {
  /**
   * Will return a byte[] for a GIF/PNG, or an int[] for a JPG.
   */
  public abstract Object getPixels();

  public abstract int getWidth();

  public abstract int getHeight();

  /**
   * Only valid for indexed color models.
   */
  public abstract int getNumColors();

  /**
   * Red component of a pixel.
   */
  public abstract int getRed(int pixel);

  /**
   * Green component of a pixel.
   */
  public abstract int getGreen(int pixel);

  /**
   * Blue component of a pixel.
   */
  public abstract int getBlue(int pixel);

  /**
   * Get the greyscale value of pixel brightness.
   */
  public abstract int getPixelBrightness(int x, int y);

}