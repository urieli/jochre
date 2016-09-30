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

/**
 * A pair of consecutive shapes.
 * 
 * @author Assaf Urieli
 *
 */
public class ShapePair {
	private final Shape firstShape;
	private final Shape secondShape;

	public ShapePair(Shape firstShape, Shape secondShape) {
		this.firstShape = firstShape;
		this.secondShape = secondShape;
	}

	public Shape getFirstShape() {
		return firstShape;
	}

	public Shape getSecondShape() {
		return secondShape;
	}

	public int getLeft() {
		int left = firstShape.getLeft() < secondShape.getLeft() ? firstShape.getLeft() : secondShape.getLeft();
		return left;
	}

	public int getRight() {
		int right = firstShape.getRight() > secondShape.getRight() ? firstShape.getRight() : secondShape.getRight();
		return right;
	}

	public int getTop() {
		int top = firstShape.getTop() < secondShape.getTop() ? firstShape.getTop() : secondShape.getTop();
		return top;
	}

	public int getBottom() {
		int bottom = firstShape.getBottom() > secondShape.getBottom() ? firstShape.getBottom() : secondShape.getBottom();
		return bottom;
	}

	public int getXHeight() {
		int xHeight = firstShape.getXHeight() > secondShape.getXHeight() ? firstShape.getXHeight() : secondShape.getXHeight();
		if (xHeight <= 0)
			xHeight = 1;
		return xHeight;
	}

	public int getWidth() {
		return this.getRight() - this.getLeft() + 1;
	}

	public int getHeight() {
		return this.getBottom() - this.getTop() + 1;
	}

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
		return "ShapePair [firstShape=" + firstShape + ", secondShape=" + secondShape + "]";
	}

}
