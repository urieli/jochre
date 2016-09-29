package com.joliciel.jochre.boundaries;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.features.SplitFeature;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import mockit.Mocked;
import mockit.NonStrictExpectations;

public class RecursiveShapeSplitterTest {
	private static final Logger LOG = LoggerFactory.getLogger(RecursiveShapeSplitterTest.class);

	/**
	 * Make sure we get 5 equally weighted sequences in the case of a 50/50 prob
	 * for splitting each time.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSplitShape(@Mocked final SplitCandidateFinder splitCandidateFinder, @Mocked final DecisionMaker decisionMaker) throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		Config config = ConfigFactory.load();
		JochreSession jochreSession = new JochreSession(config);

		BufferedImage originalImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
		final JochreImage jochreImage = new JochreImage(originalImage, jochreSession);

		final Shape shape = new Shape(jochreImage, 0, 0, 63, 15, jochreSession);
		shape.setBaseLine(12);
		shape.setMeanLine(4);
		final Shape shape1 = new Shape(jochreImage, 0, 0, 31, 15, jochreSession);
		shape1.setBaseLine(12);
		shape1.setMeanLine(4);
		final Shape shape2 = new Shape(jochreImage, 32, 0, 63, 15, jochreSession);
		shape2.setBaseLine(12);
		shape2.setMeanLine(4);

		new NonStrictExpectations() {
			{
				Split split = new Split(shape, jochreSession);
				split.setPosition(31);
				List<Split> splits = new ArrayList<Split>();
				splits.add(split);
				splitCandidateFinder.findSplitCandidates(shape);
				returns(splits);

				Decision yesDecision = new Decision(SplitOutcome.DO_SPLIT.name(), 0.5);
				Decision noDecision = new Decision(SplitOutcome.DO_NOT_SPLIT.name(), 0.5);
				List<Decision> decisions = new ArrayList<Decision>();
				decisions.add(yesDecision);
				decisions.add(noDecision);

				decisionMaker.decide((List<FeatureResult<?>>) any);
				returns(decisions);
				maxTimes = 500;

				Split split1 = new Split(shape1, jochreSession);
				split1.setPosition(15);
				List<Split> splits1 = new ArrayList<Split>();
				splits1.add(split1);
				splitCandidateFinder.findSplitCandidates(shape1);
				returns(splits1);

				Split split2 = new Split(shape2, jochreSession);
				split2.setPosition(15);
				List<Split> splits2 = new ArrayList<Split>();
				splits2.add(split2);
				splitCandidateFinder.findSplitCandidates(shape2);
				returns(splits2);
			}
		};

		Set<SplitFeature<?>> splitFeatures = new TreeSet<SplitFeature<?>>();

		RecursiveShapeSplitter splitter = new RecursiveShapeSplitter(splitCandidateFinder, splitFeatures, decisionMaker, jochreSession);
		splitter.setBeamWidth(10);
		splitter.setMaxDepth(2);
		splitter.setMinWidthRatio(1.0);

		List<ShapeSequence> shapeSequences = splitter.split(shape);
		assertEquals(5, shapeSequences.size());

		for (ShapeSequence shapeSequence : shapeSequences) {
			assertEquals(1.0, shapeSequence.getScore(), 0.0001);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSplitShapeNoSplitMoreLikely(@Mocked final SplitCandidateFinder splitCandidateFinder, @Mocked final DecisionMaker decisionMaker)
			throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		Config config = ConfigFactory.load();
		JochreSession jochreSession = new JochreSession(config);

		BufferedImage originalImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
		final JochreImage jochreImage = new JochreImage(originalImage, jochreSession);

		final Shape shape = new Shape(jochreImage, 0, 0, 63, 15, jochreSession);
		shape.setBaseLine(12);
		shape.setMeanLine(4);
		final Shape shape1 = new Shape(jochreImage, 0, 0, 31, 15, jochreSession);
		shape1.setBaseLine(12);
		shape1.setMeanLine(4);
		final Shape shape2 = new Shape(jochreImage, 32, 0, 63, 15, jochreSession);
		shape2.setBaseLine(12);
		shape2.setMeanLine(4);

		new NonStrictExpectations() {
			{
				Split split = new Split(shape, jochreSession);
				split.setPosition(31);
				List<Split> splits = new ArrayList<Split>();
				splits.add(split);
				splitCandidateFinder.findSplitCandidates(shape);
				returns(splits);

				Decision yesDecision = new Decision(SplitOutcome.DO_SPLIT.name(), 0.4);
				Decision noDecision = new Decision(SplitOutcome.DO_NOT_SPLIT.name(), 0.6);
				List<Decision> decisions = new ArrayList<Decision>();
				decisions.add(yesDecision);
				decisions.add(noDecision);

				decisionMaker.decide((List<FeatureResult<?>>) any);
				returns(decisions);
				maxTimes = 500;

				Split split1 = new Split(shape1, jochreSession);
				split1.setPosition(15);
				List<Split> splits1 = new ArrayList<Split>();
				splits1.add(split1);
				splitCandidateFinder.findSplitCandidates(shape1);
				returns(splits1);

				Split split2 = new Split(shape2, jochreSession);
				split2.setPosition(15);
				List<Split> splits2 = new ArrayList<Split>();
				splits2.add(split2);
				splitCandidateFinder.findSplitCandidates(shape2);
				returns(splits2);
			}
		};

		Set<SplitFeature<?>> splitFeatures = new TreeSet<SplitFeature<?>>();

		RecursiveShapeSplitter splitter = new RecursiveShapeSplitter(splitCandidateFinder, splitFeatures, decisionMaker, jochreSession);

		splitter.setBeamWidth(10);
		splitter.setMaxDepth(2);
		splitter.setMinWidthRatio(1.0);

		List<ShapeSequence> shapeSequences = splitter.split(shape);
		assertEquals(5, shapeSequences.size());

		int i = 0;
		double prob = 1.0;
		double twoThirds = 0.4 / 0.6;
		LOG.debug("twoThirds: " + twoThirds);
		for (ShapeSequence shapeSequence : shapeSequences) {
			LOG.debug("sequence " + i + " decisions:");
			for (Decision decision : shapeSequence.getDecisions())
				LOG.debug("" + decision.getProbability());

			if (i == 0) {
				prob = 1.0;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(1, shapeSequence.size());
			} else if (i == 1) {
				prob = 1.0 * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(2, shapeSequence.size());
			} else if (i == 2) {
				prob = 1.0 * twoThirds * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(3, shapeSequence.size());
			} else if (i == 3) {
				prob = 1.0 * twoThirds * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(3, shapeSequence.size());
			} else if (i == 4) {
				prob = 1.0 * twoThirds * twoThirds * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(4, shapeSequence.size());
			}
			i++;
		}
	}

	/**
	 * If a split is always more likely (e.g. 60% likelihood), ensure the shape
	 * sequences are ordered correctly.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSplitShapeSplitMoreLikely(@Mocked final SplitCandidateFinder splitCandidateFinder, @Mocked final DecisionMaker decisionMaker)
			throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		Config config = ConfigFactory.load();
		JochreSession jochreSession = new JochreSession(config);

		BufferedImage originalImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
		final JochreImage jochreImage = new JochreImage(originalImage, jochreSession);

		final Shape shape = new Shape(jochreImage, 0, 0, 63, 15, jochreSession);
		shape.setBaseLine(12);
		shape.setMeanLine(4);
		final Shape shape1 = new Shape(jochreImage, 0, 0, 31, 15, jochreSession);
		shape1.setBaseLine(12);
		shape1.setMeanLine(4);
		final Shape shape2 = new Shape(jochreImage, 32, 0, 63, 15, jochreSession);
		shape2.setBaseLine(12);
		shape2.setMeanLine(4);

		new NonStrictExpectations() {
			{
				Split split = new Split(shape, jochreSession);
				split.setPosition(31);
				List<Split> splits = new ArrayList<Split>();
				splits.add(split);
				splitCandidateFinder.findSplitCandidates(shape);
				returns(splits);

				Decision yesDecision = new Decision(SplitOutcome.DO_SPLIT.name(), 0.6);
				Decision noDecision = new Decision(SplitOutcome.DO_NOT_SPLIT.name(), 0.4);
				List<Decision> decisions = new ArrayList<Decision>();
				decisions.add(yesDecision);
				decisions.add(noDecision);

				decisionMaker.decide((List<FeatureResult<?>>) any);
				returns(decisions);
				maxTimes = 500;

				Split split1 = new Split(shape1, jochreSession);
				split1.setPosition(15);
				List<Split> splits1 = new ArrayList<Split>();
				splits1.add(split1);
				splitCandidateFinder.findSplitCandidates(shape1);
				returns(splits1);

				Split split2 = new Split(shape2, jochreSession);
				split2.setPosition(15);
				List<Split> splits2 = new ArrayList<Split>();
				splits2.add(split2);
				splitCandidateFinder.findSplitCandidates(shape2);
				returns(splits2);
			}
		};

		Set<SplitFeature<?>> splitFeatures = new TreeSet<SplitFeature<?>>();

		RecursiveShapeSplitter splitter = new RecursiveShapeSplitter(splitCandidateFinder, splitFeatures, decisionMaker, jochreSession);
		splitter.setBeamWidth(10);
		splitter.setMaxDepth(2);
		splitter.setMinWidthRatio(1.0);

		List<ShapeSequence> shapeSequences = splitter.split(shape);
		assertEquals(5, shapeSequences.size());

		int i = 0;
		for (ShapeSequence shapeSequence : shapeSequences) {
			LOG.debug("sequence " + i + " shapes:");
			for (ShapeInSequence shapeInSequence : shapeSequence) {
				Shape oneShape = shapeInSequence.getShape();
				LOG.debug("Shape: " + oneShape.getLeft() + "," + oneShape.getRight());
			}
			LOG.debug("" + shapeSequence.getScore());
			i++;
		}
		i = 0;
		double prob = 1.0;
		double twoThirds = 0.4 / 0.6;
		LOG.debug("twoThirds: " + twoThirds);
		for (ShapeSequence shapeSequence : shapeSequences) {
			LOG.debug("sequence " + i + " decisions:");
			for (Decision decision : shapeSequence.getDecisions())
				LOG.debug("" + decision.getProbability());

			if (i == 0) {
				prob = 1.0;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(4, shapeSequence.size());
			} else if (i == 1) {
				prob = 1.0 * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(3, shapeSequence.size());
			} else if (i == 2) {
				prob = 1.0 * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(3, shapeSequence.size());
			} else if (i == 3) {
				prob = 1.0 * twoThirds * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(2, shapeSequence.size());
			} else if (i == 4) {
				prob = 1.0 * twoThirds * twoThirds * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(1, shapeSequence.size());
			}
			i++;
		}
	}
}
