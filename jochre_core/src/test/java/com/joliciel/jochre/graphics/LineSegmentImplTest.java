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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mockit.Expectations;
import mockit.Mocked;

public class LineSegmentImplTest {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(LineSegmentImplTest.class);

  @Test
  public void testGetEnclosingRectangle(@Mocked final Shape shape) {
    LineDefinition lineDef = new LineDefinition(0, 0);
    List<Integer> steps = new ArrayList<>();
    steps.add(2);
    steps.add(3);
    lineDef.setSteps(steps);

    new Expectations() {
      {
        shape.getHeight();
        result = 8;
        minTimes = 0;
        shape.getWidth();
        result = 8;
        minTimes = 0;
      }
    };

    LineSegment lineSegment = new LineSegment(shape, lineDef, 5, 2, 1, 3);
    lineSegment.setLength(4);

    BitSet rectangle = lineSegment.getEnclosingRectangle(1);

    int[] bitsetPixels = { 0, 0, 0, 0, 0, 0, 0, 0, // row
        0, 0, 0, 0, 1, 1, 0, 0, // row
        0, 1, 1, 1, 1, 1, 0, 0, // row
        0, 1, 1, 1, 1, 1, 0, 0, // row
        0, 1, 1, 1, 0, 0, 0, 0, // row
        0, 0, 0, 0, 0, 0, 0, 0, // row
        0, 0, 0, 0, 0, 0, 0, 0, // row
        0, 0, 0, 0, 0, 0, 0, 0 // row
    };

    for (int x = 0; x < 8; x++)
      for (int y = 0; y < 8; y++) {
        assertEquals("x = " + x + ", y = " + y, bitsetPixels[y * 8 + x] == 1, rectangle.get(y * 8 + x));
      }

    assertEquals(3 * (lineSegment.getLength() + 1), rectangle.cardinality());
  }

  @Test
  public void testGetEnclosingRectangleDiagonal(@Mocked final Shape shape) {
    LineDefinition lineDef = new LineDefinition(0, 0);
    List<Integer> steps = new ArrayList<>();
    steps.add(1);
    steps.add(2);
    lineDef.setSteps(steps);

    new Expectations() {
      {
        shape.getHeight();
        result = 8;
        minTimes = 0;
        shape.getWidth();
        result = 8;
        minTimes = 0;
      }
    };

    LineSegment lineSegment = new LineSegment(shape, lineDef, 5, 2, 1, 5);
    lineSegment.setLength(4);

    BitSet rectangle = lineSegment.getEnclosingRectangle(1);

    int[] bitsetPixels = { 0, 0, 0, 0, 0, 0, 0, 0, // row
        0, 0, 0, 0, 0, 1, 0, 0, // row
        0, 0, 0, 1, 1, 1, 0, 0, // row
        0, 0, 1, 1, 1, 1, 0, 0, // row
        0, 1, 1, 1, 1, 0, 0, 0, // row
        0, 1, 1, 0, 0, 0, 0, 0, // row
        0, 1, 0, 0, 0, 0, 0, 0, // row
        0, 0, 0, 0, 0, 0, 0, 0 // row
    };

    for (int y = 0; y < 8; y++) {
      for (int x = 0; x < 8; x++) {
        assertEquals("failure at x=" + x + ",y=" + y, bitsetPixels[y * 8 + x] == 1, rectangle.get(y * 8 + x));
      }
    }
    assertEquals(3 * (lineSegment.getLength() + 1), rectangle.cardinality());
  }

  @Test
  public void testGetEnclosingRectangleDoubleDiagonal(@Mocked final Shape shape) {
    LineDefinition lineDef = new LineDefinition(1, 0);
    List<Integer> steps = new ArrayList<>();
    steps.add(2);
    lineDef.setSteps(steps);

    new Expectations() {
      {
        shape.getHeight();
        result = 8;
        minTimes = 0;
        shape.getWidth();
        result = 8;
        minTimes = 0;
      }
    };

    LineSegment lineSegment = new LineSegment(shape, lineDef, 5, 2, 3, 6);

    lineSegment.setLength(4);

    BitSet rectangle = lineSegment.getEnclosingRectangle(1);

    int[] bitsetPixels = { 0, 0, 0, 0, 0, 0, 0, 0, // row
        0, 0, 0, 0, 0, 0, 0, 0, // row
        0, 0, 0, 0, 1, 1, 1, 0, // row
        0, 0, 0, 1, 1, 1, 0, 0, // row
        0, 0, 0, 1, 1, 1, 0, 0, // row
        0, 0, 1, 1, 1, 0, 0, 0, // row
        0, 0, 1, 1, 1, 0, 0, 0, // row
        0, 0, 0, 0, 0, 0, 0, 0 // row
    };

    for (int x = 0; x < 8; x++)
      for (int y = 0; y < 8; y++) {
        assertEquals("failure at x=" + x + ",y=" + y, bitsetPixels[y * 8 + x] == 1, rectangle.get(y * 8 + x));
      }
    assertEquals(3 * (lineSegment.getLength() + 1), rectangle.cardinality());
  }

  @Test
  public void testGetEnclosingRectangleIntersection(@Mocked final Shape shape) {
    LineDefinition lineDef1 = new LineDefinition(0, 0);
    List<Integer> steps1 = new ArrayList<>();
    steps1.add(2);
    steps1.add(3);
    lineDef1.setSteps(steps1);

    new Expectations() {
      {
        shape.getHeight();
        result = 8;
        minTimes = 0;
        shape.getWidth();
        result = 8;
        minTimes = 0;
      }
    };

    LineSegment lineSegement1 = new LineSegment(shape, lineDef1, 5, 2, 1, 3);
    lineSegement1.setLength(4);

    LineDefinition lineDef2 = new LineDefinition(1, 0);
    List<Integer> steps2 = new ArrayList<>();
    steps2.add(2);
    lineDef2.setSteps(steps2);

    LineSegment lineSegment2 = new LineSegment(shape, lineDef2, 5, 2, 3, 6);
    lineSegment2.setLength(4);

    BitSet intersection = lineSegement1.getEnclosingRectangleIntersection(lineSegment2, 1);

    int[] bitsetPixels = { 0, 0, 0, 0, 0, 0, 0, 0, // row
        0, 0, 0, 0, 0, 0, 0, 0, // row
        0, 0, 0, 0, 1, 1, 0, 0, // row
        0, 0, 0, 1, 1, 1, 0, 0, // row
        0, 0, 0, 1, 0, 0, 0, 0, // row
        0, 0, 0, 0, 0, 0, 0, 0, // row
        0, 0, 0, 0, 0, 0, 0, 0, // row
        0, 0, 0, 0, 0, 0, 0, 0 // row
    };

    for (int x = 0; x < 8; x++)
      for (int y = 0; y < 8; y++) {
        assertEquals("failure at x=" + x + ",y=" + y, bitsetPixels[y * 8 + x] == 1, intersection.get(y * 8 + x));
      }
  }

}
