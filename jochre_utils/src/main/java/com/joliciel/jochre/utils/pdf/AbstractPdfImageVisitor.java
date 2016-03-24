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
package com.joliciel.jochre.utils.pdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;

import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.utils.LogUtils;

/**
 * A base class for visiting the images in a Pdf document one at a time.
 * @author Assaf Urieli
 *
 */
public abstract class AbstractPdfImageVisitor {
	private static final Log LOG = LogFactory.getLog(AbstractPdfImageVisitor.class);
	private PDDocument pdfDocument = null;
	private File pdfFile;
	private Map<String,String> fields = new TreeMap<String, String>();
	private boolean docClosed = false;
	private boolean stopOnError = false;
	
	public AbstractPdfImageVisitor(File pdfFile) {
		try {
			this.pdfFile = pdfFile;
	
			pdfDocument = PDDocument.load(pdfFile);
			PDDocumentInformation info = pdfDocument.getDocumentInformation();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
			fields.put("PageCount",  "" + pdfDocument.getNumberOfPages());
			if (info.getTitle()!=null)
				fields.put("Title", info.getTitle());
			if (info.getAuthor()!=null)
				fields.put("Author", info.getAuthor());
			if (info.getSubject()!=null)
				fields.put("Subject", info.getSubject());
			if (info.getKeywords()!=null)
				fields.put("Keywords", info.getKeywords());
			if (info.getCreator()!=null)
				fields.put("Creator", info.getCreator());
			if (info.getProducer()!=null)
				fields.put("Producer", info.getProducer());
			if (info.getCreationDate()!=null)
				fields.put("CreateDate", dateFormat.format(info.getCreationDate().getTime()));
			if (info.getModificationDate()!=null)
				fields.put("ModificationDate",  dateFormat.format(info.getModificationDate().getTime()));
			for (String metaDataField : info.getMetadataKeys()) {
				fields.put(metaDataField, info.getCustomMetadataValue(metaDataField));
			}

		} catch (FileNotFoundException fnfe) {
			throw new RuntimeException(fnfe);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Visit all of the images in a pdf file.
	 * @param firstPage a value of -1 means no first page
	 * @param lastPage a value of -1 means no last page
	 */
	final protected void visitImages(int firstPage, int lastPage) {
		try {
			@SuppressWarnings("unchecked")
			List<PDPage> allPages = pdfDocument.getDocumentCatalog().getAllPages();
			Iterator<PDPage> pageIterator = allPages.iterator();

			int i=0;
			while (pageIterator.hasNext()) {
				PDPage pdfPage = pageIterator.next();
				i++;
				if (i < firstPage)
					continue;
				if (lastPage > 0 && i > lastPage)
					break;

				LOG.info("Decoding page " + i + " (out of " + allPages.size() + ")");
				
				try {
					PDResources resources = pdfPage.getResources();
					Map<String,PDXObject> pdxObjects = resources.getXObjects();
					int j = 0;
					for (String key : pdxObjects.keySet()) {
						PDXObject pdxObject = pdxObjects.get(key);
						if (pdxObject instanceof PDXObjectImage) {
			                PDXObjectImage pdfImage = (PDXObjectImage) pdxObject;
			                BufferedImage image = pdfImage.getRGBImage();
			                if (image==null) {
			                	throw new PdfImageExtractionException("Something went wrong: unable to extract image " + j + " in file  " + pdfFile.getAbsolutePath() + ", page " + i);
			                }
			                this.visitImage(image, key, i, j);
			                j++;
			            }
			        }
				} catch (PdfImageExtractionException e) {
					LOG.error("Error in file  " + pdfFile.getAbsolutePath() + ", page " + i);
					LogUtils.logError(LOG, e);
					if (stopOnError)
						throw e;
				} catch (IOException e) {
					LOG.error("Error in file  " + pdfFile.getAbsolutePath() + ", page " + i);
					LogUtils.logError(LOG, e);
					if (stopOnError)
						throw new RuntimeException(e);
				} catch (JochreException e) {
					LOG.error("Error in file  " + pdfFile.getAbsolutePath() + ", page " + i);
					LogUtils.logError(LOG, e);
					if (stopOnError)
						throw e;
				}
			} // next page
		} finally {
			this.close();
		}
	}
	
	/**
	 * Visit a single image.
	 */
	protected abstract void visitImage(BufferedImage image, String imageName, int pageIndex, int imageIndex);
	
	public int getPageCount() {
		return pdfDocument.getNumberOfPages();
	}
	
	public File getPdfFile() {
		return pdfFile;
	}

	public Map<String, String> getFields() {
		return fields;
	}
	
	public void close() {
		try {
			if (!docClosed) {
				pdfDocument.close();
				docClosed = true;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Should processing stop if an error is encountered extracting an image from a given page.
	 */
	public boolean isStopOnError() {
		return stopOnError;
	}

	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}
	
	
}
