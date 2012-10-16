package com.joliciel.jochre.boundaries;

import com.joliciel.talismane.machineLearning.AbstractDecisionFactory;

public class BoundaryDecisionFactory extends AbstractDecisionFactory<SplitMergeOutcome> {
	private static final long serialVersionUID = 474555753735292424L;

	@Override
	public SplitMergeOutcome createOutcome(String name) {
		SplitMergeOutcome outcome = null;
		try {
			outcome = MergeOutcome.valueOf(name);
		} catch (IllegalArgumentException iae) {
			outcome = SplitOutcome.valueOf(name);
		}
		return outcome;
	}

}
