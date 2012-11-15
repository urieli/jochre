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

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

import mockit.NonStrict;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.jochre.JochreServiceLocator;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Shape;

public class SplitCandidateFinderImplTest {
	private static final Log LOG = LogFactory.getLog(SplitCandidateFinderImplTest.class);

	@Test
	public void testFindSplitCanidates(@NonStrict final JochrePage page) throws Exception {
		JochreServiceLocator locator = JochreServiceLocator.getInstance();
		
		GraphicsService graphicsService = locator.getGraphicsServiceLocator().getGraphicsService();
		BoundaryServiceInternal boundaryService = (BoundaryServiceInternal) locator.getBoundaryServiceLocator().getBoundaryService();
        InputStream imageFileStream = getClass().getResourceAsStream("shape_370454.png"); 
        assertNotNull(imageFileStream);
		BufferedImage image = ImageIO.read(imageFileStream);
		
		JochreImage jochreImage = graphicsService.getSourceImage(page, "name", image);
		Shape shape = jochreImage.getShape(0, 0, jochreImage.getWidth()-1, jochreImage.getHeight()-1);
		
		SplitCandidateFinderImpl splitCandidateFinder = new SplitCandidateFinderImpl();
		splitCandidateFinder.setBoundaryServiceInternal(boundaryService);
		List<Split> splits = splitCandidateFinder.findSplitCandidates(shape);
		
		int[] trueSplitPositions = new int[] {38, 59, 82};
		boolean[] foundSplit = new boolean[] {false, false, false};
		for (Split splitCandidate : splits) {
			LOG.debug("Split candidate at " + splitCandidate.getPosition());
			for (int i = 0; i<trueSplitPositions.length; i++) {
				int truePos = trueSplitPositions[i];
				int distance = splitCandidate.getPosition() - truePos;
				if (distance<0) distance = 0-distance;
				if (distance<splitCandidateFinder.getMinDistanceBetweenSplits()) {
					foundSplit[i] = true;
					LOG.debug("Found split: " + truePos + ", distance " + distance);
				}
			}
		}
		
		for (int i = 0; i<trueSplitPositions.length; i++) {
			assertTrue("didn't find split " + trueSplitPositions[i], foundSplit[i]);
		}
	}

}
