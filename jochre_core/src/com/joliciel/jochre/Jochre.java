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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

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
import com.joliciel.jochre.doc.SourceFileProcessor;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.CorpusSelectionCriteria;
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
import com.joliciel.jochre.output.OutputService;
import com.joliciel.jochre.output.TextFormat;
import com.joliciel.jochre.pdf.PdfImageVisitor;
import com.joliciel.jochre.pdf.PdfImageSaver;
import com.joliciel.jochre.pdf.PdfService;
import com.joliciel.jochre.security.SecurityService;
import com.joliciel.jochre.security.User;
import com.joliciel.jochre.stats.FScoreCalculator;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.OutcomeEqualiserEventStream;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * Class encapsulating the various top-level Jochre commands and command-line interface.
 * @author Assaf Urieli
 *
 */
public class Jochre implements LocaleSpecificLexiconService {
	private static final Log LOG = LogFactory.getLog(Jochre.class);

	public enum BoundaryDetectorType {
		LetterByLetter,
		Deterministic
	}
	
	public enum OutputFormat {
		Jochre,
		JochrePageByPage,
		AbbyyFineReader8,
		HTML,
		UnknownWords
	}
	
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
	MachineLearningService machineLearningService;
	FeatureService featureService;

	Locale locale = null;
	int userId = -1;
	String dataSourcePropertiesPath;
	
	String encoding = null;
	String lexiconPath = null;
	WordSplitter wordSplitter = null;
	Lexicon lexicon = null;
	Map<String,Set<Integer>> documentGroups = new LinkedHashMap<String, Set<Integer>>();
	
	public Jochre() { }

	private void initialise() {
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
		machineLearningService = locator.getMachineLearningServiceLocator().getMachineLearningService();
		featureService = locator.getFeatureService();
	}
	
	public static void main(String[] args) throws Exception {
		Map<String, String> argMap = new HashMap<String, String>();
		
		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			argMap.put(argName, argValue);
		}
		
