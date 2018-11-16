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

import java.util.List;

import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.letterGuesser.LetterSequence;

/**
 * An observer during analysis which gets events whenever letters are guessed.
 * @author Assaf Urieli
 *
 */
public interface LetterGuessObserver {
  /**
   * Called when we start analysing the next image.
   */
  public void onImageStart(JochreImage jochreImage);
  
  /**
   * Called each time the final letter is guessed for a given shape.
   */
  public void onGuessLetter(ShapeInSequence shapeInSequence, String bestGuess);
  
  /**
   * Called when a new sequence is about to be guessed, before any calls to onGuessLetter.
   */
  public void onStartSequence(LetterSequence letterSequence);

  /**
   * Called when the beam search has completed and we have the n most likely sequences,
   * as well as the best sequence selected from among them.
   */
  public void onBeamSearchEnd(LetterSequence bestSequence, List<LetterSequence> finalSequences, List<LetterSequence> holdoverSequences);
  
  /**
   * Called when the best letter sequence has been chosen for a given group, after all calls to onGuessLetter.
   */
  public void onGuessSequence(LetterSequence bestSequence);
  
  /**
   * Called whenever processing ends for the previous image.
   */
  public void onImageEnd();
  
  /**
   * Called when analysis is complete.
   */
  public void onFinish();
}
