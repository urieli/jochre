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
package com.joliciel.jochre.text;

import java.io.Writer;

import com.joliciel.jochre.graphics.JochreImage;

/**
 * Gets the text out of an image after it's been analysed.
 * @author Assaf Urieli
 *
 */
public interface TextGetter {
	/**
	 * Gets the text out of an image and writes it to the writer.
	 * @param image
	 * @param writer
	 */
	public void getText(JochreImage image, Writer writer, TextFormat textFormat);
}
