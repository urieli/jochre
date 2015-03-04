package com.joliciel.jochre.boundaries;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.DataSource;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.jochre.JochreServiceLocator;
import com.joliciel.jochre.boundaries.features.SplitFeature;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;

public class RecursiveShapeSplitterTest {
	private static final Log LOG = LogFactory.getLog(RecursiveShapeSplitterTest.class);

	/**
	 * Make sure we get 5 equally weighted sequences in the case of a 50/50 prob for splitting each time.
	 * @param splitCandidateFinder
	 * @param decisionMaker
	 * @param shape
	 * @param jochreImage
	 * @param dataSource
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSplitShape(@Mocked final SplitCandidateFinder splitCandidateFinder,
			@Mocked final DecisionMaker<SplitOutcome> decisionMaker,
			@Mocked final Shape shape,
			@Mocked final JochreImage jochreImage,
			@Mocked final DataSource dataSource,
			@Mocked final Split split,
			@Mocked final Shape shape1, @Mocked final Shape shape2,
			@Mocked final Split split1, @Mocked final Split split2,
			@Mocked final Shape shape11, @Mocked final Shape shape12, @Mocked final Shape shape21, @Mocked final Shape shape22,
			@Mocked final Decision<SplitOutcome> yesDecision, @Mocked final Decision<SplitOutcome> noDecision
	) {
		JochreServiceLocator locator = JochreServiceLocator.getInstance();
		locator.setDataSource(dataSource);
		
		new NonStrictExpectations() {
			{
				shape.getWidth(); returns(64);
				shape.getXHeight(); returns(8);
				shape.getLeft(); returns(0);
				shape.getRight(); returns(63);
				shape.getTop(); returns(0);
				shape.getBottom(); returns(15);
				shape.getJochreImage(); returns(jochreImage); minTimes=0; maxTimes=500;
				
				List<Split> splits = new ArrayList<Split>();
				splits.add(split);
				split.getPosition(); returns(31); minTimes=0; maxTimes=500;
				split.getShape(); returns(shape); minTimes=0; maxTimes=500;
				splitCandidateFinder.findSplitCandidates(shape); returns(splits);
				
				yesDecision.getOutcome(); returns(SplitOutcome.DO_SPLIT);
				yesDecision.getProbability(); returns(0.5);
				noDecision.getCode(); returns(SplitOutcome.DO_NOT_SPLIT);
				noDecision.getProbability(); returns(0.5);
				
				List<Decision<SplitOutcome>> decisions = new ArrayList<Decision<SplitOutcome>>();
				decisions.add(yesDecision);
				decisions.add(noDecision);

				decisionMaker.decide((List<FeatureResult<?>>) any); returns(decisions); maxTimes=500;
				
				jochreImage.getShape(0, 0, 31, 15); returns (shape1);
				jochreImage.getShape(32, 0, 63, 15); returns (shape2);
				
				shape1.getWidth(); returns(32);
				shape1.getXHeight(); returns(8);
				shape1.getLeft(); returns(0);
				shape1.getRight(); returns(31);
				shape1.getTop(); returns(0);
				shape1.getBottom(); returns(15);
				shape1.getJochreImage(); returns(jochreImage); minTimes=0; maxTimes=500;
					
				List<Split> splits1 = new ArrayList<Split>();
				splits1.add(split1);
				split1.getPosition(); returns(15); minTimes=0; maxTimes=500;
				split1.getShape(); returns(shape1); minTimes=0; maxTimes=500;
				splitCandidateFinder.findSplitCandidates(shape1); returns(splits1);


				shape2.getWidth(); returns(32);
				shape2.getXHeight(); returns(8);
				shape2.getLeft(); returns(32);
				shape2.getRight(); returns(63);
				shape2.getTop(); returns(0);
				shape2.getBottom(); returns(15);
				shape2.getJochreImage(); returns(jochreImage);

				List<Split> splits2 = new ArrayList<Split>();
				splits2.add(split2);
				split2.getPosition(); returns(15); minTimes=0; maxTimes=500;
				split2.getShape(); returns(shape2); minTimes=0; maxTimes=500;
				splitCandidateFinder.findSplitCandidates(shape2); returns(splits2);

				jochreImage.getShape(0, 0, 15, 15); returns (shape11);
				jochreImage.getShape(16, 0, 31, 15); returns (shape12);
				jochreImage.getShape(32, 0, 47, 15); returns (shape21);
				jochreImage.getShape(48, 0, 63, 15); returns (shape22);

				shape11.getWidth(); returns(16);
				shape11.getXHeight(); returns(8);
				shape11.getLeft(); returns(0);
				shape11.getRight(); returns(15);
				shape11.getTop(); returns(0);
				shape11.getBottom(); returns(15);
				shape11.getJochreImage(); returns(jochreImage);

				shape12.getWidth(); returns(16);
				shape12.getXHeight(); returns(8);
				shape12.getLeft(); returns(16);
				shape12.getRight(); returns(31);
				shape12.getTop(); returns(0);
				shape12.getBottom(); returns(15);
				shape12.getJochreImage(); returns(jochreImage);

				shape21.getWidth(); returns(16);
				shape21.getXHeight(); returns(8);
				shape21.getLeft(); returns(32);
				shape21.getRight(); returns(47);
				shape21.getTop(); returns(0);
				shape21.getBottom(); returns(15);
				shape21.getJochreImage(); returns(jochreImage);

				shape22.getWidth(); returns(16);
				shape22.getXHeight(); returns(8);
				shape22.getLeft(); returns(48);
				shape22.getRight(); returns(63);
				shape22.getTop(); returns(0);
				shape22.getBottom(); returns(15);
				shape22.getJochreImage(); returns(jochreImage);
			}
		};
		
		Set<SplitFeature<?>> splitFeatures = new TreeSet<SplitFeature<?>>();
		BoundaryServiceInternal boundaryServiceInternal = new BoundaryServiceImpl();
		
		RecursiveShapeSplitter splitter = new RecursiveShapeSplitter(splitCandidateFinder, splitFeatures, decisionMaker);
		splitter.setBoundaryServiceInternal(boundaryServiceInternal);
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
	public void testSplitShapeNoSplitMoreLikely(@Mocked final SplitCandidateFinder splitCandidateFinder,
			@Mocked final DecisionMaker<SplitOutcome> decisionMaker,
			@Mocked final Shape shape,
			@Mocked final JochreImage jochreImage,
			@Mocked final DataSource dataSource,
			@Mocked final Split split,
			@Mocked final Shape shape1, @Mocked final Shape shape2,
			@Mocked final Split split1, @Mocked final Split split2,
			@Mocked final Shape shape11, @Mocked final Shape shape12, @Mocked final Shape shape21, @Mocked final Shape shape22,
			@Mocked final Decision<SplitOutcome> yesDecision, @Mocked final Decision<SplitOutcome> noDecision
	) {
		JochreServiceLocator locator = JochreServiceLocator.getInstance();
		locator.setDataSource(dataSource);
		
		new NonStrictExpectations() {
			{
				shape.getWidth(); returns(64);
				shape.getXHeight(); returns(8);
				shape.getLeft(); returns(0);
				shape.getRight(); returns(63);
				shape.getTop(); returns(0);
				shape.getBottom(); returns(15);
				shape.getJochreImage(); returns(jochreImage); minTimes=0; maxTimes=500;
				
				List<Split> splits = new ArrayList<Split>();
				splits.add(split);
				split.getPosition(); returns(31); minTimes=0; maxTimes=500;
				split.getShape(); returns(shape); minTimes=0; maxTimes=500;
				splitCandidateFinder.findSplitCandidates(shape); returns(splits);
				
				yesDecision.getOutcome(); returns(SplitOutcome.DO_SPLIT);
				yesDecision.getProbability(); returns(0.4);
				noDecision.getCode(); returns(SplitOutcome.DO_NOT_SPLIT);
				noDecision.getProbability(); returns(0.6);
				
				List<Decision<SplitOutcome>> decisions = new ArrayList<Decision<SplitOutcome>>();
				decisions.add(yesDecision);
				decisions.add(noDecision);
				
				decisionMaker.decide((List<FeatureResult<?>>) any); returns(decisions); maxTimes=500;
				
				jochreImage.getShape(0, 0, 31, 15); returns (shape1);
				jochreImage.getShape(32, 0, 63, 15); returns (shape2);
				
				shape1.getWidth(); returns(32);
				shape1.getXHeight(); returns(8);
				shape1.getLeft(); returns(0);
				shape1.getRight(); returns(31);
				shape1.getTop(); returns(0);
				shape1.getBottom(); returns(15);
				shape1.getJochreImage(); returns(jochreImage); minTimes=0; maxTimes=500;
					
				List<Split> splits1 = new ArrayList<Split>();
				splits1.add(split1);
				split1.getPosition(); returns(15); minTimes=0; maxTimes=500;
				split1.getShape(); returns(shape1); minTimes=0; maxTimes=500;
				splitCandidateFinder.findSplitCandidates(shape1); returns(splits1);


				shape2.getWidth(); returns(32);
				shape2.getXHeight(); returns(8);
				shape2.getLeft(); returns(32);
				shape2.getRight(); returns(63);
				shape2.getTop(); returns(0);
				shape2.getBottom(); returns(15);
				shape2.getJochreImage(); returns(jochreImage);

				List<Split> splits2 = new ArrayList<Split>();
				splits2.add(split2);
				split2.getPosition(); returns(15); minTimes=0; maxTimes=500;
				split2.getShape(); returns(shape2); minTimes=0; maxTimes=500;
				splitCandidateFinder.findSplitCandidates(shape2); returns(splits2);

				jochreImage.getShape(0, 0, 15, 15); returns (shape11);
				jochreImage.getShape(16, 0, 31, 15); returns (shape12);
				jochreImage.getShape(32, 0, 47, 15); returns (shape21);
				jochreImage.getShape(48, 0, 63, 15); returns (shape22);

				shape11.getWidth(); returns(16);
				shape11.getXHeight(); returns(8);
				shape11.getLeft(); returns(0);
				shape11.getRight(); returns(15);
				shape11.getTop(); returns(0);
				shape11.getBottom(); returns(15);
				shape11.getJochreImage(); returns(jochreImage);

				shape12.getWidth(); returns(16);
				shape12.getXHeight(); returns(8);
				shape12.getLeft(); returns(16);
				shape12.getRight(); returns(31);
				shape12.getTop(); returns(0);
				shape12.getBottom(); returns(15);
				shape12.getJochreImage(); returns(jochreImage);

				shape21.getWidth(); returns(16);
				shape21.getXHeight(); returns(8);
				shape21.getLeft(); returns(32);
				shape21.getRight(); returns(47);
				shape21.getTop(); returns(0);
				shape21.getBottom(); returns(15);
				shape21.getJochreImage(); returns(jochreImage);

				shape22.getWidth(); returns(16);
				shape22.getXHeight(); returns(8);
				shape22.getLeft(); returns(48);
				shape22.getRight(); returns(63);
				shape22.getTop(); returns(0);
				shape22.getBottom(); returns(15);
				shape22.getJochreImage(); returns(jochreImage);

			}
		};
		
		Set<SplitFeature<?>> splitFeatures = new TreeSet<SplitFeature<?>>();
		BoundaryServiceInternal boundaryServiceInternal = new BoundaryServiceImpl();
		
		RecursiveShapeSplitter splitter = new RecursiveShapeSplitter(splitCandidateFinder, splitFeatures, decisionMaker);
		splitter.setBoundaryServiceInternal(boundaryServiceInternal);
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
			for (Decision<SplitMergeOutcome> decision : shapeSequence.getDecisions())
				LOG.debug(decision.getProbability());
			
			if (i==0) {
				prob = 1.0;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(1, shapeSequence.size());
			} else if (i==1) {
				prob = 1.0 * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(2, shapeSequence.size());
			} else if (i==2) {
				prob = 1.0 * twoThirds * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(3, shapeSequence.size());
			} else if (i==3) {
				prob = 1.0 * twoThirds * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(3, shapeSequence.size());
			} else if (i==4) {
				prob = 1.0 * twoThirds * twoThirds * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(4, shapeSequence.size());
			}
			i++;
		}
	}
	
	/**
	 * If a split is always more likely (e.g. 60% likelihood),
	 * ensure the shape sequences are ordered correctly.
	 * @param splitCandidateFinder
	 * @param decisionMaker
	 * @param shape
	 * @param jochreImage
	 * @param dataSource
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSplitShapeSplitMoreLikely(@Mocked final SplitCandidateFinder splitCandidateFinder,
			@Mocked final DecisionMaker<SplitOutcome> decisionMaker,
			@Mocked final Shape shape,
			@Mocked final JochreImage jochreImage,
			@Mocked final DataSource dataSource,
			@Mocked final Split split,
			@Mocked final Shape shape1, @Mocked final Shape shape2,
			@Mocked final Split split1, @Mocked final Split split2,
			@Mocked final Shape shape11, @Mocked final Shape shape12, @Mocked final Shape shape21, @Mocked final Shape shape22,
			@Mocked final Decision<SplitOutcome> yesDecision, @Mocked final Decision<SplitOutcome> noDecision
	) {
		JochreServiceLocator locator = JochreServiceLocator.getInstance();
		locator.setDataSource(dataSource);
		
		new NonStrictExpectations() {
			{
				shape.getWidth(); returns(64);
				shape.getXHeight(); returns(8);
				shape.getLeft(); returns(0);
				shape.getRight(); returns(63);
				shape.getTop(); returns(0);
				shape.getBottom(); returns(15);
				shape.getJochreImage(); returns(jochreImage);
				
				List<Split> splits = new ArrayList<Split>();
				splits.add(split);
				split.getPosition(); returns(31); minTimes=0; maxTimes=500;
				split.getShape(); returns(shape); minTimes=0; maxTimes=500;
				splitCandidateFinder.findSplitCandidates(shape); returns(splits);
				
				yesDecision.getOutcome(); returns(SplitOutcome.DO_SPLIT);
				yesDecision.getProbability(); returns(0.6);
				noDecision.getCode(); returns(SplitOutcome.DO_NOT_SPLIT);
				noDecision.getProbability(); returns(0.4);
				
				List<Decision<SplitOutcome>> decisions = new ArrayList<Decision<SplitOutcome>>();
				decisions.add(yesDecision);
				decisions.add(noDecision);
				
				decisionMaker.decide((List<FeatureResult<?>>) any); returns(decisions); maxTimes=500;
				
				jochreImage.getShape(0, 0, 31, 15); returns (shape1);
				jochreImage.getShape(32, 0, 63, 15); returns (shape2);
				
				shape1.getWidth(); returns(32);
				shape1.getXHeight(); returns(8);
				shape1.getLeft(); returns(0);
				shape1.getRight(); returns(31);
				shape1.getTop(); returns(0);
				shape1.getBottom(); returns(15);
				shape1.getJochreImage(); returns(jochreImage);
				
				List<Split> splits1 = new ArrayList<Split>();
				splits1.add(split1);
				split1.getPosition(); returns(15); minTimes=0; maxTimes=500;
				split1.getShape(); returns(shape1); minTimes=0; maxTimes=500;
				splitCandidateFinder.findSplitCandidates(shape1); returns(splits1);


				shape2.getWidth(); returns(32);
				shape2.getXHeight(); returns(8);
				shape2.getLeft(); returns(32);
				shape2.getRight(); returns(63);
				shape2.getTop(); returns(0);
				shape2.getBottom(); returns(15);
				shape2.getJochreImage(); returns(jochreImage);

				List<Split> splits2 = new ArrayList<Split>();
				splits2.add(split2);
				split2.getPosition(); returns(15); minTimes=0; maxTimes=500;
				split2.getShape(); returns(shape2); minTimes=0; maxTimes=500;
				splitCandidateFinder.findSplitCandidates(shape2); returns(splits2);

				jochreImage.getShape(0, 0, 15, 15); returns (shape11);
				jochreImage.getShape(16, 0, 31, 15); returns (shape12);
				jochreImage.getShape(32, 0, 47, 15); returns (shape21);
				jochreImage.getShape(48, 0, 63, 15); returns (shape22);

				shape11.getWidth(); returns(16);
				shape11.getXHeight(); returns(8);
				shape11.getLeft(); returns(0);
				shape11.getRight(); returns(15);
				shape11.getTop(); returns(0);
				shape11.getBottom(); returns(15);
				shape11.getJochreImage(); returns(jochreImage);

				shape12.getWidth(); returns(16);
				shape12.getXHeight(); returns(8);
				shape12.getLeft(); returns(16);
				shape12.getRight(); returns(31);
				shape12.getTop(); returns(0);
				shape12.getBottom(); returns(15);
				shape12.getJochreImage(); returns(jochreImage);

				shape21.getWidth(); returns(16);
				shape21.getXHeight(); returns(8);
				shape21.getLeft(); returns(32);
				shape21.getRight(); returns(47);
				shape21.getTop(); returns(0);
				shape21.getBottom(); returns(15);
				shape21.getJochreImage(); returns(jochreImage);

				shape22.getWidth(); returns(16);
				shape22.getXHeight(); returns(8);
				shape22.getLeft(); returns(48);
				shape22.getRight(); returns(63);
				shape22.getTop(); returns(0);
				shape22.getBottom(); returns(15);
				shape22.getJochreImage(); returns(jochreImage);

			}
		};
		
		Set<SplitFeature<?>> splitFeatures = new TreeSet<SplitFeature<?>>();
		BoundaryServiceInternal boundaryServiceInternal = new BoundaryServiceImpl();
		
		RecursiveShapeSplitter splitter = new RecursiveShapeSplitter(splitCandidateFinder, splitFeatures, decisionMaker);
		splitter.setBoundaryServiceInternal(boundaryServiceInternal);
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
			LOG.debug(shapeSequence.getScore());
			i++;
		}
		i = 0;
		double prob = 1.0;
		double twoThirds = 0.4 / 0.6;
		LOG.debug("twoThirds: " + twoThirds);
		for (ShapeSequence shapeSequence : shapeSequences) {
			LOG.debug("sequence " + i + " decisions:");
			for (Decision<SplitMergeOutcome> decision : shapeSequence.getDecisions())
				LOG.debug(decision.getProbability());
			
			if (i==0) {
				prob = 1.0;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(4, shapeSequence.size());
			} else if (i==1) {
				prob = 1.0 * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(3, shapeSequence.size());
			} else if (i==2) {
				prob = 1.0 * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(3, shapeSequence.size());
			} else if (i==3) {
				prob = 1.0 * twoThirds * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(2, shapeSequence.size());
			} else if (i==4) {
				prob = 1.0 * twoThirds * twoThirds * twoThirds;
				assertEquals(prob, shapeSequence.getScore(), 0.0001);
				assertEquals(1, shapeSequence.size());
			}
			i++;
		}
	}
}
