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


class JochreCorpusImageReaderImpl extends JochreCorpusReaderImpl implements JochreCorpusImageReader {
    private static final Log LOG = LogFactory.getLog(JochreCorpusImageReaderImpl.class);
	
	private JochreImage image = null;
	private int imageIndex = 0;
	
	public JochreCorpusImageReaderImpl() {
		super();
	}

	@Override
	public JochreImage next()  {
		if (this.image!=null) {
			this.image.clearMemory();
		}
		JochreImage nextImage = null;
		if (this.hasNext()) {
			LOG.debug("next image: " + this.image);
			nextImage = this.image;

			this.image = null;
		}
		return nextImage;
	}

	@Override
	public boolean hasNext()  {
		this.initialiseStream();
		if (image==null && imageIndex < this.getImages().size()) {
			image = this.getImages().get(imageIndex);
			imageIndex++;
		}

		return image!=null;
	}
}
