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
import java.util.List;

import com.joliciel.jochre.graphics.Shape;

class ShapeInSequenceImpl implements ShapeInSequence {
	private Shape shape;
	private int index;
	private ShapeSequence shapeSequence;
	private List<Shape> originalShapes = new ArrayList<Shape>();
	
	public  ShapeInSequenceImpl(Shape shape, int index,
			ShapeSequence shapeSequence) {
		super();
		this.shape = shape;
		this.index = index;
		this.shapeSequence = shapeSequence;
	}
	public Shape getShape() {
		return shape;
	}
	public void setShape(Shape shape) {
		this.shape = shape;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public ShapeSequence getShapeSequence() {
		return shapeSequence;
	}
	public void setShapeSequence(ShapeSequence shapeSequence) {
		this.shapeSequence = shapeSequence;
	}
	public List<Shape> getOriginalShapes() {
		return originalShapes;
	}
	

}
