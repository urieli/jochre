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

import com.joliciel.jochre.boundaries.BoundaryDetector;
import com.joliciel.jochre.graphics.JochreCorpusImageReader;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.letterGuesser.LetterGuesser;

/**
 * Analyse images using a given model.
 * Can be used for both evaluation of held-out/test data, and for analysis of new pages.
 * 
 * @author Assaf Urieli
 *
 */
public interface ImageAnalyser {
	/**
	 * Analyse the letters in a set of images.
	 * @param letterGuesser the letter guesser to use
	 * @param imageStatus which image status to evaluate (typically held-out or test)
	 */
	public void analyse(LetterGuesser letterGuesser, JochreCorpusImageReader imageReader);

	public abstract void addObserver(LetterGuessObserver letterGuessObserver);

	public abstract void analyse(LetterGuesser letterGuesser, JochreImage image);

	public abstract void setBoundaryDetector(BoundaryDetector boundaryDetector);

	public abstract BoundaryDetector getBoundaryDetector();
}
