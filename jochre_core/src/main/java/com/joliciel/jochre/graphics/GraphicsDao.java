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

import java.util.List;
import java.util.Locale;

import com.joliciel.jochre.doc.JochrePage;

interface GraphicsDao {
	public Shape loadShape(int shapeId);
	public void saveShape(Shape shape);
	
	public RowOfShapes loadRowOfShapes(int rowId);
	public void saveRowOfShapes(RowOfShapes row);
	
	public JochreImage loadJochreImage(int imageId);
	public void saveJochreImage(JochreImage image);
	
	public GraphicsServiceInternal getGraphicsServiceInternal();

	public void setGraphicsServiceInternal(
			GraphicsServiceInternal graphicsServiceInternal);
	
	public void deleteContiguousShapeInternal(
			ShapeInternal contiguousShapeInternal);
	
	public GroupOfShapes loadGroupOfShapes(int groupId);
	public void saveGroupOfShapes(GroupOfShapes group);
	
	public List<Paragraph> findParagraphs(JochreImage jochreImage);
	public List<RowOfShapes> findRows(Paragraph paragraph);
	public abstract List<GroupOfShapes> findGroups(RowOfShapes row);
	public abstract List<Shape> findShapes(GroupOfShapes group);
	public void saveParagraph(Paragraph paragraph);
	public Paragraph loadParagraph(int paragraphId);

	public List<JochreImage> findImages(JochrePage page);

	
	/**
	 * Find all images with a given status.
	 */
	public List<JochreImage> findImages(ImageStatus[] imageStatuses);
	
	/**
	 * Find all shape ids in the training set (ImageStatus = TRAINING_VALIDATED) correspoding to a certain letter.
	 */
	public List<Integer> findShapeIds(String letter);
	
	public List<Shape> findShapes(RowOfShapes row);
	public abstract void saveOriginalImage(JochreImage jochreImage);
	public abstract void loadOriginalImage(JochreImageInternal jochreImage);
	
	public int getShapeCount(JochreImage jochreImage);
	
	public void deleteJochreImage(JochreImage image);

	/**
	 * Return a list of all shapes that need to be split.
	 */
	public List<Shape> findShapesToSplit(Locale locale);
	
	/**
	 * Returns a list of any groups containing shapes that need to be merged.
	 */
	public abstract List<GroupOfShapes> findGroupsForMerge();

}
