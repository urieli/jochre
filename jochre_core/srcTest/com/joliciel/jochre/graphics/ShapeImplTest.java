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
import java.util.BitSet;
import mockit.Delegate;
import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.jochre.graphics.Shape.SectionBrightnessMeasurementMethod;
import com.joliciel.jochre.graphics.util.ImagePixelGrabber;
import static org.junit.Assert.*;

public class ShapeImplTest  {
    private static final Log LOG = LogFactory.getLog(ShapeImplTest.class);

    @Test
	public void testGetHeight() {
		Shape shape = new ShapeImpl();
		shape.setTop(10);
		shape.setBottom(40);
		assertEquals(31, shape.getHeight());
	}

    @Test
	public void testGetWidth() {
		Shape shape = new ShapeImpl();
		shape.setLeft(10);
		shape.setRight(40);
		assertEquals(31, shape.getWidth());
	}

    @Test
	public void testGetOutline(@NonStrict final SourceImage sourceImage) {
		final int threshold = 100;
		final int width = 8;
		final int height = 8;

		new NonStrictExpectations() {
			{
         	sourceImage.getHeight(); returns(height);
        	sourceImage.getWidth(); returns(width);
        	sourceImage.getSeparationThreshold(); returns(threshold);
        	sourceImage.getWhiteGapFillFactor(); returns(0);
        	
        	int[] pixels =
        		{ 0, 1, 1, 0, 0, 1, 1, 1, // row 0
        		  0, 1, 1, 1, 0, 1, 1, 1, // row 1
        		  0, 0, 1, 1, 0, 0, 1, 1, // row 2
        		  0, 0, 1, 1, 0, 1, 1, 0, // row 3
        		  0, 0, 0, 1, 1, 1, 1, 0, // row 4
        		  0, 0, 0, 1, 1, 1, 0, 0, // row 5
        		  0, 0, 1, 1, 1, 0, 0, 0, // row 6
        		  1, 1, 1, 1, 1, 0, 0, 0, // row 7
        		};
        	
           	for (int x = -1; x <= width; x++)
        		for (int y = -1; y <= height; y++) {
        			sourceImage.isPixelBlack(x, y, threshold); 
        			if (x >= 0 && x < width && y >= 0 && y < height)
        				returns(pixels[y*width + x]==1);
        			else
        				returns(false);
        			if (x >= 0 && x < width && y >= 0 && y < height) {
	        			sourceImage.getAbsolutePixel(x, y);
	        			if (pixels[y*width + x]==1)
	        				returns(0);
	        			else
	        				returns(255);
        			}
        		}
        	
        }};
        Shape shape = new ShapeImpl(sourceImage);
        shape.setTop(0);
        shape.setBottom(7);
        shape.setLeft(0);
        shape.setRight(7);

		BitSet outline = shape.getOutline(threshold);

    	int[] outlinePixels =
		{ 0, 1, 1, 0, 0, 1, 1, 1, // row 0
		  0, 1, 0, 1, 0, 1, 0, 1, // row 1
		  0, 0, 1, 1, 0, 0, 1, 1, // row 2
		  0, 0, 1, 1, 0, 1, 1, 0, // row 3
		  0, 0, 0, 1, 1, 0, 1, 0, // row 4
		  0, 0, 0, 1, 0, 1, 0, 0, // row 5
		  0, 0, 1, 0, 1, 0, 0, 0, // row 6
		  1, 1, 1, 1, 1, 0, 0, 0, // row 7
		};
    	
    	for (int x = 0; x < 8; x++)
    		for (int y = 0; y < 8; y++) {
    			assertEquals("x = " + x + ",y = " + y, outlinePixels[y*8 + x]==1, outline.get(y*8 + x));
    		}
	}
	
