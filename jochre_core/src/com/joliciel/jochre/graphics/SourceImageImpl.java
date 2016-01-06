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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.joliciel.jochre.stats.DBSCANClusterer;
import com.joliciel.jochre.utils.JochreException;

class SourceImageImpl extends JochreImageImpl implements SourceImageInternal {
	private static final Log LOG = LogFactory.getLog(SourceImageImpl.class);

	List<RowOfShapes> rows;
	Set<Set<RowOfShapes>> rowClusters = null;
	
	boolean shapeStatisticsCalculated = false;
	double averageShapeWidth;
	double averageShapeWidthMargin;
	double averageShapeHeight;
	double averageShapeHeightMargin;
	
	boolean meanHorizontalSlopeCalculated = false;
	double meanHorizontalSlope = 0;

	double greyscaleMultiplier = 1.0;
	List<Shape> largeShapes = null;
	List<Rectangle> whiteAreasAroundLargeShapes = null;
	List<Rectangle> columnSeparators = null;
	
	BufferedImage imageBackup;
	
	int myShapeCount = -1;
	
	boolean drawPixelSpread = false;
	
	SourceImageImpl() { super(); }
	
	public SourceImageImpl(GraphicsServiceInternal graphicsService, String name, BufferedImage image) {
		super(image);
		this.name = name;
		
		// increase contrast for grayscale images, to help with segmentation
		// the original image will be restored again after segmentation with a call to restoreOriginalImage()
		imageBackup = image;
		
        ColorModel srcCM = image.getColorModel();
        if (!(srcCM instanceof IndexColorModel)) {
			float contrastFactor = 1.4f;
			float brightnessFactor = 30f;
			BrightnessContrastOp op = new BrightnessContrastOp(contrastFactor, brightnessFactor, null);
			image = op.filter(image, null);
        }
		
		this.setOriginalImage(image);

		this.setGraphicsService(graphicsService);
		
		this.setWidth(this.getPixelGrabber().getWidth());
		this.setHeight(this.getPixelGrabber().getHeight());
		
		this.calculateThresholds(drawPixelSpread);
	}
	
	private void calculateThresholds(boolean drawPixelSpread) {
		// to normalise the image, we need to figure out where black and white are
		// we want to leave out anomalies (ink blots!)
		// also, we leave x% at each margin, in case there's black areas surrounding the image
		int[] pixelSpread = new int[256];
		
		int verticalMarginPixels = (int) Math.floor(this.getHeight() * 0.10);
		int horizontalMarginPixels = (int) Math.floor(this.getWidth() * 0.10);
		for (int y = verticalMarginPixels; y < this.getHeight()-verticalMarginPixels; y++)
			for (int x = horizontalMarginPixels; x < this.getWidth()-horizontalMarginPixels; x++){
				int pixel = this.getPixelGrabber().getPixelBrightness(x, y);
				pixelSpread[pixel]++;
			}
		
		if (LOG.isTraceEnabled()) {
			for (int i = 0; i<256; i++)
				LOG.trace("Brightness " + i + ": " + pixelSpread[i]);
		}
		
		DescriptiveStatistics countStats = new DescriptiveStatistics();
		for (int i = 0; i<256; i++) {
			countStats.addValue(pixelSpread[i]);
		}
		
		int startWhite = -1;
		int endWhite = -1;
		for (int i = 255; i>=0; i--) {
			if (startWhite < 0 && pixelSpread[i]>countStats.getMean())
				startWhite = i;
			if (startWhite >= 0 && endWhite < 0 && pixelSpread[i]<countStats.getMean()) {
				endWhite = i;
				break;
			}
		}

		LOG.debug("Start white: " + startWhite);
		LOG.debug("End white: " + endWhite);
		
		DescriptiveStatistics blackCountStats = new DescriptiveStatistics();
		DescriptiveStatistics blackSpread = new DescriptiveStatistics();
		for (int i = 0; i<=endWhite; i++) {
			blackCountStats.addValue(pixelSpread[i]);
			for (int j = 0; j<pixelSpread[i]; j++) {
				blackSpread.addValue(i);
			}
		}

		LOG.debug("mean counts: " + countStats.getMean());
		LOG.debug("mean black counts: " + blackCountStats.getMean());
		LOG.debug("std dev black counts: " + blackCountStats.getStandardDeviation());
		
		int startBlack = -1;
		for (int i = 0; i < 256; i++) {
			if (pixelSpread[i] > blackCountStats.getMean()) {
				startBlack = i;
				break;
			}
		}
		LOG.debug("Start black: " + startBlack);
		
		this.setBlackLimit(startBlack);
		this.setWhiteLimit(startWhite);
		
		this.greyscaleMultiplier = (255.0 / (double) (whiteLimit - blackLimit));
		
		// use mean + 2 sigma to find the black threshold
		// we make the threshold high (darker) to put more pixels in the letter when analysing
		double blackthresholdCount = blackCountStats.getMean() + (2.0 * blackCountStats.getStandardDeviation());
		LOG.debug("blackthresholdCount: " + blackthresholdCount);
				
		int blackThresholdValue = endWhite;
		for (int i = endWhite; i >= startBlack; i--) {
			if (pixelSpread[i] < blackthresholdCount) {
				blackThresholdValue = i;
				break;
			}
		}
		LOG.debug("Black threshold value (old): " + blackThresholdValue);
		blackThreshold = (int) Math.round((blackThresholdValue - blackLimit) * greyscaleMultiplier);
		LOG.debug("Black threshold (old): " + blackThreshold);
		
		blackThresholdValue = (int) Math.round(blackSpread.getPercentile(60.0));
		LOG.debug("Black threshold value (new): " + blackThresholdValue);
		LOG.debug("Black spread 25 percentile: " + (int) Math.round(blackSpread.getPercentile(25.0)));
		LOG.debug("Black spread 50 percentile: " + (int) Math.round(blackSpread.getPercentile(50.0)));
		LOG.debug("Black spread 75 percentile: " + (int) Math.round(blackSpread.getPercentile(75.0)));

		blackThreshold = (int) Math.round((blackThresholdValue - blackLimit) * greyscaleMultiplier);
		LOG.debug("Black threshold (new): " + blackThreshold);
		
		// use mean + 1 sigma to find the separation threshold
		// we keep threshold low (1 sigma) to encourage letter breaks
		double separationthresholdCount =  blackCountStats.getMean() + (1.0 * blackCountStats.getStandardDeviation());
		LOG.debug("Separation threshold value: " + separationthresholdCount);
		
		int separationThresholdValue = endWhite;
		for (int i = endWhite; i >= startBlack; i--) {
			if (pixelSpread[i] < separationthresholdCount) {
				separationThresholdValue = i;
				break;
			}
		}
		LOG.debug("Separation threshold value (old): " + separationThresholdValue);
		
		separationThresholdValue = (int) Math.round(blackSpread.getPercentile(75.0));
		LOG.debug("Separation threshold value (new): " + separationThresholdValue);
		LOG.debug("Black spread 25 percentile: " + (int) Math.round(blackSpread.getPercentile(25.0)));
		LOG.debug("Black spread 50 percentile: " + (int) Math.round(blackSpread.getPercentile(50.0)));
		LOG.debug("Black spread 75 percentile: " + (int) Math.round(blackSpread.getPercentile(75.0)));
		
		separationThreshold = (int) Math.round((separationThresholdValue - blackLimit) * greyscaleMultiplier);
		LOG.debug("Separation threshold: " + separationThreshold);
		if (drawPixelSpread)
			this.drawChart(pixelSpread, countStats, blackCountStats, blackSpread, startWhite, endWhite, startBlack, blackThresholdValue);

	}
	
