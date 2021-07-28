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

import java.util.BitSet;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VectorizerImplTest {
  private static final Logger LOG = LoggerFactory.getLogger(VectorizerImplTest.class);

  @Test
  public void testGetLongestLines() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    Config config = ConfigFactory.load();
    JochreSession jochreSession = new JochreSession(config);
    final int threshold = 100;
    final int maxLines = 60;
    final int whiteGapFillFactor = 5;
    int[] pixels = { 0, 1, 1, 0, 0, 1, 1, 1, // row
        0, 1, 1, 1, 0, 1, 1, 1, // row
        0, 0, 1, 1, 0, 0, 1, 1, // row
        0, 0, 1, 1, 0, 1, 1, 0, // row
        0, 0, 0, 1, 1, 1, 1, 0, // row
        0, 0, 0, 1, 1, 1, 0, 0, // row
        0, 0, 1, 1, 1, 0, 0, 0, // row
        1, 1, 1, 1, 1, 0, 0, 0 // row
    };

    Shape shape = new ShapeMock(pixels, 8, 8, jochreSession);

    BitSet outline = new BitSet(64);

    int[] outlinePixels = { 0, 1, 1, 0, 0, 1, 1, 1, // row
        0, 1, 0, 1, 0, 1, 0, 1, // row
        0, 0, 1, 1, 0, 0, 1, 1, // row
        0, 0, 1, 1, 0, 1, 1, 0, // row
        0, 0, 0, 1, 1, 0, 1, 0, // row
        0, 0, 0, 1, 0, 1, 0, 0, // row
        0, 0, 1, 0, 1, 0, 0, 0, // row
        1, 1, 1, 1, 1, 0, 0, 0 // row
    };
    for (int x = 0; x < 8; x++)
      for (int y = 0; y < 8; y++) {
        outline.set(y * 8 + x, outlinePixels[y * 8 + x] == 1);
      }

    Vectorizer vectorizer = new Vectorizer();

    vectorizer.setWhiteGapFillFactor(whiteGapFillFactor);
    List<LineSegment> lines = vectorizer.getLongestLines(shape, outline, maxLines, threshold);
    assertEquals(maxLines, lines.size());
  }

  @Test
  public void testGetLinesToEdge() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    Config config = ConfigFactory.load();
    JochreSession jochreSession = new JochreSession(config);
    final int threshold = 100;
    final int whiteGapFillFactor = 5;
    int[] pixels = { 0, 1, 1, 0, 0, 1, 1, 1, // row
        0, 1, 1, 1, 0, 1, 1, 1, // row
        0, 0, 1, 1, 0, 0, 1, 1, // row
        0, 0, 1, 1, 0, 1, 1, 0, // row
        0, 0, 0, 1, 1, 1, 1, 0, // row
        0, 0, 0, 1, 1, 1, 0, 0, // row
        0, 0, 1, 1, 1, 0, 0, 0, // row
        1, 1, 1, 1, 1, 0, 0, 0 // row
    };
    Shape shape = new ShapeMock(pixels, 8, 8, jochreSession);

    Vectorizer vectorizer = new Vectorizer();

    vectorizer.setWhiteGapFillFactor(whiteGapFillFactor);
    List<LineSegment> lines = vectorizer.getLinesToEdge(shape, 2, 2, threshold);

    assertEquals(6, lines.size());
  }

  @Test
  public void testArrayListize() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    Config config = ConfigFactory.load();
    JochreSession jochreSession = new JochreSession(config);

    final int threshold = 100;
    final int whiteGapFillFactor = 5;

    int[] pixels = { 0, 1, 1, 0, 0, 1, 1, 1, // row
        0, 1, 1, 1, 0, 1, 1, 1, // row
        0, 0, 1, 1, 0, 0, 1, 1, // row
        0, 0, 1, 1, 0, 1, 1, 0, // row
        0, 0, 0, 1, 1, 1, 1, 0, // row
        0, 0, 0, 1, 1, 1, 0, 0, // row
        0, 0, 1, 1, 1, 0, 0, 0, // row
        1, 1, 1, 1, 1, 0, 0, 0 // row
    };

    ShapeMock shape = new ShapeMock(pixels, 8, 8, jochreSession);

    int[] outlinePixels = { 0, 1, 1, 0, 0, 1, 1, 1, // row 0
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
      outline.set(i, outlinePixels[i] == 1);

    final JochreImage image = mock(JochreImage.class);
    when(image.getBlackThreshold()).thenReturn(threshold);
    shape.setOutline(outline);
    shape.setJochreImage(image);

    Vectorizer vectorizer = new Vectorizer();

    vectorizer.setWhiteGapFillFactor(whiteGapFillFactor);
    List<LineSegment> lines = vectorizer.vectorize(shape);
    int i = 0;
    for (LineSegment lineSegment : lines) {
      double slope = (double) (lineSegment.getEndY() - lineSegment.getStartY()) / (double) (lineSegment.getEndX() - lineSegment.getStartX());
      LOG.debug("Line " + i++ + "(" + lineSegment.getStartX() + "," + lineSegment.getStartY() + ") " + "(" + lineSegment.getEndX() + ","
          + lineSegment.getEndY() + "). Length = " + lineSegment.getLength() + ", Slope = " + slope);
    }

  }
}
