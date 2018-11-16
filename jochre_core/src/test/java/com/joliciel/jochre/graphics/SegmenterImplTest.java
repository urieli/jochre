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
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class SegmenterImplTest {
  private static final Logger LOG = LoggerFactory.getLogger(SegmenterImplTest.class);

  @Test
  public void testGetShape() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    Config config = ConfigFactory.load();
    JochreSession jochreSession = new JochreSession(config);

    final int startX = 3;
    final int startY = 2;
    int[] pixels = { 0, 0, 0, 0, 0, 0, 0, 0, // row 0
        0, 1, 0, 0, 0, 0, 0, 0, // row 1
        0, 0, 0, 1, 0, 0, 1, 1, // row 2
        0, 0, 1, 1, 1, 0, 0, 1, // row 3
        0, 0, 0, 1, 1, 1, 1, 1, // row 4
        0, 1, 0, 0, 0, 0, 0, 0, // row 5
        0, 1, 1, 0, 0, 0, 0, 0, // row 6
        0, 0, 1, 1, 0, 0, 0, 0, // row 7
    };

    SourceImage sourceImage = new SourceImageMock(pixels, 8, 8, jochreSession);

    Segmenter segmenter = new Segmenter(sourceImage, jochreSession);

    final WritableImageGrid imageMirror = new ImageMirror(sourceImage);

    Shape shape = segmenter.getShape(sourceImage, imageMirror, startX, startY);

    assertEquals(2, shape.getTop());
    assertEquals(2, shape.getLeft());
    assertEquals(4, shape.getBottom());
    assertEquals(7, shape.getRight());
  }

  @Test
  public void testSplitShape() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    Config config = ConfigFactory.load();
    JochreSession jochreSession = new JochreSession(config);

    final int threshold = 100;
    final int width = 12;
    final int height = 9;
    final int maxBridgeWidth = 2;
    final int minLetterWeight = 12;
    final int maxOverlap = 2;
    final int left = 10;
    final int top = 10;

    int[] pixels = { 1, 1, 0, 0, 0, 1, 0, 1, 0, 1, 1, 1, // row 0
        0, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, // row 1
        0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 1, 1, // row 2
        0, 1, 1, 0, 1, 1, 0, 0, 1, 0, 1, 1, // row 3
        0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, // row 4
        0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, // row 5
        0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, // row 6
        1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, // row 7
        0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, // row 8
    };

    SourceImage sourceImage = new SourceImageMock(pixels, 9, 12, jochreSession);
    sourceImage.setSeparationThreshold(threshold);

    Shape shape = new ShapeMock(pixels, left, top, width, height, jochreSession);
    shape.setJochreImage(sourceImage);

    Segmenter segmenter = new Segmenter(sourceImage, jochreSession);

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
  public void testSegment() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    Config config = ConfigFactory.load();
    JochreSession jochreSession = new JochreSession(config);

    final int width = 12;
    final int height = 10;
    final List<Rectangle> whiteAreas = new ArrayList<Rectangle>();

    int[] pixels =
        // 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11
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
    SourceImage sourceImage = new SourceImageMock(pixels, height, width, jochreSession);

    Segmenter segmenter = new Segmenter(sourceImage, jochreSession);

    LOG.debug("findContiguousShapes");
    Set<Shape> shapes = segmenter.findContiguousShapes(sourceImage);
    assertEquals(11, shapes.size());
    int i = 0;
    int j = 0;
    for (Shape shape : shapes) {
      if (i == 0) {
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
    for (RowOfShapes row : rows) {
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
