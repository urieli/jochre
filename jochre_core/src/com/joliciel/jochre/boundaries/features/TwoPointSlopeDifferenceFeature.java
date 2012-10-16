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

import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.jochre.boundaries.Split;
import com.joliciel.jochre.graphics.Shape;

/**
 * Like SlopeDifferenceFeature, except that we only look at the contour point most distant from the start point.
 * @author Assaf Urieli
 *
 */
public class TwoPointSlopeDifferenceFeature extends AbstractSplitFeature<Double> implements DoubleFeature<Split> {
    private static final Log LOG = LogFactory.getLog(TwoPointSlopeDifferenceFeature.class);
    private IntegerFeature<Split> contourDistanceFeature;
    
    /**
     * 
     * @param contourDistance the distance to travel along the contour when calculating the slope
     */
	public TwoPointSlopeDifferenceFeature(IntegerFeature<Split> contourDistanceFeature) {
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
			int x = split.getPosition();
			Shape shape = split.getShape();
			int topStart = verticalContour[x][0];
			int bottomStart = verticalContour[x][1];
			
			SimpleRegression topRightRegression = new SimpleRegression();
			SimpleRegression bottomRightRegression = new SimpleRegression();
			SimpleRegression topLeftRegression = new SimpleRegression();
			SimpleRegression bottomLeftRegression = new SimpleRegression();
			
			topRightRegression.addData(x, topStart);
			topLeftRegression.addData(x, topStart);
			bottomRightRegression.addData(x, bottomStart);
			bottomLeftRegression.addData(x, bottomStart);
			
			int[] minTopRight = new int[] {x,topStart};
			int[] minTopLeft = new int[] {x,topStart};
			int[] maxTopRight = new int[] {x,topStart};
			int[] maxTopLeft = new int[] {x,topStart};
			int[] minBottomRight = new int[] {x,bottomStart};
			int[] minBottomLeft= new int[] {x,bottomStart};
			int[] maxBottomRight = new int[] {x,bottomStart};
			int[] maxBottomLeft = new int[] {x,bottomStart};
			for (int i = 1; i<=contourDistance; i++) {
				if (x+i<shape.getWidth()) {
					if (verticalContour[x+i][0]<minTopRight[1])
						minTopRight = new int[] {x+i,verticalContour[x+i][0]};
					if (verticalContour[x+i][0]>maxTopRight[1])
						maxTopRight = new int[] {x+i,verticalContour[x+i][0]};
	
					if (verticalContour[x+i][1]<minBottomRight[1])
						minBottomRight = new int[] {x+i,verticalContour[x+i][1]};
					if (verticalContour[x+i][1]>maxBottomRight[1])
						maxBottomRight = new int[] {x+i,verticalContour[x+i][1]};
				}
				if (x-i>=0) {
					if (verticalContour[x-i][0]<minTopLeft[1])
						minTopLeft = new int[] {x-i,verticalContour[x-i][0]};
					if (verticalContour[x-i][0]>maxTopLeft[1])
						maxTopLeft = new int[] {x-i,verticalContour[x-i][0]};
	
					if (verticalContour[x-i][1]<minBottomLeft[1])
						minBottomLeft = new int[] {x-i,verticalContour[x-i][1]};
					if (verticalContour[x-i][1]>maxBottomLeft[1])
						maxBottomLeft = new int[] {x-i,verticalContour[x-i][1]};
				}
			}
			
			if (minTopRight[0]==x)
				topRightRegression.addData(maxTopRight[0], maxTopRight[1]);
			else
				topRightRegression.addData(minTopRight[0], minTopRight[1]);
	
			if (minTopLeft[0]==x)
				topLeftRegression.addData(maxTopLeft[0], maxTopLeft[1]);
			else
				topLeftRegression.addData(minTopLeft[0], minTopLeft[1]);
	
			if (maxBottomRight[0]==x)
				bottomRightRegression.addData(minBottomRight[0], minBottomRight[1]);
			else
				bottomRightRegression.addData(maxBottomRight[0], maxBottomRight[1]);
				
			if (maxBottomLeft[0]==x)
				bottomLeftRegression.addData(minBottomLeft[0], minBottomLeft[1]);
			else
				bottomLeftRegression.addData(maxBottomLeft[0], maxBottomLeft[1]);
			
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
