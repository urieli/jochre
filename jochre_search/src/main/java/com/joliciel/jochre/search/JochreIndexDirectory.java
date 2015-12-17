///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Assaf Urieli
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
package com.joliciel.jochre.search;

import java.io.File;
import java.util.Map;

/**
 * A directory to be indexed by JochreSearch. We assume this directory will continue to exist
 * after indexing, since we store the directory path in the index, and retrieve images from the PDF
 * file located in this directory.<br/>
 * The directory name is used to uniquely identify a particular indexed work. When indexing a directory
 * any previous work indexed using the same directory name will be deleted first.<br/>
 * The directory must contain the following files:<br/>
 * <ul>
 * <li><i>filename</i>.pdf: the pdf file containing the images of the work that was ocred.</li>
 * <li><i>filename</i>.zip/.xml: a file in Alto3 format (either zipped or not) containing the OCR text layer of the PDF. If a zip file is found,
 * it is used, otherwise an XML file is used.</li>
 * <li>delete|delete.txt|skip|skip.txt|update|update.txt: if one of these files is present (processed in this order),
 * provides explicit instructions on what to do when indexing this directory. The file contents are ignored, and can be empty - only the filename is important.</li>
 * </ul>
 * The PDF/ZIP/XML filename above is arbitrary: only the extension is required. However, the system will first look for a file
 * with the same name as the directory name, and only then look for any arbitrary file.<br/>
 * 
 * @author Assaf Urieli
 *
 */
public interface JochreIndexDirectory {
	public enum Instructions {
		None,
		Delete,
		Skip,
		Update
	}
	
	/**
	 * The unique directory name.
	 * @return
	 */
	String getName();
	
	/**
	 * The directory wrapped by this object.
	 * @return
	 */
	File getDirectory();
	
	/**
	 * The PDF file being indexed.
	 * @return
	 */
	File getPdfFile();
	
	/**
	 * The Alto text layer of the PDF file being indexed.
	 * @return
	 */
	File getAltoFile();
	
	/**
	 * An optional file containing metadata, with the same name as the PDF file + _meta.xml.
	 * @return
	 */
	File getMetaDataFile();
	
	/**
	 * The metadata contained in the PDF file.
	 * @return
	 */
	Map<String,String> getMetaData();
	
	/**
	 * Explicit instructions on what to do with this directory.
	 * @return
	 */
	Instructions getInstructions();
	
	/**
	 * An input stream for the Alto XML content.
	 * @return
	 */
	UnclosableInputStream getAltoInputStream();
}
