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

class JochreCorpusShapeReaderImpl extends JochreCorpusReaderImpl implements JochreCorpusShapeReader {
    private static final Log LOG = LogFactory.getLog(JochreCorpusShapeReaderImpl.class);
	
	private int shapeIndex = 0;

	private GroupOfShapes group = null;
	private Shape shape = null;
	
	private JochreCorpusGroupReader groupReader;
	
	public JochreCorpusShapeReaderImpl() {
		super();
	}

	@Override
	public Shape next()  {
		Shape nextShape = null;
		if (this.hasNext()) {
			LOG.debug("next shape: " + shape);
			nextShape = this.shape;

			this.shape = null;
		}
		return nextShape;
	}

	@Override
	public boolean hasNext()  {
		this.initialiseStream();
		while (shape==null && group!=null) {
			if (shapeIndex < group.getShapes().size()) {
				shape = group.getShapes().get(shapeIndex);
				shapeIndex++;
			} else {
				group = null;
				shapeIndex = 0;
				if (groupReader.hasNext())
					group = groupReader.next();
			}
		}

		return shape!=null;
	}
	
	protected void initialiseStream() {
		if (groupReader==null) {
			groupReader = this.getGraphicsService().getJochreCorpusGroupReader();
			groupReader.setImageCount(this.getImageCount());
			groupReader.setImageId(this.getImageId());
			groupReader.setImageStatusesToInclude(this.getImageStatusesToInclude());
			
			if (groupReader.hasNext())
				group = groupReader.next();
		}
	}



}
