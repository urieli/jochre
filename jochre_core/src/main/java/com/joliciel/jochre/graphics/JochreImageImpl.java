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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.doc.DocumentService;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.util.ImagePixelGrabber;
import com.joliciel.jochre.graphics.util.ImagePixelGrabberImpl;
import com.joliciel.jochre.security.User;
import com.joliciel.talismane.utils.Monitorable;
import com.joliciel.talismane.utils.ProgressMonitor;
import com.joliciel.talismane.utils.SimpleProgressMonitor;

class JochreImageImpl implements JochreImageInternal, Monitorable {
	private static final Logger LOG = LoggerFactory.getLogger(JochreImageImpl.class);
	private int id;
	int blackThreshold;
	int separationThreshold;
	String name;
	int width;
	int height;
	List<Paragraph> paragraphs;
	int pageId;
	JochrePage page;
	int index;
	int whiteLimit;
	int blackLimit;
	private int ownerId;
	private User owner;

	int[] normalizedBrightnessValues;
	int whiteGapFillFactor;
	double averageRowHeight = 0;
	boolean originalImageChanged = false;
	private BufferedImage originalImage = null;
	int shapeCount = -1;
	private ImagePixelGrabber pixelGrabber;

	private double confidence = -1;

	GraphicsServiceInternal graphicsService;
	DocumentService documentService;

	ImageStatus imageStatus;

	private Map<String, Shape> shapeMap = null;
	SimpleProgressMonitor currentMonitor = null;
	int shapesSaved = 0;

	JochreImageImpl() {
	}

	public JochreImageImpl(BufferedImage originalImage) {
		this.originalImage = originalImage;
	}

	ImagePixelGrabber getPixelGrabber() {
		if (this.pixelGrabber == null) {
			this.pixelGrabber = new ImagePixelGrabberImpl(this.getOriginalImage());
		}
		return this.pixelGrabber;
	}

	@Override
	public int getAbsolutePixel(int x, int y) {
		int brightness = this.getRawAbsolutePixel(x, y);
		// brightness now gives us a greyscale value
		// all we need to do is normalise it
		return this.normalize(brightness);
	}

	@Override
	public int getPixel(int x, int y) {
		return this.getAbsolutePixel(x, y);
	}

	@Override
	public int getRawPixel(int x, int y) {
		return this.getRawAbsolutePixel(x, y);
	}

	@Override
	public int getRawAbsolutePixel(int x, int y) {
		return this.getPixelGrabber().getPixelBrightness(x, y);
	}

	@Override
	public boolean isPixelBlack(int x, int y, int threshold) {
		if (x < 0 || y < 0 || x >= this.getWidth() || y >= this.getHeight())
			return false;

		if (this.getPixel(x, y) <= threshold)
			return true;
		else
			return false;
	}

	@Override
	public int getBlackThreshold() {
		return blackThreshold;
	}

