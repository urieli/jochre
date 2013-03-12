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

import java.util.ArrayList;
import java.util.List;


abstract class JochreCorpusReaderImpl implements JochreCorpusReader {
	private GraphicsService graphicsService;
	
	private List<JochreImage> images = null;

	private CorpusSelectionCriteria selectionCriteria = null;
	
	public JochreCorpusReaderImpl() {
		super();
	}
	
	protected void initialiseStream() {
		if (images==null) {
			images = new ArrayList<JochreImage>();
			if (selectionCriteria.getImageId()!=0) {
				JochreImage jochreImage = this.graphicsService.loadJochreImage(selectionCriteria.getImageId());
				images.add(jochreImage);
			} else {
				List<JochreImage> myImages = this.graphicsService.findImages(selectionCriteria.getImageStatusesToInclude());
				int i = 0;
				for (JochreImage image : myImages) {
					if (selectionCriteria.getImageCount()>0 && images.size()>=selectionCriteria.getImageCount())
						break;
					if (image.getId()==selectionCriteria.getExcludeImageId())
						continue;
					if (selectionCriteria.getDocumentId()>0 && image.getPage().getDocumentId()!=selectionCriteria.getDocumentId())
						continue;
					if (selectionCriteria.getDocumentIds()!=null && !selectionCriteria.getDocumentIds().contains(image.getPage().getDocumentId()))
						continue;
					if (selectionCriteria.getCrossValidationSize()>0) {
						i++;
						if (selectionCriteria.getIncludeIndex()>=0) {
							if (i % selectionCriteria.getCrossValidationSize() != selectionCriteria.getIncludeIndex()) {
								continue;
							}
						} else if (selectionCriteria.getExcludeIndex()>=0) {
							if (i % selectionCriteria.getCrossValidationSize() == selectionCriteria.getExcludeIndex()) {
								continue;
							}
						}
					}
					images.add(image);
				}
			}
		}
	}

	public GraphicsService getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}
	
	public List<JochreImage> getImages() {
		return images;
	}

	@Override
	public CorpusSelectionCriteria getSelectionCriteria() {
		return selectionCriteria;
	}

	@Override
	public void setSelectionCriteria(CorpusSelectionCriteria selectionCriteria) {
		this.selectionCriteria = selectionCriteria;
	}
	
	
}
