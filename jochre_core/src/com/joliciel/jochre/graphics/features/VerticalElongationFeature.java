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

import com.joliciel.talismane.utils.features.DoubleFeature;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;

/**
 * Returns height / width.
 * @author Assaf Urieli
 *
 */
public final class VerticalElongationFeature extends AbstractShapeFeature<Double> implements DoubleFeature<ShapeWrapper> {

	@Override
	public FeatureResult<Double> checkInternal(ShapeWrapper shapeWrapper) {
		Shape shape = shapeWrapper.getShape();
		double ratio = (double)shape.getHeight() / (double)shape.getWidth();

		FeatureResult<Double> outcome = this.generateResult(ratio);
		return outcome;
	}
}
