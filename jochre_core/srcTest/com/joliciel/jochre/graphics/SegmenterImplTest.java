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

import java.util.BitSet;
import java.util.List;
import java.util.ArrayList;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.jochre.letterGuesser.LetterGuesserService;

import static org.junit.Assert.*;

public class SegmenterImplTest {
	private static final Log LOG = LogFactory.getLog(SegmenterImplTest.class);
 
	@Test
	public void testGetShape(@NonStrict final LetterGuesserService letterGuesserService,
			@NonStrict final SourceImage sourceImage) throws Exception {
		GraphicsServiceImpl graphicsService = new GraphicsServiceImpl();
		graphicsService.setLetterGuesserService(letterGuesserService);
		
		final int threshold = 100;
		final int startX = 3;
		final int startY = 2;

		new NonStrictExpectations() {
			{
        	sourceImage.getHeight(); returns(8);
        	sourceImage.getWidth(); returns(8);
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
        	
        	for (int x = -1; x <= 8; x++)
        		for (int y = -1; y <= 8; y++) {
        			sourceImage.isPixelBlack(x, y, threshold); 
        			if (x >= 0 && x < 8 && y >= 0 && y < 8)
        				returns(pixels[y*8 + x]==1);
        			else
        				returns(false);
        		}
        	
        }};
        
		SegmenterImpl segmenter = new SegmenterImpl(sourceImage);
		segmenter.setGraphicsService(graphicsService);

		final WritableImageGrid imageMirror = graphicsService.getEmptyMirror(sourceImage);

		Shape shape = segmenter.getShape(sourceImage, imageMirror, startX, startY);
		
		assertEquals(2, shape.getTop());
		assertEquals(2, shape.getLeft());
		assertEquals(4, shape.getBottom());
		assertEquals(7, shape.getRight());
	}

