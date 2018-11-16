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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.graphics.JochreCorpusShapeReader;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.stats.FScoreCalculator;
import com.typesafe.config.Config;

/**
 * For evaluating a split model.
 * 
 * @author Assaf Urieli
 *
 */
public class SplitEvaluator {
  private static final Logger LOG = LoggerFactory.getLogger(SplitEvaluator.class);
  int tolerance;
  double minWidthRatio;
  double minHeightRatio;
  double minProbabilityForDecision;

  private final JochreSession jochreSession;

  public SplitEvaluator(JochreSession jochreSession) {
    this.jochreSession = jochreSession;

    Config splitterConfig = jochreSession.getConfig().getConfig("jochre.boundaries.splitter");
    minWidthRatio = splitterConfig.getDouble("min-width-ratio");
    minHeightRatio = splitterConfig.getDouble("min-height-ratio");
    tolerance = splitterConfig.getInt("evaluation-tolerance");

    Config boundaryConfig = jochreSession.getConfig().getConfig("jochre.boundaries");
    minProbabilityForDecision = boundaryConfig.getDouble("min-prob-for-decision");
  }

  public FScoreCalculator<String> evaluate(JochreCorpusShapeReader shapeReader, ShapeSplitter shapeSplitter) {
    FScoreCalculator<String> fScoreCalculator = new FScoreCalculator<String>();
    while (shapeReader.hasNext()) {
      Shape shape = shapeReader.next();
      // check if shape is wide enough to bother with
      double widthRatio = (double) shape.getWidth() / (double) shape.getXHeight();
      double heightRatio = (double) shape.getHeight() / (double) shape.getXHeight();

      if (widthRatio >= minWidthRatio || shape.getSplits().size() > 0) {
        LOG.debug("Testing " + shape);

        List<Split> guessedSplits = new ArrayList<Split>();

        if (widthRatio >= minWidthRatio && heightRatio >= minHeightRatio) {
          List<ShapeSequence> shapeSequences = shapeSplitter.split(shape);
          ShapeSequence splitShapes = shapeSequences.get(0);
          if (splitShapes.getScore() > minProbabilityForDecision) {
            for (ShapeInSequence splitShapeInSequence : splitShapes) {
              Shape splitShape = splitShapeInSequence.getShape();
              if (splitShape.getRight() != shape.getRight()) {
                Split guessedSplit = new Split(shape, jochreSession);
                guessedSplit.setPosition(splitShape.getRight() - shape.getLeft());
                guessedSplits.add(guessedSplit);
              }
            }
          }
        } else {
          LOG.debug("Insufficient width or height");
          LOG.debug("widthRatio: " + widthRatio);
          LOG.debug("heightRatio: " + heightRatio);
        }

        Set<Split> splitsNotFound = new HashSet<Split>();
        Set<Split> wrongSplitGuesses = new HashSet<Split>(guessedSplits);

        Set<Split> remainingSplitGuesses = new HashSet<Split>(guessedSplits);

        if (shape.getSplits().size() > 0) {
          for (Split split : shape.getSplits()) {
            LOG.debug("true split: " + split + ", right=" + (shape.getLeft() + split.getPosition()));
            boolean foundSplit = false;
            for (Split splitGuess : remainingSplitGuesses) {
              int diff = split.getPosition() - splitGuess.getPosition();
              if (diff < 0)
                diff = 0 - diff;
              if (diff <= tolerance) {
                LOG.debug("Found split: " + splitGuess);
                fScoreCalculator.increment("YES", "YES");

                wrongSplitGuesses.remove(splitGuess);
                foundSplit = true;
                break;
              }
            }
            if (!foundSplit)
              splitsNotFound.add(split);
            remainingSplitGuesses = wrongSplitGuesses;
          }

          for (Split split : splitsNotFound) {
            LOG.debug("Didn't find split: " + split);
            if (widthRatio >= minWidthRatio)
              fScoreCalculator.increment("YES", "NO");
            else
              fScoreCalculator.increment("YES", "NARROW");
          }
          for (Split guess : wrongSplitGuesses) {
            LOG.debug("Bad guess: " + guess);
            fScoreCalculator.increment("NO", "YES");
          }
        } else {
          if (wrongSplitGuesses.size() == 0) {
            fScoreCalculator.increment("NO", "NO");
          } else {
            for (Split guess : wrongSplitGuesses) {
              LOG.debug("Bad guess: " + guess);
              fScoreCalculator.increment("NO", "YES");
            }
          }
        }
      }

    }
    return fScoreCalculator;
  }

  /**
   * The tolerance of distance between a guessed split and a real split to
   * assume we got the right answer.
   */
  public int getTolerance() {
    return tolerance;
  }

  public void setTolerance(int tolerance) {
    this.tolerance = tolerance;
  }

  /**
   * The minimum ratio between the shape's width and it's x-height for the shape
   * to even be considered for splitting.
   */
  public double getMinWidthRatio() {
    return minWidthRatio;
  }

  public void setMinWidthRatio(double minWidthRatio) {
    this.minWidthRatio = minWidthRatio;
  }

  public double getMinHeightRatio() {
    return minHeightRatio;
  }

  public void setMinHeightRatio(double minHeightRatio) {
    this.minHeightRatio = minHeightRatio;
  }

  /**
   * The minimum probability to take a split decision. Default: 0.5.
   */
  public double getMinProbabilityForDecision() {
    return minProbabilityForDecision;
  }

  public void setMinProbabilityForDecision(double minProbabilityForDecision) {
    this.minProbabilityForDecision = minProbabilityForDecision;
  }
}
