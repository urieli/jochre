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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.JochreException;
import com.joliciel.jochre.analyser.AnalyserService;
import com.joliciel.jochre.analyser.ImageAnalyser;
import com.joliciel.jochre.analyser.LetterAssigner;
import com.joliciel.jochre.analyser.OriginalShapeLetterAssigner;
import com.joliciel.jochre.boundaries.BoundaryDetector;
import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.boundaries.MergeOutcome;
import com.joliciel.jochre.boundaries.ShapeMerger;
import com.joliciel.jochre.boundaries.ShapeSplitter;
import com.joliciel.jochre.boundaries.SplitCandidateFinder;
import com.joliciel.jochre.boundaries.SplitOutcome;
import com.joliciel.jochre.boundaries.features.BoundaryFeatureService;
import com.joliciel.jochre.boundaries.features.MergeFeature;
import com.joliciel.jochre.boundaries.features.SplitFeature;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Segmenter;
import com.joliciel.jochre.graphics.SourceImage;
import com.joliciel.jochre.letterGuesser.Letter;
import com.joliciel.jochre.letterGuesser.LetterGuesser;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.jochre.letterGuesser.features.LetterFeature;
import com.joliciel.jochre.letterGuesser.features.LetterFeatureService;
import com.joliciel.jochre.lexicon.MostLikelyWordChooser;
import com.joliciel.jochre.security.User;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.Monitorable;
import com.joliciel.talismane.utils.MultiTaskProgressMonitor;
import com.joliciel.talismane.utils.ProgressMonitor;
import com.joliciel.talismane.utils.SimpleProgressMonitor;

/**
 * A utility class to create and analyse JochreDocuments out of a source file
 * containing multiple pages and images (typically a PDF file).
 * @author Assaf Urieli
 *
 */
class JochreDocumentGeneratorImpl implements JochreDocumentGenerator {
	private static final Log LOG = LogFactory.getLog(JochreDocumentGeneratorImpl.class);

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
	boolean analyse = false;
	JochreDocument doc = null;
	User currentUser = null;
	File letterModelFile = null;
	File splitModelFile = null;
	File mergeModelFile = null;
	
	MostLikelyWordChooser wordChooser;
	
	ImageAnalyser analyser = null;
	LetterGuesser letterGuesser = null;
	
	boolean showSegmentation = false;
	MultiTaskProgressMonitor currentMonitor;
	
	List<ProcessedImageObserver> processedImageObservers = new ArrayList<ProcessedImageObserver>();

	/**
	 * Constructor for existing documents.
	 * @param jochreDocument existing document to which we want to add stuff
	 */
	public JochreDocumentGeneratorImpl(JochreDocument jochreDocument) {
		this.doc = jochreDocument;
	}
		
	/**
	 * Constructor
	 * @param filename name of the document (required if saving)
	 * @param userFriendlyName user-friendly name for the document (required if saving)
	 * @param locale document's locale
	 */
	public JochreDocumentGeneratorImpl(String filename,
			String userFriendlyName, Locale locale) {
		this.filename = filename;
		this.userFriendlyName = userFriendlyName;
		this.locale = locale;
	}
	
	@Override
	public JochreDocument onDocumentStart() {
		LOG.debug("JochreDocumentGeneratorImpl.onDocumentStart");
		
		if (this.doc==null) {
			this.doc = this.documentService.getEmptyJochreDocument();
	
			this.doc.setFileName(filename);
			this.doc.setName(userFriendlyName);
			this.doc.setLocale(locale);
			if (save) {
				if (this.currentUser==null) {
					throw new JochreException("Cannot save a document without an owner - please specify the user.");
				}
				this.doc.setOwner(this.currentUser);
				this.doc.save();
			}
		}
		
		return this.doc;
	}


	@Override
	public void onDocumentComplete(JochreDocument doc) {
		// nothing to do here
	}


	@Override
	public JochreDocument getDocument() {
		return this.doc;
	}

	@Override
	public JochrePage onPageStart(int pageIndex) {
		JochrePage currentPage = this.doc.newPage();
		currentPage.setIndex(pageIndex);
		if (save)
			currentPage.save();
		return currentPage;
	}
	
	@Override
	public void onPageComplete(JochrePage jochrePage) {
		jochrePage.clearMemory();
	}

