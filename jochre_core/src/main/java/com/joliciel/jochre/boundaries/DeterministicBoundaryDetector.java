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
package com.joliciel.jochre.boundaries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.features.MergeFeature;
import com.joliciel.jochre.boundaries.features.MergeFeatureParser;
import com.joliciel.jochre.boundaries.features.SplitFeature;
import com.joliciel.jochre.boundaries.features.SplitFeatureParser;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.Decision;
import com.typesafe.config.Config;

/**
 * Returns a single "most likely" shape boundary guess, regardless of the
 * original boundaries.
 * 
 * @author Assaf Urieli
 *
 */
public class DeterministicBoundaryDetector implements BoundaryDetector {
  private final ShapeSplitter shapeSplitter;
  private final ShapeMerger shapeMerger;
  private double minWidthRatioForSplit;
  private double minHeightRatioForSplit;
  private double maxWidthRatioForMerge;
  private double maxDistanceRatioForMerge;
  private double minProbabilityForDecision;

  private void configure(JochreSession jochreSession) {
    Config splitterConfig = jochreSession.getConfig().getConfig("jochre.boundaries.splitter");
    minWidthRatioForSplit = splitterConfig.getDouble("min-width-ratio");
    minHeightRatioForSplit = splitterConfig.getDouble("min-height-ratio");

    Config mergerConfig = jochreSession.getConfig().getConfig("jochre.boundaries.merger");
    maxWidthRatioForMerge = mergerConfig.getDouble("max-width-ratio");
    maxDistanceRatioForMerge = mergerConfig.getDouble("max-distance-ratio");

    Config boundaryConfig = jochreSession.getConfig().getConfig("jochre.boundaries");
    minProbabilityForDecision = boundaryConfig.getDouble("min-prob-for-decision");
  }

  public DeterministicBoundaryDetector(ShapeSplitter shapeSplitter, ShapeMerger shapeMerger, JochreSession jochreSession) {
    this.shapeSplitter = shapeSplitter;
    this.shapeMerger = shapeMerger;
    this.configure(jochreSession);
  }

  public DeterministicBoundaryDetector(ClassificationModel splitModel, ClassificationModel mergeModel, JochreSession jochreSession) throws IOException {
    SplitCandidateFinder splitCandidateFinder = new SplitCandidateFinder(jochreSession);

    List<String> splitFeatureDescriptors = splitModel.getFeatureDescriptors();
    SplitFeatureParser splitFeatureParser = new SplitFeatureParser();
    Set<SplitFeature<?>> splitFeatures = splitFeatureParser.getSplitFeatureSet(splitFeatureDescriptors);
    ShapeSplitter shapeSplitter = new RecursiveShapeSplitter(splitCandidateFinder, splitFeatures, splitModel.getDecisionMaker(), jochreSession);

    List<String> mergeFeatureDescriptors = mergeModel.getFeatureDescriptors();
    MergeFeatureParser mergeFeatureParser = new MergeFeatureParser();
    Set<MergeFeature<?>> mergeFeatures = mergeFeatureParser.getMergeFeatureSet(mergeFeatureDescriptors);
    ShapeMerger shapeMerger = new ShapeMerger(mergeFeatures, mergeModel.getDecisionMaker());

    this.shapeSplitter = shapeSplitter;
    this.shapeMerger = shapeMerger;

    this.configure(jochreSession);
  }