	public void testSplitShape(@NonStrict final LetterGuesserService letterGuesserService,
			@NonStrict final SourceImage sourceImage,
			@NonStrict final Shape shape) throws Exception {
		GraphicsServiceImpl graphicsService = new GraphicsServiceImpl();
		graphicsService.setLetterGuesserService(letterGuesserService);

		final int threshold = 100;
		final int width = 12;
		final int height = 9;
		final int maxBridgeWidth = 2;
		final int minLetterWeight = 12;
		final int maxOverlap = 2;
		final BitSet bitset = new BitSet(width*height);
		final int left = 10;
		final int top = 10;

		new NonStrictExpectations() {
			{
        	shape.getHeight(); returns(height);
        	shape.getWidth(); returns(width);
        	shape.getLeft(); returns(left);
        	shape.getTop(); returns(top);
        	shape.getRight(); returns(left + width-1);
        	shape.getBottom(); returns(top + height-1);
        	
        	sourceImage.getSeparationThreshold(); returns(threshold);
        	int[] pixels =
        		{ 1, 1, 0, 0, 0, 1, 0, 1, 0, 1, 1, 1, // row 0
        		  0, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, // row 1
        		  0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 1, 1, // row 2
        		  0, 1, 1, 0, 1, 1, 0, 0, 1, 0, 1, 1, // row 3
        		  0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, // row 4
        		  0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, // row 5
        		  0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, // row 6
        		  1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, // row 7
        		  0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, // row 8
        		};
        	
        	for (int x = -1; x <= width; x++)
        		for (int y = -1; y <= height; y++) {
        			shape.isPixelBlack(x, y, threshold); 
        			if (x >= 0 && x < width && y >= 0 && y < height) {
        				returns(pixels[y*width + x]==1);
        				if (pixels[y*width + x]==1) {
        					bitset.set(y*width + x);
        				}
        			}
        			else
        				returns(false);
        		}
        	
        	shape.getBlackAndWhiteBitSet(threshold, 0); returns(bitset);
        }};
        
		SegmenterImpl segmenter = new SegmenterImpl(sourceImage);
		segmenter.setGraphicsService(graphicsService);

		List<Shape> shapes = segmenter.splitShape(shape, sourceImage, maxBridgeWidth, minLetterWeight, maxOverlap);
		
		for (Shape splitShape : shapes) {
			LOG.debug("Split shape:  " + splitShape);
		}
		assertEquals(2, shapes.size());
		
		Shape leftShape = shapes.get(0);
		assertEquals(left, leftShape.getLeft());
		assertEquals(left + 5, leftShape.getRight());
		assertEquals(top, leftShape.getTop());
		assertEquals(top + 7, leftShape.getBottom());
		Shape rightShape = shapes.get(1);
		assertEquals(left + 6, rightShape.getLeft());
		assertEquals(top + 11, rightShape.getRight());
		assertEquals(top, rightShape.getTop());
		assertEquals(top + 8, rightShape.getBottom());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testSegment(@NonStrict final LetterGuesserService letterGuesserService,
			@NonStrict final SourceImage sourceImage) throws Exception {
		GraphicsServiceImpl graphicsService = new GraphicsServiceImpl();
		graphicsService.setLetterGuesserService(letterGuesserService);

		final int threshold = 100;
		final int width = 12;
		final int height = 10;
		final List<Rectangle> whiteAreas = new ArrayList<Rectangle>();

		new NonStrictExpectations() {
			{
        	sourceImage.getHeight(); returns(height);
        	sourceImage.getWidth(); returns(width);
        	sourceImage.getSeparationThreshold(); returns(threshold);
        	sourceImage.getWhiteGapFillFactor(); returns(0);
        	sourceImage.isLeftToRight(); returns(false);
        	sourceImage.getWhiteAreas((List<Shape>) any); returns(whiteAreas);
        	int[] pixels =
        		//0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11
        		{ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // row 0
        		  0, 1, 1, 0, 0, 0, 1, 1, 0, 1, 1, 1, // row 1
        		  0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, // row 2
        		  0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, // row 3
        		  0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, // row 4
        		  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // row 5
        		  0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, // row 6
        		  0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, // row 7
        		  0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, // row 8
        		  0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, // row 9
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
        
		SegmenterImpl segmenter = new SegmenterImpl(sourceImage);
		segmenter.setGraphicsService(graphicsService);

		LOG.debug("findContiguousShapes");
		List<Shape> shapes = segmenter.findContiguousShapes(sourceImage);
		assertEquals(11, shapes.size());
		int i = 0;
		int j = 0;
		for (Shape shape : shapes) {
			if (i==0) {
				assertEquals(0, shape.getTop());
				assertEquals(1, shape.getLeft());
				assertEquals(4, shape.getBottom());
				assertEquals(2, shape.getRight());
			}
			i++;
		}
		
		i = 0;
		for (Shape shape : shapes) {
			LOG.debug("============= Shape (" + i + ") ================");
			LOG.debug("Left = " + shape.getLeft() + ". Top = " + shape.getTop() + ". Right = " + shape.getRight() + ". Bottom = " + shape.getBottom());
			for (int y = 0; y < shape.getHeight(); y++) {
				String line = "";
				for (int x = 0; x < shape.getWidth(); x++) {
					if (shape.isPixelBlack(x, y, sourceImage.getSeparationThreshold()))
						line += "x";
					else
						line += "o";
				}
				LOG.debug(line);
			}
			i++;
		}
		
        LOG.debug("arrangeShapesInRows");
		List<RowOfShapes> rows = segmenter.groupShapesIntoRows(sourceImage, shapes, whiteAreas, false);
		assertEquals(2, rows.size());
		
		i = 0;
		for (RowOfShapes row: rows) {
			j = 0;
			for (Shape shape : row.getShapes()) {
				LOG.debug("============= Shape (" + i + "," + j + ") ================");
				LOG.debug("Left = " + shape.getLeft() + ". Top = " + shape.getTop() + ". Right = " + shape.getRight() + ". Bottom = " + shape.getBottom());
				for (int y = 0; y < shape.getHeight(); y++) {
					String line = "";
					for (int x = 0; x < shape.getWidth(); x++) {
						if (shape.isPixelBlack(x, y, sourceImage.getSeparationThreshold()))
							line += "x";
						else
							line += "o";
					}
					LOG.debug(line);
				}
				j++;
			}
			i++;
		}
	}

}
