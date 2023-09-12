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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.joliciel.jochre.doc.*;
import com.joliciel.jochre.pdf.PdfDocumentProcessor;
import com.joliciel.jochre.utils.pdf.PdfImageObserver;
import com.joliciel.jochre.utils.pdf.PdfImageVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.analyser.BeamSearchImageAnalyser;
import com.joliciel.jochre.analyser.ErrorLogger;
import com.joliciel.jochre.analyser.FScoreObserver;
import com.joliciel.jochre.analyser.ImageAnalyser;
import com.joliciel.jochre.analyser.LetterAssigner;
import com.joliciel.jochre.analyser.LetterGuessObserver;
import com.joliciel.jochre.analyser.OriginalShapeLetterAssigner;
import com.joliciel.jochre.analyser.SimpleLetterFScoreObserver;
import com.joliciel.jochre.boundaries.BoundaryDetector;
import com.joliciel.jochre.boundaries.DeterministicBoundaryDetector;
import com.joliciel.jochre.boundaries.JochreMergeEventStream;
import com.joliciel.jochre.boundaries.JochreSplitEventStream;
import com.joliciel.jochre.boundaries.LetterByLetterBoundaryDetector;
import com.joliciel.jochre.boundaries.MergeEvaluator;
import com.joliciel.jochre.boundaries.OriginalBoundaryDetector;
import com.joliciel.jochre.boundaries.RecursiveShapeSplitter;
import com.joliciel.jochre.boundaries.ShapeMerger;
import com.joliciel.jochre.boundaries.ShapeSplitter;
import com.joliciel.jochre.boundaries.SplitCandidateFinder;
import com.joliciel.jochre.boundaries.SplitEvaluator;
import com.joliciel.jochre.boundaries.TrainingCorpusShapeMerger;
import com.joliciel.jochre.boundaries.TrainingCorpusShapeSplitter;
import com.joliciel.jochre.boundaries.features.MergeFeature;
import com.joliciel.jochre.boundaries.features.MergeFeatureParser;
import com.joliciel.jochre.boundaries.features.SplitFeature;
import com.joliciel.jochre.boundaries.features.SplitFeatureParser;
import com.joliciel.jochre.graphics.CorpusSelectionCriteria;
import com.joliciel.jochre.graphics.GraphicsDao;
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
import com.joliciel.jochre.letterGuesser.JochreLetterEventStream;
import com.joliciel.jochre.letterGuesser.LetterGuesser;
import com.joliciel.jochre.letterGuesser.LetterValidator;
import com.joliciel.jochre.letterGuesser.features.LetterFeature;
import com.joliciel.jochre.letterGuesser.features.LetterFeatureParser;
import com.joliciel.jochre.letterGuesser.features.LetterFeatureTester;
import com.joliciel.jochre.lexicon.CorpusLexiconBuilder;
import com.joliciel.jochre.lexicon.LexiconErrorWriter;
import com.joliciel.jochre.lexicon.MostLikelyWordChooser;
import com.joliciel.jochre.lexicon.TextFileLexicon;
import com.joliciel.jochre.lexicon.UnknownWordListWriter;
import com.joliciel.jochre.output.AbbyyFineReader8Exporter;
import com.joliciel.jochre.output.AltoXMLExporter;
import com.joliciel.jochre.output.JochrePageByPageExporter;
import com.joliciel.jochre.output.JochreXMLExporter;
import com.joliciel.jochre.output.MetaDataExporter;
import com.joliciel.jochre.output.TextExporter;
import com.joliciel.jochre.output.TextGetter;
import com.joliciel.jochre.output.TextGetter.TextFormat;
import com.joliciel.jochre.pdf.PdfImageSaver;
import com.joliciel.jochre.security.SecurityDao;
import com.joliciel.jochre.security.User;
import com.joliciel.jochre.stats.FScoreCalculator;
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.jochre.utils.JochreLogUtils;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.ModelTrainerFactory;
import com.joliciel.talismane.machineLearning.OutcomeEqualiserEventStream;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Class encapsulating the various top-level Jochre commands and command-line
 * interface.
 * 
 * @author Assaf Urieli
 *
 */
public class Jochre {
  private static final Logger LOG = LoggerFactory.getLogger(Jochre.class);

  public enum BoundaryDetectorType {
    LetterByLetter, Deterministic
  }

  public enum OutputFormat {
    Jochre,
    JochrePageByPage,
    Alto3,
    Alto3zip,
    Alto4,
    Alto4zip,
    AbbyyFineReader8,
    HTML,
    UnknownWords,
    Metadata,
    ImageExtractor,
    Text,
    GuessText
  }

  private int userId = -1;

  private final Map<String, Set<Integer>> documentGroups = new LinkedHashMap<>();

  private final JochreSession jochreSession;
  private final Config config;

  public Jochre() throws ReflectiveOperationException {
    this(ConfigFactory.load());
  }

  public Jochre(Config config) throws ReflectiveOperationException {
    this.config = config;
    this.jochreSession = new JochreSession(config);
  }

  public Jochre(Config config, Map<String, String> argMap) throws ReflectiveOperationException {
    if (argMap != null) {
      Set<String> handledArgs = new HashSet<>();
      Map<String, Object> values = new HashMap<>();
      for (Entry<String, String> argMapEntry : argMap.entrySet()) {
        String argName = argMapEntry.getKey();
        String argValue = argMapEntry.getValue();
        boolean handled = true;

        if (argName.equals("drawSegmentedImage")) {
          boolean drawSegmentedImage = argValue.equals("true");
          values.put("jochre.segmentater.draw-segmented-image", drawSegmentedImage);
        } else if (argName.equals("drawPixelSpread")) {
          boolean drawPixelSpread = argValue.equals("true");
          values.put("jochre.segmentater.draw-pixel-spread", drawPixelSpread);
        } else if (argName.equals("letterModel")) {
          values.put("jochre.image-analyser.letter-model", argValue);
        } else if (argName.equals("splitModel")) {
          values.put("jochre.image-analyser.split-model", argValue);
        } else if (argName.equals("mergeModel")) {
          values.put("jochre.image-analyser.merge-model", argValue);
        } else if (argName.equals("junkThreshold")) {
          values.put("jochre.image-analyser.junk-threshold", Double.parseDouble(argValue));
        } else if (argName.equals("unknownWordFactor")) {
          values.put("jochre.word-chooser.unknown-word-factor", Double.parseDouble(argValue));
        } else if (argName.equals("beamWidth")) {
          values.put("jochre.image-analyser.beam-width", Integer.parseInt(argValue));
          values.put("jochre.boundaries.splitter.beam-width", Integer.parseInt(argValue));
        } else if (argName.equals("lexicon")) {
          values.put("jochre.lexicon", argValue);
        } else if (argName.equals("targetShortDimension")) {
          values.put("jochre.segmenter.target-short-dimension", Integer.parseInt(argValue));
        } else if (argName.equals("isCleanSegment")) {
          values.put("jochre.segmenter.is-clean-segment", Boolean.parseBoolean(argValue));
        } else {
          handled = false;
        }

        if (handled)
          handledArgs.add(argName);
      }
      argMap.keySet().removeAll(handledArgs);

      config = ConfigFactory.parseMap(values).withFallback(config);
    }
    this.config = config;
    this.jochreSession = new JochreSession(config);

  }

