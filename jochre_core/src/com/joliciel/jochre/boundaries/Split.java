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
package com.joliciel.jochre.boundaries;

import com.joliciel.jochre.Entity;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.jochre.boundaries.features.SplitFeature;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;

/**
 * A split is a split-point separating a shape into two sub-shapes.
 * It is simply defined by the parent shape and an x-coordinate indicating where the shape is to be split.
 * @author Assaf Urieli
 *
 */
public interface Split extends Entity, ShapeWrapper {

	public abstract Shape getShape();

	/**
	 * X-coordinate of the split, where zero is the left-most coordinate of the shape.
	 * @return
	 */
	public abstract int getPosition();
	public abstract void setPosition(int position);
	
	public abstract int getShapeId();

	/**
	 * Delete this split from persistent store.
	 */
	public void delete();

	/**
	 * Get a feature result from the cache.
	 * @param feature
	 * @return
	 */
	public <T> FeatureResult<T> getResultFromCache(SplitFeature<T> splitFeature);
	
	/**
	 * Get a feature result from the cache.
	 * @return
	 */
	public <T> void putResultInCache(SplitFeature<T> splitFeature, FeatureResult<T> featureResult);
}
