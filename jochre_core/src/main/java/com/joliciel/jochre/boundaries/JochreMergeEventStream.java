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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.features.MergeFeature;
import com.joliciel.jochre.graphics.CorpusSelectionCriteria;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreCorpusGroupReader;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.ClassificationEvent;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.typesafe.config.Config;

public class JochreMergeEventStream implements ClassificationEventStream {
	private static final Logger LOG = LoggerFactory.getLogger(JochreMergeEventStream.class);

	private SplitCandidateFinder splitCandidateFinder;

	private final Set<MergeFeature<?>> mergeFeatures;
	private final JochreSession jochreSession;

	private double maxWidthRatio = 1.2;
	private double maxDistanceRatio = 0.15;

	private int shapeIndex = 0;

	private int belowRatioCount = 0;
	private int aboveRatioCount = 0;
	private int yesCount = 0;
	private int noCount = 0;

	private ShapePair mergeCandidate;

	private JochreCorpusGroupReader groupReader;
	private GroupOfShapes group = null;

	private final CorpusSelectionCriteria criteria;

	/**
	 * Constructor.
	 * 
	 * @param mergeFeatures
	 *            the features to analyse when training
	 */
	public JochreMergeEventStream(CorpusSelectionCriteria criteria, Set<MergeFeature<?>> mergeFeatures, JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.criteria = criteria;
		this.mergeFeatures = mergeFeatures;

		Config mergerConfig = jochreSession.getConfig().getConfig("jochre.boundaries.merger");
		maxWidthRatio = mergerConfig.getDouble("max-width-ratio");
		maxDistanceRatio = mergerConfig.getDouble("max-distance-ratio");
	}

	@Override
	public ClassificationEvent next() {
		ClassificationEvent event = null;
		if (this.hasNext()) {
			LOG.debug("next event, " + mergeCandidate.getFirstShape() + ", " + mergeCandidate.getSecondShape());

			List<FeatureResult<?>> featureResults = new ArrayList<>();

			// analyse features
			for (MergeFeature<?> feature : mergeFeatures) {
				RuntimeEnvironment env = new RuntimeEnvironment();
				FeatureResult<?> featureResult = feature.check(mergeCandidate, env);
				if (featureResult != null) {
					featureResults.add(featureResult);
					if (LOG.isTraceEnabled()) {
						LOG.trace(featureResult.toString());
					}
				}
			}

			MergeOutcome outcome = MergeOutcome.DO_NOT_MERGE;
			boolean shouldMerge = false;
			if (mergeCandidate.getFirstShape().getLetter().startsWith("|")) {
				if (mergeCandidate.getSecondShape().getLetter().length() == 0 || mergeCandidate.getSecondShape().getLetter().endsWith("|"))
					shouldMerge = true;
			} else if (mergeCandidate.getSecondShape().getLetter().endsWith("|")) {
				if (mergeCandidate.getFirstShape().getLetter().length() == 0)
					shouldMerge = true;
			}
			if (shouldMerge)
				outcome = MergeOutcome.DO_MERGE;

			if (outcome.equals(MergeOutcome.DO_MERGE))
				yesCount++;
			else
				noCount++;

			LOG.debug("Outcome: " + outcome);
			event = new ClassificationEvent(featureResults, outcome.name());

			// set mergeCandidate to null so that hasNext can retrieve the next
			// one.
			this.mergeCandidate = null;
		}
		return event;
	}

	@Override
	public boolean hasNext() {
		this.initialiseStream();

		while (mergeCandidate == null && group != null) {
			if (shapeIndex < group.getShapes().size() - 1) {
				Shape shape1 = group.getShapes().get(shapeIndex);
				Shape shape2 = group.getShapes().get(shapeIndex + 1);

				ShapePair shapePair = new ShapePair(shape1, shape2);
				double widthRatio = (double) shapePair.getWidth() / (double) shapePair.getXHeight();
				double distanceRatio = (double) shapePair.getInnerDistance() / (double) shapePair.getXHeight();
				if (widthRatio <= maxWidthRatio && distanceRatio <= maxDistanceRatio) {
					belowRatioCount++;
					mergeCandidate = shapePair;
				} else {
					aboveRatioCount++;
					mergeCandidate = null;
				}
				shapeIndex++;
			} else {
				group = null;
				shapeIndex = 0;
				if (groupReader.hasNext())
					group = groupReader.next();
			}
		}

		if (mergeCandidate == null) {

			LOG.debug("aboveRatioCount: " + aboveRatioCount);
			LOG.debug("belowRatioCount: " + belowRatioCount);
			LOG.debug("yesCount: " + yesCount);
			LOG.debug("noCount: " + noCount);
		}

		return mergeCandidate != null;
	}

	void initialiseStream() {
		if (groupReader == null) {
			groupReader = new JochreCorpusGroupReader(jochreSession);
			groupReader.setSelectionCriteria(criteria);
			if (groupReader.hasNext())
				group = groupReader.next();
		}
	}

	public SplitCandidateFinder getSplitCandidateFinder() {
		return splitCandidateFinder;
	}

	public void setSplitCandidateFinder(SplitCandidateFinder splitCandidateFinder) {
		this.splitCandidateFinder = splitCandidateFinder;
	}

	/**
	 * The maximum ratio between the merged shape candidate's width and x-height
	 * to even be considered for merging.
	 */
	public double getMaxWidthRatio() {
		return maxWidthRatio;
	}

	public void setMaxWidthRatio(double maxWidthRatio) {
		this.maxWidthRatio = maxWidthRatio;
	}

	/**
	 * The maximum ratio of the distance between the two inner shapes and the
	 * x-height to even be considered for merging.
	 */
	public double getMaxDistanceRatio() {
		return maxDistanceRatio;
	}

	public void setMaxDistanceRatio(double maxDistanceRatio) {
		this.maxDistanceRatio = maxDistanceRatio;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = new LinkedHashMap<>();
		attributes.put("eventStream", this.getClass().getSimpleName());
		attributes.put("maxDistanceRatio", "" + maxDistanceRatio);
		attributes.put("maxWidthRatio", "" + maxWidthRatio);
		attributes.putAll(this.criteria.getAttributes());

		return attributes;
	}

	public CorpusSelectionCriteria getCriteria() {
		return criteria;
	}

}
