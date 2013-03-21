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
package com.joliciel.jochre.boundaries.features;

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.graphics.GroupOfShapes;

/**
 * Is this shape the last one in the row? Especially useful for dashes!
 * @author Assaf Urieli
 *
 */
public final class LastShapeInRowFeature extends AbstractShapeInSequenceFeature<Boolean> implements BooleanFeature<ShapeInSequence> {
	@Override
	public FeatureResult<Boolean> checkInternal(ShapeInSequence shapeInSequence, RuntimeEnvironment env) {
		boolean lastShapeInSequence = false;
		if (shapeInSequence.getShapeSequence().size() == (shapeInSequence.getIndex()+1))
			lastShapeInSequence = true;
		
		boolean lastShapeInRow = false;
		if (lastShapeInSequence) {
			GroupOfShapes group = shapeInSequence.getShape().getGroup();
			if (group.getIndex()==group.getRow().getGroups().size()-1)
				lastShapeInRow = true;
		}
		
		FeatureResult<Boolean> outcome = this.generateResult(lastShapeInRow);

		return outcome;
	}
}
