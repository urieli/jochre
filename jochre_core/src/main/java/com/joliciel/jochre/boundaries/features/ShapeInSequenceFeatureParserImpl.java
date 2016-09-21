package com.joliciel.jochre.boundaries.features;

import java.util.List;

import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.talismane.machineLearning.features.AbstractFeatureParser;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;

class ShapeInSequenceFeatureParserImpl extends AbstractFeatureParser<ShapeInSequence> implements ShapeInSequenceFeatureParser {
	FeatureClassContainer container;

	public ShapeInSequenceFeatureParserImpl() {
		super();
	}

	@Override
	public void addFeatureClasses(FeatureClassContainer container) {
		container.addFeatureClass("LastShapeInRow", LastShapeInRowFeature.class);
		container.addFeatureClass("LastShapeInSequence", LastShapeInSequenceFeature.class);
		container.addFeatureClass("ShapeIndex", ShapeIndexFeature.class);
		container.addFeatureClass("ShapeReverseIndex", ShapeReverseIndexFeature.class);

		this.container = container;
	}

	@Override
	public List<FunctionDescriptor> getModifiedDescriptors(FunctionDescriptor functionDescriptor) {
		return null;
	}

	@Override
	public void injectDependencies(@SuppressWarnings("rawtypes") Feature feature) {
		// no dependencies to inject
	}

	@Override
	protected boolean canConvert(Class<?> parameterType, Class<?> originalArgumentType) {
		return false;
	}

	@Override
	protected Feature<ShapeInSequence, ?> convertArgument(Class<?> parameterType, Feature<ShapeInSequence, ?> originalArgument) {
		return null;
	}

	@Override
	public Feature<ShapeInSequence, ?> convertFeatureCustomType(Feature<ShapeInSequence, ?> feature) {
		return null;
	}

}
