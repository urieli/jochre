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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.analyser.BeamSearchImageAnalyser;
import com.joliciel.jochre.analyser.ImageAnalyser;
import com.joliciel.jochre.analyser.LetterAssigner;
import com.joliciel.jochre.analyser.LetterGuessObserver;
import com.joliciel.jochre.analyser.OriginalShapeLetterAssigner;
import com.joliciel.jochre.boundaries.BoundaryDetector;
import com.joliciel.jochre.boundaries.DeterministicBoundaryDetector;
import com.joliciel.jochre.boundaries.OriginalBoundaryDetector;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Segmenter;
import com.joliciel.jochre.graphics.SourceImage;
import com.joliciel.jochre.letterGuesser.LetterGuesser;
import com.joliciel.jochre.letterGuesser.features.LetterFeature;
import com.joliciel.jochre.letterGuesser.features.LetterFeatureParser;
import com.joliciel.jochre.lexicon.MostLikelyWordChooser;
import com.joliciel.jochre.security.User;
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.utils.Monitorable;
import com.joliciel.talismane.utils.MultiTaskProgressMonitor;
import com.joliciel.talismane.utils.ProgressMonitor;
import com.joliciel.talismane.utils.SimpleProgressMonitor;
import com.typesafe.config.Config;

/**
 * A utility interface to create and analyse JochreDocuments out of a source
 * file containing multiple pages and images (typically a PDF file).
 * 
 * @author Assaf Urieli
 *
 */
public class JochreDocumentGenerator implements SourceFileProcessor, Monitorable {
	private static final Logger LOG = LoggerFactory.getLogger(JochreDocumentGenerator.class);

	private static String SUFFIX = "png";

	private File outputDirectory = null;
	private String filename = "";
	private String userFriendlyName = "";
	private boolean save = false;
	private JochreDocument doc = null;
	private User currentUser = null;

	private boolean drawSegmentedImage = false;
	private boolean drawPixelSpread = false;

	private MultiTaskProgressMonitor currentMonitor;

	private List<DocumentObserver> documentObservers = new ArrayList<DocumentObserver>();

	private final JochreSession jochreSession;

	/**
	 * Constructor for existing documents.
	 * 
	 * @param jochreDocument
	 *            existing document to which we want to add stuff
	 */
	public JochreDocumentGenerator(JochreDocument jochreDocument, JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.doc = jochreDocument;

		Config segmenterConfig = jochreSession.getConfig().getConfig("jochre.segmenter");
		drawSegmentedImage = segmenterConfig.getBoolean("draw-segmented-image");
		drawPixelSpread = segmenterConfig.getBoolean("draw-pixel-spread");
	}

	/**
	 * Constructor
	 * 
	 * @param filename
	 *            name of the document (required if saving)
	 * @param userFriendlyName
	 *            user-friendly name for the document (required if saving)
	 * @param locale
	 *            document's locale
	 */
	public JochreDocumentGenerator(String filename, String userFriendlyName, JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.filename = filename;
		this.userFriendlyName = userFriendlyName;
	}

	@Override
	public JochreDocument onDocumentStart() {
		LOG.debug("JochreDocumentGeneratorImpl.onDocumentStart");

		if (this.doc == null) {
			this.doc = new JochreDocument(jochreSession);

			this.doc.setFileName(filename);
			this.doc.setName(userFriendlyName);
			this.doc.setLocale(jochreSession.getLocale());
			if (save) {
				LOG.debug("saving document");
				if (this.currentUser == null) {
					throw new JochreException("Cannot save a document without an owner - please specify the user.");
				}
				this.doc.setOwner(this.currentUser);
				this.doc.save();
			}
		}

		for (DocumentObserver observer : documentObservers)
			observer.onDocumentStart(this.doc);

		return this.doc;
	}

