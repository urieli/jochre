package com.joliciel.jochre.search;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.SortedMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.search.JochreIndexTermLister.JochreTerm;
import com.joliciel.jochre.utils.JochreException;

class JochreIndexWordImpl implements JochreIndexWord {
	private static final Log LOG = LogFactory.getLog(JochreIndexWordImpl.class);
	private int startOffset;
	private JochreIndexDocument doc;
	private Rectangle rectangle;
	private Rectangle secondRectangle;
	private String text;
	private BufferedImage image;
	private Rectangle rowRectangle;
	private BufferedImage rowImage;
	private Rectangle secondRowRectangle;
	private BufferedImage secondRowImage;
	private JochreTerm jochreTerm;

	private SearchServiceInternal searchService;

	public JochreIndexWordImpl(JochreIndexDocument doc, int startOffset) {
		this.doc = doc;
		JochreIndexTermLister termLister = doc.getTermLister();
		NavigableMap<Integer, JochreTerm> termMap = termLister
				.getTextTermByOffset();

		jochreTerm = termMap.floorEntry(startOffset).getValue();
		if (jochreTerm == null)
			throw new JochreException("No term found at startoffset "
					+ startOffset + ", in doc " + doc.getName() + ", section "
					+ doc.getSectionNumber());

		if (LOG.isTraceEnabled()) {
			SortedMap<Integer, JochreTerm> ascendingMap = termMap
					.tailMap(startOffset);
			Iterator<Integer> ascendingKeys = ascendingMap.keySet().iterator();
			for (int i = 0; i < 5; i++) {
				if (ascendingKeys.hasNext()) {
					int key = ascendingKeys.next();
					LOG.trace(termMap.get(key));
				}
			}
		}
		this.startOffset = jochreTerm.getStart();
	}

	@Override
	public int getStartOffset() {
		return startOffset;
	}

	@Override
	public JochreIndexDocument getDocument() {
		return doc;
	}

	@Override
	public Rectangle getRectangle() {
		if (rectangle == null)
			rectangle = jochreTerm.getPayload().getRectangle();
		return rectangle;
	}

	@Override
	public Rectangle getSecondRectangle() {
		if (secondRectangle == null)
			secondRectangle = jochreTerm.getPayload().getSecondaryRectangle();
		return secondRectangle;
	}

	@Override
	public String getText() {
		if (this.text == null)
			text = doc.getContents()
					.substring(jochreTerm.start, jochreTerm.end);
		return text;
	}

	@Override
	public BufferedImage getImage() {
		this.getImages();
		return image;
	}

	@Override
	public Rectangle getRowRectangle() {
		if (rowRectangle == null) {
			rowRectangle = doc.getRowRectangle(jochreTerm.getPayload()
					.getPageIndex(), jochreTerm.getPayload().getRowIndex());
		}
		return rowRectangle;
	}

	@Override
	public BufferedImage getRowImage() {
		this.getImages();
		return rowImage;
	}

	@Override
	public Rectangle getSecondRowRectangle() {
		if (secondRowRectangle == null) {
			if (jochreTerm.getPayload().getSecondaryRectangle() != null) {
				secondRowRectangle = doc.getRowRectangle(jochreTerm
						.getPayload().getPageIndex(), jochreTerm.getPayload()
						.getRowIndex() + 1);
			}
		}
		return secondRowRectangle;
	}

	@Override
	public BufferedImage getSecondRowImage() {
		this.getImages();
		return secondRowImage;
	}

	private void getImages() {
		if (this.image == null) {
			int pageIndex = jochreTerm.getPayload().getPageIndex();
			BufferedImage originalImage = doc.getImage(pageIndex);
			Rectangle rect = jochreTerm.getPayload().getRectangle();
			image = originalImage.getSubimage(rect.x, rect.y, rect.width,
					rect.height);

			Rectangle secondaryRect = jochreTerm.getPayload()
					.getSecondaryRectangle();
			if (secondaryRect != null) {
				BufferedImage secondSnippet = originalImage.getSubimage(
						secondaryRect.x, secondaryRect.y, secondaryRect.width,
						secondaryRect.height);
				if (searchService.isLeftToRight())
					image = joinBufferedImage(image, secondSnippet);
				else
					image = joinBufferedImage(secondSnippet, image);
			}

			Rectangle rowRect = this.getRowRectangle();
			rowImage = originalImage.getSubimage(rowRect.x, rowRect.y,
					rowRect.width, rowRect.height);

			Rectangle secondRowRect = this.getSecondRectangle();
			if (secondRowRect != null) {
				secondRowImage = originalImage.getSubimage(secondRowRect.x,
						secondRowRect.y, secondRowRect.width,
						secondRowRect.height);
			}
		}
	}

	@Override
	public int getPageIndex() {
		return this.jochreTerm.getPayload().getPageIndex();
	}

	/**
	 * From http://stackoverflow.com/questions/20826216/copy-two-buffered-image-
	 * into-one-image-side-by-side
	 */
	public static BufferedImage joinBufferedImage(BufferedImage img1,
			BufferedImage img2) {
		// do some calculations first
		int offset = 5;
		int wid = img1.getWidth() + img2.getWidth() + offset;
		int height = Math.max(img1.getHeight(), img2.getHeight()) + offset;
		// create a new buffer and draw two images into the new image
		BufferedImage newImage = new BufferedImage(wid, height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = newImage.createGraphics();
		Color oldColor = g2.getColor();
		// fill background
		g2.setPaint(Color.WHITE);
		g2.fillRect(0, 0, wid, height);
		// draw image
		g2.setColor(oldColor);
		g2.drawImage(img1, null, 0, 0);
		g2.drawImage(img2, null, img1.getWidth() + offset, 0);
		g2.dispose();
		return newImage;
	}

	public SearchServiceInternal getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchServiceInternal searchService) {
		this.searchService = searchService;
	}
}
