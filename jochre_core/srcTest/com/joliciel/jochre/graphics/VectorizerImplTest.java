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

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.*;

public class VectorizerImplTest {
	private static final Log LOG = LogFactory.getLog(VectorizerImplTest.class);

	@Test
	public void testGetLongestLines() {
		final int threshold = 100;
		final int maxLines = 60;
		final int whiteGapFillFactor = 5;
    	int[] pixels =
		{ 0, 1, 1, 0, 0, 1, 1, 1,
		  0, 1, 1, 1, 0, 1, 1, 1,
		  0, 0, 1, 1, 0, 0, 1, 1,
		  0, 0, 1, 1, 0, 1, 1, 0,
		  0, 0, 0, 1, 1, 1, 1, 0,
		  0, 0, 0, 1, 1, 1, 0, 0,
		  0, 0, 1, 1, 1, 0, 0, 0,
		  1, 1, 1, 1, 1, 0, 0, 0
		};
    	
    	Shape shape = new ShapeMock(pixels, 8, 8);
    	
		BitSet outline = new BitSet(64);

    	int[] outlinePixels =
		{ 0, 1, 1, 0, 0, 1, 1, 1,
		  0, 1, 0, 1, 0, 1, 0, 1,
		  0, 0, 1, 1, 0, 0, 1, 1,
		  0, 0, 1, 1, 0, 1, 1, 0,
		  0, 0, 0, 1, 1, 0, 1, 0,
		  0, 0, 0, 1, 0, 1, 0, 0,
		  0, 0, 1, 0, 1, 0, 0, 0,
		  1, 1, 1, 1, 1, 0, 0, 0
		};
    	for (int x = 0; x < 8; x++)
    		for (int y = 0; y < 8; y++) {
    			outline.set(y*8 + x, outlinePixels[y*8+x]==1);
    		}
    	
		VectorizerImpl vectorizer = new VectorizerImpl();
		GraphicsServiceInternal graphicsService = new GraphicsServiceImpl();
		vectorizer.setGraphicsService(graphicsService);
		vectorizer.setWhiteGapFillFactor(whiteGapFillFactor);
		List<LineSegment> lines = vectorizer.getLongestLines(shape, outline, maxLines, threshold);
		assertEquals(maxLines, lines.size());
	}

	@Test
	public void testGetLinesToEdge() {
		final int threshold = 100;
		final int whiteGapFillFactor = 5;
    	int[] pixels =
		{ 0, 1, 1, 0, 0, 1, 1, 1,
		  0, 1, 1, 1, 0, 1, 1, 1,
		  0, 0, 1, 1, 0, 0, 1, 1,
		  0, 0, 1, 1, 0, 1, 1, 0,
		  0, 0, 0, 1, 1, 1, 1, 0,
		  0, 0, 0, 1, 1, 1, 0, 0,
		  0, 0, 1, 1, 1, 0, 0, 0,
		  1, 1, 1, 1, 1, 0, 0, 0
		};

    	Shape shape = new ShapeMock(pixels, 8, 8);
    	
		VectorizerImpl vectorizer = new VectorizerImpl();
		GraphicsServiceInternal graphicsService = new GraphicsServiceImpl();
		vectorizer.setGraphicsService(graphicsService);
		vectorizer.setWhiteGapFillFactor(whiteGapFillFactor);
		List<LineSegment> lines = vectorizer.getLinesToEdge(shape, 2, 2, threshold);

		assertEquals(6, lines.size());
	}

	@Test
	public void testArrayListize(@Mocked final JochreImage image) {
		final int threshold = 100;
		final int whiteGapFillFactor = 5;
		
    	int[] pixels =
		{ 0, 1, 1, 0, 0, 1, 1, 1,
		  0, 1, 1, 1, 0, 1, 1, 1,
		  0, 0, 1, 1, 0, 0, 1, 1,
		  0, 0, 1, 1, 0, 1, 1, 0,
		  0, 0, 0, 1, 1, 1, 1, 0,
		  0, 0, 0, 1, 1, 1, 0, 0,
		  0, 0, 1, 1, 1, 0, 0, 0,
		  1, 1, 1, 1, 1, 0, 0, 0
		};

    	ShapeMock shape = new ShapeMock(pixels, 8, 8);
    	
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
    	
    	BitSet outline = new BitSet();
    	for (int i = 0; i < 8 * 8; i++)
    		outline.set(i,outlinePixels[i]==1);
    	
    	shape.setOutline(outline);
    	shape.setJochreImage(image);
    	
		new NonStrictExpectations() {
		{
        	image.getBlackThreshold();returns(threshold);
        	

        }};
	 	
		VectorizerImpl vectorizer = new VectorizerImpl();
		GraphicsServiceInternal graphicsService = new GraphicsServiceImpl();
		vectorizer.setGraphicsService(graphicsService);
		vectorizer.setWhiteGapFillFactor(whiteGapFillFactor);
		List<LineSegment> lines = vectorizer.vectorize(shape);
		int i = 0;
		for (LineSegment lineSegment : lines) {
			double slope = (double)(lineSegment.getEndY() - lineSegment.getStartY()) / (double) (lineSegment.getEndX() - lineSegment.getStartX());
			LOG.debug("Line " + i++ + "(" + lineSegment.getStartX() + "," + lineSegment.getStartY() + ") "
					+ "(" + lineSegment.getEndX() + "," + lineSegment.getEndY() + "). Length = " + lineSegment.getLength()
					+ ", Slope = " + slope);
		}

	}
}
