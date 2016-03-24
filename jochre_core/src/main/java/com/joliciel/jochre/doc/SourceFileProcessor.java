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

import java.awt.image.BufferedImage;

import com.joliciel.jochre.graphics.JochreImage;

/**
 * A marker interface to enable processing documents "on-the-run" while the source,
 * say a large PDF file, is being processed
 * (rather than waiting till the end and running out of memory!).
 * @author Assaf Urieli
 *
 */
public interface SourceFileProcessor {
	/**
	 * Called when the document is first created (before any pages are processed).
	 * @return a JochreDocument that is loaded or created in this method.
	 */
	public JochreDocument onDocumentStart();
	
	/**
	 * Will be called after processing is complete for all pages in this document.
	 * @param doc the document that has completed
	 */
	public void onDocumentComplete(JochreDocument doc);
	
	/**
	 * Get the document currently being processed.
	 */
	public JochreDocument getDocument();
	
	/**
	 * Called when the page is first created (before any images are processed).
	 * @param pageIndex the index of this page
	 * @return a JochrePage that has been loaded or created.
	 */
	public JochrePage onPageStart(int pageIndex);
	
	/**
	 * Will be called after processing is complete for all images found on this page.
	 * @param jochrePage the page that has completed
	 */
	public void onPageComplete(JochrePage jochrePage);
	
	/**
	 * Called when each BufferedImage is first extracted from its source.
	 * @param jochrePage the current JochrePage
	 * @param image the BufferedImage that was extracted
	 * @param imageName the name of the image
	 * @param imageIndex the index of the image on the current page
	 * @return a JochreImage that has either been loaded or created.
	 * */
	public JochreImage onImageFound(JochrePage jochrePage, BufferedImage image, String imageName,
			int imageIndex);
}
