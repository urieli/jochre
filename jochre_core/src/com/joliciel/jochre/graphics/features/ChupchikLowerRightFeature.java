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
import com.joliciel.talismane.utils.features.BooleanFeature;
import com.joliciel.talismane.utils.features.FeatureResult;

/**
 * Is there a noticeable chupchik at the lower right?
 * Useful for distinguishing Gimel from Nun in the Hebrew alphabet.
 * @author Assaf Urieli
 *
 */
public class ChupchikLowerRightFeature extends AbstractShapeFeature<Boolean> implements BooleanFeature<ShapeWrapper> {
	private static final Log LOG = LogFactory.getLog(ChupchikLowerRightFeature.class);

	@Override
	public FeatureResult<Boolean> checkInternal(ShapeWrapper shapeWrapper) {
		Shape shape = shapeWrapper.getShape();
		int xSectors = 11;
		int centreSectors = 13;
		int marginSectors = 1;
		double[][] grid = shape.getBrightnessBySection(xSectors, centreSectors, marginSectors, SectionBrightnessMeasurementMethod.RELATIVE_TO_MAX_SECTION);

		double minChupchikStart = 8;
		
		boolean foundChupchik = false;
		for (int j = grid[0].length - 1; j>=grid[0].length-5; j--) {
			boolean foundBlack = false;
			int chupchikSize = 0;
			for (int i = grid.length-1; i>0;i--) {
				if (!foundBlack && i<minChupchikStart)
					break;
				if (grid[i][j]>=0.5) {
					foundBlack = true;
					chupchikSize++;
				}
				else if (grid[i][j]<0.5 && foundBlack) {
					break;
				}
			}
			if (LOG.isTraceEnabled())
				LOG.trace("Row " + j + ", chupchickSize: " + chupchikSize);
			if (foundBlack&&chupchikSize<=3) {
				foundChupchik = true;
				break;
			} else if (foundBlack) {
				break;
			}
		}
		
		FeatureResult<Boolean> outcome = this.generateResult(foundChupchik);
		return outcome;
	}
}