	@Override
	public void onDocumentComplete(JochreDocument doc) {
		LOG.debug("JochreDocumentGeneratorImpl.onDocumentComplete");
		for (DocumentObserver observer : documentObservers)
			observer.onDocumentComplete(doc);
	}

	@Override
	public JochreDocument getDocument() {
		return this.doc;
	}

	@Override
	public JochrePage onPageStart(int pageIndex) {
		LOG.debug("JochreDocumentGeneratorImpl.onPageStart(" + pageIndex + ")");
		JochrePage jochrePage = this.doc.newPage();
		jochrePage.setIndex(pageIndex);
		if (save)
			jochrePage.save();
		for (DocumentObserver observer : documentObservers)
			observer.onPageStart(jochrePage);
		return jochrePage;
	}

	@Override
	public void onPageComplete(JochrePage jochrePage) {
		LOG.debug("JochreDocumentGeneratorImpl.onPageComplete(" + jochrePage.getIndex() + ")");
		for (DocumentObserver observer : documentObservers)
			observer.onPageComplete(jochrePage);
		jochrePage.clearMemory();
	}

	@Override
	public JochreImage onImageFound(JochrePage jochrePage, BufferedImage image, String imageName, int imageIndex) {
		LOG.debug("JochreDocumentGeneratorImpl.onImageFound");
		try {
			int monitorableCount = 0;
			for (DocumentObserver observer : documentObservers) {
				if (observer instanceof Monitorable)
					monitorableCount++;
			}

			if (currentMonitor != null) {
				currentMonitor.setCurrentAction("imageMonitor.segmentingImage", new Object[] { jochrePage.getIndex() });
			}
			LOG.debug("Creating source image object");
			SourceImage sourceImage = jochrePage.newJochreImage(image, imageName + '.' + SUFFIX);
			sourceImage.setDrawPixelSpread(drawPixelSpread);

			if (currentUser != null)
				sourceImage.setOwner(currentUser);

			LOG.debug("Running observers onImageStart");
			for (DocumentObserver observer : documentObservers)
				observer.onImageStart(sourceImage);

			LOG.debug("Segmenting image");
			Segmenter segmenter = new Segmenter(sourceImage, jochreSession);
			segmenter.setDrawSegmentation(drawSegmentedImage);
			if (currentMonitor != null) {
				ProgressMonitor monitor = segmenter.monitorTask();
				double percentAlloted = 1;
				if (monitorableCount > 0 && save) {
					percentAlloted = 0.3;
				} else if (monitorableCount > 0) {
					percentAlloted = 0.4;
				} else if (save) {
					percentAlloted = 0.8;
				}
				currentMonitor.startTask(monitor, percentAlloted);
			}
			segmenter.segment();
			if (currentMonitor != null)
				currentMonitor.endTask();

			if (drawSegmentedImage) {
				LOG.debug("Writing segmentation file");
				BufferedImage segmentedImage = segmenter.getSegmentedImage();
				File imageFile = new File(outputDirectory, imageName + "_seg.png");
				LOG.debug("Writing segmented image to " + imageFile.getAbsolutePath());
				ImageIO.write(segmentedImage, "PNG", imageFile);
			}
			sourceImage.setImageStatus(ImageStatus.AUTO_NEW);

			if (currentMonitor != null) {
				currentMonitor.setCurrentAction("");
			}

			if (save) {
				LOG.debug("Saving image");
				if (currentMonitor != null) {
					SimpleProgressMonitor monitor = new SimpleProgressMonitor();
					monitor.setCurrentAction("imageMonitor.savingImage", new Object[] { jochrePage.getIndex() });
					double percentAlloted = 0.2;
					if (monitorableCount > 0)
						percentAlloted = 0.1;
					currentMonitor.startTask(monitor, percentAlloted);
				}
				sourceImage.save();
				if (currentMonitor != null) {
					currentMonitor.endTask();
				}
			}

			LOG.debug("Running observers onImageComplete");

			for (DocumentObserver observer : documentObservers) {
				if (currentMonitor != null && observer instanceof Monitorable) {
					ProgressMonitor monitor = ((Monitorable) observer).monitorTask();
					currentMonitor.startTask(monitor, 0.6 / monitorableCount);
				}
				observer.onImageComplete(sourceImage);
				if (currentMonitor != null && observer instanceof Monitorable) {
					currentMonitor.endTask();
				}
			}
			return sourceImage;
		} catch (IOException ioe) {
			LOG.error("Failed to process image", ioe);
			throw new RuntimeException(ioe);
		} finally {
			LOG.debug("Exit JochreDocumentGeneratorImpl.onImageFound");
		}
	}

