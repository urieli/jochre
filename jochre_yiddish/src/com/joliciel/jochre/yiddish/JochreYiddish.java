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
package com.joliciel.jochre.yiddish;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.Jochre;
import com.joliciel.jochre.JochreServiceLocator;
import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.lexicon.Lexicon;
import com.joliciel.jochre.lexicon.LexiconMerger;
import com.joliciel.jochre.lexicon.LexiconService;
import com.joliciel.jochre.lexicon.LocaleSpecificLexiconService;
import com.joliciel.jochre.lexicon.MostLikelyWordChooser;
import com.joliciel.jochre.lexicon.TextFileLexicon;
import com.joliciel.jochre.lexicon.UnknownWordListWriter;
import com.joliciel.jochre.lexicon.WordSplitter;
import com.joliciel.jochre.output.TextFormat;
import com.joliciel.jochre.output.OutputService;
import com.joliciel.talismane.utils.LogUtils;

public class JochreYiddish implements LocaleSpecificLexiconService {
	private static final Log LOG = LogFactory.getLog(JochreYiddish.class);

	String lexiconDirPath = "";
	WordSplitter yiddishWordSplitter = null;
	Lexicon yiddishLexicon = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		try {
			if (args.length==0) {
				System.out.println("Usage (* indicates optional):");
				System.out.println("JochreYiddish load file=[filename] name=[userFriendlyName] lang=[isoLanguageCode] first=[firstPage]* last=[lastPage]* outputDir=[outputDirectory]* showSeg=[true/false]");
				return;
			}
			String command = args[0];

			String filename = "";
			String userFriendlyName = "";
			String outputDirPath = null;
			int docId = -1;
			int imageId = 0;
			int imageCount=-1;
			int userId = -1;
			int beamWidth = 5;
			boolean save = false;
			String letterModelPath = "";
			String splitModelPath = "";
			String mergeModelPath = "";
			String lexiconDirPath = "";
			ImageStatus testSet = ImageStatus.TRAINING_HELD_OUT;
			double smoothing = 0.5;
			double frequencyLogBase = 2.0;
			boolean reconstructLetters = false;
			int firstPage = -1;
			int lastPage = -1;
	
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
				else if (argName.equals("outputDir"))
					outputDirPath = argValue;
				else if (argName.equals("save"))
					save = (argValue.equals("true"));
				else if (argName.equals("imageId"))
					imageId = Integer.parseInt(argValue);
				else if (argName.equals("imageCount"))
					imageCount = Integer.parseInt(argValue);
				else if (argName.equals("docId"))
					docId = Integer.parseInt(argValue);
				else if (argName.equals("userId"))
					userId = Integer.parseInt(argValue);
				else if (argName.equals("beamWidth"))
					beamWidth = Integer.parseInt(argValue);
				else if (argName.equals("frequencyLogBase"))
					frequencyLogBase = Double.parseDouble(argValue);
				else if (argName.equals("smoothing"))
					smoothing = Double.parseDouble(argValue);
				else if (argName.equals("letterModel"))
					letterModelPath = argValue;
				else if (argName.equals("splitModel"))
					splitModelPath = argValue;
				else if (argName.equals("mergeModel"))
					mergeModelPath = argValue;
				else if (argName.equals("lexiconDir"))
					lexiconDirPath = argValue;
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
				else if (argName.equals("firstPage"))
					firstPage = Integer.parseInt(argValue);
				else if (argName.equals("lastPage"))
					lastPage = Integer.parseInt(argValue);
				else
					throw new RuntimeException("Unknown argument: " + argName);
	
				if (userFriendlyName.length()==0)
					userFriendlyName = filename;
			}    	
			
			JochreServiceLocator jochreServiceLocator = JochreServiceLocator.getInstance();
			String dataSourcePropertiesPath = "jdbc-live.properties";
			jochreServiceLocator.setDataSourcePropertiesResource(dataSourcePropertiesPath);
			
			String isoLanguage = "yi";

			JochreYiddish jochreYiddish = new JochreYiddish();
			jochreYiddish.setLexiconDirPath(lexiconDirPath);
			
			WordSplitter wordSplitter = jochreYiddish.getWordSplitter();
			
			MostLikelyWordChooser wordChooser = null;
			
			OutputService outputService = jochreServiceLocator.getTextServiceLocator().getTextService();
			