	private void drawChart(int[] pixelSpread,
			DescriptiveStatistics countStats,
			DescriptiveStatistics blackCountStats,
			DescriptiveStatistics blackSpread,
			int startWhite, int endWhite, int startBlack, int blackThresholdValue) {
		XYSeries xySeries = new XYSeries("Brightness data");
		double maxSpread = 0;
		for (int i = 0; i<256; i++) {
			xySeries.add(i, pixelSpread[i]);
			if (pixelSpread[i]>maxSpread)
				maxSpread = pixelSpread[i];
		}
		
		XYSeries keyValues = new XYSeries("Key values");

		
		XYSeries counts = new XYSeries("Counts");
		
		counts.add(10.0, countStats.getMean());
		counts.add(100.0, blackCountStats.getMean());
		counts.add(125.0, blackCountStats.getMean() + (1.0 * blackCountStats.getStandardDeviation()));
		counts.add(150.0, blackCountStats.getMean() + (2.0 * blackCountStats.getStandardDeviation()));
		counts.add(175.0, blackCountStats.getMean() + (3.0 * blackCountStats.getStandardDeviation()));
		counts.add(75.0, blackCountStats.getMean() - (1.0 * blackCountStats.getStandardDeviation()));
		counts.add(50.0, blackCountStats.getMean() - (2.0 * blackCountStats.getStandardDeviation()));
		counts.add(25.0, blackCountStats.getMean() - (3.0 * blackCountStats.getStandardDeviation()));
		keyValues.add(startWhite, maxSpread/4.0);
		keyValues.add(endWhite, maxSpread/4.0);
		keyValues.add(startBlack, maxSpread/4.0);
		keyValues.add(blackThresholdValue, maxSpread/2.0);
		
		XYSeriesCollection dataset = new XYSeriesCollection(xySeries); 
		dataset.addSeries(keyValues);
		dataset.addSeries(counts);	
		final ValueAxis xAxis = new NumberAxis("Brightness");
		final ValueAxis yAxis = new NumberAxis("Count");
		yAxis.setUpperBound(maxSpread);
		xAxis.setUpperBound(255.0);	
		
        //final XYItemRenderer  renderer = new XYBarRenderer();
		final XYBarRenderer renderer = new XYBarRenderer();
		renderer.setShadowVisible(false);
        final XYPlot          plot     = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        String title = "BrightnessChart";
        final JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint( Color.white );
        File file = new File("data/" + title + ".png");
        try {
			ChartUtilities.saveChartAsPNG(file, chart, 600, 600 );
		} catch (IOException e) {
			e.printStackTrace();
			throw new JochreException(e);
		}   
	}
	
