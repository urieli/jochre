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
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochreIndexDocument;

public class ImageSnippet {
	private static final Logger LOG = LoggerFactory.getLogger(ImageSnippet.class);
	private Snippet snippet;
	private Rectangle rectangle;
	private List<Rectangle> highlights;
	private JochreIndexDocument jochreDoc;
	
	public ImageSnippet(JochreIndexDocument jochreDoc, Snippet snippet) {
		this.snippet = snippet;
		this.jochreDoc = jochreDoc;
		this.initialize();
	}
	
	private void initialize() {
		rectangle = snippet.getRectangle(jochreDoc);
		if (LOG.isDebugEnabled())
			LOG.debug("new rect: " + rectangle.toString());
		
		rectangle.grow(5, 5);
		
		highlights = new ArrayList<Rectangle>();
		for (HighlightTerm term : snippet.getHighlightTerms()) {
			Rectangle rect = term.getPayload().getRectangle();
			LOG.debug("Added highlight: " + rect.toString());
			highlights.add(rect);
			Rectangle secondaryRect = term.getPayload().getSecondaryRectangle();
			if (secondaryRect!=null) {
				LOG.debug("Added highlight: " + secondaryRect.toString());
				highlights.add(secondaryRect);
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
		BufferedImage originalImage = jochreDoc.getImage(snippet.getPageIndex());
		BufferedImage imageSnippet = new BufferedImage(this.rectangle.width, this.rectangle.height, BufferedImage.TYPE_INT_ARGB);
		originalImage = originalImage.getSubimage(this.rectangle.x, this.rectangle.y, this.rectangle.width, this.rectangle.height);
		Graphics2D graphics2D = imageSnippet.createGraphics();
		graphics2D.drawImage(originalImage, 0, 0, this.rectangle.width, this.rectangle.height, null);
		int extra=2;
		for (Rectangle rect : this.highlights) {
			graphics2D.setStroke(new BasicStroke(1));
			graphics2D.setPaint(Color.BLACK);
			graphics2D.drawRect(rect.x-this.rectangle.x-extra, rect.y-this.rectangle.y-extra, rect.width + (extra*2), rect.height + (extra*2));
			graphics2D.setColor(new Color(255, 255, 0, 127));
			graphics2D.fillRect(rect.x-this.rectangle.x-extra, rect.y-this.rectangle.y-extra, rect.width + (extra*2), rect.height + (extra*2));
		}
		return imageSnippet;
	}
}
