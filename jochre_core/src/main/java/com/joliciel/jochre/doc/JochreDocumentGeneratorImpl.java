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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.analyser.AnalyserService;
import com.joliciel.jochre.analyser.ImageAnalyser;
import com.joliciel.jochre.analyser.LetterAssigner;
import com.joliciel.jochre.analyser.OriginalShapeLetterAssigner;
import com.joliciel.jochre.boundaries.BoundaryDetector;
import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.boundaries.features.BoundaryFeatureService;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Segmenter;
import com.joliciel.jochre.graphics.SourceImage;
import com.joliciel.jochre.letterGuesser.LetterGuesser;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.jochre.letterGuesser.features.LetterFeature;
import com.joliciel.jochre.letterGuesser.features.LetterFeatureService;
import com.joliciel.jochre.lexicon.MostLikelyWordChooser;
import com.joliciel.jochre.security.User;
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.utils.Monitorable;
import com.joliciel.talismane.utils.MultiTaskProgressMonitor;
import com.joliciel.talismane.utils.ProgressMonitor;
import com.joliciel.talismane.utils.SimpleProgressMonitor;

class JochreDocumentGeneratorImpl implements JochreDocumentGenerator {
	private static final Logger LOG = LoggerFactory.getLogger(JochreDocumentGeneratorImpl.class);

	private static String SUFFIX = "png";
	GraphicsService graphicsService;
	DocumentService documentService;
	LetterGuesserService letterGuesserService;
	AnalyserService analyserService;
	BoundaryService boundaryService;
	LetterFeatureService letterFeatureService;
	BoundaryFeatureService boundaryFeatureService;
	MachineLearningService machineLearningService;

	File outputDirectory = null;
	String filename = "";
	String userFriendlyName = "";
	Locale locale = null;
	boolean save = false;
	JochreDocument doc = null;
	User currentUser = null;
	File letterModelFile = null;
	File splitModelFile = null;
	File mergeModelFile = null;

	boolean showSegmentation = false;
	boolean drawPixelSpread = false;

	MultiTaskProgressMonitor currentMonitor;

	List<DocumentObserver> documentObservers = new ArrayList<DocumentObserver>();

