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
package com.joliciel.jochre.graphics;

class ImageMirror implements WritableImageGrid {
	private ImageGrid imageGrid;
	private boolean[] pixels;
	
	public ImageMirror(ImageGrid imageGrid) {
		this.imageGrid = imageGrid;
		pixels = new boolean[imageGrid.getWidth() * imageGrid.getHeight()];
	}

	@Override
	public void setPixel(int x, int y, int value) {
		pixels[y * imageGrid.getWidth() + x] = (value!=0);
	}

	@Override
	public int getAbsolutePixel(int x, int y) {
		if (x<0 || y<0 || x>=this.getWidth() || y >= this.getHeight())
			return 0;
		return pixels[y * imageGrid.getWidth() + x] ? 1 : 0;
	}

	@Override
	public int getHeight() {
		return imageGrid.getHeight();
	}

	@Override
	public int getPixel(int x, int y) {
		return this.getAbsolutePixel(x, y);
	}
	
	

	@Override
	public int getRawPixel(int x, int y) {
		return this.getAbsolutePixel(x, y);
	}
	
	

	@Override
	public int getRawAbsolutePixel(int x, int y) {
		return this.getAbsolutePixel(x, y);
	}

	@Override
	public int getWidth() {
		return imageGrid.getWidth();
	}

	@Override
	public boolean isPixelBlack(int x, int y, int threshold) {
		if (x < 0 || y < 0 || x >= this.getWidth() || y >= this.getHeight())
			return false;
		if (this.getPixel(x, y)==1)
			return true;
		else
			return false;
	}

	
}
