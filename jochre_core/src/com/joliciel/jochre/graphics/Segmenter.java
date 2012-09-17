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

import com.joliciel.talismane.utils.util.Monitorable;

/**
 * Takes a SourceImage and converts it into an JochreImage,
 * segmented into ordered Rows, Groups and Shapes.
 * The Locale matters for the ordering (e.g. right-to-left or left-to-write).
 * @author Assaf Urieli
 *
 */
public interface Segmenter extends Monitorable {

	/**
	 * Divide an image up into rows, groups and shapes
	 * (corresponding to rows, words and letters).
	 * @return
	 */
	public abstract void segment();
	
	/**
	 * The source image to be analysed.
	 * @return
	 */
	public SourceImage getSourceImage();
	
	/**
	 * A representation of the segmentation performed,
	 * if isDrawSegmentation is true.
	 * @return
	 */
	public BufferedImage getSegmentedImage();
	
	/**
	 * Should we or should we not draw the segmentation?
	 * @return
	 */
	public boolean isDrawSegmentation();
	public void setDrawSegmentation(boolean drawSegmentation);

}