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

import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.joliciel.jochre.Entity;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.security.User;

public interface JochreDocument extends Entity {
	/**
	 * The filename of this document.
	 */
	public String getFileName();

	void setFileName(String string);

	/**
	 * A more human-friendly name for this document.
	 */
	public String getName();

	void setName(String string);

	/**
	 * The pages contained in this document.
	 */
	public List<JochrePage> getPages();

	public JochrePage newPage();

	public JochrePage getCurrentPage();

	/**
	 * The document's locale.
	 */
	public Locale getLocale();

	public void setLocale(Locale locale);

	/**
	 * Is this document's locale left-to-right or right-to-left (ignoring
	 * top-to-bottom for now!)
	 */
	public boolean isLeftToRight();

	/**
	 * Returns an xml representation of this document as it currently stands, to
	 * be used for correcting the text associated with this document.
	 */
	public void getXml(OutputStream outputStream);

	/**
	 * For a document containing pages which contain raw images, segments these
	 * images by converting them to JochreImages.
	 */
	public void segment();

	/**
	 * Segment the document and output the segmentation into PNG files so that
	 * they can be viewed by the user.
	 */
	public void segmentAndShow(String outputDirectory);

	/**
	 * All images in this document, ordered by page index and image index.
	 */
	public List<JochreImage> getImages();

	public abstract void setOwner(User owner);

	/**
	 * The User who uploaded this document in the first place.
	 */
	public abstract User getOwner();

	/**
	 * The authors for this document.
	 */
	public List<Author> getAuthors();

	/**
	 * Delete an image from this document. If it is the last image on the page,
	 * delete the page containing the image as well.
	 */
	public void deleteImage(JochreImage image);

	public abstract int getOwnerId();

	public abstract void setYear(int year);

	public abstract int getYear();

	public abstract void setCity(String city);

	public abstract String getCity();

	public abstract void setPublisher(String publisher);

	public abstract String getPublisher();

	public abstract void setNameLocal(String nameLocal);

	public abstract String getNameLocal();

	public abstract void setReference(String reference);

	public abstract String getReference();

	public int getTotalPageCount();

	public void setTotalPageCount(int totalPageCount);

	public void deletePage(JochrePage page);

	/**
	 * Any fields associated with this document.
	 */
	public Map<String, String> getFields();

	/**
	 * Get a file base useful for saving files based on this document.
	 */
	public String getFileBase();

}
