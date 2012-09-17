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
package com.joliciel.jochre;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import opennlp.model.EventStream;
import opennlp.model.GenericModelReader;
import opennlp.model.MaxentModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.analyser.AnalyserService;
import com.joliciel.jochre.analyser.ErrorLogger;
import com.joliciel.jochre.analyser.FScoreObserver;
import com.joliciel.jochre.analyser.ImageAnalyser;
import com.joliciel.jochre.analyser.LetterAssigner;
import com.joliciel.jochre.analyser.OriginalShapeLetterAssigner;
import com.joliciel.jochre.analyser.SimpleLetterFScoreObserver;
import com.joliciel.jochre.boundaries.BoundaryDetector;
import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.boundaries.MergeEvaluator;
import com.joliciel.jochre.boundaries.ShapeMerger;
import com.joliciel.jochre.boundaries.ShapeSplitter;
import com.joliciel.jochre.boundaries.SplitCandidateFinder;
import com.joliciel.jochre.boundaries.SplitEvaluator;
import com.joliciel.jochre.boundaries.features.BoundaryFeatureService;
import com.joliciel.jochre.boundaries.features.MergeFeature;
import com.joliciel.jochre.boundaries.features.SplitFeature;
import com.joliciel.jochre.doc.DocumentService;
import com.joliciel.jochre.doc.ImageDocumentExtractor;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochreDocumentGenerator;
import com.joliciel.jochre.doc.SourceFileProcessor;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.JochreCorpusGroupReader;
import com.joliciel.jochre.graphics.JochreCorpusImageReader;
import com.joliciel.jochre.graphics.JochreCorpusShapeReader;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.graphics.features.ShapeFeature;
import com.joliciel.jochre.graphics.features.VerticalElongationFeature;
import com.joliciel.jochre.letterGuesser.ComponentCharacterValidator;
import com.joliciel.jochre.letterGuesser.LetterGuesser;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.jochre.letterGuesser.LetterValidator;
import com.joliciel.jochre.letterGuesser.features.LetterFeature;
import com.joliciel.jochre.letterGuesser.features.LetterFeatureService;
import com.joliciel.jochre.letterGuesser.features.LetterFeatureTester;
import com.joliciel.jochre.lexicon.CorpusLexiconBuilder;
import com.joliciel.jochre.lexicon.LexiconService;
import com.joliciel.jochre.lexicon.MostLikelyWordChooser;
import com.joliciel.jochre.lexicon.TextFileLexicon;
import com.joliciel.jochre.lexicon.WordSplitter;
import com.joliciel.jochre.pdf.PdfImageVisitor;
import com.joliciel.jochre.pdf.PdfImageSaver;
import com.joliciel.jochre.pdf.PdfService;
import com.joliciel.jochre.security.SecurityService;
import com.joliciel.jochre.security.User;
import com.joliciel.jochre.stats.FScoreCalculator;
import com.joliciel.talismane.utils.CorpusEventStream;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.maxent.JolicielMaxentModel;
import com.joliciel.talismane.utils.maxent.MaxentDecisionMaker;
import com.joliciel.talismane.utils.maxent.MaxentEventStream;
import com.joliciel.talismane.utils.maxent.MaxentModelTrainer;
import com.joliciel.talismane.utils.maxent.OutcomeEqualiserEventStream;
import com.joliciel.talismane.utils.util.LogUtils;
import com.joliciel.talismane.utils.util.PerformanceMonitor;

/**
 * Class encapsulating the various top-level Jochre commands and command-line interface.
 * @author Assaf Urieli
 *
 */
public class Jochre {
	private static final Log LOG = LogFactory.getLog(Jochre.class);

	GraphicsService graphicsService;
	DocumentService documentService;
	AnalyserService analyserService;
	LexiconService lexiconService;
	LetterGuesserService letterGuesserService;
	BoundaryService boundaryService;
	SecurityService securityService;
	PdfService pdfService;
	LetterFeatureService letterFeatureService;
	BoundaryFeatureService boundaryFeatureService;

	String isoLanguage = "yi";
	Locale locale = null;
	int userId = -1;
	String dataSourcePropertiesPath;
	
