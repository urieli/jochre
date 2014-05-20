///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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

import java.util.Map;
import java.util.Set;

/**
 * Selection criteria for a corpus reader.
 * @author Assaf Urieli
 *
 */
public interface CorpusSelectionCriteria {

	/**
	 * The max number of images to return. 0 means all images.
	 * @return
	 */
	public int getImageCount();
	public void setImageCount(int imageCount);
	
	/**
	 * The single image to include. Will override all other parameters.
	 * @return
	 */
	public int getImageId();
	public void setImageId(int imageId);
	
	/**
	 * The set of image statuses to include.
	 * To be used in combination with all other parameters (except imageId).
	 * @param imageStatusesToInclude
	 */
	public abstract void setImageStatusesToInclude(ImageStatus[] imageStatusesToInclude);
	public abstract ImageStatus[] getImageStatusesToInclude();
	
	/**
	 * Exclude this image id from the set of images returned.
	 * @param excludeImageId
	 */
	public abstract void setExcludeImageId(int excludeImageId);
	public abstract int getExcludeImageId();
	
	/**
	 * If either cross-validation training or evaluation, gives
	 * the size of the cross-validation set.
	 * @param crossValidationSize
	 */
	public abstract void setCrossValidationSize(int crossValidationSize);
	public abstract int getCrossValidationSize();
	
	/**
	 * If cross-validation training, the index of the 
	 * document we want to exclude from training, where
	 * this index goes from 0 to crossValidationSize-1.
	 * @param excludeIndex
	 */
	public abstract void setExcludeIndex(int excludeIndex);
	public abstract int getExcludeIndex();
	
	/**
	 * If cross-validation evaluation, the index of the 
	 * document we want to evaluate, where
	 * this index goes from 0 to crossValidationSize-1.
	 * Should be the same as the index excluded from training.
	 * @param includeIndex
	 */
	public abstract void setIncludeIndex(int includeIndex);
	public abstract int getIncludeIndex();
	
	/**
	 * Limit images to those belonging to a single document.
	 * @return
	 */
	public void setDocumentId(int documentId);
	public int getDocumentId();
	
	/**
	 * Limit images to those belonging to a set of documents.
	 * @param documentIds
	 */
	public void setDocumentIds(Set<Integer> documentIds);
	public Set<Integer> getDocumentIds();
	
	public Map<String, String> getAttributes();
}
