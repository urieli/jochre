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

import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.ShapeWrapper;
import com.joliciel.jochre.graphics.Shape.SectionBrightnessMeasurementMethod;

/**
 * Checks brightness of a set of sectors compared to the total.
 * @author Assaf Urieli
 *
 */
public class SectionBrightnessRatioFeature extends AbstractShapeFeature<Double> implements DoubleFeature<ShapeWrapper> {
    private static final Logger LOG = LoggerFactory.getLogger(SectionBrightnessRatioFeature.class);
  private boolean[][] testSectors;
    
  public SectionBrightnessRatioFeature(boolean[][]testSectors) {
    this.testSectors = testSectors;
    
    String name = super.getName();
    name += "{";
    for (int i = 0; i < testSectors.length; i++)
      for (int j = 0; j<testSectors[0].length; j++)
        if (testSectors[i][j])
          name += "[" + i + "," + j + "]";
    name += "}";
    this.setName(name);
  }
  
  @Override
  public FeatureResult<Double> checkInternal(ShapeWrapper shapeWrapper, RuntimeEnvironment env) {
    Shape shape = shapeWrapper.getShape();
    double[][] totals = shape.getBrightnessBySection(5, 5, 1, SectionBrightnessMeasurementMethod.RAW);
    
    double testTotal = 0;
    double fullTotal = 0;
    
    for (int i = 0; i < totals.length; i++) {
      for (int j = 0; j<totals[0].length; j++) {
        double brightness = totals[i][j];
        if (testSectors[i][j])
          testTotal += brightness;
        fullTotal += brightness;
      }
    }

    double ratio = 0;
    if (fullTotal > 0) {
      ratio = testTotal / fullTotal;
    }
    
    if (LOG.isDebugEnabled())
      LOG.trace("Test: " + testTotal + "), Total: " + fullTotal + ", Ratio: " + ratio);
    
    FeatureResult<Double> outcome = this.generateResult(ratio);
    return outcome;    
  }

  public boolean[][] getTestSectors() {
    return testSectors;
  }

  protected void setTestSectors(boolean[][] testSectors) {
    this.testSectors = testSectors;
  }
}