	public List<RowOfShapes> getRows() {
		if (rows==null) {
			rows = new ArrayList<RowOfShapes>();
		}
		return rows;
	}
	
	public void addRow(RowOfShapes row) {
		RowOfShapesInternal theRow = (RowOfShapesInternal) row;
		theRow.setContainer(this);
		this.getRows().add(row);
	}
	
	public void replaceRow(RowOfShapes row, List<RowOfShapes> newRows) {
		int rowIndex = this.getRows().indexOf(row);
		for (RowOfShapes newRow : newRows) {
			RowOfShapesInternal theRow = (RowOfShapesInternal) newRow;
			theRow.setContainer(this);
			this.getRows().add(rowIndex, newRow);
		}
		this.getRows().remove(row);
	}
	

	@Override
	public void removeRow(RowOfShapes row) {
		this.getRows().remove(row);
	}

	@Override
	public Set<Set<RowOfShapes>> getRowClusters() {
		if (rowClusters==null) {
			Mean heightMean = new Mean();
			StandardDeviation heightStdDev = new StandardDeviation();
			List<double[]> rowHeights = new ArrayList<double[]>(this.getRows().size());
			for (RowOfShapes row : this.getRows()) {
				Shape shape = row.getShapes().iterator().next();
				int height = shape.getBaseLine() - shape.getMeanLine();
				rowHeights.add(new double[] { height });
				heightMean.increment(height);
				heightStdDev.increment(height);
			}
			
			double stdDevHeight = heightStdDev.getResult();
			List<RowOfShapes> rows = new ArrayList<RowOfShapes>(this.getRows());
			DBSCANClusterer<RowOfShapes> clusterer = new DBSCANClusterer<RowOfShapes>(rows, rowHeights);
			rowClusters = clusterer.cluster(stdDevHeight, 2, true);	
			LOG.debug("Found " + rowClusters.size() + " row clusters.");
		}
		return rowClusters;
	}

	@Override
	public Locale getLocale() {
		return this.getPage().getDocument().getLocale();
	}

	@Override
	public double getAverageShapeWidth() {
		this.calculateShapeStatistics();
		return this.averageShapeWidth;
	}

	@Override
	public double getAverageShapeWidthMargin() {
		this.calculateShapeStatistics();
		return this.averageShapeWidthMargin;
	}

	@Override
	public double getAverageShapeHeight() {
		this.calculateShapeStatistics();
		return this.averageShapeHeight;
	}

	@Override
	public double getAverageShapeHeightMargin() {
		this.calculateShapeStatistics();
		return this.averageShapeHeightMargin;
	}
	
	void calculateShapeStatistics() {
		if (!shapeStatisticsCalculated) {
			DescriptiveStatistics shapeWidthStats = new DescriptiveStatistics();
			DescriptiveStatistics shapeHeightStats = new DescriptiveStatistics();
			
			for (RowOfShapes row : this.getRows()) {
				for (Shape shape : row.getShapes()) {
					shapeWidthStats.addValue(shape.getWidth());
					shapeHeightStats.addValue(shape.getHeight());
				}
			}
			
			double minWidth = shapeWidthStats.getPercentile(50);
			double maxWidth = shapeWidthStats.getPercentile(80);
			double minHeight = shapeHeightStats.getPercentile(50);
			double maxHeight = shapeHeightStats.getPercentile(80);
			this.averageShapeWidth = shapeWidthStats.getPercentile(65);
			this.averageShapeHeight = shapeHeightStats.getPercentile(65);

			this.averageShapeWidthMargin = (maxWidth - minWidth) / 2.0;
			this.averageShapeHeightMargin = (maxHeight - minHeight) / 2.0;
			
			this.shapeStatisticsCalculated = true;
		}
	}
	
	/**
	 * Returns the slope of the current image's horizontal inclination.
	 * Assumes an initial stab has already been made at group shapes into rows,
	 * and that rows are grouped from top to bottom.
	 * @return
	 */
	public double getInclination() {
		LOG.debug("#### getInclination ####");
		// It may well be that rows have been grouped together
		// wrongly if the image has several columns.
		// The only rows that are fairly reliable are very long horizontal bars
		// and the first long row, in which all letters are aligned to the same baseline,
		// regardless of their size.
		
		// let's get the medium width first
		Mean widthMean = new Mean();
		for (RowOfShapes row : this.getRows()) {
			widthMean.increment(row.getRight() - row.getLeft());
		}
		double meanWidth = widthMean.getResult();
		LOG.debug("meanWidth: " + meanWidth);
		
		double minWidth = meanWidth * 0.75;
		
		// find the first row with a pretty wide width
		RowOfShapes theRow = null;
		for (RowOfShapes row : this.getRows()) {
			int width = row.getRight() - row.getLeft();
			if (width > minWidth) {
				theRow = row;
				break;
			}
		}
		
		// calculate a regression for average shapes on this row
		double minHeight = theRow.getAverageShapeHeight() - theRow.getAverageShapeHeightMargin();
		double maxHeight = theRow.getAverageShapeHeight() + theRow.getAverageShapeHeightMargin();
		SimpleRegression regression = new SimpleRegression();
		for (Shape shape : theRow.getShapes()) {
			if (shape.getHeight() >= minHeight && shape.getHeight() <= maxHeight ) {
				for (int x = 0; x < shape.getWidth(); x++) {
					for (int y = 0; y < shape.getHeight(); y++) {
						if (shape.isPixelBlack(x, y, this.getBlackThreshold())) {
							regression.addData(shape.getLeft() + x, shape.getTop() + y);
						}
					}
				}
			}
		}
		
		return regression.getSlope();
	}
	
