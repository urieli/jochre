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
import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.letterGuesser.LetterGuesser;
import com.joliciel.jochre.lexicon.MostLikelyWordChooser;

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

	public abstract void setLetterGuesser(LetterGuesser letterGuesser);

	public abstract LetterGuesser getLetterGuesser();

	public abstract void setBoundaryDetector(BoundaryDetector boundaryDetector);

	public abstract BoundaryDetector getBoundaryDetector();

	public abstract void setMostLikelyWordChooser(MostLikelyWordChooser mostLikelyWordChooser);

	public abstract MostLikelyWordChooser getMostLikelyWordChooser();

	public abstract double getMinOutcomeWeight();

	public abstract int getBeamWidth();
}
