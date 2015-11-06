///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Assaf Urieli
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
package com.joliciel.jochre.search;

import java.awt.image.BufferedImage;
import java.io.File;

import com.joliciel.jochre.utils.pdf.AbstractPdfImageVisitor;

public class PdfImageReader {
	File pdfFile = null;
	public PdfImageReader(File pdfFile) {
		this.pdfFile = pdfFile;
	}

	public BufferedImage readImage(int pageNumber) {
		PdfImageReaderInternal imageReader = new PdfImageReaderInternal(this.pdfFile);
		BufferedImage image = imageReader.readImage(pageNumber);
		return image;
	}

	public final static class PdfImageReaderInternal extends AbstractPdfImageVisitor {
		BufferedImage image = null;
		
		public PdfImageReaderInternal(File pdfFile) {
			super(pdfFile);
		}
		
		public BufferedImage readImage(int pageNumber) {
			super.visitImages(pageNumber, pageNumber);
			return this.image;
		}
		
		@Override
		protected void visitImage(BufferedImage image, String imageName,
				int pageIndex, int imageIndex) {
			this.image = image;
		}
		
	}
}