	@Override
	public List<Rectangle> getWhiteAreas(Set<Shape> shapes) {
		LOG.debug("#### getWhiteAreas ####");
		// Delimit area to be examined based on shapes
		int top=Integer.MAX_VALUE, bottom=0, left=Integer.MAX_VALUE, right=0;
		for (Shape shape : shapes) {
			if (shape.getTop()<top)
				top = shape.getTop();
			if (shape.getBottom()>bottom)
				bottom = shape.getBottom();
			if (shape.getLeft()<left)
				left = shape.getLeft();
			if (shape.getRight()>right)
				right = shape.getRight();
		}
		
		// get average shape width & height
		DescriptiveStatistics shapeWidthStats = new DescriptiveStatistics();
		DescriptiveStatistics shapeHeightStats = new DescriptiveStatistics();
		
		for (Shape shape : shapes) {
			shapeWidthStats.addValue(shape.getWidth());
			shapeHeightStats.addValue(shape.getHeight());
		}
		
		double averageShapeWidth = shapeWidthStats.getPercentile(75);
		double averageShapeHeight = shapeHeightStats.getPercentile(75);
		LOG.debug("averageShapeWidth: " + averageShapeWidth);
		LOG.debug("averageShapeHeight: " + averageShapeHeight);
		
		List<Rectangle> whiteAreas = new ArrayList<Rectangle>();
		
		// Horizontal white areas
		double minHorizontalWhiteAreaWidth = 40.0 * averageShapeWidth;
		double minHorizontalWhiteAreaHeight = 2.5 * averageShapeHeight;
		LOG.debug("minHorizontalWhiteAreaWidth: " + minHorizontalWhiteAreaWidth);
		LOG.debug("minHorizontalWhiteAreaHeight: " + minHorizontalWhiteAreaHeight);
		
		WhiteAreaFinder whiteAreaFinder = new WhiteAreaFinder();
		List<Rectangle> blackAreas = new ArrayList<Rectangle>();
		blackAreas.addAll(shapes);
		
		List<Rectangle> horizontalWhiteAreas = whiteAreaFinder.getWhiteAreas(blackAreas, left, top, right, bottom, minHorizontalWhiteAreaWidth, minHorizontalWhiteAreaHeight);
		// we add the horizontal white areas to the "black areas", since we don't want vertical
		// white areas detected at page top & page bottom, splitting a valid row
		blackAreas.addAll(horizontalWhiteAreas);		
		whiteAreas.addAll(horizontalWhiteAreas);
		
		// Long vertical white areas
		double minVerticalWhiteAreaWidth = 2.5 * averageShapeWidth;
		double minVerticalWhiteAreaHeight = 10.0 * averageShapeHeight;
		LOG.debug("minVerticalWhiteAreaWidth: " + minVerticalWhiteAreaWidth);
		LOG.debug("minVerticalWhiteAreaHeight: " + minVerticalWhiteAreaHeight);
		
		List<Rectangle> verticalWhiteAreas = whiteAreaFinder.getWhiteAreas(blackAreas, left, top, right, bottom, minVerticalWhiteAreaWidth, minVerticalWhiteAreaHeight);
		whiteAreas.addAll(verticalWhiteAreas);

		// Square white areas
		double minSquareWhiteAreaWidth = 4.0 * averageShapeWidth;
		double minSquareWhiteAreaHeight = 4.0 * averageShapeHeight;
		LOG.debug("minSquareWhiteAreaWidth: " + minSquareWhiteAreaWidth);
		LOG.debug("minSquareWhiteAreaHeight: " + minSquareWhiteAreaHeight);
		
		List<Rectangle> squareWhiteAreas = whiteAreaFinder.getWhiteAreas(blackAreas, left, top, right, bottom, minSquareWhiteAreaWidth, minSquareWhiteAreaHeight);
		whiteAreas.addAll(squareWhiteAreas);
		blackAreas.addAll(squareWhiteAreas);
		blackAreas.addAll(this.getWhiteAreasAroundLargeShapes(shapes));

		// Long narrow vertical white areas
		minVerticalWhiteAreaWidth = 1.0 * averageShapeWidth;
		minVerticalWhiteAreaHeight = 20.0 * averageShapeHeight;
		LOG.debug("minVerticalWhiteAreaWidth: " + minVerticalWhiteAreaWidth);
		LOG.debug("minVerticalWhiteAreaHeight: " + minVerticalWhiteAreaHeight);

		List<Rectangle> verticalWhiteAreas2 = whiteAreaFinder.getWhiteAreas(blackAreas, left, top, right, bottom, minVerticalWhiteAreaWidth, minVerticalWhiteAreaHeight);
		whiteAreas.addAll(verticalWhiteAreas2);


		return whiteAreas;
	}
	

	
	@Override
	public double getMeanHorizontalSlope() {
		if (!meanHorizontalSlopeCalculated) {
			// Calculate the average regression to be used for analysis
			Mean meanForSlope = new Mean();
			StandardDeviation stdDevForSlope = new StandardDeviation();
			List<SimpleRegression> regressions = new ArrayList<SimpleRegression>();
			for (RowOfShapes row : this.getRows()) {
				SimpleRegression regression = row.getRegression();
				// only include rows for which regression was really calculated (more than 2 points)
				if (regression.getN()>2) {
					meanForSlope.increment(regression.getSlope());
					stdDevForSlope.increment(regression.getSlope());
					regressions.add(regression);
				}
			}
			
			double slopeMean = 0.0;
			double slopeStdDev = 0.0;
			
			if (meanForSlope.getN()>0) {
				slopeMean = meanForSlope.getResult();
				slopeStdDev = stdDevForSlope.getResult();
			}
			LOG.debug("slopeMean: " + slopeMean);
			LOG.debug("slopeStdDev: " + slopeStdDev);
			
			if (regressions.size()>0) {
				double minSlope = slopeMean - slopeStdDev;
				double maxSlope = slopeMean + slopeStdDev;
				meanForSlope = new Mean();
				for (SimpleRegression regression : regressions) {
					if (minSlope <= regression.getSlope() && regression.getSlope() <= maxSlope) 
						meanForSlope.increment(regression.getSlope());
				}
				
				meanHorizontalSlope = meanForSlope.getResult();
			} else {
				meanHorizontalSlope = 0.0;
			}
			LOG.debug("meanHorizontalSlope: " + meanHorizontalSlope);
			meanHorizontalSlopeCalculated = true;
		}
		return meanHorizontalSlope;
	}

