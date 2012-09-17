package com.joliciel.jochre.graphics;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class LineDefinitionImpl implements LineDefinition {
	private int sector;
	private int index;
	private int[] deltaX = {-1, -1, 0, 1, 1, 1, 0, -1};
	private int[] deltaY = {0, 1, 1, 1, 0, -1, -1, -1};
	private int[] xIncrement = {0, 1, 1, 0, 0, -1, -1, 0};
	private int[] yIncrement = {1, 0, 0, -1, -1, 0, 0, 1};
	private List<Integer> steps;
	
	public LineDefinitionImpl(int sector, int index) {
		this.sector = sector;
		this.index = index;
	}
	
	public int getDeltaX() {
		return deltaX[sector];
	}

	public int getDeltaY() {
		return deltaY[sector];
	}

	public int getXIncrement() {
		return xIncrement[sector];
	}

	public int getYIncrement() {
		return yIncrement[sector];
	}

	public List<Integer> getSteps() {
		return steps;
	}
	public void setSteps(List<Integer> steps) {
		this.steps = steps;
	}
	
	@Override
	public void trace(BitSet bitset, Shape shape, int xOrigin, int yOrigin, int length, int rotation) {
		this.followLineDef(bitset, shape, xOrigin, yOrigin, length, rotation, true, false, 0, 0, -1, null);
	}
	
	public int[] follow(Shape shape, int xOrigin, int yOrigin, int length, int rotation) {
		int[] endPoint = this.followLineDef(null, shape, xOrigin, yOrigin, length, rotation, false, false, 0, 0, -1, null);
		return new int[] {endPoint[0], endPoint[1]};
	}
	
	public int[] followInShape(Shape shape, int xOrigin, int yOrigin, int rotation, int threshold, int whiteGapFillFactor) {
		return this.followLineDef(null, shape, xOrigin, yOrigin, -1, rotation, false, true, threshold, whiteGapFillFactor, -1, null);
	}
	
	int[] followLineDef(BitSet bitset, Shape shape, int xOrigin, int yOrigin, int length, int rotation, boolean trace, boolean stayInShape, int threshold, int whiteGapFillFactor, int sampleGap, List<int[]> samplePoints) {
		int x = xOrigin;
		int y = yOrigin;
		int currentSegment = steps.get(0);
		int currentLineDefPos = 0;
		int posOnSegment = 0;
		
		int newSector = sector + rotation;
		if (newSector >= 8) newSector = newSector - 8;
		if (newSector < 0) newSector = 8 + newSector;
		
		int lastX = 0;
		int lastY = 0;
		int i;
		int sampleCounter = 0;
		for (i = 0; i <= length || length<0; i++) {
			if (trace && x>=0 && y>=0 && x<shape.getWidth() && y<shape.getHeight())
				bitset.set(y * shape.getWidth() + x, true);
			lastX = x;
			lastY = y;
			x += this.deltaX[newSector];
			y += this.deltaY[newSector];
			sampleCounter++;
			if (sampleCounter == sampleGap) {
				samplePoints.add(new int[] {x,y});
				sampleCounter = 0;
			}
			posOnSegment++;
			if (posOnSegment==currentSegment) {
				x += this.xIncrement[newSector];
				y += this.yIncrement[newSector];
				posOnSegment = 0;
				currentLineDefPos++;
				if (currentLineDefPos >= steps.size())
					currentLineDefPos = 0;
				currentSegment = steps.get(currentLineDefPos);
			} // have we hit an end of a segment?
			if (stayInShape && !shape.isPixelBlack(x, y, threshold, whiteGapFillFactor)) {
				// have we hit an empty pixel on the shape?
				break;
			}
		} // loop until we reach the desired length
		
		int[] endPoint = {lastX, lastY, i};
		return endPoint;
	}

	
	@Override
	public List<Integer> findArrayListThickness(Shape shape, int xOrigin, int yOrigin,
			int length, int threshold, int whiteGapFillFactor, int sampleStep) {
		if (sampleStep==0) sampleStep = 1;
		int sampleSize = length / sampleStep;
		
		List<int[]> samplePoints = new ArrayList<int[]>(sampleSize + 1);
		this.followLineDef(null, shape, xOrigin, yOrigin, length, 0, false, true, threshold, whiteGapFillFactor, sampleStep, samplePoints);
		List<Integer> sampleLengths = new ArrayList<Integer>(samplePoints.size());
		
		for (int[] samplePoint : samplePoints) {
			int[] endPoint = this.followInShape(shape, samplePoint[0], samplePoint[1], 2, threshold, whiteGapFillFactor);
			int[] oppositeEndPoint = this.followInShape(shape, endPoint[0], endPoint[1], -2, threshold, whiteGapFillFactor);
			int perpendicularLineLength = oppositeEndPoint[2];
			sampleLengths.add(perpendicularLineLength);
		}
		return sampleLengths;
	}

	public int getSector() {
		return sector;
	}

	public void setSector(int sector) {
		this.sector = sector;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
	
}
