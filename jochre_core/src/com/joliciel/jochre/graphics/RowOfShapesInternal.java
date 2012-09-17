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

import com.joliciel.jochre.EntityInternal;

interface RowOfShapesInternal extends RowOfShapes, EntityInternal {

	public void setParagraphId(int paragraphId);
	public void setParagraph(Paragraph paragraph);
	
	SourceImage getContainer();
	void setContainer(SourceImage container);
	
	void setImage(BufferedImage image);
	
	public void clearMemory();
	
	public void setXHeight(int height);
}
