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

import mockit.Expectations;
import mockit.Mocked;

public class TextGetterImplTest {
	private static final Logger LOG = LoggerFactory.getLogger(TextGetterImplTest.class);

	@Test
	public void testGetText(@Mocked final JochreDocument doc, @Mocked final JochrePage page, @Mocked final JochreImage jochreImage,
			@Mocked final Paragraph paragraph, @Mocked final RowOfShapes row, @Mocked final GroupOfShapes group, @Mocked final Shape shape1,
			@Mocked final Shape shape2, @Mocked final Shape shape3, @Mocked final Shape shape4, @Mocked final Shape shape5, @Mocked final Shape shape6,
			@Mocked final Shape shape7, @Mocked final Shape shape8, @Mocked final Shape shape9) {
		final List<Paragraph> paragraphs = new ArrayList<>();
		paragraphs.add(paragraph);
		final List<RowOfShapes> rows = new ArrayList<>();
		rows.add(row);
		final List<GroupOfShapes> groups = new ArrayList<>();
		groups.add(group);

		new Expectations() {
			{
				jochreImage.getPage();
				result = page;
				minTimes = 0;
				page.getDocument();
				result = doc;
				minTimes = 0;
				doc.isLeftToRight();
				result = false;
				minTimes = 0;
				jochreImage.getParagraphs();
				result = paragraphs;
				minTimes = 0;
				paragraph.getRows();
				result = rows;
				minTimes = 0;
				row.getGroups();
				result = groups;
				minTimes = 0;
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

				group.getShapes();
				result = shapes;
				minTimes = 0;
				group.getXHeight();
				result = 10;
				minTimes = 0;
				shape1.getLetter();
				result = ",";
				minTimes = 0;
				shape2.getLetter();
				result = ",";
				minTimes = 0;
				shape3.getLetter();
				result = "|אַ";
				minTimes = 0;
				shape4.getLetter();
				result = "אַ|";
				minTimes = 0;
				shape5.getLetter();
				result = "|m";
				minTimes = 0;
				shape6.getLetter();
				result = "m|";
				minTimes = 0;
				shape7.getLetter();
				result = "|ש";
				minTimes = 0;
				shape8.getLetter();
				result = "ע|";
				minTimes = 0;
				shape9.getLetter();
				result = ",";
				minTimes = 0;

			}
		};

		StringWriter writer = new StringWriter();
		TextGetter textGetter = new TextGetter(writer, TextFormat.PLAIN);
		textGetter.onImageComplete(jochreImage);
		String result = writer.toString();
		LOG.debug(result);
		assertEquals("„אַm|שע|, \n", result);
	}

	@Test
	public void testGetTextFontSizes(@Mocked final JochreDocument doc, @Mocked final JochrePage page, @Mocked final JochreImage jochreImage,
			@Mocked final Paragraph paragraph, @Mocked final RowOfShapes row, @Mocked final GroupOfShapes group1, @Mocked final GroupOfShapes group2,
			@Mocked final GroupOfShapes group3, @Mocked final GroupOfShapes group4, @Mocked final Shape shape1, @Mocked final Shape shape2,
			@Mocked final Shape shape3, @Mocked final Shape shape4) {
		final List<Paragraph> paragraphs = new ArrayList<>();
		paragraphs.add(paragraph);
		final List<RowOfShapes> rows = new ArrayList<>();
		rows.add(row);

		new Expectations() {
			{
				jochreImage.getPage();
				result = page;
				minTimes = 0;
				page.getDocument();
				result = doc;
				minTimes = 0;
				doc.isLeftToRight();
				result = true;
				minTimes = 0;
				jochreImage.getParagraphs();
				result = paragraphs;
				minTimes = 0;
				paragraph.getRows();
				result = rows;
				minTimes = 0;
				List<GroupOfShapes> groups = new ArrayList<>();
				groups.add(group1);
				groups.add(group2);
				groups.add(group3);
				groups.add(group4);

				row.getGroups();
				result = groups;
				minTimes = 0;

				List<Shape> shapes1 = new ArrayList<>();
				shapes1.add(shape1);
				group1.getShapes();
				result = shapes1;
				minTimes = 0;

				List<Shape> shapes2 = new ArrayList<>();
				shapes2.add(shape2);
				group2.getShapes();
				result = shapes2;
				minTimes = 0;

				List<Shape> shapes3 = new ArrayList<>();
				shapes3.add(shape3);
				group3.getShapes();
				result = shapes3;
				minTimes = 0;

				List<Shape> shapes4 = new ArrayList<>();
				shapes4.add(shape4);
				group4.getShapes();
				result = shapes4;
				minTimes = 0;
				group1.getXHeight();
				result = 10;
				minTimes = 0;
				group2.getXHeight();
				result = 20;
				minTimes = 0;
				group3.getXHeight();
				result = 10;
				minTimes = 0;
				group4.getXHeight();
				result = 5;
				minTimes = 0;

				shape1.getLetter();
				result = "A";
				minTimes = 0;
				shape2.getLetter();
				result = "B";
				minTimes = 0;
				shape3.getLetter();
				result = "C";
				minTimes = 0;
				shape4.getLetter();
				result = "D";
				minTimes = 0;
				shape1.getXHeight();
				result = 10;
				minTimes = 0;
				shape2.getXHeight();
				result = 20;
				minTimes = 0;
				shape3.getXHeight();
				result = 10;
				minTimes = 0;
				shape4.getXHeight();
				result = 5;
				minTimes = 0;
			}
		};

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
