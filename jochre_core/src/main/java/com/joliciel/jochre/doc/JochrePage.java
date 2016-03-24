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
package com.joliciel.jochre.doc;

import java.awt.image.BufferedImage;
import java.util.List;

import com.joliciel.jochre.Entity;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.SourceImage;

/**
 * A single page on a document being analysed.
 * @author Assaf Urieli
 *
 */
public interface JochrePage extends Entity {
	/**
	 * The document containing this page.
	 */
	public JochreDocument getDocument();
	public int getDocumentId();
	public int getIndex();
	public void setIndex(int index);
	
	public List<JochreImage> getImages();
	
	public SourceImage newJochreImage(BufferedImage image, String imageName);
		
	/**
	 * For any Image on this page, segments it by converting to a JochreImage.
	 */
	public void segment();
	
	/**
	 * Segment any image on this page and output the segmentation into
	 * PNG files so that they can be viewed by the user.
	 */
	public void segmentAndShow(String outputDirectory);

	/**
	 * Clears out objects in memory to avoid filling it up.
	 */
	public void clearMemory();
}