		Jochre jochre = new Jochre();
		jochre.execute(argMap);
	}
	
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
	public void execute(Map<String, String> argMap) throws Exception {
		if (argMap.size()==0) {
			System.out.println("Usage (* indicates optional):");
			System.out.println("Jochre command=load file=[filename] name=[userFriendlyName] lang=[isoLanguageCode] first=[firstPage]* last=[lastPage]* outputDir=[outputDirectory]* showSeg=[true/false]");
			System.out.println("Jochre command=extract file=[filename] outputDir=[outputDirectory] first=[firstPage]* last=[lastPage]*");
			System.out.println("Jochre command=analyse");
			System.out.println("Jochre command=train file=[filename] outputDir=[outputDirectory] iterations=[iterations] cutoff=[cutoff]");
			return;
		}
		
		String logConfigPath = argMap.get("logConfigFile");
		if (logConfigPath!=null) {
			argMap.remove("logConfigFile");
			Properties props = new Properties();
			props.load(new FileInputStream(logConfigPath));
			PropertyConfigurator.configure(props);
		}
		
		File performanceConfigFile = null;

		String command = "";
		String filename = "";
		String userFriendlyName = "";
		String outputDirPath = null;
		int firstPage = -1;
		int lastPage = -1;
		int shapeId = -1;
		int docId = -1;
		int imageId = 0;
		int iterations = 100;
		int cutoff = 0;
		int userId = -1;
		int imageCount = 0;
		int multiplier = 0;
		int beamWidth = 5;
		boolean showSegmentation = false;
		boolean drawPixelSpread = false;
		boolean save = false;
		String letterModelPath = "";
		String splitModelPath = "";
		String mergeModelPath = "";
		ImageStatus[] imageSet = new ImageStatus[] { ImageStatus.TRAINING_HELD_OUT };
		String letterFeatureFilePath = "";
		String splitFeatureFilePath = "";
		String mergeFeatureFilePath = "";
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
		double smoothing = 0.3;
		double frequencyLogBase = 10.0;
		String suffix = "";
		String dataSourcePath = null;
		String docGroupPath = null;
		boolean includeBeam = false;
		List<OutputFormat> outputFormats = new ArrayList<Jochre.OutputFormat>();
		outputFormats.add(OutputFormat.Jochre);
		outputFormats.add(OutputFormat.HTML);

		for (Entry<String, String> argMapEntry : argMap.entrySet()) {
			String argName = argMapEntry.getKey();
			String argValue = argMapEntry.getValue();
			if (argName.equals("command"))
				command = argValue;
			else if (argName.equals("file"))
				filename = argValue;
			else if (argName.equals("name"))
				userFriendlyName = argValue;
			else if (argName.equals("lang"))
				locale = new Locale(argValue);
			else if (argName.equals("first"))
				firstPage = Integer.parseInt(argValue);
			else if (argName.equals("last"))
				lastPage = Integer.parseInt(argValue);
			else if (argName.equals("outputDir"))
				outputDirPath = argValue;
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
			else if (argName.equals("imageStatus")) {
				if (argValue.equals("heldOut"))
					imageSet = new ImageStatus[] { ImageStatus.TRAINING_HELD_OUT };
				else if (argValue.equals("test"))
					imageSet = new ImageStatus[] { ImageStatus.TRAINING_TEST };
				else if (argValue.equals("training"))
					imageSet = new ImageStatus[] { ImageStatus.TRAINING_VALIDATED };
				else if (argValue.equals("all"))
					imageSet = new ImageStatus[] { ImageStatus.TRAINING_VALIDATED , ImageStatus.TRAINING_HELD_OUT, ImageStatus.TRAINING_TEST };
				else
					throw new RuntimeException("Unknown imageSet: " + argValue);
			}
			else if (argName.equals("reconstructLetters"))
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
			}
			else if (argName.equals("docGroupFile")) {
				docGroupPath = argValue;
			}
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
				if (outputFormats.size()==0)
					throw new JochreException("At least one outputFormat required.");
			}
			else
				throw new RuntimeException("Unknown argument: " + argName);
		}
		
		PerformanceMonitor.start(performanceConfigFile);
		try {
			if (userFriendlyName.length()==0)
				userFriendlyName = filename;
			
			if (locale==null) {
				throw new JochreException("Argument lang is required");
			}
			JochreSession jochreSession = JochreSession.getInstance();
			jochreSession.setLocale(locale);
			
    		if (encoding==null)
    			encoding = Charset.defaultCharset().name();
    		
			JochreServiceLocator locator = JochreServiceLocator.getInstance();
			if (dataSourcePath!=null)
				locator.setDataSourcePropertiesFile(dataSourcePath);

			this.initialise();

			this.setUserId(userId);
			
			CorpusSelectionCriteria criteria = this.getGraphicsService().getCorpusSelectionCriteria();
			criteria.setImageId(imageId);
			criteria.setImageCount(imageCount);
			criteria.setImageStatusesToInclude(imageSet);
			criteria.setExcludeImageId(excludeImageId);
			criteria.setCrossValidationSize(crossValidationSize);
			criteria.setIncludeIndex(includeIndex);
			criteria.setExcludeIndex(excludeIndex);
			criteria.setDocumentId(docId);
			criteria.setDocumentIds(documentSet);
			
			if (docGroupPath!=null) {
				File docGroupFile = new File(docGroupPath);
				Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(docGroupFile), encoding)));

				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					int equalsPos = line.indexOf('=');
					String groupName = line.substring(0, equalsPos);
					String[] ids = line.substring(equalsPos+1).split(",");
					Set<Integer> idSet = new HashSet<Integer>();
					for (String idString : ids) {
						idSet.add(Integer.parseInt(idString));
					}
					documentGroups.put(groupName, idSet);
				}
			}
			
			OutputService outputService = locator.getTextServiceLocator().getTextService();			
			MostLikelyWordChooser wordChooser = null;
			
			LexiconService lexiconService = locator.getLexiconServiceLocator().getLexiconService();

        	wordChooser = lexiconService.getMostLikelyWordChooser(this.getLexicon(), this.getWordSplitter());
        	wordChooser.setAdditiveSmoothing(smoothing);
        	wordChooser.setFrequencyLogBase(frequencyLogBase);
        	wordChooser.setFrequencyAdjusted(frequencyAdjusted);
			
        	jochreSession.setJunkConfidenceThreshold(junkThreshold);
			
			if (command.equals("segment")) {
				this.doCommandSegment(filename, userFriendlyName, showSegmentation, drawPixelSpread, outputDirPath, save, firstPage, lastPage);
			} else if (command.equals("extract")) {
				this.doCommandExtractImages(filename, outputDirPath, firstPage, lastPage);
			} else if (command.equals("updateImages")) {
				this.doCommandUpdateImages(filename, docId, firstPage, lastPage);
			} else if (command.equals("applyFeatures")) {
				this.doCommandApplyFeatures(imageId, shapeId, letterFeatureFilePath);
			} else if (command.equals("train")) {
				this.doCommandTrain(letterModelPath, letterFeatureFilePath, iterations, cutoff, criteria, reconstructLetters);
			} else if (command.equals("evaluate")||command.equals("evaluateComplex")) {
				this.doCommandEvaluate(letterModelPath, criteria, outputDirPath, wordChooser, beamWidth, reconstructLetters, save, suffix, includeBeam);
			} else if (command.equals("evaluateFull")) {
				this.doCommandEvaluateFull(letterModelPath, splitModelPath, mergeModelPath, criteria, save, outputDirPath, wordChooser, beamWidth, boundaryDetectorType, minProbForDecision, suffix);
			} else if (command.equals("analyse")) {
				this.doCommandAnalyse(letterModelPath, docId, criteria, wordChooser);
			} else if (command.equals("trainSplits")) {
				this.doCommandTrainSplits(splitModelPath, splitFeatureFilePath, iterations, cutoff, criteria);
			} else if (command.equals("evaluateSplits")) {
				this.doCommandEvaluateSplits(splitModelPath, criteria, beamWidth, minProbForDecision);
			} else if (command.equals("trainMerge")) {
				this.doCommandTrainMerge(mergeModelPath, mergeFeatureFilePath, multiplier, iterations, cutoff, criteria);	
			} else if (command.equals("evaluateMerge")) {
				this.doCommandEvaluateMerge(mergeModelPath, criteria, minProbForDecision);
			} else if (command.equals("logImage")) {
				this.doCommandLogImage(shapeId);
			} else if (command.equals("testFeature")) {
				this.doCommandTestFeature(shapeId);
			} else if (command.equals("serializeLexicon")) {
        		File outputDir = new File(outputDirPath);
        		outputDir.mkdirs();
        		
        		File inputFile = new File(filename);
        		if (inputFile.isDirectory()) {
        			File[] lexiconFiles = inputFile.listFiles();
        			for (File oneLexFile : lexiconFiles) {
        				LOG.debug(oneLexFile.getName() + ": " + ", size: " + oneLexFile.length());
        				
    	        		TextFileLexicon lexicon = new TextFileLexicon(oneLexFile, encoding);
    	        		
    	        		String baseName = oneLexFile.getName().substring(0, oneLexFile.getName().indexOf("."));
    		    		if (baseName.lastIndexOf("/")>0)
    		    			baseName = baseName.substring(baseName.lastIndexOf("/")+1);
    	
    		    		File lexiconFile = new File(outputDir, baseName + ".obj");
    	        		lexicon.serialize(lexiconFile);
        			}
        		} else {
	        		LOG.debug(filename + ": " + inputFile.exists() + ", size: " + inputFile.length());
	
	        		TextFileLexicon lexicon = new TextFileLexicon(inputFile, encoding);
	        		
	        		String baseName = filename.substring(0, filename.indexOf("."));
		    		if (baseName.lastIndexOf("/")>0)
		    			baseName = baseName.substring(baseName.lastIndexOf("/")+1);
	
		    		File lexiconFile = new File(outputDir, baseName + ".obj");
	        		lexicon.serialize(lexiconFile);
        		}
			} else if (command.equals("analyseFile")) {
				File pdfFile = new File(filename);
				File letterModelFile = new File(letterModelPath);
				File splitModelFile = null;
				File mergeModelFile = null;
				if (splitModelPath.length()>0)
					splitModelFile = new File(splitModelPath);
				if (mergeModelPath.length()>0)
					mergeModelFile = new File(mergeModelPath);
				
        		File outputDir = new File(outputDirPath);
        		outputDir.mkdirs();

        		String baseName = filename;
        		if (baseName.lastIndexOf('.')>0)
        			baseName = filename.substring(0, filename.lastIndexOf('.'));
	    		if (baseName.lastIndexOf('/')>0)
	    			baseName = baseName.substring(baseName.lastIndexOf('/')+1);
	    		if (baseName.lastIndexOf('\\')>0)
	    			baseName = baseName.substring(baseName.lastIndexOf('\\')+1);
	    		
        		List<DocumentObserver> observers = new ArrayList<DocumentObserver>();
        		
        		for (OutputFormat outputFormat : outputFormats) {
        			switch (outputFormat) {
        			case AbbyyFineReader8:
        			{
        		       	Writer analysisFileWriter = null;
        	    		String outputFileName = baseName+ "_abbyy8.xml";
        				File analysisFile = new File(outputDir, outputFileName);
        				analysisFile.delete();
        				analysisFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(analysisFile, true),"UTF8"));
        				
        				DocumentObserver observer = outputService.getAbbyyFineReader8Exporter(analysisFileWriter);
        				observers.add(observer);
        				break;
        			}
        			case HTML:
        			{
        		       	Writer htmlWriter = null;
        	    		String htmlFileName = baseName+ ".html";
        	    		
        				File htmlFile = new File(outputDir, htmlFileName);
        				htmlFile.delete();
        				htmlWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(htmlFile, true),"UTF8"));
        				
        				DocumentObserver textGetter = outputService.getTextGetter(htmlWriter, TextFormat.XHTML, this.getLexicon());
        				observers.add(textGetter);
        				break;
        			}
        			case Jochre:
        			{
        		       	Writer analysisFileWriter = null;
        	    		String outputFileName = baseName+ ".xml";
        				File analysisFile = new File(outputDir, outputFileName);
        				analysisFile.delete();
        				analysisFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(analysisFile, true),"UTF8"));
        				
        				DocumentObserver observer = outputService.getJochreXMLExporter(analysisFileWriter);
        				observers.add(observer);
        				break;
        			}
           			case JochrePageByPage:
        			{
         				DocumentObserver observer = outputService.getJochrePageByPageExporter(outputDir, baseName);
        				observers.add(observer);
        				break;
        			}
        			case UnknownWords:
        			{
        				if (this.getLexicon()!=null) {
        					File unknownWordFile = new File(outputDir, "unknownWords.txt");
        					unknownWordFile.delete();
        					Writer unknownWordWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(unknownWordFile, true),"UTF8"));

        					UnknownWordListWriter unknownWordListWriter = new UnknownWordListWriter(unknownWordWriter);
        					observers.add(unknownWordListWriter);
        				}
        				break;
        			}
        			}
        		}
				
				
				this.doCommandAnalyse(pdfFile, letterModelFile, splitModelFile, mergeModelFile, wordChooser, observers, firstPage, lastPage);
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
			RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
			feature.check(shape, env);
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
				RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
				FeatureResult<?> weightedOutcome = feature.check(shape, env);

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
	public void doCommandBuildLexicon(String outputDirPath, WordSplitter wordSplitter, CorpusSelectionCriteria criteria) {
		try {
			CorpusLexiconBuilder builder = lexiconService.getCorpusLexiconBuilder(wordSplitter);
			builder.setCriteria(criteria);
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
	public void doCommandTrainMerge(String mergeModelPath, String mergeFeatureFilePath, int multiplier, int iterations, int cutoff, CorpusSelectionCriteria criteria) {
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
			ClassificationEventStream corpusEventStream = boundaryService.getJochreMergeEventStream(criteria, mergeFeatures, maxWidthRatio, maxDistanceRatio);
			if (multiplier > 0) {
				corpusEventStream = new OutcomeEqualiserEventStream(corpusEventStream, multiplier);
			}
			
			File file = new File(mergeModelPath);
			Map<String,Object> trainParameters = new HashMap<String, Object>();
			trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Iterations.name(), iterations);
			trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Cutoff.name(), cutoff);
			ClassificationModelTrainer trainer = machineLearningService.getClassificationModelTrainer(MachineLearningAlgorithm.MaxEnt, trainParameters);

			ClassificationModel mergeModel = trainer.trainModel(corpusEventStream, mergeFeatureDescriptors);
			mergeModel.persist(file);
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
	 * @param minProbForDecision 
	 * @throws IOException
	 */
	public void doCommandEvaluateMerge(String mergeModelPath, CorpusSelectionCriteria criteria, double minProbForDecision) throws IOException {
		if (mergeModelPath.length()==0)
			throw new RuntimeException("Missing argument: mergeModel");
		if (!mergeModelPath.endsWith(".zip"))
			throw new RuntimeException("mergeModel must end with .zip");

		ZipInputStream zis = new ZipInputStream(new FileInputStream(mergeModelPath));
		ClassificationModel mergeModel = machineLearningService.getClassificationModel(zis);
		
		List<String> mergeFeatureDescriptors = mergeModel.getFeatureDescriptors();
		Set<MergeFeature<?>> mergeFeatures = boundaryFeatureService.getMergeFeatureSet(mergeFeatureDescriptors);
		
		JochreCorpusGroupReader groupReader = graphicsService.getJochreCorpusGroupReader();
		groupReader.setSelectionCriteria(criteria);
	
		double maxWidthRatio = 1.2;
		double maxDistanceRatio = 0.15;
		
		ShapeMerger merger = boundaryService.getShapeMerger(mergeFeatures, mergeModel.getDecisionMaker());
		MergeEvaluator evaluator = boundaryService.getMergeEvaluator(maxWidthRatio, maxDistanceRatio);
		if (minProbForDecision>=0)
			evaluator.setMinProbabilityForDecision(minProbForDecision);
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
	public void doCommandTrainSplits(String splitModelPath, String splitFeatureFilePath, int iterations, int cutoff, CorpusSelectionCriteria criteria) {
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
			ClassificationEventStream corpusEventStream = boundaryService.getJochreSplitEventStream(criteria, splitFeatures, minWidthRatio, minHeightRatio);
		
			File splitModelFile = new File(splitModelPath);
			Map<String,Object> trainParameters = new HashMap<String, Object>();
			trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Iterations.name(), iterations);
			trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Cutoff.name(), cutoff);
			ClassificationModelTrainer trainer = machineLearningService.getClassificationModelTrainer(MachineLearningAlgorithm.MaxEnt, trainParameters);

			ClassificationModel splitModel = trainer.trainModel(corpusEventStream, splitFeatureDescriptors);
			splitModel.persist(splitModelFile);

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
	 * @param minProbForDecision 
	 * @throws IOException
	 */
	public void doCommandEvaluateSplits(String splitModelPath, CorpusSelectionCriteria criteria, int beamWidth, double minProbForDecision) throws IOException {
		if (splitModelPath.length()==0)
			throw new RuntimeException("Missing argument: splitModel");
		if (!splitModelPath.endsWith(".zip"))
			throw new RuntimeException("splitModel must end with .zip");
		
		ZipInputStream zis = new ZipInputStream(new FileInputStream(splitModelPath));
		ClassificationModel splitModel = machineLearningService.getClassificationModel(zis);
		
		List<String> splitFeatureDescriptors = splitModel.getFeatureDescriptors();
		Set<SplitFeature<?>> splitFeatures = boundaryFeatureService.getSplitFeatureSet(splitFeatureDescriptors);
			
		double minWidthRatio = 1.1;
		double minHeightRatio = 1.0;
		int maxDepth = 2;
		
		SplitCandidateFinder splitCandidateFinder = boundaryService.getSplitCandidateFinder();
		splitCandidateFinder.setMinDistanceBetweenSplits(5);
		
		ShapeSplitter shapeSplitter = boundaryService.getShapeSplitter(splitCandidateFinder, splitFeatures, splitModel.getDecisionMaker(), minWidthRatio, beamWidth, maxDepth);
		
		JochreCorpusShapeReader shapeReader = graphicsService.getJochreCorpusShapeReader();
		shapeReader.setSelectionCriteria(criteria);
		
		SplitEvaluator splitEvaluator = boundaryService.getSplitEvaluator(5, minWidthRatio, minHeightRatio);
		if (minProbForDecision>=0)
			splitEvaluator.setMinProbabilityForDecision(minProbForDecision);
		
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
	public void doCommandTrain(String letterModelPath, String letterFeatureFilePath, int iterations, int cutoff, CorpusSelectionCriteria criteria, boolean reconstructLetters) {
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
	
			ClassificationEventStream corpusEventStream = letterGuesserService.getJochreLetterEventStream(criteria, features, boundaryDetector, letterValidator);
			
			File letterModelFile = new File(letterModelPath);
			
			Map<String,Object> trainParameters = new HashMap<String, Object>();
			trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Iterations.name(), iterations);
			trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Cutoff.name(), cutoff);
			
			ClassificationModelTrainer trainer = machineLearningService.getClassificationModelTrainer(MachineLearningAlgorithm.MaxEnt, trainParameters);
			
			ClassificationModel letterModel = trainer.trainModel(corpusEventStream, descriptors);
			letterModel.persist(letterModelFile);

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
	 * @param imageId2 
	 * @param outputDirPath the directory to which we write the evaluation files
	 * @param includeBeam 
	 * @param lexicon the lexicon to use for word correction
	 * @throws IOException
	 */
	public void doCommandEvaluate(String letterModelPath, CorpusSelectionCriteria criteria, String outputDirPath, MostLikelyWordChooser wordChooser, int beamWidth, boolean reconstructLetters, boolean save, String suffix, boolean includeBeam) throws IOException {
		if (letterModelPath.length()==0)
			throw new RuntimeException("Missing argument: letterModel");
		if (!letterModelPath.endsWith(".zip"))
			throw new RuntimeException("letterModel must end with .zip");
    	if (outputDirPath==null||outputDirPath.length()==0)
    		throw new RuntimeException("Missing argument: outputDir");

   		File outputDir = new File(outputDirPath);
		outputDir.mkdirs();

		ZipInputStream zis = new ZipInputStream(new FileInputStream(letterModelPath));
		ClassificationModel letterModel = machineLearningService.getClassificationModel(zis);
		
		List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
		Set<LetterFeature<?>> letterFeatures = letterFeatureService.getLetterFeatureSet(letterFeatureDescriptors);
		
		LetterGuesser letterGuesser = letterGuesserService.getLetterGuesser(letterFeatures, letterModel.getDecisionMaker());
		
		String baseName = letterModelPath.substring(0, letterModelPath.indexOf("."));
		if (baseName.lastIndexOf("/")>0)
			baseName = baseName.substring(baseName.lastIndexOf("/")+1);
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
		if (wordChooser!=null) {
			errorLogger.setLexicon(wordChooser.getLexicon());
			errorLogger.setWordSplitter(wordChooser.getWordSplitter());
		}
       	Writer errorWriter = null;
    	
		File errorFile = new File(outputDir, baseName + "_errors.txt");
		errorFile.delete();
		errorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile, true),"UTF8"));
    	
    	errorLogger.setErrorWriter(errorWriter);
    	evaluator.addObserver(errorLogger);
    	
		LexiconErrorWriter lexiconErrorWriter = new LexiconErrorWriter(outputDir, baseName, wordChooser);
		if (documentGroups!=null)
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
		
    	JochreCorpusImageReader imageReader = graphicsService.getJochreCorpusImageReader();
    	imageReader.setSelectionCriteria(criteria);
    	
