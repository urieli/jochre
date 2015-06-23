///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
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
package com.joliciel.jochre.search;

import java.io.Serializable;

public class Rectangle implements Serializable {
	private static final long serialVersionUID = 1L;
	private int left,top,right,bottom;

	public Rectangle(int left, int top, int right, int bottom) {
		super();
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
	}
	
	public Rectangle(Rectangle source) {
		this(source.getLeft(), source.getTop(), source.getRight(), source.getBottom());
	}
	
	public Rectangle(String fromString) {
		String[] parts = fromString.split("\\|");
		this.left = Integer.parseInt(parts[0]);
		this.top = Integer.parseInt(parts[1]);
		this.right = Integer.parseInt(parts[2]);
		this.bottom = Integer.parseInt(parts[3]);
	}


	public void expand(Rectangle source) {
		this.expand(source.getLeft(), source.getTop(), source.getRight(), source.getBottom());
	}
	public void expand(int left, int top, int right, int bottom) {
		if (left<this.left) this.left = left;
		if (top<this.top) this.top = top;
		if (right>this.right) this.right = right;
		if (bottom>this.bottom) this.bottom = bottom;
	}
	

	public int getLeft() {
		return left;
	}

	public void setLeft(int left) {
		this.left = left;
	}

	public void setTop(int top) {
		this.top = top;
	}

	public void setRight(int right) {
		this.right = right;
	}

	public void setBottom(int bottom) {
		this.bottom = bottom;
	}

	public int getTop() {
		return top;
	}

	public int getRight() {
		return right;
	}

	public int getBottom() {
		return bottom;
	}
	
	public int getWidth() {
		return right-left+1;
	}
	
	public int getHeight() {
		return bottom-top+1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bottom;
		result = prime * result + left;
		result = prime * result + right;
		result = prime * result + top;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rectangle other = (Rectangle) obj;
		if (bottom != other.bottom)
			return false;
		if (left != other.left)
			return false;
		if (right != other.right)
			return false;
		if (top != other.top)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Rectangle [left=" + left + ", top=" + top + ", right=" + right
				+ ", bottom=" + bottom + "]";
	}
	
	public String getString() {
		return this.getLeft() + "|" + this.getTop() + "|" + this.getRight() + "|" + this.getBottom();
	}
	
}
