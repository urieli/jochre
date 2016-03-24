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

import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;

/**
 * Gives the ratio of shape width to shape x-height,
 * as a value going from 0 to 1, where 1 = ratios&gt;=2.
 * @author Assaf Urieli
 *
 */
public class WidthToXHeightRatioFeature extends AbstractShapeFeature<Double> implements DoubleFeature<ShapeWrapper> {

	@Override
	public FeatureResult<Double> checkInternal(ShapeWrapper shapeWrapper, RuntimeEnvironment env) {
		Shape shape = shapeWrapper.getShape();
		FeatureResult<Double> result = null;
		double width = shape.getWidth();
		double xHeight = shape.getXHeight();
		if (xHeight==0) xHeight = 1;
		double ratio = width / xHeight;
		ratio = ratio * 0.5;
		if (ratio > 1)
			ratio = 1.0;
		result = this.generateResult(ratio);
		return result;
	}

}
