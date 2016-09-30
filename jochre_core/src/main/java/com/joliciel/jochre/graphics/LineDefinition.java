package com.joliciel.jochre.graphics;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Defines a line direction from an origin point in a manner which simplifies
 * manipulation in raster spaces.
 * 
 * @author Assaf Urieli
 *
 */
public class LineDefinition {
	private int sector;
	private int index;
	private int[] deltaX = { -1, -1, 0, 1, 1, 1, 0, -1 };
	private int[] deltaY = { 0, 1, 1, 1, 0, -1, -1, -1 };
	private int[] xIncrement = { 0, 1, 1, 0, 0, -1, -1, 0 };
	private int[] yIncrement = { 1, 0, 0, -1, -1, 0, 0, 1 };
	private List<Integer> steps;

	public LineDefinition(int sector, int index) {
		this.sector = sector;
		this.index = index;
	}

	/**
	 * The x delta of the default axis. Values will be -1, 0 or +1.
	 */
	public int getDeltaX() {
		return deltaX[sector];
	}

	/**
	 * The y delta of the default axis. Values will be -1, 0 or +1.
	 */
	public int getDeltaY() {
		return deltaY[sector];
	}

	/**
	 * The x-increment to take when a step has been completed. Values will be -1,
	 * 0 or +1.
	 */
	public int getXIncrement() {
		return xIncrement[sector];
	}

	/**
	 * The y-increment to take when a step has been completed. Values will be -1,
	 * 0 or +1.
	 */
	public int getYIncrement() {
		return yIncrement[sector];
	}

	/**
	 * The numbers represent the pixels to traverse before in the default axis
	 * before making a single increment in the increment axis. Thus, {1} means
	 * take 1 step in the default axis and increments simultaneously. {2} means
	 * take 2 steps in the default axis, incrementing on the 2nd step. {2,1} means
	 * take 2 steps, increment, take 1 step, increment, and repeat. {0} represents
	 * infinity = stay in the default axis.
	 */
	public List<Integer> getSteps() {
		return steps;
	}

	public void setSteps(List<Integer> steps) {
		this.steps = steps;
	}

	/**
	 * Trace a line using this line definition from a given origin for a number of
	 * pixels = length. If parts of the line lie outside the bitset, they will
	 * simply not be traced. The rotation allows us to rotate the sector of this
	 * line definition counter-clockwise by the number of sectors indicated.
	 * 
	 * @param bitset
	 *          bitset in which to trace the line
	 * @param shape
	 *          shape containing this line
	 * @param length
	 *          in pixels excluding the origin
	 * @param rotation
	 *          number of sectors to rotate, can be a negative number.
	 */
	public void trace(BitSet bitset, Shape shape, int xOrigin, int yOrigin, int length, int rotation) {
		this.followLineDef(bitset, shape, xOrigin, yOrigin, length, rotation, true, false, 0, 0, -1, null);
	}

	/**
	 * Follow a line using this line definition from a given origin for a number
	 * of pixels = length. The rotation allows us to rotate the sector of this
	 * line definition counter-clockwise by the number of sectors indicated.
	 * 
	 * @param shape
	 *          shape containing this line
	 * @param length
	 *          in pixels excluding the origin
	 * @param rotation
	 *          number of sectors to rotate, can be a negative number.
	 * @return the x,y coordinates of the resulting point as {x,y}
	 */
	public int[] follow(Shape shape, int xOrigin, int yOrigin, int length, int rotation) {
		int[] endPoint = this.followLineDef(null, shape, xOrigin, yOrigin, length, rotation, false, false, 0, 0, -1, null);
		return new int[] { endPoint[0], endPoint[1] };
	}

	/**
	 * Follow this line until we exit the shape (that is, until we hit a pixel in
	 * the shape that is off). For other parameters @see #follow(Shape, int, int,
	 * int, int)
	 * 
	 * @param threshold
	 *          the threshold below which a pixel is considered black (on)
	 * @return the last pixel along the line that is black, and the total length,
	 *         as {x, y, length}
	 */
	public int[] followInShape(Shape shape, int xOrigin, int yOrigin, int rotation, int threshold, int whiteGapFillFactor) {
		return this.followLineDef(null, shape, xOrigin, yOrigin, -1, rotation, false, true, threshold, whiteGapFillFactor, -1, null);
	}

	int[] followLineDef(BitSet bitset, Shape shape, int xOrigin, int yOrigin, int length, int rotation, boolean trace, boolean stayInShape, int threshold,
			int whiteGapFillFactor, int sampleGap, List<int[]> samplePoints) {
		int x = xOrigin;
		int y = yOrigin;
		int currentSegment = steps.get(0);
		int currentLineDefPos = 0;
		int posOnSegment = 0;

		int newSector = sector + rotation;
		if (newSector >= 8)
			newSector = newSector - 8;
		if (newSector < 0)
			newSector = 8 + newSector;

		int lastX = 0;
		int lastY = 0;
		int i;
		int sampleCounter = 0;
		for (i = 0; i <= length || length < 0; i++) {
			if (trace && x >= 0 && y >= 0 && x < shape.getWidth() && y < shape.getHeight())
				bitset.set(y * shape.getWidth() + x, true);
			lastX = x;
			lastY = y;
			x += this.deltaX[newSector];
			y += this.deltaY[newSector];
			sampleCounter++;
			if (sampleCounter == sampleGap) {
				samplePoints.add(new int[] { x, y });
				sampleCounter = 0;
			}
			posOnSegment++;
			if (posOnSegment == currentSegment) {
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

		int[] endPoint = { lastX, lastY, i };
		return endPoint;
	}

	/**
	 * Given a known vector within a shape, find the lengths of line segments
	 * perpendicular to this vector, sampled every sampleGap pixels.
	 * 
	 * @param shape
	 *          the shape to analyse
	 * @param xOrigin
	 *          the x-origin of the vector
	 * @param yOrigin
	 *          the y-origin of the vector
	 * @param length
	 *          the length of the vector
	 * @param threshold
	 *          the threshold for considering a pixel black
	 * @param sampleStep
	 *          the number of pixels to skip before taking the next sample
	 * @return a List of Integer containing the length of each line segment
	 *         sampled
	 */
	public List<Integer> findArrayListThickness(Shape shape, int xOrigin, int yOrigin, int length, int threshold, int whiteGapFillFactor, int sampleStep) {
		if (sampleStep == 0)
			sampleStep = 1;
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

	/**
	 * The 45 degree sector of this line definition. Sector 0 starts at the
	 * horizontal facing left and moves down to a diagonal facing left and down.
	 * There are 8 sectors.
	 */
	public int getSector() {
		return sector;
	}

	public void setSector(int sector) {
		this.sector = sector;
	}

	/**
	 * The index of this line definition. Starts at 0 for the horizontal going
	 * left, and increases counter-clockwise. There are 8 lines per 45 degree
	 * sector. Thus, lines with an adjacent index will have a similar slope.
	 */
	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

}