	public void recalculate() {
		this.shapeStatisticsCalculated = false;
		this.meanHorizontalSlopeCalculated = false;
	}

	@Override
	public List<Shape> getLargeShapes() {
		if (largeShapes==null)
			largeShapes = new ArrayList<Shape>();
		return largeShapes;
	}
	
	private List<Rectangle> getWhiteAreasAroundLargeShapes(Set<Shape> shapes) {
		List<Rectangle> whiteAreas = new ArrayList<Rectangle>(this.getLargeShapes().size());
		
		for (Shape largeShape : this.getLargeShapes()) {
			LOG.debug("Large shape: " + largeShape);
			int nearestAbove = 0;
			int nearestBelow = this.getHeight();
			int nearestRight = this.getWidth();
			int nearestLeft = 0;
			
			// start with extensions to the right and left
			for (Shape shape : shapes) {
				if (shape.getTop()<=largeShape.getBottom()&&shape.getBottom()>=largeShape.getTop()
						&& shape.getRight()<=largeShape.getLeft()&&shape.getRight()>=nearestLeft) {
					nearestLeft = shape.getRight();
				} else if (shape.getTop()<=largeShape.getBottom()&&shape.getBottom()>=largeShape.getTop()
						&& shape.getLeft()>=largeShape.getRight()&&shape.getLeft()<=nearestRight) {
					nearestRight = shape.getLeft();
				}
			}
			// extensions up and down
			for (Shape shape : shapes) {
				if (shape.getLeft()<=nearestRight&&shape.getRight()>=nearestLeft
						&& shape.getBottom()<=largeShape.getTop()&&shape.getBottom()>=nearestAbove) {
					nearestAbove = shape.getBottom();
				} else if (shape.getLeft()<=nearestRight&&shape.getRight()>=nearestLeft
						&& shape.getTop()>=largeShape.getBottom()&&shape.getTop()<=nearestBelow) {
					nearestBelow = shape.getTop();
				}
			}
			
			Rectangle whiteArea = new RectangleImpl(nearestLeft+1, nearestAbove+1, nearestRight-1, nearestBelow-1);
			whiteAreas.add(whiteArea);
			LOG.debug("White area: " + whiteArea);
		}
		return whiteAreas;
	}
	
