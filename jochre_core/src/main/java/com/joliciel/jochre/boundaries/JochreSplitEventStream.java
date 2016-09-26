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
import com.joliciel.jochre.boundaries.features.SplitFeature;
import com.joliciel.jochre.graphics.CorpusSelectionCriteria;
import com.joliciel.jochre.graphics.JochreCorpusShapeReader;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.ClassificationEvent;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.utils.PerformanceMonitor;

class JochreSplitEventStream implements ClassificationEventStream {
	private static final Logger LOG = LoggerFactory.getLogger(JochreSplitEventStream.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(JochreSplitEventStream.class);

	private BoundaryServiceInternal boundaryService;
	private SplitCandidateFinder splitCandidateFinder;

	private final Set<SplitFeature<?>> splitFeatures;
	private final JochreSession jochreSession;

	private double minWidthRatio = 1.1;
	private double minHeightRatio = 1.0;

	private int splitCandidateIndex = 0;

	private int belowRatioCount = 0;
	private int aboveRatioCount = 0;
	private int yesCount = 0;
	private int noCount = 0;

	List<Split> splitCandidates = null;
	private Split splitCandidate = null;

	private JochreCorpusShapeReader shapeReader;
	private Shape shape;

	private CorpusSelectionCriteria criteria;

	/**
	 * Constructor.
	 * 
	 * @param splitFeatures
	 *          the SplitFeatures to analyse when training
	 */
	public JochreSplitEventStream(Set<SplitFeature<?>> splitFeatures, JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.splitFeatures = splitFeatures;
	}

	@Override
	public ClassificationEvent next() {
		MONITOR.startTask("next");
		try {
			ClassificationEvent event = null;
			if (this.hasNext()) {
				LOG.debug("next event, " + splitCandidate.getShape() + ", split: " + splitCandidate.getPosition());

				List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();

				MONITOR.startTask("analyse features");
				try {
					for (SplitFeature<?> feature : splitFeatures) {
						MONITOR.startTask(feature.getName());
						try {
							RuntimeEnvironment env = new RuntimeEnvironment();
							FeatureResult<?> featureResult = feature.check(splitCandidate, env);
							if (featureResult != null) {
								featureResults.add(featureResult);
								if (LOG.isTraceEnabled()) {
									LOG.trace(featureResult.toString());
								}
							}
						} finally {
							MONITOR.endTask();
						}
					}
				} finally {
					MONITOR.endTask();
				}

				SplitOutcome outcome = SplitOutcome.DO_NOT_SPLIT;
				for (Split split : splitCandidate.getShape().getSplits()) {
					int distance = splitCandidate.getPosition() - split.getPosition();
					if (distance < 0)
						distance = 0 - distance;
					// Note: making the distance to the split the same as the min distance
					// between splits
					// is somewhat arbitrary, and obviously allows for ambiguity, where 2
					// split candidates
					// both return "YES" for the same split
					// Ideally, we'd have a minDistanceBetweenSplits = n (where n is odd)
					// and than calculate this distance as n / 2 (e.g. 9 and 4).
					// But this reduces recall too much, and what we care about here is
					// recall
					if (distance < splitCandidateFinder.getMinDistanceBetweenSplits()) {
						outcome = SplitOutcome.DO_SPLIT;
						break;
					}
				}
				if (outcome.equals(SplitOutcome.DO_SPLIT))
					yesCount++;
				else
					noCount++;

				LOG.debug("Outcome: " + outcome);
				event = new ClassificationEvent(featureResults, outcome.name());

				// set splitCandidate to null so that hasNext can retrieve the next one.
				this.splitCandidate = null;
			}
			return event;
		} finally {
			MONITOR.endTask();
		}
	}

	@Override
	public boolean hasNext() {
		MONITOR.startTask("hasNext");
		try {
			this.initialiseStream();

			while (splitCandidate == null && shape != null) {
				if (splitCandidateIndex < splitCandidates.size()) {
					splitCandidate = splitCandidates.get(splitCandidateIndex);
					splitCandidateIndex++;
				} else {
					this.getNextShape();
				}
			}

			if (splitCandidate == null) {
				LOG.debug("aboveRatioCount: " + aboveRatioCount);
				LOG.debug("belowRatioCount: " + belowRatioCount);
				LOG.debug("yesCount: " + yesCount);
				LOG.debug("noCount: " + noCount);
			}

			return splitCandidate != null;
		} finally {
			MONITOR.endTask();
		}
	}

	void getNextShape() {
		shape = null;
		while (shape == null && shapeReader.hasNext()) {
			shape = shapeReader.next();
			double widthRatio = (double) shape.getWidth() / (double) shape.getXHeight();
			double heightRatio = (double) shape.getHeight() / (double) shape.getXHeight();
			if (widthRatio >= minWidthRatio && heightRatio >= minHeightRatio) {
				aboveRatioCount++;
				splitCandidates = splitCandidateFinder.findSplitCandidates(shape);
				splitCandidateIndex = 0;
			} else {
				belowRatioCount++;
				splitCandidates = null;
				shape = null;
			}
		}

	}

	void initialiseStream() {
		if (shapeReader == null) {
			shapeReader = new JochreCorpusShapeReader(jochreSession);
			shapeReader.setSelectionCriteria(criteria);
			this.getNextShape();
		}
	}

	public BoundaryServiceInternal getBoundaryService() {
		return boundaryService;
	}

	public void setBoundaryService(BoundaryServiceInternal boundaryService) {
		this.boundaryService = boundaryService;
	}

	public SplitCandidateFinder getSplitCandidateFinder() {
		return splitCandidateFinder;
	}

	public void setSplitCandidateFinder(SplitCandidateFinder splitCandidateFinder) {
		this.splitCandidateFinder = splitCandidateFinder;
	}

	/**
	 * The minimum ratio between the shape's width and it's x-height for the shape
	 * to even be considered for splitting.
	 */
	public double getMinWidthRatio() {
		return minWidthRatio;
	}

	public void setMinWidthRatio(double minWidthRatio) {
		this.minWidthRatio = minWidthRatio;
	}

	public double getMinHeightRatio() {
		return minHeightRatio;
	}

	public void setMinHeightRatio(double minHeightRatio) {
		this.minHeightRatio = minHeightRatio;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = new LinkedHashMap<String, String>();
		attributes.put("eventStream", this.getClass().getSimpleName());
		attributes.put("minHeightRatio", "" + minHeightRatio);
		attributes.put("minWidthRatio", "" + minWidthRatio);
		attributes.putAll(this.criteria.getAttributes());

		return attributes;
	}

	public CorpusSelectionCriteria getCriteria() {
		return criteria;
	}

	public void setCriteria(CorpusSelectionCriteria criteria) {
		this.criteria = criteria;
	}

}
