package com.joliciel.jochre.boundaries.features;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;

public class BoundaryFeatureServiceImpl implements BoundaryFeatureService {
	private static final Logger LOG = LoggerFactory.getLogger(BoundaryFeatureServiceImpl.class);

	@Override
	public Set<MergeFeature<?>> getMergeFeatureSet(List<String> featureDescriptors) {
		Set<MergeFeature<?>> features = new TreeSet<MergeFeature<?>>();
		FunctionDescriptorParser descriptorParser = new FunctionDescriptorParser();
		MergeFeatureParser mergeFeatureParser = this.getMergeFeatureParser();

		for (String featureDescriptor : featureDescriptors) {
			LOG.trace(featureDescriptor);
			if (featureDescriptor.length() > 0 && !featureDescriptor.startsWith("#")) {
				FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
				List<MergeFeature<?>> myFeatures = mergeFeatureParser.parseDescriptor(functionDescriptor);
				features.addAll(myFeatures);

			}
		}
		return features;
	}

	private MergeFeatureParser getMergeFeatureParser() {
		MergeFeatureParser mergeFeatureParser = new MergeFeatureParser();
		return mergeFeatureParser;
	}

	@Override
	public Set<SplitFeature<?>> getSplitFeatureSet(List<String> featureDescriptors) {
		Set<SplitFeature<?>> features = new TreeSet<SplitFeature<?>>();
		FunctionDescriptorParser descriptorParser = new FunctionDescriptorParser();
		SplitFeatureParser mergeFeatureParser = this.getSplitFeatureParser();

		for (String featureDescriptor : featureDescriptors) {
			LOG.trace(featureDescriptor);
			if (featureDescriptor.length() > 0 && !featureDescriptor.startsWith("#")) {
				FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
				List<SplitFeature<?>> myFeatures = mergeFeatureParser.parseDescriptor(functionDescriptor);
				features.addAll(myFeatures);

			}
		}
		return features;
	}

	private SplitFeatureParser getSplitFeatureParser() {
		SplitFeatureParser mergeFeatureParser = new SplitFeatureParser();
		return mergeFeatureParser;
	}

	@Override
	public ShapeInSequenceFeatureParser getShapeInSequenceFeatureParser() {
		ShapeInSequenceFeatureParserImpl parser = new ShapeInSequenceFeatureParserImpl();
		return parser;
	}

}
