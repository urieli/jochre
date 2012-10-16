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

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;

/**
 * Is this row much thinner than the average for this image.
 * @author Assaf Urieli
 *
 */
public class ThinRowFeature extends AbstractShapeFeature<Boolean> implements BooleanFeature<ShapeWrapper> {
	private static final Log LOG = LogFactory.getLog(ThinRowFeature.class);

	@Override
	public FeatureResult<Boolean> checkInternal(ShapeWrapper shapeWrapper) {
		Shape shape = shapeWrapper.getShape();
		double threshold = 0.75;
		JochreImage image = shape.getJochreImage();
		double averageRowHeight = image.getAverageRowHeight();
		double shapeHeight = shape.getGroup().getRow().getXHeight();
		
		double ratio = shapeHeight / averageRowHeight;
		
		LOG.trace("averageRowHeight: " + averageRowHeight);
		LOG.trace("shapeHeight: " + shapeHeight);
		LOG.trace("ratio: " + ratio);
		LOG.trace("threshold: " + threshold);

		FeatureResult<Boolean> outcome = this.generateResult(ratio < threshold);
		return outcome;
	}

}
