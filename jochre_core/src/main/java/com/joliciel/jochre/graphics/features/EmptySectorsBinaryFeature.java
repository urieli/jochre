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
package com.joliciel.jochre.graphics.features;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.math.stat.descriptive.moment.Mean;

import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;
import com.joliciel.jochre.graphics.Shape.SectionBrightnessMeasurementMethod;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

/**
 * Check whether a set of brightness sectors is relatively empty.
 * @author Assaf Urieli
 *
 */
public class EmptySectorsBinaryFeature extends AbstractShapeFeature<Boolean> implements BooleanFeature<ShapeWrapper> {
    private static final Logger LOG = LoggerFactory.getLogger(EmptySectorsBinaryFeature.class);
  private static final double THRESHOLD = 3.0;
  private boolean[][] testSectors;

  public EmptySectorsBinaryFeature(boolean[][]testSectors) {
    this.testSectors = testSectors;
    String name = null;
    name = super.getName();
    name += "{";
    for (int i = 0; i < testSectors.length; i++)
      for (int j = 0; j<testSectors[0].length; j++)
        if (testSectors[i][j])
          name += "[" + i + "," + j + "]";
    name += "}";
    this.setName(name);
  }
  
  @Override
  public FeatureResult<Boolean> checkInternal(ShapeWrapper shapeWrapper, RuntimeEnvironment env) {
    Shape shape = shapeWrapper.getShape();
    double[][] totals = shape.getBrightnessBySection(5, 5, 1, SectionBrightnessMeasurementMethod.RAW);
    
    Mean testMean = new Mean();
    Mean otherMean = new Mean();
    
    for (int i = 0; i < totals.length; i++) {
      for (int j = 0; j<totals[0].length; j++) {
        double brightness = totals[i][j];
        if (testSectors[i][j])
          testMean.increment(brightness);
        else if (brightness>shape.getBrightnessMeanBySection(5, 5, 1, SectionBrightnessMeasurementMethod.RAW))
          otherMean.increment(brightness);
      }
    }
    
    double testMeanValue = testMean.getResult();
    double otherMeanValue = otherMean.getResult();
    
    if (LOG.isDebugEnabled())
      LOG.trace("Test mean: " + testMeanValue + " (* threshold = " + testMeanValue * THRESHOLD + "), Other mean: " + otherMeanValue);
    
    boolean result = (testMeanValue * THRESHOLD < otherMeanValue);
    FeatureResult<Boolean> outcome = this.generateResult(result);
    return outcome;
  }

  public boolean[][] getTestSectors() {
    return testSectors;
  }
}
