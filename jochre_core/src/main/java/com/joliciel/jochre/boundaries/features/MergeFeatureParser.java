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

import com.joliciel.jochre.boundaries.ShapePair;
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

class MergeFeatureParser extends AbstractFeatureParser<ShapePair> {
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(MergeFeatureParser.class);

	public MergeFeatureParser(FeatureService featureService) {
		super(featureService);
	}
	
	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("MergedWidth", MergedWidthFeature.class);
		container.addFeatureClass("MergeDistance", MergeDistanceFeature.class);
		container.addFeatureClass("BorderlineNeighbours", BorderlineNeighboursFeature.class);
	}

	@Override
	public List<FunctionDescriptor> getModifiedDescriptors(
			FunctionDescriptor functionDescriptor) {
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<MergeFeature<?>> parseDescriptor(
			FunctionDescriptor functionDescriptor) {
		MONITOR.startTask("parseDescriptor");
		try {
			List<Feature<ShapePair, ?>> mergeFeatures = this.parse(functionDescriptor);
			List<MergeFeature<?>> wrappedFeatures = new ArrayList<MergeFeature<?>>();
			for (Feature<ShapePair, ?> mergeFeature : mergeFeatures) {
				MergeFeature<?> wrappedFeature = null;
				if (mergeFeature instanceof MergeFeature) {
					wrappedFeature = (MergeFeature<?>) mergeFeature;
				} else if (mergeFeature instanceof BooleanFeature) {
					wrappedFeature = new MergeBooleanFeatureWrapper((Feature<ShapePair, Boolean>) mergeFeature);
				} else if (mergeFeature instanceof StringFeature) {
					wrappedFeature = new MergeStringFeatureWrapper((Feature<ShapePair, String>) mergeFeature);
				} else if (mergeFeature instanceof IntegerFeature) {
					wrappedFeature = new MergeIntegerFeatureWrapper((Feature<ShapePair, Integer>) mergeFeature);
				} else if (mergeFeature instanceof DoubleFeature) {
					wrappedFeature = new MergeDoubleFeatureWrapper((Feature<ShapePair, Double>) mergeFeature);
				} else {
					wrappedFeature = new MergeFeatureWrapper(mergeFeature);
				}
				wrappedFeatures.add(wrappedFeature);
			}
			return wrappedFeatures;
		} finally {
			MONITOR.endTask();
		}
	}

	private static class MergeFeatureWrapper<T> extends AbstractFeature<ShapePair,T> implements
		MergeFeature<T>, FeatureWrapper<ShapePair, T> {
		private Feature<ShapePair,T> wrappedFeature = null;
		
		public MergeFeatureWrapper(
				Feature<ShapePair, T> wrappedFeature) {
			super();
			this.wrappedFeature = wrappedFeature;
			this.setName(wrappedFeature.getName());
		}
		
		@Override
		public FeatureResult<T> check(ShapePair context, RuntimeEnvironment env) {
			return wrappedFeature.check(context, env);
		}
		
		@Override
		public Feature<ShapePair, T> getWrappedFeature() {
			return this.wrappedFeature;
		}
	
		@SuppressWarnings("rawtypes")
		@Override
		public Class<? extends Feature> getFeatureType() {
			return wrappedFeature.getFeatureType();
		}
	}
	
	private class MergeBooleanFeatureWrapper extends MergeFeatureWrapper<Boolean> implements BooleanFeature<ShapePair> {
		public MergeBooleanFeatureWrapper(
				Feature<ShapePair, Boolean> wrappedFeature) {
			super(wrappedFeature);
		}
	}
	
	private class MergeStringFeatureWrapper extends MergeFeatureWrapper<String> implements StringFeature<ShapePair> {
		public MergeStringFeatureWrapper(
				Feature<ShapePair, String> wrappedFeature) {
			super(wrappedFeature);
		}
	}
	
	private class MergeDoubleFeatureWrapper extends MergeFeatureWrapper<Double> implements DoubleFeature<ShapePair> {
		public MergeDoubleFeatureWrapper(
				Feature<ShapePair, Double> wrappedFeature) {
			super(wrappedFeature);
		}
	}
	
	private class MergeIntegerFeatureWrapper extends MergeFeatureWrapper<Integer> implements IntegerFeature<ShapePair> {
		public MergeIntegerFeatureWrapper(
				Feature<ShapePair, Integer> wrappedFeature) {
			super(wrappedFeature);
		}
	}
	
	@Override
	public void injectDependencies(@SuppressWarnings("rawtypes") Feature feature) {
		// no dependencies to inject
	}

	@Override
	protected boolean canConvert(Class<?> parameterType,
			Class<?> originalArgumentType) {
		return false;
	}

	@Override
	protected Feature<ShapePair, ?> convertArgument(Class<?> parameterType,
			Feature<ShapePair, ?> originalArgument) {
		return null;
	}

	@Override
	public Feature<ShapePair, ?> convertFeatureCustomType(
			Feature<ShapePair, ?> feature) {
		return null;
	}
}
