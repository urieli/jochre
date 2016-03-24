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
import java.util.Collection;
import java.util.TreeSet;

import com.joliciel.jochre.EntityInternal;

interface ShapeInternal extends Shape, EntityInternal {	

	public void setImage(BufferedImage image);
	
	public boolean isDirty();

	public void setDirty(boolean dirty);

	/**
	 * A representation of the shape as a set of vertical line segments.
	 */
	public TreeSet<VerticalLineSegment> getVerticalLineSegments();
	
	/**
	 * Returns vertical line segments that are likely to be a bridge
	 * between two connected letters (due to an ink splotch, etc.).
	 */
	public Collection<BridgeCandidate> getBridgeCandidates(double maxBridgeWidth);
	
	
	/**
	 * Of all of the bridge candidates in this shape, gives the single best candidate.
	 * Criteria include: bridge width, pixel weight to either side of the bridge, and 
	 * overlap between right and left shapes.
	 * @maxBridgeWidth the maximum width in pixels of the bridge - used to reduce the search space
	 */
	public BridgeCandidate getBestBridgeCandidate(double maxBridgeWidth);

	public abstract void setJochreImage(JochreImage jochreImage);

	public abstract JochreImage getJochreImage();
	
	public void setGraphicsService(GraphicsServiceInternal graphicsService);

}
