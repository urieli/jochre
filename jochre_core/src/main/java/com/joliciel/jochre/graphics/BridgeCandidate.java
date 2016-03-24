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

/**
 * A vertical line segment in a shape which is a likely candidate for a bridge
 * between two letters which have been joined together by error (ink spot, etc.).
 * @author Assaf Urieli
 *
 */
public final class BridgeCandidate extends VerticalLineSegment {
	private double score = -1;
	private Shape shape = null;
	
	public BridgeCandidate(Shape shape, VerticalLineSegment line) {
		super(line.x, line.yTop);
		this.shape = shape;
		this.yBottom = line.yBottom;
		this.topTouch = line.yTop;
		this.bottomTouch = line.yBottom;
		this.leftSegments = line.leftSegments;
		this.rightSegments = line.rightSegments;
		this.leftShapeLeftBoundary = shape.getWidth()-1;
		this.leftShapeRightBoundary = 0;
		this.rightShapeLeftBoundary = shape.getWidth()-1;
		this.rightShapeRightBoundary = 0;
	}

	/**
	 * The total weight of pixels to the left of this bridge candidate.
	 */
	public int leftPixels = 0;
	
	/**
	 * The total weight of pixels to the right of this bridge candidate.
	 */
	public int rightPixels = 0;

	
	/**
	 * The right-hand boundary of the shape found to the left of this bridge candidate.
	 * Could be to the right of this candidate's x-coordinate, if the shape curls around above
	 * or below this candidate.
	 */
	public int leftShapeRightBoundary;
	
	/**
	 * The left-hand boundary of the shape found to the left of this bridge candidate.
	 */
	public int leftShapeLeftBoundary;
	
	/**
	 * The left-hand boundary of the shape found to the right of this bridge candidate.
	 * Could be to the left of this candidate's x-coordinate, if the shape curls around above
	 * or below this candidate.
	 */
	public int rightShapeLeftBoundary;
	
	/**
	 * The right-hand boundary of the shape found to the right of this bridge candidate.
	 */
	public int rightShapeRightBoundary;
	
	/**
	 * A group of vertical line segments contained in the shape to the left of this candidate.
	 */
	public VerticalLineGroup leftGroup;
	
	/**
	 * A group of vertical line segments contained in the shape to the right of this candidate.
	 */
	public VerticalLineGroup rightGroup;
	
	/**
	 * The bridge width should be considered where two vertical line segments touch each other 
	 * rather than for the full length of the candidate.
	 * topTouch gives the top y-coordinate of the space where the line segments actually touch.
	 */
	public int topTouch;
	
	/**
	 * See topTouch.
	 * bottomTouch gives the bottom y-coordinate of the space where the line segments actually touch.
	 */
	public int bottomTouch;
	
	/**
	 * The width of this bridge, in pixels.
	 */
	public int bridgeWidth() {
		return (bottomTouch - topTouch) + 1;
	}
	
	public int overlap() {
		return (this.leftShapeRightBoundary - this.rightShapeLeftBoundary);
	}
	
	/**
	 * A score used to rank this candidate.
	 */
	public double score() {
		// for now, this is a naive score
		// should really use machine learning to provide a linear combination of various feature combinations
		// but for this, we'd need a proper dataset
		if (score==-1) {
			double ratio = (double) this.leftPixels / (double) this.rightPixels;
			if (ratio > 1)
				ratio = 1 / ratio;
			
			score = ratio / (double) this.bridgeWidth();
			double overlap = (this.overlap() + 2) / shape.getWidth(); // value going from 0 to 1
			double overlapWeight = 1.0;
			score = score * (1 - (overlapWeight * overlap));
		}
		return score;
	}
}