	@Override
	public List<Rectangle> getWhiteAreasAroundLargeShapes() {
		if (whiteAreasAroundLargeShapes==null) {
			LOG.debug("getWhiteAreasAroundLargeShapes");
			List<Rectangle> whiteAreas = new ArrayList<Rectangle>(this.getLargeShapes().size());
			
			for (Shape largeShape : this.getLargeShapes()) {
				int nearestAbove = 0;
				int nearestBelow = this.getHeight();
				int nearestRight = this.getWidth();
				int nearestLeft = 0;
				
				// start with extensions to the right and left
				for (RowOfShapes row : this.getRows()) {
					if (row.getTop()<=largeShape.getBottom()&&row.getBottom()>=largeShape.getTop()
							&& row.getRight()<=largeShape.getLeft()&&row.getRight()>=nearestLeft) {
						nearestLeft = row.getRight();
					} else if (row.getTop()<=largeShape.getBottom()&&row.getBottom()>=largeShape.getTop()
							&& row.getLeft()>=largeShape.getRight()&&row.getLeft()<=nearestRight) {
						nearestRight = row.getLeft();
					}
				}
				
				// extensions up and down
				for (RowOfShapes row : this.getRows()) {
					if (row.getLeft()<=nearestRight&&row.getRight()>=nearestLeft
							&& row.getBottom()<=largeShape.getTop()&&row.getBottom()>=nearestAbove) {
						nearestAbove = row.getBottom();
					} else if (row.getLeft()<=nearestRight&&row.getRight()>=nearestLeft
							&& row.getTop()>=largeShape.getBottom()&&row.getTop()<=nearestBelow) {
						nearestBelow = row.getTop();
					}
				}		
				Rectangle whiteArea = new RectangleImpl(nearestLeft+1, nearestAbove+1, nearestRight-1, nearestBelow-1);
				whiteAreas.add(whiteArea);
			}
			for (Rectangle whiteArea : whiteAreas) {
				LOG.debug("White area: " + whiteArea);
			}
			this.whiteAreasAroundLargeShapes = whiteAreas;
		}
		return this.whiteAreasAroundLargeShapes;
	}
	
