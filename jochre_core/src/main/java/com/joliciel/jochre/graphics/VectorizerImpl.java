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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.BitSet;
import java.util.List;
import java.util.TreeSet;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VectorizerImpl implements Vectorizer {
	private static final Log LOG = LogFactory.getLog(VectorizerImpl.class);
	private List<LineDefinition> lineDefinitions = null;
	private GraphicsServiceInternal graphicsService;
	
	/**
	 * The number of line definitions per 45 degree sector.
	 */
	public static final int LINE_DEFS_PER_SECTOR = 4;
	
	/**
	 * The default white gap fill factor used prior to vectorizing a shape.
	 */
	public static final int WHITE_GAP_FILL_FACTOR = 0;
	
	private int whiteGapFillFactor = WHITE_GAP_FILL_FACTOR;
	
	public BufferedImage drawArrayLists(JochreImage jochreImage) {
		long startTime = (new Date()).getTime();
		BufferedImage vectorizedImage = new BufferedImage(jochreImage.getWidth(), jochreImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics2D = vectorizedImage.createGraphics();
		graphics2D.setStroke(new BasicStroke(1));
		graphics2D.setPaint(Color.BLACK);
		
		for (Paragraph paragraph : jochreImage.getParagraphs()) {
			for (RowOfShapes row : paragraph.getRows()) {
				for (GroupOfShapes group : row.getGroups()) {
					for (Shape shape : group.getShapes()) {
						List<LineSegment> lines = this.vectorize(shape);
						
						for (LineSegment line : lines)
							graphics2D.drawLine(shape.getLeft() + line.getStartX(), shape.getTop() + line.getStartY(),
									shape.getLeft() + line.getEndX(), shape.getTop() + line.getEndY());
					}
				}
			}
		}
		
		if (LOG.isDebugEnabled()) {
			long endTime = (new Date()).getTime();
			long diff = endTime - startTime;
			LOG.debug("Time elapsed: " + ((double)diff/1000));
		}
		return vectorizedImage;
	}
	
	@Override
	public List<LineSegment> vectorize(Shape shape) {
		int threshold = shape.getJochreImage().getBlackThreshold();
		
		// find outline of shape
		BitSet outline = shape.getOutline(threshold);
		
		// find n longest lines within shape which connect two points in the outline
		int maxLines = 200;
		List<LineSegment> lineSegments = this.getLongestLines(shape, outline, maxLines, threshold);
		
		// combine/eliminate similar lines
		lineSegments = this.combineSegments(shape, lineSegments);
		
		return lineSegments;
	}

	/**
	 * Find n longest lines within shape which connect two points in the outline
	 */
	List<LineSegment> getLongestLines(Shape shape, BitSet outline, int maxLines, int threshold) {
		TreeSet<LineSegment> lineSegmentSet = new TreeSet<LineSegment>();
		
		int outlineCardinality = outline.cardinality();
		int samplingInterval = outlineCardinality / 100;
		if (samplingInterval==0) samplingInterval = 1;
		
		int samplingIndex = 0;
		for (int y = 0; y < shape.getHeight(); y++) {
			for (int x = 0; x < shape.getWidth(); x++) {
				if (outline.get(y * shape.getWidth() + x)) {
					// this pixel is part of the outline
					if (samplingIndex==0) {
						lineSegmentSet.addAll(this.getLinesToEdge(shape, x, y, threshold));
					}
					samplingIndex++;
					if (samplingIndex == samplingInterval)
						samplingIndex = 0;
				}
			}
		}
		
		int i = 0;
		List<LineSegment> lineSegments = new ArrayList<LineSegment>();
		for (LineSegment lineSegment : lineSegmentSet) {
			lineSegments.add(lineSegment);
			i++;
			if (i>=maxLines)
				break;
		}
		
		if (LOG.isDebugEnabled()) {
			i = 0;
			for (LineSegment lineSegment : lineSegments) {
				double slope = (double)(lineSegment.getEndY() - lineSegment.getStartY()) / (double) (lineSegment.getEndX() - lineSegment.getStartX());
				LOG.debug("Line " + i++ + "(" + lineSegment.getStartX() + "," + lineSegment.getStartY() + ") "
						+ "(" + lineSegment.getEndX() + "," + lineSegment.getEndY() + "). Length = " + lineSegment.getLength()
						+ ", Slope = " + slope);
			}
		}
		return lineSegments;
	}

	/**
	 * Get the line segments going from a particular point in the outline to an opposite edge.
	 */
	List<LineSegment> getLinesToEdge(Shape shape, int xOrigin, int yOrigin, int threshold) {
		List<LineSegment> lineSegments = new ArrayList<LineSegment>();
		
		for (LineDefinition lineDef : this.getLineDefinitions()) {
			if (LOG.isDebugEnabled()) {
				String lineDefText = "Sector " + lineDef.getSector() + ". Delta (" + lineDef.getDeltaX() + "," + lineDef.getDeltaY() + "). "
					+ "Incr (" + lineDef.getXIncrement() + "," + lineDef.getYIncrement() + "). ";
				lineDefText += "{";
				for (int j = 0; j < lineDef.getSteps().size(); j++)
					lineDefText += lineDef.getSteps().get(j);
				lineDefText += "}";
				LOG.debug("Line def: " + lineDefText);
			}
			int[] edgePoint = lineDef.followInShape(shape, xOrigin, yOrigin, 0, threshold, this.whiteGapFillFactor);
			
			int endX = edgePoint[0];
			int endY = edgePoint[1];
			int length = edgePoint[2];
			if (length!=0) {
				LineSegment lineSegment = this.graphicsService.getEmptyLineSegment(shape, lineDef, xOrigin, yOrigin, endX, endY);
				lineSegment.setLength(length);
				boolean foundLineSegment = false;
				for (LineSegment lineSegment2 : lineSegments) {
					if (lineSegment.compareTo(lineSegment2)==0) {
						foundLineSegment = true;
						break;
					}
				}
				if (!foundLineSegment) {
					if (LOG.isDebugEnabled())
						LOG.debug("Line (" + lineSegment.getStartX() + "," + lineSegment.getStartY() + ") "
								+ "(" + lineSegment.getEndX() + "," + lineSegment.getEndY() + "). Length = " + lineSegment.getLength());
					lineSegments.add(lineSegment);
				}
			}
		} // next line definition
		
		return lineSegments;
	}
	
	/**
	 * The line definitions basically give definitions for a series of lines
	 * inside 45° of a circle.
	 * These go from a straight line (infinity) to a diagonal line (1).
	 * The numbers represent the steps to take before taking one step
	 * in the other axis.
	 * Thus, {1} means take 1 step in the x axis, 1 step in the y axis
	 * {2} means take 2 x-steps followed by 1 y-step, and start over
	 * {2,1} means take 2 x-steps, 1 y-step, 1 x-step, 1 y-step and start over
	 * {0} represents infinity = stay in the original axis.
	 */
	List<LineDefinition> getLineDefinitions() {
		if (this.lineDefinitions==null) {
			lineDefinitions = new ArrayList<LineDefinition>();
			List<List<Integer>> stepList = new ArrayList<List<Integer>>();
			for (int i = 0; i<64; i+= 64/LINE_DEFS_PER_SECTOR) {
				List<Integer> line = new ArrayList<Integer>();
			
				if (i==0) {
					line.add(0);
				} else {
					double slope = 64.0 / (double) i;
					double currentSpot = slope;
					int currentPixelCount = 0;
					while(currentSpot <= 64) {
						int newPixelCount = (int) Math.floor(currentSpot);
						int diff = newPixelCount - currentPixelCount;
						line.add(diff);
						currentPixelCount = newPixelCount;
						currentSpot += slope;
					}
				}
				if (LOG.isDebugEnabled()) {
					String lineDefText = "{";
					for (int j = 0; j < line.size(); j++)
						lineDefText += line.get(j);
					lineDefText += "}";
					LOG.debug("Line " + i + ": " + lineDefText);
				}
				stepList.add(line);
			}
			
			int index = 0;
			for (int sector=0; sector<4; sector++) {
				for (List<Integer> steps: stepList) {
					LineDefinition lineDef = graphicsService.getEmptyLineDefinition(sector,index++);
					lineDef.setSteps(steps);
					lineDefinitions.add(lineDef);
				}
			}
		}

		return this.lineDefinitions;
	}

	private List<LineSegment> combineSegments(Shape shape,
			List<LineSegment> lineSegments) {
		// get rid of overlapping segments
		List<LineSegment> lineSegmentsToDelete = new ArrayList<LineSegment>();
		for (int i=0; i< lineSegments.size() - 1; i++) {
			for (int j=i+1;j<lineSegments.size(); j++) {
				LineSegment lineSegment1 = lineSegments.get(i);
				LineSegment lineSegment2 = lineSegments.get(j);
				
				// check for overlap
				int tolerance = 3;
				int line1left, line1top, line1right, line1bottom;
				int line2left, line2top, line2right, line2bottom;
				if (lineSegment1.getStartX()<=lineSegment1.getEndX()) {
					line1left = lineSegment1.getStartX()-tolerance;
					line1right = lineSegment1.getEndX()+tolerance;
				} else {
					line1left = lineSegment1.getEndX()-tolerance;
					line1right = lineSegment1.getStartX()+tolerance;
				}
				if (lineSegment2.getStartX()<=lineSegment2.getEndX()) {
					line2left = lineSegment2.getStartX()-tolerance;
					line2right = lineSegment2.getEndX()+tolerance;
				} else {
					line2left = lineSegment2.getEndX()-tolerance;
					line2right = lineSegment2.getStartX()+tolerance;
				}
				if (lineSegment1.getStartY()<=lineSegment1.getEndY()) {
					line1top = lineSegment1.getStartY()-tolerance;
					line1bottom = lineSegment1.getEndY()+tolerance;
				} else {
					line1top = lineSegment1.getEndY()-tolerance;
					line1bottom = lineSegment1.getStartY()+tolerance;
				}
				if (lineSegment2.getStartY()<=lineSegment2.getEndY()) {
					line2top = lineSegment2.getStartY()-tolerance;
					line2bottom = lineSegment2.getEndY()+tolerance;
				} else {
					line2top = lineSegment2.getEndY()-tolerance;
					line2bottom = lineSegment2.getStartY()+tolerance;
				}
				
				// is overlap possible?
				if (line1left <= line2right && line1right >= line2left
						&& line1top <= line2bottom && line1bottom >= line2top) {
					// note: line1 is guaranteed to be longer than or of equal length to line 2
					BitSet rect2 = lineSegment2.getEnclosingRectangle(tolerance);
					BitSet intersection = lineSegment1.getEnclosingRectangleIntersection(lineSegment2, tolerance);
					int area2 = rect2.cardinality();
					int interMulitplied = intersection.cardinality() * 2;
					if (interMulitplied>area2) {
						lineSegmentsToDelete.add(lineSegment2);
					}
				}
			}
		}
		lineSegments.removeAll(lineSegmentsToDelete);
		
		//TODO: combine lines that are "more or less" in the same location & direction
		
		return lineSegments;
	}
	
	public GraphicsServiceInternal getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsServiceInternal graphicsService) {
		this.graphicsService = graphicsService;
	}

	/**
	 * The white gap fill factor used prior to vectorizing a shape.
	 */
	public int getWhiteGapFillFactor() {
		return whiteGapFillFactor;
	}

	public void setWhiteGapFillFactor(int whiteGapFillFactor) {
		this.whiteGapFillFactor = whiteGapFillFactor;
	}
	
	
}
