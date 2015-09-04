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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;

class JochreCorpusImageProcessorImpl extends JochreCorpusReaderImpl implements JochreCorpusImageProcessor {
    private static final Log LOG = LogFactory.getLog(JochreCorpusImageProcessorImpl.class);
	private List<DocumentObserver> observers = new ArrayList<DocumentObserver>();
	
	public JochreCorpusImageProcessorImpl(CorpusSelectionCriteria corpusSelectionCriteria) {
		super();
		super.setSelectionCriteria(corpusSelectionCriteria);
	}
	
	@Override
	public void process() {
		this.initialiseStream();
		JochreDocument currentDoc = null;
		JochrePage currentPage = null;
		for (JochreImage image : this.getImages()) {
			if (!image.getPage().equals(currentPage)) {
				if (currentPage!=null) {
					for (DocumentObserver observer : observers) {
						observer.onPageComplete(currentPage);
					}
					LOG.debug("completed page: " + currentPage);
				}
			}
			
			if (!image.getPage().getDocument().equals(currentDoc)) {
				if (currentDoc!=null) {
					for (DocumentObserver observer : observers) {
						observer.onDocumentComplete(currentDoc);
					}
					LOG.debug("completed doc: " + currentDoc);
				}
				currentDoc = image.getPage().getDocument();
				LOG.debug("next doc: " + currentDoc);
				for (DocumentObserver observer : observers) {
					observer.onDocumentStart(currentDoc);
				}
			}
			
			if (!image.getPage().equals(currentPage)) {
				currentPage = image.getPage();
				LOG.debug("next page: " + currentPage);
				for (DocumentObserver observer : observers) {
					observer.onPageStart(currentPage);
				}
			}

			LOG.debug("next image: " + image);

			for (DocumentObserver observer : observers) {
				observer.onImageStart(image);
			}

			for (DocumentObserver observer : observers) {
				observer.onImageComplete(image);
			}
			LOG.debug("completed image: " + image);
			image.clearMemory();
		}
		
		if (currentPage!=null) {
			for (DocumentObserver observer : observers) {
				observer.onPageComplete(currentPage);
			}
			LOG.debug("completed page: " + currentPage);
		}
		
		if (currentDoc!=null) {
			for (DocumentObserver observer : observers) {
				observer.onDocumentComplete(currentDoc);
			}
			LOG.debug("completed doc: " + currentDoc);
		}
	}

	@Override
	public List<DocumentObserver> getObservers() {
		return observers;
	}
	
	@Override
	public void addObserver(DocumentObserver observer) {
		this.observers.add(observer);
	}
}
