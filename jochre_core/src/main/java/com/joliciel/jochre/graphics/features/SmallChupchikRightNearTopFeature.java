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
package com.joliciel.jochre.graphics.features;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;
import com.joliciel.jochre.graphics.Shape.SectionBrightnessMeasurementMethod;

/**
 * Is there a small chupchik on the right near the top?
 * Useful for distinguishing Daled from Reysh in the Hebrew alphabet.
 * @author Assaf Urieli
 *
 */
public class SmallChupchikRightNearTopFeature extends AbstractShapeFeature<Boolean> implements BooleanFeature<ShapeWrapper> {
	private static final Logger LOG = LoggerFactory.getLogger(SmallChupchikRightNearTopFeature.class);

	@Override
	public FeatureResult<Boolean> checkInternal(ShapeWrapper shapeWrapper, RuntimeEnvironment env) {
		Shape shape = shapeWrapper.getShape();
		int xSectors = 11;
		int centreSectors = 13;
		int marginSectors = 1;
		double[][] grid = shape.getBrightnessBySection(xSectors, centreSectors, marginSectors, SectionBrightnessMeasurementMethod.RELATIVE_TO_MAX_SECTION);

		boolean foundChupchik = false;
		int ySectors = grid[0].length;
		boolean foundBlack = false;
		boolean foundMoreBlack = false;

		int testColumn = xSectors-1;
		boolean foundStuff = false;
		while (!foundStuff) {
			for (int i = 0; i<ySectors;i++) {
				if (grid[testColumn][i]>=0.5) {
					foundStuff = true;
					break;
				}
			}
			if (!foundStuff) {
				testColumn = testColumn - 1;
			}
		}
		
		int startChupchik = 0;
		int startWhite = 0;
		int numBlackBelowChupchik = 0;
		int chupchikSize = 0;
		int gapSize = 0;
		double maxChupchikStart = ySectors/3;
		
		for (int i = 0; i<ySectors; i++) {
			if (!foundBlack && i<maxChupchikStart) {
				if (grid[testColumn][i]>=0.5) {
					foundBlack = true;
					startChupchik = i;
				}
			}
			else if (!foundChupchik && foundBlack && grid[testColumn][i]<0.5){
				chupchikSize = i-startChupchik;
				if (LOG.isTraceEnabled())	
					LOG.trace("Found chupchick, start=" + startChupchik + ", size=" + chupchikSize);
				foundChupchik = true;
				startWhite = i;
			}
			else if (foundChupchik && grid[testColumn][i]>=0.5) {
				if (!foundMoreBlack) {
					gapSize = i-startWhite;
					foundMoreBlack = true;
				}
				numBlackBelowChupchik++;
			}
		}
		if (foundChupchik&&!foundMoreBlack) {
			gapSize = ySectors - startChupchik;
		}
		
		int maxBlackBelowGap = gapSize+3;
		
		if (chupchikSize>5) {
			if (LOG.isTraceEnabled())
				LOG.trace("Chupchik too big: " + chupchikSize);
			foundChupchik = false;
		} else if (gapSize<3) {
			if (LOG.isTraceEnabled())
				LOG.trace("Gap size too small: "+ gapSize);
			foundChupchik = false;
		} else if (numBlackBelowChupchik>maxBlackBelowGap) {
			if (LOG.isTraceEnabled())
				LOG.trace("Too much black below gap, max: " + maxBlackBelowGap + ", found: " + numBlackBelowChupchik);
			foundChupchik = false;
		}

		FeatureResult<Boolean> outcome = this.generateResult(foundChupchik);
		return outcome;
	}
}
