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
package com.joliciel.jochre.stats;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.stat.descriptive.moment.Mean;

/**
 * A mean absolute deviation to replace the standard deviation where outliers are a problem.
 * @author Assaf Urieli
 *
 */
public class MeanAbsoluteDeviation {
	List<Double> values = new ArrayList<Double>();
	double meanAbsoluteDeviation = Double.NaN;
	boolean dirty = false;
	
	public void clear() {
		values.clear();
		meanAbsoluteDeviation = Double.NaN;
		dirty = false;
	}
	
	public void increment(double value) {
		values.add(value);
		dirty = true;
	}
	
	public double getResult() {
		if (dirty) {
			Mean mean = new Mean();
			for (double value : values)
				mean.increment(value);
			double meanResult = mean.getResult();
			
			Mean deviationMean = new Mean();
			for (double value : values) {
				double deviation = value - meanResult;
				if (deviation < 0) deviation = 0 - deviation;
				deviationMean.increment(deviation);
			}
			
			meanAbsoluteDeviation = deviationMean.getResult();
			
			dirty = false;
		}
		return meanAbsoluteDeviation;
	}
}
