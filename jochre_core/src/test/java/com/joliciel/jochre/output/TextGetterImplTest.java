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
package com.joliciel.jochre.output;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.output.TextGetter.TextFormat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TextGetterImplTest {
  private static final Logger LOG = LoggerFactory.getLogger(TextGetterImplTest.class);

  @Test
  public void testGetText() {
    final JochreDocument doc = mock(JochreDocument.class);
    final JochrePage page = mock(JochrePage.class);
    final JochreImage jochreImage = mock(JochreImage.class);
    final Paragraph paragraph = mock(Paragraph.class);
    final RowOfShapes row = mock(RowOfShapes.class);
    final GroupOfShapes group = mock(GroupOfShapes.class);
    final Shape shape1 = mock(Shape.class);
    final Shape shape2 = mock(Shape.class);
    final Shape shape3 = mock(Shape.class);
    final Shape shape4 = mock(Shape.class);
    final Shape shape5 = mock(Shape.class);
    final Shape shape6 = mock(Shape.class);
    final Shape shape7 = mock(Shape.class);
    final Shape shape8 = mock(Shape.class);
    final Shape shape9 = mock(Shape.class);
        
    final List<Paragraph> paragraphs = new ArrayList<>();
    paragraphs.add(paragraph);
    final List<RowOfShapes> rows = new ArrayList<>();
    rows.add(row);
    final List<GroupOfShapes> groups = new ArrayList<>();
    groups.add(group);

    when(jochreImage.getPage()).thenReturn(page);
    when(page.getDocument()).thenReturn(doc);
    when(doc.isLeftToRight()).thenReturn(false);
    when(jochreImage.getParagraphs()).thenReturn(paragraphs);
    when(paragraph.getRows()).thenReturn(rows);
    when(row.getGroups()).thenReturn(groups);

    List<Shape> shapes = new ArrayList<>();
    shapes.add(shape1);
    shapes.add(shape2);
    shapes.add(shape3);
    shapes.add(shape4);
    shapes.add(shape5);
    shapes.add(shape6);
    shapes.add(shape7);
    shapes.add(shape8);
    shapes.add(shape9);

    when(group.getShapes()).thenReturn(shapes);
    when(group.getXHeight()).thenReturn(10);
    when(shape1.getLetter()).thenReturn(",");
    when(shape2.getLetter()).thenReturn(",");
    when(shape3.getLetter()).thenReturn("|אַ");
    when(shape4.getLetter()).thenReturn("אַ|");
    when(shape5.getLetter()).thenReturn("|m");
    when(shape6.getLetter()).thenReturn("m|");
    when(shape7.getLetter()).thenReturn("|ש");
    when(shape8.getLetter()).thenReturn("ע|");
    when(shape9.getLetter()).thenReturn(",");

    StringWriter writer = new StringWriter();
    TextGetter textGetter = new TextGetter(writer, TextFormat.PLAIN);
    textGetter.onImageComplete(jochreImage);
    String result = writer.toString();
    LOG.debug(result);
    assertEquals("„אַm|שע|, \n", result);
  }

  @Test
  public void testGetTextFontSizes() {
    final JochreDocument doc = mock(JochreDocument.class);
    final JochrePage page = mock(JochrePage.class);
    final JochreImage jochreImage = mock(JochreImage.class);
    
    final Paragraph paragraph = mock(Paragraph.class);
    final RowOfShapes row = mock(RowOfShapes.class);
    final GroupOfShapes group1 = mock(GroupOfShapes.class);
    final GroupOfShapes group2 = mock(GroupOfShapes.class);
    
    final GroupOfShapes group3 = mock(GroupOfShapes.class);
    final GroupOfShapes group4 = mock(GroupOfShapes.class);
    final Shape shape1 = mock(Shape.class);
    final Shape shape2 = mock(Shape.class);
    final Shape shape3 = mock(Shape.class);
    final Shape shape4 = mock(Shape.class);

        
    final List<Paragraph> paragraphs = new ArrayList<>();
    paragraphs.add(paragraph);
    final List<RowOfShapes> rows = new ArrayList<>();
    rows.add(row);

    when(jochreImage.getPage()).thenReturn(page);
    when(page.getDocument()).thenReturn(doc);
    when(doc.isLeftToRight()).thenReturn(true);
    when(jochreImage.getParagraphs()).thenReturn(paragraphs);
    when(paragraph.getRows()).thenReturn(rows);

    List<GroupOfShapes> groups = new ArrayList<>();
    groups.add(group1);
    groups.add(group2);
    groups.add(group3);
    groups.add(group4);
    
    when(row.getGroups()).thenReturn(groups);

    List<Shape> shapes1 = new ArrayList<>();
    shapes1.add(shape1);
    when(group1.getShapes()).thenReturn(shapes1);
    when(group1.getXHeight()).thenReturn(10);

    List<Shape> shapes2 = new ArrayList<>();
    shapes2.add(shape2);
    when(group2.getShapes()).thenReturn(shapes2);
    when(group2.getXHeight()).thenReturn(20);

    List<Shape> shapes3 = new ArrayList<>();
    shapes3.add(shape3);
    when(group3.getShapes()).thenReturn(shapes3);
    when(group3.getXHeight()).thenReturn(10);

    List<Shape> shapes4 = new ArrayList<>();
    shapes4.add(shape4);
    when(group4.getShapes()).thenReturn(shapes4);
    when(group4.getXHeight()).thenReturn(5);
    
    when(shape1.getLetter()).thenReturn("A");
    when(shape1.getXHeight()).thenReturn(10);
    when(shape2.getLetter()).thenReturn("B");
    when(shape2.getXHeight()).thenReturn(20);
    when(shape3.getLetter()).thenReturn("C");
    when(shape3.getXHeight()).thenReturn(10);
    when(shape4.getLetter()).thenReturn("D");
    when(shape4.getXHeight()).thenReturn(5);

    StringWriter writer = new StringWriter();
    TextGetter textGetter = new TextGetter(writer, TextFormat.XHTML);
    textGetter.onImageComplete(jochreImage);
    String result = writer.toString();
    LOG.debug(result);
    assertEquals("<p dir=\"rtl\">A <big>B </big>C <small>D </small></p>", result);
  }

  public void testAppendBidiText() {
    StringWriter writer = new StringWriter();
    TextGetter textGetter = new TextGetter(writer, TextFormat.XHTML);
    String text = "איך מײן אַז ס'איז 5.01 פּראָצענט, אָדער ebyam אַפֿילו %5.02.";
    textGetter.appendBidiText(text, writer);
    String result = writer.toString();
    LOG.debug(result);
    assertEquals("איך מײן אַז ס'איז 10.5 פּראָצענט, אָדער maybe אַפֿילו 20.5%.", result);
  }

}
