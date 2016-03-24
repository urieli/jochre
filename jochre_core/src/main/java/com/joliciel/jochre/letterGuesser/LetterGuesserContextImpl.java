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
package com.joliciel.jochre.letterGuesser;

import java.util.List;

import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.Shape;

class LetterGuesserContextImpl implements LetterGuesserContext {
	private ShapeInSequence shapeInSequence;
	private LetterSequence history;
	public LetterGuesserContextImpl(ShapeInSequence shapeInSequence, LetterSequence history) {
		super();
		this.shapeInSequence = shapeInSequence;
		this.history = history;
	}

	public ShapeInSequence getShapeInSequence() {
		return shapeInSequence;
	}

	public LetterSequence getHistory() {
		return history;
	}

	@Override
	public Shape getShape() {
		return this.shapeInSequence.getShape();
	}

	@Override
	public int getIndex() {
		return this.shapeInSequence.getIndex();
	}

	@Override
	public void setIndex(int index) {
		this.shapeInSequence.setIndex(index);
	}

	@Override
	public ShapeSequence getShapeSequence() {
		return this.shapeInSequence.getShapeSequence();
	}

	@Override
	public List<Shape> getOriginalShapes() {
		return this.shapeInSequence.getOriginalShapes();
	}
	

}
