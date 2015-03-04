package com.joliciel.jochre.graphics;

import java.util.BitSet;

public class ShapeMock extends ShapeImpl {
	private int[] pixels;
	private BitSet bitset;
	private BitSet outline;

	public ShapeMock(int[] pixels, int height, int width) {
		this.pixels = pixels;
		this.setTop(0);
		this.setBottom(height);
		this.setLeft(0);
		this.setRight(width);
		
		bitset = new BitSet(height * width);
       	for (int x = -1; x <= width; x++) {
    		for (int y = -1; y <= height; y++) {
    			if (x >= 0 && x < width && y >= 0 && y < height && pixels[y*width + x]==1)
    				bitset.set(y * width + x);
    		}
       	}
	}

	@Override
	public boolean isPixelBlack(int x, int y) {
		if (x >= 0 && x < this.getWidth() && y >= 0 && y < this.getHeight())
			return pixels[y*this.getWidth() + x]==1;
		else
			return false;
	}

	@Override
	public int getRawAbsolutePixel(int x, int y) {
		if (this.isPixelBlack(x, y))
			return 0;
		else
			return 255;
	}

	@Override
	public boolean isPixelBlack(int x, int y, int threshold) {
		return this.isPixelBlack(x, y);
	}

	@Override
	public BitSet getBlackAndWhiteBitSet(int threshold) {
		return bitset;
	}

	@Override
	public boolean isPixelBlack(int x, int y, int threshold,
			int whiteGapFillFactor) {
		return this.isPixelBlack(x, y);
	}

	@Override
	public BitSet getBlackAndWhiteBitSet(int threshold, int whiteGapFillFactor) {
		return bitset;
	}

	@Override
	public BitSet getOutline(int threshold) {
		if (outline!=null)
			return outline;
		return super.getOutline(threshold);
	}
	
	public void setOutline(BitSet outline) {
		this.outline = outline;
	}
}
