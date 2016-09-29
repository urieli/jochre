package com.joliciel.jochre.graphics;

import java.util.BitSet;

import com.joliciel.jochre.JochreSession;

public class ShapeMock extends Shape {
	private int[] pixels;
	private BitSet bitset;
	private BitSet outline;

	public ShapeMock(int[] pixels, int left, int top, int width, int height, JochreSession jochreSession) {
		super(jochreSession);
		this.pixels = pixels;
		this.setTop(top);
		this.setBottom(top + height - 1);
		this.setLeft(left);
		this.setRight(left + width - 1);

		bitset = new BitSet(height * width);
		for (int x = -1; x <= width; x++) {
			for (int y = -1; y <= height; y++) {
				if (x >= 0 && x < width && y >= 0 && y < height && pixels[y * width + x] == 1)
					bitset.set(y * width + x);
			}
		}
	}

	public ShapeMock(int[] pixels, int width, int height, JochreSession jochreSession) {
		this(pixels, 0, 0, width, height, jochreSession);
	}

	@Override
	public boolean isPixelBlack(int x, int y) {
		if (x >= 0 && x < this.getWidth() && y >= 0 && y < this.getHeight()) {
			int index = y * this.getWidth() + x;
			return pixels[index] == 1;
		} else
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
	public boolean isPixelBlack(int x, int y, int threshold, int whiteGapFillFactor) {
		return this.isPixelBlack(x, y);
	}

	@Override
	public BitSet getBlackAndWhiteBitSet(int threshold, int whiteGapFillFactor) {
		return bitset;
	}

	@Override
	public BitSet getOutline(int threshold) {
		if (outline != null)
			return outline;
		return super.getOutline(threshold);
	}

	public void setOutline(BitSet outline) {
		this.outline = outline;
	}
}
