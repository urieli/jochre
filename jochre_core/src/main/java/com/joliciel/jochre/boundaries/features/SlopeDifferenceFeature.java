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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.math.stat.regression.SimpleRegression;

import com.joliciel.jochre.boundaries.Split;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

/**
 * The intuition behind this feature is that a shape's contour at the split
 * should look something like this: &gt;&lt;. Given the split's position
 * (x-coordinate), we start with the top shape edge at this coordinate, and the
 * bottom shape edge at this coordinate. For each of these, we travel
 * [contourDistance] pixels to the right or left, following the shape's contour,
 * calculate the slope of the linear regression describing this contour, and
 * convert the slope to an angle. We then take the difference between the two
 * right hand angles (top and bottom contour) - this will tend to zero if the
 * slopes are more parallel, and will tend to 180° if the slopes are more
 * perpendicular. This difference is normalised to a value from 0 to 1 (where 0
 * = parallel, and 1=180°). The right-hand difference is then multiplied by the
 * left-hand difference.
 * 
 * @author Assaf Urieli
 *
 */
public class SlopeDifferenceFeature extends AbstractSplitFeature<Double> implements DoubleFeature<Split> {
  private static final Logger LOG = LoggerFactory.getLogger(SlopeDifferenceFeature.class);
  private IntegerFeature<Split> contourDistanceFeature;

  /**
   * 
   * @param contourDistanceFeature
   *            the distance to travel along the contour when calculating the
   *            slope
   */
  public SlopeDifferenceFeature(IntegerFeature<Split> contourDistanceFeature) {
    super();
    this.contourDistanceFeature = contourDistanceFeature;
    this.setName(super.getName() + "(" + contourDistanceFeature.getName() + ")");
  }

  @Override
  public FeatureResult<Double> checkInternal(Split split, RuntimeEnvironment env) {
    FeatureResult<Double> result = null;
    FeatureResult<Integer> contourDistanceResult = contourDistanceFeature.check(split, env);
    if (contourDistanceResult != null) {
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

      for (int i = 1; i <= contourDistance; i++) {
        if (x + i < shape.getWidth()) {
          topRightRegression.addData(x + i, verticalContour[x + i][0]);
          bottomRightRegression.addData(x + i, verticalContour[x + i][1]);
        }
        if (x - i >= 0) {
          topLeftRegression.addData(x - i, verticalContour[x - i][0]);
          bottomLeftRegression.addData(x - i, verticalContour[x - i][1]);
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