    @Test
	public void testIsPixelBlackFromContainer(@NonStrict final SourceImage sourceImage) {
		final int threshold = 100;
		final int width = 8;
		final int height = 8;

		new NonStrictExpectations() {
			{
        	sourceImage.getHeight(); returns(height);
        	sourceImage.getWidth(); returns(width);
        	sourceImage.getSeparationThreshold(); returns(threshold);
        	sourceImage.getWhiteGapFillFactor(); returns(0);
        	int[] pixels =
        		{ 0, 0, 0, 0, 0, 0, 0, 0, // row 0
        		  0, 1, 0, 0, 0, 0, 0, 0, // row 1
        		  0, 0, 0, 1, 0, 0, 1, 1, // row 2
        		  0, 0, 1, 1, 1, 0, 0, 1, // row 3
        		  0, 0, 0, 1, 1, 1, 1, 1, // row 4
        		  0, 1, 0, 0, 0, 0, 0, 0, // row 5
        		  0, 1, 1, 0, 0, 0, 0, 0, // row 6
        		  0, 0, 1, 1, 0, 0, 0, 0, // row 7
        		};
        	
           	for (int x = -1; x <= width; x++)
        		for (int y = -1; y <= height; y++) {
        			sourceImage.isPixelBlack(x, y, threshold); 
        			if (x >= 0 && x < width && y >= 0 && y < height)
        				returns(pixels[y*width + x]==1);
        			else
        				returns(false);
        			if (x >= 0 && x < width && y >= 0 && y < height) {
	        			sourceImage.getAbsolutePixel(x, y);
	        			if (pixels[y*width + x]==1)
	        				returns(0);
	        			else
	        				returns(255);
        			}
        		}
         }};
        Shape shape = new ShapeImpl(sourceImage);
        shape.setTop(2);
        shape.setLeft(2);
        shape.setBottom(4);
        shape.setRight(7);
        
        for (int i=0;i<shape.getHeight();i++) {
			String line = "";
        	for (int j=0;j<shape.getWidth();j++) {
        		boolean expectedBlack = sourceImage.getAbsolutePixel(j+shape.getLeft(), i+shape.getTop())==0;
        		boolean actualBlack = shape.isPixelBlack(j, i, threshold);
        		if (actualBlack)
        			line+="x";
        		else
        			line+="o";
        		assertEquals("Wrong value at ("+j+","+i+")",
        				expectedBlack,
        				actualBlack);
        	}
        	LOG.debug(line);
        }
	}
	
    @Test
	public void getVerticalCounts(@NonStrict final SourceImage sourceImage) {
		final int threshold = 100;
		final int width = 8;
		final int height = 8;

		new NonStrictExpectations() {
			{
        	sourceImage.getHeight(); returns(height);
        	sourceImage.getWidth(); returns(width);
        	sourceImage.getSeparationThreshold(); returns(threshold);
        	int[] pixels =
        		{ 0, 0, 0, 0, 0, 0, 0, 0, // row 0
        		  0, 1, 0, 0, 0, 0, 0, 0, // row 1
        		  0, 0, 0, 1, 0, 0, 1, 1, // row 2
        		  0, 0, 1, 1, 1, 0, 0, 1, // row 3
        		  0, 0, 0, 1, 1, 1, 1, 1, // row 4
        		  0, 1, 0, 0, 0, 0, 0, 0, // row 5
        		  0, 1, 1, 0, 0, 0, 0, 0, // row 6
        		  0, 0, 1, 1, 0, 0, 0, 0, // row 7
        		};
        	
          	for (int x = -1; x <= width; x++)
        		for (int y = -1; y <= height; y++) {
        			sourceImage.isPixelBlack(x, y, threshold); 
        			if (x >= 0 && x < width && y >= 0 && y < height)
        				returns(pixels[y*width + x]==1);
        			else
        				returns(false);
        			if (x >= 0 && x < width && y >= 0 && y < height) {
	        			sourceImage.getAbsolutePixel(x, y);
	        			if (pixels[y*width + x]==1)
	        				returns(0);
	        			else
	        				returns(255);
        			}
        		}
         }};
        
        Shape shape = new ShapeImpl(sourceImage);
        shape.setTop(0);
        shape.setLeft(0);
        shape.setBottom(7);
        shape.setRight(7);
        
        int[] verticalCounts = shape.getVerticalCounts();
        for (int i = 0; i < verticalCounts.length; i++) {
        	switch(i) {
	        	case 0 :
	        		assertEquals(0*255, verticalCounts[i]);
	        		break;
	        	case 1 :
	        		assertEquals(3*255, verticalCounts[i]);
	        		break;
	        	case 2 :
	        		assertEquals(3*255, verticalCounts[i]);
	        		break;
	        	case 3 :
	        		assertEquals(4*255, verticalCounts[i]);
	        		break;
	        	case 4 :
	        		assertEquals(2*255, verticalCounts[i]);
	        		break;
	        	case 5 :
	        		assertEquals(1*255, verticalCounts[i]);
	        		break;
	        	case 6 :
	        		assertEquals(2*255, verticalCounts[i]);
	        		break;
	        	case 7 :
	        		assertEquals(3*255, verticalCounts[i]);
	        		break;
        	}
        	assertEquals(shape.getWidth(), verticalCounts.length);
        }
	}
	
