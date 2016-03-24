package com.joliciel.jochre.boundaries.features;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptorParser;

public class BoundaryFeatureServiceImpl implements BoundaryFeatureService {
	private static final Log LOG = LogFactory.getLog(BoundaryFeatureServiceImpl.class);
	FeatureService featureService;
	
	@Override
	public Set<MergeFeature<?>> getMergeFeatureSet(
			List<String> featureDescriptors) {
		Set<MergeFeature<?>> features = new TreeSet<MergeFeature<?>>();
		FunctionDescriptorParser descriptorParser = this.getFeatureService().getFunctionDescriptorParser();
		MergeFeatureParser mergeFeatureParser = this.getMergeFeatureParser();
		
		for (String featureDescriptor : featureDescriptors) {
			LOG.trace(featureDescriptor);
			if (featureDescriptor.length()>0 && !featureDescriptor.startsWith("#")) {
				FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
				List<MergeFeature<?>> myFeatures = mergeFeatureParser.parseDescriptor(functionDescriptor);
				features.addAll(myFeatures);
				
			}
		}
		return features;
	}

	private MergeFeatureParser getMergeFeatureParser() {
		MergeFeatureParser mergeFeatureParser = new MergeFeatureParser(this.getFeatureService());
		return mergeFeatureParser;
	}
	
	@Override
	public Set<SplitFeature<?>> getSplitFeatureSet(
			List<String> featureDescriptors) {
		Set<SplitFeature<?>> features = new TreeSet<SplitFeature<?>>();
		FunctionDescriptorParser descriptorParser = this.getFeatureService().getFunctionDescriptorParser();
		SplitFeatureParser mergeFeatureParser = this.getSplitFeatureParser();
		
		for (String featureDescriptor : featureDescriptors) {
			LOG.trace(featureDescriptor);
			if (featureDescriptor.length()>0 && !featureDescriptor.startsWith("#")) {
				FunctionDescriptor functionDescriptor = descriptorParser.parseDescriptor(featureDescriptor);
				List<SplitFeature<?>> myFeatures = mergeFeatureParser.parseDescriptor(functionDescriptor);
				features.addAll(myFeatures);
				
			}
		}
		return features;
	}

	private SplitFeatureParser getSplitFeatureParser() {
		SplitFeatureParser mergeFeatureParser = new SplitFeatureParser(this.getFeatureService());
		return mergeFeatureParser;
	}

	@Override
	public ShapeInSequenceFeatureParser getShapeInSequenceFeatureParser() {
		ShapeInSequenceFeatureParserImpl parser = new ShapeInSequenceFeatureParserImpl(this.getFeatureService());
		return parser;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

}
