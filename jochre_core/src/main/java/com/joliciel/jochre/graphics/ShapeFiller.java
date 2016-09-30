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
package com.joliciel.jochre.graphics;

import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to fill "holes" within a shape, and also determine how much filling is
 * required.
 * 
 * @author Assaf Urieli
 *
 */
public class ShapeFiller {
	private static final Logger LOG = LoggerFactory.getLogger(ShapeFiller.class);
	public static final int MAX_FILL_FACTOR = 10;

	/**
	 * How many neighbours are required to be ON for this pixel to be ON.
	 * Neighbours are the 8 surrounding pixels. Note if this value = 5, filling is
	 * pretty much guaranteed to stabilize. If this value = 4, filling will
	 * typically not stabilize as quickly, as whole lines will get filled in
	 * little by little as long as there's one chupchik. However, the value of 4
	 * is necessary to fill in any holes that are more than one pixel wide.
	 */
	public static final int NEIGHBOUR_COUNT_BIRTH = 4;

	/**
	 * Given a shape and a "black" threshold, how many time do we need to run the
	 * filling algorithm to fill up the holes.
	 */
	public int getFillFactor(Shape shape, int threshold) {
		// Note, we get the fill factor based on a neighbour count of 5,
		// on the assumption that "holey" shapes will contain more white space
		// that's surrounded on all sides.
		BitSet bitset = shape.getBlackAndWhiteBitSet(threshold);
		int fillFactor = MAX_FILL_FACTOR;
		int startCardinality = bitset.cardinality();
		LOG.debug("startCardinality: " + startCardinality);
		int endCardinality = startCardinality;
		int lastCardinality = startCardinality;
		for (int i = 0; i < MAX_FILL_FACTOR; i++) {
			BitSet newBitSet = this.fillBitSet(shape, bitset, 5);
			endCardinality = newBitSet.cardinality();
			LOG.debug("endCardinality: " + endCardinality);
			double percentIncrease = ((double) endCardinality / (double) lastCardinality) * 100.0;
			LOG.debug("% increase: " + percentIncrease);
			// for NEIGHBOUR_COUNT_BIRTH = 5, we could check, or a growth percentage
			// if (newBitSet.cardinality()==bitset.cardinality()) {
			// for NEIGHBOUR_COUNT_BIRTH = 4, we need to go by a growth percentage
			// if (percentIncrease < 105) {
			if (percentIncrease < 105) {
				fillFactor = i;
				break;
			}
			bitset = newBitSet;
			lastCardinality = endCardinality;
		}
		LOG.debug("Fill factor: " + fillFactor);
		LOG.debug("Total % increase: " + (((double) endCardinality / (double) startCardinality) * 100.0));
		return fillFactor;
	}

	/**
	 * Return a bitset corresponding to the "filled-in" shape.
	 */
	public BitSet fillShape(Shape shape, int threshold, int fillFactor) {
		// We fill the shapes based on a neighbour count of 4, to make it possible
		// to fill holes that are wider than one pixel.
		BitSet bitset = shape.getBlackAndWhiteBitSet(threshold);
		for (int i = 0; i < fillFactor; i++) {
			bitset = this.fillBitSet(shape, bitset, 4);
		}
		return bitset;
	}

	BitSet fillBitSet(Shape shape, BitSet bitset, int neighbourBirthCount) {
		BitSet newBitSet = new BitSet(bitset.size());
		int baseIndex = 0;
		for (int y = 0; y < shape.getHeight(); y++) {
			for (int x = 0; x < shape.getWidth(); x++) {
				int index = baseIndex + x;
				if (bitset.get(index))
					newBitSet.set(index);
				else {
					int surroundingCount = 0;
					if (y > 0) {
						if (x > 0)
							surroundingCount += bitset.get(index - (shape.getWidth() + 1)) ? 1 : 0;
						surroundingCount += bitset.get(index - (shape.getWidth())) ? 1 : 0;
						if (x < shape.getWidth() - 1)
							surroundingCount += bitset.get(index - (shape.getWidth() - 1)) ? 1 : 0;
					}
					if (x > 0)
						surroundingCount += bitset.get(index - 1) ? 1 : 0;
					if (x < shape.getWidth() - 1)
						surroundingCount += bitset.get(index + 1) ? 1 : 0;
					if (y < shape.getHeight() - 1) {
						if (x > 0)
							surroundingCount += bitset.get(index + (shape.getWidth() - 1)) ? 1 : 0;
						surroundingCount += bitset.get(index + (shape.getWidth())) ? 1 : 0;
						if (x < shape.getWidth() - 1)
							surroundingCount += bitset.get(index + (shape.getWidth() + 1)) ? 1 : 0;
					}
					// if at least NEIGHBOUR_COUNT_BIRTH out of 8 surrounding pixels are
					// on,
					// assume this one should be on
					if (surroundingCount >= NEIGHBOUR_COUNT_BIRTH)
						newBitSet.set(index);
				}
			}
			baseIndex += shape.getWidth();
		}
		return newBitSet;
	}
}
