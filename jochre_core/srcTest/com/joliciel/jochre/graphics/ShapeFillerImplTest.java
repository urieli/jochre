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

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.BitSet;

import javax.imageio.ImageIO;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.*;


public class ShapeFillerImplTest  {
	private static final Log LOG = LogFactory.getLog(ShapeFillerImplTest.class);
	
	@Test
	public void testGetFillFactor(@NonStrict final JochreImage jochreImage) throws Exception {
		
		new NonStrictExpectations() {
			{
        	jochreImage.normalize(255); returns(255);
        	jochreImage.normalize(0); returns(0);
        }};

        for (int i = 0; i<=2; i++) {
			String imageName = "";
			if (i==0) {
				imageName = "AlephWithHoles.png";
			} else if (i==1) {
				imageName = "TesWithHoles.png";
			} else {
				imageName = "AlephNoHoles.png";
			}
			LOG.debug(imageName);
	        InputStream imageFileStream = getClass().getResourceAsStream("/com/joliciel/jochre/test/resources/" + imageName); 
	        assertNotNull(imageFileStream);
			BufferedImage image = ImageIO.read(imageFileStream);
			
	        
			ShapeInternal shape = new ShapeImpl();
			shape.setImage(image);
			shape.setJochreImage(jochreImage);
			shape.setTop(0);
			shape.setLeft(0);
			shape.setRight(image.getWidth()-1);
			shape.setBottom(image.getHeight()-1);
			
			ShapeFillerImpl shapeFiller = new ShapeFillerImpl();
			shapeFiller.getFillFactor(shape, 100);
		}
	}

	@Test
	public void testFillShape(@NonStrict final JochreImage jochreImage) throws Exception {
		
		new NonStrictExpectations() {
			{
        	jochreImage.normalize(255); returns(255);
        	jochreImage.normalize(0); returns(0);
        }};

        for (int i = 0; i<=1; i++) {
			String imageName = "";
			int fillFactor = 0;
			if (i==0) {
				imageName = "AlephWithHoles.png";
				fillFactor = 5;
			} else if (i==1) {
				imageName = "TesWithHoles.png";
				fillFactor = 5;
			} else {
				imageName = "AlephNoHoles.png";
				fillFactor = 1;
			}
			LOG.debug(imageName);
	        InputStream imageFileStream = getClass().getResourceAsStream("/com/joliciel/jochre/test/resources/" + imageName); 
	        assertNotNull(imageFileStream);
			BufferedImage image = ImageIO.read(imageFileStream);
			ShapeInternal shape = new ShapeImpl();
			shape.setJochreImage(jochreImage);
			shape.setImage(image);
			shape.setTop(0);
			shape.setLeft(0);
			shape.setRight(image.getWidth()-1);
			shape.setBottom(image.getHeight()-1);
			
			ShapeFillerImpl shapeFiller = new ShapeFillerImpl();
			BitSet bitset = shapeFiller.fillShape(shape, 100, fillFactor);
	 		for (int y = 0; y < shape.getHeight(); y++) {
	 			StringBuilder line = new StringBuilder();
	 	      	for (int x = 0; x < shape.getWidth(); x++) {
	 	      		if (bitset.get(y * shape.getWidth() + x))
	 	      			line.append("x");
	 	      		else
	 	      			line.append("o");
	 	      	}
	 	      	LOG.debug(line.toString());
	 		}
 		}
	}

	@Test
	public void testFillBitSet(@NonStrict final Shape shape) {
		final int threshold = 100;
		final int width = 8;
		final int height = 8;

		new NonStrictExpectations() {
			{
        	shape.getHeight(); returns(height);
        	shape.getWidth(); returns(width);
        	
        	int[] pixels =
        		{ 0, 1, 1, 0, 0, 1, 1, 1, // row 0
        		  0, 1, 0, 1, 0, 1, 0, 1, // row 1
        		  0, 0, 1, 1, 0, 0, 1, 1, // row 2
        		  0, 0, 1, 1, 0, 1, 1, 0, // row 3
        		  0, 0, 0, 1, 0, 1, 1, 0, // row 4
        		  0, 0, 0, 1, 1, 1, 0, 0, // row 5
        		  0, 0, 1, 0, 1, 0, 0, 0, // row 6
        		  1, 1, 0, 1, 1, 0, 0, 0, // row 7
        		};
        	
        	BitSet bitset = new BitSet(height * width);
        	
           	for (int x = -1; x <= width; x++)
        		for (int y = -1; y <= height; y++) {
        			if (x >= 0 && x < width && y >= 0 && y < height && pixels[y*width + x]==1)
        				bitset.set(y * width + x);
        			shape.isPixelBlack(x, y, threshold); 
        			if (x >= 0 && x < width && y >= 0 && y < height)
        				returns(pixels[y*width + x]==1);
        			else
        				returns(false);
        			if (x >= 0 && x < width && y >= 0 && y < height) {
	        			shape.getAbsolutePixel(x, y);
	        			if (pixels[y*width + x]==1)
	        				returns(0);
	        			else
	        				returns(255);
        			}
        		}
        	
           	shape.getBlackAndWhiteBitSet(threshold); returns(bitset);
        }};
        
		ShapeFillerImpl shapeFiller = new ShapeFillerImpl();
		BitSet filledBitSet = shapeFiller.fillBitSet(shape, shape.getBlackAndWhiteBitSet(threshold), 5);
 		for (int y = 0; y < height; y++) {
 			StringBuilder line = new StringBuilder();
 	      	for (int x = 0; x < width; x++) {
 	      		if (filledBitSet.get(y * width + x))
 	      			line.append("x");
 	      		else
 	      			line.append("o");
 	      	}
 	      	LOG.debug(line.toString());
 		}
 		
 		
	}
}