  @Override
  public List<ShapeSequence> findBoundaries(GroupOfShapes group) {
    // find the possible shape sequences that make up this group
    ShapeSequence bestSequence = new ShapeSequence();
    for (Shape shape : group.getShapes()) {
      // check if shape is wide enough to bother with
      double widthRatio = (double) shape.getWidth() / (double) shape.getXHeight();
      double heightRatio = (double) shape.getHeight() / (double) shape.getXHeight();

      // Splitting/merging shapes as required
      ShapeSequence bestSplitSequence = null;
      if (this.shapeSplitter != null && widthRatio >= minWidthRatioForSplit && heightRatio >= minHeightRatioForSplit) {
        List<ShapeSequence> splitSequences = shapeSplitter.split(shape);
        double bestProb = 0;
        for (ShapeSequence splitSequence : splitSequences) {
          if (splitSequence.getScore() > bestProb) {
            bestSplitSequence = splitSequence;
            bestProb = splitSequence.getScore();
          }
        }
        if (bestProb < minProbabilityForDecision) {
          // create a sequence containing only this shape
          ShapeSequence singleShapeSequence = new ShapeSequence();
          singleShapeSequence.addShape(shape);
          bestSplitSequence = singleShapeSequence;
        }
      } else {
        // create a sequence containing only this shape
        ShapeSequence singleShapeSequence = new ShapeSequence();
        singleShapeSequence.addShape(shape);
        bestSplitSequence = singleShapeSequence;
      }

      ShapeInSequence previousShapeInSequence = null;
      Shape previousShape = null;
      if (bestSequence.size() > 0) {
        previousShapeInSequence = bestSequence.get(bestSequence.size() - 1);
        previousShape = previousShapeInSequence.getShape();
      }

      ShapeInSequence firstShapeInSequence = bestSplitSequence.get(0);
      Shape firstShape = firstShapeInSequence.getShape();

      double mergeProb = 0;
      if (this.shapeMerger != null && previousShape != null) {
        ShapePair mergeCandidate = new ShapePair(previousShape, shape);
        double mergeCandidateWidthRatio = 0;
        double mergeCandidateDistanceRatio = 0;

        mergeCandidateWidthRatio = (double) mergeCandidate.getWidth() / (double) mergeCandidate.getXHeight();
        mergeCandidateDistanceRatio = (double) mergeCandidate.getInnerDistance() / (double) mergeCandidate.getXHeight();

        if (mergeCandidateWidthRatio <= maxWidthRatioForMerge && mergeCandidateDistanceRatio <= maxDistanceRatioForMerge) {
          mergeProb = shapeMerger.checkMerge(previousShape, firstShape);
        }
      }
      if (mergeProb > minProbabilityForDecision) {
        Shape mergedShape = shapeMerger.merge(previousShape, firstShape);
        bestSequence.remove(bestSequence.size() - 1);

        List<Shape> originalShapesForMerge = new ArrayList<Shape>();
        originalShapesForMerge.addAll(previousShapeInSequence.getOriginalShapes());
        originalShapesForMerge.addAll(firstShapeInSequence.getOriginalShapes());
        bestSequence.addShape(mergedShape, originalShapesForMerge);
        boolean isFirstShape = true;
        for (ShapeInSequence splitShape : bestSplitSequence) {
          if (!isFirstShape)
            bestSequence.add(splitShape);
          isFirstShape = false;
        }

        Decision mergeDecision = new Decision(MergeOutcome.DO_MERGE.name(), mergeProb);
        bestSequence.addDecision(mergeDecision);
        for (Decision splitDecision : bestSplitSequence.getDecisions())
          bestSequence.addDecision(splitDecision);
      } else {
        if (mergeProb > 0) {
          Decision mergeDecision = new Decision(MergeOutcome.DO_NOT_MERGE.name(), 1 - mergeProb);
          bestSequence.addDecision(mergeDecision);
        }
        for (Decision splitDecision : bestSplitSequence.getDecisions())
          bestSequence.addDecision(splitDecision);

        for (ShapeInSequence splitShape : bestSplitSequence) {
          bestSequence.add(splitShape);
        }
      }
    } // next shape in group

    List<ShapeSequence> result = new ArrayList<ShapeSequence>();
    result.add(bestSequence);

    return result;
  }

  public ShapeSplitter getShapeSplitter() {
    return shapeSplitter;
  }

  public ShapeMerger getShapeMerger() {
    return shapeMerger;
  }

  public double getMinProbabilityForDecision() {
    return minProbabilityForDecision;
  }

}
