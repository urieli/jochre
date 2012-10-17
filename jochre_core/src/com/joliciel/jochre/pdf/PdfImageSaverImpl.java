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
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PdfImageSaverImpl extends AbstractPdfImageVisitor implements PdfImageSaver {
	private static final Log LOG = LogFactory.getLog(PdfImageSaverImpl.class);

	private static String SEPARATOR = System.getProperty( "file.separator" );
	private static String SUFFIX = "png";

	private String outputDirectory;

	public PdfImageSaverImpl(File pdfFile) {
		super(pdfFile);
	}
	
	@Override
	public void saveImages(String outputDirectory, int firstPage, int lastPage) {

		// Create the output directory if it doesn't exist
		this.outputDirectory = outputDirectory;

		LOG.debug( "Images will be stored to " + outputDirectory );
		File outputDirectoryPath = new File( outputDirectory );
		if( outputDirectoryPath.exists() == false )
			outputDirectoryPath.mkdirs();
		
		this.visitImages(firstPage, lastPage);
	}

	@Override
	void visitImage(BufferedImage image, String imageName, int pageIndex,
			int imageIndex) {
		
		// Each page gets its own directory
		String pageDirectory=this.outputDirectory + SEPARATOR + pageIndex;

		File pagePath = new File( pageDirectory );
		if( pagePath.exists() == false )
			pagePath.mkdirs();
		
		String fileName = this.getPdfFile().getName().substring(0, this.getPdfFile().getName().lastIndexOf('.'));
		fileName += "_" + pageIndex + "_" + imageIndex + "." + SUFFIX;
		try {
			ImageIO.write(image,SUFFIX,new File(pageDirectory + SEPARATOR + fileName));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
