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

/**
 * Convert a bitmap shape into a List of line segments representing the major
 * line strokes forming this shape.
 * @author Assaf Urieli
 *
 */
public interface Vectorizer {
	public List<LineSegment> vectorize(Shape shape);
	
	public BufferedImage drawArrayLists(JochreImage jochreImage);
	
	/**
	 * The white gap fill factor used prior to vectorizing a shape.
	 */
	public int getWhiteGapFillFactor();

	public void setWhiteGapFillFactor(int whiteGapFillFactor);
}
