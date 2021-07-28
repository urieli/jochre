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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class TrainingCorpusShapeSplitterTest {
  private static final Logger LOG = LoggerFactory.getLogger(TrainingCorpusShapeSplitterTest.class);

  @Test
  public void testSplit() throws Exception {

    System.setProperty("config.file", "src/test/resources/testDualCharacters.conf");
    ConfigFactory.invalidateCaches();
    Config config = ConfigFactory.load();
    final JochreSession jochreSession = new JochreSession(config);

    final Shape shape = mock(Shape.class);
    final Shape shape1 = mock(Shape.class);
    final Shape shape2 = mock(Shape.class);
    final Shape shape3 = mock(Shape.class);
    
    final Shape shape4 = mock(Shape.class);
    final GroupOfShapes group = mock(GroupOfShapes.class);
    final RowOfShapes row = mock(RowOfShapes.class);
    final Paragraph paragraph = mock(Paragraph.class);
    
    final JochreImage jochreImage = mock(JochreImage.class);
    final JochrePage jochrePage = mock(JochrePage.class);
    final JochreDocument jochreDocument = mock(JochreDocument.class);
    
    final Iterator<Split> i = (Iterator<Split>) mock(Iterator.class);
    final List<Split> splits = (List<Split>) mock(List.class);
    final Split split1 = mock(Split.class);
    final Split split2 = mock(Split.class);
    
    final Split split3 = mock(Split.class);

    when(shape.getLetter()).thenReturn("אָבּער");

    when(shape.getLeft()).thenReturn(100);
    when(shape.getRight()).thenReturn(200);
    when(shape.getTop()).thenReturn(100);
    when(shape.getBottom()).thenReturn(200);
    
    when(shape.getGroup()).thenReturn(group);
    when(shape.getJochreImage()).thenReturn(jochreImage);
    
    when(group.getRow()).thenReturn(row);
    when(row.getParagraph()).thenReturn(paragraph);
    when(paragraph.getImage()).thenReturn(jochreImage);
    when(jochreImage.getPage()).thenReturn(jochrePage);
    when(jochrePage.getDocument()).thenReturn(jochreDocument);
    when(jochreDocument.getLocale()).thenReturn(jochreSession.getLocale());
    
    when(shape.getSplits()).thenReturn(splits);
    when(splits.iterator()).thenReturn(i);
    
    when(i.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
    when(i.next()).thenReturn(split1).thenReturn(split2).thenReturn(split3);

    when(split1.getPosition()).thenReturn(35);
    when(split2.getPosition()).thenReturn(59);
    when(split3.getPosition()).thenReturn(82);
    
    when(jochreImage.getShape(100, 100, 135,200)).thenReturn(shape1);
    when(jochreImage.getShape(136, 100, 159,200)).thenReturn(shape2);
    when(jochreImage.getShape(160, 100, 182, 200)).thenReturn(shape3);
    when(jochreImage.getShape(183, 100, 200, 200)).thenReturn(shape4);
    
    LOG.debug(shape.toString());
    LOG.debug(shape.getLetter());
    TrainingCorpusShapeSplitter splitter = new TrainingCorpusShapeSplitter(jochreSession);
    List<ShapeSequence> result = splitter.split(shape);
    ShapeSequence shapeSequence = result.get(0);
    assertEquals(4, shapeSequence.size());
    LOG.debug("Split into: " + shapeSequence.toString());

    verify(shape1).setLetter("אָ");
    verify(shape2).setLetter("בּ");
    verify(shape3).setLetter("ע");
    verify(shape4).setLetter("ר");
  }

}