    @Test
	public void testGetBrightnessTotalsBySector(@NonStrict final GroupOfShapes group,
			@NonStrict final RowOfShapes row,
			@NonStrict final Paragraph paragraph,
			@NonStrict final JochreImage image,
			@NonStrict final ImagePixelGrabber pixelGrabber,
			@NonStrict final BufferedImage shapeImage) {
		final ShapeImpl shape = new ShapeImpl();
		
		final int top = 0;
		final int bottom = 6;
		final int left = 0;
		final int right = 4;
		
		new NonStrictExpectations() {
			{
        	group.getId(); returns(1);
        	group.getRow(); returns(row);
        	row.getParagraph(); returns(paragraph);
        	paragraph.getImage(); returns(image);
        	image.normalize(anyInt);
        	result = new Delegate() {
        		@SuppressWarnings("unused")
				int normalize(int i) { return i; }
        	};
        		
    		int[] pixels = new int[]
                 { 		0, 255, 254, 253, 252,		// row
						251, 250, 249, 248, 247,		// row
						246, 245, 244, 243, 242,		// row
						241, 240, 239, 238, 237,		// row
					    236, 235, 234, 233, 232,		// row
						231, 230, 229, 228, 227,		// row
						226, 225, 224, 223, 222};
    		
    		int width = right - left + 1;
    		int height = bottom - top + 1;
           	for (int x = 0; x < width; x++)
        		for (int y = 0; y < height; y++) {
        			pixelGrabber.getPixelBrightness(x,y); 
        			returns(pixels[y*width + x]);
        		}        		
        }};
        
        shape.setPixelGrabber(pixelGrabber);
		shape.setGroup(group);

		shape.setTop(0);
		shape.setBottom(6);
		shape.setLeft(0);
		shape.setRight(4);
		
		shape.setMeanLine(1);
		shape.setBaseLine(5);
		
		shape.setImage(shapeImage);
		
		double[][] totals = shape.getBrightnessBySection(5, 5, 1, SectionBrightnessMeasurementMethod.RAW);
		
		LOG.debug("Pixels:");
		for (int y = 0; y < shape.getHeight(); y++) {
			String pixelsStr = "";

			for (int x = 0; x < shape.getWidth(); x++) {
				pixelsStr += shape.getPixel(x, y) + ",";
			}
			LOG.debug(pixelsStr);
		}
		
		LOG.debug("Brightness totals by sector:");
		for (int y = 0; y < totals[0].length; y++) {
			String brightnessTotalsStr = "";

			for (int x = 0; x < totals.length; x++) {
				brightnessTotalsStr += totals[x][y] + ",";
			}
			LOG.debug(brightnessTotalsStr);
		}
		
		assertEquals(255.0, totals[0][0], 0.0001);
		double testBrightness = 0.0;
		for (int y = 0; y < totals[0].length; y++) {
			for (int x = 0; x < totals.length; x++) {
				if (x!=0 || y!=0) {
					assertEquals("For x=" + x + ",y=" + y + " expected " + testBrightness + " but was " + totals[x][y], testBrightness, totals[x][y], 0.0001);
					testBrightness += 1.0;
				}
			}
		}
	}

