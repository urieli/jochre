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
package com.joliciel.jochre.doc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.utils.Monitorable;
import com.joliciel.talismane.utils.MultiTaskProgressMonitor;
import com.joliciel.talismane.utils.ProgressMonitor;

class ImageDocumentExtractorImpl implements ImageDocumentExtractor {
	private static final Logger LOG = LoggerFactory.getLogger(ImageDocumentExtractorImpl.class);
	SourceFileProcessor documentProcessor;
	MultiTaskProgressMonitor currentMonitor;
	File imageFile;
	int pageNumber = 1;

	public ImageDocumentExtractorImpl(File imageFile, SourceFileProcessor documentProcessor) {
		this.documentProcessor = documentProcessor;
		this.imageFile = imageFile;
	}

	@Override
	public void run() {
		this.extractDocument();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.joliciel.jochre.doc.ImageDocumentExtractor#extractDocument(java.io.
	 * File, com.joliciel.jochre.doc.SourceFileProcessor)
	 */
	@Override
	public JochreDocument extractDocument() {
		LOG.debug("ImageDocumentExtractorImpl.extractDocument");
		try {
			File[] files = new File[1];

			if (imageFile.isDirectory()) {
				files = imageFile.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {
						return (name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg")
								|| name.toLowerCase().endsWith(".gif"));
					}
				});
			} else {
				files[0] = imageFile;
			}

			JochreDocument doc = this.documentProcessor.onDocumentStart();
			doc.setTotalPageCount(files.length);

			int currentPageNumber = this.pageNumber;
			for (File file : files) {
				JochrePage page = this.documentProcessor.onPageStart(currentPageNumber++);

				BufferedImage image = ImageIO.read(file);
				String imageName = file.getName();

				if (currentMonitor != null && documentProcessor instanceof Monitorable) {
					ProgressMonitor monitor = ((Monitorable) documentProcessor).monitorTask();
					double percentAllotted = (1 / (double) (files.length));
					currentMonitor.startTask(monitor, percentAllotted);
				}

				documentProcessor.onImageFound(page, image, imageName, 0);
				if (currentMonitor != null && documentProcessor instanceof Monitorable) {
					currentMonitor.endTask();
				}

				this.documentProcessor.onPageComplete(page);
			}
			this.documentProcessor.onDocumentComplete(doc);

			if (currentMonitor != null)
				currentMonitor.setFinished(true);
			return doc;
		} catch (Exception e) {
			LOG.debug("Exception occurred. Have monitor? " + currentMonitor);
			if (currentMonitor != null)
				currentMonitor.setException(e);
			LOG.error("Exception while processing document", e);
			throw new RuntimeException(e);
		} finally {
			LOG.debug("Exit ImageDocumentExtractorImpl.extractDocument");
		}
	}

	@Override
	public ProgressMonitor monitorTask() {
		currentMonitor = new MultiTaskProgressMonitor();

		return currentMonitor;
	}

	@Override
	public int getPageNumber() {
		return pageNumber;
	}

	@Override
	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

}
