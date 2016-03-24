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
import com.joliciel.talismane.machineLearning.features.AbstractCachableFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

/**
 * An Abstract base class for shape features.
 * @author Assaf Urieli
 *
 */
abstract class AbstractShapeFeature<Y> extends AbstractCachableFeature<ShapeWrapper,Y> implements ShapeFeature<Y> {

	@Override
	protected FeatureResult<Y> checkInCache(ShapeWrapper shapeWrapper, RuntimeEnvironment env) {
		Shape shape = shapeWrapper.getShape();
		return shape.getResultFromCache(this, env);
	}

	@Override
	protected void putInCache(ShapeWrapper shapeWrapper, FeatureResult<Y> featureResult, RuntimeEnvironment env) {
		Shape shape = shapeWrapper.getShape();
		shape.putResultInCache(this, featureResult, env);
	}
	
}