    @Test
	public void testGetBrightnessTotalsBySectorMidPixelBreaks(@NonStrict final GroupOfShapes group,
			@NonStrict final RowOfShapes row,
			@NonStrict final Paragraph paragraph,
			@NonStrict final JochreImage image,
			@NonStrict final ImagePixelGrabber pixelGrabber,
			@NonStrict final BufferedImage shapeImage) {
		ShapeImpl shape = new ShapeImpl();
		
		final int top = 0;
		final int bottom = 7;
		final int left = 0;
		final int right = 5;
		
		
		new NonStrictExpectations() {
			{
        	group.getId(); returns(1);
        	group.getRow(); returns(row);
        	row.getParagraph(); returns(paragraph);
        	paragraph.getImage(); returns(image);
        	image.normalize(anyInt);
        	result = new Delegate() {
        		@SuppressWarnings("unused")
				int normalize(int i) { return i; }
        	};
        		
    		int[] pixels = new int[]
                 { 		245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245, };
       		int width = right - left + 1;
    		int height = bottom - top + 1;
           	for (int x = 0; x < width; x++)
        		for (int y = 0; y < height; y++) {
        			pixelGrabber.getPixelBrightness(x,y); 
        			returns(pixels[y*width + x]);
        		}        		
       	}};
        
        shape.setPixelGrabber(pixelGrabber);
        shape.setGroup(group);
        
        shape.setTop(0);
		shape.setBottom(7);
		shape.setLeft(0);
		shape.setRight(5);
		
		shape.setMeanLine(1);
		shape.setBaseLine(6);
		shape.setImage(shapeImage);

		
		double[][] totals = shape.getBrightnessBySection(5, 5, 1, SectionBrightnessMeasurementMethod.RAW);
		
		LOG.debug("Pixels:");
		for (int y = 0; y < shape.getHeight(); y++) {
			String pixelsStr = "";

			for (int x = 0; x < shape.getWidth(); x++) {
				pixelsStr += shape.getPixel(x, y) + ",";
			}
			LOG.debug(pixelsStr);
		}
		
		LOG.debug("Brightness totals by sector:");
		for (int y = 0; y < totals[0].length; y++) {
			String brightnessTotalsStr = "";

			for (int x = 0; x < totals.length; x++) {
				brightnessTotalsStr += totals[x][y] + ",";
			}
			LOG.debug(brightnessTotalsStr);
		}
		
		for (int y = 0; y < totals[0].length; y++) {
			for (int x = 0; x < totals.length; x++) {
				double expected = 360.0/25.0;
				if (y==0||y==totals[0].length-1)
					expected = 60.0/5.0;
				assertEquals("For x=" + x + ",y=" + y + " expected " + expected + " but was " + totals[x][y], expected, totals[x][y], 0.1);
			}
		}
	}
	
    @Test
	public void testGetBrightnessTotalsBySectorTwoSectorMargins(@NonStrict final GroupOfShapes group,
			@NonStrict final RowOfShapes row,
			@NonStrict final Paragraph paragraph,
			@NonStrict final JochreImage image,
			@NonStrict final ImagePixelGrabber pixelGrabber,
			@NonStrict final BufferedImage shapeImage) {
		ShapeImpl shape = new ShapeImpl();
		
		final int top = 0;
		final int bottom = 7;
		final int left = 0;
		final int right = 5;
		
		
		new NonStrictExpectations() {
			{
        	group.getId(); returns(1);
        	group.getRow(); returns(row);
        	row.getParagraph(); returns(paragraph);
        	paragraph.getImage(); returns(image);
        	image.normalize(anyInt);
        	result = new Delegate() {
        		@SuppressWarnings("unused")
				int normalize(int i) { return i; }
        	};
        		
    		int[] pixels = new int[]
                 { 		245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245,		// row
						245, 245, 245, 245, 245, 245, };
       		int width = right - left + 1;
    		int height = bottom - top + 1;
           	for (int x = 0; x < width; x++)
        		for (int y = 0; y < height; y++) {
        			pixelGrabber.getPixelBrightness(x,y); 
        			returns(pixels[y*width + x]);
        		}        		
       	}};
        
        shape.setPixelGrabber(pixelGrabber);
        shape.setGroup(group);
        
        shape.setTop(0);
		shape.setBottom(7);
		shape.setLeft(0);
		shape.setRight(5);
		
		shape.setMeanLine(2);
		shape.setBaseLine(4);
		shape.setImage(shapeImage);

		
		double[][] totals = shape.getBrightnessBySection(4, 4, 2, SectionBrightnessMeasurementMethod.RAW);
		
		LOG.debug("Pixels:");
		for (int y = 0; y < shape.getHeight(); y++) {
			String pixelsStr = "";

			for (int x = 0; x < shape.getWidth(); x++) {
				pixelsStr += shape.getPixel(x, y) + ",";
			}
			LOG.debug(pixelsStr);
		}
		
		LOG.debug("Brightness totals by sector:");
		for (int y = 0; y < totals[0].length; y++) {
			String brightnessTotalsStr = "";

			for (int x = 0; x < totals.length; x++) {
				brightnessTotalsStr += totals[x][y] + ",";
			}
			LOG.debug(brightnessTotalsStr);
		}
		
		for (int y = 0; y < totals[0].length; y++) {
			for (int x = 0; x < totals.length; x++) {
				double expected = 180.0/16.0;
				if (y<2)
					expected = 120.0/8.0;
				else if (y>5)
					expected = 180.0/8.0;
				assertEquals("For x=" + x + ",y=" + y + " expected " + expected + " but was " + totals[x][y], expected, totals[x][y], 0.1);
			}
		}
		
		double[][] ratios = shape.getBrightnessBySection(4, 4, 2, SectionBrightnessMeasurementMethod.SIZE_NORMALISED);
		for (int y = 0; y < ratios[0].length; y++) {
			String brightnessRatioStr = "";

			for (int x = 0; x < ratios.length; x++) {
				assertEquals(10.0, ratios[x][y], 0.01);
				brightnessRatioStr += ratios[x][y] + ",";
			}
			LOG.debug(brightnessRatioStr);
		}
	}
	
