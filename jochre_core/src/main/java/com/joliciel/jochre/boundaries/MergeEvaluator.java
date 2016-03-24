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

import com.joliciel.jochre.graphics.JochreCorpusGroupReader;
import com.joliciel.jochre.stats.FScoreCalculator;

/**
 * For evaluating a merge model.
 * @author Assaf Urieli
 *
 */
public interface MergeEvaluator {
	FScoreCalculator<String> evaluate(JochreCorpusGroupReader groupReader, ShapeMerger shapeMerger);

	/**
	 * The minimum probability to take a merge decision. Default: 0.5.
	 */
	public double getMinProbabilityForDecision();
	public void setMinProbabilityForDecision(double minProbabilityForDecision);

}
