package com.joliciel.jochre.doc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.EntityImpl;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Segmenter;
import com.joliciel.jochre.graphics.SourceImage;

class JochrePageImpl extends EntityImpl implements JochrePageInternal {
    private static final Log LOG = LogFactory.getLog(JochrePageImpl.class);
	private DocumentServiceInternal documentService;
	private GraphicsService graphicsService;
	
	private int index;
	private int documentId;
	private JochreDocument document;
	private List<JochreImage> jochreImages;
	
	@Override
	public JochreDocument getDocument() {
		if (this.document==null && this.documentId!=0)
			this.document = this.documentService.loadJochreDocument(this.documentId);
		return this.document;
	}

	@Override
	public List<JochreImage> getImages() {
		if (this.jochreImages==null) {
			if (this.isNew())
				this.jochreImages = new ArrayList<JochreImage>();
			else
				this.jochreImages = this.graphicsService.findImages(this);
		}
		return this.jochreImages;
	}

	@Override
	public SourceImage newJochreImage(BufferedImage image, String imageName) {
		SourceImage jochreImage = this.graphicsService.getSourceImage(this, imageName, image);
		this.getImages().add(jochreImage);
		jochreImage.setIndex(this.getImages().size());
		return jochreImage;
	}

	@Override
	public void saveInternal() {
		if (this.document!=null && this.documentId==0)
			this.documentId = this.document.getId();
		this.documentService.saveJochrePage(this);
		if (this.jochreImages!=null) {
			for (JochreImage jochreImage : this.jochreImages) {
				jochreImage.save();
			}
		}
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getDocumentId() {
		return documentId;
	}

	public void setDocumentId(int documentId) {
		this.documentId = documentId;
	}

	public DocumentServiceInternal getGraphicsService() {
		return documentService;
	}

	public void setGraphicsService(DocumentServiceInternal graphicsService) {
		this.documentService = graphicsService;
	}

	@Override
	public void setDocument(JochreDocument jochreDocument) {
		this.document = jochreDocument;
		this.documentId = jochreDocument.getId();
	}

	@Override
	public void segment() {
		int i = 0;
		
		for (JochreImage image : this.getImages()) {
			SourceImage sourceImage = (SourceImage) image;
			Segmenter segmenter = graphicsService.getSegmenter(sourceImage);
			segmenter.segment();
			LOG.debug("Image " + i + " segmented: " + sourceImage.getName());
			i++;
		}
	}

	@Override
	public void segmentAndShow(String outputDirectory) {
		int i = 0;
		
		for (JochreImage image : this.getImages()) {
			SourceImage sourceImage = (SourceImage) image;
			Segmenter segmenter = graphicsService.getSegmenter(sourceImage);
			segmenter.setDrawSegmentation(true);
			segmenter.segment();
			
			BufferedImage segmentedImage = segmenter.getSegmentedImage();
			try {
				ImageIO.write(segmentedImage, "PNG", new File(outputDirectory + "/" + image.getName() + "_seg.png"));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			LOG.debug("Image " + i + " segmented: " + sourceImage.getName());
			i++;
		}
	}

	@Override
	public void clearMemory() {
		this.jochreImages = null;
		System.gc();
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}

	public DocumentServiceInternal getDocumentService() {
		return documentService;
	}

	public void setDocumentService(DocumentServiceInternal documentService) {
		this.documentService = documentService;
	}
	

	@Override
	public int hashCode() {
		if (this.isNew())
			return super.hashCode();
		else
			return ((Integer)this.getId()).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this.isNew()) {
			return super.equals(obj);
		} else {
			JochrePage other = (JochrePage) obj;
			return (this.getId()==other.getId());
		}
	}
}
