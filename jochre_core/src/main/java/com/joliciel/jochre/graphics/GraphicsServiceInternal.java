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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;

import com.joliciel.jochre.graphics.util.ImagePixelGrabber;

interface GraphicsServiceInternal extends GraphicsService {
	

	
	public void saveShape(Shape shape);
	
	public RowOfShapes loadRowOfShapes(int rowId);
	public void saveRowOfShapes(RowOfShapes row);

	
	public ShapeInternal getEmptyShapeInternal();
	public RowOfShapesInternal getEmptyRowOfShapesInternal();
	public JochreImageInternal getEmptyJochreImageInternal();

	public void deleteShapeInternal(
			ShapeInternal shape);

	public GroupOfShapes loadGroupOfShapes(int groupId);

	public GroupOfShapesInternal getEmptyGroupOfShapesInternal();

	public void saveGroupOfShapes(GroupOfShapes group);

	public List<RowOfShapes> findRows(Paragraph paragraph);

	public List<GroupOfShapes> findGroups(RowOfShapes row);

	public List<Shape> findShapes(GroupOfShapes groupOfShapes);

	public void saveParagraph(Paragraph paragraph);

	public Paragraph loadParagraph(int paragraphId);

	public List<Paragraph> findParagraphs(JochreImage jochreImage);

	public ParagraphInternal getEmptyParagraphInternal();

	public LineDefinition getEmptyLineDefinition(int sector, int index);

	LineSegment getEmptyLineSegment(Shape shape, LineDefinition lineDefinition, int startX, int startY, int endX, int endY);
	
	
	/**
	 * Get an image grid from an image.
	 */
	public SourceImageInternal getSourceImageInternal(String name, BufferedImage image);
	
	public RowOfShapes getEmptyRow(SourceImage sourceImage);
	
	public ImagePixelGrabber getPixelGrabber(Image image);
	
	public ShapeFiller getShapeFiller();

	public List<Shape> findShapes(RowOfShapes rowOfShapes);
	
	public abstract void saveOriginalImage(JochreImage jochreImage);
	public abstract void loadOriginalImage(JochreImageInternal jochreImage);



	public int getShapeCount(JochreImage jochreImage);

}
