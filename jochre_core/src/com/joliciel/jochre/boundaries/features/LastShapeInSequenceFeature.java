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

import com.joliciel.talismane.utils.features.BooleanFeature;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.jochre.boundaries.ShapeInSequence;

/**
 * Is this shape the last one in the sequence?
 * @author Assaf Urieli
 *
 */
public final class LastShapeInSequenceFeature extends AbstractShapeInSequenceFeature<Boolean> implements BooleanFeature<ShapeInSequence> {
	@Override
	public FeatureResult<Boolean> checkInternal(ShapeInSequence shapeInSequence) {
		boolean result = (shapeInSequence.getShapeSequence().size() == (shapeInSequence.getIndex()+1));
		FeatureResult<Boolean> outcome = this.generateResult(result);

		return outcome;
	}
}