	@Override
	public JochreImage onImageFound(JochrePage jochrePage, BufferedImage image, String imageName,
			int imageIndex) {
		try {
			if (currentMonitor != null) {
				currentMonitor.setCurrentAction("imageMonitor.segmentingImage", new Object[] {jochrePage.getIndex()});
			}
			SourceImage sourceImage = jochrePage.newJochreImage(image, imageName+ '.' +SUFFIX);
			if (currentUser!=null)
				sourceImage.setOwner(currentUser);
			
			Segmenter segmenter = graphicsService.getSegmenter(sourceImage);
			segmenter.setDrawSegmentation(showSegmentation);
			if (currentMonitor!=null) {
				ProgressMonitor monitor = segmenter.monitorTask();
				double percentAlloted = 1;
				if (analyse&&save) {
					percentAlloted = 0.3;
				} else if (analyse) {
					percentAlloted = 0.4;
				} else if (save) {
					percentAlloted = 0.8;
				}
				currentMonitor.startTask(monitor, percentAlloted);
			}
			segmenter.segment();
			if (currentMonitor!=null)
				currentMonitor.endTask();
			
			if (showSegmentation) {
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
				if (currentMonitor != null) {
					SimpleProgressMonitor monitor = new SimpleProgressMonitor();
					monitor.setCurrentAction("imageMonitor.savingImage", new Object[] {jochrePage.getIndex()});
					double percentAlloted = 0.2;
					if (analyse)
						percentAlloted = 0.1;
					currentMonitor.startTask(monitor, percentAlloted);
				}
				sourceImage.save();
				if (currentMonitor!=null) {
					currentMonitor.endTask();
				}
			}
			
			if (analyse) {

					
				if (currentMonitor!=null&&analyser instanceof Monitorable) {
					ProgressMonitor monitor = ((Monitorable)analyser).monitorTask();
					currentMonitor.startTask(monitor, 0.6);
				}

				analyser.analyse(letterGuesser, sourceImage);
				
				if (currentMonitor!=null&&analyser instanceof Monitorable) {
					currentMonitor.endTask();
				}
			}
			
			for (ProcessedImageObserver observer : processedImageObservers) {
				observer.onImageProcessed(sourceImage);
			}
			return sourceImage;
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
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

	public void setBoundaryFeatureService(
			BoundaryFeatureService boundaryFeatureService) {
		this.boundaryFeatureService = boundaryFeatureService;
	}

	public void addProcessedImageObserver(ProcessedImageObserver observer) {
		this.processedImageObservers.add(observer);
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
	public MostLikelyWordChooser getWordChooser() {
		return wordChooser;
	}


	@Override
	public boolean isShowSegmentation() {
		return showSegmentation;
	}

	@Override
	public boolean isAnalyse() {
		return analyse;
	}

	@Override
	public void requestAnalysis(File letterModelFile,
			MostLikelyWordChooser wordChooser) {
		this.requestAnalysis(null, null, letterModelFile, wordChooser);
	}

	@Override
	public void requestAnalysis(File splitModelFile, File mergeModelFile,
			File letterModelFile, MostLikelyWordChooser wordChooser) {
		this.analyse = true;
		this.letterModelFile = letterModelFile;
		this.splitModelFile = splitModelFile;
		this.mergeModelFile = mergeModelFile;
		this.wordChooser = wordChooser;
		
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(letterModelFile));
			MachineLearningModel<Letter> letterModel = machineLearningService.getModel(zis);

			List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
			Set<LetterFeature<?>> letterFeatures = letterFeatureService.getLetterFeatureSet(letterFeatureDescriptors);
			letterGuesser = letterGuesserService.getLetterGuesser(letterFeatures, letterModel.getDecisionMaker());
			analyser = analyserService.getBeamSearchImageAnalyzer(5, 0.01, wordChooser);

			BoundaryDetector boundaryDetector = null;
			
			if (splitModelFile!=null && mergeModelFile!=null) {
				double minWidthRatioForSplit = 1.1;
				double minHeightRatioForSplit = 1.0;
				int splitBeamWidth = 5;
				int maxSplitDepth = 2;
				
				SplitCandidateFinder splitCandidateFinder = boundaryService.getSplitCandidateFinder();
				splitCandidateFinder.setMinDistanceBetweenSplits(5);
				
				ZipInputStream splitZis = new ZipInputStream(new FileInputStream(splitModelFile));
				MachineLearningModel<SplitOutcome> splitModel = machineLearningService.getModel(splitZis);
				List<String> splitFeatureDescriptors = splitModel.getFeatureDescriptors();
				Set<SplitFeature<?>> splitFeatures = boundaryFeatureService.getSplitFeatureSet(splitFeatureDescriptors);
				ShapeSplitter shapeSplitter = boundaryService.getShapeSplitter(splitCandidateFinder, splitFeatures, splitModel.getDecisionMaker(), minWidthRatioForSplit, splitBeamWidth, maxSplitDepth);
			
				ZipInputStream mergeZis = new ZipInputStream(new FileInputStream(splitModelFile));
				MachineLearningModel<MergeOutcome> mergeModel = machineLearningService.getModel(mergeZis);
				List<String> mergeFeatureDescriptors = mergeModel.getFeatureDescriptors();
				Set<MergeFeature<?>> mergeFeatures = boundaryFeatureService.getMergeFeatureSet(mergeFeatureDescriptors);
				double maxWidthRatioForMerge = 1.2;
				double maxDistanceRatioForMerge = 0.15;
				double minProbForDecision = 0.7;
				
				ShapeMerger shapeMerger = boundaryService.getShapeMerger(mergeFeatures, mergeModel.getDecisionMaker());

				boundaryDetector = boundaryService.getDeterministicBoundaryDetector(shapeSplitter, shapeMerger, minProbForDecision);
				boundaryDetector.setMinWidthRatioForSplit(minWidthRatioForSplit);
				boundaryDetector.setMinHeightRatioForSplit(minHeightRatioForSplit);
				boundaryDetector.setMaxWidthRatioForMerge(maxWidthRatioForMerge);
				boundaryDetector.setMaxDistanceRatioForMerge(maxDistanceRatioForMerge);
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
			

		} catch (Exception e) {
			LogUtils.logError(LOG, e);
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

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}
	
}
