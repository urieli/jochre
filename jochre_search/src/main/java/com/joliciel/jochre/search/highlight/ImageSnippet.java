///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
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
package com.joliciel.jochre.search.highlight;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.search.CoordinateStorage;
import com.joliciel.jochre.search.Rectangle;
import com.joliciel.talismane.utils.LogUtils;

public class ImageSnippet {
	private static final Log LOG = LogFactory.getLog(ImageSnippet.class);
	private static String[] imageExtensions = new String[] {"png","jpg","jpeg","gif","tiff"};
	private File directory;
	private String baseName;
	private CoordinateStorage coordinateStorage;
	private Snippet snippet;
	private Rectangle rectangle;
	private List<Rectangle> highlights;
	
	public ImageSnippet(CoordinateStorage coordinateStorage, Snippet snippet, File directory, String baseName) {
		this.coordinateStorage = coordinateStorage;
		this.snippet = snippet;
		this.directory = directory;
		this.baseName = baseName;
		this.initialize();
	}
	
	private void initialize() {
		rectangle = new Rectangle(coordinateStorage.getRowCoordinates(snippet.getStartOffset()));
		if (LOG.isDebugEnabled())
			LOG.debug("start offset " + snippet.getStartOffset() + " rect: " + rectangle.toString());
		Rectangle endOffsetRect = coordinateStorage.getRowCoordinates(snippet.getEndOffset()-1);
		if (LOG.isDebugEnabled())
			LOG.debug("end offset " + snippet.getEndOffset() + " rect : " + endOffsetRect.toString());
		rectangle.expand(endOffsetRect);
		if (LOG.isDebugEnabled())
			LOG.debug("new rect: " + rectangle.toString());
		
		rectangle.setLeft(rectangle.getLeft()-5);
		rectangle.setTop(rectangle.getTop()-5);
		rectangle.setRight(rectangle.getRight()+5);
		rectangle.setBottom(rectangle.getBottom()+5);
		
		highlights = new ArrayList<Rectangle>();
		for (HighlightTerm term : snippet.getHighlightTerms()) {
			List<Rectangle> termRects = coordinateStorage.getRectangles(term.getStartOffset());
			if (termRects==null) {
				throw new RuntimeException("No rectangles for offset " + term.getStartOffset());
			}
			for (Rectangle termRect : termRects) {
				highlights.add(termRect);
			}
		}
	}
	
	public Rectangle getRectangle() {
		return rectangle;
	}

	public List<Rectangle> getHighlights() {
		return highlights;
	}

	public BufferedImage getImage() {
		try {
			File imageFile = null;
			for (String imageExtension : imageExtensions) {
				imageFile = new File(directory, baseName + "." + imageExtension);
				if (imageFile.exists())
					break;
				imageFile = null;
			}
			if (imageFile==null)
				throw new RuntimeException("No image found in directory " + directory.getAbsolutePath() + ", baseName " + baseName);
			
			BufferedImage originalImage = ImageIO.read(imageFile);
			BufferedImage imageSnippet = new BufferedImage(this.rectangle.getWidth(), this.rectangle.getHeight(), BufferedImage.TYPE_INT_ARGB);
			originalImage = originalImage.getSubimage(this.rectangle.getLeft(), this.rectangle.getTop(), this.rectangle.getWidth(), this.rectangle.getHeight());
			Graphics2D graphics2D = imageSnippet.createGraphics();
			graphics2D.drawImage(originalImage, 0, 0, this.rectangle.getWidth(), this.rectangle.getHeight(), null);
			int extra=2;
			for (Rectangle rect : this.highlights) {
				graphics2D.setStroke(new BasicStroke(1));
				graphics2D.setPaint(Color.BLACK);
				graphics2D.drawRect(rect.getLeft()-this.rectangle.getLeft()-extra, rect.getTop()-this.rectangle.getTop()-extra, rect.getWidth() + (extra*2), rect.getHeight() + (extra*2));
				graphics2D.setColor(new Color(255, 255, 0, 127));
				graphics2D.fillRect(rect.getLeft()-this.rectangle.getLeft()-extra, rect.getTop()-this.rectangle.getTop()-extra, rect.getWidth() + (extra*2), rect.getHeight() + (extra*2));
			}
			return imageSnippet;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
}