	/**
	 * Usage (* indicates optional):<br/>
	 * Jochre load [filename] [isoLanguageCode] [firstPage]* [lastPage]*<br/>
	 * Loads a file (pdf or image) and segments it into letters.
	 * The analysed version is stored in the persistent store.
	 * Writes [filename].xml to the same location, to enable the user to indicate the text
	 * to associate with this file.<br/>
	 * Jochre extract [filename] [outputDirectory] [firstPage]* [lastPage]*<br/>
	 * Extracts images form a pdf file.<br/>
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		PerformanceMonitor.start();
		try {
			if (args.length==0) {
				System.out.println("Usage (* indicates optional):");
				System.out.println("Jochre load file=[filename] name=[userFriendlyName] lang=[isoLanguageCode] first=[firstPage]* last=[lastPage]* outputDir=[outputDirectory]* showSeg=[true/false]");
				System.out.println("Jochre extract file=[filename] outputDir=[outputDirectory] first=[firstPage]* last=[lastPage]*");
				System.out.println("Jochre analyse");
				System.out.println("Jochre train file=[filename] outputDir=[outputDirectory] iterations=[iterations] cutoff=[cutoff]");
				return;
			}
			String command = args[0];

			String filename = "";
			String userFriendlyName = "";
			String isoLanguage = "yi";
			String outputDirPath = null;
			int firstPage = -1;
			int lastPage = -1;
			int shapeId = -1;
			int docId = -1;
			int imageId = 0;
			int iterations = 0;
			int cutoff = 0;
			int userId = -1;
			int imageCount = 0;
			int multiplier = 0;
			int beamWidth = 5;
			boolean showSegmentation = false;
			boolean save = false;
			String letterModelPath = "";
			String splitModelPath = "";
			String mergeModelPath = "";
			ImageStatus testSet = ImageStatus.TRAINING_HELD_OUT;
			String letterFeatureFilePath = "";
			String splitFeatureFilePath = "";
			String mergeFeatureFilePath = "";
			boolean reconstructLetters = false;
	
			boolean firstArg = true;
			for (String arg : args) {
				if (firstArg) {
					firstArg = false;
					continue;
				}
				int equalsPos = arg.indexOf('=');
				String argName = arg.substring(0, equalsPos);
				String argValue = arg.substring(equalsPos+1);
				if (argName.equals("file"))
					filename = argValue;
				else if (argName.equals("name"))
					userFriendlyName = argValue;
				else if (argName.equals("lang"))
					isoLanguage = argValue;
				else if (argName.equals("first"))
					firstPage = Integer.parseInt(argValue);
				else if (argName.equals("last"))
					lastPage = Integer.parseInt(argValue);
				else if (argName.equals("outputDir"))
					outputDirPath = argValue;
				else if (argName.equals("showSeg"))
					showSegmentation = (argValue.equals("true"));
				else if (argName.equals("save"))
					save = (argValue.equals("true"));
				else if (argName.equals("shapeId"))
					shapeId = Integer.parseInt(argValue);
				else if (argName.equals("imageId"))
					imageId = Integer.parseInt(argValue);
				else if (argName.equals("docId"))
					docId = Integer.parseInt(argValue);
				else if (argName.equals("userId"))
					userId = Integer.parseInt(argValue);
				else if (argName.equals("iterations"))
					iterations = Integer.parseInt(argValue);
				else if (argName.equals("cutoff"))
					cutoff = Integer.parseInt(argValue);
				else if (argName.equals("imageCount"))
					imageCount = Integer.parseInt(argValue);
				else if (argName.equals("beamWidth"))
					beamWidth = Integer.parseInt(argValue);
				else if (argName.equals("multiplier"))
					multiplier = Integer.parseInt(argValue);
				else if (argName.equals("letterModel"))
					letterModelPath = argValue;
				else if (argName.equals("splitModel"))
					splitModelPath = argValue;
				else if (argName.equals("mergeModel"))
					mergeModelPath = argValue;
				else if (argName.equals("letterFeatures"))
					letterFeatureFilePath = argValue;
				else if (argName.equals("splitFeatures"))
					splitFeatureFilePath = argValue;
				else if (argName.equals("mergeFeatures"))
					mergeFeatureFilePath = argValue;
				else if (argName.equals("testSet")) {
					if (argValue.equals("heldOut"))
						testSet = ImageStatus.TRAINING_HELD_OUT;
					else if (argValue.equals("test"))
						testSet = ImageStatus.TRAINING_TEST;
					else if (argValue.equals("training"))
						testSet = ImageStatus.TRAINING_VALIDATED;
					else
						throw new RuntimeException("Unknonw testSet: " + argValue);
				}
				else if (argName.equals("reconstructLetters"))
					reconstructLetters = (argValue.equals("true"));
				else
					throw new RuntimeException("Unknown argument: " + argName);
	
				if (userFriendlyName.length()==0)
					userFriendlyName = filename;
			}    	
			
			JochreServiceLocator locator = JochreServiceLocator.getInstance();
			String dataSourcePropertiesPath = "jdbc-live.properties";
			locator.setDataSourcePropertiesResource(dataSourcePropertiesPath);

			Jochre jochre = new Jochre();
			
			jochre.setIsoLanguage(isoLanguage);
			jochre.setUserId(userId);

			if (command.equals("segment")) {
				jochre.doCommandSegment(filename, userFriendlyName, showSegmentation, outputDirPath, save, firstPage, lastPage);
			} else if (command.equals("extract")) {
				jochre.doCommandExtractImages(filename, outputDirPath, firstPage, lastPage);
			} else if (command.equals("updateImages")) {
				jochre.doCommandUpdateImages(filename, docId, firstPage, lastPage);
			} else if (command.equals("applyFeatures")) {
				jochre.doCommandApplyFeatures(imageId, shapeId, letterFeatureFilePath);
			} else if (command.equals("train")) {
				jochre.doCommandTrain(letterModelPath, letterFeatureFilePath, iterations, cutoff, imageCount, reconstructLetters);
			} else if (command.equals("evaluate")||command.equals("evaluateComplex")) {
				jochre.doCommandEvaluate(letterModelPath, testSet, imageId, outputDirPath, null, beamWidth, reconstructLetters);
			} else if (command.equals("evaluateFull")) {
				jochre.doCommandEvaluateFull(letterModelPath, splitModelPath, mergeModelPath, testSet, imageId, save, outputDirPath, null, beamWidth);
			} else if (command.equals("analyse")) {
				jochre.doCommandAnalyse(letterModelPath, docId, imageId, testSet, null);
			} else if (command.equals("trainSplits")) {
				jochre.doCommandTrainSplits(splitModelPath, splitFeatureFilePath, iterations, cutoff, imageCount);
			} else if (command.equals("evaluateSplits")) {
				jochre.doCommandEvaluateSplits(splitModelPath, testSet, imageCount, beamWidth);
			} else if (command.equals("trainMerge")) {
				jochre.doCommandTrainMerge(mergeModelPath, mergeFeatureFilePath, multiplier, iterations, cutoff, imageCount);	
			} else if (command.equals("evaluateMerge")) {
				jochre.doCommandEvaluateMerge(mergeModelPath, testSet, imageCount);
			} else if (command.equals("logImage")) {
				jochre.doCommandLogImage(shapeId);
			} else if (command.equals("testFeature")) {
				jochre.doCommandTestFeature(shapeId);
			} else {
				throw new RuntimeException("Unknown command: " + command);
			}
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw e;
		} finally {
			PerformanceMonitor.end();
		}
		LOG.debug("#### finished #####");
	}
	
	public Jochre() {
		JochreServiceLocator locator = JochreServiceLocator.getInstance();
		graphicsService = locator.getGraphicsServiceLocator().getGraphicsService();
		documentService = locator.getDocumentServiceLocator().getDocumentService();
		analyserService = locator.getAnalyserServiceLocator().getAnalyserService();
		lexiconService = locator.getLexiconServiceLocator().getLexiconService();
		letterGuesserService = locator.getLetterGuesserServiceLocator().getLetterGuesserService();
		boundaryService = locator.getBoundaryServiceLocator().getBoundaryService();
		securityService = locator.getSecurityServiceLocator().getSecurityService();
		pdfService = locator.getPdfServiceLocator().getPdfService();
		letterFeatureService = locator.getLetterFeatureServiceLocator().getLetterFeatureService();
		boundaryFeatureService = locator.getBoundaryFeatureServiceLocator().getBoundaryFeatureService();
	}

	/**
	 * Test a feature on a particular shape.
	 * @param shapeId
	 */
	public void doCommandTestFeature(int shapeId) {
		// just a utility for testing a feature on a particular shape
		ShapeFeature<?> feature = new VerticalElongationFeature();
		if (shapeId>0) {
			Shape shape = graphicsService.loadShape(shapeId);
			shape.writeImageToLog();
			
			feature.check(shape);
		} else {
			
//				String result = "false";
//				TrainingServiceLocator trainingServiceLocator = locator.getTrainingServiceLocator();
//				TrainingService trainingService = trainingServiceLocator.getTrainingService();
//				List<Integer> shapeIds = trainingService.findShapesForFeature("ג", feature, result);
			List<Integer> shapeIds = graphicsService.findShapeIds("—");
			Map<Object,Integer> outcomeMap = new HashMap<Object, Integer>();
			for (int oneShapeId : shapeIds) {
				Shape shape = graphicsService.loadShape(oneShapeId);
				shape.writeImageToLog();
				FeatureResult<?> weightedOutcome = feature.check(shape);

				Object outcome = weightedOutcome.getOutcome();
				Integer count = outcomeMap.get(outcome);
				if (count==null)
					outcomeMap.put(outcome, 1);
				else
					outcomeMap.put(outcome, count.intValue()+1);
				
			}
			LOG.debug("Outcomes:");
			for (Object outcome : outcomeMap.keySet()) {
				LOG.debug("Outcome " + outcome.toString() + ", count " + outcomeMap.get(outcome));
			}
		}
	}

