package com.joliciel.jochre.boundaries.features;

import java.util.List;

import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.talismane.utils.features.AbstractFeatureParser;
import com.joliciel.talismane.utils.features.FeatureClassContainer;
import com.joliciel.talismane.utils.features.FeatureService;
import com.joliciel.talismane.utils.features.FunctionDescriptor;

class ShapeInSequenceFeatureParserImpl extends AbstractFeatureParser<ShapeInSequence>
		implements ShapeInSequenceFeatureParser {
	FeatureClassContainer container;
	
	public ShapeInSequenceFeatureParserImpl(FeatureService featureService) {
		super(featureService);
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
	protected Object parseArgument(FunctionDescriptor argumentDescriptor) {
		return null;
	}


	@Override
	public List<FunctionDescriptor> getModifiedDescriptors(
			FunctionDescriptor functionDescriptor) {
		return null;
	}


}
