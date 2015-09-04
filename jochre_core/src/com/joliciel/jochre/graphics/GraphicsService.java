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
import java.util.Locale;

import com.joliciel.jochre.doc.JochrePage;

/**
 * Image manipulation service, including segmentation into rows and letters.
 * @author Assaf Urieli
 *
 */
public interface GraphicsService {
	
	/**
	 * Get a shape which is a single pixel inside a given SourceImage.
	 * @return
	 */
	public Shape getDot(JochreImage sourceImage, int x, int y);
	
	/**
	 * Get a segmenter for segmenting a raw image.
	 * @return
	 */
	public Segmenter getSegmenter(SourceImage sourceImage);
	
	/**
	 * Get a vectorizer for vectorizing shapes.
	 */
	public Vectorizer getVectorizer();

	/**
	 * Load a JochreImage from the persistent store.
	 * @param imageId
	 * @return
	 */
	public JochreImage loadJochreImage(int imageId);
	
	/**
	 * Load a Shape from the persistent store.
	 * @param shapeId
	 * @return
	 */
	public Shape loadShape(int shapeId);

	
	/**
	 * Find all images with a given status.
	 * @param imageStatus
	 * @return
	 */
	public List<JochreImage> findImages(ImageStatus[] imageStatuses);
	
	/**
	 * Saves a jochre image (but doesn't recursively save it's contents).
	 * @param image
	 */
	public void saveJochreImage(JochreImage image);
	
	/**
	 * Find all shape ids in the training set (ImageStatus = TRAINING_VALIDATED) corresponding to a certain letter.
	 * @param letter
	 * @return
	 */
	public List<Integer> findShapeIds(String letter);
	
	/**
	 * Get a mirror of a given image grid, used to
	 * store pixels that have already been processed.
	 * @param imageGrid
	 * @return
	 */
	public WritableImageGrid getEmptyMirror(ImageGrid imageGrid);
	
	/**
	 * Find all JochreImages on a given page.
	 * @param page
	 * @return
	 */
	public List<JochreImage> findImages(JochrePage page);
	
	/**
	 * Get a source image.
	 * @param image
	 * @return
	 */
	public SourceImage getSourceImage(JochrePage page, String name, BufferedImage image);

	public void deleteJochreImage(JochreImage image);

	/**
	 * Return a list of all shapes that need to be split.
	 * @return
	 */
	public List<Shape> findShapesToSplit(Locale locale);
	
	/**
	 * Returns a list of any groups containing shapes that need to be merged.
	 * @return
	 */
	public abstract List<GroupOfShapes> findGroupsForMerge();
	
	public JochreCorpusShapeReader getJochreCorpusShapeReader();

	public JochreCorpusGroupReader getJochreCorpusGroupReader();

	public JochreCorpusImageReader getJochreCorpusImageReader();
	
	public JochreCorpusImageProcessor getJochreCorpusImageProcessor(CorpusSelectionCriteria corpusSelectionCriteria);
	
	public CorpusSelectionCriteria getCorpusSelectionCriteria();

}
