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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.doc.DocumentService;
import com.joliciel.jochre.doc.ImageDocumentExtractor;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochreDocumentGenerator;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.doc.SourceFileProcessor;
import com.joliciel.jochre.graphics.CorpusSelectionCriteria;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.JochreCorpusGroupReader;
import com.joliciel.jochre.graphics.JochreCorpusImageProcessor;
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
import com.joliciel.jochre.lexicon.DefaultLexiconWrapper;
import com.joliciel.jochre.lexicon.DefaultWordSplitter;
import com.joliciel.jochre.lexicon.FakeLexicon;
import com.joliciel.jochre.lexicon.Lexicon;
import com.joliciel.jochre.lexicon.LexiconErrorWriter;
import com.joliciel.jochre.lexicon.LexiconMerger;
import com.joliciel.jochre.lexicon.LexiconService;
import com.joliciel.jochre.lexicon.LocaleSpecificLexiconService;
import com.joliciel.jochre.lexicon.MostLikelyWordChooser;
import com.joliciel.jochre.lexicon.TextFileLexicon;
import com.joliciel.jochre.lexicon.UnknownWordListWriter;
import com.joliciel.jochre.lexicon.WordSplitter;
import com.joliciel.jochre.output.ImageExtractor;
import com.joliciel.jochre.output.MetaDataExporter;
import com.joliciel.jochre.output.OutputService;
import com.joliciel.jochre.output.OutputService.ExportFormat;
import com.joliciel.jochre.output.TextFormat;
import com.joliciel.jochre.pdf.PdfImageSaver;
import com.joliciel.jochre.pdf.PdfImageVisitor;
import com.joliciel.jochre.pdf.PdfService;
import com.joliciel.jochre.security.User;
import com.joliciel.jochre.stats.FScoreCalculator;
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.jochre.utils.JochreLogUtils;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.MachineLearningModelFactory;
import com.joliciel.talismane.machineLearning.ModelTrainerFactory;
import com.joliciel.talismane.machineLearning.OutcomeEqualiserEventStream;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.PerformanceMonitor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Class encapsulating the various top-level Jochre commands and command-line
 * interface.
 * 
 * @author Assaf Urieli
 *
 */
public class Jochre implements LocaleSpecificLexiconService {
	private static final Logger LOG = LoggerFactory.getLogger(Jochre.class);

	public enum BoundaryDetectorType {
		LetterByLetter, Deterministic
	}

	public enum OutputFormat {
		Jochre, JochrePageByPage, Alto3, Alto3zip, AbbyyFineReader8, HTML, UnknownWords, Metadata, ImageExtractor, Text, GuessText
	}

	GraphicsService graphicsService;
	DocumentService documentService;
	AnalyserService analyserService;
	LexiconService lexiconService;
	LetterGuesserService letterGuesserService;
	BoundaryService boundaryService;
	PdfService pdfService;
	LetterFeatureService letterFeatureService;
	BoundaryFeatureService boundaryFeatureService;

	Locale locale = null;
	int userId = -1;
	String dataSourcePropertiesPath;

	String encoding = null;
	String lexiconPath = null;
	WordSplitter wordSplitter = null;
	Lexicon lexicon = null;
	Map<String, Set<Integer>> documentGroups = new LinkedHashMap<String, Set<Integer>>();
	String csvEncoding = null;

	private int beamWidth = 5;

	public Jochre() {
	}

	private void initialise() {
		JochreServiceLocator locator = JochreServiceLocator.getInstance();

		graphicsService = locator.getGraphicsServiceLocator().getGraphicsService();
		documentService = locator.getDocumentServiceLocator().getDocumentService();
		analyserService = locator.getAnalyserServiceLocator().getAnalyserService();
		lexiconService = locator.getLexiconServiceLocator().getLexiconService();
		letterGuesserService = locator.getLetterGuesserServiceLocator().getLetterGuesserService();
		boundaryService = locator.getBoundaryServiceLocator().getBoundaryService();
		pdfService = locator.getPdfServiceLocator().getPdfService();
		letterFeatureService = locator.getLetterFeatureServiceLocator().getLetterFeatureService();
		boundaryFeatureService = locator.getBoundaryFeatureServiceLocator().getBoundaryFeatureService();
	}

