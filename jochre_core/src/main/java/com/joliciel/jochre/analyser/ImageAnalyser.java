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

import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.graphics.JochreImage;

/**
 * Analyse images using a given model. Can be used for both evaluation of
 * held-out/test data, and for analysis of new pages.
 * 
 * @author Assaf Urieli
 *
 */
public interface ImageAnalyser extends DocumentObserver {
  /**
   * Analyse the letters in an image.
   */
  public abstract void analyse(JochreImage image);

  public abstract void addObserver(LetterGuessObserver letterGuessObserver);

  /**
   * Only retain those guesses whose probability is &gt;= this value.
   */
  public abstract double getMinOutcomeWeight();

  /**
   * The number of guesses to retain per analysis step.
   */
  public abstract int getBeamWidth();
}
