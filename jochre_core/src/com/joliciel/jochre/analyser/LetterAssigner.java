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

import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.letterGuesser.LetterSequence;

public class LetterAssigner implements LetterGuessObserver {
	JochreImage jochreImage = null;
	private boolean save;
	
	public LetterAssigner() {
		super();
	}

	@Override
	public void onGuessLetter(ShapeInSequence shapeInSequence, String bestGuess) {
		Shape shape = shapeInSequence.getShape();
		if (jochreImage.getImageStatus().equals(ImageStatus.AUTO_NEW)) {
			// if the image is brand new, we assign both the letter and the original guess
			// for all other images, we assign only the original guess
			// so as not to override the letter selected by the user
			shape.setLetter(bestGuess);
		}
		shape.setOriginalGuess(bestGuess);
	}


	@Override
	public void onImageStart(JochreImage jochreImage) {
		this.jochreImage = jochreImage;
	}

	@Override
	public void onImageEnd() {
	}

	@Override
	public void onFinish() {
	}

	@Override
	public void onGuessSequence(LetterSequence letterSequence) {
		if (save) {
			ShapeSequence shapeSequence = letterSequence.getUnderlyingShapeSequence();
			for (ShapeInSequence shapeInSequence : shapeSequence) {
				Shape shape = shapeInSequence.getShape();
				shape.save();
			}
		}
	}

	@Override
	public void onStartSequence(LetterSequence letterSequence) {
		
	}

	public boolean isSave() {
		return save;
	}

	public void setSave(boolean save) {
		this.save = save;
	}

}