//			evaluator.setOutcomesToAnalyse(new String[] {"מ"});
		try {
			evaluator.analyse(imageReader);
		} finally {
			if (errorWriter!=null)
				errorWriter.close();
		}
		LOG.debug("F-score for " + letterModelPath + ": " + fScoreObserver.getFScoreCalculator().getTotalFScore());

		String modelFileName = baseName;
		if (reconstructLetters)
			modelFileName += "_Reconstruct";

		File fscoreFile = new File(outputDir, modelFileName + "_fscores.csv");
		fScoreObserver.getFScoreCalculator().writeScoresToCSVFile(fscoreFile);

	}
	
	/**
	 * Analyse a document or image or test set based on a given letter-guessing model.
	 * @param letterModelPath the path to the letter-guessing model.
	 * @param docId the document to be analysed
	 * @param imageId the image to be analysed
	 * @param testSet the test set to be analysed
	 * @throws IOException
	 */
	public void doCommandAnalyse(String letterModelPath, int docId, CorpusSelectionCriteria criteria, MostLikelyWordChooser wordChooser) throws IOException {
		if (letterModelPath.length()==0)
			throw new RuntimeException("Missing argument: letterModel");
		if (!letterModelPath.endsWith(".zip"))
			throw new RuntimeException("letterModel must end with .zip");
		

		ZipInputStream zis = new ZipInputStream(new FileInputStream(letterModelPath));
		ClassificationModel letterModel = machineLearningService.getClassificationModel(zis);
		
		List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
		Set<LetterFeature<?>> letterFeatures = letterFeatureService.getLetterFeatureSet(letterFeatureDescriptors);
		
		LetterGuesser letterGuesser = letterGuesserService.getLetterGuesser(letterFeatures, letterModel.getDecisionMaker());
		
		ImageAnalyser analyser = analyserService.getBeamSearchImageAnalyser(5, 0.01);
		analyser.setLetterGuesser(letterGuesser);
		analyser.setMostLikelyWordChooser(wordChooser);
		
		JochreCorpusImageReader imageReader = graphicsService.getJochreCorpusImageReader();
		imageReader.setSelectionCriteria(criteria);
		
		LetterAssigner letterAssigner = new LetterAssigner();

		analyser.addObserver(letterAssigner);
		
		if (docId>0) {
			JochreDocument doc = documentService.loadJochreDocument(docId);
			for (JochrePage page : doc.getPages()) {
				for (JochreImage image : page.getImages()) {
					if (image.getImageStatus().equals(ImageStatus.AUTO_NEW)) {
						analyser.analyse(image);
					}
					image.clearMemory();
				}
			}
		} else {
			analyser.analyse(imageReader);
		}

	}
	
	public void doCommandAnalyse(File sourceFile, File letterModelFile, File splitModelFile, File mergeModelFile, MostLikelyWordChooser wordChooser, List<DocumentObserver> observers, int firstPage, int lastPage) throws IOException {
		ZipInputStream zis = new ZipInputStream(new FileInputStream(letterModelFile));
		ClassificationModel letterModel = machineLearningService.getClassificationModel(zis);

		List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
		Set<LetterFeature<?>> letterFeatures = letterFeatureService.getLetterFeatureSet(letterFeatureDescriptors);
		LetterGuesser letterGuesser = letterGuesserService.getLetterGuesser(letterFeatures, letterModel.getDecisionMaker());
		ImageAnalyser analyser = analyserService.getBeamSearchImageAnalyser(5, 0.01);
		analyser.setLetterGuesser(letterGuesser);
		analyser.setMostLikelyWordChooser(wordChooser);
		BoundaryDetector boundaryDetector = null;
		
		if (splitModelFile!=null && mergeModelFile!=null) {
			double minWidthRatioForSplit = 1.1;
			double minHeightRatioForSplit = 1.0;
			int splitBeamWidth = 5;
			int maxSplitDepth = 2;
			
			SplitCandidateFinder splitCandidateFinder = boundaryService.getSplitCandidateFinder();
			splitCandidateFinder.setMinDistanceBetweenSplits(5);
			
			ZipInputStream splitZis = new ZipInputStream(new FileInputStream(splitModelFile));
			ClassificationModel splitModel = machineLearningService.getClassificationModel(splitZis);
			List<String> splitFeatureDescriptors = splitModel.getFeatureDescriptors();
			Set<SplitFeature<?>> splitFeatures = boundaryFeatureService.getSplitFeatureSet(splitFeatureDescriptors);
			ShapeSplitter shapeSplitter = boundaryService.getShapeSplitter(splitCandidateFinder, splitFeatures, splitModel.getDecisionMaker(), minWidthRatioForSplit, splitBeamWidth, maxSplitDepth);
		
			ZipInputStream mergeZis = new ZipInputStream(new FileInputStream(splitModelFile));
			ClassificationModel mergeModel = machineLearningService.getClassificationModel(mergeZis);
			List<String> mergeFeatureDescriptors = mergeModel.getFeatureDescriptors();
			Set<MergeFeature<?>> mergeFeatures = boundaryFeatureService.getMergeFeatureSet(mergeFeatureDescriptors);
			double maxWidthRatioForMerge = 1.2;
			double maxDistanceRatioForMerge = 0.15;
			double minProbForDecision = 0.5;
			
			ShapeMerger shapeMerger = boundaryService.getShapeMerger(mergeFeatures, mergeModel.getDecisionMaker());

			boundaryDetector = boundaryService.getDeterministicBoundaryDetector(shapeSplitter, shapeMerger, minProbForDecision);
			boundaryDetector.setMinWidthRatioForSplit(minWidthRatioForSplit);
			boundaryDetector.setMinHeightRatioForSplit(minHeightRatioForSplit);
			boundaryDetector.setMaxWidthRatioForMerge(maxWidthRatioForMerge);
			boundaryDetector.setMaxDistanceRatioForMerge(maxDistanceRatioForMerge);
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
		documentGenerator.requestAnalysis(analyser);
		
		for (DocumentObserver observer : observers)
			documentGenerator.addDocumentObserver(observer);
		
		if (!sourceFile.exists())
			throw new JochreException("The file " + sourceFile.getPath() + " does not exist");
		
		if (sourceFile.getName().toLowerCase().endsWith(".pdf")) {
			PdfImageVisitor pdfImageVisitor = pdfService.getPdfImageVisitor(sourceFile, firstPage, lastPage, documentGenerator);
			
			pdfImageVisitor.visitImages();
		} else if (sourceFile.getName().toLowerCase().endsWith(".png")
				|| sourceFile.getName().toLowerCase().endsWith(".jpg")
				|| sourceFile.getName().toLowerCase().endsWith(".jpeg")
				|| sourceFile.getName().toLowerCase().endsWith(".gif")) {
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
	 * @param letterModelPath the path to the letter-guessing model
	 * @param splitModelPath the path to the splitting model
	 * @param mergeModelPath the path to the merging model
	 * @param testSet the set of images to evaluate in the saved corpus
	 * @param imageId the single image to evaluate in the saved corpus
	 * @param imageId2 
	 * @param save whether or not the letter guesses should be saved
	 * @param outputDirPath the output directory where we write the evaluation results
	 * @param boundaryDetectorType 
	 * @param minProbForDecision 
	 * @throws IOException
	 */
	public void doCommandEvaluateFull(String letterModelPath, String splitModelPath, String mergeModelPath, CorpusSelectionCriteria criteria, 
			boolean save, String outputDirPath, MostLikelyWordChooser wordChooser, int beamWidth, BoundaryDetectorType boundaryDetectorType, double minProbForDecision, String suffix) throws IOException {
		if (letterModelPath.length()==0)
			throw new RuntimeException("Missing argument: letterModel");
	   	if (outputDirPath==null||outputDirPath.length()==0)
    		throw new RuntimeException("Missing argument: outputDir");

   		File outputDir = new File(outputDirPath);
		outputDir.mkdirs();
		String baseName = letterModelPath.substring(0, letterModelPath.indexOf("."));
		if (baseName.lastIndexOf("/")>0)
			baseName = baseName.substring(baseName.lastIndexOf("/")+1);
		
		
		ZipInputStream zis = new ZipInputStream(new FileInputStream(letterModelPath));
		ClassificationModel letterModel = machineLearningService.getClassificationModel(zis);
		
		List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
		Set<LetterFeature<?>> letterFeatures = letterFeatureService.getLetterFeatureSet(letterFeatureDescriptors);
		
		LetterGuesser letterGuesser = letterGuesserService.getLetterGuesser(letterFeatures, letterModel.getDecisionMaker());
		
		if (splitModelPath.length()==0)
			throw new RuntimeException("Missing argument: splitModel");
		if (!splitModelPath.endsWith(".zip"))
			throw new RuntimeException("splitModel must end with .zip");

		ZipInputStream splitZis = new ZipInputStream(new FileInputStream(splitModelPath));
		ClassificationModel splitModel = machineLearningService.getClassificationModel(splitZis);
		
		List<String> splitFeatureDescriptors = splitModel.getFeatureDescriptors();
		Set<SplitFeature<?>> splitFeatures = boundaryFeatureService.getSplitFeatureSet(splitFeatureDescriptors);
		
		double minWidthRatioForSplit = 1.1;
		double minHeightRatioForSplit = 1.0;
		int maxSplitDepth = 2;
		
		SplitCandidateFinder splitCandidateFinder = boundaryService.getSplitCandidateFinder();
		splitCandidateFinder.setMinDistanceBetweenSplits(5);
	
		ShapeSplitter shapeSplitter = boundaryService.getShapeSplitter(splitCandidateFinder, splitFeatures, splitModel.getDecisionMaker(), minWidthRatioForSplit, beamWidth, maxSplitDepth);

		if (mergeModelPath.length()==0)
			throw new RuntimeException("Missing argument: mergeModel");
		if (!mergeModelPath.endsWith(".zip"))
			throw new RuntimeException("mergeModel must end with .zip");

		ZipInputStream mergeZis = new ZipInputStream(new FileInputStream(mergeModelPath));
		ClassificationModel mergeModel = machineLearningService.getClassificationModel(mergeZis);
		
		List<String> mergeFeatureDescriptors = mergeModel.getFeatureDescriptors();
		Set<MergeFeature<?>> mergeFeatures = boundaryFeatureService.getMergeFeatureSet(mergeFeatureDescriptors);
		double maxWidthRatioForMerge = 1.2;
		double maxDistanceRatioForMerge = 0.15;
		
		ShapeMerger shapeMerger = boundaryService.getShapeMerger(mergeFeatures, mergeModel.getDecisionMaker());
			
		JochreCorpusImageReader imageReader = graphicsService.getJochreCorpusImageReader();
		imageReader.setSelectionCriteria(criteria);
		
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
		if (wordChooser!=null) {
			errorLogger.setLexicon(wordChooser.getLexicon());
			errorLogger.setWordSplitter(wordChooser.getWordSplitter());
		}
       	Writer errorWriter = null;
    	
		File errorFile = new File(outputDir, baseName + suffix + "errors.txt");
		errorFile.delete();
		errorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile, true),"UTF8"));
    	
    	errorLogger.setErrorWriter(errorWriter);
    	evaluator.addObserver(errorLogger);
		
//			evaluator.setOutcomesToAnalyse(new String[] {"מ"});
		
		try {
			evaluator.analyse(imageReader);
		} finally {
			if (errorWriter!=null)
				errorWriter.close();
		}
		
		LOG.debug("F-score for " + letterModelPath + ": " + shapeLetterAssigner.getFScoreCalculator().getTotalFScore());
		
		String modelFileName = baseName + suffix + "_full";

		File fscoreFile = new File(outputDir, modelFileName + "_fscores.csv");
		shapeLetterAssigner.getFScoreCalculator().writeScoresToCSVFile(fscoreFile);
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
			PdfImageSaver pdfImageSaver = pdfService.getPdfImageSaver(pdfFile);
			pdfImageSaver.saveImages(outputDirPath, firstPage, lastPage);
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
			boolean drawPixelSpread,
			String outputDirPath,
			boolean save,
			int firstPage,
			int lastPage) {
		
		if (filename.length()==0)
			throw new RuntimeException("Missing argument: file");
		if (userId<0&&save)
			throw new RuntimeException("Missing argument (for save=true): userId");

		User user = null;
		if (userId>=0) {
			user = securityService.loadUser(userId);
		}
		

		
		File file = new File(filename);
		JochreDocumentGenerator sourceFileProcessor = this.documentService.getJochreDocumentGenerator(file.getName(), userFriendlyName, locale);
		sourceFileProcessor.setDrawPixelSpread(drawPixelSpread);
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
			if (lexiconPath!=null && lexiconPath.length()>0) {
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
		if (wordSplitter==null)
			wordSplitter = new DefaultWordSplitter();
		return wordSplitter;
	}

	public String getLexiconPath() {
		return lexiconPath;
	}

	public void setLexiconPath(String lexiconPath) {
		this.lexiconPath = lexiconPath;
	}

	
}
