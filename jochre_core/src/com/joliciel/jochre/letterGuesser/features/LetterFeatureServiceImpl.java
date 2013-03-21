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
package com.joliciel.jochre.letterGuesser.features;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.boundaries.features.BoundaryFeatureService;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.features.GraphicsFeatureService;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.jochre.letterGuesser.features.LetterFeature;
import com.joliciel.jochre.letterGuesser.features.LetterFeatureTester;
import com.joliciel.jochre.letterGuesser.features.LetterFeatureTesterImpl;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;

class LetterFeatureServiceImpl implements LetterFeatureServiceInternal {
    private static final Log LOG = LogFactory.getLog(LetterFeatureServiceImpl.class);
	private GraphicsService graphicsService;
	private FeatureService featureService;
	private BoundaryFeatureService boundaryFeatureService;
	private GraphicsFeatureService graphicsFeatureService;
	private BoundaryService boundaryService;
	private LetterGuesserService letterGuesserService;

	@Override
	public LetterFeatureTester getFeatureTester() {
		LetterFeatureTesterImpl featureTester = new LetterFeatureTesterImpl();
		featureTester.setGraphicsService(this.getGraphicsService());
		featureTester.setBoundaryService(this.getBoundaryService());
		featureTester.setLetterGuesserService(this.getLetterGuesserService());
		featureTester.setFeatureService(this.getFeatureService());
		
		return featureTester;
	}
	
	public GraphicsService getGraphicsService() {
		return graphicsService;
	}

	@Override
	public Set<LetterFeature<?>> getLetterFeatureSet(
			List<String> featureDescriptors) {
		Set<LetterFeature<?>> features = new TreeSet<LetterFeature<?>>();
		FunctionDescriptorParser descriptorParser = this.getFeatureService().getFunctionDescriptorParser();
		LetterFeatureParser mergeFeatureParser = this.getLetterFeatureParser();
		
		for (String featureDescriptor : featureDescriptors) {
			LOG.trace(featureDescriptor);
			if (featureDescriptor.length()>0 && !featureDescriptor.startsWith("#")) {
				FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
				List<LetterFeature<?>> myFeatures = mergeFeatureParser.parseDescriptor(functionDescriptor);
				features.addAll(myFeatures);
				
			}
		}
		return features;
	}

	private LetterFeatureParser getLetterFeatureParser() {
		LetterFeatureParser parser = new LetterFeatureParser(this.getFeatureService());
		parser.setShapeFeatureParser(this.getGraphicsFeatureService().getShapeFeatureParser());
		parser.setShapeInSequenceFeatureParser(this.getBoundaryFeatureService().getShapeInSequenceFeatureParser());
		
		return parser;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

	public BoundaryFeatureService getBoundaryFeatureService() {
		return boundaryFeatureService;
	}

	public void setBoundaryFeatureService(
			BoundaryFeatureService boundaryFeatureService) {
		this.boundaryFeatureService = boundaryFeatureService;
	}

	public GraphicsFeatureService getGraphicsFeatureService() {
		return graphicsFeatureService;
	}

	public void setGraphicsFeatureService(
			GraphicsFeatureService graphicsFeatureService) {
		this.graphicsFeatureService = graphicsFeatureService;
	}

	public BoundaryService getBoundaryService() {
		return boundaryService;
	}

	public void setBoundaryService(BoundaryService boundaryService) {
		this.boundaryService = boundaryService;
	}

	public LetterGuesserService getLetterGuesserService() {
		return letterGuesserService;
	}

	public void setLetterGuesserService(LetterGuesserService letterGuesserService) {
		this.letterGuesserService = letterGuesserService;
	}

}