	/**
	 * Rebuild the training corpus lexicon.
	 */
	public void doCommandBuildLexicon(String outputDirPath, WordSplitter wordSplitter) {
		try {
			CorpusLexiconBuilder builder = lexiconService.getCorpusLexiconBuilder(wordSplitter);
			TextFileLexicon lexicon = builder.buildLexicon();
	
			File outputDir = new File(outputDirPath);
			outputDir.mkdirs();
			File textFile = new File(outputDir, "jochreCorpusLexicon.txt");
			textFile.delete();
			Writer textFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(textFile, true),"UTF8"));
			try {
				lexicon.writeFile(textFileWriter);
			} finally {
				textFileWriter.flush();
				textFileWriter.close();
			}
	
			File lexiconFile = new File(outputDir, "jochreCorpusLexicon.zip");
			lexicon.serialize(lexiconFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Log a shape's image to the log file, to make sure it got segmented and stored correctly.
	 * @param shapeId
	 */
	public void doCommandLogImage(int shapeId) {
		// just a utility for making sure images got segmented and stored correctly
		if (shapeId>0) {
			Shape shape = graphicsService.loadShape(shapeId);
			shape.writeImageToLog();
			LOG.debug("Letter: " + shape.getLetter());
		}
	}


	/**
	 * Train the letter merging model.
	 * @param mergeModelPath the path where the model should be saved
	 * @param iterations the number of training iterations
	 * @param cutoff the feature count cutoff
	 * @param imageCount the maximum number of training corpus images to use when training - if <= 0 will use all.
	 */
	public void doCommandTrainMerge(String mergeModelPath, String mergeFeatureFilePath, int multiplier, int iterations, int cutoff, int imageCount) {
		try {
			if (mergeModelPath.length()==0)
				throw new RuntimeException("Missing argument: mergeModel");
			if (!mergeModelPath.endsWith(".zip"))
				throw new RuntimeException("mergeModel must end with .zip");
			if (mergeFeatureFilePath.length()==0)
				throw new RuntimeException("Missing argument: mergeFeatures");
			String modelDirPath = mergeModelPath.substring(0, mergeModelPath.lastIndexOf('/'));
			File modelDir = new File(modelDirPath);
			modelDir.mkdirs();
			
			ImageStatus[] imageStatusesToInclude = new ImageStatus[] { ImageStatus.TRAINING_VALIDATED };
			File mergeFeatureFile = new File(mergeFeatureFilePath);
			Scanner scanner = new Scanner(mergeFeatureFile);
	
			List<String> mergeFeatureDescriptors = new ArrayList<String>();
			while (scanner.hasNextLine()) {
				String descriptor = scanner.nextLine();
				mergeFeatureDescriptors.add(descriptor);
				LOG.debug(descriptor);
			}
			
			Set<MergeFeature<?>> mergeFeatures = this.boundaryFeatureService.getMergeFeatureSet(mergeFeatureDescriptors);
			double maxWidthRatio = 1.2;
			double maxDistanceRatio = 0.15;
			CorpusEventStream corpusEventStream = boundaryService.getJochreMergeEventStream(imageStatusesToInclude, mergeFeatures, imageCount, maxWidthRatio, maxDistanceRatio);
			EventStream eventStream = new MaxentEventStream(corpusEventStream);
			if (multiplier > 0) {
				eventStream = new OutcomeEqualiserEventStream(eventStream, multiplier);
			}
			
			File file = new File(mergeModelPath);
			MaxentModelTrainer trainer = new MaxentModelTrainer(eventStream);
			trainer.setCutoff(cutoff);
			trainer.setIterations(iterations);
			
			trainer.trainModel();
			trainer.persistModel(file);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}


	/**
	 * Evaluate the letter merging model on its own.
	 * @param mergeModelPath the path of the model to be evaluated.
	 * @param testSet the test set to be evaluated
	 * @param imageCount the maximum number of corpus images to use when testing - if <= 0 will use all.
	 * @throws IOException
	 */
	public void doCommandEvaluateMerge(String mergeModelPath, ImageStatus testSet,
			int imageCount) throws IOException {
		if (mergeModelPath.length()==0)
			throw new RuntimeException("Missing argument: mergeModel");
		if (!mergeModelPath.endsWith(".zip"))
			throw new RuntimeException("mergeModel must end with .zip");

		MaxentModel mergeModel = new GenericModelReader(new File(mergeModelPath)).getModel();
		MaxentDecisionMaker mergeDecisionMaker = new MaxentDecisionMaker(mergeModel);
		
		File mergeModelFile = new File(mergeModelPath);
		JolicielMaxentModel mergeMaxentModel = new JolicielMaxentModel(mergeModelFile);
		
		List<String> mergeFeatureDescriptors = mergeMaxentModel.getFeatureDescriptors();
		Set<MergeFeature<?>> mergeFeatures = boundaryFeatureService.getMergeFeatureSet(mergeFeatureDescriptors);
		
		ImageStatus[] imageStatusesToInclude = new ImageStatus[] { testSet };
		JochreCorpusGroupReader groupReader = graphicsService.getJochreCorpusGroupReader();
		groupReader.setImageStatusesToInclude(imageStatusesToInclude);
		groupReader.setImageCount(imageCount);
	
		double maxWidthRatio = 1.2;
		double maxDistanceRatio = 0.15;
		
		ShapeMerger merger = boundaryService.getShapeMerger(mergeFeatures, mergeDecisionMaker);
		MergeEvaluator evaluator = boundaryService.getMergeEvaluator(maxWidthRatio, maxDistanceRatio);
		FScoreCalculator<String> fScoreCalculator = evaluator.evaluate(groupReader, merger);
		LOG.debug(fScoreCalculator.getTotalFScore());
	}
	
	/**
	 * Train the letter splitting model.
	 * @param splitModelPath the path where the model should be saved
	 * @param iterations the number of training iterations
	 * @param cutoff the feature count cutoff
	 * @param imageCount the maximum number of training corpus images to use when training - if <= 0 will use all.
	 */
	public void doCommandTrainSplits(String splitModelPath, String splitFeatureFilePath, int iterations, int cutoff, int imageCount) {
		try {
			if (splitModelPath.length()==0)
				throw new RuntimeException("Missing argument: splitModel");
			if (!splitModelPath.endsWith(".zip"))
				throw new RuntimeException("splitModel must end with .zip");
			if (splitFeatureFilePath.length()==0)
				throw new RuntimeException("Missing argument: splitFeatures");
			String modelDirPath = splitModelPath.substring(0, splitModelPath.lastIndexOf('/'));
			File modelDir = new File(modelDirPath);
			modelDir.mkdirs();
			
			ImageStatus[] imageStatusesToInclude = new ImageStatus[] { ImageStatus.TRAINING_VALIDATED };
			File splitFeatureFile = new File(splitFeatureFilePath);
			Scanner scanner = new Scanner(splitFeatureFile);
	
			List<String> splitFeatureDescriptors = new ArrayList<String>();
			while (scanner.hasNextLine()) {
				String descriptor = scanner.nextLine();
				splitFeatureDescriptors.add(descriptor);
				LOG.debug(descriptor);
			}
			
			Set<SplitFeature<?>> splitFeatures = this.boundaryFeatureService.getSplitFeatureSet(splitFeatureDescriptors);
			
			double minWidthRatio = 1.1;
			double minHeightRatio = 1.0;
			CorpusEventStream corpusEventStream = boundaryService.getJochreSplitEventStream(imageStatusesToInclude, splitFeatures, imageCount, minWidthRatio, minHeightRatio);
		
			File splitModelFile = new File(splitModelPath);
			MaxentModelTrainer trainer = new MaxentModelTrainer(corpusEventStream);
			trainer.setCutoff(cutoff);
			trainer.setIterations(iterations);

			JolicielMaxentModel jolicielMaxentModel = new JolicielMaxentModel(trainer, splitFeatureDescriptors);
			jolicielMaxentModel.persist(splitModelFile);

		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Evaluate the letter splitting model on its own.
	 * @param splitModelPath the path of the model to be evaluated.
	 * @param testSet the test set to be evaluated
	 * @param imageCount the maximum number of corpus images to use when testing - if <= 0 will use all.
	 * @throws IOException
	 */
	public void doCommandEvaluateSplits(String splitModelPath, ImageStatus testSet, int imageCount, int beamWidth) throws IOException {
		if (splitModelPath.length()==0)
			throw new RuntimeException("Missing argument: splitModel");
		if (!splitModelPath.endsWith(".zip"))
			throw new RuntimeException("splitModel must end with .zip");
		
		ImageStatus[] imageStatusesToInclude = new ImageStatus[] { testSet};
		File splitModelFile = new File(splitModelPath);
		JolicielMaxentModel splitMaxentModel = new JolicielMaxentModel(splitModelFile);
		
		List<String> splitFeatureDescriptors = splitMaxentModel.getFeatureDescriptors();
		Set<SplitFeature<?>> splitFeatures = boundaryFeatureService.getSplitFeatureSet(splitFeatureDescriptors);
			
		double minWidthRatio = 1.1;
		double minHeightRatio = 1.0;
		int maxDepth = 2;
		
		SplitCandidateFinder splitCandidateFinder = boundaryService.getSplitCandidateFinder();
		splitCandidateFinder.setMinDistanceBetweenSplits(5);
		
		ShapeSplitter shapeSplitter = boundaryService.getShapeSplitter(splitCandidateFinder, splitFeatures, splitMaxentModel.getDecisionMaker(), minWidthRatio, beamWidth, maxDepth);
		
		JochreCorpusShapeReader shapeReader = graphicsService.getJochreCorpusShapeReader();
		shapeReader.setImageStatusesToInclude(imageStatusesToInclude);
		shapeReader.setImageCount(imageCount);
		
		SplitEvaluator splitEvaluator = boundaryService.getSplitEvaluator(5, minWidthRatio, minHeightRatio);
		FScoreCalculator<String> fScoreCalculator = splitEvaluator.evaluate(shapeReader, shapeSplitter);
		LOG.debug(fScoreCalculator.getTotalFScore());
	}
	
	/**
	 * Train a letter guessing model.
	 * @param letterModelPath the path where the model should be saved
	 * @param iterations the number of training iterations
	 * @param cutoff the feature count cutoff
	 * @param imageCount the maximum number of training corpus images to use when training - if <= 0 will use all.
	 */
	public void doCommandTrain(String letterModelPath, String letterFeatureFilePath, int iterations, int cutoff, int imageCount, boolean reconstructLetters) {
		try {
			if (letterModelPath.length()==0)
				throw new RuntimeException("Missing argument: letterModel");
			if (!letterModelPath.endsWith(".zip"))
				throw new RuntimeException("letterModel must end with .zip");
			if (letterFeatureFilePath.length()==0)
				throw new RuntimeException("Missing argument: letterFeatures");
			String modelDirPath = letterModelPath.substring(0, letterModelPath.lastIndexOf('/'));
			File modelDir = new File(modelDirPath);
			modelDir.mkdirs();
			
			ImageStatus[] imageStatusesToInclude = new ImageStatus[] { ImageStatus.TRAINING_VALIDATED };
			File letterFeatureFile = new File(letterFeatureFilePath);
			Scanner scanner = new Scanner(letterFeatureFile);
	
			List<String> descriptors = new ArrayList<String>();
			while (scanner.hasNextLine()) {
				String descriptor = scanner.nextLine();
				descriptors.add(descriptor);
				LOG.debug(descriptor);
			}
			Set<LetterFeature<?>> features = letterFeatureService.getLetterFeatureSet(descriptors);
			
			BoundaryDetector boundaryDetector = null;
			if (reconstructLetters) {
				ShapeSplitter splitter = boundaryService.getTrainingCorpusShapeSplitter();
				ShapeMerger merger = boundaryService.getTrainingCorpusShapeMerger();
				boundaryDetector = boundaryService.getLetterByLetterBoundaryDetector(splitter, merger, 1);
			} else {
				boundaryDetector = boundaryService.getOriginalBoundaryDetector();
			}
			
			LetterValidator letterValidator = new ComponentCharacterValidator(locale);
	
			CorpusEventStream corpusEventStream = letterGuesserService.getJochreLetterEventStream(imageStatusesToInclude, features, boundaryDetector, letterValidator, imageCount);
			
			File letterModelFile = new File(letterModelPath);
			MaxentModelTrainer trainer = new MaxentModelTrainer(corpusEventStream);
			trainer.setCutoff(cutoff);
			trainer.setIterations(iterations);

			JolicielMaxentModel jolicielMaxentModel = new JolicielMaxentModel(trainer, descriptors);
			jolicielMaxentModel.persist(letterModelFile);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Evaluate a given letter guessing model.
	 * @param letterModelPath the path to the model
	 * @param testSet the set of images to be evaluated
	 * @param imageId the single image to be evaluated
	 * @param outputDirPath the directory to which we write the evaluation files
	 * @param lexicon the lexicon to use for word correction
	 * @throws IOException
	 */
	public void doCommandEvaluate(String letterModelPath, ImageStatus testSet, int imageId,
			String outputDirPath, MostLikelyWordChooser wordChooser, int beamWidth, boolean reconstructLetters) throws IOException {
		if (letterModelPath.length()==0)
			throw new RuntimeException("Missing argument: letterModel");
		if (!letterModelPath.endsWith(".zip"))
			throw new RuntimeException("letterModel must end with .zip");

		File letterModelFile = new File(letterModelPath);
		JolicielMaxentModel letterMaxentModel = new JolicielMaxentModel(letterModelFile);
		
		List<String> letterFeatureDescriptors = letterMaxentModel.getFeatureDescriptors();
		Set<LetterFeature<?>> letterFeatures = letterFeatureService.getLetterFeatureSet(letterFeatureDescriptors);
			
		LetterGuesser letterGuesser = letterGuesserService.getLetterGuesser(letterFeatures, letterMaxentModel.getDecisionMaker());
		
		JochreCorpusImageReader imageReader = graphicsService.getJochreCorpusImageReader();
		imageReader.setImageStatusesToInclude(new ImageStatus[] {testSet});
		imageReader.setImageId(imageId);

		BoundaryDetector boundaryDetector = null;
		if (reconstructLetters) {
			ShapeSplitter splitter = boundaryService.getTrainingCorpusShapeSplitter();
			ShapeMerger merger = boundaryService.getTrainingCorpusShapeMerger();
			boundaryDetector = boundaryService.getLetterByLetterBoundaryDetector(splitter, merger, 1);
			boundaryDetector.setMinWidthRatioForSplit(0.0);
			boundaryDetector.setMinHeightRatioForSplit(0.0);
			boundaryDetector.setMaxWidthRatioForMerge(100.0);
			boundaryDetector.setMaxDistanceRatioForMerge(100.0);
		} else {
			boundaryDetector = boundaryService.getOriginalBoundaryDetector();
		}
		
		ImageAnalyser evaluator = analyserService.getBeamSearchImageAnalyzer(beamWidth, 0.01, wordChooser);
		evaluator.setBoundaryDetector(boundaryDetector);
	
		FScoreObserver fScoreObserver = null;
		LetterValidator letterValidator = new ComponentCharacterValidator(locale);
		if (reconstructLetters) {
			OriginalShapeLetterAssigner originalShapeLetterAssigner = new OriginalShapeLetterAssigner();
			originalShapeLetterAssigner.setEvaluate(true);
			originalShapeLetterAssigner.setSave(false);
			originalShapeLetterAssigner.setLetterValidator(letterValidator);
        	
			fScoreObserver = originalShapeLetterAssigner;
		} else {
			LetterAssigner letterAssigner = new LetterAssigner();
			evaluator.addObserver(letterAssigner);
			
			fScoreObserver = new SimpleLetterFScoreObserver(letterValidator);
		}
		    	
		evaluator.addObserver(fScoreObserver);
		
		ErrorLogger errorLogger = new ErrorLogger();	
		if (wordChooser!=null) {
			errorLogger.setLexicon(wordChooser.getLexicon());
			errorLogger.setWordSplitter(wordChooser.getWordSplitter());
		}
       	Writer errorWriter = null;
    	if (outputDirPath!=null) {
    		File outputDir = new File(outputDirPath);
    		outputDir.mkdirs();
			File errorFile = new File(outputDir, "errors.txt");
			errorFile.delete();
			errorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile, true),"UTF8"));
    	}
    	errorLogger.setErrorWriter(errorWriter);
    	evaluator.addObserver(errorLogger);
		
//			evaluator.setOutcomesToAnalyse(new String[] {"מ"});
		try {
			evaluator.analyse(letterGuesser, imageReader);
		} finally {
			if (errorWriter!=null)
				errorWriter.close();
		}
		LOG.debug("F-score for " + letterModelPath + ": " + fScoreObserver.getFScoreCalculator().getTotalFScore());
		if (outputDirPath!=null) {
			File outputDir = new File(outputDirPath);
			outputDir.mkdirs();
			String modelFileName = letterModelPath.substring(letterModelPath.lastIndexOf('/')+1);
			if (reconstructLetters)
				modelFileName += "_Reconstruct";
			File fscoreFile = new File(outputDir, modelFileName + ".fscores.csv");
			fScoreObserver.getFScoreCalculator().writeScoresToCSVFile(fscoreFile);
		}
	}
	
	/**
	 * Analyse a document or image or test set based on a given letter-guessing model.
	 * @param letterModelPath the path to the letter-guessing model.
	 * @param docId the document to be analysed
	 * @param imageId the image to be analysed
	 * @param testSet the test set to be analysed
	 * @throws IOException
	 */
	public void doCommandAnalyse(String letterModelPath, int docId, int imageId, ImageStatus testSet, MostLikelyWordChooser wordChooser) throws IOException {
		if (letterModelPath.length()==0)
			throw new RuntimeException("Missing argument: letterModel");
		if (!letterModelPath.endsWith(".zip"))
			throw new RuntimeException("letterModel must end with .zip");
		

		File letterModelFile = new File(letterModelPath);
		JolicielMaxentModel letterMaxentModel = new JolicielMaxentModel(letterModelFile);
		
		List<String> letterFeatureDescriptors = letterMaxentModel.getFeatureDescriptors();
		Set<LetterFeature<?>> letterFeatures = letterFeatureService.getLetterFeatureSet(letterFeatureDescriptors);
		
		LetterGuesser letterGuesser = letterGuesserService.getLetterGuesser(letterFeatures, letterMaxentModel.getDecisionMaker());
		
		ImageAnalyser analyser = analyserService.getBeamSearchImageAnalyzer(5, 0.01, wordChooser);

		JochreCorpusImageReader imageReader = graphicsService.getJochreCorpusImageReader();
		imageReader.setImageStatusesToInclude(new ImageStatus[] {testSet});
		imageReader.setImageId(imageId);
		
		LetterAssigner letterAssigner = new LetterAssigner();

		analyser.addObserver(letterAssigner);
		
		if (docId>0) {
			JochreDocument doc = documentService.loadJochreDocument(docId);
			for (JochrePage page : doc.getPages()) {
				for (JochreImage image : page.getImages()) {
					if (image.getImageStatus().equals(ImageStatus.AUTO_NEW)) {
						analyser.analyse(letterGuesser, image);
					}
					image.clearMemory();
				}
			}
		} else {
			analyser.analyse(letterGuesser, imageReader);
		}

	}

	/**
	 * Evaluate a suite of split/merge models and letter guessing model.
	 * @param letterModelPath the path to the letter-guessing model
	 * @param splitModelPath the path to the splitting model
	 * @param mergeModelPath the path to the merging model
	 * @param testSet the set of images to evaluate in the saved corpus
	 * @param imageId the single image to evaluate in the saved corpus
	 * @param save whether or not the letter guesses should be saved
	 * @param outputDirPath the output directory where we write the evaluation results
	 * @throws IOException
	 */
	public void doCommandEvaluateFull(String letterModelPath, String splitModelPath, String mergeModelPath, ImageStatus testSet,
			int imageId, boolean save, String outputDirPath, MostLikelyWordChooser wordChooser, int beamWidth) throws IOException {
		if (letterModelPath.length()==0)
			throw new RuntimeException("Missing argument: letterModel");
		
		File letterModelFile = new File(letterModelPath);
		JolicielMaxentModel letterMaxentModel = new JolicielMaxentModel(letterModelFile);
		
		List<String> letterFeatureDescriptors = letterMaxentModel.getFeatureDescriptors();
		Set<LetterFeature<?>> letterFeatures = letterFeatureService.getLetterFeatureSet(letterFeatureDescriptors);
		
		LetterGuesser letterGuesser = letterGuesserService.getLetterGuesser(letterFeatures, letterMaxentModel.getDecisionMaker());
		
		if (splitModelPath.length()==0)
			throw new RuntimeException("Missing argument: splitModel");
		if (!splitModelPath.endsWith(".zip"))
			throw new RuntimeException("splitModel must end with .zip");

		MaxentModel splitModel = new GenericModelReader(new File(splitModelPath)).getModel();
		MaxentDecisionMaker splitDecisionMaker = new MaxentDecisionMaker(splitModel);
		
		File splitModelFile = new File(splitModelPath);
		JolicielMaxentModel splitMaxentModel = new JolicielMaxentModel(splitModelFile);
		
		List<String> splitFeatureDescriptors = splitMaxentModel.getFeatureDescriptors();
		Set<SplitFeature<?>> splitFeatures = boundaryFeatureService.getSplitFeatureSet(splitFeatureDescriptors);
		
		double minWidthRatioForSplit = 1.1;
		double minHeightRatioForSplit = 1.0;
		int maxSplitDepth = 2;
		
		SplitCandidateFinder splitCandidateFinder = boundaryService.getSplitCandidateFinder();
		splitCandidateFinder.setMinDistanceBetweenSplits(5);
	
		ShapeSplitter shapeSplitter = boundaryService.getShapeSplitter(splitCandidateFinder, splitFeatures, splitDecisionMaker, minWidthRatioForSplit, beamWidth, maxSplitDepth);

		if (mergeModelPath.length()==0)
			throw new RuntimeException("Missing argument: mergeModel");
		if (!mergeModelPath.endsWith(".zip"))
			throw new RuntimeException("mergeModel must end with .zip");

		MaxentModel mergeModel = new GenericModelReader(new File(mergeModelPath)).getModel();
		MaxentDecisionMaker mergeDecisionMaker = new MaxentDecisionMaker(mergeModel);
		
		File mergeModelFile = new File(mergeModelPath);
		JolicielMaxentModel mergeMaxentModel = new JolicielMaxentModel(mergeModelFile);
		
		List<String> mergeFeatureDescriptors = mergeMaxentModel.getFeatureDescriptors();
		Set<MergeFeature<?>> mergeFeatures = boundaryFeatureService.getMergeFeatureSet(mergeFeatureDescriptors);
		double maxWidthRatioForMerge = 1.2;
		double maxDistanceRatioForMerge = 0.15;
		
		ShapeMerger shapeMerger = boundaryService.getShapeMerger(mergeFeatures, mergeDecisionMaker);
			
		JochreCorpusImageReader imageReader = graphicsService.getJochreCorpusImageReader();
		imageReader.setImageStatusesToInclude(new ImageStatus[] {testSet});
		imageReader.setImageId(imageId);

		BoundaryDetector boundaryDetector = boundaryService.getLetterByLetterBoundaryDetector(shapeSplitter, shapeMerger, 5);
		boundaryDetector.setMinWidthRatioForSplit(minWidthRatioForSplit);
		boundaryDetector.setMinHeightRatioForSplit(minHeightRatioForSplit);
		boundaryDetector.setMaxWidthRatioForMerge(maxWidthRatioForMerge);
		boundaryDetector.setMaxDistanceRatioForMerge(maxDistanceRatioForMerge);
		
		ImageAnalyser evaluator = analyserService.getBeamSearchImageAnalyzer(beamWidth, 0.01, wordChooser);
		evaluator.setBoundaryDetector(boundaryDetector);
		
		LetterValidator letterValidator = new ComponentCharacterValidator(locale);
	
		OriginalShapeLetterAssigner shapeLetterAssigner = new OriginalShapeLetterAssigner();
		shapeLetterAssigner.setEvaluate(true);
		shapeLetterAssigner.setSave(save);
		shapeLetterAssigner.setLetterValidator(letterValidator);
		shapeLetterAssigner.setSingleLetterMethod(false);
		evaluator.addObserver(shapeLetterAssigner);
		
		ErrorLogger errorLogger = new ErrorLogger();	
		if (wordChooser!=null) {
			errorLogger.setLexicon(wordChooser.getLexicon());
			errorLogger.setWordSplitter(wordChooser.getWordSplitter());
		}
       	Writer errorWriter = null;
    	if (outputDirPath!=null) {
    		File outputDir = new File(outputDirPath);
    		outputDir.mkdirs();
			File errorFile = new File(outputDir, "errors.txt");
			errorFile.delete();
			errorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile, true),"UTF8"));
    	}
    	errorLogger.setErrorWriter(errorWriter);
    	evaluator.addObserver(errorLogger);
		
//			evaluator.setOutcomesToAnalyse(new String[] {"מ"});
		
		try {
			evaluator.analyse(letterGuesser, imageReader);
		} finally {
			if (errorWriter!=null)
				errorWriter.close();
		}
		
		LOG.debug("F-score for " + letterModelPath + ": " + shapeLetterAssigner.getFScoreCalculator().getTotalFScore());
		if (outputDirPath!=null) {
			File outputDir = new File(outputDirPath);
			outputDir.mkdirs();
			String modelFileName = letterModelPath.substring(letterModelPath.lastIndexOf('/')+1);

			modelFileName += "_Full";
			File fscoreFile = new File(outputDir, modelFileName + ".fscores.csv");
			shapeLetterAssigner.getFScoreCalculator().writeScoresToCSVFile(fscoreFile);
		}
	}

