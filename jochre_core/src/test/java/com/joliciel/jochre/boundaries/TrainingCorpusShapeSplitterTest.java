///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2011 Assaf Urieli
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
package com.joliciel.jochre.boundaries;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

public class TrainingCorpusShapeSplitterTest {
  private static final Logger LOG = LoggerFactory.getLogger(TrainingCorpusShapeSplitterTest.class);

  @Test
  public void testSplit(@Mocked final Shape shape, @Mocked final Shape shape1, @Mocked final Shape shape2, @Mocked final Shape shape3,
      @Mocked final Shape shape4, @Mocked final GroupOfShapes group, @Mocked final RowOfShapes row, @Mocked final Paragraph paragraph,
      @Mocked final JochreImage jochreImage, @Mocked final JochrePage jochrePage, @Mocked final JochreDocument jochreDocument,
      @Mocked final Iterator<Split> i, @Mocked final List<Split> splits, @Mocked final Split split1, @Mocked final Split split2,
      @Mocked final Split split3) throws Exception {

    System.setProperty("config.file", "src/test/resources/testDualCharacters.conf");
    ConfigFactory.invalidateCaches();
    Config config = ConfigFactory.load();
    final JochreSession jochreSession = new JochreSession(config);

    new Expectations() {
      {
        shape.getLetter();
        result = "אָבּער";
        minTimes = 0;
        shape.getLeft();
        result = 100;
        minTimes = 0;
        shape.getRight();
        result = 200;
        minTimes = 0;
        shape.getTop();
        result = 100;
        minTimes = 0;
        shape.getBottom();
        result = 200;
        minTimes = 0;

        shape.getGroup();
        result = group;
        minTimes = 0;
        shape.getJochreImage();
        result = jochreImage;
        minTimes = 0;

        group.getRow();
        result = row;
        minTimes = 0;
        row.getParagraph();
        result = paragraph;
        minTimes = 0;
        paragraph.getImage();
        result = jochreImage;
        minTimes = 0;
        jochreImage.getPage();
        result = jochrePage;
        minTimes = 0;
        jochrePage.getDocument();
        result = jochreDocument;
        minTimes = 0;
        jochreDocument.getLocale();
        result = jochreSession.getLocale();
        minTimes = 0;

        shape.getSplits();
        result = splits;
        minTimes = 0;
        splits.iterator();
        result = i;
        minTimes = 0;

        i.hasNext();
        returns(true, true, true, false);
        i.next();
        returns(split1, split2, split3);

        split1.getPosition();
        result = 35;
        minTimes = 0;
        split2.getPosition();
        result = 59;
        minTimes = 0;
        split3.getPosition();
        result = 82;
        minTimes = 0;

        jochreImage.getShape(100, 100, 135, 200);
        result = shape1;
        minTimes = 0;
        jochreImage.getShape(136, 100, 159, 200);
        result = shape2;
        minTimes = 0;
        jochreImage.getShape(160, 100, 182, 200);
        result = shape3;
        minTimes = 0;
        jochreImage.getShape(183, 100, 200, 200);
        result = shape4;
        minTimes = 0;
      }
    };

    LOG.debug(shape.toString());
    LOG.debug(shape.getLetter());
    TrainingCorpusShapeSplitter splitter = new TrainingCorpusShapeSplitter(jochreSession);
    List<ShapeSequence> result = splitter.split(shape);
    ShapeSequence shapeSequence = result.get(0);
    assertEquals(4, shapeSequence.size());
    LOG.debug("Split into: " + shapeSequence.toString());

    new Verifications() {
      {
        shape1.setLetter("אָ");
        shape2.setLetter("בּ");
        shape3.setLetter("ע");
        shape4.setLetter("ר");
      }
    };
  }

}
