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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import com.joliciel.jochre.EntityImpl;
import com.joliciel.jochre.doc.DocumentService;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.util.ImagePixelGrabber;
import com.joliciel.jochre.graphics.util.ImagePixelGrabberImpl;
import com.joliciel.jochre.security.SecurityService;
import com.joliciel.jochre.security.User;

class JochreImageImpl extends EntityImpl implements JochreImageInternal {
    private static final Log LOG = LogFactory.getLog(JochreImageImpl.class);
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

	
	GraphicsServiceInternal graphicsService;
	DocumentService documentService;
	SecurityService securityService;
	
	ImageStatus imageStatus;
	
	private Map<String, Shape> shapeMap = null;

	JochreImageImpl() {	
	}
	
	public JochreImageImpl(BufferedImage originalImage) {
		this.originalImage = originalImage;
	}
	
	ImagePixelGrabber getPixelGrabber() {
		if (this.pixelGrabber==null) {
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

		if (this.getPixel(x, y)<=threshold)
			return true;
		else
			return false;
	}
	
	public int getBlackThreshold() {
		return blackThreshold;
	}
	public void setBlackThreshold(int blackThreshold) {
		this.blackThreshold = blackThreshold;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	@Override
	public List<Paragraph> getParagraphs() {
		if (paragraphs==null) {
			if (this.isNew())
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
	public void saveInternal() {
		if (this.pageId==0 && this.page!=null) this.pageId = this.page.getId();
		this.graphicsService.saveJochreImage(this);
		if (this.paragraphs!=null) {
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
		if (this.page==null && this.pageId!=0)
			this.page = this.documentService.loadJochrePage(this.pageId);
		return page;
	}
	
	public void setPage(JochrePage page) {
		this.page = page;
		this.pageId = page.getId();
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public int getSeparationThreshold() {
		return separationThreshold;
	}
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
		if (normalizedBrightnessValues==null) {
			normalizedBrightnessValues = new int[256];
			double greyscaleMultiplier = (255.0 / (double) (whiteLimit - blackLimit));
			for (int i=0;i<256;i++) {
				if (i < blackLimit)
					normalizedBrightnessValues[i]=0;
				if (i > whiteLimit)
					normalizedBrightnessValues[i]=255;
				normalizedBrightnessValues[i] = (int) Math.round((double)(i - blackLimit) * greyscaleMultiplier);
			}
		}

		return normalizedBrightnessValues[brightness];
	}
	public int getWhiteGapFillFactor() {
		return whiteGapFillFactor;
	}
	public void setWhiteGapFillFactor(int whiteGapFillFactor) {
		this.whiteGapFillFactor = whiteGapFillFactor;
	}
	public ImageStatus getImageStatus() {
		return imageStatus;
	}
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
	

	public BufferedImage getOriginalImage() {
		if (this.originalImage==null) {
			this.getGraphicsService().loadOriginalImage(this);
		}
		return originalImage;
	}
	public void setOriginalImage(BufferedImage originalImage) {
		this.originalImage = originalImage;
		originalImageChanged = true;
	}
	
	public void setOriginalImageDB(BufferedImage originalImage) {
		this.originalImage = originalImage;
	}
	public DocumentService getDocumentService() {
		return documentService;
	}
	public void setDocumentService(DocumentService documentService) {
		this.documentService = documentService;
	}
	public int getShapeCount() {
		if (shapeCount<0) {
			shapeCount = this.graphicsService.getShapeCount(this);
		}
		return shapeCount;
	}
	public void setShapeCount(int shapeCount) {
		this.shapeCount = shapeCount;
	}
	
	@Override
	public int hashCode() {
		if (this.isNew())
			return super.hashCode();
		else
			return ((Integer)this.getId()).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this.isNew()) {
			return super.equals(obj);
		} else {
			JochreImage other = (JochreImage) obj;
			return (this.getId()==other.getId());
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
		if (this.owner==null && this.ownerId!=0)
			this.owner = this.getSecurityService().loadUser(this.ownerId);
		return owner;
	}

	@Override
	public void setOwner(User owner) {
		this.setOwnerId(owner.getId());
		this.owner = owner;
	}
	public SecurityService getSecurityService() {
		return securityService;
	}
	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	@Override
	public Shape getShape(int left, int top, int right, int bottom) {
		String key = left + "," + top + "," + right + "," + bottom;
		
		if (this.shapeMap==null)
			this.shapeMap = new TreeMap<String, Shape>();
		Shape shape = this.shapeMap.get(key);
		if (shape==null) {
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

}
