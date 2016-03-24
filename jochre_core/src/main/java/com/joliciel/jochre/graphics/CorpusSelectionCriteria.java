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
import java.util.Scanner;
import java.util.Set;

/**
 * Selection criteria for a corpus reader.
 * @author Assaf Urieli
 *
 */
public interface CorpusSelectionCriteria {

	/**
	 * The max number of images to return. 0 means all images.
	 */
	public int getImageCount();
	public void setImageCount(int imageCount);
	
	/**
	 * The single image to include. Will override all other parameters.
	 */
	public int getImageId();
	public void setImageId(int imageId);
	
	/**
	 * The set of image statuses to include.
	 * To be used in combination with all other parameters (except imageId).
	 */
	public abstract void setImageStatusesToInclude(ImageStatus[] imageStatusesToInclude);
	public abstract ImageStatus[] getImageStatusesToInclude();
	
	/**
	 * Exclude this image id from the set of images returned.
	 */
	public abstract void setExcludeImageId(int excludeImageId);
	public abstract int getExcludeImageId();
	
	/**
	 * If either cross-validation training or evaluation, gives
	 * the size of the cross-validation set.
	 */
	public abstract void setCrossValidationSize(int crossValidationSize);
	public abstract int getCrossValidationSize();
	
	/**
	 * If cross-validation training, the index of the 
	 * document we want to exclude from training, where
	 * this index goes from 0 to crossValidationSize-1.
	 */
	public abstract void setExcludeIndex(int excludeIndex);
	public abstract int getExcludeIndex();
	
	/**
	 * If cross-validation evaluation, the index of the 
	 * document we want to evaluate, where
	 * this index goes from 0 to crossValidationSize-1.
	 * Should be the same as the index excluded from training.
	 */
	public abstract void setIncludeIndex(int includeIndex);
	public abstract int getIncludeIndex();
	
	/**
	 * Limit images to those belonging to a single document.
	 */
	public void setDocumentId(int documentId);
	public int getDocumentId();
	
	/**
	 * Limit images to those belonging to a set of documents.
	 */
	public void setDocumentIds(Set<Integer> documentIds);
	public Set<Integer> getDocumentIds();
	
	/**
	 * Load the document selection from a scanner.
	 * There should be one line per document.
	 * Each line starts with the document name.
	 * If the line contains a tab, it can then contain a set of comma-separated pages or page ranges.
	 * Example:<br/>
	 * <pre>
	 * Bessou_Tata
	 * Bessou_Tomba	1,3-5,7
	 * </pre>
	 */
	public void loadSelection(Scanner scanner);
	
	/**
	 * A Map of document names to sets of pages. If the set is empty, all pages must be included.
	 */
	public Map<String, Set<Integer>> getDocumentSelections();
	
	public Map<String, String> getAttributes();
}
