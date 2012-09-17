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

import com.joliciel.jochre.graphics.Shape;

class ShapePairImpl implements ShapePair {
	private Shape firstShape;
	private Shape secondShape;
	public ShapePairImpl(Shape firstShape, Shape secondShape) {
		super();
		this.firstShape = firstShape;
		this.secondShape = secondShape;
	}
	public Shape getFirstShape() {
		return firstShape;
	}
	public Shape getSecondShape() {
		return secondShape;
	}
	
	@Override
	public int getLeft() {
		int left = firstShape.getLeft() < secondShape.getLeft() ? firstShape.getLeft() : secondShape.getLeft();
		return left;
	}
	
	@Override
	public int getRight() {
		int right = firstShape.getRight() > secondShape.getRight() ? firstShape.getRight() : secondShape.getRight();
		return right;
	}
	
	@Override
	public int getTop() {
		int top = firstShape.getTop() < secondShape.getTop() ? firstShape.getTop() : secondShape.getTop();
		return top;
	}
	
	@Override
	public int getBottom() {
		int bottom = firstShape.getBottom() > secondShape.getBottom() ? firstShape.getBottom() : secondShape.getBottom();
		return bottom;
	}
	
	@Override
	public int getXHeight() {
		int xHeight = firstShape.getXHeight() > secondShape.getXHeight() ? firstShape.getXHeight() : secondShape.getXHeight();
		if (xHeight<=0) xHeight = 1;
		return xHeight;
	}
	@Override
	public int getWidth() {
		return this.getRight() - this.getLeft() + 1;
	}
	@Override
	public int getHeight() {
		return this.getBottom() - this.getTop() + 1;
	}
	@Override
	public int getInnerDistance() {
		int distance = 0;
		if (this.getFirstShape().getLeft() < this.getSecondShape().getLeft()) {
			distance = this.getSecondShape().getLeft() - this.getFirstShape().getRight() - 1;
		} else {
			distance = this.getFirstShape().getLeft() - this.getSecondShape().getRight() - 1;
		}
		return distance;
	}
	@Override
	public String toString() {
		return "ShapePair [firstShape=" + firstShape + ", secondShape="
				+ secondShape + "]";
	}
	
	
}
