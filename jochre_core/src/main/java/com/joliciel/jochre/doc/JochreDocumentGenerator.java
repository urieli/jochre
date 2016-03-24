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

import java.io.File;

import com.joliciel.jochre.lexicon.MostLikelyWordChooser;
import com.joliciel.jochre.security.User;
import com.joliciel.talismane.utils.Monitorable;

/**
 * A utility interface to create and analyse JochreDocuments out of a source file
 * containing multiple pages and images (typically a PDF file).
 * @author Assaf Urieli
 *
 */
public interface JochreDocumentGenerator extends SourceFileProcessor, Monitorable {
	public void addDocumentObserver(DocumentObserver observer);
	
	/**
	 * Call if this document should be analysed for letters.
	 */
	public void requestAnalysis(File letterModelFile, MostLikelyWordChooser wordChooser);
	
	/**
	 * Call if this document should be analysed for letters, after applying split/merge models.
	 */
	public void requestAnalysis(File splitModelFile, File mergeModelFile, File letterModelFile, MostLikelyWordChooser wordChooser);
	
	/**
	 * Call if this document should be saved to the database.
	 */
	public void requestSave(User currentUser);
	
	/**
	 * Call if the segmented images should be saved to a directory.
	 */
	public void requestSegmentation(File outputDirectory);
	
	/**
	 * Should segmented images be generated?
	 */
	public abstract boolean isShowSegmentation();

	/**
	 * If analyse and split/merge required, the file containing the merge model.
	 */
	public abstract File getMergeModelFile();

	/**
	 * If analyse and split/merge required, the file containing the split model.
	 */
	public abstract File getSplitModelFile();

	/**
	 * If analyse, the file containing the letter guessing model.
	 */
	public abstract File getLetterModelFile();

	/**
	 * The current user (required if saving)
	 */
	public abstract User getCurrentUser();

	/**
	 * Should the document be saved?
	 */
	public abstract boolean isSave();

	/**
	 * Where to save segmented images
	 */
	public abstract File getOutputDirectory();
	

	public abstract void setDrawPixelSpread(boolean drawPixelSpread);

	public abstract boolean isDrawPixelSpread();
}