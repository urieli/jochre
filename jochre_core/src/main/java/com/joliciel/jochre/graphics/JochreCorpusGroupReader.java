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
package com.joliciel.jochre.graphics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;

/**
 * An interface for reading shape groups out of a Jochre corpus.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreCorpusGroupReader extends JochreCorpusReader {
	private static final Logger LOG = LoggerFactory.getLogger(JochreCorpusGroupReader.class);

	private int imageIndex = 0;
	private int paragraphIndex = 0;
	private int rowIndex = 0;
	private int groupIndex = 0;

	private GroupOfShapes group = null;

	public JochreCorpusGroupReader(JochreSession jochreSession) {
		super(jochreSession);
	}

	public GroupOfShapes next() {
		GroupOfShapes nextGroup = null;
		if (this.hasNext()) {
			LOG.debug("next group: " + this.group);
			nextGroup = this.group;

			this.group = null;
		}
		return nextGroup;
	}

	public boolean hasNext() {
		this.initialiseStream();
		while (group == null && imageIndex < this.getImages().size()) {
			JochreImage image = this.getImages().get(imageIndex);
			while (group == null && paragraphIndex < image.getParagraphs().size()) {
				Paragraph paragraph = image.getParagraphs().get(paragraphIndex);
				while (group == null && rowIndex < paragraph.getRows().size()) {
					RowOfShapes row = paragraph.getRows().get(rowIndex);
					while (group == null && groupIndex < row.getGroups().size()) {
						group = row.getGroups().get(groupIndex);
						if (group.isSkip())
							group = null;
						groupIndex++;
					}
					if (group == null) {
						rowIndex++;
						groupIndex = 0;
					}
				}
				if (group == null) {
					paragraphIndex++;
					rowIndex = 0;
					groupIndex = 0;
				}
			}
			if (group == null) {
				image.clearMemory();
				imageIndex++;
				paragraphIndex = 0;
				rowIndex = 0;
				groupIndex = 0;
			}
		}

		return group != null;
	}

}