	/**
	 * Apply a set of images to a given image or a given shape.
	 * @param imageId
	 * @param shapeId
	 */
	public void doCommandApplyFeatures(int imageId, int shapeId, String letterFeatureFilePath) {
		try {
			if (letterFeatureFilePath.length()==0)
				throw new RuntimeException("Missing argument: letterFeatures");
			LetterFeatureTester featureTester = letterFeatureService.getFeatureTester();
			
			File letterFeatureFile = new File(letterFeatureFilePath);
			Scanner scanner = new Scanner(letterFeatureFile);
	
			List<String> descriptors = new ArrayList<String>();
			while (scanner.hasNextLine()) {
				String descriptor = scanner.nextLine();
				descriptors.add(descriptor);
				LOG.debug(descriptor);
			}
			Set<LetterFeature<?>> features = letterFeatureService.getLetterFeatureSet(descriptors);
			Set<String> letters = new HashSet<String>();
	//			letters.add("ג");
	//			letters.add("נ");
			featureTester.applyFeatures(features, letters, imageId, shapeId);
		} catch (FileNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}		
	}

	/**
	 * Update the images in an existing Jochre document.
	 * @param filename the PDF file containing the images
	 * @param docId the id of the document ot update
	 * @param firstPage the first page to segment, if <=0 will start with first document page
	 * @param lastPage the last page to segment, if <=0 will segment until last document page
	 */
	public void doCommandUpdateImages(String filename,
			int docId,
			int firstPage,
			int lastPage) {
		if (filename.length()==0)
			throw new RuntimeException("Missing argument: file");
		if (docId<0)
			throw new RuntimeException("Missing argument: docId");
		
		JochreDocument doc = documentService.loadJochreDocument(docId);
		if (filename.toLowerCase().endsWith(".pdf")) {
			File pdfFile = new File(filename);
			PdfImageVisitor pdfImageVisitor = pdfService.getPdfImageVisitor(pdfFile, firstPage, lastPage,
					new PdfImageUpdater(doc));
			pdfImageVisitor.visitImages();
		} else {
			throw new RuntimeException("Unrecognised file extension");
		}
	}

