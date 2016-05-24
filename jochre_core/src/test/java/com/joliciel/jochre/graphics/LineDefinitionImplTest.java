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

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

import static org.junit.Assert.*;

public class LineDefinitionImplTest {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(LineDefinitionImplTest.class);

	@Test
	public void testTrace(@Mocked final Shape shape) {
		LineDefinitionImpl lineDef = new LineDefinitionImpl(0,0);
		List<Integer> steps = new ArrayList<Integer>();
		steps.add(2);
		steps.add(3);
		lineDef.setSteps(steps);
	
		new NonStrictExpectations() {
			{
        	shape.getHeight(); returns(8);
        	shape.getWidth(); returns(8);
        }};
        BitSet bitset = new BitSet(shape.getHeight() * shape.getWidth());
        lineDef.trace(bitset, shape, 5, 2, 8, 0);
        
    	int[] bitsetPixels =
		{ 0, 0, 0, 0, 0, 0, 0, 0,
		  0, 0, 0, 0, 0, 0, 0, 0,
		  0, 0, 0, 0, 1, 1, 0, 0,
		  0, 1, 1, 1, 0, 0, 0, 0,
		  1, 0, 0, 0, 0, 0, 0, 0,
		  0, 0, 0, 0, 0, 0, 0, 0,
		  0, 0, 0, 0, 0, 0, 0, 0,
		  0, 0, 0, 0, 0, 0, 0, 0
		};
    	
    	for (int x = 0; x < 8; x++)
    		for (int y = 0; y < 8; y++) {
    			assertEquals(bitsetPixels[y*8 + x]==1, bitset.get(y*8 + x));
    		}
    	
    	bitset = new BitSet(shape.getHeight() * shape.getWidth());
        lineDef.trace(bitset, shape, 1, 1, 4, 2);
        
    	int[] bitsetPixels2 =
		{ 0, 0, 0, 0, 0, 0, 0, 0,
		  0, 1, 0, 0, 0, 0, 0, 0,
		  0, 1, 0, 0, 0, 0, 0, 0,
		  0, 0, 1, 0, 0, 0, 0, 0,
		  0, 0, 1, 0, 0, 0, 0, 0,
		  0, 0, 1, 0, 0, 0, 0, 0,
		  0, 0, 0, 0, 0, 0, 0, 0,
		  0, 0, 0, 0, 0, 0, 0, 0
		};
    	
    	for (int x = 0; x < 8; x++)
    		for (int y = 0; y < 8; y++) {
    			assertEquals("failure at x=" + x + ",y=" + y, bitsetPixels2[y*8 + x]==1, bitset.get(y*8 + x));
    		}
	}

	@Test
	public void testFollow(@Mocked final Shape shape) {
		LineDefinitionImpl lineDef = new LineDefinitionImpl(0,0);
		List<Integer> steps = new ArrayList<Integer>();
		steps.add(2);
		steps.add(3);
		lineDef.setSteps(steps);
		
		new NonStrictExpectations() {
		{
        	shape.getHeight(); returns(8);
        	shape.getWidth(); returns(8);
        }};
        
        int[] endPoint = lineDef.follow(shape, 5, 2, 4, 0);
        
        assertEquals(1, endPoint[0]);
        assertEquals(3, endPoint[1]);
  	}

	@Test
	public void testFollowInShape() {
		LineDefinitionImpl lineDef = new LineDefinitionImpl(0,0);
		List<Integer> steps = new ArrayList<Integer>();
		steps.add(2);
		steps.add(3);
		lineDef.setSteps(steps);
		
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

		ShapeInternal shape = new ShapeMock(pixels, 8, 8);
        
        int[] endPoint = lineDef.followInShape(shape, 5, 5, 0, 100, 0);
        
        assertEquals(2, endPoint[0]);
        assertEquals(6, endPoint[1]);
        assertEquals(3, endPoint[2]); //the length
	}
}
