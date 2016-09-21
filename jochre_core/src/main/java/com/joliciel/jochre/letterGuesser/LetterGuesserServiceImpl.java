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

import java.util.List;
import java.util.Set;

import com.joliciel.jochre.boundaries.BoundaryDetector;
import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.CorpusSelectionCriteria;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.letterGuesser.features.LetterFeature;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.utils.ObjectCache;

class LetterGuesserServiceImpl implements LetterGuesserServiceInternal {
	private ObjectCache objectCache;
	private GraphicsService graphicsService;
	private BoundaryService boundaryService;

	public LetterGuesserServiceImpl() {
	}

	public ObjectCache getObjectCache() {
		return objectCache;
	}

	public void setObjectCache(ObjectCache objectCache) {
		this.objectCache = objectCache;
	}

	@Override
	public LetterGuesser getLetterGuesser(Set<LetterFeature<?>> features, DecisionMaker decisionMaker) {
		LetterGuesserImpl letterGuesser = new LetterGuesserImpl(features, decisionMaker);
		letterGuesser.setLetterGuesserServiceInternal(this);
		return letterGuesser;
	}

	private GraphicsService getGraphicsService() {
		return graphicsService;
	}

	@Override
	public LetterSequence getEmptyLetterSequence(ShapeSequence shapeSequence) {
		LetterSequenceImpl letterSequence = new LetterSequenceImpl(shapeSequence);
		letterSequence.setBoundaryService(this.getBoundaryService());
		letterSequence.setLetterGuesserService(this);
		return letterSequence;
	}

	@Override
	public LetterSequence getLetterSequencePlusOne(LetterSequence history) {
		LetterSequenceImpl letterSequence = new LetterSequenceImpl(history);
		letterSequence.setBoundaryService(this.getBoundaryService());
		letterSequence.setLetterGuesserService(this);
		return letterSequence;
	}

	@Override
	public LetterSequence getLetterSequence(LetterSequence sequence1, LetterSequence sequence2) {
		LetterSequenceImpl letterSequence = new LetterSequenceImpl(sequence1, sequence2, this.getBoundaryService());
		letterSequence.setLetterGuesserService(this);
		return letterSequence;
	}

	@Override
	public LetterSequence getLetterSequence(ShapeSequence shapeSequence, List<String> letters) {
		LetterSequenceImpl letterSequence = new LetterSequenceImpl(shapeSequence, letters);
		letterSequence.setBoundaryService(this.getBoundaryService());
		letterSequence.setLetterGuesserService(this);
		return letterSequence;
	}

	@Override
	public LetterGuesserContext getContext(ShapeInSequence shapeInSequence, LetterSequence history) {
		LetterGuesserContext context = new LetterGuesserContextImpl(shapeInSequence, history);
		return context;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}

	private BoundaryService getBoundaryService() {
		return boundaryService;
	}

	@Override
	public ClassificationEventStream getJochreLetterEventStream(CorpusSelectionCriteria criteria, Set<LetterFeature<?>> features,
			BoundaryDetector boundaryDetector, LetterValidator letterValidator) {
		JochreLetterEventStream eventStream = new JochreLetterEventStream(features, letterValidator);
		eventStream.setCriteria(criteria);
		eventStream.setGraphicsService(this.getGraphicsService());
		eventStream.setLetterGuesserServiceInternal(this);
		eventStream.setBoundaryService(this.getBoundaryService());

		eventStream.setBoundaryDetector(boundaryDetector);
		return eventStream;
	}

	public void setBoundaryService(BoundaryService boundaryService) {
		this.boundaryService = boundaryService;
	}

}
