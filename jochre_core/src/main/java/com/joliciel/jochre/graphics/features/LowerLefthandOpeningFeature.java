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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

/**
 * Is there an opening on the lower-left?
 * Useful for distinguishing Pey from other letters in the Hebrew alphabet.
 * @author Assaf Urieli
 *
 */
public class LowerLefthandOpeningFeature extends AbstractShapeFeature<Boolean> implements BooleanFeature<ShapeWrapper> {
	private static final Log LOG = LogFactory.getLog(LowerLefthandOpeningFeature.class);

	@Override
	public FeatureResult<Boolean> checkInternal(ShapeWrapper shapeWrapper, RuntimeEnvironment env) {
		Shape shape = shapeWrapper.getShape();
		int lowerPoint = (int) ((double) shape.getHeight() * (7.0 / 8.0));
		int upperPoint = shape.getHeight() / 2;
		int openingThreshold = shape.getWidth() / 2;
		int wallThreshold = shape.getWidth() / 4;
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("lowerPoint: " + lowerPoint);
			LOG.trace("upperPoint: " + upperPoint);
			LOG.trace("openingThreshold: " + openingThreshold);
			LOG.trace("wallThreshold: " + wallThreshold);
		}
		boolean foundWall = false;
		boolean foundOpening = false;
		boolean foundAnotherWall = false;
		for (int y = upperPoint; y <= lowerPoint; y++) {
			for (int x = 0; x <= openingThreshold; x++) {
				if (!foundWall && x > wallThreshold) {
					break;
				}
				else if (!foundWall && shape.isPixelBlack(x, y, shape.getJochreImage().getBlackThreshold())) {
					foundWall = true;
					if (LOG.isTraceEnabled())
						LOG.trace("foundWall y=" + y + ", x=" + x);
					break;
				}
				else if (foundWall && !foundOpening && shape.isPixelBlack(x, y, shape.getJochreImage().getBlackThreshold())) {
					break;
				}
				else if (foundWall && !foundOpening && x == openingThreshold) {
					foundOpening = true;
					if (LOG.isTraceEnabled())
						LOG.trace("foundOpening y=" + y + ", x=" + x);
					break;
				}
				else if (foundOpening && !foundAnotherWall && x>= wallThreshold) {
					break;
				}
				else if (foundOpening && !foundAnotherWall && shape.isPixelBlack(x, y, shape.getJochreImage().getBlackThreshold())) {
					foundAnotherWall = true;
					if (LOG.isTraceEnabled())
						LOG.trace("foundAnotherWall y=" + y + ", x=" + x);
					break;
				}
			}
			if (foundAnotherWall)
				break;
		}
		
		FeatureResult<Boolean> outcome = this.generateResult(foundAnotherWall);
		return outcome;
	}
}
