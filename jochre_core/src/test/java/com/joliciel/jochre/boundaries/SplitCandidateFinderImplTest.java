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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.SourceImage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import mockit.Mocked;

public class SplitCandidateFinderImplTest {
	private static final Logger LOG = LoggerFactory.getLogger(SplitCandidateFinderImplTest.class);

	@Test
	public void testFindSplitCanidates(@Mocked final JochrePage page) throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		Config config = ConfigFactory.load();
		JochreSession jochreSession = new JochreSession(config);
		InputStream imageFileStream = getClass().getResourceAsStream("shape_370454.png");
		assertNotNull(imageFileStream);
		BufferedImage image = ImageIO.read(imageFileStream);

		JochreImage jochreImage = new SourceImage(page, "name", image, jochreSession);
		Shape shape = jochreImage.getShape(0, 0, jochreImage.getWidth() - 1, jochreImage.getHeight() - 1);

		SplitCandidateFinder splitCandidateFinder = new SplitCandidateFinder(jochreSession);
		List<Split> splits = splitCandidateFinder.findSplitCandidates(shape);

		int[] trueSplitPositions = new int[] { 38, 59, 82 };
		boolean[] foundSplit = new boolean[] { false, false, false };
		for (Split splitCandidate : splits) {
			LOG.debug("Split candidate at " + splitCandidate.getPosition());
			for (int i = 0; i < trueSplitPositions.length; i++) {
				int truePos = trueSplitPositions[i];
				int distance = splitCandidate.getPosition() - truePos;
				if (distance < 0)
					distance = 0 - distance;
				if (distance < splitCandidateFinder.getMinDistanceBetweenSplits()) {
					foundSplit[i] = true;
					LOG.debug("Found split: " + truePos + ", distance " + distance);
				}
			}
		}

		for (int i = 0; i < trueSplitPositions.length; i++) {
			assertTrue("didn't find split " + trueSplitPositions[i], foundSplit[i]);
		}
	}

}
