package com.joliciel.jochre.output;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.JochreImage;

public class ImageExtractor implements DocumentObserver {
	private static final Logger LOG = LoggerFactory.getLogger(ImageExtractor.class);
	private static String SUFFIX = "png";
	private File outDir;
	private String baseName;

	public ImageExtractor(File outDir, String baseName) {
		super();
		this.outDir = outDir;
		this.baseName = baseName;
	}

	@Override
	public void onDocumentStart(JochreDocument jochreDocument) {
	}

	@Override
	public void onPageStart(JochrePage jochrePage) {
	}

	@Override
	public void onImageStart(JochreImage jochreImage) {
		File outputFile = new File(outDir, this.getImageBaseName(jochreImage) + "." + SUFFIX);
		try {
			outputFile.delete();
			ImageIO.write(jochreImage.getOriginalImage(), SUFFIX, outputFile);
		} catch (IOException e) {
			LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
			throw new RuntimeException(e);
		}
	}

	String getImageBaseName(JochreImage jochreImage) {
		String imageBaseName = baseName + "_" + String.format("%04d", jochreImage.getPage().getIndex());
		if (jochreImage.getPage().getImages().size() > 1)
			imageBaseName += "_" + String.format("%02d", jochreImage.getIndex());
		return imageBaseName;
	}

	@Override
	public void onImageComplete(JochreImage jochreImage) {
	}

	@Override
	public void onPageComplete(JochrePage jochrePage) {
	}

	@Override
	public void onDocumentComplete(JochreDocument jochreDocument) {
	}
	
	@Override
	public void onAnalysisComplete() {
	}
	
}