    @Test
	public void testGetBrightnessTotalsBySectorWithSquare(@NonStrict final GroupOfShapes group,
			@NonStrict final RowOfShapes row,
			@NonStrict final Paragraph paragraph,
			@NonStrict final JochreImage image,
			@NonStrict final ImagePixelGrabber pixelGrabber,
			@NonStrict final BufferedImage shapeImage) {
		final ShapeImpl shape = new ShapeImpl();
		
		final int top = 0;
		final int bottom = 5;
		final int left = 0;
		final int right = 5;
		
		new NonStrictExpectations() {
			{
         	group.getId(); returns(1);
        	group.getRow(); returns(row);
        	row.getParagraph(); returns(paragraph);
        	paragraph.getImage(); returns(image);
          	image.normalize(anyInt);
        	result = new Delegate() {
        		@SuppressWarnings("unused")
				int normalize(int i) { return i; }
        	};
        		
    		int[] pixels = new int[]
               { 	245, 245, 245, 245, 245, 245,		// row
					245, 245, 245, 245, 245, 245,		// row
					245, 245, 245, 245, 245, 245,		// row
					245, 245, 245, 245, 245, 245,		// row
					245, 245, 245, 245, 245, 245,		// row
					245, 245, 245, 245, 245, 245,		// row
					};
		
    		int width = right - left + 1;
    		int height = bottom - top + 1;
           	for (int x = 0; x < width; x++)
        		for (int y = 0; y < height; y++) {
        			pixelGrabber.getPixelBrightness(x,y); 
        			returns(pixels[y*width + x]);
        		}        		
        }};
        
        shape.setPixelGrabber(pixelGrabber);
		shape.setGroup(group);

		shape.setTop(0);
		shape.setBottom(5);
		shape.setLeft(0);
		shape.setRight(5);
		
		shape.setMeanLine(1);
		shape.setBaseLine(4);
		shape.setImage(shapeImage);
		
		double[][] totals = shape.getBrightnessBySection(6, 8, 0.5, 0.5, SectionBrightnessMeasurementMethod.RAW);
		
		LOG.debug("Pixels:");
		for (int y = 0; y < shape.getHeight(); y++) {
			String pixelsStr = "";

			for (int x = -1; x < shape.getWidth(); x++) {
				pixelsStr += shape.getPixelInShape(x, y) + ",";
			}
			LOG.debug(pixelsStr);
		}
		
		LOG.debug("Brightness totals by sector:");
		for (int y = 0; y < totals[0].length; y++) {
			String brightnessTotalsStr = "";

			for (int x = 0; x < totals.length; x++) {
				brightnessTotalsStr += totals[x][y] + ",";
			}
			LOG.debug(brightnessTotalsStr);
		}
		
		for (int y = 0; y < totals[0].length; y++) {
			for (int x = 0; x < totals.length; x++) {
				double expected = 160.0/16.0;
				if (y<1)
					expected = 0.0;
				else if (y>6)
					expected = 0;
				assertEquals("For x=" + x + ",y=" + y + " expected " + expected + " but was " + totals[x][y], expected, totals[x][y], 0.1);
			}
		}
	}
	
