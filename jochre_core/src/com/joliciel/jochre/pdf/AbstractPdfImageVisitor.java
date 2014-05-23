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
package com.joliciel.jochre.pdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;
import org.jpedal.objects.PdfFileInformation;
import org.jpedal.objects.PdfImageData;

/**
 * A base class for visiting the images in a Pdf document one at a time.
 * @author Assaf Urieli
 *
 */
abstract class AbstractPdfImageVisitor {
	private static final Log LOG = LogFactory.getLog(AbstractPdfImageVisitor.class);
	private PdfDecoder pdfDecoder = null;
	private File pdfFile;
	private Map<String,String> fields = new TreeMap<String, String>();
	
	public AbstractPdfImageVisitor(File pdfFile) {
		try {
			this.pdfFile = pdfFile;
			this.pdfDecoder = new PdfDecoder( false );

			this.pdfDecoder.setExtractionMode(PdfDecoder.RAWIMAGES+PdfDecoder.FINALIMAGES);
	
			FileInputStream fis = new FileInputStream(pdfFile);
			this.pdfDecoder.openPdfFileFromInputStream(fis, true);
			
	        PdfFileInformation currentFileInformation=pdfDecoder.getFileInformationData();

	        String[] values=currentFileInformation.getFieldValues();
	        String[] fieldNames= PdfFileInformation.getFieldNames();

	        int count = fieldNames.length;

	        LOG.info("Fields");
	        for(int i=0;i<count;i++){
	        	fields.put(fieldNames[i], values[i].replace("Ì£", ""));
	        	LOG.info(fieldNames[i]+" = "+values[i]);
	        }

	        LOG.info("Metadata");
	        LOG.info(currentFileInformation.getFileXMLMetaData());

		} catch (FileNotFoundException fnfe) {
			throw new RuntimeException(fnfe);
		} catch (PdfException pdfe) {
			throw new RuntimeException(pdfe);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Visit all of the images in a pdf file.
	 * @param pdfFile
	 * @param firstPage a value of -1 means no first page
	 * @param lastPage a value of -1 means no last page
	 */
	final void visitImages(int firstPage, int lastPage) {
		try {
			try {
				for( int i = 1;i < pdfDecoder.getPageCount() + 1; i++ )
				{
					if (i < firstPage)
						continue;
					if (lastPage > 0 && i > lastPage)
						break;

					LOG.debug("Decoding page " + i);
					pdfDecoder.decodePage(i);

					// This object contains image meta-data
					// and gets the binary data which has been stored in a temp directory
					PdfImageData pdfImages = pdfDecoder.getPdfImageData();

					int imageCount = pdfImages.getImageCount();
					LOG.debug( imageCount + " images found" );

					for( int j = 0; j < imageCount; j++ )
					{
						String imageName = pdfImages.getImageName(j);
						// JPedal indicates the raw version of the image with the R prefix
						BufferedImage image = pdfDecoder.getObjectStore().loadStoredImage('R' + imageName );
						this.visitImage(image, imageName, i, j);
					}

					// After each page we need to flush the images out, otherwise we accumulate
					// images from previous pages
					pdfDecoder.flushObjectValues(true);
				}
			} finally {
				pdfDecoder.closePdfFile();	
			}
		} catch (FileNotFoundException fnfe) {
			throw new RuntimeException(fnfe);
		} catch (PdfException pdfe) {
			throw new RuntimeException(pdfe);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	
	/**
	 * Visit a single image.
	 * @param image
	 * @param imageName
	 * @param pageIndex
	 * @param imageIndex
	 */
	abstract void visitImage(BufferedImage image, String imageName, int pageIndex, int imageIndex);
	
	public int getPageCount() {
		return pdfDecoder.getPageCount();
	}
	
	public File getPdfFile() {
		return pdfFile;
	}

	public Map<String, String> getFields() {
		return fields;
	}
	
}
