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

import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;
import com.joliciel.jochre.graphics.Shape.SectionBrightnessMeasurementMethod;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;

/**
 * Is the lower-right corner full?
 * Useful for distinguishing Yiddish Samekh from Shlos Mem.
 * @author Assaf Urieli
 *
 */
public class LowerRightCornerFullFeature extends AbstractShapeFeature<Boolean> implements BooleanFeature<ShapeWrapper>  {
	@Override
	public FeatureResult<Boolean> checkInternal(ShapeWrapper shapeWrapper) {
		Shape shape = shapeWrapper.getShape();
		int xSectors = 5;
		int centreSectors = 7;
		int marginSectors = 1;
		double[][] grid = shape.getBrightnessBySection(xSectors, centreSectors, marginSectors, SectionBrightnessMeasurementMethod.RELATIVE_TO_MAX_SECTION);

		boolean cornerFull = false;
		int cornerY = grid[0].length-(2*marginSectors);
		
		cornerFull = (grid[grid.length-1][cornerY] >= 0.5);

		FeatureResult<Boolean> outcome = this.generateResult(cornerFull);
		return outcome;
	}
}
