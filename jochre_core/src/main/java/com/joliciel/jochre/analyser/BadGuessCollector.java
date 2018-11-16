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
package com.joliciel.jochre.analyser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.graphics.GraphicsDao;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.letterGuesser.LetterSequence;

public class BadGuessCollector implements LetterGuessObserver {
  private static final Logger LOG = LoggerFactory.getLogger(BadGuessCollector.class);
  private String[] outcomesToAnalyse = new String[0];
  List<String> outcomesToAnalyseList = new ArrayList<String>();
  Map<Integer, String> shapeIdsToAnalyse = new HashMap<Integer, String>();

  @SuppressWarnings("unused")
  private final JochreSession jochreSession;
  private final GraphicsDao graphicsDao;

  private BadGuessCollector(String[] outcomesToAnalyse, JochreSession jochreSession) {
    this.jochreSession = jochreSession;
    this.graphicsDao = GraphicsDao.getInstance(jochreSession);
    this.outcomesToAnalyse = outcomesToAnalyse;
  }

  @Override
  public void onImageStart(JochreImage jochreImage) {
  }

  @Override
  public void onGuessLetter(ShapeInSequence shapeInSequence, String bestGuess) {
    Shape shape = shapeInSequence.getShape();
    if (outcomesToAnalyseList.contains(shape.getLetter()) && !shape.getLetter().equals(bestGuess))
      shapeIdsToAnalyse.put(shape.getId(), bestGuess);
  }

  @Override
  public void onFinish() {

    for (int shapeId : shapeIdsToAnalyse.keySet()) {
      Shape shape = this.graphicsDao.loadShape(shapeId);
      String bestOutcome = shapeIdsToAnalyse.get(shapeId);
      LOG.debug("### Shape " + shape);
      LOG.debug("Expected: " + shape.getLetter() + " Guessed: " + bestOutcome);
      shape.writeImageToLog();
    }
  }

  /**
   * A list of outcomes that should be written to the log to allow for more
   * detailed analysis.
   */
  public String[] getOutcomesToAnalyse() {
    return outcomesToAnalyse;
  }

  public void setOutcomesToAnalyse(String[] outcomesToAnalyse) {
    this.outcomesToAnalyse = outcomesToAnalyse;
    this.outcomesToAnalyseList = new ArrayList<String>();
    for (String outcomeToAnalyse : this.outcomesToAnalyse)
      this.outcomesToAnalyseList.add(outcomeToAnalyse);
  }

  @Override
  public void onImageEnd() {
  }

  @Override
  public void onGuessSequence(LetterSequence letterSequence) {

  }

  @Override
  public void onStartSequence(LetterSequence letterSequence) {

  }

  @Override
  public void onBeamSearchEnd(LetterSequence bestSequence, List<LetterSequence> finalSequences, List<LetterSequence> holdoverSequences) {
  }

}