	/**
	 * Constructor for existing documents.
	 * 
	 * @param jochreDocument
	 *            existing document to which we want to add stuff
	 */
	public JochreDocumentGeneratorImpl(JochreDocument jochreDocument) {
		this.doc = jochreDocument;
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
	public JochreDocumentGeneratorImpl(String filename, String userFriendlyName, Locale locale) {
		this.filename = filename;
		this.userFriendlyName = userFriendlyName;
		this.locale = locale;
	}

	@Override
	public JochreDocument onDocumentStart() {
		LOG.debug("JochreDocumentGeneratorImpl.onDocumentStart");

		if (this.doc == null) {
			this.doc = this.documentService.getEmptyJochreDocument();

			this.doc.setFileName(filename);
			this.doc.setName(userFriendlyName);
			this.doc.setLocale(locale);
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
			Segmenter segmenter = graphicsService.getSegmenter(sourceImage);
			segmenter.setDrawSegmentation(showSegmentation);
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

			if (showSegmentation) {
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

	public GraphicsService getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}

	public DocumentService getDocumentService() {
		return documentService;
	}

	public void setDocumentService(DocumentService documentService) {
		this.documentService = documentService;
	}

	public LetterGuesserService getLetterGuesserService() {
		return letterGuesserService;
	}

	public void setLetterGuesserService(LetterGuesserService letterGuesserService) {
		this.letterGuesserService = letterGuesserService;
	}

	public AnalyserService getAnalyserService() {
		return analyserService;
	}

	public void setAnalyserService(AnalyserService analyserService) {
		this.analyserService = analyserService;
	}

	@Override
	public ProgressMonitor monitorTask() {
		currentMonitor = new MultiTaskProgressMonitor();
		return currentMonitor;
	}

	public BoundaryService getBoundaryService() {
		return boundaryService;
	}

	public void setBoundaryService(BoundaryService boundaryService) {
		this.boundaryService = boundaryService;
	}

	public LetterFeatureService getLetterFeatureService() {
		return letterFeatureService;
	}

	public void setLetterFeatureService(LetterFeatureService letterFeatureService) {
		this.letterFeatureService = letterFeatureService;
	}

	public BoundaryFeatureService getBoundaryFeatureService() {
		return boundaryFeatureService;
	}

	public void setBoundaryFeatureService(BoundaryFeatureService boundaryFeatureService) {
		this.boundaryFeatureService = boundaryFeatureService;
	}

	@Override
	public void addDocumentObserver(DocumentObserver observer) {
		this.documentObservers.add(observer);
	}

	@Override
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

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	@Override
	public boolean isSave() {
		return save;
	}

	@Override
	public User getCurrentUser() {
		return currentUser;
	}

	@Override
	public File getLetterModelFile() {
		return letterModelFile;
	}

	@Override
	public File getSplitModelFile() {
		return splitModelFile;
	}

	@Override
	public File getMergeModelFile() {
		return mergeModelFile;
	}

	@Override
	public boolean isShowSegmentation() {
		return showSegmentation;
	}

	@Override
	public void requestAnalysis(File letterModelFile, MostLikelyWordChooser wordChooser) {
		this.requestAnalysis(null, null, letterModelFile, wordChooser);
	}

	@Override
	public void requestAnalysis(File splitModelFile, File mergeModelFile, File letterModelFile, MostLikelyWordChooser wordChooser) {
		this.letterModelFile = letterModelFile;
		this.splitModelFile = splitModelFile;
		this.mergeModelFile = mergeModelFile;

		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(letterModelFile));
			ClassificationModel letterModel = machineLearningService.getClassificationModel(zis);

			List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
			Set<LetterFeature<?>> letterFeatures = letterFeatureService.getLetterFeatureSet(letterFeatureDescriptors);
			LetterGuesser letterGuesser = letterGuesserService.getLetterGuesser(letterFeatures, letterModel.getDecisionMaker());
			ImageAnalyser analyser = analyserService.getBeamSearchImageAnalyser(5, 0.01);
			analyser.setLetterGuesser(letterGuesser);
			analyser.setMostLikelyWordChooser(wordChooser);

			BoundaryDetector boundaryDetector = null;

			if (splitModelFile != null && mergeModelFile != null) {
				boundaryDetector = boundaryService.getBoundaryDetector(splitModelFile, mergeModelFile);
				analyser.setBoundaryDetector(boundaryDetector);

				OriginalShapeLetterAssigner shapeLetterAssigner = new OriginalShapeLetterAssigner();
				shapeLetterAssigner.setEvaluate(false);
				shapeLetterAssigner.setSave(save);
				shapeLetterAssigner.setSingleLetterMethod(false);

				analyser.addObserver(shapeLetterAssigner);
			} else {
				boundaryDetector = boundaryService.getOriginalBoundaryDetector();
				analyser.setBoundaryDetector(boundaryDetector);

				LetterAssigner letterAssigner = new LetterAssigner();
				letterAssigner.setSave(save);
				analyser.addObserver(letterAssigner);
			}

			this.documentObservers.add(0, analyser);
		} catch (Exception e) {
			LOG.error("Failed to load models", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void requestSave(User currentUser) {
		this.save = true;
		this.currentUser = currentUser;
	}

	@Override
	public void requestSegmentation(File outputDirectory) {
		this.showSegmentation = true;
		this.outputDirectory = outputDirectory;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	@Override
	public boolean isDrawPixelSpread() {
		return drawPixelSpread;
	}

	@Override
	public void setDrawPixelSpread(boolean drawPixelSpread) {
		this.drawPixelSpread = drawPixelSpread;
	}
}
