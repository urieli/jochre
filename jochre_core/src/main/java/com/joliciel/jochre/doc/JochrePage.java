package com.joliciel.jochre.doc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.Entity;
import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.graphics.GraphicsDao;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Segmenter;
import com.joliciel.jochre.graphics.SourceImage;
import com.joliciel.jochre.utils.JochreException;

/**
 * A single page on a document being analysed.
 * 
 * @author Assaf Urieli
 *
 */
public class JochrePage implements Entity {
	private static final Logger LOG = LoggerFactory.getLogger(JochrePage.class);
	private DocumentServiceInternal documentService;

	private int index;
	private int documentId;
	private JochreDocument document;
	private List<JochreImage> jochreImages;

	private int id;

	private final JochreSession jochreSession;
	private final GraphicsDao graphicsDao;

	public JochrePage(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.graphicsDao = GraphicsDao.getInstance(jochreSession);
	}

	/**
	 * The document containing this page.
	 */

	public JochreDocument getDocument() {
		if (this.document == null && this.documentId != 0)
			this.document = this.documentService.loadJochreDocument(this.documentId);
		return this.document;
	}

	public List<JochreImage> getImages() {
		if (this.jochreImages == null) {
			if (this.id == 0)
				this.jochreImages = new ArrayList<JochreImage>();
			else
				this.jochreImages = this.graphicsDao.findImages(this);
		}
		return this.jochreImages;
	}

	public SourceImage newJochreImage(BufferedImage image, String imageName) {
		SourceImage jochreImage = new SourceImage(this, imageName, image, jochreSession);
		this.getImages().add(jochreImage);
		jochreImage.setIndex(this.getImages().size());
		return jochreImage;
	}

	@Override
	public void save() {
		if (this.document != null && this.documentId == 0)
			this.documentId = this.document.getId();
		this.documentService.saveJochrePage(this);
		if (this.jochreImages != null) {
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

	void setDocumentId(int documentId) {
		this.documentId = documentId;
	}

	public DocumentServiceInternal getGraphicsService() {
		return documentService;
	}

	public void setGraphicsService(DocumentServiceInternal graphicsService) {
		this.documentService = graphicsService;
	}

	void setDocument(JochreDocument jochreDocument) {
		this.document = jochreDocument;
		this.documentId = jochreDocument.getId();
	}

	/**
	 * For any Image on this page, segments it by converting to a JochreImage.
	 */

	public void segment() {
		int i = 0;

		for (JochreImage image : this.getImages()) {
			SourceImage sourceImage = (SourceImage) image;
			Segmenter segmenter = new Segmenter(sourceImage, jochreSession);
			segmenter.segment();
			LOG.debug("Image " + i + " segmented: " + sourceImage.getName());
			i++;
		}
	}

	/**
	 * Segment any image on this page and output the segmentation into PNG files
	 * so that they can be viewed by the user.
	 */

	public void segmentAndShow(String outputDirectory) {
		int i = 0;

		for (JochreImage image : this.getImages()) {
			SourceImage sourceImage = (SourceImage) image;
			Segmenter segmenter = new Segmenter(sourceImage, jochreSession);
			segmenter.setDrawSegmentation(true);
			segmenter.segment();

			BufferedImage segmentedImage = segmenter.getSegmentedImage();
			try {
				ImageIO.write(segmentedImage, "PNG", new File(outputDirectory + "/" + image.getName() + "_seg.png"));
			} catch (IOException e) {
				throw new JochreException(e);
			}

			LOG.debug("Image " + i + " segmented: " + sourceImage.getName());
			i++;
		}
	}

	/**
	 * Clears out objects in memory to avoid filling it up.
	 */

	public void clearMemory() {
		this.jochreImages = null;
		System.gc();
	}

	public DocumentServiceInternal getDocumentService() {
		return documentService;
	}

	public void setDocumentService(DocumentServiceInternal documentService) {
		this.documentService = documentService;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

}
