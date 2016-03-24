package com.joliciel.jochre.graphics;

public class SourceImageMock extends SourceImageImpl {
	private int[] pixels;

	public SourceImageMock(int[] pixels, int height, int width) {
		this.pixels = pixels;
		this.setHeight(height);
		this.setWidth(width);
	}

	@Override
	public boolean isPixelBlack(int x, int y, int threshold) {
		if (x >= 0 && x < this.getWidth() && y >= 0 && y < this.getHeight())
			return pixels[y*this.getWidth() + x]==1;
		else
			return false;
	}

	@Override
	public int getRawAbsolutePixel(int x, int y) {
		if (this.isPixelBlack(x, y, 100))
			return 0;
		else
			return 255;
	}

	@Override
	public int getAbsolutePixel(int x, int y) {
		return this.getRawAbsolutePixel(x, y);
	}

	@Override
	public int getPixel(int x, int y) {
		return this.getAbsolutePixel(x, y);
	}
	
	
}
