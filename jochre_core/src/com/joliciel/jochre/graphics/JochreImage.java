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

import com.joliciel.jochre.Entity;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.security.User;

/**
 * A representation of an image that contains the results
 * of segmentataion analysis.
 * In other words, the image has been broken up into paragraphs,
 * rows, groups (words) and shapes (letters).
 * @author Assaf Urieli
 *
 */
public interface JochreImage extends Entity, ImageGrid {
	/**
	 * The image on which this SourceImage was built.
	 */
	public BufferedImage getOriginalImage();
	public void setOriginalImage(BufferedImage originalImage);
	
	/**
	 * The paragraphs contained in this image.
	 * @return
	 */
	public List<Paragraph> getParagraphs();
	public Paragraph newParagraph();
	
	/**
	 * A black brightness threshold, used for analysing the contents
	 * of a shape as black-or-white bits in order to recognise the letter.
	 * Anything &lt;= the black threshold should be considered black
	 * when analysing a letter.
	 * @return
	 */
	public int getBlackThreshold();
	public void setBlackThreshold(int blackThreshold);
	
	/**
	 * The separation threshold, used for analysing the image as
	 * black-or-white bits to determine where letters separate.
	 * Anything &lt;= the separation threshold should be considered black
	 * when separating letters.
	 * @return
	 */
	public int getSeparationThreshold();
	public void setSeparationThreshold(int separationThreshold);
	
	/**
	 * The name given to this image.
	 * @return
	 */
	public String getName();
	public void setName(String name);
	
	/**
	 * Image width.
	 * @return
	 */
	public int getWidth();
	public void setWidth(int width);
	
	/**
	 * Image height.
	 * @return
	 */
	public int getHeight();
	public void setHeight(int height);
	
	/**
	 * The page containing this image (in multi-page documents).
	 * @return
	 */
	public abstract JochrePage getPage();
	public abstract void setPageId(int pageId);
	public abstract int getPageId();
	
	/**
	 * The index of this image on the page.
	 * @return
	 */
	public int getIndex();
	public void setIndex(int index);
	
	/**
	 * The brightness limit below which pixels are considered to be pure black.
	 * Used to normalise brightness on the image.
	 * @return
	 */
	public abstract int getBlackLimit();
	public abstract void setBlackLimit(int blackLimit);
	
	/**
	 * The brightness limit above which pixels are considered to be pure white.
	 * Used to normalise brightness on the image.
	 * @return
	 */
	public abstract int getWhiteLimit();
	public abstract void setWhiteLimit(int whiteLimit);
	
	/**
	 * Returns the normalized brightness value
	 * corresponding to this brightness, taking into account the black limit and white limit.
	 * @param brightness
	 * @return
	 */
	public int normalize(int brightness);
	
	/**
	 * Fill factor for gaps that were mistakenly
	 * left empty when converting a grayscale image to black and white.
	 * Defaults to 0, but should be set higher if many white gaps appear in image.
	 * @return
	 */
	public int getWhiteGapFillFactor();
	public void setWhiteGapFillFactor(int whiteGapFillFactor);
	
	/**
	 * This image's status.
	 * @return
	 */
	public ImageStatus getImageStatus();
	public void setImageStatus(ImageStatus imageStatus);
	
	/**
	 * Clears out objects in memory to avoid filling it up.
	 */
	public void clearMemory();
	
	/**
	 * The average row height for this shape, from meanline to baseline.
	 * @return
	 */
	public double getAverageRowHeight();
	
	/**
	 * The number of shapes in this image.
	 * @return
	 */
	public int getShapeCount();
	
	/**
	 * The image's current owner, among the Jochre users.
	 * @return
	 */
	public abstract User getOwner();
	public abstract void setOwner(User owner);

	public abstract int getOwnerId();
	
	/**
	 * Returns a shape with the coordinates provided.
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	public Shape getShape(int left, int top, int right, int bottom);
	
	/**
	 * Is this containing document's locale left-to-right or right-to-left
	 * (ignoring top-to-bottom for now!)
	 * @return
	 */
	public boolean isLeftToRight();
	
	/**
	 * The average confidence of the current page, in a scale from 0 to 1.
	 * @return
	 */
	public double getConfidence();
	
	/**
	 * A rectangle containing all of this images paragraphs.
	 * @return
	 */
	public Rectangle getPrintSpace();
	
	/**
	 * Recalculate all indexes on this image.
	 */
	public void recalculateIndexes();
}
