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

import mockit.Mocked;
import mockit.NonStrictExpectations;
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

		new NonStrictExpectations() {
			{
				shape.getLetter();
				returns("אָבּער");
				shape.getLeft();
				returns(100);
				shape.getRight();
				returns(200);
				shape.getTop();
				returns(100);
				shape.getBottom();
				returns(200);

				shape.getGroup();
				returns(group);
				shape.getJochreImage();
				returns(jochreImage);

				group.getRow();
				returns(row);
				row.getParagraph();
				returns(paragraph);
				paragraph.getImage();
				returns(jochreImage);
				jochreImage.getPage();
				returns(jochrePage);
				jochrePage.getDocument();
				returns(jochreDocument);
				jochreDocument.getLocale();
				returns(jochreSession.getLocale());

				shape.getSplits();
				returns(splits);
				splits.iterator();
				returns(i);

				i.hasNext();
				returns(true, true, true, false);
				i.next();
				returns(split1, split2, split3);

				split1.getPosition();
				returns(35);
				split2.getPosition();
				returns(59);
				split3.getPosition();
				returns(82);

				jochreImage.getShape(100, 100, 135, 200);
				returns(shape1);
				jochreImage.getShape(136, 100, 159, 200);
				returns(shape2);
				jochreImage.getShape(160, 100, 182, 200);
				returns(shape3);
				jochreImage.getShape(183, 100, 200, 200);
				returns(shape4);
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