  public static void main(String[] args) throws Exception {
    Map<String, String> argMap = new HashMap<>();

    for (String arg : args) {
      int equalsPos = arg.indexOf('=');
      String argName = arg.substring(0, equalsPos);
      String argValue = arg.substring(equalsPos + 1);
      argMap.put(argName, argValue);
    }

    Jochre jochre = new Jochre(ConfigFactory.load(), argMap);
    jochre.execute(argMap);
  }

  /**
   * Usage (* indicates optional):<br/>
   * Jochre load [filename] [isoLanguageCode] [firstPage]* [lastPage]*<br/>
   * Loads a file (pdf or image) and segments it into letters. The analysed
   * version is stored in the persistent store. Writes [filename].xml to the same
   * location, to enable the user to indicate the text to associate with this
   * file.<br/>
   * Jochre extract [filename] [outputDirectory] [firstPage]* [lastPage]*<br/>
   * Extracts images form a pdf file.<br/>
   */
  public void execute(Map<String, String> argMap) throws Exception {
    if (argMap.size() == 0) {
      System.out.println("See jochre wiki for usage");
      return;
    }

    String logConfigPath = argMap.get("logConfigFile");
    if (logConfigPath != null) {
      argMap.remove("logConfigFile");
      JochreLogUtils.configureLogging(logConfigPath);
    }

    String command = "";
    String inFilePath = "";
    String inDirPath = null;
    String userFriendlyName = "";
    String outputDirPath = null;
    String outputFilePath = null;
    int firstPage = -1;
    int lastPage = -1;
    Set<Integer> pages = Collections.emptySet();
    int shapeId = -1;
    int docId = -1;
    int imageId = 0;
    int userId = -1;
    int imageCount = 0;
    int multiplier = 0;
    boolean save = false;
    ImageStatus[] imageSet = null;
    boolean reconstructLetters = false;
    int excludeImageId = 0;
    int crossValidationSize = -1;
    int includeIndex = -1;
    int excludeIndex = -1;
    Set<Integer> documentSet = null;
    String suffix = "";
    String docGroupPath = null;
    boolean includeBeam = false;
    List<OutputFormat> outputFormats = new ArrayList<>();
    String docSelectionPath = null;
    List<String> featureDescriptors = null;
    boolean includeDate = false;

    for (Entry<String, String> argMapEntry : argMap.entrySet()) {
      String argName = argMapEntry.getKey();
      String argValue = argMapEntry.getValue();
      if (argName.equals("command"))
        command = argValue;
      else if (argName.equals("file"))
        inFilePath = argValue;
      else if (argName.equals("name"))
        userFriendlyName = argValue;
      else if (argName.equals("first"))
        firstPage = Integer.parseInt(argValue);
      else if (argName.equals("last"))
        lastPage = Integer.parseInt(argValue);
      else if (argName.equals("pages")) {
        final String WITH_DELIMITER = "((?<=%1$s)|(?=%1$s))";
        final Pattern numberPattern = Pattern.compile("\\d+");
        final String[] parts = argValue.split(String.format(WITH_DELIMITER, "[\\-,]"));
        int number = -1;
        boolean inRange = false;
        final Set<Integer> myPages = new HashSet<>();
        for (String part : parts) {
          if (numberPattern.matcher(part).matches()) {
            int lowerBound = number;
            number = Integer.parseInt(part);
            if (inRange) {
              if (lowerBound > number)
                throw new IllegalArgumentException(
                    "Lower bound (" + lowerBound + ") greater than upper bound (" + number + "): " + argValue);
              IntStream.rangeClosed(lowerBound, number).forEach(i -> myPages.add(i));
              number = -1;
              inRange = false;
            }
          } else if (part.equals(",")) {
            if (number >= 0)
              myPages.add(number);
            number = -1;
          } else if (part.equals("-")) {
            if (inRange)
              throw new IllegalArgumentException("Unable to parse pages (unclosed range): " + argValue);
            if (number < 0)
              throw new IllegalArgumentException("Range without lower bound: " + argValue);

            inRange = true;
          } else {
            throw new IllegalArgumentException(
                "Unable to parse pages - unexpected character '" + part + "': " + argValue);
          }
        }
        if (inRange) {
          throw new IllegalArgumentException("Unable to parse pages (unclosed range): " + argValue);
        }
        if (number >= 0)
          myPages.add(number);

        pages = myPages;
      } else if (argName.equals("inDir"))
        inDirPath = argValue;
      else if (argName.equals("outDir"))
        outputDirPath = argValue;
      else if (argName.equals("outputFile"))
        outputFilePath = argValue;
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
      else if (argName.equals("multiplier"))
        multiplier = Integer.parseInt(argValue);
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
        documentSet = new HashSet<>();
        for (String docIdString : docIdArray) {
          int oneId = Integer.parseInt(docIdString);
          documentSet.add(oneId);
        }
      } else if (argName.equals("docSelection")) {
        docSelectionPath = argValue;
      } else if (argName.equals("docGroupFile"))
        docGroupPath = argValue;
      else if (argName.equals("suffix"))
        suffix = argValue;
      else if (argName.equals("includeBeam"))
        includeBeam = argValue.equalsIgnoreCase("true");
      else if (argName.equals("outputFormat")) {
        outputFormats = new ArrayList<>();
        String[] outputFormatStrings = argValue.split(",");
        for (String outputFormatString : outputFormatStrings) {
          outputFormats.add(OutputFormat.valueOf(outputFormatString));
        }
        if (outputFormats.size() == 0)
          throw new JochreException("At least one outputFormat required.");
      } else if (argName.equals("features")) {
        featureDescriptors = new ArrayList<>();
        InputStream featureFile = new FileInputStream(new File(argValue));
        try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(featureFile, "UTF-8")))) {
          while (scanner.hasNextLine()) {
            String descriptor = scanner.nextLine();
            featureDescriptors.add(descriptor);
            LOG.debug(descriptor);
          }
        }
      } else if (argName.equals("includeDate")) {
        includeDate = argValue.equalsIgnoreCase("true");
      } else {
        throw new RuntimeException("Unknown argument: " + argName);
      }
    }

    if (pages.isEmpty() && (firstPage >= 0 || lastPage >= 0)) {
      if (firstPage < 0)
        firstPage = 0;
      if (lastPage < 0)
        lastPage = config.getInt("jochre.pdf.max-page");
      pages = IntStream.rangeClosed(firstPage, lastPage).boxed().collect(Collectors.toSet());
    }

    long startTime = System.currentTimeMillis();
    try {

      this.setUserId(userId);

      CorpusSelectionCriteria criteria = new CorpusSelectionCriteria();
      if (docSelectionPath != null) {
        File docSelectionFile = new File(docSelectionPath);
        Scanner scanner = new Scanner(new BufferedReader(
            new InputStreamReader(new FileInputStream(docSelectionFile), jochreSession.getEncoding())));
        criteria.loadSelection(scanner);
        scanner.close();
      } else {
        criteria.setImageId(imageId);
        criteria.setImageCount(imageCount);
        if (imageSet != null)
          criteria.setImageStatusesToInclude(imageSet);
        criteria.setExcludeImageId(excludeImageId);
        criteria.setCrossValidationSize(crossValidationSize);
        criteria.setIncludeIndex(includeIndex);
        criteria.setExcludeIndex(excludeIndex);
        criteria.setDocumentId(docId);
        criteria.setDocumentIds(documentSet);
      }
      if (LOG.isDebugEnabled())
        LOG.debug(criteria.getAttributes().toString());

      if (docGroupPath != null) {
        File docGroupFile = new File(docGroupPath);
        Scanner scanner = new Scanner(
            new BufferedReader(new InputStreamReader(new FileInputStream(docGroupFile), jochreSession.getEncoding())));

        while (scanner.hasNextLine()) {
          String line = scanner.nextLine();
          int equalsPos = line.indexOf('=');
          String groupName = line.substring(0, equalsPos);
          String[] ids = line.substring(equalsPos + 1).split(",");
          Set<Integer> idSet = new HashSet<>();
          for (String idString : ids) {
            idSet.add(Integer.parseInt(idString));
          }
          documentGroups.put(groupName, idSet);
        }
        scanner.close();
      }

      MostLikelyWordChooser wordChooser = new MostLikelyWordChooser(jochreSession);

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
      List<PdfImageObserver> imageObservers = null;
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

        observers = this.getObservers(outputFormats, baseName, outputDir, includeDate);
        imageObservers = this.getImageObservers(outputFormats, baseName, outputDir);
      }

      if (userFriendlyName.length() == 0)
        userFriendlyName = inFilePath;

      if (command.equals("segment")) {
        this.doCommandSegment(inFilePath, userFriendlyName, outputDir, save, pages);
      } else if (command.equals("extract")) {
        this.doCommandExtractImages(inFilePath, outputDir, pages);
      } else if (command.equals("updateImages")) {
        this.doCommandUpdateImages(inFilePath, docId, pages);
      } else if (command.equals("applyFeatures")) {
        this.doCommandApplyFeatures(imageId, shapeId, featureDescriptors);
      } else if (command.equals("train")) {
        this.doCommandTrain(featureDescriptors, criteria, reconstructLetters);
      } else if (command.equals("evaluate") || command.equals("evaluateComplex")) {
        this.doCommandEvaluate(criteria, outputDir, wordChooser, reconstructLetters, save, suffix, includeBeam,
            observers);
      } else if (command.equals("evaluateFull")) {
        this.doCommandEvaluateFull(criteria, save, outputDir, wordChooser, suffix, observers);
      } else if (command.equals("analyse")) {
        this.doCommandAnalyse(criteria, wordChooser, observers);
      } else if (command.equals("transform")) {
        this.doCommandTransform(criteria, observers, imageObservers);
      } else if (command.equals("trainSplits")) {
        this.doCommandTrainSplits(featureDescriptors, criteria);
      } else if (command.equals("evaluateSplits")) {
        this.doCommandEvaluateSplits(criteria);
      } else if (command.equals("trainMerge")) {
        this.doCommandTrainMerge(featureDescriptors, multiplier, criteria);
      } else if (command.equals("evaluateMerge")) {
        this.doCommandEvaluateMerge(criteria);
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

            TextFileLexicon lexicon = new TextFileLexicon(oneLexFile, jochreSession.getEncoding());

            String baseName = oneLexFile.getName().substring(0, oneLexFile.getName().indexOf("."));
            if (baseName.lastIndexOf("/") > 0)
              baseName = baseName.substring(baseName.lastIndexOf("/") + 1);

            File lexiconFile = new File(outputDir, baseName + ".obj");
            lexicon.serialize(lexiconFile);
          }
        } else {
          LOG.debug(inFilePath + ": " + inputFile.exists() + ", size: " + inputFile.length());

          TextFileLexicon lexicon = new TextFileLexicon(inputFile, jochreSession.getEncoding());

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
            List<DocumentObserver> pdfObservers = this.getObservers(outputFormats, baseName, analysisDir, includeDate);
            List<PdfImageObserver> pdfImageObservers = this.getImageObservers(outputFormats, baseName, analysisDir);
            this.doCommandAnalyse(pdfFile, wordChooser, pages, pdfObservers, pdfImageObservers);

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
        this.doCommandAnalyse(pdfFile, wordChooser, pages, observers, imageObservers);
      } else if (command.equals("findSplits")) {
        GraphicsDao graphicsDao = GraphicsDao.getInstance(jochreSession);
        List<Shape> shapesToSplit = graphicsDao.findShapesToSplit(jochreSession.getLocale());
        for (Shape shape : shapesToSplit) {
          LOG.info(shape.toString());
        }
      } else {
        throw new RuntimeException("Unknown command: " + command);
      }
    } catch (Exception e) {
      LOG.error("An error occurred while running Jochre", e);
      throw e;
    } finally {
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
    GraphicsDao graphicsDao = GraphicsDao.getInstance(jochreSession);
    if (shapeId > 0) {
      Shape shape = graphicsDao.loadShape(shapeId);
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
      List<Integer> shapeIds = graphicsDao.findShapeIds("—");
      Map<Object, Integer> outcomeMap = new HashMap<>();
      for (int oneShapeId : shapeIds) {
        Shape shape = graphicsDao.loadShape(oneShapeId);
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
  public void doCommandBuildLexicon(File outputDir, CorpusSelectionCriteria criteria) {
    try {
      CorpusLexiconBuilder builder = new CorpusLexiconBuilder(criteria, jochreSession);
      TextFileLexicon lexicon = builder.buildLexicon();
      
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
   * Log a shape's image to the log file, to make sure it got segmented and stored
   * correctly.
   */
  public void doCommandLogImage(int shapeId) {
    // just a utility for making sure images got segmented and stored
    // correctly
    if (shapeId > 0) {
      GraphicsDao graphicsDao = GraphicsDao.getInstance(jochreSession);
      Shape shape = graphicsDao.loadShape(shapeId);
      shape.writeImageToLog();
      LOG.debug("Letter: " + shape.getLetter());
    }
  }

  /**
   * Train the letter merging model.
   * 
   * @param featureDescriptors
   *          feature descriptors for training
   * @param multiplier
   *          if &gt; 0, will be used to equalize the outcomes
   * @param criteria
   *          the criteria used to select the training corpus
   */
  public void doCommandTrainMerge(List<String> featureDescriptors, int multiplier, CorpusSelectionCriteria criteria) {
    if (jochreSession.getMergeModelPath() == null)
      throw new RuntimeException("Missing argument: mergeModel");

    if (featureDescriptors == null)
      throw new JochreException("features is required");

    File mergeModelFile = new File(jochreSession.getMergeModelPath());
    mergeModelFile.getParentFile().mkdirs();

    MergeFeatureParser mergeFeatureParser = new MergeFeatureParser();
    Set<MergeFeature<?>> mergeFeatures = mergeFeatureParser.getMergeFeatureSet(featureDescriptors);
    ClassificationEventStream corpusEventStream = new JochreMergeEventStream(criteria, mergeFeatures, jochreSession);
    if (multiplier > 0) {
      corpusEventStream = new OutcomeEqualiserEventStream(corpusEventStream, multiplier);
    }

    ModelTrainerFactory modelTrainerFactory = new ModelTrainerFactory();
    ClassificationModelTrainer trainer = modelTrainerFactory.constructTrainer(jochreSession.getConfig());

    ClassificationModel mergeModel = trainer.trainModel(corpusEventStream, featureDescriptors);
    mergeModel.persist(mergeModelFile);
  }

  /**
   * Evaluate the letter merging model on its own.
   * 
   * @param criteria
   *          for selecting the portion of the corpus to evaluate
   */
  public void doCommandEvaluateMerge(CorpusSelectionCriteria criteria) throws IOException {
    ClassificationModel mergeModel = jochreSession.getMergeModel();
    if (mergeModel == null)
      throw new IllegalArgumentException("Missing parameter: jochre.image-analyser.merge-model");

    List<String> mergeFeatureDescriptors = mergeModel.getFeatureDescriptors();
    MergeFeatureParser mergeFeatureParser = new MergeFeatureParser();

    Set<MergeFeature<?>> mergeFeatures = mergeFeatureParser.getMergeFeatureSet(mergeFeatureDescriptors);

    JochreCorpusGroupReader groupReader = new JochreCorpusGroupReader(jochreSession);
    groupReader.setSelectionCriteria(criteria);

    ShapeMerger merger = new ShapeMerger(mergeFeatures, mergeModel.getDecisionMaker());
    MergeEvaluator evaluator = new MergeEvaluator(jochreSession);
    FScoreCalculator<String> fScoreCalculator = evaluator.evaluate(groupReader, merger);
    LOG.debug("" + fScoreCalculator.getTotalFScore());
  }

  /**
   * Train the letter splitting model.
   * 
   * @param featureDescriptors
   *          the feature descriptors for training this model
   * @param criteria
   *          the criteria used to select the training corpus
   */
  public void doCommandTrainSplits(List<String> featureDescriptors, CorpusSelectionCriteria criteria) {
    if (jochreSession.getSplitModelPath() == null)
      throw new RuntimeException("Missing argument: splitModel");
    if (featureDescriptors == null)
      throw new JochreException("features is required");

    File splitModelFile = new File(jochreSession.getSplitModelPath());
    splitModelFile.getParentFile().mkdirs();

    SplitFeatureParser splitFeatureParser = new SplitFeatureParser();
    Set<SplitFeature<?>> splitFeatures = splitFeatureParser.getSplitFeatureSet(featureDescriptors);

    ClassificationEventStream corpusEventStream = new JochreSplitEventStream(criteria, splitFeatures, jochreSession);

    ModelTrainerFactory modelTrainerFactory = new ModelTrainerFactory();
    ClassificationModelTrainer trainer = modelTrainerFactory.constructTrainer(jochreSession.getConfig());

    ClassificationModel splitModel = trainer.trainModel(corpusEventStream, featureDescriptors);
    splitModel.persist(splitModelFile);
  }

  /**
   * Evaluate the letter splitting model on its own.
   * 
   * @param criteria
   *          the criteria used to select the evaluation corpus
   */
  public void doCommandEvaluateSplits(CorpusSelectionCriteria criteria) throws IOException {
    ClassificationModel splitModel = jochreSession.getSplitModel();
    if (splitModel == null)
      throw new IllegalArgumentException("Missing parameter: jochre.image-analyser.split-model");

    List<String> splitFeatureDescriptors = splitModel.getFeatureDescriptors();
    SplitFeatureParser splitFeatureParser = new SplitFeatureParser();
    Set<SplitFeature<?>> splitFeatures = splitFeatureParser.getSplitFeatureSet(splitFeatureDescriptors);

    SplitCandidateFinder splitCandidateFinder = new SplitCandidateFinder(jochreSession);
    splitCandidateFinder.setMinDistanceBetweenSplits(5);

    ShapeSplitter shapeSplitter = new RecursiveShapeSplitter(splitCandidateFinder, splitFeatures,
        splitModel.getDecisionMaker(), jochreSession);

    JochreCorpusShapeReader shapeReader = new JochreCorpusShapeReader(jochreSession);
    shapeReader.setSelectionCriteria(criteria);

    SplitEvaluator splitEvaluator = new SplitEvaluator(jochreSession);

    FScoreCalculator<String> fScoreCalculator = splitEvaluator.evaluate(shapeReader, shapeSplitter);
    LOG.debug("" + fScoreCalculator.getTotalFScore());
  }

  /**
   * Train a letter guessing model.
   * 
   * @param featureDescriptors
   *          the feature descriptors for training
   * @param criteria
   *          criteria for selecting images to include when training
   * @param reconstructLetters
   *          whether or not complete letters should be reconstructed for
   *          training, from merged/split letters
   */
  public void doCommandTrain(List<String> featureDescriptors, CorpusSelectionCriteria criteria,
      boolean reconstructLetters) {
    if (jochreSession.getLetterModelPath() == null)
      throw new RuntimeException("Missing argument: letterModel");
    if (featureDescriptors == null)
      throw new JochreException("features is required");

    LetterFeatureParser letterFeatureParser = new LetterFeatureParser();
    Set<LetterFeature<?>> features = letterFeatureParser.getLetterFeatureSet(featureDescriptors);

    BoundaryDetector boundaryDetector = null;
    if (reconstructLetters) {
      ShapeSplitter splitter = new TrainingCorpusShapeSplitter(jochreSession);
      ShapeMerger merger = new TrainingCorpusShapeMerger();
      boundaryDetector = new LetterByLetterBoundaryDetector(splitter, merger, jochreSession);
    } else {
      boundaryDetector = new OriginalBoundaryDetector();
    }

    LetterValidator letterValidator = new ComponentCharacterValidator(jochreSession);

    ClassificationEventStream corpusEventStream = new JochreLetterEventStream(features, boundaryDetector,
        letterValidator, criteria, jochreSession);

    File letterModelFile = new File(jochreSession.getLetterModelPath());
    letterModelFile.getParentFile().mkdirs();

    ModelTrainerFactory modelTrainerFactory = new ModelTrainerFactory();
    ClassificationModelTrainer trainer = modelTrainerFactory.constructTrainer(jochreSession.getConfig());

    ClassificationModel letterModel = trainer.trainModel(corpusEventStream, featureDescriptors);
    letterModel.persist(letterModelFile);
  }

  /**
   * Evaluate a given letter guessing model.
   *  @param criteria
   *          the criteria used to select the evaluation corpus
   */
  public void doCommandEvaluate(CorpusSelectionCriteria criteria, File outputDir,
                                MostLikelyWordChooser wordChooser, boolean reconstructLetters, boolean save, String suffix, boolean includeBeam,
                                List<DocumentObserver> observers) throws IOException {
    ClassificationModel letterModel = jochreSession.getLetterModel();

    List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
    LetterFeatureParser letterFeatureParser = new LetterFeatureParser();
    Set<LetterFeature<?>> letterFeatures = letterFeatureParser.getLetterFeatureSet(letterFeatureDescriptors);

    LetterGuesser letterGuesser = new LetterGuesser(letterFeatures, letterModel.getDecisionMaker());

    String baseName = jochreSession.getLetterModelPath().substring(0, jochreSession.getLetterModelPath().indexOf("."));
    if (baseName.lastIndexOf("/") > 0)
      baseName = baseName.substring(baseName.lastIndexOf("/") + 1);
    baseName += suffix;

    BoundaryDetector boundaryDetector = null;
    if (reconstructLetters) {
      ShapeSplitter splitter = new TrainingCorpusShapeSplitter(jochreSession);
      ShapeMerger merger = new TrainingCorpusShapeMerger();
      boundaryDetector = new LetterByLetterBoundaryDetector(splitter, merger, jochreSession);
    } else {
      boundaryDetector = new OriginalBoundaryDetector();
    }

    ImageAnalyser evaluator = new BeamSearchImageAnalyser(boundaryDetector, letterGuesser, wordChooser, jochreSession);

    FScoreObserver fScoreObserver = null;
    LetterValidator letterValidator = new ComponentCharacterValidator(jochreSession);
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

      fScoreObserver = new SimpleLetterFScoreObserver(letterValidator, jochreSession);
    }

    evaluator.addObserver(fScoreObserver);

    ErrorLogger errorLogger = new ErrorLogger(jochreSession);
    Writer errorWriter = null;

    File errorFile = new File(outputDir, baseName + "_errors.txt");
    errorFile.delete();
    errorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile, true), "UTF8"));

    errorLogger.setErrorWriter(errorWriter);
    evaluator.addObserver(errorLogger);

    LexiconErrorWriter lexiconErrorWriter = new LexiconErrorWriter(outputDir, baseName, wordChooser, jochreSession);
    if (documentGroups != null)
      lexiconErrorWriter.setDocumentGroups(documentGroups);
    lexiconErrorWriter.setIncludeBeam(includeBeam);

    // find all document names (alphabetical ordering)
    Set<String> documentNameSet = new TreeSet<>();
    JochreCorpusImageReader imageReader1 = new JochreCorpusImageReader(jochreSession);
    CorpusSelectionCriteria docCriteria = new CorpusSelectionCriteria();
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
    List<String> documentNames = new ArrayList<>(documentNameSet);
    lexiconErrorWriter.setDocumentNames(documentNames);

    evaluator.addObserver(lexiconErrorWriter);

    JochreCorpusImageProcessor imageProcessor = new JochreCorpusImageProcessor(criteria, jochreSession);
    imageProcessor.addObserver(evaluator);
    for (DocumentObserver observer : observers)
      imageProcessor.addObserver(observer);

    try {
      imageProcessor.process();
    } finally {
      if (errorWriter != null)
        errorWriter.close();
    }
    LOG.debug("F-score for " + jochreSession.getLetterModelPath() + ": "
        + fScoreObserver.getFScoreCalculator().getTotalFScore());

    String modelFileName = baseName;
    if (reconstructLetters)
      modelFileName += "_Reconstruct";

    File fscoreFile = new File(outputDir, modelFileName + "_fscores.csv");
    Writer fscoreWriter = errorWriter = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(fscoreFile, true), jochreSession.getCsvEncoding()));
    fScoreObserver.getFScoreCalculator().writeScoresToCSV(fscoreWriter);

  }

  /**
   * Analyse a set of images based on a given letter-guessing model.
   * 
   * @param criteria
   *          the criteria used to select the documents to be analysed
   * @param wordChooser
   *          the word chooser to use
   * @param observers
   *          the observers, used to create analysis output
   */
  public void doCommandAnalyse(CorpusSelectionCriteria criteria, MostLikelyWordChooser wordChooser,
      List<DocumentObserver> observers) throws IOException {
    ClassificationModel letterModel = jochreSession.getLetterModel();

    List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
    LetterFeatureParser letterFeatureParser = new LetterFeatureParser();
    Set<LetterFeature<?>> letterFeatures = letterFeatureParser.getLetterFeatureSet(letterFeatureDescriptors);

    LetterGuesser letterGuesser = new LetterGuesser(letterFeatures, letterModel.getDecisionMaker());

    ImageAnalyser analyser = new BeamSearchImageAnalyser(null, letterGuesser, wordChooser, jochreSession);
    LetterAssigner letterAssigner = new LetterAssigner();
    analyser.addObserver(letterAssigner);

    JochreCorpusImageProcessor imageProcessor = new JochreCorpusImageProcessor(criteria, jochreSession);
    imageProcessor.addObserver(analyser);
    for (DocumentObserver observer : observers)
      imageProcessor.addObserver(observer);

    imageProcessor.process();
  }

  /**
   * Transform stuff in the corpus into a given output format.
   *  @param criteria
   *          the criteria used to select the documents to be analysed
   * @param observers
   * @param imageObservers
   */
  public void doCommandTransform(CorpusSelectionCriteria criteria, List<DocumentObserver> observers, List<PdfImageObserver> imageObservers)
      throws IOException {
    JochreCorpusImageProcessor imageProcessor = new JochreCorpusImageProcessor(criteria, jochreSession);
    for (DocumentObserver observer : observers)
      imageProcessor.addObserver(observer);

    imageProcessor.process();
  }

  /**
   * Full analysis, including merge, split and letter guessing.
   * 
   * @param pages
   *          the pages to process, empty means all
   */
  public void doCommandAnalyse(File sourceFile, MostLikelyWordChooser wordChooser, Set<Integer> pages,
      List<DocumentObserver> observers, List<PdfImageObserver> imageObservers) throws IOException {

    JochreDocumentGenerator documentGenerator = this.getDocumentGenerator(sourceFile.getName(), wordChooser, pages, observers, imageObservers);

    if (!sourceFile.exists())
      throw new JochreException("The file " + sourceFile.getPath() + " does not exist");

    if (sourceFile.getName().toLowerCase().endsWith(".pdf")) {
      PdfDocumentProcessor pdfDocumentProcessor = new PdfDocumentProcessor(sourceFile, pages, documentGenerator);
      for (PdfImageObserver imageObserver : imageObservers) {
        pdfDocumentProcessor.addImageObserver(imageObserver);
      }
      pdfDocumentProcessor.process();
    } else if (sourceFile.getName().toLowerCase().endsWith(".png")
        || sourceFile.getName().toLowerCase().endsWith(".jpg") || sourceFile.getName().toLowerCase().endsWith(".jpeg")
        || sourceFile.getName().toLowerCase().endsWith(".gif") || sourceFile.getName().toLowerCase().endsWith(".tif")
        || sourceFile.getName().toLowerCase().endsWith(".tiff")) {
      ImageDocumentExtractor extractor = new ImageDocumentExtractor(sourceFile, documentGenerator);
      extractor.extractDocument();
    } else if (sourceFile.isDirectory()) {
      ImageDocumentExtractor extractor = new ImageDocumentExtractor(sourceFile, documentGenerator);
      extractor.extractDocument();
    } else {
      throw new RuntimeException("Unrecognised file extension");
    }
  }

  private JochreDocumentGenerator getDocumentGenerator(String fileName, MostLikelyWordChooser wordChooser, Set<Integer> pages,
                                List<DocumentObserver> observers, List<PdfImageObserver> imageObservers) throws IOException {

    ClassificationModel letterModel = jochreSession.getLetterModel();

    List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
    LetterFeatureParser letterFeatureParser = new LetterFeatureParser();
    Set<LetterFeature<?>> letterFeatures = letterFeatureParser.getLetterFeatureSet(letterFeatureDescriptors);
    LetterGuesser letterGuesser = new LetterGuesser(letterFeatures, letterModel.getDecisionMaker());
    BoundaryDetector boundaryDetector = null;
    LetterGuessObserver letterGuessObserver = null;

    if (jochreSession.getSplitModel() != null && jochreSession.getMergeModel() != null) {
      boundaryDetector = new DeterministicBoundaryDetector(jochreSession.getSplitModel(), jochreSession.getMergeModel(),
          jochreSession);

      OriginalShapeLetterAssigner shapeLetterAssigner = new OriginalShapeLetterAssigner();
      shapeLetterAssigner.setEvaluate(false);
      shapeLetterAssigner.setSingleLetterMethod(false);

      letterGuessObserver = shapeLetterAssigner;
    } else {
      boundaryDetector = new OriginalBoundaryDetector();

      LetterAssigner letterAssigner = new LetterAssigner();
      letterGuessObserver = letterAssigner;
    }

    ImageAnalyser analyser = new BeamSearchImageAnalyser(boundaryDetector, letterGuesser, wordChooser, jochreSession);
    analyser.addObserver(letterGuessObserver);

    JochreDocumentGenerator documentGenerator = new JochreDocumentGenerator(fileName, "", jochreSession);
    documentGenerator.addDocumentObserver(analyser);

    for (DocumentObserver observer : observers)
      documentGenerator.addDocumentObserver(observer);

    return documentGenerator;
  }

  /**
   * Evaluate a suite of split/merge models and letter guessing model.
   *  @param criteria
   *          for selecting the evaluation corpus
   * @param save
   *          whether or not the letter guesses should be saved
   */
  public void doCommandEvaluateFull(CorpusSelectionCriteria criteria, boolean save, File outputDir,
                                    MostLikelyWordChooser wordChooser, String suffix, List<DocumentObserver> observers) throws IOException {
    String baseName = jochreSession.getLetterModelPath().substring(0, jochreSession.getLetterModelPath().indexOf("."));
    if (baseName.lastIndexOf("/") > 0)
      baseName = baseName.substring(baseName.lastIndexOf("/") + 1);

    ClassificationModel letterModel = jochreSession.getLetterModel();
    List<String> letterFeatureDescriptors = letterModel.getFeatureDescriptors();
    LetterFeatureParser letterFeatureParser = new LetterFeatureParser();
    Set<LetterFeature<?>> letterFeatures = letterFeatureParser.getLetterFeatureSet(letterFeatureDescriptors);

    LetterGuesser letterGuesser = new LetterGuesser(letterFeatures, letterModel.getDecisionMaker());

    ClassificationModel splitModel = jochreSession.getSplitModel();
    if (splitModel == null)
      throw new IllegalArgumentException("Missing parameter: jochre.image-analyser.split-model");

    List<String> splitFeatureDescriptors = splitModel.getFeatureDescriptors();
    SplitFeatureParser splitFeatureParser = new SplitFeatureParser();
    Set<SplitFeature<?>> splitFeatures = splitFeatureParser.getSplitFeatureSet(splitFeatureDescriptors);

    SplitCandidateFinder splitCandidateFinder = new SplitCandidateFinder(jochreSession);
    splitCandidateFinder.setMinDistanceBetweenSplits(5);

    ShapeSplitter shapeSplitter = new RecursiveShapeSplitter(splitCandidateFinder, splitFeatures,
        splitModel.getDecisionMaker(), jochreSession);

    ClassificationModel mergeModel = jochreSession.getMergeModel();
    if (mergeModel == null)
      throw new IllegalArgumentException("Missing parameter: jochre.image-analyser.merge-model");

    List<String> mergeFeatureDescriptors = mergeModel.getFeatureDescriptors();
    MergeFeatureParser mergeFeatureParser = new MergeFeatureParser();
    Set<MergeFeature<?>> mergeFeatures = mergeFeatureParser.getMergeFeatureSet(mergeFeatureDescriptors);

    ShapeMerger shapeMerger = new ShapeMerger(mergeFeatures, mergeModel.getDecisionMaker());

    BoundaryDetector boundaryDetector = null;
    String boundaryDetectorTypeName = jochreSession.getConfig().getConfig("jochre.boundaries")
        .getString("boundary-detector-type");
    BoundaryDetectorType boundaryDetectorType = BoundaryDetectorType.valueOf(boundaryDetectorTypeName);
    switch (boundaryDetectorType) {
    case LetterByLetter:
      boundaryDetector = new LetterByLetterBoundaryDetector(shapeSplitter, shapeMerger, jochreSession);
      break;
    case Deterministic:
      boundaryDetector = new DeterministicBoundaryDetector(shapeSplitter, shapeMerger, jochreSession);
      break;
    }

    ImageAnalyser imageAnalyser = new BeamSearchImageAnalyser(boundaryDetector, letterGuesser, wordChooser,
        jochreSession);

    LetterValidator letterValidator = new ComponentCharacterValidator(jochreSession);

    OriginalShapeLetterAssigner shapeLetterAssigner = new OriginalShapeLetterAssigner();
    shapeLetterAssigner.setEvaluate(true);
    shapeLetterAssigner.setSave(save);
    shapeLetterAssigner.setLetterValidator(letterValidator);
    shapeLetterAssigner.setSingleLetterMethod(false);
    imageAnalyser.addObserver(shapeLetterAssigner);

    ErrorLogger errorLogger = new ErrorLogger(jochreSession);
    Writer errorWriter = null;

    File errorFile = new File(outputDir, baseName + suffix + "errors.txt");
    errorFile.delete();
    errorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile, true), "UTF8"));

    errorLogger.setErrorWriter(errorWriter);
    imageAnalyser.addObserver(errorLogger);

    JochreCorpusImageProcessor imageProcessor = new JochreCorpusImageProcessor(criteria, jochreSession);
    imageProcessor.addObserver(imageAnalyser);
    for (DocumentObserver observer : observers)
      imageProcessor.addObserver(observer);
    imageProcessor.process();

    LOG.debug("F-score for " + jochreSession.getLetterModelPath() + ": "
        + shapeLetterAssigner.getFScoreCalculator().getTotalFScore());

    String modelFileName = baseName + suffix + "_full";

    File fscoreFile = new File(outputDir, modelFileName + "_fscores.csv");
    Writer fscoreWriter = errorWriter = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(fscoreFile, true), jochreSession.getCsvEncoding()));
    shapeLetterAssigner.getFScoreCalculator().writeScoresToCSV(fscoreWriter);
  }

  /**
   * Apply a set of features to a given image or a given shape.
   */
  public void doCommandApplyFeatures(int imageId, int shapeId, List<String> featureDescriptors) {
    LetterFeatureTester featureTester = new LetterFeatureTester(jochreSession);

    LetterFeatureParser letterFeatureParser = new LetterFeatureParser();
    Set<LetterFeature<?>> features = letterFeatureParser.getLetterFeatureSet(featureDescriptors);
    Set<String> letters = new HashSet<>();

    featureTester.applyFeatures(features, letters, imageId, shapeId);
  }

  /**
   * Update the images in an existing Jochre document.
   */
  public void doCommandUpdateImages(String filename, int docId, Set<Integer> pages) {
    if (filename.length() == 0)
      throw new RuntimeException("Missing argument: file");
    if (docId < 0)
      throw new RuntimeException("Missing argument: docId");

    DocumentDao documentDao = DocumentDao.getInstance(jochreSession);
    JochreDocument doc = documentDao.loadJochreDocument(docId);
    if (filename.toLowerCase().endsWith(".pdf")) {
      File pdfFile = new File(filename);
      PdfDocumentProcessor pdfDocumentProcessor = new PdfDocumentProcessor(pdfFile, pages, new PdfImageUpdater(doc));
      pdfDocumentProcessor.process();
    } else {
      throw new RuntimeException("Unrecognised file extension");
    }
  }

  /**
   * Extract the images from a PDF file.
   * @param filename
   *          the path to the PDF file
   * @param pages
   */
  public void doCommandExtractImages(String filename, File outputDir, Set<Integer> pages) {
    if (filename.length() == 0)
      throw new RuntimeException("Missing argument: file");

    if (filename.toLowerCase().endsWith(".pdf")) {
      File pdfFile = new File(filename);
      String baseName = this.getBaseName(pdfFile);
      List<PdfImageObserver> imageObservers = this.getImageObservers(Arrays.asList(OutputFormat.ImageExtractor), baseName, outputDir);
      PdfImageVisitor pdfImageVisitor = new PdfImageVisitor(pdfFile, pages);
      for (PdfImageObserver imageObserver : imageObservers) {
        pdfImageVisitor.addImageObserver(imageObserver);
      }
      pdfImageVisitor.visitImages();
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
   * @param save
   *          should we save this file to the database?
   * @param pages
   *          the pages to process, empty means all
   */
  public void doCommandSegment(String filename, String userFriendlyName, File outputDir, boolean save,
      Set<Integer> pages) {

    if (filename.length() == 0)
      throw new RuntimeException("Missing argument: file");
    if (userId < 0 && save)
      throw new RuntimeException("Missing argument (for save=true): userId");

    User user = null;
    if (userId >= 0) {
      SecurityDao securityDao = SecurityDao.getInstance(jochreSession);

      user = securityDao.loadUser(userId);
    }

    File file = new File(filename);
    JochreDocumentGenerator jochreDocumentGenerator = new JochreDocumentGenerator(file.getName(), userFriendlyName,
        jochreSession);
    if (save)
      jochreDocumentGenerator.requestSave(user);
    if (jochreDocumentGenerator.isDrawSegmentedImage()) {
      if (outputDir != null) {
        jochreDocumentGenerator.requestSegmentation(outputDir);
      }
    }

    if (filename.toLowerCase().endsWith(".pdf")) {
      PdfDocumentProcessor pdfDocumentProcessor = new PdfDocumentProcessor(file, pages, jochreDocumentGenerator);
      pdfDocumentProcessor.process();
    } else if (filename.toLowerCase().endsWith(".png") || filename.toLowerCase().endsWith(".jpg")
        || filename.toLowerCase().endsWith(".jpeg") || filename.toLowerCase().endsWith(".gif")) {
      ImageDocumentExtractor extractor = new ImageDocumentExtractor(file, jochreDocumentGenerator);
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
    public void onAnalysisComplete() {
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

  public List<DocumentObserver> getObservers(List<OutputFormat> outputFormats, String baseName, File outputDir,
      boolean includeDate) {
    try {
      List<DocumentObserver> observers = new ArrayList<>();

      for (OutputFormat outputFormat : outputFormats) {
        switch (outputFormat) {
        case AbbyyFineReader8: {
          AbbyyFineReader8Exporter observer = new AbbyyFineReader8Exporter(outputDir);
          observer.setBaseName(baseName);
          observer.setIncludeDate(includeDate);
          observers.add(observer);
          break;
        }
        case Alto3:
        case Alto3zip: {
          boolean zipped = (outputFormat == OutputFormat.Alto3zip);
          AltoXMLExporter observer = new AltoXMLExporter(outputDir, zipped, 3);
          observer.setBaseName(baseName);
          observer.setIncludeDate(includeDate);
          observers.add(observer);
          break;
        }
        case Alto4:
        case Alto4zip: {
          boolean zipped = (outputFormat == OutputFormat.Alto3zip);
          AltoXMLExporter observer = new AltoXMLExporter(outputDir, zipped, 4);
          observer.setBaseName(baseName);
          observer.setIncludeDate(includeDate);
          observers.add(observer);
          break;
        }
        case HTML: {
          TextGetter textGetter = new TextGetter(outputDir, TextFormat.XHTML, jochreSession.getLexicon());
          textGetter.setBaseName(baseName);
          textGetter.setIncludeDate(includeDate);
          observers.add(textGetter);

          break;
        }
        case Text: {
          TextGetter textGetter = new TextGetter(outputDir, TextFormat.PLAIN, jochreSession.getLexicon());
          textGetter.setBaseName(baseName);
          textGetter.setIncludeDate(includeDate);
          observers.add(textGetter);
          break;
        }
        case GuessText: {
          TextExporter observer = new TextExporter(outputDir);
          observer.setBaseName(baseName);
          observer.setIncludeDate(includeDate);
          observers.add(observer);
          break;
        }
        case Jochre: {
          JochreXMLExporter observer = new JochreXMLExporter(outputDir);
          observer.setBaseName(baseName);
          observer.setIncludeDate(includeDate);
          observers.add(observer);
          break;
        }
        case JochrePageByPage: {
          outputDir.mkdirs();
          File zipFile = new File(outputDir, baseName + "_jochre.zip");

          DocumentObserver observer = new JochrePageByPageExporter(zipFile, baseName);
          observers.add(observer);
          break;
        }
        case Metadata: {
          DocumentObserver observer = new MetaDataExporter(outputDir, baseName);
          observers.add(observer);
          break;
        }
        case UnknownWords: {
          File unknownWordFile = new File(outputDir, baseName + "_unknownWords.txt");
          unknownWordFile.delete();
          Writer unknownWordWriter = new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream(unknownWordFile, true), "UTF8"));

          UnknownWordListWriter unknownWordListWriter = new UnknownWordListWriter(unknownWordWriter);
          observers.add(unknownWordListWriter);
          break;
        }
        default: {
          // do nothing
        }
        }
      }
      return observers;
    } catch (IOException e) {
      LOG.error("Couldn't configure observers", e);
      throw new RuntimeException(e);
    }
  }

  public List<PdfImageObserver> getImageObservers(List<OutputFormat> outputFormats, String baseName, File outputDir) {
    List<PdfImageObserver> imageObservers = new ArrayList<>();

    for (OutputFormat outputFormat : outputFormats) {
      switch (outputFormat) {
        case ImageExtractor: {
          PdfImageObserver imageObserver = new PdfImageSaver(baseName, outputDir);
          imageObservers.add(imageObserver);
          break;
        }
        default: {
          // do nothing
        }
      }
    }
    return imageObservers;
  }

  public JochreSession getJochreSession() {
    return jochreSession;
  }

  public void imageFileToAlto4(File sourceFile, Writer writer) throws IOException {
    final Set<Integer> myPages = new HashSet<>();
    MostLikelyWordChooser wordChooser = new MostLikelyWordChooser(jochreSession);
    List<DocumentObserver> documentObservers = new ArrayList<>();
    AltoXMLExporter altoXMLExporter = new AltoXMLExporter(writer, 4);
    documentObservers.add(altoXMLExporter);
    this.doCommandAnalyse(sourceFile, wordChooser, myPages, documentObservers, new ArrayList<>());
  }

  public void imageInputStreamToAlto4(InputStream inputStream, String fileName, Writer writer) throws IOException {
    final Set<Integer> myPages = new HashSet<>();
    MostLikelyWordChooser wordChooser = new MostLikelyWordChooser(jochreSession);
    List<DocumentObserver> documentObservers = new ArrayList<>();
    AltoXMLExporter altoXMLExporter = new AltoXMLExporter(writer, 4);
    documentObservers.add(altoXMLExporter);
    JochreDocumentGenerator documentGenerator = this.getDocumentGenerator(fileName, wordChooser, myPages, documentObservers, new ArrayList<>());
    InputStreamDocumentExtractor documentExtractor = new InputStreamDocumentExtractor(inputStream, fileName, documentGenerator);
    documentExtractor.extractDocument();
  }
}