	public List<Rectangle> findColumnSeparators() {
		if (columnSeparators==null) {
			LOG.debug("############ findColumnSeparators ##############");
			double slope = this.getMeanHorizontalSlope();
			
			double imageMidPointX = (double) this.getWidth() / 2.0;
			
			int[] horizontalCounts = new int[this.getHeight()];
			DescriptiveStatistics rowXHeightStats = new DescriptiveStatistics();
			// first get the fill factor for each horizontal row in the image
			for (RowOfShapes row : this.getRows()) {
				rowXHeightStats.addValue(row.getXHeight());
				for (Shape shape : row.getShapes()) {
					double shapeMidPointX = (double) (shape.getLeft() + shape.getRight()) / 2.0;
					int slopeAdjustedTop = (int) Math.round(shape.getTop() + (slope * (shapeMidPointX - imageMidPointX)));	
					if (slopeAdjustedTop>=0 && slopeAdjustedTop < this.getHeight()) {
						for (int i = 0; i<shape.getHeight(); i++) {
							if (slopeAdjustedTop+i < horizontalCounts.length)
								horizontalCounts[slopeAdjustedTop+i] += shape.getWidth();
						}
					}
				}
			}
			DescriptiveStatistics horizontalStats = new DescriptiveStatistics();
			DescriptiveStatistics horizontalStatsNonEmpty = new DescriptiveStatistics();
			for (int i = 0; i<this.getHeight(); i++) {
	//			LOG.trace("Row " + i + ": " + horizontalCounts[i]);
				horizontalStats.addValue(horizontalCounts[i]);
				if (horizontalCounts[i]>0)
					horizontalStatsNonEmpty.addValue(horizontalCounts[i]);
			}
			LOG.debug("Mean horizontal count: " + horizontalStats.getMean());
			LOG.debug("Median horizontal count: " + horizontalStats.getPercentile(50));
			LOG.debug("25 percentile horizontal count: " + horizontalStats.getPercentile(25));
			LOG.debug("Mean horizontal count (non empty): " + horizontalStatsNonEmpty.getMean());
			LOG.debug("Median horizontal count (non empty): " + horizontalStatsNonEmpty.getPercentile(50));
			LOG.debug("25 percentile horizontal count (non empty): " + horizontalStatsNonEmpty.getPercentile(25));
			LOG.debug("10 percentile horizontal count (non empty): " + horizontalStatsNonEmpty.getPercentile(10));
			
			double maxEmptyRowCount = horizontalStatsNonEmpty.getMean() / 8.0;
			LOG.debug("maxEmptyRowCount: " + maxEmptyRowCount);
			
			boolean inEmptyHorizontalRange = false;
			List<int[]> emptyHorizontalRanges = new ArrayList<int[]>();
			int emptyHorizontalRangeStart = 0;
			for (int i = 0; i<this.getHeight(); i++) {
				if (!inEmptyHorizontalRange && horizontalCounts[i] <= maxEmptyRowCount) {
					inEmptyHorizontalRange = true;
					emptyHorizontalRangeStart = i;
				} else if (inEmptyHorizontalRange && horizontalCounts[i] > maxEmptyRowCount) {
					inEmptyHorizontalRange = false;
					emptyHorizontalRanges.add(new int[] {emptyHorizontalRangeStart, i});
				}
			}
			if (inEmptyHorizontalRange) {
				emptyHorizontalRanges.add(new int[] {emptyHorizontalRangeStart, this.getHeight()-1});
			}
			
			LOG.debug("rowXHeight mean: " + rowXHeightStats.getMean());
			LOG.debug("rowXHeight median: " + rowXHeightStats.getPercentile(50));
			double minHorizontalBreak = rowXHeightStats.getMean() * 2.0;
			LOG.debug("minHorizontalBreak: " + minHorizontalBreak);
			int smallBreakCount = 0;
			int mainTextTop = 0;
			int bigBreakCount = 0;
			for (int[] emptyHorizontalRange : emptyHorizontalRanges) {
				int height = emptyHorizontalRange[1]-emptyHorizontalRange[0];
				LOG.trace("empty range: " + emptyHorizontalRange[0] + ", " + emptyHorizontalRange[1] + " = " + height);
				if (bigBreakCount < 2 && smallBreakCount<2 && height > minHorizontalBreak) {
					mainTextTop = emptyHorizontalRange[1];
					bigBreakCount++;
				}
				if (height <= minHorizontalBreak)
					smallBreakCount++;
			}
			
			LOG.debug("mainTextTop:" + mainTextTop);
			// lift mainTextTop upwards by max an x-height or till we reach a zero row
			int minTop = mainTextTop - (int)(rowXHeightStats.getMean() / 2.0);
			if (minTop < 0) minTop = 0;
			for (int i = mainTextTop; i > minTop; i--) {
				mainTextTop = i;
				if (horizontalCounts[i]==0) {
					break;
				}
			}
			LOG.debug("mainTextTop (adjusted):" + mainTextTop);
			
			smallBreakCount = 0;
			bigBreakCount = 0;
			int mainTextBottom = this.getHeight();
			for (int i = emptyHorizontalRanges.size()-1; i>=0; i--) {
				int[] emptyHorizontalRange = emptyHorizontalRanges.get(i);
				int height = emptyHorizontalRange[1]-emptyHorizontalRange[0];
				LOG.trace("emptyHorizontalRange: " + emptyHorizontalRange[0] + ", height: " + height + ", bigBreakCount: " + bigBreakCount + ", smallBreakCount: " + smallBreakCount);
				if ((bigBreakCount+smallBreakCount)<=2 && height > minHorizontalBreak) {
					mainTextBottom = emptyHorizontalRange[0];
					LOG.trace("Set mainTextBottom to " + mainTextBottom);
					bigBreakCount++;
				}
				if (height <= minHorizontalBreak)
					smallBreakCount++;
				if ((bigBreakCount+smallBreakCount)>2)
					break;
			}
			LOG.debug("mainTextBottom:" + mainTextBottom);
			// lower mainTextBottom downwards by max an x-height or till we reach a zero row
			int maxBottom = mainTextBottom + (int)(rowXHeightStats.getMean() / 2.0);
			if (maxBottom > this.getHeight())
				maxBottom = this.getHeight();
			for (int i = mainTextBottom; i < maxBottom; i++) {
				mainTextBottom = i;
				if (horizontalCounts[i]==0) {
					break;
				}
			}
			LOG.debug("mainTextBottom (adjusted):" + mainTextBottom);
			
			int[] verticalCounts = new int[this.getWidth()];
			// first get the fill factor for each horizontal row in the image
			for (RowOfShapes row : this.getRows()) {
				for (Shape shape : row.getShapes()) {
					int slopeAdjustedLeft = (int) Math.round(shape.getLeft() - row.getXAdjustment());
					double shapeMidPointX = (double) (shape.getLeft() + shape.getRight()) / 2.0;
					int slopeAdjustedTop = (int) Math.round(shape.getTop() + (slope * (shapeMidPointX - imageMidPointX)));	
					if (slopeAdjustedTop>=mainTextTop && slopeAdjustedTop <= mainTextBottom
							&& slopeAdjustedLeft >=0 && slopeAdjustedLeft < this.getWidth()) {
						for (int i = 0; i<shape.getWidth(); i++) {
							if (slopeAdjustedLeft+i<this.getWidth())
								verticalCounts[slopeAdjustedLeft+i] += shape.getHeight();
						}
					}
				}
			}
	
			DescriptiveStatistics verticalStats = new DescriptiveStatistics();
			DescriptiveStatistics verticalStatsNonEmpty = new DescriptiveStatistics();
			for (int i = 0; i<this.getWidth(); i++) {
	//			LOG.trace("Column " + i + ": " + verticalCounts[i]);
				verticalStats.addValue(verticalCounts[i]);
				if (verticalCounts[i]>0)
					verticalStatsNonEmpty.addValue(verticalCounts[i]);
			}
			LOG.debug("Mean vertical count: " + verticalStats.getMean());
			LOG.debug("Median vertical count: " + verticalStats.getPercentile(50));
			LOG.debug("25 percentile vertical count: " + verticalStats.getPercentile(25));
			LOG.debug("Mean vertical count (non empty): " + verticalStatsNonEmpty.getMean());
			LOG.debug("Median vertical count (non empty): " + verticalStatsNonEmpty.getPercentile(50));
			LOG.debug("25 percentile vertical count (non empty): " + verticalStatsNonEmpty.getPercentile(25));
			LOG.debug("10 percentile vertical count (non empty): " + verticalStatsNonEmpty.getPercentile(10));
			LOG.debug("1 percentile vertical count (non empty): " + verticalStatsNonEmpty.getPercentile(1));
	
//			double maxEmptyColumnCount = verticalStatsNonEmpty.getMean() / 8.0;
			double maxEmptyColumnCount = verticalStatsNonEmpty.getPercentile(1);
			LOG.debug("maxEmptyColumnCount: " + maxEmptyColumnCount);
			
			boolean inEmptyVerticalRange = false;
			List<int[]> emptyVerticalRanges = new ArrayList<int[]>();
			int emptyVerticalRangeStart = 0;
			for (int i = 0; i<this.getWidth(); i++) {
				if (!inEmptyVerticalRange && verticalCounts[i] <= maxEmptyColumnCount) {
					inEmptyVerticalRange = true;
					emptyVerticalRangeStart = i;
				} else if (inEmptyVerticalRange && verticalCounts[i] > maxEmptyColumnCount) {
					inEmptyVerticalRange = false;
					emptyVerticalRanges.add(new int[] {emptyVerticalRangeStart, i});
				}
			}
			if (inEmptyVerticalRange) {
				emptyVerticalRanges.add(new int[] {emptyVerticalRangeStart, this.getWidth()-1});
			}
			
			LOG.debug("rowXHeight mean: " + rowXHeightStats.getMean());
			LOG.debug("rowXHeight median: " + rowXHeightStats.getPercentile(50));
			double minVerticalBreak = rowXHeightStats.getPercentile(50) * 1.0;
			LOG.debug("minVerticalBreak: " + minVerticalBreak);
	
			List<int[]> columnBreaks = new ArrayList<int[]>();
			for (int[] emptyVerticalRange : emptyVerticalRanges) {
				int width = emptyVerticalRange[1]-emptyVerticalRange[0];
				LOG.trace("empty range: " + emptyVerticalRange[0] + ", " + emptyVerticalRange[1] + " = " + width);
				
				if (width >= minVerticalBreak) {
					columnBreaks.add(emptyVerticalRange);
					LOG.trace("Found column break!");
				}
			}
			
			columnSeparators = new ArrayList<Rectangle>();
			for (int[] columnBreak : columnBreaks) {
				// reduce the column break to the thickest empty area if possible
				int[] bestColumnBreak = null;
				double originalCount = maxEmptyColumnCount;
				maxEmptyColumnCount = 0;
				while (bestColumnBreak == null && maxEmptyColumnCount <= originalCount) {
					inEmptyVerticalRange = false;
					emptyVerticalRanges = new ArrayList<int[]>();
					emptyVerticalRangeStart = columnBreak[0];
					for (int i = columnBreak[0]; i<=columnBreak[1]; i++) {
						if (!inEmptyVerticalRange && verticalCounts[i] <= maxEmptyColumnCount) {
							inEmptyVerticalRange = true;
							emptyVerticalRangeStart = i;
						} else if (inEmptyVerticalRange && verticalCounts[i] > maxEmptyColumnCount) {
							inEmptyVerticalRange = false;
							emptyVerticalRanges.add(new int[] {emptyVerticalRangeStart, i});
						}
					}
					if (inEmptyVerticalRange) {
						emptyVerticalRanges.add(new int[] {emptyVerticalRangeStart, columnBreak[1]});
					}
					
					for (int[] emptyVerticalRange : emptyVerticalRanges) {
						if (bestColumnBreak==null||(emptyVerticalRange[1]-emptyVerticalRange[0]>bestColumnBreak[1]-bestColumnBreak[0]))
							bestColumnBreak = emptyVerticalRange;
					}
					maxEmptyColumnCount += (originalCount / 8.0);
				}
				
				if (bestColumnBreak==null)
					bestColumnBreak = columnBreak;
				
				Rectangle whiteArea = new RectangleImpl(bestColumnBreak[0], mainTextTop, bestColumnBreak[1], mainTextBottom);
				columnSeparators.add(whiteArea);
				LOG.debug("ColumnBreak: " + whiteArea);
			} // next column break
		}
		return columnSeparators;
	}