	public static void main(String[] args) throws Exception {
		Map<String, String> argMap = new HashMap<String, String>();

		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos + 1);
			argMap.put(argName, argValue);
		}

		Jochre jochre = new Jochre();
		jochre.execute(argMap);
	}

	/**
	 * Usage (* indicates optional):<br/>
	 * Jochre load [filename] [isoLanguageCode] [firstPage]* [lastPage]*<br/>
	 * Loads a file (pdf or image) and segments it into letters. The analysed
	 * version is stored in the persistent store. Writes [filename].xml to the
	 * same location, to enable the user to indicate the text to associate with
	 * this file.<br/>
	 * Jochre extract [filename] [outputDirectory] [firstPage]* [lastPage]*<br/>
	 * Extracts images form a pdf file.<br/>
	 */
	public void execute(Map<String, String> argMap) throws Exception {
		if (argMap.size() == 0) {
			System.out.println("Usage (* indicates optional):");
			System.out.println(
					"Jochre command=load file=[filename] name=[userFriendlyName] lang=[isoLanguageCode] first=[firstPage]* last=[lastPage]* outputDir=[outputDirectory]* showSeg=[true/false]");
			System.out.println("Jochre command=extract file=[filename] outputDir=[outputDirectory] first=[firstPage]* last=[lastPage]*");
			System.out.println("Jochre command=analyse");
			System.out.println("Jochre command=train file=[filename] outputDir=[outputDirectory] iterations=[iterations] cutoff=[cutoff]");
			return;
		}

		String logConfigPath = argMap.get("logConfigFile");
		argMap.remove("logConfigFile");
		JochreLogUtils.configureLogging(logConfigPath);

		File performanceConfigFile = null;

		String command = "";
		String inFilePath = "";
		String inDirPath = null;
		String userFriendlyName = "";
		String outputDirPath = null;
		String outputFilePath = null;
		int firstPage = -1;
		int lastPage = -1;
		int shapeId = -1;
		int docId = -1;
		int imageId = 0;
		int userId = -1;
		int imageCount = 0;
		int multiplier = 0;
		boolean showSegmentation = false;
		boolean drawPixelSpread = false;
		boolean save = false;
		String letterModelPath = "";
		String splitModelPath = "";
		String mergeModelPath = "";
		ImageStatus[] imageSet = new ImageStatus[] { ImageStatus.TRAINING_HELD_OUT };
		boolean reconstructLetters = false;
		double minProbForDecision = 0.5;
		double junkThreshold = 0.0;
		BoundaryDetectorType boundaryDetectorType = BoundaryDetectorType.LetterByLetter;
		int excludeImageId = 0;
		int crossValidationSize = -1;
		int includeIndex = -1;
		int excludeIndex = -1;
		Set<Integer> documentSet = null;
		boolean frequencyAdjusted = false;
		double smoothing = -1;
		double frequencyLogBase = 10.0;
		String suffix = "";
		String dataSourcePath = null;
		String docGroupPath = null;
		boolean includeBeam = false;
		List<OutputFormat> outputFormats = new ArrayList<Jochre.OutputFormat>();
		String csvSeparator = "\t";
		String csvLocale = null;
		String docSelectionPath = null;
		List<String> featureDescriptors = null;

		for (Entry<String, String> argMapEntry : argMap.entrySet()) {
			String argName = argMapEntry.getKey();
			String argValue = argMapEntry.getValue();
			if (argName.equals("command"))
				command = argValue;
			else if (argName.equals("file"))
				inFilePath = argValue;
			else if (argName.equals("name"))
				userFriendlyName = argValue;
			else if (argName.equals("lang"))
				locale = new Locale(argValue);
			else if (argName.equals("first"))
				firstPage = Integer.parseInt(argValue);
			else if (argName.equals("last"))
				lastPage = Integer.parseInt(argValue);
			else if (argName.equals("inDir"))
				inDirPath = argValue;
			else if (argName.equals("outDir"))
				outputDirPath = argValue;
			else if (argName.equals("outputFile"))
				outputFilePath = argValue;
			else if (argName.equals("showSeg"))
				showSegmentation = (argValue.equals("true"));
			else if (argName.equals("drawPixelSpread"))
				drawPixelSpread = (argValue.equals("true"));
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
			else if (argName.equals("imageStatus")) {
				String[] statusCodes = argValue.split(",");
				Set<ImageStatus> imageStasuses = new HashSet<>();
				for (String statusCode : statusCodes) {
					if (statusCode.equals("heldOut"))
						imageStasuses.add(ImageStatus.TRAINING_HELD_OUT);
					else if (statusCode.equals("test"))
						imageStasuses.add(ImageStatus.TRAINING_TEST);
					else if (statusCode.equals("training"))
						imageStasuses.add(ImageStatus.TRAINING_VALIDATED);
					else if (statusCode.equals("all")) {
						imageStasuses.add(ImageStatus.TRAINING_VALIDATED);
						imageStasuses.add(ImageStatus.TRAINING_HELD_OUT);
						imageStasuses.add(ImageStatus.TRAINING_TEST);
					} else
						throw new RuntimeException("Unknown imageSet: " + statusCode);
				}
				imageSet = new ImageStatus[imageStasuses.size()];
				int i = 0;
				for (ImageStatus imageStatus : imageStasuses) {
					imageSet[i++] = imageStatus;
				}
			} else if (argName.equals("reconstructLetters"))
				reconstructLetters = (argValue.equals("true"));
			else if (argName.equals("minProbForDecision"))
				minProbForDecision = Double.parseDouble(argValue);
			else if (argName.equals("junkThreshold"))
				junkThreshold = Double.parseDouble(argValue);
			else if (argName.equals("boundaryDetector"))
				boundaryDetectorType = BoundaryDetectorType.valueOf(argValue);
			else if (argName.equals("lexicon"))
				lexiconPath = argValue;
			else if (argName.equals("freqLogBase")) {
				frequencyLogBase = Double.parseDouble(argValue);
				frequencyAdjusted = true;
			} else if (argName.equals("smoothing"))
				smoothing = Double.parseDouble(argValue);
			else if (argName.equals("excludeImageId"))
				excludeImageId = Integer.parseInt(argValue);
			else if (argName.equals("crossValidationSize"))
				crossValidationSize = Integer.parseInt(argValue);
			else if (argName.equals("includeIndex"))
				includeIndex = Integer.parseInt(argValue);
			else if (argName.equals("excludeIndex"))
				excludeIndex = Integer.parseInt(argValue);
			else if (argName.equals("docSet")) {
				String[] docIdArray = argValue.split(",");
				documentSet = new HashSet<Integer>();
				for (String docIdString : docIdArray) {
					int oneId = Integer.parseInt(docIdString);
					documentSet.add(oneId);
				}
			} else if (argName.equals("docSelection")) {
				docSelectionPath = argValue;
			} else if (argName.equals("docGroupFile"))
				docGroupPath = argValue;
			else if (argName.equals("frequencyAdjusted"))
				frequencyAdjusted = argValue.equalsIgnoreCase("true");
			else if (argName.equals("suffix"))
				suffix = argValue;
			else if (argName.equals("dataSource"))
				dataSourcePath = argValue;
			else if (argName.equals("encoding"))
				encoding = argValue;
			else if (argName.equals("performanceConfigFile"))
				performanceConfigFile = new File(argValue);
			else if (argName.equals("includeBeam"))
				includeBeam = argValue.equalsIgnoreCase("true");
			else if (argName.equals("outputFormat")) {
				outputFormats = new ArrayList<Jochre.OutputFormat>();
				String[] outputFormatStrings = argValue.split(",");
				for (String outputFormatString : outputFormatStrings) {
					outputFormats.add(OutputFormat.valueOf(outputFormatString));
				}
				if (outputFormats.size() == 0)
					throw new JochreException("At least one outputFormat required.");
			} else if (argName.equals("csvSeparator"))
				csvSeparator = argValue;
			else if (argName.equals("csvEncoding"))
				csvEncoding = argValue;
			else if (argName.equals("csvLocale"))
				csvLocale = argValue;
			else if (argName.equals("features")) {
				featureDescriptors = new ArrayList<>();
				InputStream featureFile = new FileInputStream(new File(argValue));
				try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(featureFile, "UTF-8")))) {
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						featureDescriptors.add(descriptor);
						LOG.debug(descriptor);
					}
				}
			} else
				throw new RuntimeException("Unknown argument: " + argName);
		}

		long startTime = System.currentTimeMillis();
		PerformanceMonitor.start(performanceConfigFile);
		try {
			if (locale == null) {
				throw new JochreException("Argument lang is required");
			}
			JochreSession jochreSession = JochreSession.getInstance();
			jochreSession.setLocale(locale);

			if (encoding == null)
				encoding = Charset.defaultCharset().name();
			if (csvEncoding == null)
				csvEncoding = encoding;

			CSVFormatter.setGlobalCsvSeparator(csvSeparator);
			if (csvLocale != null)
				CSVFormatter.setGlobalLocale(Locale.forLanguageTag(csvLocale));

			JochreServiceLocator locator = JochreServiceLocator.getInstance();
			if (dataSourcePath != null)
				locator.setDataSourcePropertiesFile(dataSourcePath);

			this.initialise();

			this.setUserId(userId);

			CorpusSelectionCriteria criteria = this.getGraphicsService().getCorpusSelectionCriteria();
			if (docSelectionPath != null) {
				File docSelectionFile = new File(docSelectionPath);
				Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(docSelectionFile), encoding)));
				criteria.loadSelection(scanner);
				scanner.close();
			} else {
				criteria.setImageId(imageId);
				criteria.setImageCount(imageCount);
				criteria.setImageStatusesToInclude(imageSet);
				criteria.setExcludeImageId(excludeImageId);
				criteria.setCrossValidationSize(crossValidationSize);
				criteria.setIncludeIndex(includeIndex);
				criteria.setExcludeIndex(excludeIndex);
				criteria.setDocumentId(docId);
				criteria.setDocumentIds(documentSet);
			}

			if (docGroupPath != null) {
				File docGroupFile = new File(docGroupPath);
				Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(docGroupFile), encoding)));

				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					int equalsPos = line.indexOf('=');
					String groupName = line.substring(0, equalsPos);
					String[] ids = line.substring(equalsPos + 1).split(",");
					Set<Integer> idSet = new HashSet<Integer>();
					for (String idString : ids) {
						idSet.add(Integer.parseInt(idString));
					}
					documentGroups.put(groupName, idSet);
				}
				scanner.close();
			}

			OutputService outputService = locator.getTextServiceLocator().getTextService();
			MostLikelyWordChooser wordChooser = null;

			LexiconService lexiconService = locator.getLexiconServiceLocator().getLexiconService();

			wordChooser = lexiconService.getMostLikelyWordChooser(this.getLexicon(), this.getWordSplitter());
			if (smoothing > 0)
				wordChooser.setAdditiveSmoothing(smoothing);
			wordChooser.setFrequencyLogBase(frequencyLogBase);
			wordChooser.setFrequencyAdjusted(frequencyAdjusted);

			jochreSession.setJunkConfidenceThreshold(junkThreshold);

			File outputDir = null;
			File outputFile = null;
			if (outputDirPath != null) {
				outputDir = new File(outputDirPath);
			} else if (outputFilePath != null) {
				outputFile = new File(outputFilePath);
				outputDir = outputFile.getParentFile();
			}
			if (outputDir != null)
				outputDir.mkdirs();

			List<DocumentObserver> observers = null;
			if (outputFormats.size() > 0 && !command.equals("analyseFolder")) {
				if (outputDir == null) {
					throw new JochreException("Either outputDir our outputFile are required with outputFormats");
				}
				String baseName = null;
				if (userFriendlyName != null && userFriendlyName.length() > 0) {
					baseName = userFriendlyName;
				} else if (inFilePath != null && inFilePath.length() > 0) {
					File inFile = new File(inFilePath);
					baseName = this.getBaseName(inFile);
				}

				observers = this.getObservers(outputFormats, baseName, outputDir, outputService);
			}

			if (userFriendlyName.length() == 0)
				userFriendlyName = inFilePath;

			if (command.equals("segment")) {
				this.doCommandSegment(inFilePath, userFriendlyName, showSegmentation, drawPixelSpread, outputDirPath, save, firstPage, lastPage);
			} else if (command.equals("extract")) {
				this.doCommandExtractImages(inFilePath, outputDirPath, firstPage, lastPage);
			} else if (command.equals("updateImages")) {
				this.doCommandUpdateImages(inFilePath, docId, firstPage, lastPage);
			} else if (command.equals("applyFeatures")) {
				this.doCommandApplyFeatures(imageId, shapeId, featureDescriptors);
			} else if (command.equals("train")) {
				this.doCommandTrain(letterModelPath, featureDescriptors, criteria, reconstructLetters);
			} else if (command.equals("evaluate") || command.equals("evaluateComplex")) {
				this.doCommandEvaluate(letterModelPath, criteria, outputDirPath, wordChooser, reconstructLetters, save, suffix, includeBeam, observers);
			} else if (command.equals("evaluateFull")) {
				this.doCommandEvaluateFull(letterModelPath, splitModelPath, mergeModelPath, criteria, save, outputDirPath, wordChooser, boundaryDetectorType,
						minProbForDecision, suffix, observers);
			} else if (command.equals("analyse")) {
				this.doCommandAnalyse(letterModelPath, criteria, wordChooser, observers);
			} else if (command.equals("trainSplits")) {
				this.doCommandTrainSplits(splitModelPath, featureDescriptors, criteria);
			} else if (command.equals("evaluateSplits")) {
				this.doCommandEvaluateSplits(splitModelPath, criteria, minProbForDecision);
			} else if (command.equals("trainMerge")) {
				this.doCommandTrainMerge(mergeModelPath, featureDescriptors, multiplier, criteria);
			} else if (command.equals("evaluateMerge")) {
				this.doCommandEvaluateMerge(mergeModelPath, criteria, minProbForDecision);
			} else if (command.equals("logImage")) {
				this.doCommandLogImage(shapeId);
			} else if (command.equals("testFeature")) {
				this.doCommandTestFeature(shapeId);
			} else if (command.equals("serializeLexicon")) {
				if (outputDir == null) {
					throw new JochreException("Either outputDir our outputFile are required for " + command);
				}

				File inputFile = new File(inFilePath);
				if (inputFile.isDirectory()) {
					File[] lexiconFiles = inputFile.listFiles();
					for (File oneLexFile : lexiconFiles) {
						LOG.debug(oneLexFile.getName() + ": " + ", size: " + oneLexFile.length());

						TextFileLexicon lexicon = new TextFileLexicon(oneLexFile, encoding);

						String baseName = oneLexFile.getName().substring(0, oneLexFile.getName().indexOf("."));
						if (baseName.lastIndexOf("/") > 0)
							baseName = baseName.substring(baseName.lastIndexOf("/") + 1);

						File lexiconFile = new File(outputDir, baseName + ".obj");
						lexicon.serialize(lexiconFile);
					}
				} else {
					LOG.debug(inFilePath + ": " + inputFile.exists() + ", size: " + inputFile.length());

					TextFileLexicon lexicon = new TextFileLexicon(inputFile, encoding);

					String baseName = inFilePath.substring(0, inFilePath.indexOf("."));
					if (baseName.lastIndexOf("/") > 0)
						baseName = baseName.substring(baseName.lastIndexOf("/") + 1);

					File lexiconFile = outputFile;
					if (lexiconFile == null)
						lexiconFile = new File(outputDir, baseName + ".obj");
					lexicon.serialize(lexiconFile);
				}
			} else if (command.equals("analyseFolder")) {
				File inDir = new File(inDirPath);
				File[] pdfFiles = inDir.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {
						return (name.toLowerCase().endsWith(".pdf"));
					}
				});

				Arrays.sort(pdfFiles);

				for (File pdfFile : pdfFiles) {
					LOG.info("Analysing file: " + pdfFile.getAbsolutePath());
					try {
						String baseName = this.getBaseName(pdfFile);
						File analysisDir = new File(inDir, baseName);
						analysisDir.mkdirs();
						List<DocumentObserver> pdfObservers = this.getObservers(outputFormats, baseName, analysisDir, outputService);
						File letterModelFile = new File(letterModelPath);
						File splitModelFile = null;
						File mergeModelFile = null;
						if (splitModelPath.length() > 0)
							splitModelFile = new File(splitModelPath);
						if (mergeModelPath.length() > 0)
							mergeModelFile = new File(mergeModelPath);
						this.doCommandAnalyse(pdfFile, letterModelFile, splitModelFile, mergeModelFile, wordChooser, firstPage, lastPage, pdfObservers);

						File pdfOutputDir = new File(outputDir, baseName);
						pdfOutputDir.mkdirs();

						File targetFile = new File(pdfOutputDir, pdfFile.getName());
						Files.move(pdfFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						File[] analysisFiles = analysisDir.listFiles();
						for (File analysisFile : analysisFiles) {
							targetFile = new File(pdfOutputDir, analysisFile.getName());
							Files.move(analysisFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
						Files.delete(analysisDir.toPath());
					} catch (Exception e) {
						// log errors, but continue processing
						LOG.error("Error processing file: " + pdfFile.getAbsolutePath(), e);
					}
				}
			} else if (command.equals("analyseFile")) {
				File pdfFile = new File(inFilePath);
				File letterModelFile = new File(letterModelPath);
				File splitModelFile = null;
				File mergeModelFile = null;
				if (splitModelPath.length() > 0)
					splitModelFile = new File(splitModelPath);
				if (mergeModelPath.length() > 0)
					mergeModelFile = new File(mergeModelPath);
				this.doCommandAnalyse(pdfFile, letterModelFile, splitModelFile, mergeModelFile, wordChooser, firstPage, lastPage, observers);
			} else {
				throw new RuntimeException("Unknown command: " + command);
			}
		} catch (Exception e) {
			LOG.error("An error occurred while running Jochre", e);
			throw e;
		} finally {
			PerformanceMonitor.end();
			long duration = System.currentTimeMillis() - startTime;
			LOG.info("Duration (ms):" + duration);
		}
		LOG.info("#### finished #####");
	}

	private String getBaseName(File file) {
		String baseName = file.getName();
		if (baseName.lastIndexOf('.') > 0)
			baseName = baseName.substring(0, baseName.lastIndexOf('.'));
		return baseName;
	}

	/**
	 * Test a feature on a particular shape.
	 */
	public void doCommandTestFeature(int shapeId) {
		// just a utility for testing a feature on a particular shape
		ShapeFeature<?> feature = new VerticalElongationFeature();
		if (shapeId > 0) {
			Shape shape = graphicsService.loadShape(shapeId);
			shape.writeImageToLog();
			RuntimeEnvironment env = new RuntimeEnvironment();
			feature.check(shape, env);
		} else {

			// String result = "false";
			// TrainingServiceLocator trainingServiceLocator =
			// locator.getTrainingServiceLocator();
			// TrainingService trainingService =
			// trainingServiceLocator.getTrainingService();
			// List<Integer> shapeIds =
			// trainingService.findShapesForFeature("ג", feature, result);
			List<Integer> shapeIds = graphicsService.findShapeIds("—");
			Map<Object, Integer> outcomeMap = new HashMap<Object, Integer>();
			for (int oneShapeId : shapeIds) {
				Shape shape = graphicsService.loadShape(oneShapeId);
				shape.writeImageToLog();
				RuntimeEnvironment env = new RuntimeEnvironment();
				FeatureResult<?> weightedOutcome = feature.check(shape, env);

				Object outcome = weightedOutcome.getOutcome();
				Integer count = outcomeMap.get(outcome);
				if (count == null)
					outcomeMap.put(outcome, 1);
				else
					outcomeMap.put(outcome, count.intValue() + 1);

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
	public void doCommandBuildLexicon(String outputDirPath, WordSplitter wordSplitter, CorpusSelectionCriteria criteria) {
		try {
			CorpusLexiconBuilder builder = lexiconService.getCorpusLexiconBuilder(wordSplitter);
			builder.setCriteria(criteria);
			TextFileLexicon lexicon = builder.buildLexicon();

			File outputDir = new File(outputDirPath);
			outputDir.mkdirs();
			File textFile = new File(outputDir, "jochreCorpusLexicon.txt");
			textFile.delete();
			Writer textFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(textFile, true), "UTF8"));
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
	 * Log a shape's image to the log file, to make sure it got segmented and
	 * stored correctly.
	 */
	public void doCommandLogImage(int shapeId) {
		// just a utility for making sure images got segmented and stored
		// correctly
		if (shapeId > 0) {
			Shape shape = graphicsService.loadShape(shapeId);
			shape.writeImageToLog();
			LOG.debug("Letter: " + shape.getLetter());
		}
	}

	/**
	 * Train the letter merging model.
	 * 
	 * @param mergeModelPath
	 *          the path where the model should be saved
	 * @param featureDescriptors
	 *          feature descriptors for training
	 * @param multiplier
	 *          if &gt; 0, will be used to equalize the outcomes
	 * @param criteria
	 *          the criteria used to select the training corpus
	 */
	public void doCommandTrainMerge(String mergeModelPath, List<String> featureDescriptors, int multiplier, CorpusSelectionCriteria criteria) {
		if (mergeModelPath.length() == 0)
			throw new RuntimeException("Missing argument: mergeModel");
		if (!mergeModelPath.endsWith(".zip"))
			throw new RuntimeException("mergeModel must end with .zip");

		if (featureDescriptors == null)
			throw new JochreException("features is required");

		File mergeModelFile = new File(mergeModelPath);
		mergeModelFile.getParentFile().mkdirs();

		Set<MergeFeature<?>> mergeFeatures = this.boundaryFeatureService.getMergeFeatureSet(featureDescriptors);
		double maxWidthRatio = 1.2;
		double maxDistanceRatio = 0.15;
		ClassificationEventStream corpusEventStream = boundaryService.getJochreMergeEventStream(criteria, mergeFeatures, maxWidthRatio, maxDistanceRatio);
		if (multiplier > 0) {
			corpusEventStream = new OutcomeEqualiserEventStream(corpusEventStream, multiplier);
		}

		Config config = ConfigFactory.load();
		ModelTrainerFactory modelTrainerFactory = new ModelTrainerFactory();
		ClassificationModelTrainer trainer = modelTrainerFactory.constructTrainer(config);

		ClassificationModel mergeModel = trainer.trainModel(corpusEventStream, featureDescriptors);
		mergeModel.persist(mergeModelFile);
	}

	/**
	 * Evaluate the letter merging model on its own.
	 * 
	 * @param mergeModelPath
	 *          the path of the model to be evaluated.
	 * @param criteria
	 *          for selecting the portion of the corpus to evaluate
	 * @param minProbForDecision
	 *          at which probability should a merge be made. If &lt; 0, 0.5 is
	 *          assumed.
	 */
	public void doCommandEvaluateMerge(String mergeModelPath, CorpusSelectionCriteria criteria, double minProbForDecision) throws IOException {
		if (mergeModelPath.length() == 0)
			throw new RuntimeException("Missing argument: mergeModel");
		if (!mergeModelPath.endsWith(".zip"))
			throw new RuntimeException("mergeModel must end with .zip");

		MachineLearningModelFactory modelFactory = new MachineLearningModelFactory();
		ClassificationModel mergeModel = null;
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(mergeModelPath))) {
			mergeModel = modelFactory.getClassificationModel(zis);
		}

		List<String> mergeFeatureDescriptors = mergeModel.getFeatureDescriptors();
		Set<MergeFeature<?>> mergeFeatures = boundaryFeatureService.getMergeFeatureSet(mergeFeatureDescriptors);

		JochreCorpusGroupReader groupReader = graphicsService.getJochreCorpusGroupReader();
		groupReader.setSelectionCriteria(criteria);

		double maxWidthRatio = 1.2;
		double maxDistanceRatio = 0.15;

		ShapeMerger merger = boundaryService.getShapeMerger(mergeFeatures, mergeModel.getDecisionMaker());
		MergeEvaluator evaluator = boundaryService.getMergeEvaluator(maxWidthRatio, maxDistanceRatio);
		if (minProbForDecision >= 0)
			evaluator.setMinProbabilityForDecision(minProbForDecision);
		FScoreCalculator<String> fScoreCalculator = evaluator.evaluate(groupReader, merger);
		LOG.debug("" + fScoreCalculator.getTotalFScore());
	}

	/**
	 * Train the letter splitting model.
	 * 
	 * @param splitModelPath
	 *          the path where the model should be saved
	 * @param featureDescriptors
	 *          the feature descriptors for training this model
	 * @param criteria
	 *          the criteria used to select the training corpus
	 */
	public void doCommandTrainSplits(String splitModelPath, List<String> featureDescriptors, CorpusSelectionCriteria criteria) {
		if (splitModelPath.length() == 0)
			throw new RuntimeException("Missing argument: splitModel");
		if (!splitModelPath.endsWith(".zip"))
			throw new RuntimeException("splitModel must end with .zip");
		if (featureDescriptors == null)
			throw new JochreException("features is required");

		File splitModelFile = new File(splitModelPath);
		splitModelFile.getParentFile().mkdirs();

		Set<SplitFeature<?>> splitFeatures = this.boundaryFeatureService.getSplitFeatureSet(featureDescriptors);

		double minWidthRatio = 1.1;
		double minHeightRatio = 1.0;
		ClassificationEventStream corpusEventStream = boundaryService.getJochreSplitEventStream(criteria, splitFeatures, minWidthRatio, minHeightRatio);

		Config config = ConfigFactory.load();
		ModelTrainerFactory modelTrainerFactory = new ModelTrainerFactory();
		ClassificationModelTrainer trainer = modelTrainerFactory.constructTrainer(config);

		ClassificationModel splitModel = trainer.trainModel(corpusEventStream, featureDescriptors);
		splitModel.persist(splitModelFile);
	}

	/**
	 * Evaluate the letter splitting model on its own.
	 * 
	 * @param splitModelPath
	 *          the path of the model to be evaluated.
	 * @param criteria
	 *          the criteria used to select the evaluation corpus
	 * @param minProbForDecision
	 *          at which probability should a split be made. If &lt; 0, 0.5 is
	 *          assumed.
	 */
	public void doCommandEvaluateSplits(String splitModelPath, CorpusSelectionCriteria criteria, double minProbForDecision) throws IOException {
		if (splitModelPath.length() == 0)
			throw new RuntimeException("Missing argument: splitModel");
		if (!splitModelPath.endsWith(".zip"))
			throw new RuntimeException("splitModel must end with .zip");

		MachineLearningModelFactory modelFactory = new MachineLearningModelFactory();
		ClassificationModel splitModel = null;
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(splitModelPath))) {
			splitModel = modelFactory.getClassificationModel(zis);
		}

		List<String> splitFeatureDescriptors = splitModel.getFeatureDescriptors();
		Set<SplitFeature<?>> splitFeatures = boundaryFeatureService.getSplitFeatureSet(splitFeatureDescriptors);

		double minWidthRatio = 1.1;
		double minHeightRatio = 1.0;
		int maxDepth = 2;

		SplitCandidateFinder splitCandidateFinder = boundaryService.getSplitCandidateFinder();
		splitCandidateFinder.setMinDistanceBetweenSplits(5);

		ShapeSplitter shapeSplitter = boundaryService.getShapeSplitter(splitCandidateFinder, splitFeatures, splitModel.getDecisionMaker(), minWidthRatio, beamWidth,
				maxDepth);

		JochreCorpusShapeReader shapeReader = graphicsService.getJochreCorpusShapeReader();
		shapeReader.setSelectionCriteria(criteria);

		SplitEvaluator splitEvaluator = boundaryService.getSplitEvaluator(5, minWidthRatio, minHeightRatio);
		if (minProbForDecision >= 0)
			splitEvaluator.setMinProbabilityForDecision(minProbForDecision);

		FScoreCalculator<String> fScoreCalculator = splitEvaluator.evaluate(shapeReader, shapeSplitter);
		LOG.debug("" + fScoreCalculator.getTotalFScore());
	}

	/**
	 * Train a letter guessing model.
	 * 
	 * @param letterModelPath
	 *          the path where the model should be saved
	 * @param featureDescriptors
	 *          the feature descriptors for training
	 * @param criteria
	 *          criteria for selecting images to include when training
	 * @param reconstructLetters
	 *          whether or not complete letters should be reconstructed for
	 *          training, from merged/split letters
	 */
	public void doCommandTrain(String letterModelPath, List<String> featureDescriptors, CorpusSelectionCriteria criteria, boolean reconstructLetters) {
		if (letterModelPath.length() == 0)
			throw new RuntimeException("Missing argument: letterModel");
		if (!letterModelPath.endsWith(".zip"))
			throw new RuntimeException("letterModel must end with .zip");
		if (featureDescriptors == null)
			throw new JochreException("features is required");

		Set<LetterFeature<?>> features = letterFeatureService.getLetterFeatureSet(featureDescriptors);

		BoundaryDetector boundaryDetector = null;
		if (reconstructLetters) {
			ShapeSplitter splitter = boundaryService.getTrainingCorpusShapeSplitter();
			ShapeMerger merger = boundaryService.getTrainingCorpusShapeMerger();
			boundaryDetector = boundaryService.getLetterByLetterBoundaryDetector(splitter, merger, 1);
		} else {
			boundaryDetector = boundaryService.getOriginalBoundaryDetector();
		}

		LetterValidator letterValidator = new ComponentCharacterValidator(locale);

		ClassificationEventStream corpusEventStream = letterGuesserService.getJochreLetterEventStream(criteria, features, boundaryDetector, letterValidator);

		File letterModelFile = new File(letterModelPath);
		letterModelFile.getParentFile().mkdirs();
		Config config = ConfigFactory.load();
		ModelTrainerFactory modelTrainerFactory = new ModelTrainerFactory();
		ClassificationModelTrainer trainer = modelTrainerFactory.constructTrainer(config);

		ClassificationModel letterModel = trainer.trainModel(corpusEventStream, featureDescriptors);
		letterModel.persist(letterModelFile);
	}

	/**
	 * Evaluate a given letter guessing model.
	 * 
	 * @param letterModelPath
	 *          the path to the model
	 * @param criteria
	 *          the criteria used to select the evaluation corpus
	 * @param outputDirPath
	 *          the directory to which we write the evaluation files
	 */
	public void doCommandEvaluate(String letterModelPath, CorpusSelectionCriteria criteria, String outputDirPath, MostLikelyWordChooser wordChooser,
			boolean reconstructLetters, boolean save, String suffix, boolean includeBeam, List<DocumentObserver> observers) throws IOException {
		if (letterModelPath.length() == 0)
			throw new RuntimeException("Missing argument: letterModel");
		if (!letterModelPath.endsWith(".zip"))
			throw new RuntimeException("letterModel must end with .zip");
		if (outputDirPath == null || outputDirPath.length() == 0)
			throw new RuntimeException("Missing argument: outputDir");

		File outputDir = new File(outputDirPath);
		outputDir.mkdirs();

		MachineLearningModelFactory modelFactory = new MachineLearningModelFactory();
		ClassificationModel letterModel = null;
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(letterModelPath))) {
			letterModel = modelFactory.getClassificationModel(zis);
		}

		List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
		Set<LetterFeature<?>> letterFeatures = letterFeatureService.getLetterFeatureSet(letterFeatureDescriptors);

		LetterGuesser letterGuesser = letterGuesserService.getLetterGuesser(letterFeatures, letterModel.getDecisionMaker());

		String baseName = letterModelPath.substring(0, letterModelPath.indexOf("."));
		if (baseName.lastIndexOf("/") > 0)
			baseName = baseName.substring(baseName.lastIndexOf("/") + 1);
		baseName += suffix;

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

		ImageAnalyser evaluator = analyserService.getBeamSearchImageAnalyser(beamWidth, 0.01);
		evaluator.setBoundaryDetector(boundaryDetector);
		evaluator.setLetterGuesser(letterGuesser);
		evaluator.setMostLikelyWordChooser(wordChooser);

		FScoreObserver fScoreObserver = null;
		LetterValidator letterValidator = new ComponentCharacterValidator(locale);
		if (reconstructLetters) {
			OriginalShapeLetterAssigner originalShapeLetterAssigner = new OriginalShapeLetterAssigner();
			originalShapeLetterAssigner.setEvaluate(true);
			originalShapeLetterAssigner.setSave(save);
			originalShapeLetterAssigner.setLetterValidator(letterValidator);

			fScoreObserver = originalShapeLetterAssigner;
		} else {
			LetterAssigner letterAssigner = new LetterAssigner();
			letterAssigner.setSave(save);
			evaluator.addObserver(letterAssigner);

			fScoreObserver = new SimpleLetterFScoreObserver(letterValidator);
		}

		evaluator.addObserver(fScoreObserver);

		ErrorLogger errorLogger = new ErrorLogger();
		if (wordChooser != null) {
			errorLogger.setLexicon(wordChooser.getLexicon());
			errorLogger.setWordSplitter(wordChooser.getWordSplitter());
		}
		Writer errorWriter = null;

		File errorFile = new File(outputDir, baseName + "_errors.txt");
		errorFile.delete();
		errorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile, true), "UTF8"));

		errorLogger.setErrorWriter(errorWriter);
		evaluator.addObserver(errorLogger);

		LexiconErrorWriter lexiconErrorWriter = new LexiconErrorWriter(outputDir, baseName, wordChooser, csvEncoding);
		if (documentGroups != null)
			lexiconErrorWriter.setDocumentGroups(documentGroups);
		lexiconErrorWriter.setIncludeBeam(includeBeam);

		// find all document names (alphabetical ordering)
		Set<String> documentNameSet = new TreeSet<String>();
		JochreCorpusImageReader imageReader1 = graphicsService.getJochreCorpusImageReader();
		CorpusSelectionCriteria docCriteria = graphicsService.getCorpusSelectionCriteria();
		docCriteria.setImageStatusesToInclude(criteria.getImageStatusesToInclude());
		docCriteria.setImageId(criteria.getImageId());
		docCriteria.setDocumentId(criteria.getDocumentId());
		docCriteria.setDocumentIds(criteria.getDocumentIds());
		imageReader1.setSelectionCriteria(docCriteria);
		JochreDocument currentDoc = null;
		while (imageReader1.hasNext()) {
			JochreImage image = imageReader1.next();
			if (!image.getPage().getDocument().equals(currentDoc)) {
				currentDoc = image.getPage().getDocument();
				documentNameSet.add(currentDoc.getName());
			}
		}
		List<String> documentNames = new ArrayList<String>(documentNameSet);
		lexiconErrorWriter.setDocumentNames(documentNames);

		evaluator.addObserver(lexiconErrorWriter);

		JochreCorpusImageProcessor imageProcessor = graphicsService.getJochreCorpusImageProcessor(criteria);
		imageProcessor.addObserver(evaluator);
		for (DocumentObserver observer : observers)
			imageProcessor.addObserver(observer);

		try {
			imageProcessor.process();
		} finally {
			if (errorWriter != null)
				errorWriter.close();
		}
		LOG.debug("F-score for " + letterModelPath + ": " + fScoreObserver.getFScoreCalculator().getTotalFScore());

		String modelFileName = baseName;
		if (reconstructLetters)
			modelFileName += "_Reconstruct";

		File fscoreFile = new File(outputDir, modelFileName + "_fscores.csv");
		Writer fscoreWriter = errorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fscoreFile, true), csvEncoding));
		fScoreObserver.getFScoreCalculator().writeScoresToCSV(fscoreWriter);

	}

	/**
	 * Analyse a set of images based on a given letter-guessing model.
	 * 
	 * @param letterModelPath
	 *          the path to the letter-guessing model.
	 * @param criteria
	 *          the criteria used to select the documents to be analysed
	 * @param wordChooser
	 *          the word chooser to use
	 * @param observers
	 *          the observers, used to create analysis output
	 */
	public void doCommandAnalyse(String letterModelPath, CorpusSelectionCriteria criteria, MostLikelyWordChooser wordChooser, List<DocumentObserver> observers)
			throws IOException {
		if (letterModelPath.length() == 0)
			throw new RuntimeException("Missing argument: letterModel");
		if (!letterModelPath.endsWith(".zip"))
			throw new RuntimeException("letterModel must end with .zip");

		MachineLearningModelFactory modelFactory = new MachineLearningModelFactory();
		ClassificationModel letterModel = null;
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(letterModelPath))) {
			letterModel = modelFactory.getClassificationModel(zis);
		}

		List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
		Set<LetterFeature<?>> letterFeatures = letterFeatureService.getLetterFeatureSet(letterFeatureDescriptors);

		LetterGuesser letterGuesser = letterGuesserService.getLetterGuesser(letterFeatures, letterModel.getDecisionMaker());

		ImageAnalyser analyser = analyserService.getBeamSearchImageAnalyser(beamWidth, 0.01);
		analyser.setLetterGuesser(letterGuesser);
		analyser.setMostLikelyWordChooser(wordChooser);
		LetterAssigner letterAssigner = new LetterAssigner();
		analyser.addObserver(letterAssigner);

		JochreCorpusImageProcessor imageProcessor = graphicsService.getJochreCorpusImageProcessor(criteria);
		imageProcessor.addObserver(analyser);
		for (DocumentObserver observer : observers)
			imageProcessor.addObserver(observer);

		imageProcessor.process();
	}

	/**
	 * Full analysis, including merge, split and letter guessing.
	 */
	public void doCommandAnalyse(File sourceFile, File letterModelFile, File splitModelFile, File mergeModelFile, MostLikelyWordChooser wordChooser,
			int firstPage, int lastPage, List<DocumentObserver> observers) throws IOException {

		MachineLearningModelFactory modelFactory = new MachineLearningModelFactory();
		ClassificationModel letterModel = null;
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(letterModelFile))) {
			letterModel = modelFactory.getClassificationModel(zis);
		}

		List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
		Set<LetterFeature<?>> letterFeatures = letterFeatureService.getLetterFeatureSet(letterFeatureDescriptors);
		LetterGuesser letterGuesser = letterGuesserService.getLetterGuesser(letterFeatures, letterModel.getDecisionMaker());
		ImageAnalyser analyser = analyserService.getBeamSearchImageAnalyser(beamWidth, 0.01);
		analyser.setLetterGuesser(letterGuesser);
		analyser.setMostLikelyWordChooser(wordChooser);
		BoundaryDetector boundaryDetector = null;

		if (splitModelFile != null && mergeModelFile != null) {
			boundaryDetector = boundaryService.getBoundaryDetector(splitModelFile, mergeModelFile);
			analyser.setBoundaryDetector(boundaryDetector);

			OriginalShapeLetterAssigner shapeLetterAssigner = new OriginalShapeLetterAssigner();
			shapeLetterAssigner.setEvaluate(false);
			shapeLetterAssigner.setSingleLetterMethod(false);

			analyser.addObserver(shapeLetterAssigner);
		} else {
			boundaryDetector = boundaryService.getOriginalBoundaryDetector();
			analyser.setBoundaryDetector(boundaryDetector);

			LetterAssigner letterAssigner = new LetterAssigner();
			analyser.addObserver(letterAssigner);
		}

		JochreDocumentGenerator documentGenerator = documentService.getJochreDocumentGenerator(sourceFile.getName(), "", locale);
		documentGenerator.addDocumentObserver(analyser);

		for (DocumentObserver observer : observers)
			documentGenerator.addDocumentObserver(observer);

		if (!sourceFile.exists())
			throw new JochreException("The file " + sourceFile.getPath() + " does not exist");

		if (sourceFile.getName().toLowerCase().endsWith(".pdf")) {
			PdfImageVisitor pdfImageVisitor = pdfService.getPdfImageVisitor(sourceFile, firstPage, lastPage, documentGenerator);

			pdfImageVisitor.visitImages();
		} else if (sourceFile.getName().toLowerCase().endsWith(".png") || sourceFile.getName().toLowerCase().endsWith(".jpg")
				|| sourceFile.getName().toLowerCase().endsWith(".jpeg") || sourceFile.getName().toLowerCase().endsWith(".gif")) {
			ImageDocumentExtractor extractor = documentService.getImageDocumentExtractor(sourceFile, documentGenerator);
			extractor.extractDocument();
		} else if (sourceFile.isDirectory()) {
			ImageDocumentExtractor extractor = documentService.getImageDocumentExtractor(sourceFile, documentGenerator);
			extractor.extractDocument();
		} else {
			throw new RuntimeException("Unrecognised file extension");
		}
	}

	/**
	 * Evaluate a suite of split/merge models and letter guessing model.
	 * 
	 * @param letterModelPath
	 *          the path to the letter-guessing model
	 * @param splitModelPath
	 *          the path to the splitting model
	 * @param mergeModelPath
	 *          the path to the merging model
	 * @param criteria
	 *          for selecting the evaluation corpus
	 * @param save
	 *          whether or not the letter guesses should be saved
	 * @param outputDirPath
	 *          the output directory where we write the evaluation results
	 */
	public void doCommandEvaluateFull(String letterModelPath, String splitModelPath, String mergeModelPath, CorpusSelectionCriteria criteria, boolean save,
			String outputDirPath, MostLikelyWordChooser wordChooser, BoundaryDetectorType boundaryDetectorType, double minProbForDecision, String suffix,
			List<DocumentObserver> observers) throws IOException {
		if (letterModelPath.length() == 0)
			throw new RuntimeException("Missing argument: letterModel");
		if (outputDirPath == null || outputDirPath.length() == 0)
			throw new RuntimeException("Missing argument: outputDir");

		File outputDir = new File(outputDirPath);
		outputDir.mkdirs();
		String baseName = letterModelPath.substring(0, letterModelPath.indexOf("."));
		if (baseName.lastIndexOf("/") > 0)
			baseName = baseName.substring(baseName.lastIndexOf("/") + 1);

		MachineLearningModelFactory modelFactory = new MachineLearningModelFactory();
		ClassificationModel letterModel = null;
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(letterModelPath))) {
			letterModel = modelFactory.getClassificationModel(zis);
		}

		List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
		Set<LetterFeature<?>> letterFeatures = letterFeatureService.getLetterFeatureSet(letterFeatureDescriptors);

		LetterGuesser letterGuesser = letterGuesserService.getLetterGuesser(letterFeatures, letterModel.getDecisionMaker());

		if (splitModelPath.length() == 0)
			throw new RuntimeException("Missing argument: splitModel");
		if (!splitModelPath.endsWith(".zip"))
			throw new RuntimeException("splitModel must end with .zip");

		ClassificationModel splitModel = null;
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(splitModelPath))) {
			splitModel = modelFactory.getClassificationModel(zis);
		}

		List<String> splitFeatureDescriptors = splitModel.getFeatureDescriptors();
		Set<SplitFeature<?>> splitFeatures = boundaryFeatureService.getSplitFeatureSet(splitFeatureDescriptors);

		double minWidthRatioForSplit = 1.1;
		double minHeightRatioForSplit = 1.0;
		int maxSplitDepth = 2;

		SplitCandidateFinder splitCandidateFinder = boundaryService.getSplitCandidateFinder();
		splitCandidateFinder.setMinDistanceBetweenSplits(5);

		ShapeSplitter shapeSplitter = boundaryService.getShapeSplitter(splitCandidateFinder, splitFeatures, splitModel.getDecisionMaker(), minWidthRatioForSplit,
				beamWidth, maxSplitDepth);

		if (mergeModelPath.length() == 0)
			throw new RuntimeException("Missing argument: mergeModel");
		if (!mergeModelPath.endsWith(".zip"))
			throw new RuntimeException("mergeModel must end with .zip");

		ClassificationModel mergeModel = null;
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(mergeModelPath))) {
			mergeModel = modelFactory.getClassificationModel(zis);
		}

		List<String> mergeFeatureDescriptors = mergeModel.getFeatureDescriptors();
		Set<MergeFeature<?>> mergeFeatures = boundaryFeatureService.getMergeFeatureSet(mergeFeatureDescriptors);
		double maxWidthRatioForMerge = 1.2;
		double maxDistanceRatioForMerge = 0.15;

		ShapeMerger shapeMerger = boundaryService.getShapeMerger(mergeFeatures, mergeModel.getDecisionMaker());

		BoundaryDetector boundaryDetector = null;
		switch (boundaryDetectorType) {
		case LetterByLetter:
			boundaryDetector = boundaryService.getLetterByLetterBoundaryDetector(shapeSplitter, shapeMerger, 5);
			break;
		case Deterministic:
			boundaryDetector = boundaryService.getDeterministicBoundaryDetector(shapeSplitter, shapeMerger, minProbForDecision);
			break;
		}
		boundaryDetector.setMinWidthRatioForSplit(minWidthRatioForSplit);
		boundaryDetector.setMinHeightRatioForSplit(minHeightRatioForSplit);
		boundaryDetector.setMaxWidthRatioForMerge(maxWidthRatioForMerge);
		boundaryDetector.setMaxDistanceRatioForMerge(maxDistanceRatioForMerge);

		ImageAnalyser evaluator = analyserService.getBeamSearchImageAnalyser(beamWidth, 0.01);
		evaluator.setLetterGuesser(letterGuesser);
		evaluator.setMostLikelyWordChooser(wordChooser);
		evaluator.setBoundaryDetector(boundaryDetector);

		LetterValidator letterValidator = new ComponentCharacterValidator(locale);

		OriginalShapeLetterAssigner shapeLetterAssigner = new OriginalShapeLetterAssigner();
		shapeLetterAssigner.setEvaluate(true);
		shapeLetterAssigner.setSave(save);
		shapeLetterAssigner.setLetterValidator(letterValidator);
		shapeLetterAssigner.setSingleLetterMethod(false);
		evaluator.addObserver(shapeLetterAssigner);

		ErrorLogger errorLogger = new ErrorLogger();
		if (wordChooser != null) {
			errorLogger.setLexicon(wordChooser.getLexicon());
			errorLogger.setWordSplitter(wordChooser.getWordSplitter());
		}
		Writer errorWriter = null;

		File errorFile = new File(outputDir, baseName + suffix + "errors.txt");
		errorFile.delete();
		errorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile, true), "UTF8"));

		errorLogger.setErrorWriter(errorWriter);
		evaluator.addObserver(errorLogger);

		JochreCorpusImageProcessor imageProcessor = graphicsService.getJochreCorpusImageProcessor(criteria);
		imageProcessor.addObserver(evaluator);
		for (DocumentObserver observer : observers)
			imageProcessor.addObserver(observer);
		imageProcessor.process();

		LOG.debug("F-score for " + letterModelPath + ": " + shapeLetterAssigner.getFScoreCalculator().getTotalFScore());

		String modelFileName = baseName + suffix + "_full";

		File fscoreFile = new File(outputDir, modelFileName + "_fscores.csv");
		Writer fscoreWriter = errorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fscoreFile, true), csvEncoding));
		shapeLetterAssigner.getFScoreCalculator().writeScoresToCSV(fscoreWriter);
	}

	/**
	 * Apply a set of features to a given image or a given shape.
	 */
	public void doCommandApplyFeatures(int imageId, int shapeId, List<String> featureDescriptors) {
		LetterFeatureTester featureTester = letterFeatureService.getFeatureTester();

		Set<LetterFeature<?>> features = letterFeatureService.getLetterFeatureSet(featureDescriptors);
		Set<String> letters = new HashSet<String>();

		featureTester.applyFeatures(features, letters, imageId, shapeId);
	}

	/**
	 * Update the images in an existing Jochre document.
	 * 
	 * @param filename
	 *          the PDF file containing the images
	 * @param docId
	 *          the id of the document to update
	 * @param firstPage
	 *          the first page to segment, if &lt;=0 will start with first
	 *          document page
	 * @param lastPage
	 *          the last page to segment, if &lt;=0 will segment until last
	 *          document page
	 */
	public void doCommandUpdateImages(String filename, int docId, int firstPage, int lastPage) {
		if (filename.length() == 0)
			throw new RuntimeException("Missing argument: file");
		if (docId < 0)
			throw new RuntimeException("Missing argument: docId");

		JochreDocument doc = documentService.loadJochreDocument(docId);
		if (filename.toLowerCase().endsWith(".pdf")) {
			File pdfFile = new File(filename);
			PdfImageVisitor pdfImageVisitor = pdfService.getPdfImageVisitor(pdfFile, firstPage, lastPage, new PdfImageUpdater(doc));
			pdfImageVisitor.visitImages();
		} else {
			throw new RuntimeException("Unrecognised file extension");
		}
	}

	/**
	 * Extract the images from a PDF file.
	 * 
	 * @param filename
	 *          the path to the PDF file
	 * @param outputDirPath
	 *          the directory where to store the images extracted.
	 * @param firstPage
	 *          the first page to segment, if &lt;=0 will start with first
	 *          document page
	 * @param lastPage
	 *          the last page to segment, if &lt;=0 will segment until last
	 *          document page
	 */
	public void doCommandExtractImages(String filename, String outputDirPath, int firstPage, int lastPage) {
		if (filename.length() == 0)
			throw new RuntimeException("Missing argument: file");
		if (outputDirPath.length() == 0)
			throw new RuntimeException("Missing argument: outputDir");

		if (filename.toLowerCase().endsWith(".pdf")) {
			File pdfFile = new File(filename);
			PdfImageSaver pdfImageSaver = pdfService.getPdfImageSaver(pdfFile);
			pdfImageSaver.saveImages(outputDirPath, firstPage, lastPage);
		} else {
			throw new RuntimeException("Unrecognised file extension");
		}
	}

	/**
	 * Segment a file, without analysing it.
	 * 
	 * @param filename
	 *          the path of the file to load
	 * @param userFriendlyName
	 *          a name to store against this file in the database
	 * @param showSegmentation
	 *          whether or not to output the graphical segmentation files
	 * @param outputDirPath
	 *          an output directory for the graphical segmentation files
	 * @param save
	 *          should we save this file to the database?
	 * @param firstPage
	 *          the first page to segment, if &lt;=0 will start with first
	 *          document page
	 * @param lastPage
	 *          the last page to segment, if &lt;=0 will segment until last
	 *          document page
	 */
	public void doCommandSegment(String filename, String userFriendlyName, boolean showSegmentation, boolean drawPixelSpread, String outputDirPath, boolean save,
			int firstPage, int lastPage) {

		if (filename.length() == 0)
			throw new RuntimeException("Missing argument: file");
		if (userId < 0 && save)
			throw new RuntimeException("Missing argument (for save=true): userId");

		User user = null;
		if (userId >= 0) {
			user = User.loadUser(userId);
		}

		File file = new File(filename);
		JochreDocumentGenerator sourceFileProcessor = this.documentService.getJochreDocumentGenerator(file.getName(), userFriendlyName, locale);
		sourceFileProcessor.setDrawPixelSpread(drawPixelSpread);
		if (save)
			sourceFileProcessor.requestSave(user);
		if (showSegmentation) {
			if (outputDirPath != null && outputDirPath.length() > 0) {
				File outputDir = new File(outputDirPath);
				outputDir.mkdirs();
				sourceFileProcessor.requestSegmentation(outputDir);
			}
		}

		if (filename.toLowerCase().endsWith(".pdf")) {
			PdfImageVisitor pdfImageVisitor = pdfService.getPdfImageVisitor(file, firstPage, lastPage, sourceFileProcessor);
			pdfImageVisitor.visitImages();
		} else if (filename.toLowerCase().endsWith(".png") || filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")
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
				if (page.getIndex() == pageIndex) {
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
		public JochreImage onImageFound(JochrePage jochrePage, BufferedImage image, String imageName, int imageIndex) {
			JochreImage theImage = jochrePage.getImages().get(0);
			theImage.setOriginalImage(image);
			theImage.save();
			return theImage;
		}

	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public void setLocale(Locale locale) {
		this.locale = locale;
		JochreSession jochreSession = JochreSession.getInstance();
		jochreSession.setLocale(locale);
	}

	public GraphicsService getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}

	protected Lexicon readLexicon(File lexiconDir) {
		Lexicon myLexicon = null;

		if (lexiconDir.isDirectory()) {
			LexiconMerger lexiconMerger = new LexiconMerger();
			File[] lexiconFiles = lexiconDir.listFiles();
			for (File lexiconFile : lexiconFiles) {
				if (lexiconFile.getName().endsWith(".txt")) {
					TextFileLexicon textFileLexicon = new TextFileLexicon(lexiconFile, encoding);
					lexiconMerger.addLexicon(textFileLexicon);
				} else {
					TextFileLexicon textFileLexicon = TextFileLexicon.deserialize(lexiconFile);
					lexiconMerger.addLexicon(textFileLexicon);
				}
			}

			myLexicon = lexiconMerger;
		} else {
			if (lexiconDir.getName().endsWith(".txt")) {
				TextFileLexicon textFileLexicon = new TextFileLexicon(lexiconDir, encoding);
				myLexicon = textFileLexicon;
			} else {
				TextFileLexicon textFileLexicon = TextFileLexicon.deserialize(lexiconDir);
				myLexicon = textFileLexicon;
			}
		}
		return myLexicon;
	}

	@Override
	public Lexicon getLexicon() {
		if (lexicon == null) {
			if (lexiconPath != null && lexiconPath.length() > 0) {
				File lexiconDir = new File(lexiconPath);
				Lexicon myLexicon = this.readLexicon(lexiconDir);
				this.lexicon = new DefaultLexiconWrapper(myLexicon);
			} else {
				this.lexicon = new FakeLexicon();
			}
		}
		return this.lexicon;
	}

	@Override
	public WordSplitter getWordSplitter() {
		if (wordSplitter == null)
			wordSplitter = new DefaultWordSplitter();
		return wordSplitter;
	}

	@Override
	public String getLexiconPath() {
		return lexiconPath;
	}

	@Override
	public void setLexiconPath(String lexiconPath) {
		this.lexiconPath = lexiconPath;
	}

	public List<DocumentObserver> getObservers(List<OutputFormat> outputFormats, String baseName, File outputDir, OutputService outputService) {
		try {
			List<DocumentObserver> observers = new ArrayList<DocumentObserver>();

			for (OutputFormat outputFormat : outputFormats) {
				switch (outputFormat) {
				case AbbyyFineReader8: {
					Writer analysisFileWriter = null;
					String outputFileName = baseName + "_abbyy8.xml";
					File analysisFile = new File(outputDir, outputFileName);
					analysisFile.delete();
					analysisFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(analysisFile, true), "UTF8"));

					DocumentObserver observer = outputService.getExporter(analysisFileWriter, ExportFormat.Abbyy);
					observers.add(observer);
					break;
				}
				case Alto3: {
					if (baseName != null) {
						Writer analysisFileWriter = null;
						String outputFileName = baseName + "_alto3.xml";
						File analysisFile = new File(outputDir, outputFileName);
						analysisFile.delete();
						analysisFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(analysisFile, true), "UTF8"));

						DocumentObserver observer = outputService.getExporter(analysisFileWriter, ExportFormat.Alto);
						observers.add(observer);
					} else {
						DocumentObserver observer = outputService.getExporter(outputDir, ExportFormat.Alto);
						observers.add(observer);
					}
					break;
				}
				case Alto3zip: {
					String outputFileName = baseName + ".zip";
					File zipFile = new File(outputDir, outputFileName);
					zipFile.delete();

					ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile, false));
					ZipEntry zipEntry = new ZipEntry(baseName + "_alto3.xml");
					zos.putNextEntry(zipEntry);
					Writer zipWriter = new BufferedWriter(new OutputStreamWriter(zos, "UTF-8"));

					DocumentObserver observer = outputService.getExporter(zipWriter, ExportFormat.Alto);
					observers.add(observer);
					break;
				}
				case HTML: {
					if (baseName != null) {
						Writer htmlWriter = null;
						String htmlFileName = baseName + ".html";

						File htmlFile = new File(outputDir, htmlFileName);
						htmlFile.delete();
						htmlWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(htmlFile, true), "UTF8"));

						DocumentObserver textGetter = outputService.getTextGetter(htmlWriter, TextFormat.XHTML, this.getLexicon());
						observers.add(textGetter);
					} else {
						DocumentObserver textGetter = outputService.getTextGetter(outputDir, TextFormat.XHTML, this.getLexicon());
						observers.add(textGetter);
					}
					break;
				}
				case Text: {
					if (baseName != null) {
						Writer htmlWriter = null;
						String htmlFileName = baseName + ".txt";

						File htmlFile = new File(outputDir, htmlFileName);
						htmlFile.delete();
						htmlWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(htmlFile, true), "UTF8"));

						DocumentObserver textGetter = outputService.getTextGetter(htmlWriter, TextFormat.PLAIN, this.getLexicon());
						observers.add(textGetter);
					} else {
						DocumentObserver textGetter = outputService.getTextGetter(outputDir, TextFormat.PLAIN, this.getLexicon());
						observers.add(textGetter);
					}
					break;
				}
				case GuessText: {
					if (baseName != null) {
						Writer analysisFileWriter = null;
						String outputFileName = baseName + "_guess.txt";
						File analysisFile = new File(outputDir, outputFileName);
						analysisFile.delete();
						analysisFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(analysisFile, true), "UTF8"));

						DocumentObserver observer = outputService.getExporter(analysisFileWriter, ExportFormat.GuessedText);
						observers.add(observer);
					} else {
						DocumentObserver observer = outputService.getExporter(outputDir, ExportFormat.GuessedText);
						observers.add(observer);
					}
					break;
				}
				case ImageExtractor: {
					DocumentObserver imageExtractor = new ImageExtractor(outputDir, baseName);
					observers.add(imageExtractor);
					break;
				}
				case Jochre: {
					Writer analysisFileWriter = null;
					String outputFileName = baseName + ".xml";
					File analysisFile = new File(outputDir, outputFileName);
					analysisFile.delete();
					analysisFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(analysisFile, true), "UTF8"));

					DocumentObserver observer = outputService.getExporter(analysisFileWriter, ExportFormat.Jochre);
					observers.add(observer);
					break;
				}
				case JochrePageByPage: {
					outputDir.mkdirs();
					File zipFile = new File(outputDir, baseName + "_jochre.zip");

					DocumentObserver observer = outputService.getJochrePageByPageExporter(zipFile, baseName);
					observers.add(observer);
					break;
				}
				case Metadata: {
					DocumentObserver observer = new MetaDataExporter(outputDir, baseName);
					observers.add(observer);
					break;
				}
				case UnknownWords: {
					if (this.getLexicon() != null) {
						File unknownWordFile = new File(outputDir, baseName + "_unknownWords.txt");
						unknownWordFile.delete();
						Writer unknownWordWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(unknownWordFile, true), "UTF8"));

						UnknownWordListWriter unknownWordListWriter = new UnknownWordListWriter(unknownWordWriter);
						observers.add(unknownWordListWriter);
					}
					break;
				}
				}
			}
			return observers;
		} catch (IOException e) {
			LOG.error("Couldn't configure observers", e);
			throw new RuntimeException(e);
		}
	}
}