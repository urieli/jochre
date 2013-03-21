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
import com.joliciel.jochre.graphics.Shape.SectionBrightnessMeasurementMethod;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

/**
 * Is there a noticeable gap at the lower right?
 * Useful for distinguishing Gimel from Nun in the Hebrew alphabet.
 * @author Assaf Urieli
 *
 */
public class LowerRighthandGapFeature extends AbstractShapeFeature<Boolean> implements BooleanFeature<ShapeWrapper> {
	private static final Log LOG = LogFactory.getLog(LowerRighthandGapFeature.class);

	@Override
	public FeatureResult<Boolean> checkInternal(ShapeWrapper shapeWrapper, RuntimeEnvironment env) {
		Shape shape = shapeWrapper.getShape();
		int xSectors = 7;
		int centreSectors = 21;
		int marginSectors = 1;
		double[][] initialGrid = shape.getBrightnessBySection(xSectors, centreSectors, marginSectors, SectionBrightnessMeasurementMethod.RELATIVE_TO_MAX_SECTION);
		double[][] grid = initialGrid.clone();
		// get rid of holes
		for (int i = 1; i < grid.length - 1; i++) {
			for (int j = 1; j < grid[0].length -1; j++) {
				if (grid[i][j]==0) {
					if (grid[i-1][j]>=0.5 && grid[i+1][j]>=0.5 && grid[i][j-1]>=0.5 && grid[i][j+1]>=0.5)
						grid[i][j]=1;
				}
			}
		}
		
		boolean foundGap = false;
		boolean goodGap = true;
		int gapLeft = grid[0].length;
		int gapRight = 0;
		for (int j = (centreSectors / 2)+1; j<grid[0].length; j++) {
			if (LOG.isTraceEnabled())
				LOG.trace("Row " + j);
			if (foundGap) {
				// check that there's a bit of white beneath it
				goodGap = false;
				for (int i=gapLeft;i<=gapRight;i++) {
					double brightness = grid[i][j];
					if (brightness < 0.5) {
						goodGap = true;
						gapRight = i;
						gapLeft = i;
						break;
					}
				}
				if (!goodGap) {
					foundGap = false;
					goodGap = true;
				}
				// check the gap's limits
				for (int i=gapRight-1;i>=0;i--) {
					double brightness = grid[i][j];
					if (brightness < 0.5)
						gapLeft = i;
					else
						break;
				}
				for (int i=gapLeft+1;i<grid.length;i++) {
					double brightness = grid[i][j];
					if (brightness < 0.5)
						gapRight = i;
					else
						break;
				}
				if (LOG.isTraceEnabled())
					LOG.trace("foundGap, gapLeft=" + gapLeft + ", gapRight=" + gapRight);
			}
			if (!foundGap) {
				boolean reachedBlack = false;
				boolean reachedWhite = false;
				for (int i = 0; i<grid.length;i++) {
					double brightness = grid[i][j];
					if (foundGap)
						reachedBlack = true;
					if (!reachedBlack && brightness>=0.5) {
						if (LOG.isTraceEnabled())
							LOG.trace("reachedBlack");
						reachedBlack = true;
					} else if (reachedBlack && brightness>=0.5) {
						if (grid[i][j-1]==1.0) {
							if (!reachedWhite) {
								if (LOG.isTraceEnabled())
									LOG.trace("reachedWhite");
								reachedWhite = true;
								gapLeft = i;
							}
						} else {
							reachedBlack = false;
							reachedWhite = false;
						}
					} else if (reachedWhite && brightness>=0.5) {
						foundGap = true;
						gapRight = i-1;
						if (LOG.isTraceEnabled())
							LOG.trace("foundGap, gapLeft=" + gapLeft + ", gapRight=" + gapRight);
						break;
					}
				}
			}
		}
		
		FeatureResult<Boolean> outcome = this.generateResult(foundGap&&goodGap);
		return outcome;
	}
}
