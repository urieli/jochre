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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreCorpusGroupReader;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.stats.FScoreCalculator;

class MergeEvaluatorImpl implements MergeEvaluator {
	private static final Logger LOG = LoggerFactory.getLogger(MergeEvaluatorImpl.class);
	double maxWidthRatio = 1.2;
	double maxDistanceRatio = 0.15;
	double minProbabilityForDecision = 0.5;
	BoundaryServiceInternal boundaryServiceInternal;

	@Override
	public FScoreCalculator<String> evaluate(JochreCorpusGroupReader groupReader, ShapeMerger shapeMerger) {
		LOG.debug("evaluate");
		FScoreCalculator<String> fScoreCalculator = new FScoreCalculator<String>();

		while (groupReader.hasNext()) {
			GroupOfShapes group = groupReader.next();
			Shape previousShape = null;
			for (Shape shape : group.getShapes()) {
				if (previousShape != null) {

					ShapePair mergeCandidate = boundaryServiceInternal.getShapePair(previousShape, shape);
					double widthRatio = 0;
					double distanceRatio = 0;
					if (mergeCandidate.getXHeight() > 0) {
						widthRatio = (double) mergeCandidate.getWidth() / (double) mergeCandidate.getXHeight();
						distanceRatio = (double) mergeCandidate.getInnerDistance() / (double) mergeCandidate.getXHeight();
					}

					boolean shouldMerge = false;
					if (mergeCandidate.getFirstShape().getLetter().startsWith("|")) {
						if (mergeCandidate.getSecondShape().getLetter().length() == 0 || mergeCandidate.getSecondShape().getLetter().endsWith("|"))
							shouldMerge = true;
					} else if (mergeCandidate.getSecondShape().getLetter().endsWith("|")) {
						if (mergeCandidate.getFirstShape().getLetter().length() == 0)
							shouldMerge = true;
					}

					if (LOG.isTraceEnabled()) {
						LOG.trace(mergeCandidate.toString());
						LOG.trace("widthRatio: " + widthRatio);
						LOG.trace("distanceRatio: " + distanceRatio);
						LOG.trace("shouldMerge: " + shouldMerge);
					}

					if (widthRatio <= maxWidthRatio && distanceRatio <= maxDistanceRatio) {
						double mergeProb = shapeMerger.checkMerge(previousShape, shape);
						boolean wantsToMerge = (mergeProb >= minProbabilityForDecision);
						fScoreCalculator.increment(shouldMerge ? "YES" : "NO", wantsToMerge ? "YES" : "NO");
					} else {
						LOG.trace("too wide");
						if (shouldMerge)
							fScoreCalculator.increment("YES", "WIDE");
						else
							fScoreCalculator.increment("NO", "NO");
					} // too wide?
				} // have previous shape?
				previousShape = shape;
			} // next shape
		} // next group
		return fScoreCalculator;
	}

	public double getMaxWidthRatio() {
		return maxWidthRatio;
	}

	public void setMaxWidthRatio(double maxWidthRatio) {
		this.maxWidthRatio = maxWidthRatio;
	}

	public double getMaxDistanceRatio() {
		return maxDistanceRatio;
	}

	public void setMaxDistanceRatio(double maxDistanceRatio) {
		this.maxDistanceRatio = maxDistanceRatio;
	}

	@Override
	public double getMinProbabilityForDecision() {
		return minProbabilityForDecision;
	}

	@Override
	public void setMinProbabilityForDecision(double minProbabilityForDecision) {
		this.minProbabilityForDecision = minProbabilityForDecision;
	}

	public BoundaryServiceInternal getBoundaryServiceInternal() {
		return boundaryServiceInternal;
	}

	public void setBoundaryServiceInternal(BoundaryServiceInternal boundaryServiceInternal) {
		this.boundaryServiceInternal = boundaryServiceInternal;
	}

}
