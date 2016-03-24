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

import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelGrabber;

import com.joliciel.jochre.utils.JochreException;

// Initially from: http://www.permadi.com/tutorial/javaGetImagePixels/index.html
// notice here that I extends an image observer, this is
// not necessary, but I need the ImageObserver to be passed as
// a paramater to get the width and height of the image
public final class ImagePixelGrabberImpl implements ImageObserver, ImagePixelGrabber
{
    private Object pixels=null; // will contains either array of bytes (for gif) or int (for jpeg)
	byte[] bytePixels = null;
	int[] intPixels = null;
	boolean isBytes = true;
	
    private int numColors=0;  
    private int width, height;
    private ColorModel colorModel;

    public ImagePixelGrabberImpl(Image image)
    {
        width=image.getWidth(this);
        height=image.getHeight(this); 
        // The parameter false below tells Java to get an indexed file.
        // When used with a gif/png file, this will grab in palletized mode.
        // When used with a jpg, you'll get DirectColorModel data
        // If set to true, all images will be grabbed in DirectColorModel

        PixelGrabber pixelGrabber=new PixelGrabber(image, 0,0, 
                width, height, false);  

        try {
			pixelGrabber.grabPixels();
		} catch (InterruptedException e) {
			throw new JochreException(e);
		}

        pixels=(Object)pixelGrabber.getPixels();
        
		try {
			bytePixels = (byte[]) pixelGrabber.getPixels();
		} catch (ClassCastException cce) {
			intPixels = (int[]) pixelGrabber.getPixels();
			isBytes = false;
		}
		
        // get the palette of the image, if possible
        colorModel=pixelGrabber.getColorModel();
        
        // IndexColorModel only available for GIF/PNG's.
        if (!(colorModel instanceof IndexColorModel))
        {
            // not an indexed file (ie: not a gif file)
        }
        else
        {
            numColors=((IndexColorModel)colorModel).getMapSize();
        }
    }

    /* (non-Javadoc)
	 * @see com.joliciel.jochre.graphics.util.ImagePixelGrabber#getPixels()
	 */
    @Override
	public Object getPixels()
    {
        return pixels; 
    }

    /* (non-Javadoc)
	 * @see com.joliciel.jochre.graphics.util.ImagePixelGrabber#getWidth()
	 */
    @Override
	public int getWidth()
    {
        return width;
    }

    /* (non-Javadoc)
	 * @see com.joliciel.jochre.graphics.util.ImagePixelGrabber#getHeight()
	 */
    @Override
	public int getHeight()
    {
        return height;
    }

    /* (non-Javadoc)
	 * @see com.joliciel.jochre.graphics.util.ImagePixelGrabber#getNumOfColors()
	 */
    @Override
	public int getNumColors()
    {
        return numColors;
    }

    /* (non-Javadoc)
	 * @see com.joliciel.jochre.graphics.util.ImagePixelGrabber#getRed(int)
	 */
    @Override
	public int getRed(int pixel)
    {
        if ((colorModel instanceof IndexColorModel))    
            return ((IndexColorModel)colorModel).getRed(pixel);
        else
            return ((DirectColorModel)colorModel).getRed(pixel);
    }

    /* (non-Javadoc)
	 * @see com.joliciel.jochre.graphics.util.ImagePixelGrabber#getGreen(int)
	 */
    @Override
	public int getGreen(int pixel)
    {
        if ((colorModel instanceof IndexColorModel))    
            return ((IndexColorModel)colorModel).getGreen(pixel);
        else
            return ((DirectColorModel)colorModel).getGreen(pixel);
    }

    /* (non-Javadoc)
	 * @see com.joliciel.jochre.graphics.util.ImagePixelGrabber#getBlue(int)
	 */
    @Override
	public int getBlue(int pixel)
    {
        if ((colorModel instanceof IndexColorModel))    
            return ((IndexColorModel)colorModel).getBlue(pixel);
        else
            return ((DirectColorModel)colorModel).getBlue(pixel);
    }

	/* (non-Javadoc)
	 * @see com.joliciel.jochre.graphics.util.ImagePixelGrabber#getPixelBrightness(int, int)
	 */
	@Override
	public int getPixelBrightness(int x, int y) {
		int index = y * this.getWidth() + x;
		int pixel;
		if (isBytes)
			pixel = bytePixels[index];
		else
			pixel = intPixels[index];
		int red = this.getRed(pixel);
		int green = this.getGreen(pixel);
		int blue = this.getBlue(pixel);
		int brightness = ( 11 * red + 16 * green + 5 * blue) / 32;
		return brightness;
	}

    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y, 
            int width, int height) 
    {
        return true;      
    }  
}