    @Test
	public void testGetBrightnessTotalsBySectorWithSquareBigger(@NonStrict final GroupOfShapes group,
			@NonStrict final RowOfShapes row,
			@NonStrict final Paragraph paragraph,
			@NonStrict final JochreImage image,
			@NonStrict final ImagePixelGrabber pixelGrabber,
			@NonStrict final BufferedImage shapeImage) {
		final ShapeImpl shape = new ShapeImpl();
		
		final int top = 0;
		final int bottom = 5;
		final int left = 0;
		final int right = 4;
		
		new NonStrictExpectations() {
			{
         	group.getId(); returns(1);
        	group.getRow(); returns(row);
        	row.getParagraph(); returns(paragraph);
        	paragraph.getImage(); returns(image);
        	image.normalize(anyInt);
        	result = new Delegate() {
        		@SuppressWarnings("unused")
				int normalize(int i) { return i; }
        	};
        		
    		int[] pixels = new int[]
               { 	245, 245, 245, 245, 245,		// row
					245, 245, 245, 245, 245,		// row
					245, 245, 245, 245, 245,		// row
					245, 245, 245, 245, 245,		// row
					245, 245, 245, 245, 245,		// row
					245, 245, 245, 245, 245,		// row
					};
    		
    		int width = right - left + 1;
    		int height = bottom - top + 1;
           	for (int x = 0; x < width; x++)
        		for (int y = 0; y < height; y++) {
        			pixelGrabber.getPixelBrightness(x,y); 
        			returns(pixels[y*width + x]);
        		}        		
        }};
        
        shape.setPixelGrabber(pixelGrabber);
		shape.setGroup(group);

		shape.setTop(0);
		shape.setBottom(bottom);
		shape.setLeft(0);
		shape.setRight(right);
		
		shape.setMeanLine(1);
		shape.setBaseLine(4);
		shape.setImage(shapeImage);
		
		double[][] totals = shape.getBrightnessBySection(6, 8, 0.5, 0.5, SectionBrightnessMeasurementMethod.RAW);
		
		LOG.debug("Pixels:");
		for (int y = 0; y < shape.getHeight(); y++) {
			String pixelsStr = "";

			for (int x = -1; x < shape.getWidth(); x++) {
				pixelsStr += shape.getPixelInShape(x, y) + ",";
			}
			LOG.debug(pixelsStr);
		}
		
		LOG.debug("Brightness totals by sector:");
		for (int y = 0; y < totals[0].length; y++) {
			String brightnessTotalsStr = "";

			for (int x = 0; x < totals.length; x++) {
				brightnessTotalsStr += totals[x][y] + ",";
			}
			LOG.debug(brightnessTotalsStr);
		}
		
		for (int y = 0; y < totals[0].length; y++) {
			for (int x = 0; x < totals.length; x++) {
				double expected = 160.0/16.0;
				if (x<1)
					expected = 0.0;
				else if (y<1)
					expected = 0.0;
				else if (y>6)
					expected = 0.0;
				assertEquals("For x=" + x + ",y=" + y + " expected " + expected + " but was " + totals[x][y], expected, totals[x][y], 0.1);
			}
		}
	}
	
    @Test
	public void testGetBrightnessTotalsBySectorWithSquareSmaller(@NonStrict final GroupOfShapes group,
			@NonStrict final RowOfShapes row,
			@NonStrict final Paragraph paragraph,
			@NonStrict final JochreImage image,
			@NonStrict final ImagePixelGrabber pixelGrabber,
			@NonStrict final BufferedImage shapeImage) {
		final ShapeImpl shape = new ShapeImpl();
		
		final int top = 0;
		final int bottom = 5;
		final int left = 0;
		final int right = 2;
		
		new NonStrictExpectations() {
			{
        	group.getId(); returns(1);
        	group.getRow(); returns(row);
        	row.getParagraph(); returns(paragraph);
        	paragraph.getImage(); returns(image);
           	image.normalize(anyInt);
        	result = new Delegate() {
        		@SuppressWarnings("unused")
				int normalize(int i) { return i; }
        	};

        		
    		int[] pixels = new int[]
               { 	245, 245, 245,		// row
					245, 245, 245,		// row
					245, 245, 245,		// row
					245, 245, 245,		// row
					245, 245, 245,		// row
					245, 245, 245,		// row
					};
    		
    		int width = right - left + 1;
    		int height = bottom - top + 1;
           	for (int x = 0; x < width; x++)
        		for (int y = 0; y < height; y++) {
        			pixelGrabber.getPixelBrightness(x,y); 
        			returns(pixels[y*width + x]);
        		}        		
        }};
        
        shape.setPixelGrabber(pixelGrabber);
		shape.setGroup(group);

		shape.setTop(0);
		shape.setBottom(bottom);
		shape.setLeft(0);
		shape.setRight(right);
		
		shape.setMeanLine(1);
		shape.setBaseLine(4);
		shape.setImage(shapeImage);
		
		double[][] totals = shape.getBrightnessBySection(6, 8, 0.5, 0.5, SectionBrightnessMeasurementMethod.RAW);
		
		LOG.debug("Pixels:");
		for (int y = 0; y < shape.getHeight(); y++) {
			String pixelsStr = "";

			for (int x = -1; x < shape.getWidth(); x++) {
				pixelsStr += shape.getPixelInShape(x, y) + ",";
			}
			LOG.debug(pixelsStr);
		}
		
		LOG.debug("Brightness totals by sector:");
		for (int y = 0; y < totals[0].length; y++) {
			String brightnessTotalsStr = "";

			for (int x = 0; x < totals.length; x++) {
				brightnessTotalsStr += totals[x][y] + ",";
			}
			LOG.debug(brightnessTotalsStr);
		}
		
		for (int y = 0; y < totals[0].length; y++) {
			for (int x = 0; x < totals.length; x++) {
				double expected = 120.0/12.0;
				if (x<3)
					expected = 0.0;
				else if (y<1)
					expected = 0.0;
				else if (y>6)
					expected = 0.0;
				assertEquals("For x=" + x + ",y=" + y + " expected " + expected + " but was " + totals[x][y], expected, totals[x][y], 0.1);
			}
		}
	}
	
