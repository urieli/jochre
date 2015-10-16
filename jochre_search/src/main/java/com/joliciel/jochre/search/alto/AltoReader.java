///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
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
package com.joliciel.jochre.search.alto;

import java.io.File;
import java.io.InputStream;

/**
 * A reader for files in Alto 3 format.
 * By default, the reader feeds pages read to consumers via {@link #addConsumer(AltoPageConsumer)}.
 * If the client wishes to parse an entire document into memory rather than using consumers,
 * it needs to use {@link #setBuildEntireDocument(boolean)}.
 * @author Assaf Urieli
 *
 */
public interface AltoReader {
	/**
	 * Parse an input stream, with the given file name.
	 * @param inputStream
	 * @param fileNameBase
	 */
	public void parseFile(InputStream inputStream, String fileNameBase);
	
	/**
	 * Parse a file.
	 * @param altoFile
	 */
	public void parseFile(File altoFile);

	/**
	 * Add a consumer which gets notified of pages when processing completes.
	 * @param consumer
	 */
	public void addConsumer(AltoPageConsumer consumer);
	
	/**
	 * Should the AltoReader construct an entire document? Requires a lot more memory.
	 * Default is false, in which case the reader is only usable with AltoPageConsumers.
	 * @param buildEntireDocument
	 */
	public void setBuildEntireDocument(boolean buildEntireDocument);
	public boolean isBuildEntireDocument();
	
	/**
	 * Get the document built.
	 * @return
	 */
	public AltoDocument getDocument();
}