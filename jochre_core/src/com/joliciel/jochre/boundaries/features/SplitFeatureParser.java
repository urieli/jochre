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

import java.util.ArrayList;
import java.util.List;

import com.joliciel.jochre.boundaries.Split;
import com.joliciel.talismane.machineLearning.features.AbstractFeature;
import com.joliciel.talismane.machineLearning.features.AbstractFeatureParser;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FeatureWrapper;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.utils.PerformanceMonitor;

class SplitFeatureParser extends AbstractFeatureParser<Split> {
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(SplitFeatureParser.class);

	public SplitFeatureParser(FeatureService featureService) {
		super(featureService);
	}
	
	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("BridgeWidth", BridgeWidthFeature.class);
		container.addFeatureClass("SplitShapeWeightRatio", SplitShapeWeightRatioFeature.class);
		container.addFeatureClass("SplitShapeWidthRatio", SplitShapeWidthRatioFeature.class);
		container.addFeatureClass("TrueContourSlopeDifference", TrueContourSlopeDifferenceFeature.class);
		container.addFeatureClass("TwoPointSlopeDifference", TwoPointSlopeDifferenceFeature.class);
		container.addFeatureClass("SlopeDifference", SlopeDifferenceFeature.class);
	}

	@Override
	public List<FunctionDescriptor> getModifiedDescriptors(
			FunctionDescriptor functionDescriptor) {
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<SplitFeature<?>> parseDescriptor(
			FunctionDescriptor functionDescriptor) {
		MONITOR.startTask("parseDescriptor");
		try {
			List<Feature<Split, ?>> mergeFeatures = this.parse(functionDescriptor);
			List<SplitFeature<?>> wrappedFeatures = new ArrayList<SplitFeature<?>>();
			for (Feature<Split, ?> mergeFeature : mergeFeatures) {
				SplitFeature<?> wrappedFeature = null;
				if (mergeFeature instanceof SplitFeature) {
					wrappedFeature = (SplitFeature<?>) mergeFeature;
				} else if (mergeFeature instanceof BooleanFeature) {
					wrappedFeature = new SplitBooleanFeatureWrapper((Feature<Split, Boolean>) mergeFeature);
				} else if (mergeFeature instanceof StringFeature) {
					wrappedFeature = new SplitStringFeatureWrapper((Feature<Split, String>) mergeFeature);
				} else if (mergeFeature instanceof IntegerFeature) {
					wrappedFeature = new SplitIntegerFeatureWrapper((Feature<Split, Integer>) mergeFeature);
				} else if (mergeFeature instanceof DoubleFeature) {
					wrappedFeature = new SplitDoubleFeatureWrapper((Feature<Split, Double>) mergeFeature);
				} else {
					wrappedFeature = new SplitFeatureWrapper(mergeFeature);
				}
				wrappedFeatures.add(wrappedFeature);
			}
			return wrappedFeatures;
		} finally {
			MONITOR.endTask("parseDescriptor");
		}
	}

	private static class SplitFeatureWrapper<T> extends AbstractFeature<Split,T> implements
		SplitFeature<T>, FeatureWrapper<Split, T> {
		private Feature<Split,T> wrappedFeature = null;
		
		public SplitFeatureWrapper(
				Feature<Split, T> wrappedFeature) {
			super();
			this.wrappedFeature = wrappedFeature;
			this.setName(wrappedFeature.getName());
		}
		
		@Override
		public FeatureResult<T> check(Split context, RuntimeEnvironment env) {
			return wrappedFeature.check(context, env);
		}
		
		@Override
		public Feature<Split, T> getWrappedFeature() {
			return this.wrappedFeature;
		}
	
		@SuppressWarnings("rawtypes")
		@Override
		public Class<? extends Feature> getFeatureType() {
			return wrappedFeature.getFeatureType();
		}
	}
	
	private class SplitBooleanFeatureWrapper extends SplitFeatureWrapper<Boolean> implements BooleanFeature<Split> {
		public SplitBooleanFeatureWrapper(
				Feature<Split, Boolean> wrappedFeature) {
			super(wrappedFeature);
		}
	}
	
	private class SplitStringFeatureWrapper extends SplitFeatureWrapper<String> implements StringFeature<Split> {
		public SplitStringFeatureWrapper(
				Feature<Split, String> wrappedFeature) {
			super(wrappedFeature);
		}
	}
	
	private class SplitDoubleFeatureWrapper extends SplitFeatureWrapper<Double> implements DoubleFeature<Split> {
		public SplitDoubleFeatureWrapper(
				Feature<Split, Double> wrappedFeature) {
			super(wrappedFeature);
		}
	}
	
	private class SplitIntegerFeatureWrapper extends SplitFeatureWrapper<Integer> implements IntegerFeature<Split> {
		public SplitIntegerFeatureWrapper(
				Feature<Split, Integer> wrappedFeature) {
			super(wrappedFeature);
		}
	}

	@Override
	protected void injectDependencies(@SuppressWarnings("rawtypes") Feature feature) {
		// no dependencies to inject
	}

	@Override
	protected boolean canConvert(Class<?> parameterType,
			Class<?> originalArgumentType) {
		return false;
	}

	@Override
	protected Feature<Split, ?> convertArgument(Class<?> parameterType,
			Feature<Split, ?> originalArgument) {
		return null;
	}
}
