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
	private ImageStatus[] imageStatusesToInclude = new ImageStatus[] { ImageStatus.TRAINING_HELD_OUT };
	private int imageCount = 0;
	private int imageId = 0;
	
	public JochreCorpusReaderImpl() {
		super();
	}
	
	protected void initialiseStream() {
		if (images==null) {
			images = new ArrayList<JochreImage>();
			if (imageId!=0) {
				JochreImage jochreImage = this.graphicsService.loadJochreImage(imageId);
				images.add(jochreImage);
			} else {
				List<JochreImage> myImages = this.graphicsService.findImages(this.imageStatusesToInclude);
				int i = 0;
				for (JochreImage image : myImages) {
					if (imageCount>0 && i>=imageCount)
						break;
					images.add(image);
					i++;
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

	public int getImageCount() {
		return imageCount;
	}

	public void setImageCount(int imageCount) {
		this.imageCount = imageCount;
	}

	@Override
	public ImageStatus[] getImageStatusesToInclude() {
		return imageStatusesToInclude;
	}

	@Override
	public void setImageStatusesToInclude(ImageStatus[] imageStatusesToInclude) {
		this.imageStatusesToInclude = imageStatusesToInclude;
	}

	@Override
	public int getImageId() {
		return imageId;
	}

	@Override
	public void setImageId(int imageId) {
		this.imageId = imageId;
	}

	public List<JochreImage> getImages() {
		return images;
	}
	
	
}
