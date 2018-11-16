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

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.jochre.boundaries.ShapePair;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Shape;

/**
 * Counts the number of neighbors on the borderline between the two shapes,
 * proportional to x-height * 0.25.
 * @author Assaf Urieli
 *
 */
public class BorderlineNeighboursFeature extends AbstractMergeFeature<Double> implements DoubleFeature<ShapePair> {
  private static final Logger LOG = LoggerFactory.getLogger(BorderlineNeighboursFeature.class);
  private IntegerFeature<ShapePair> verticalToleranceFeature;
  private IntegerFeature<ShapePair> horizontalToleranceFeature;
  
  public BorderlineNeighboursFeature(IntegerFeature<ShapePair> horizontalToleranceFeature, IntegerFeature<ShapePair> verticalToleranceFeature) {
    super();
    this.horizontalToleranceFeature = horizontalToleranceFeature;
    this.verticalToleranceFeature = verticalToleranceFeature;
    this.setName(super.getName() + "(" + horizontalToleranceFeature.getName() + "," + verticalToleranceFeature.getName() + ")");
  }


  @Override
  public FeatureResult<Double> checkInternal(ShapePair pair, RuntimeEnvironment env) {
    FeatureResult<Double> result = null;
  
    FeatureResult<Integer> horizontalToleranceResult = horizontalToleranceFeature.check(pair, env);
    FeatureResult<Integer> verticalToleranceResult = verticalToleranceFeature.check(pair, env);
    
    if (horizontalToleranceResult!=null && verticalToleranceResult!=null) {
      int horizontalTolerance = horizontalToleranceResult.getOutcome();
      int verticalTolerance = verticalToleranceResult.getOutcome();
  
      Shape shape1 = pair.getFirstShape();
      Shape shape2 = pair.getSecondShape();
      JochreImage sourceImage = shape1.getJochreImage();
      
      // check that the two shapes have dark areas near each other
      Set<Integer> shape1BorderPoints = new HashSet<Integer>();
      int shape1MinBorder = sourceImage.isLeftToRight() ? (shape1.getWidth()-horizontalTolerance) - 1 : 0;
      int shape1MaxBorder = sourceImage.isLeftToRight() ? shape1.getWidth() : horizontalTolerance + 1;
      
      LOG.trace("shape1MinBorder" + shape1MinBorder);
      LOG.trace("shape1MaxBorder" + shape1MaxBorder);
      StringBuilder sb = new StringBuilder();
  
      for (int x = shape1MinBorder; x<shape1MaxBorder; x++) {
        for (int y = 0; y< shape1.getHeight(); y++) {
          if (shape1.isPixelBlack(x, y, sourceImage.getBlackThreshold())) {
            shape1BorderPoints.add(shape1.getTop()+y);
            sb.append(shape1.getTop()+y);
            sb.append(',');
          }
        }
      }
      LOG.trace(sb.toString());
      Set<Integer> shape2BorderPoints = new HashSet<Integer>();
      sb = new StringBuilder();
      int shape2MinBorder = sourceImage.isLeftToRight() ? 0 : (shape2.getWidth()-horizontalTolerance) - 1;
      int shape2MaxBorder = sourceImage.isLeftToRight() ? horizontalTolerance + 1 : shape2.getWidth();
      LOG.trace("shape2MinBorder" + shape2MinBorder);
      LOG.trace("shape2MaxBorder" + shape2MaxBorder);
      for (int x = shape2MinBorder; x<shape2MaxBorder; x++) {
        for (int y = 0; y< shape2.getHeight(); y++) {
          if (shape2.isPixelBlack(x, y, sourceImage.getBlackThreshold())) {
            shape2BorderPoints.add(shape2.getTop()+y);
            sb.append(shape2.getTop()+y);
            sb.append(',');
          }
        }
      }
      LOG.trace(sb.toString());
      int numNeighbours1 = 0;
      for (int shape1BorderPoint : shape1BorderPoints) {
        for (int shape2BorderPoint : shape2BorderPoints) {
          if (Math.abs(shape2BorderPoint - shape1BorderPoint) <= verticalTolerance) {
            numNeighbours1++;
            break;
          }
        }
      }
      LOG.trace("numNeighbours1: " + numNeighbours1);
      int numNeighbours2 = 0;
      for (int shape2BorderPoint : shape2BorderPoints) {
        for (int shape1BorderPoint : shape1BorderPoints) {
          if (Math.abs(shape1BorderPoint - shape2BorderPoint) <= verticalTolerance) {
            numNeighbours2++;
            break;
          }
        }
      }
      LOG.trace("numNeighbours2: " + numNeighbours2);
      
      LOG.trace("shape1BorderPoints: " + shape1BorderPoints.size());
      LOG.trace("shape2BorderPoints: " + shape2BorderPoints.size());
      double ratio = 0;
      if (shape1BorderPoints.size() + shape2BorderPoints.size()>0)
        ratio = ((double) numNeighbours1 + numNeighbours2) / (shape1BorderPoints.size() + shape2BorderPoints.size());
      
      result = this.generateResult(ratio);
    }
    return result;
  }

}