	@Override
	public ProgressMonitor monitorTask() {
		currentMonitor = new MultiTaskProgressMonitor();
		return currentMonitor;
	}

	public void addDocumentObserver(DocumentObserver observer) {
		this.documentObservers.add(observer);
	}

	/**
	 * Where to save segmented images
	 */
	public File getOutputDirectory() {
		return outputDirectory;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getUserFriendlyName() {
		return userFriendlyName;
	}

	public void setUserFriendlyName(String userFriendlyName) {
		this.userFriendlyName = userFriendlyName;
	}

	/**
	 * Should the document be saved?
	 */
	public boolean isSave() {
		return save;
	}

	/**
	 * The current user (required if saving)
	 */
	public User getCurrentUser() {
		return currentUser;
	}

	/**
	 * Should segmented images be generated?
	 */
	public boolean isDrawSegmentedImage() {
		return drawSegmentedImage;
	}

	/**
	 * Call if this document should be analysed for letters, after applying
	 * split/merge models.
	 */
	public void requestAnalysis(MostLikelyWordChooser wordChooser) {
		try {
			ClassificationModel letterModel = jochreSession.getLetterModel();

			List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
			LetterFeatureParser letterFeatureParser = new LetterFeatureParser();
			Set<LetterFeature<?>> letterFeatures = letterFeatureParser.getLetterFeatureSet(letterFeatureDescriptors);
			LetterGuesser letterGuesser = new LetterGuesser(letterFeatures, letterModel.getDecisionMaker());

			BoundaryDetector boundaryDetector = null;
			LetterGuessObserver observer = null;
			if (jochreSession.getSplitModel() != null && jochreSession.getMergeModel() != null) {
				boundaryDetector = new DeterministicBoundaryDetector(jochreSession.getSplitModel(), jochreSession.getMergeModel(), jochreSession);

				OriginalShapeLetterAssigner shapeLetterAssigner = new OriginalShapeLetterAssigner();
				shapeLetterAssigner.setEvaluate(false);
				shapeLetterAssigner.setSave(save);
				shapeLetterAssigner.setSingleLetterMethod(false);

				observer = shapeLetterAssigner;
			} else {
				boundaryDetector = new OriginalBoundaryDetector();

				LetterAssigner letterAssigner = new LetterAssigner();
				letterAssigner.setSave(save);
				observer = letterAssigner;
			}

			ImageAnalyser analyser = new BeamSearchImageAnalyser(boundaryDetector, letterGuesser, wordChooser, jochreSession);
			analyser.addObserver(observer);

			this.documentObservers.add(0, analyser);
		} catch (Exception e) {
			LOG.error("Failed to load models", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Call if this document should be saved to the database.
	 */
	public void requestSave(User currentUser) {
		this.save = true;
		this.currentUser = currentUser;
	}

	/**
	 * Call if the segmented images should be saved to a directory.
	 */
	public void requestSegmentation(File outputDirectory) {
		this.drawSegmentedImage = true;
		this.outputDirectory = outputDirectory;
	}

	public boolean isDrawPixelSpread() {
		return drawPixelSpread;
	}

	public void setDrawPixelSpread(boolean drawPixelSpread) {
		this.drawPixelSpread = drawPixelSpread;
	}
}