	@Override
	public double getXAdjustment(double yCoordinate) {
		// determine the vertical slope for adjusting the line accordingly
		boolean infiniteSlope = false;
		if (this.getMeanHorizontalSlope()==0)
			infiniteSlope = true;
		double meanVerticalSlope = 0;
		if (!infiniteSlope) {
			meanVerticalSlope = 0 - (1 / this.getMeanHorizontalSlope());
		}
		
		double xAdjustment = 0;
		if (!infiniteSlope) {
			xAdjustment = yCoordinate / meanVerticalSlope;
		}
		return xAdjustment;
	}

	@Override
	public int getShapeCount() {
		if (myShapeCount<0) {
			myShapeCount = 0;
			for (RowOfShapes row : this.getRows()) {
				for (GroupOfShapes group : row.getGroups()) {
					myShapeCount += group.getShapes().size();
				}
			}
		}
		return myShapeCount;
	}

	@Override
	public boolean isDrawPixelSpread() {
		return drawPixelSpread;
	}

	@Override
	public void setDrawPixelSpread(boolean drawPixelSpread) {
		this.drawPixelSpread = drawPixelSpread;
	}

	@Override
	public void restoreOriginalImage() {
		this.setOriginalImage(imageBackup);
		this.calculateThresholds(false);
	}
}
