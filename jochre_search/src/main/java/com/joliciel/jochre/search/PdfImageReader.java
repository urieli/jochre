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
