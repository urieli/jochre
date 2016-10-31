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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.BoundaryDetector;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.CorpusSelectionCriteria;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreCorpusGroupReader;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.letterGuesser.features.LetterFeature;
import com.joliciel.talismane.machineLearning.ClassificationEvent;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

public class JochreLetterEventStream implements ClassificationEventStream {
	private static final Logger LOG = LoggerFactory.getLogger(JochreLetterEventStream.class);

	private final BoundaryDetector boundaryDetector;

	private final Set<LetterFeature<?>> features;
	private int shapeIndex = 0;

	private ShapeInSequence shapeInSequence = null;

	private LetterSequence history = null;

	private JochreCorpusGroupReader groupReader;
	private ShapeSequence shapeSequence = null;
	private final LetterValidator letterValidator;

	private int invalidLetterCount = 0;

	private final CorpusSelectionCriteria criteria;
	private final JochreSession jochreSession;

	/**
	 * Constructor.
	 * 
	 * @param features
	 *            the features to analyse when training
	 * @param recalculateFeatures
	 *            if true, features will be recalculated from scratch (slower,
	 *            but doesn't require previous analysis & database storage
	 *            space. If false, features will be loaded from the data store.
	 */
	public JochreLetterEventStream(Set<LetterFeature<?>> features, BoundaryDetector boundaryDetector, LetterValidator letterValidator,
			CorpusSelectionCriteria criteria, JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.features = features;
		this.boundaryDetector = boundaryDetector;
		this.letterValidator = letterValidator;
		this.criteria = criteria;
	}

	@Override
	public ClassificationEvent next() {
		ClassificationEvent event = null;
		if (this.hasNext()) {
			Shape shape = shapeInSequence.getShape();
			LOG.debug("next event, shape: " + shape);
			LetterGuesserContext context = new LetterGuesserContext(shapeInSequence, history);

			List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
			// analyse features
			for (LetterFeature<?> feature : features) {
				RuntimeEnvironment env = new RuntimeEnvironment();
				FeatureResult<?> featureResult = feature.check(context, env);
				if (featureResult != null) {
					featureResults.add(featureResult);
					if (LOG.isTraceEnabled()) {
						LOG.trace(featureResult.toString());
					}
				}
			}

			String outcome = shape.getLetter();

			event = new ClassificationEvent(featureResults, outcome);

			history.getLetters().add(outcome);
			// set shape to null so that hasNext can retrieve the next one.
			this.shapeInSequence = null;
		}
		return event;
	}

	@Override
	public boolean hasNext() {
		this.initialiseStream();

		while (shapeInSequence == null && shapeSequence != null) {
			while (shapeInSequence == null && shapeIndex < shapeSequence.size()) {
				shapeInSequence = shapeSequence.get(shapeIndex);
				shapeIndex++;

				Shape shape = shapeInSequence.getShape();
				String letter = shape.getLetter();
				if (!letterValidator.validate(letter)) {
					// if there's an invalid letter, skip the rest of this
					// group
					// note we allow empty letters (which is how we indicate
					// ink smudges
					// in the text)
					LOG.debug("Invalid letter for shape " + shapeInSequence.getOriginalShapes().get(0).getId() + ": " + letter);
					invalidLetterCount++;
					shapeInSequence = null;
					break;
				}
			}

			if (shapeInSequence == null) {
				this.getNextGroup();
			}
		}

		if (shapeInSequence == null) {
			LOG.debug("invalidLetterCount: " + invalidLetterCount);
		}
		return shapeInSequence != null;
	}

	void getNextGroup() {
		shapeSequence = null;
		shapeIndex = 0;
		if (groupReader.hasNext()) {
			GroupOfShapes group = groupReader.next();
			if (boundaryDetector != null) {
				// in this case the boundary detector is supposed to give us the
				// correct
				// splits and merges
				shapeSequence = boundaryDetector.findBoundaries(group).get(0);
			} else {
				// simply add this group's shapes
				shapeSequence = new ShapeSequence();
				for (Shape shape : group.getShapes())
					shapeSequence.addShape(shape);
			}

			history = new LetterSequence(shapeSequence, jochreSession);

		}
	}

	void initialiseStream() {
		if (groupReader == null) {
			groupReader = new JochreCorpusGroupReader(jochreSession);
			groupReader.setSelectionCriteria(criteria);
			this.getNextGroup();
		}
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = new LinkedHashMap<String, String>();
		attributes.put("eventStream", this.getClass().getSimpleName());
		attributes.putAll(this.criteria.getAttributes());

		return attributes;
	}

	public CorpusSelectionCriteria getCriteria() {
		return criteria;
	}

}
