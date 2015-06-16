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
package com.joliciel.jochre.letterGuesser;

import java.util.Set;

import com.joliciel.jochre.boundaries.BoundaryDetector;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.CorpusSelectionCriteria;
import com.joliciel.jochre.letterGuesser.features.LetterFeature;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.DecisionMaker;

public interface LetterGuesserService {
	public ClassificationEventStream getJochreLetterEventStream(
			CorpusSelectionCriteria criteria, Set<LetterFeature<?>> features,
			BoundaryDetector boundaryDetector, LetterValidator letterValidator);	
	public LetterGuesser getLetterGuesser(Set<LetterFeature<?>> features, DecisionMaker decisionMaker);
	
	public LetterGuesserContext getContext(ShapeInSequence shapeInSequence, LetterSequence history);
	
	/**
	 * Combine two letter sequences into a single sequence.
	 * @param sequence1
	 * @param sequence2
	 * @return
	 */
	public LetterSequence getLetterSequence(LetterSequence sequence1, LetterSequence sequence2);
	
	/**
	 * Get an empty letter sequence with a capacity of 0.
	 * @return
	 */
	LetterSequence getEmptyLetterSequence(ShapeSequence shapeSequence);
	
	/**
	 * Get a letter sequence built on the history provided, with one empty space at the end.
	 * @param history
	 */
	LetterSequence getLetterSequencePlusOne(LetterSequence history);

}