    @Test
	public void testGetBrightnessBySectorNoMargins(@NonStrict final GroupOfShapes group,
			@NonStrict final RowOfShapes row,
			@NonStrict final Paragraph paragraph,
			@NonStrict final JochreImage image,
			@NonStrict final ImagePixelGrabber pixelGrabber,
			@NonStrict final BufferedImage shapeImage) {
		final ShapeImpl shape = new ShapeImpl();
		
		final int top = 0;
		final int bottom = 5;
		final int left = 0;
		final int right = 2;
		
		new NonStrictExpectations() {
			{
         	group.getId(); returns(1);
        	group.getRow(); returns(row);
        	row.getParagraph(); returns(paragraph);
        	paragraph.getImage(); returns(image);
          	image.normalize(anyInt);
        	result = new Delegate() {
        		@SuppressWarnings("unused")
				int normalize(int i) { return i; }
        	};
        		
    		int[] pixels = new int[]
               { 	245, 245, 245,		// row
					245, 245, 245,		// row
					245, 245, 245,		// row
					245, 245, 245,		// row
					245, 245, 245,		// row
					245, 245, 245,		// row
					};
    		
    		int width = right - left + 1;
    		int height = bottom - top + 1;
           	for (int x = 0; x < width; x++)
        		for (int y = 0; y < height; y++) {
        			pixelGrabber.getPixelBrightness(x,y); 
        			returns(pixels[y*width + x]);
        		}        		
        }};
        
        shape.setPixelGrabber(pixelGrabber);
		shape.setGroup(group);

		shape.setTop(0);
		shape.setBottom(bottom);
		shape.setLeft(0);
		shape.setRight(right);
		
		shape.setMeanLine(1);
		shape.setBaseLine(4);
		shape.setImage(shapeImage);
		
		double[][] totals = shape.getBrightnessBySection(6, 8, SectionBrightnessMeasurementMethod.RAW);
		
		LOG.debug("Pixels:");
		for (int y = 0; y < shape.getHeight(); y++) {
			String pixelsStr = "";

			for (int x = -1; x < shape.getWidth(); x++) {
				pixelsStr += shape.getPixelInShape(x, y) + ",";
			}
			LOG.debug(pixelsStr);
		}
		
		LOG.debug("Brightness totals by sector:");
		for (int y = 0; y < totals[0].length; y++) {
			String brightnessTotalsStr = "";

			for (int x = 0; x < totals.length; x++) {
				brightnessTotalsStr += totals[x][y] + ",";
			}
			LOG.debug(brightnessTotalsStr);
		}
		
		for (int y = 0; y < totals[0].length; y++) {
			for (int x = 0; x < totals.length; x++) {
				double expected = 180.0/(6*8);
				
				assertEquals("For x=" + x + ",y=" + y + " expected " + expected + " but was " + totals[x][y], expected, totals[x][y], 0.1);
			}
		}
	}
}
