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
package com.joliciel.jochre.graphics;

/**
 * A greyscale grid representation of an image, enabling the client to read the brightness
 * of individual pixels.
 * @author assaf
 *
 */
public interface ImageGrid {
  /**
   * The image width.
   */
  int getWidth();
  
  /**
   * The image height.
   */
  int getHeight();
  
  /**
   * Get a normalised brightness value from 0 to 255. Top-left corner is 0,0.
   */
  int getPixel(int x, int y);
  
  /**
   * Pixel based on absolute pixels, in this image grid's container
   * (if the image grid is inside a container).
   */
  public int getAbsolutePixel(int x, int y);
  
  /**
   * Returns the raw (un-normalised) value of the pixel.
   */
  int getRawPixel(int x, int y);
  
  /**
   * Returns the raw (un-normalised) value of the pixel,
   * based on absolute coordinates, in this image grid's container
   * (if the image grid is inside a container).
   */
  int getRawAbsolutePixel(int x, int y);
  
  /**
   * Returns a certain pixel as a black-or-white value,
   * where true = black pixel (brightness &lt;= threshold)
   * and false = white pixel (brightness &gt; threshold).
   * If the pixel falls outside the image borders, will always return 
   * white (false).
   * @param threshold a value from 0 (black) to 255 (white)
   */
  public boolean isPixelBlack(int x, int y, int threshold);
}
