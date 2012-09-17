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
package com.joliciel.jochre.boundaries.features;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.regression.SimpleRegression;

import com.joliciel.talismane.utils.features.DoubleFeature;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.features.IntegerFeature;
import com.joliciel.jochre.boundaries.Split;
import com.joliciel.jochre.graphics.Shape;

/**
 * Like SlopeDifferenceFeature, but rather than using the contour as visible from the top & bottom edges,
 * follows the actual contour of the shape.
 * @author Assaf Urieli
 *
 */
public class TrueContourSlopeDifferenceFeature extends AbstractSplitFeature<Double> implements DoubleFeature<Split> {
    private static final Log LOG = LogFactory.getLog(TrueContourSlopeDifferenceFeature.class);
    private IntegerFeature<Split> contourDistanceFeature;
    
    /**
     * 
     * @param contourDistance the distance to travel along the contour when calculating the slope
     */
	public TrueContourSlopeDifferenceFeature(IntegerFeature<Split> contourDistanceFeature) {
		super();
		this.contourDistanceFeature = contourDistanceFeature;
		this.setName(super.getName() + "(" + contourDistanceFeature.getName() + ")");
	}

	@Override
	public FeatureResult<Double> checkInternal(Split split) {
		FeatureResult<Double> result = null;
		FeatureResult<Integer> contourDistanceResult = contourDistanceFeature.check(split);
		if (contourDistanceResult!=null) {
			int contourDistance = contourDistanceResult.getOutcome();

			int[][] verticalContour = split.getShape().getVerticalContour();
			int startX = split.getPosition();
			Shape shape = split.getShape();
			int topStart = verticalContour[startX][0];
			int bottomStart = verticalContour[startX][1];
			
			SimpleRegression topRightRegression = new SimpleRegression();
			SimpleRegression bottomRightRegression = new SimpleRegression();
			SimpleRegression topLeftRegression = new SimpleRegression();
			SimpleRegression bottomLeftRegression = new SimpleRegression();
			
			topRightRegression.addData(startX, topStart);
			topLeftRegression.addData(startX, topStart);
			bottomRightRegression.addData(startX, bottomStart);
			bottomLeftRegression.addData(startX, bottomStart);
			
			int lastTopRight = topStart;
			int lastTopLeft = topStart;
			int lastBottomRight = bottomStart;
			int lastBottomLeft = bottomStart;
			for (int i = 1; i<=contourDistance; i++) {
				int x = startX + i;
				if (x<shape.getWidth()) {
					if (shape.isPixelBlack(x, lastTopRight)) {
						for (int y=lastTopRight-1; y>=-1; y--) {
							if (y<0||!shape.isPixelBlack(x, y)) {
								lastTopRight = y+1;
								topRightRegression.addData(x, lastTopRight);
								break;
							}
						}
					} else {
						for (int y=lastTopRight+1; y<=shape.getHeight(); y++) {
							if (y==shape.getHeight()||shape.isPixelBlack(x, y)) {
								lastTopRight = y;
								topRightRegression.addData(x, lastTopRight);
								break;
							}
						}
					}
					if (shape.isPixelBlack(x, lastBottomRight)) {
						for (int y=lastBottomRight+1; y<=shape.getHeight(); y++) {
							if (y==shape.getHeight()||!shape.isPixelBlack(x, y)) {
								lastBottomRight = y-1;
								bottomRightRegression.addData(x, lastBottomRight);
								break;
							}
						}
					} else {
						for (int y=lastBottomRight-1; y>=-1; y--) {
							if (y<0||shape.isPixelBlack(x, y)) {
								lastBottomRight = y;
								bottomRightRegression.addData(x, lastBottomRight);
								break;
							}
						}
					}
				}
				
				x = startX - i;
				if (x>=0) {
					if (shape.isPixelBlack(x, lastTopLeft)) {
						for (int y=lastTopLeft-1; y>=-1; y--) {
							if (y<0||!shape.isPixelBlack(x, y)) {
								lastTopLeft = y+1;
								topLeftRegression.addData(x, lastTopLeft);
								break;
							}
						}
					} else {
						for (int y=lastTopLeft+1; y<=shape.getHeight(); y++) {
							if (y==shape.getHeight()||shape.isPixelBlack(x, y)) {
								lastTopLeft = y;
								topLeftRegression.addData(x, lastTopLeft);
								break;
							}
						}
					}
					if (shape.isPixelBlack(x, lastBottomLeft)) {
						for (int y=lastBottomLeft+1; y<=shape.getHeight(); y++) {
							if (y==shape.getHeight()||!shape.isPixelBlack(x, y)) {
								lastBottomLeft = y-1;
								bottomLeftRegression.addData(x, lastBottomLeft);
								break;
							}
						}
					} else {
						for (int y=lastBottomLeft-1; y>=-1; y--) {
							if (y<0||shape.isPixelBlack(x, y)) {
								lastBottomLeft = y;
								bottomLeftRegression.addData(x, lastBottomLeft);
								break;
							}
						}
					}
				}
			}
			
			// get the slopes
			double topRightSlope = topRightRegression.getSlope();
			double bottomRightSlope = bottomRightRegression.getSlope();
			double topLeftSlope = topLeftRegression.getSlope();
			double bottomLeftSlope = bottomLeftRegression.getSlope();
			
			// convert slopes to angles
			double topRightAngle = Math.atan(topRightSlope);
			double bottomRightAngle = Math.atan(bottomRightSlope);
			double topLeftAngle = Math.atan(topLeftSlope);
			double bottomLeftAngle = Math.atan(bottomLeftSlope);
			
			
			// calculate the right & left-hand differences
			double rightDiff = Math.abs(topRightAngle - bottomRightAngle);
			double leftDiff = Math.abs(topLeftAngle - bottomLeftAngle);
			
			// normalise the differences from 0 to 1
			rightDiff = rightDiff / Math.PI;
			leftDiff = leftDiff / Math.PI;
			
			double product = rightDiff * leftDiff;
			
			if (LOG.isTraceEnabled()) {
				LOG.trace("topRightAngle: " + topRightAngle);
				LOG.trace("bottomRightAngle: " + bottomRightAngle);
				LOG.trace("topLeftAngle: " + topLeftAngle);
				LOG.trace("bottomLeftAngle: " + bottomLeftAngle);
				LOG.trace("rightDiff: " + rightDiff);
				LOG.trace("leftDiff: " + leftDiff);
				LOG.trace("product: " + product);
			}
			
			result = this.generateResult(product);
		}
		return result;
	}

}