	@Override
	public void setBlackThreshold(int blackThreshold) {
		this.blackThreshold = blackThreshold;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public void setWidth(int width) {
		this.width = width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public List<Paragraph> getParagraphs() {
		if (paragraphs == null) {
			if (this.id == 0)
				paragraphs = new ArrayList<Paragraph>();
			else
				paragraphs = graphicsService.findParagraphs(this);
		}
		return paragraphs;
	}

	public GraphicsServiceInternal getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsServiceInternal graphicsService) {
		this.graphicsService = graphicsService;
	}

	@Override
	public void save() {
		if (this.currentMonitor != null)
			this.currentMonitor.setCurrentAction("imageMonitor.savingImage");
		if (this.pageId == 0 && this.page != null)
			this.pageId = this.page.getId();
		this.graphicsService.saveJochreImage(this);
		if (this.paragraphs != null) {
			int index = 0;
			for (Paragraph paragraph : this.paragraphs) {
				paragraph.setIndex(index++);
				paragraph.save();
			}
		}

		if (this.originalImageChanged) {
			this.graphicsService.saveOriginalImage(this);
		}
	}

	@Override
	public Paragraph newParagraph() {
		ParagraphInternal paragraph = graphicsService.getEmptyParagraphInternal();
		this.getParagraphs().add(paragraph);
		paragraph.setImage(this);
		return paragraph;
	}

	@Override
	public int getPageId() {
		return pageId;
	}

	@Override
	public void setPageId(int pageId) {
		this.pageId = pageId;
	}

	@Override
	public JochrePage getPage() {
		if (this.page == null && this.pageId != 0)
			this.page = this.documentService.loadJochrePage(this.pageId);
		return page;
	}

	@Override
	public void setPage(JochrePage page) {
		this.page = page;
		this.pageId = page.getId();
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public int getSeparationThreshold() {
		return separationThreshold;
	}

	@Override
	public void setSeparationThreshold(int separationThreshold) {
		this.separationThreshold = separationThreshold;
	}

	@Override
	public int getWhiteLimit() {
		return whiteLimit;
	}

	@Override
	public void setWhiteLimit(int whiteLimit) {
		this.whiteLimit = whiteLimit;
	}

	@Override
	public int getBlackLimit() {
		return blackLimit;
	}

	@Override
	public void setBlackLimit(int blackLimit) {
		this.blackLimit = blackLimit;
	}

	@Override
	public final int normalize(int brightness) {
		if (normalizedBrightnessValues == null) {
			normalizedBrightnessValues = new int[256];
			double greyscaleMultiplier = (255.0 / (whiteLimit - blackLimit));
			for (int i = 0; i < 256; i++) {
				if (i < blackLimit)
					normalizedBrightnessValues[i] = 0;
				if (i > whiteLimit)
					normalizedBrightnessValues[i] = 255;
				normalizedBrightnessValues[i] = (int) Math.round((i - blackLimit) * greyscaleMultiplier);
			}
		}

		return normalizedBrightnessValues[brightness];
	}

	@Override
	public int getWhiteGapFillFactor() {
		return whiteGapFillFactor;
	}

	@Override
	public void setWhiteGapFillFactor(int whiteGapFillFactor) {
		this.whiteGapFillFactor = whiteGapFillFactor;
	}

	@Override
	public ImageStatus getImageStatus() {
		return imageStatus;
	}

	@Override
	public void setImageStatus(ImageStatus imageStatus) {
		this.imageStatus = imageStatus;
	}

	@Override
	public void clearMemory() {
		this.paragraphs = null;
		this.originalImage = null;
		this.pixelGrabber = null;
		this.shapeMap = null;
		System.gc();
	}

	public void recalculate() {
		this.averageRowHeight = 0;
	}

	@Override
	public double getAverageRowHeight() {
		if (averageRowHeight == 0) {
			DescriptiveStatistics rowHeightStats = new DescriptiveStatistics();
			for (Paragraph paragraph : this.getParagraphs()) {
				for (RowOfShapes row : paragraph.getRows()) {
					int height = row.getXHeight();
					rowHeightStats.addValue(height);
				}
			}
			averageRowHeight = rowHeightStats.getPercentile(50);
			LOG.debug("averageRowHeight: " + averageRowHeight);
		}
		return averageRowHeight;
	}

	@Override
	public BufferedImage getOriginalImage() {
		if (this.originalImage == null) {
			this.getGraphicsService().loadOriginalImage(this);
		}
		return originalImage;
	}

	@Override
	public void setOriginalImage(BufferedImage originalImage) {
		this.originalImage = originalImage;
		originalImageChanged = true;
	}

	@Override
	public void setOriginalImageDB(BufferedImage originalImage) {
		this.originalImage = originalImage;
	}

	public DocumentService getDocumentService() {
		return documentService;
	}

	public void setDocumentService(DocumentService documentService) {
		this.documentService = documentService;
	}

	@Override
	public int getShapeCount() {
		if (shapeCount < 0) {
			shapeCount = this.graphicsService.getShapeCount(this);
		}
		return shapeCount;
	}

	public void setShapeCount(int shapeCount) {
		this.shapeCount = shapeCount;
	}

	@Override
	public int hashCode() {
		if (this.id == 0)
			return super.hashCode();
		else
			return ((Integer) this.getId()).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this.id == 0) {
			return super.equals(obj);
		} else {
			JochreImage other = (JochreImage) obj;
			return (this.getId() == other.getId());
		}
	}

	@Override
	public int getOwnerId() {
		return ownerId;
	}

	@Override
	public void setOwnerId(int ownerId) {
		this.ownerId = ownerId;
		this.owner = null;
	}

	@Override
	public User getOwner() {
		if (this.owner == null && this.ownerId != 0)
			this.owner = User.loadUser(this.ownerId);
		return owner;
	}

	@Override
	public void setOwner(User owner) {
		this.setOwnerId(owner.getId());
		this.owner = owner;
	}

	@Override
	public Shape getShape(int left, int top, int right, int bottom) {
		String key = left + "," + top + "," + right + "," + bottom;

		if (this.shapeMap == null)
			this.shapeMap = new HashMap<String, Shape>();
		Shape shape = this.shapeMap.get(key);
		if (shape == null) {
			ShapeInternal shapeInternal = this.graphicsService.getEmptyShapeInternal();
			shapeInternal.setJochreImage(this);
			shapeInternal.setLeft(left);
			shapeInternal.setTop(top);
			shapeInternal.setRight(right);
			shapeInternal.setBottom(bottom);
			this.shapeMap.put(key, shapeInternal);
			shape = shapeInternal;
		}
		return shape;
	}

	@Override
	public boolean isLeftToRight() {
		return this.getPage().getDocument().isLeftToRight();
	}

	@Override
	public ProgressMonitor monitorTask() {
		currentMonitor = new SimpleProgressMonitor();
		shapesSaved = 0;
		return currentMonitor;
	}

	@Override
	public void onSaveShape(Shape shape) {
		if (this.currentMonitor != null) {
			shapesSaved++;
			int shapeCount = this.getShapeCount();
			if (shapeCount == 0)
				shapeCount = 1;
			this.currentMonitor.setPercentComplete((double) shapesSaved / (double) shapeCount);
		}
	}

	@Override
	public double getConfidence() {
		if (confidence < 0) {
			confidence = 0;
			int count = 0;
			for (Paragraph paragraph : paragraphs) {
				for (RowOfShapes row : paragraph.getRows()) {
					for (GroupOfShapes group : row.getGroups()) {
						count++;
						confidence += Math.log(group.getConfidence());
					}
				}
			}
			if (count == 0) {
				confidence = 0;
			} else {
				confidence /= count;
				confidence = Math.exp(confidence);
			}
		}
		return confidence;
	}

	@Override
	public Rectangle getPrintSpace() {
		RectangleImpl printSpace = new RectangleImpl(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
		for (Paragraph paragraph : this.getParagraphs()) {
			if (!paragraph.isJunk()) {
				if (paragraph.getTop() < printSpace.getTop())
					printSpace.setTop(paragraph.getTop());
				if (paragraph.getLeft() < printSpace.getLeft())
					printSpace.setLeft(paragraph.getLeft());
				if (paragraph.getBottom() > printSpace.getBottom())
					printSpace.setBottom(paragraph.getBottom());
				if (paragraph.getRight() > printSpace.getRight())
					printSpace.setRight(paragraph.getRight());
			}
		}
		return printSpace;
	}

	@Override
	public void recalculateIndexes() {
		int iPar = 0;
		for (Paragraph par : this.getParagraphs()) {
			((ParagraphInternal) par).setIndex(iPar++);
			int iRow = 0;
			for (RowOfShapes row : par.getRows()) {
				((RowOfShapesInternal) row).setIndex(iRow++);
				int iGroup = 0;
				for (GroupOfShapes group : row.getGroups()) {
					((GroupOfShapesInternal) group).setIndex(iGroup++);
					int iShape = 0;
					for (Shape shape : group.getShapes()) {
						((ShapeInternal) shape).setIndex(iShape++);
					}
				}
			}
		}
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

}
