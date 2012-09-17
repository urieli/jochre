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
package com.joliciel.jochre.pdf;

import java.io.File;

/**
 * Saves a set of images extracted from a pdf document.
 * @author Assaf Urieli
 *
 */
public interface PdfImageSaver {
	/**
	 * Save the images to the outputDirectory indicated.
	 * @param pdfFile
	 * @param outputDirectory
	 * @param firstPage a value of -1 means no first page
	 * @param lastPage a value of -1 means no last page
	 */
	public void saveImages(File pdfFile, String outputDirectory, int firstPage, int lastPage);
}
