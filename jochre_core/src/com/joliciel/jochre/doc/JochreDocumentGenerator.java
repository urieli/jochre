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

import com.joliciel.jochre.analyser.ImageAnalyser;
import com.joliciel.jochre.lexicon.MostLikelyWordChooser;
import com.joliciel.jochre.security.User;
import com.joliciel.talismane.utils.Monitorable;

public interface JochreDocumentGenerator extends SourceFileProcessor, Monitorable {
	public void addDocumentObserver(DocumentObserver observer);
	
	/**
	 * Request analysis using a pre-constructed analyser.
	 * @param analyser
	 */
	public void requestAnalysis(ImageAnalyser analyser);
	
	/**
	 * Call if this document should be analysed for letters.
	 * @param letterModelFile
	 * @param wordChooser
	 */
	public void requestAnalysis(File letterModelFile, MostLikelyWordChooser wordChooser);
	
	/**
	 * Call if this document should be analysed for letters, after applying split/merge models.
	 * @param splitModelFile
	 * @param mergeModelFile
	 * @param letterModelFile
	 * @param wordChooser
	 */
	public void requestAnalysis(File splitModelFile, File mergeModelFile, File letterModelFile, MostLikelyWordChooser wordChooser);
	
	/**
	 * Call if this document should be saved to the database.
	 * @param currentUser
	 */
	public void requestSave(User currentUser);
	
	/**
	 * Call if the segmented images should be saved to a directory.
	 * @param outputDirectory
	 */
	public void requestSegmentation(File outputDirectory);

	/**
	 * Should the document be analysed?
	 * @return
	 */
	public abstract boolean isAnalyse();

	/**
	 * Should segmented images be generated?
	 * @return
	 */
	public abstract boolean isShowSegmentation();

	/**
	 * If analyse and split/merge required, the file containing the merge model.
	 * @return
	 */
	public abstract File getMergeModelFile();

	/**
	 * If analyse and split/merge required, the file containing the split model.
	 * @return
	 */
	public abstract File getSplitModelFile();

	/**
	 * If analyse, the file containing the letter guessing model.
	 * @return
	 */
	public abstract File getLetterModelFile();

	/**
	 * The current user (required if saving)
	 * @return
	 */
	public abstract User getCurrentUser();

	/**
	 * Should the document be saved?
	 * @return
	 */
	public abstract boolean isSave();

	/**
	 * Where to save segmented images
	 * @return
	 */
	public abstract File getOutputDirectory();
	

	public abstract void setDrawPixelSpread(boolean drawPixelSpread);

	public abstract boolean isDrawPixelSpread();
}