			Lexicon yiddishLexicon = null;
			if (lexiconDirPath.length()>0) {
				yiddishLexicon = jochreYiddish.getLexicon();
				LexiconService lexiconService = jochreServiceLocator.getLexiconServiceLocator().getLexiconService();

	        	wordChooser = lexiconService.getMostLikelyWordChooser(yiddishLexicon, wordSplitter);
	        	wordChooser.setAdditiveSmoothing(smoothing);
	        	wordChooser.setFrequencyLogBase(frequencyLogBase);
			}
        	
			Jochre jochre = new Jochre();
			
			jochre.setIsoLanguage(isoLanguage);
			jochre.setUserId(userId);

			if (command.equals("evaluate")||command.equals("evaluateComplex")) {
				jochre.doCommandEvaluate(letterModelPath, testSet, imageCount, imageId, outputDirPath, wordChooser, beamWidth, reconstructLetters);
			} else if (command.equals("evaluateFull")) {
				jochre.doCommandEvaluateFull(letterModelPath, splitModelPath, mergeModelPath, testSet, imageCount, imageId, save, outputDirPath, wordChooser, beamWidth, Jochre.BoundaryDetectorType.Deterministic, 0.5);
			} else if (command.equals("analyse")) {
				jochre.doCommandAnalyse(letterModelPath, docId, imageId, testSet, wordChooser);
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

        		String baseName = filename.substring(0, filename.indexOf("."));
	    		if (baseName.lastIndexOf("/")>0)
	    			baseName = baseName.substring(baseName.lastIndexOf("/")+1);
	    		
        		List<DocumentObserver> observers = new ArrayList<DocumentObserver>();
		       	Writer analysisFileWriter = null;
	    		String outputFileName = baseName+ ".xml";
				File analysisFile = new File(outputDir, outputFileName);
				analysisFile.delete();
				analysisFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(analysisFile, true),"UTF8"));
				
				DocumentObserver observer = outputService.getAbbyyFineReader8Exporter(analysisFileWriter);
				observers.add(observer);
				
		       	Writer htmlWriter = null;
	    		String htmlFileName = baseName+ ".html";
	    		
				File htmlFile = new File(outputDir, htmlFileName);
				htmlFile.delete();
				htmlWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(htmlFile, true),"UTF8"));
				
				DocumentObserver textGetter = outputService.getTextGetter(htmlWriter, TextFormat.XHTML, yiddishLexicon);
				observers.add(textGetter);
				
				if (yiddishLexicon!=null) {
					File unknownWordFile = new File(outputDir, "unknownWords.txt");
					unknownWordFile.delete();
					Writer unknownWordWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(unknownWordFile, true),"UTF8"));

					UnknownWordListWriter unknownWordListWriter = new UnknownWordListWriter(unknownWordWriter);
					observers.add(unknownWordListWriter);
				}
				jochre.doCommandAnalyse(pdfFile, letterModelFile, splitModelFile, mergeModelFile, wordChooser, observers, firstPage, lastPage);
			} else if (command.equals("buildLexicon")) {
				jochre.doCommandBuildLexicon(outputDirPath, wordSplitter);
			}
			
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw e;
		}
		LOG.debug("#### finished #####");
	}

	@Override
	public Locale getLocale() {
		return new Locale("yi");
	}

	@Override
	public Lexicon getLexicon() {
		if (yiddishLexicon == null && lexiconDirPath!=null && lexiconDirPath.length()>0) {
			LexiconMerger lexiconMerger = new LexiconMerger();
			File lexiconDir = new File(lexiconDirPath);
		
			File[] lexiconFiles = lexiconDir.listFiles();
			for (File lexiconFile : lexiconFiles) {
				TextFileLexicon lexicon = TextFileLexicon.deserialize(lexiconFile);
				lexiconMerger.addLexicon(lexicon);
			}
	
			yiddishLexicon = new YiddishWordFrequencyFinder(lexiconMerger);
		}
		return yiddishLexicon;
	}

	@Override
	public WordSplitter getWordSplitter() {
		if (yiddishWordSplitter==null) {
			yiddishWordSplitter = new YiddishWordSplitter();
		}
		return yiddishWordSplitter;
	}

	public String getLexiconDirPath() {
		return lexiconDirPath;
	}

	public void setLexiconDirPath(String lexiconDirPath) {
		this.lexiconDirPath = lexiconDirPath;
	}

}
