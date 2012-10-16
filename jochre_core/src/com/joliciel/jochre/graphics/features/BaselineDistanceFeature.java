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
import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;

/**
 * Gives the relative bottom of the shape from the baseline, where 0 is baseline
 * and 1 is baseline + (baseline - meanline).
 * @author Assaf Urieli
 *
 */
public final class BaselineDistanceFeature extends AbstractShapeFeature<Double> implements DoubleFeature<ShapeWrapper> {
	
	@Override
	public FeatureResult<Double> checkInternal(ShapeWrapper shapeWrapper) {
		Shape shape = shapeWrapper.getShape();
		int lineHeight = shape.getBaseLine() - shape.getMeanLine();
		int zeroPoint = (shape.getTop() + shape.getBaseLine());
		int onePoint = (shape.getTop() + shape.getBaseLine()) + lineHeight;
		
		double relativeBottom = 0;
		
		if (shape.getBottom() <= zeroPoint)
			relativeBottom = 0;
		else if (shape.getBottom()>= onePoint)
			relativeBottom = 1;
		else {
			relativeBottom = ((double)(shape.getBottom() - zeroPoint) / (onePoint - zeroPoint));
		}
		
		FeatureResult<Double> outcome = this.generateResult(relativeBottom);
		return outcome;
	}
}
