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
 * An interface for reading shapes out of a Jochre corpus.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreCorpusShapeReader extends JochreCorpusReader {
	private static final Logger LOG = LoggerFactory.getLogger(JochreCorpusShapeReader.class);

	private int shapeIndex = 0;

	private GroupOfShapes group = null;
	private Shape shape = null;

	private JochreCorpusGroupReader groupReader;

	public JochreCorpusShapeReader(JochreSession jochreSession) {
		super(jochreSession);
	}

	public Shape next() {
		Shape nextShape = null;
		if (this.hasNext()) {
			LOG.debug("next shape: " + shape);
			nextShape = this.shape;

			this.shape = null;
		}
		return nextShape;
	}

	public boolean hasNext() {
		this.initialiseStream();
		while (shape == null && group != null) {
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

		return shape != null;
	}

	@Override
	protected void initialiseStream() {
		if (groupReader == null) {
			groupReader = new JochreCorpusGroupReader(jochreSession);
			groupReader.setSelectionCriteria(this.getSelectionCriteria());

			if (groupReader.hasNext())
				group = groupReader.next();
		}
	}

}
