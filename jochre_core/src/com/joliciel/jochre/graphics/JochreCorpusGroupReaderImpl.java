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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.util.PerformanceMonitor;


class JochreCorpusGroupReaderImpl extends JochreCorpusReaderImpl implements JochreCorpusGroupReader {
    private static final Log LOG = LogFactory.getLog(JochreCorpusGroupReaderImpl.class);
	private int imageIndex = 0;
	private int paragraphIndex = 0;
	private int rowIndex = 0;
	private int groupIndex = 0;
	
	private GroupOfShapes group = null;
	
	
	public JochreCorpusGroupReaderImpl() {
		super();
	}

	@Override
	public GroupOfShapes next()  {
		GroupOfShapes nextGroup = null;
		if (this.hasNext()) {
			LOG.debug("next group: " + this.group);
			nextGroup = this.group;

			this.group = null;
		}
		return nextGroup;
	}

	@Override
	public boolean hasNext()  {
		PerformanceMonitor.startTask("JochreCorpusGroupReaderImpl.hasNext");
		try {
			this.initialiseStream();
			while (group==null && imageIndex < this.getImages().size()) {
				JochreImage image = this.getImages().get(imageIndex);
				while (group==null && paragraphIndex < image.getParagraphs().size()) {
					Paragraph paragraph = image.getParagraphs().get(paragraphIndex);
					while (group==null && rowIndex < paragraph.getRows().size()) {
						RowOfShapes row = paragraph.getRows().get(rowIndex);
						while (group==null && groupIndex < row.getGroups().size()) {
							group = row.getGroups().get(groupIndex);
							groupIndex++;
						}
						if (group==null) {
							rowIndex++;
							groupIndex = 0;
						}
					}
					if (group==null) {
						paragraphIndex++;
						rowIndex = 0;
						groupIndex = 0;
					}
				}
				if (group==null) {
					image.clearMemory();
					imageIndex++;
					paragraphIndex = 0;
					rowIndex = 0;
					groupIndex = 0;
				}
			}
	
			return group!=null;
		} finally {
			PerformanceMonitor.endTask("JochreCorpusGroupReaderImpl.hasNext");
		}
	}

}
