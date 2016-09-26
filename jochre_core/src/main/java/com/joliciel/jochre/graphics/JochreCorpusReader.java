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
import java.util.Set;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.doc.DocumentService;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;

public abstract class JochreCorpusReader {
	private DocumentService documentService;

	private List<JochreImage> images = null;

	private CorpusSelectionCriteria selectionCriteria = null;

	protected final JochreSession jochreSession;
	private final GraphicsDao graphicsDao;

	public JochreCorpusReader(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.graphicsDao = GraphicsDao.getInstance(jochreSession);
	}

	protected void initialiseStream() {
		if (images == null) {
			images = new ArrayList<JochreImage>();
			if (selectionCriteria.getImageId() != 0) {
				JochreImage jochreImage = this.graphicsDao.loadJochreImage(selectionCriteria.getImageId());
				images.add(jochreImage);
			} else if (selectionCriteria.getDocumentSelections() != null) {
				for (String docName : selectionCriteria.getDocumentSelections().keySet()) {
					JochreDocument doc = this.documentService.loadJochreDocument(docName);
					Set<Integer> pageIds = selectionCriteria.getDocumentSelections().get(docName);
					for (JochrePage page : doc.getPages()) {
						if (pageIds.size() == 0 || pageIds.contains(page.getIndex())) {
							for (JochreImage jochreImage : page.getImages()) {
								images.add(jochreImage);
							}
						}
					}
				}
			} else {
				List<JochreImage> myImages = this.graphicsDao.findImages(selectionCriteria.getImageStatusesToInclude());
				int i = 0;
				for (JochreImage image : myImages) {
					if (selectionCriteria.getImageCount() > 0 && images.size() >= selectionCriteria.getImageCount())
						break;
					if (image.getId() == selectionCriteria.getExcludeImageId())
						continue;
					if (selectionCriteria.getDocumentId() > 0 && image.getPage().getDocumentId() != selectionCriteria.getDocumentId())
						continue;
					if (selectionCriteria.getDocumentIds() != null && !selectionCriteria.getDocumentIds().contains(image.getPage().getDocumentId()))
						continue;
					if (selectionCriteria.getCrossValidationSize() > 0) {
						i++;
						if (selectionCriteria.getIncludeIndex() >= 0) {
							if (i % selectionCriteria.getCrossValidationSize() != selectionCriteria.getIncludeIndex()) {
								continue;
							}
						} else if (selectionCriteria.getExcludeIndex() >= 0) {
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

	public List<JochreImage> getImages() {
		return images;
	}

	/**
	 * The selection criteria driving the choice of images in this reader.
	 */
	public CorpusSelectionCriteria getSelectionCriteria() {
		return selectionCriteria;
	}

	public void setSelectionCriteria(CorpusSelectionCriteria selectionCriteria) {
		this.selectionCriteria = selectionCriteria;
	}

	public DocumentService getDocumentService() {
		return documentService;
	}

	public void setDocumentService(DocumentService documentService) {
		this.documentService = documentService;
	}

}
