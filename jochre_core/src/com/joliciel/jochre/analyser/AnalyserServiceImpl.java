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
package com.joliciel.jochre.analyser;

import com.joliciel.jochre.analyser.BeamSearchImageAnalyser;
import com.joliciel.jochre.analyser.ImageAnalyser;
import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.talismane.utils.ObjectCache;

class AnalyserServiceImpl implements AnalyserServiceInternal {
	private ObjectCache objectCache;	
	private LetterGuesserService letterGuesserService;
	private GraphicsService graphicsService;
	private BoundaryService boundaryService;
	
	public AnalyserServiceImpl() {
	}

	public ObjectCache getObjectCache() {
		return objectCache;
	}

	public void setObjectCache(ObjectCache objectCache) {
		this.objectCache = objectCache;
	}
	
	@Override
	public ImageAnalyser getBeamSearchImageAnalyser(int maxBeamWidth,
			double minOutcomeWeight) {
		BeamSearchImageAnalyser evaluator = new BeamSearchImageAnalyser(maxBeamWidth, minOutcomeWeight);
		evaluator.setAnalyserServiceInternal(this);
		evaluator.setGraphicsService(this.getGraphicsService());
		evaluator.setLetterGuesserService(this.getLetterGuesserService());
		evaluator.setBoundaryService(this.getBoundaryService());
		
		return evaluator;
	}

	private GraphicsService getGraphicsService() {
		return this.graphicsService;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}

	public LetterGuesserService getLetterGuesserService() {
		return letterGuesserService;
	}

	public void setLetterGuesserService(LetterGuesserService letterGuesserService) {
		this.letterGuesserService = letterGuesserService;
	}

	public BoundaryService getBoundaryService() {
		return boundaryService;
	}

	public void setBoundaryService(BoundaryService boundaryService) {
		this.boundaryService = boundaryService;
	}
	
	
}