	/**
	 * Extract the images from a PDF file.
	 * @param filename the path to the PDF file
	 * @param outputDirPath the directory where to store the images extracted.
	 * @param firstPage the first page to segment, if <=0 will start with first document page
	 * @param lastPage the last page to segment, if <=0 will segment until last document page
	 */
	public void doCommandExtractImages(String filename,
			String outputDirPath,
			int firstPage,
			int lastPage) {
		if (filename.length()==0)
			throw new RuntimeException("Missing argument: file");
		if (outputDirPath.length()==0)
			throw new RuntimeException("Missing argument: outputDir");

		if (filename.toLowerCase().endsWith(".pdf")) {
			File pdfFile = new File(filename);
			PdfImageSaver pdfImageSaver = pdfService.getPdfImageSaver();
			pdfImageSaver.saveImages(pdfFile, outputDirPath, firstPage, lastPage);
		} else {
			throw new RuntimeException("Unrecognised file extension");
		}
	}

	/**
	 * Segment a file, without analysing it.
	 * @param filename the path of the file to load
	 * @param userFriendlyName a name to store against this file in the database
	 * @param showSegmentation whether or not to output the graphical segmentation files
	 * @param outputDirPath an output directory for the graphical segmentation files
	 * @param save should we save this file to the database?
	 * @param firstPage the first page to segment, if <=0 will start with first document page
	 * @param lastPage the last page to segment, if <=0 will segment until last document page
	 */
	public void doCommandSegment(String filename,
			String userFriendlyName,
			boolean showSegmentation,
			String outputDirPath,
			boolean save,
			int firstPage,
			int lastPage) {
		
		if (filename.length()==0)
			throw new RuntimeException("Missing argument: file");
		if (isoLanguage.length()==0)
			throw new RuntimeException("Missing argument: lang");
		if (userId<0&&save)
			throw new RuntimeException("Missing argument (for save=true): userId");

		User user = null;
		if (userId>=0) {
			user = securityService.loadUser(userId);
		}
		

		
		File file = new File(filename);
		JochreDocumentGenerator sourceFileProcessor = this.documentService.getJochreDocumentGenerator(file.getName(), userFriendlyName, locale);
		if (save)
			sourceFileProcessor.requestSave(user);
		if (showSegmentation) {
			if (outputDirPath!=null && outputDirPath.length()>0) {
				File outputDir = new File(outputDirPath);
				outputDir.mkdirs();
				sourceFileProcessor.requestSegmentation(outputDir);
			}
		}
			
		if (filename.toLowerCase().endsWith(".pdf")) {
			PdfImageVisitor pdfImageVisitor = pdfService.getPdfImageVisitor(file, firstPage, lastPage, sourceFileProcessor);
			pdfImageVisitor.visitImages();
		} else if (filename.toLowerCase().endsWith(".png")
				|| filename.toLowerCase().endsWith(".jpg")
				|| filename.toLowerCase().endsWith(".jpeg")
				|| filename.toLowerCase().endsWith(".gif")) {
			ImageDocumentExtractor extractor = documentService.getImageDocumentExtractor(file, sourceFileProcessor);
			extractor.extractDocument();
		} else {
			throw new RuntimeException("Unrecognised file extension");
		}
	}
	
	static class PdfImageUpdater implements SourceFileProcessor {
		JochreDocument doc = null;
		
		public PdfImageUpdater(JochreDocument document) {
			this.doc = document;
		}
		
		@Override
		public JochreDocument onDocumentStart() {
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
			JochrePage thePage = null;
			for (JochrePage page : this.doc.getPages()) {
				if (page.getIndex()==pageIndex) {
					thePage = page;
					break;
				}
			}
			return thePage;
		}

		@Override
		public void onPageComplete(JochrePage jochrePage) {
			// nothing here.
		}

		@Override
		public JochreImage onImageFound(JochrePage jochrePage,
				BufferedImage image, String imageName, int imageIndex) {
			JochreImage theImage = jochrePage.getImages().get(0);
			theImage.setOriginalImage(image);
			theImage.save();
			return theImage;
		}
		
	}

	public String getIsoLanguage() {
		return isoLanguage;
	}

	public void setIsoLanguage(String isoLanguage) {
		this.isoLanguage = isoLanguage;
		this.locale = new Locale(isoLanguage);
	}


	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}
	
}
