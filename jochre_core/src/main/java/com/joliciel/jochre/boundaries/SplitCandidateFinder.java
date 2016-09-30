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
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.graphics.Shape;
import com.typesafe.config.Config;

/**
 * Finds split candidates in given shape.
 * 
 * @author Assaf Urieli
 *
 */
public class SplitCandidateFinder {
	private static final Logger LOG = LoggerFactory.getLogger(SplitCandidateFinder.class);
	private int minDistanceBetweenSplits;

	private final JochreSession jochreSession;

	public SplitCandidateFinder(JochreSession jochreSession) {
		this.jochreSession = jochreSession;

		Config splitterConfig = jochreSession.getConfig().getConfig("jochre.boundaries.splitter");
		minDistanceBetweenSplits = splitterConfig.getInt("min-distance-between-splits");
	}

	/**
	 * Find a list of split candidates in a given shape.
	 */
	public List<Split> findSplitCandidates(Shape shape) {
		List<Split> splitCandidates = new ArrayList<Split>();

		// generate a list giving the total distance from the top and bottom to the
		// shape's edge
		// the hypothesis is that splits almost always occur at x-coordinates
		// where there's a summit in the distance to the shape's edge, between two
		// valleys
		int[] edgeDistances = new int[shape.getWidth()];
		int[][] verticalContour = shape.getVerticalContour();
		for (int x = 0; x < shape.getWidth(); x++) {
			int edgeDistance = verticalContour[x][0] + ((shape.getHeight() - 1) - verticalContour[x][1]);
			if (edgeDistance > shape.getHeight() - 1)
				edgeDistance = shape.getHeight() - 1;

			edgeDistances[x] = edgeDistance;
		}

		int[] maximaMinima = new int[shape.getWidth()];
		int lastDistance = -1;
		boolean rising = true;
		int i = 0;
		for (int edgeDistance : edgeDistances) {
			if (lastDistance >= 0) {
				if (edgeDistance < lastDistance && rising) {
					maximaMinima[i - 1] = 1;
				}
				if (edgeDistance > lastDistance && !rising) {
					maximaMinima[i - 1] = -1;
				}
			}
			if (edgeDistance > lastDistance) {
				rising = true;
			} else if (edgeDistance < lastDistance) {
				rising = false;
			}

			lastDistance = edgeDistance;
			i++;
		}
		maximaMinima[0] = 1;
		if (rising)
			maximaMinima[shape.getWidth() - 1] = 1;
		else
			maximaMinima[shape.getWidth() - 1] = -1;

		for (int x = 0; x < shape.getWidth(); x++) {
			String maxMin = "";
			if (maximaMinima[x] < 0)
				maxMin = " min";
			if (maximaMinima[x] > 0)
				maxMin = " max";
			LOG.trace("edgeDistance[" + x + "]: " + edgeDistances[x] + maxMin);
		}

		boolean haveMinimum = false;
		int lastMaximum = -1;
		int lastMinValue = 0;
		int lastMaxValue = 0;
		TreeSet<SplitCandidateValue> splitCandidateValues = new TreeSet<SplitCandidateFinder.SplitCandidateValue>();
		for (i = 0; i < shape.getWidth(); i++) {
			if (maximaMinima[i] < 0) {
				haveMinimum = true;
				if (lastMaximum > 0) {
					double diff = ((lastMaxValue - lastMinValue) + (lastMaxValue - edgeDistances[i])) / 2.0;
					splitCandidateValues.add(new SplitCandidateValue(lastMaximum, diff));
				}
				lastMinValue = edgeDistances[i];
			}
			if (maximaMinima[i] > 0) {
				if (haveMinimum) {
					lastMaximum = i;
					lastMaxValue = edgeDistances[i];
				}
				haveMinimum = false;
			}
		}

		List<SplitCandidateValue> candidatesToRemove = new ArrayList<SplitCandidateFinder.SplitCandidateValue>();
		for (SplitCandidateValue thisValue : splitCandidateValues) {
			if (candidatesToRemove.contains(thisValue))
				continue;
			for (SplitCandidateValue otherValue : splitCandidateValues) {
				if (candidatesToRemove.contains(otherValue))
					continue;
				if (otherValue.equals(thisValue))
					break;
				int distance = thisValue.getPosition() - otherValue.getPosition();
				if (distance < 0)
					distance = 0 - distance;
				if (distance < this.minDistanceBetweenSplits) {
					LOG.trace("Removing candidate " + otherValue.getPosition() + ", distance=" + distance);
					candidatesToRemove.add(otherValue);
				}
			}
		}
		splitCandidateValues.removeAll(candidatesToRemove);

		TreeMap<Integer, Split> splitCandidateMap = new TreeMap<Integer, Split>();
		for (SplitCandidateValue candidateValue : splitCandidateValues) {
			Split splitCandidate = new Split(shape, jochreSession);
			splitCandidate.setPosition(candidateValue.getPosition());
			splitCandidateMap.put(candidateValue.getPosition(), splitCandidate);
		}

		for (Split split : splitCandidateMap.values())
			splitCandidates.add(split);
		return splitCandidates;
	}

	private static class SplitCandidateValue implements Comparable<SplitCandidateValue> {
		int position;
		double magnitude;

		private SplitCandidateValue(int position, double magnitude) {
			super();
			this.position = position;
			this.magnitude = magnitude;
		}

		public int getPosition() {
			return position;
		}

		public double getMagnitude() {
			return magnitude;
		}

		@Override
		public int compareTo(SplitCandidateValue o) {
			if (position == o.getPosition())
				return 0;
			else if (this.magnitude >= o.getMagnitude())
				return -1;
			else
				return 1;
		}

	}

	public int getMinDistanceBetweenSplits() {
		return minDistanceBetweenSplits;
	}

	public void setMinDistanceBetweenSplits(int minDistanceBetweenSplits) {
		this.minDistanceBetweenSplits = minDistanceBetweenSplits;
	}

}
