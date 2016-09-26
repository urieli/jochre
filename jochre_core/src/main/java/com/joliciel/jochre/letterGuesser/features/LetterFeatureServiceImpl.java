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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.boundaries.features.BoundaryFeatureService;
import com.joliciel.jochre.graphics.features.GraphicsFeatureService;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;

class LetterFeatureServiceImpl implements LetterFeatureServiceInternal {
	private static final Logger LOG = LoggerFactory.getLogger(LetterFeatureServiceImpl.class);
	private BoundaryFeatureService boundaryFeatureService;
	private GraphicsFeatureService graphicsFeatureService;
	private BoundaryService boundaryService;
	private LetterGuesserService letterGuesserService;
	private final JochreSession jochreSession;

	public LetterFeatureServiceImpl(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
	}

	@Override
	public LetterFeatureTester getFeatureTester() {
		LetterFeatureTesterImpl featureTester = new LetterFeatureTesterImpl(jochreSession);
		featureTester.setBoundaryService(this.getBoundaryService());
		featureTester.setLetterGuesserService(this.getLetterGuesserService());

		return featureTester;
	}

	@Override
	public Set<LetterFeature<?>> getLetterFeatureSet(List<String> featureDescriptors) {
		Set<LetterFeature<?>> features = new TreeSet<LetterFeature<?>>();
		FunctionDescriptorParser descriptorParser = new FunctionDescriptorParser();
		LetterFeatureParser mergeFeatureParser = this.getLetterFeatureParser();

		for (String featureDescriptor : featureDescriptors) {
			LOG.trace(featureDescriptor);
			if (featureDescriptor.length() > 0 && !featureDescriptor.startsWith("#")) {
				FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
				List<LetterFeature<?>> myFeatures = mergeFeatureParser.parseDescriptor(functionDescriptor);
				features.addAll(myFeatures);

			}
		}
		return features;
	}

	private LetterFeatureParser getLetterFeatureParser() {
		LetterFeatureParser parser = new LetterFeatureParser();
		parser.setShapeFeatureParser(this.getGraphicsFeatureService().getShapeFeatureParser());
		parser.setShapeInSequenceFeatureParser(this.getBoundaryFeatureService().getShapeInSequenceFeatureParser());

		return parser;
	}

	public BoundaryFeatureService getBoundaryFeatureService() {
		return boundaryFeatureService;
	}

	public void setBoundaryFeatureService(BoundaryFeatureService boundaryFeatureService) {
		this.boundaryFeatureService = boundaryFeatureService;
	}

	public GraphicsFeatureService getGraphicsFeatureService() {
		return graphicsFeatureService;
	}

	public void setGraphicsFeatureService(GraphicsFeatureService graphicsFeatureService) {
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
