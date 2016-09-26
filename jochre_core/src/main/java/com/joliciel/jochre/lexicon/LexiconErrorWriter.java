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
package com.joliciel.jochre.lexicon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.analyser.LetterGuessObserver;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.talismane.utils.CSVFormatter;

/**
 * Creates the following outputs:<br/>
 * a 2x2 matrix of known/unknown vs error/correct<br/>
 * a list of words for each square in the matrix<br/>
 * 
 * @author Assaf Urieli
 *
 */
public class LexiconErrorWriter implements LetterGuessObserver {
	private static final Logger LOG = LoggerFactory.getLogger(LexiconErrorWriter.class);
	private static final CSVFormatter CSV = new CSVFormatter(5);
	private final File outputDir;
	private final String baseName;
	private final MostLikelyWordChooser wordChooser;

	private final Writer knownWordErrorWriter;
	private final Writer knownWordCorrectWriter;
	private final Writer unknownWordErrorWriter;
	private final Writer unknownWordCorrectWriter;
	private final Writer allErrorWriter;
	private final Writer allWordWriter;

	private static final String ALL_GROUP = "All";
	Map<String, Set<Integer>> documentGroups = new HashMap<String, Set<Integer>>();
	List<String> documentNames = null;

	Map<String, ErrorStatistics> errorMap = new LinkedHashMap<String, ErrorStatistics>();
	private JochreDocument currentDoc = null;
	private boolean beamContainsRightWord = false;
	private List<LetterSequence> finalSequences = null;
	private List<LetterSequence> holdoverSequences = null;

	private boolean includeBeam = false;

	private static DecimalFormat df = new DecimalFormat("0.##");

	private final JochreSession jochreSession;

	public LexiconErrorWriter(File outputDir, String baseName, MostLikelyWordChooser wordChooser, String encoding, JochreSession jochreSession) {
		try {
			this.jochreSession = jochreSession;
			this.outputDir = outputDir;
			this.baseName = baseName;
			this.wordChooser = wordChooser;

			errorMap.put(ALL_GROUP, new ErrorStatistics());

			knownWordErrorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_KE.csv"), false), encoding));
			knownWordCorrectWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_KC.csv"), false), encoding));
			unknownWordErrorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_UE.csv"), false), encoding));
			unknownWordCorrectWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_UC.csv"), false), encoding));
			allWordWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_all.csv"), false), encoding));
			allErrorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_err.csv"), false), encoding));

			String line = CSV.format("realSeq") + CSV.format("realWord") + CSV.format("guessSeq") + CSV.format("guessWord") + CSV.format("realFreq")
					+ CSV.format("guessFreq") + CSV.format("file") + CSV.format("page") + CSV.format("par") + CSV.format("row") + CSV.format("group") + CSV.format("id");

			if (this.includeBeam) {
				line += CSV.format("beam");
			}

			line += "\n";

			knownWordErrorWriter.write(line);
			knownWordCorrectWriter.write(line);
			unknownWordErrorWriter.write(line);
			unknownWordCorrectWriter.write(line);

			line = CSV.format("realSeq") + CSV.format("realWord") + CSV.format("guessSeq") + CSV.format("guessWord") + CSV.format("known") + CSV.format("error")
					+ CSV.format("realFreq") + CSV.format("guessFreq") + CSV.format("file") + CSV.format("page") + CSV.format("par") + CSV.format("row")
					+ CSV.format("group") + CSV.format("id");

			if (this.includeBeam) {
				line += CSV.format("beam");
			}

			line += "\n";
			allWordWriter.write(line);
			allErrorWriter.write(line);

		} catch (IOException e) {
			LOG.error("Failed to start LexiconErrorWriter", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onImageStart(JochreImage jochreImage) {
		JochreDocument doc = jochreImage.getPage().getDocument();
		if (!doc.equals(currentDoc)) {
			currentDoc = doc;
			ErrorStatistics stats = errorMap.get(doc.getName());
			if (stats == null) {
				stats = new ErrorStatistics();
				errorMap.put(doc.getName(), stats);
			}
		}
	}

	@Override
	public void onGuessLetter(ShapeInSequence shapeInSequence, String bestGuess) {

	}

	@Override
	public void onStartSequence(LetterSequence letterSequence) {

	}

	@Override
	public void onBeamSearchEnd(LetterSequence bestSequence, List<LetterSequence> finalSequences, List<LetterSequence> holdoverSequences) {
		beamContainsRightWord = false;
		this.finalSequences = finalSequences;
		this.holdoverSequences = holdoverSequences;

		for (LetterSequence letterSequence : finalSequences) {
			if (letterSequence.getRealWord().equals(letterSequence.getGuessedWord())) {
				beamContainsRightWord = true;
				break;
			}
		}
		if (beamContainsRightWord && holdoverSequences != null && holdoverSequences.size() > 0) {
			beamContainsRightWord = false;
			for (LetterSequence letterSequence : holdoverSequences) {
				if (letterSequence.getRealWord().equals(letterSequence.getGuessedWord())) {
					beamContainsRightWord = true;
					break;
				}
			}
		}
	}

	@Override
	public void onGuessSequence(LetterSequence bestSequence) {
		try {
			int realFrequency = 0;
			if (wordChooser != null)
				realFrequency = wordChooser.getFrequency(bestSequence, false);
			boolean error = !bestSequence.getRealWord().equals(bestSequence.getGuessedWord());
			boolean known = realFrequency > 0;
			boolean badSeg = bestSequence.getRealSequence().contains("[") || bestSequence.getRealSequence().contains("|");

			for (int i = 0; i < 3; i++) {
				Writer writer = null;
				if (i == 0) {
					writer = allWordWriter;
				} else if (i == 1) {
					if (error)
						writer = allErrorWriter;
					else
						continue;
				} else {
					int j = 0;
					List<ErrorStatistics> statList = new ArrayList<LexiconErrorWriter.ErrorStatistics>();
					statList.add(errorMap.get(ALL_GROUP));
					statList.add(errorMap.get(currentDoc.getName()));
					for (String docGroupName : documentGroups.keySet()) {
						if (documentGroups.get(docGroupName).contains(currentDoc.getId()))
							statList.add(errorMap.get(docGroupName));
					}

					if (beamContainsRightWord) {
						if (error) {
							for (ErrorStatistics stats : statList)
								stats.answerInBeamErrorCount++;
						} else {
							for (ErrorStatistics stats : statList)
								stats.answerInBeamCorrectCount++;
						}
						beamContainsRightWord = false;
					}

					Linguistics linguistics = jochreSession.getLinguistics();
					for (ShapeInSequence shapeInSequence : bestSequence.getUnderlyingShapeSequence()) {
						String letterGuess = bestSequence.getLetters().get(j++);
						String letter = shapeInSequence.getShape().getLetter();
						boolean badSegLetter = letter.contains("|") || letter.length() == 0
								|| (letter.length() > 1 && !linguistics.getDualCharacterLetters().contains(letter));
						if (letter.equals(letterGuess)) {
							if (known) {
								for (ErrorStatistics stats : statList)
									stats.knownWordCorrectLetterCount++;
							} else {
								for (ErrorStatistics stats : statList)
									stats.unknownWordCorrectLetterCount++;
							}
							if (badSegLetter) {
								for (ErrorStatistics stats : statList)
									stats.badSegCorrectLetterCount++;
							} else {
								for (ErrorStatistics stats : statList)
									stats.goodSegCorrectLetterCount++;
							}
						} else {
							if (known) {
								for (ErrorStatistics stats : statList)
									stats.knownWordErrorLetterCount++;
							} else {
								for (ErrorStatistics stats : statList)
									stats.unknownWordErrorLetterCount++;
							}
							if (badSegLetter) {
								for (ErrorStatistics stats : statList)
									stats.badSegErrorLetterCount++;
							} else {
								for (ErrorStatistics stats : statList)
									stats.goodSegErrorLetterCount++;
							}
						}
					}
					if (error && known) {
						for (ErrorStatistics stats : statList)
							stats.knownWordErrorCount++;
						writer = knownWordErrorWriter;
					} else if (error && !known) {
						for (ErrorStatistics stats : statList)
							stats.unknownWordErrorCount++;
						writer = unknownWordErrorWriter;
					} else if (!error && known) {
						for (ErrorStatistics stats : statList)
							stats.knownWordCorrectCount++;
						writer = knownWordCorrectWriter;
					} else if (!error && !known) {
						for (ErrorStatistics stats : statList)
							stats.unknownWordCorrectCount++;
						writer = unknownWordCorrectWriter;
					}

					if (error) {
						if (badSeg) {
							for (ErrorStatistics stats : statList)
								stats.badSegErrorCount++;
						} else {
							for (ErrorStatistics stats : statList)
								stats.goodSegErrorCount++;
						}
					} else {
						if (badSeg) {
							for (ErrorStatistics stats : statList)
								stats.badSegCorrectCount++;
						} else {
							for (ErrorStatistics stats : statList)
								stats.goodSegCorrectCount++;
						}
					}

				}

				writer.write(CSV.format(bestSequence.getRealSequence()));
				writer.write(CSV.format(bestSequence.getRealWord()));
				writer.write(CSV.format(bestSequence.getGuessedSequence()));
				writer.write(CSV.format(bestSequence.getGuessedWord()));

				if (i < 2) {
					writer.write(CSV.format(known ? 1 : 0));
					writer.write(CSV.format(error ? 1 : 0));
				}

				writer.write(CSV.format(realFrequency));
				writer.write(CSV.format(bestSequence.getFrequency()));
				GroupOfShapes group = bestSequence.getGroups().get(0);
				writer.write(CSV.format(group.getRow().getParagraph().getImage().getPage().getDocument().getName()));
				writer.write(CSV.format(group.getRow().getParagraph().getImage().getPage().getIndex()));
				writer.write(CSV.format(group.getRow().getParagraph().getIndex()));
				writer.write(CSV.format(group.getRow().getIndex()));
				writer.write(CSV.format(group.getIndex()));
				writer.write(CSV.format(group.getId()));

				if (this.includeBeam) {
					if (finalSequences != null) {
						for (LetterSequence sequence : finalSequences) {
							writer.write(CSV.format(sequence.getGuessedSequence()));
							writer.write(CSV.format(sequence.getScore()));
							writer.write(CSV.format(sequence.getAdjustedScore()));
						}
					}
					writer.write(CSV.format(""));
					if (holdoverSequences != null) {
						for (LetterSequence sequence : holdoverSequences) {
							writer.write(CSV.format(sequence.getGuessedSequence()));
							writer.write(CSV.format(sequence.getScore()));
							writer.write(CSV.format(sequence.getAdjustedScore()));
						}
					}
				}

				writer.write("\n");
				writer.flush();
			}

		} catch (IOException e) {
			LOG.error("Failed to write to LexiconErrorWriter", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onImageEnd() {
	}

	@Override
	public void onFinish() {
		try {
			knownWordErrorWriter.close();
			knownWordCorrectWriter.close();
			unknownWordErrorWriter.close();
			unknownWordCorrectWriter.close();
			allWordWriter.close();
			allErrorWriter.close();

			Writer statsWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, baseName + "_KEMatrix.csv"), false), "UTF8"));
			writeStats(statsWriter, errorMap);

			statsWriter.flush();
			statsWriter.close();
		} catch (IOException e) {
			LOG.error("Failed to write to LexiconErrorWriter", e);
			throw new RuntimeException(e);
		}

	}

	public static void writeStats(Writer statsWriter, Map<String, ErrorStatistics> errorMap) {
		try {
			for (String statName : errorMap.keySet()) {
				statsWriter.write(CSV.format(statName) + CSV.getCsvSeparator() + CSV.getCsvSeparator() + CSV.getCsvSeparator() + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (@SuppressWarnings("unused")
			String statName : errorMap.keySet()) {
				statsWriter.write(CSV.getCsvSeparator() + CSV.format("correct") + CSV.format("error") + CSV.format("total") + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("known") + CSV.format(stats.knownWordCorrectCount) + CSV.format(stats.knownWordErrorCount)
						+ CSV.format(stats.knownWordCorrectCount + stats.knownWordErrorCount) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("unknown") + CSV.format(stats.unknownWordCorrectCount) + CSV.format(stats.unknownWordErrorCount)
						+ CSV.format(stats.unknownWordCorrectCount + stats.unknownWordErrorCount) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("goodSeg") + CSV.format(stats.goodSegCorrectCount) + CSV.format(stats.goodSegErrorCount)
						+ CSV.format(stats.goodSegCorrectCount + stats.goodSegErrorCount) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("badSeg") + CSV.format(stats.badSegCorrectCount) + CSV.format(stats.badSegErrorCount)
						+ CSV.format(stats.badSegCorrectCount + stats.badSegErrorCount) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("inBeam") + CSV.format(stats.answerInBeamCorrectCount) + CSV.format(stats.answerInBeamErrorCount)
						+ CSV.format(stats.getAnswerInBeamCount()) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("total") + CSV.format(stats.knownWordCorrectCount + stats.unknownWordCorrectCount)
						+ CSV.format(stats.knownWordErrorCount + stats.unknownWordErrorCount)
						+ CSV.format(stats.knownWordCorrectCount + stats.knownWordErrorCount + stats.unknownWordCorrectCount + stats.unknownWordErrorCount)
						+ CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(
						CSV.format("known%") + CSV.format(stats.getKnownWordCount() == 0 ? "0" : df.format(stats.knownWordCorrectCount / stats.getKnownWordCount() * 100))
								+ CSV.format(stats.getKnownWordCount() == 0 ? "0" : df.format(stats.knownWordErrorCount / stats.getKnownWordCount() * 100))
								+ CSV.format(stats.getTotalCount() == 0 ? "0" : df.format(stats.getKnownWordCount() / stats.getTotalCount() * 100)) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("unknown%")
						+ CSV.format(stats.getUnknownWordCount() == 0 ? "0" : df.format(stats.unknownWordCorrectCount / stats.getUnknownWordCount() * 100))
						+ CSV.format(stats.getUnknownWordCount() == 0 ? "0" : df.format(stats.unknownWordErrorCount / stats.getUnknownWordCount() * 100))
						+ CSV.format(stats.getTotalCount() == 0 ? "0" : df.format(stats.getUnknownWordCount() / stats.getTotalCount() * 100)) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(
						CSV.format("goodSeg%") + CSV.format(stats.getGoodSegCount() == 0 ? "0" : df.format(stats.goodSegCorrectCount / stats.getGoodSegCount() * 100))
								+ CSV.format(stats.getGoodSegCount() == 0 ? "0" : df.format(stats.goodSegErrorCount / stats.getGoodSegCount() * 100))
								+ CSV.format(stats.getTotalCount() == 0 ? "0" : df.format(stats.getGoodSegCount() / stats.getTotalCount() * 100)) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter
						.write(CSV.format("badSeg%") + CSV.format(stats.getBadSegCount() == 0 ? "0" : df.format(stats.badSegCorrectCount / stats.getBadSegCount() * 100))
								+ CSV.format(stats.getBadSegCount() == 0 ? "0" : df.format(stats.badSegErrorCount / stats.getBadSegCount() * 100))
								+ CSV.format(stats.getTotalCount() == 0 ? "0" : df.format(stats.getBadSegCount() / stats.getTotalCount() * 100)) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("inBeam%")
						+ CSV.format(stats.getTotalCorrectCount() == 0 ? "0" : df.format(stats.answerInBeamCorrectCount / stats.getTotalCorrectCount() * 100))
						+ CSV.format(stats.getTotalErrorCount() == 0 ? "0" : df.format(stats.answerInBeamErrorCount / stats.getTotalErrorCount() * 100))
						+ CSV.format(stats.getTotalCount() == 0 ? "0" : df.format(stats.getAnswerInBeamCount() / stats.getTotalCount() * 100)) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("total%")
						+ CSV.format(
								stats.getTotalCount() == 0 ? "0" : df.format((stats.knownWordCorrectCount + stats.unknownWordCorrectCount) / stats.getTotalCount() * 100))
						+ CSV.format(stats.getTotalCount() == 0 ? "0" : df.format((stats.knownWordErrorCount + stats.unknownWordErrorCount) / stats.getTotalCount() * 100))
						+ CSV.format(stats.getTotalCount() == 0 ? "0" : df.format(100)) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("knownLetters") + CSV.format(stats.knownWordCorrectLetterCount) + CSV.format(stats.knownWordErrorLetterCount)
						+ CSV.format(stats.knownWordCorrectLetterCount + stats.knownWordErrorLetterCount) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("unknownLetters") + CSV.format(stats.unknownWordCorrectLetterCount) + CSV.format(stats.unknownWordErrorLetterCount)
						+ CSV.format(stats.unknownWordCorrectLetterCount + stats.unknownWordErrorLetterCount) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("goodSegLetters") + CSV.format(stats.goodSegCorrectLetterCount) + CSV.format(stats.goodSegErrorLetterCount)
						+ CSV.format(stats.goodSegCorrectLetterCount + stats.goodSegErrorLetterCount) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("badSegLetters") + CSV.format(stats.badSegCorrectLetterCount) + CSV.format(stats.badSegErrorLetterCount)
						+ CSV.format(stats.badSegCorrectLetterCount + stats.badSegErrorLetterCount) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("totalLetters") + CSV.format(stats.knownWordCorrectLetterCount + stats.unknownWordCorrectLetterCount)
						+ CSV.format(stats.knownWordErrorLetterCount + stats.unknownWordErrorLetterCount)
						+ CSV.format(
								stats.knownWordCorrectLetterCount + stats.knownWordErrorLetterCount + stats.unknownWordCorrectLetterCount + stats.unknownWordErrorLetterCount)
						+ CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("knownLetter%")
						+ CSV.format(stats.getKnownWordLetterCount() == 0 ? "0" : df.format(stats.knownWordCorrectLetterCount / stats.getKnownWordLetterCount() * 100))
						+ CSV.format(stats.getKnownWordLetterCount() == 0 ? "0" : df.format(stats.knownWordErrorLetterCount / stats.getKnownWordLetterCount() * 100))
						+ CSV.format(stats.getTotalLetterCount() == 0 ? "0" : df.format(stats.getKnownWordLetterCount() / stats.getTotalLetterCount() * 100))
						+ CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("unknownLetter%")
						+ CSV
								.format(stats.getUnknownWordLetterCount() == 0 ? "0" : df.format(stats.unknownWordCorrectLetterCount / stats.getUnknownWordLetterCount() * 100))
						+ CSV.format(stats.getUnknownWordLetterCount() == 0 ? "0" : df.format(stats.unknownWordErrorLetterCount / stats.getUnknownWordLetterCount() * 100))
						+ CSV.format(stats.getTotalLetterCount() == 0 ? "0" : df.format(stats.getUnknownWordLetterCount() / stats.getTotalLetterCount() * 100))
						+ CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("goodSegLetter%")
						+ CSV.format(stats.getGoodSegLetterCount() == 0 ? "0" : df.format(stats.goodSegCorrectLetterCount / stats.getGoodSegLetterCount() * 100))
						+ CSV.format(stats.getGoodSegLetterCount() == 0 ? "0" : df.format(stats.goodSegErrorLetterCount / stats.getGoodSegLetterCount() * 100))
						+ CSV.format(stats.getTotalLetterCount() == 0 ? "0" : df.format(stats.getGoodSegLetterCount() / stats.getTotalLetterCount() * 100))
						+ CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("badSegLetter%")
						+ CSV.format(stats.getBadSegLetterCount() == 0 ? "0" : df.format(stats.badSegCorrectLetterCount / stats.getBadSegLetterCount() * 100))
						+ CSV.format(stats.getBadSegLetterCount() == 0 ? "0" : df.format(stats.badSegErrorLetterCount / stats.getBadSegLetterCount() * 100))
						+ CSV.format(stats.getTotalLetterCount() == 0 ? "0" : df.format(stats.getBadSegLetterCount() / stats.getTotalLetterCount() * 100))
						+ CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			for (String statName : errorMap.keySet()) {
				ErrorStatistics stats = errorMap.get(statName);
				statsWriter.write(CSV.format("totalLetter%")
						+ CSV.format(stats.getTotalLetterCount() == 0 ? "0"
								: df.format((stats.knownWordCorrectLetterCount + stats.unknownWordCorrectLetterCount) / stats.getTotalLetterCount() * 100))
						+ CSV.format(stats.getTotalLetterCount() == 0 ? "0"
								: df.format((stats.knownWordErrorLetterCount + stats.unknownWordErrorLetterCount) / stats.getTotalLetterCount() * 100))
						+ CSV.format(stats.getTotalLetterCount() == 0 ? "0" : df.format(100)) + CSV.getCsvSeparator());
			}
			statsWriter.write("\n");

			statsWriter.flush();
		} catch (IOException e) {
			LOG.error("Failed to write to LexiconErrorWriter", e);
			throw new RuntimeException(e);
		}
	}

	public Map<String, Set<Integer>> getDocumentGroups() {
		return documentGroups;
	}

	public void setDocumentGroups(Map<String, Set<Integer>> documentGroups) {
		this.documentGroups = documentGroups;
		for (String group : documentGroups.keySet()) {
			this.errorMap.put(group, new ErrorStatistics());
		}
	}

	public List<String> getDocumentNames() {
		return documentNames;
	}

	public void setDocumentNames(List<String> documentNames) {
		this.documentNames = documentNames;
		for (String documentName : documentNames) {
			this.errorMap.put(documentName, new ErrorStatistics());
		}
	}

	private static final class ErrorStatistics {
		public int knownWordErrorCount;
		public int knownWordCorrectCount;
		public int unknownWordErrorCount;
		public int unknownWordCorrectCount;
		public int goodSegCorrectCount;
		public int goodSegErrorCount;
		public int badSegCorrectCount;
		public int badSegErrorCount;

		public int knownWordErrorLetterCount;
		public int knownWordCorrectLetterCount;
		public int unknownWordErrorLetterCount;
		public int unknownWordCorrectLetterCount;

		public int goodSegCorrectLetterCount;
		public int goodSegErrorLetterCount;
		public int badSegCorrectLetterCount;
		public int badSegErrorLetterCount;

		public int answerInBeamCorrectCount;
		public int answerInBeamErrorCount;

		public double getTotalCount() {
			return knownWordCorrectCount + unknownWordCorrectCount + knownWordErrorCount + unknownWordErrorCount;
		}

		public double getTotalLetterCount() {
			return knownWordCorrectLetterCount + unknownWordCorrectLetterCount + knownWordErrorLetterCount + unknownWordErrorLetterCount;
		}

		public double getTotalCorrectCount() {
			return knownWordCorrectCount + unknownWordCorrectCount;
		}

		public double getTotalErrorCount() {
			return knownWordErrorCount + unknownWordErrorCount;
		}

		public double getKnownWordCount() {
			return knownWordCorrectCount + knownWordErrorCount;
		}

		public double getUnknownWordCount() {
			return unknownWordCorrectCount + unknownWordErrorCount;
		}

		public double getKnownWordLetterCount() {
			return knownWordCorrectLetterCount + knownWordErrorLetterCount;
		}

		public double getUnknownWordLetterCount() {
			return unknownWordCorrectLetterCount + unknownWordErrorLetterCount;
		}

		public double getGoodSegCount() {
			return goodSegCorrectCount + goodSegErrorCount;
		}

		public double getBadSegCount() {
			return badSegCorrectCount + badSegErrorCount;
		}

		public double getGoodSegLetterCount() {
			return goodSegCorrectLetterCount + goodSegErrorLetterCount;
		}

		public double getBadSegLetterCount() {
			return badSegCorrectLetterCount + badSegErrorLetterCount;
		}

		public double getAnswerInBeamCount() {
			return answerInBeamCorrectCount + answerInBeamErrorCount;
		}
	}

	public static void main(String[] args) throws Exception {
		File evalDir = new File(args[0]);
		String prefix = args[1];
		mergeCrossValidation(evalDir, prefix);
	}

	static void mergeCrossValidation(File evalDir, String prefix) {
		try {
			File[] files = evalDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if (name.endsWith(".csv"))
						return true;
					else
						return false;
				}
			});
			List<String> groupNames = new ArrayList<String>();
			Map<String, Writer> writers = new HashMap<String, Writer>();
			Map<String, ErrorStatistics> errorMap = new LinkedHashMap<String, ErrorStatistics>();
			Map<String, Map<String, DescriptiveStatistics>> statMap = new HashMap<String, Map<String, DescriptiveStatistics>>();
			for (File file : files) {
				String filename = file.getName();
				LOG.debug("Processing " + filename);
				int index = Integer.parseInt(filename.substring(prefix.length(), prefix.length() + 1));
				String suffix = filename.substring(prefix.length() + 2, filename.lastIndexOf('_'));
				String fileType = filename.substring(filename.lastIndexOf('_') + 1, filename.lastIndexOf('.'));
				LOG.debug("Processing " + filename);
				LOG.debug("index: " + index);
				LOG.debug("suffix: " + suffix);
				LOG.debug("fileType: " + fileType);
				Writer writer = writers.get(fileType);
				boolean firstFile = false;
				if (writer == null) {
					writer = new BufferedWriter(
							new OutputStreamWriter(new FileOutputStream(new File(evalDir, prefix + "A_" + suffix + "_" + fileType + ".csv"), false), "UTF8"));
					writers.put(fileType, writer);
					firstFile = true;
				}
				if (fileType.equals("KEMatrix")) {
					Scanner scanner = new Scanner(file);
					int i = 0;
					List<String> myGroupNames = new ArrayList<String>();
					Map<String, Boolean> haveCountMap = new HashMap<String, Boolean>();
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();
						List<String> cells = CSV.getCSVCells(line);
						if (i == 0) {
							for (int j = 0; j < cells.size(); j += 5) {
								String groupName = cells.get(j);
								if (!errorMap.containsKey(groupName)) {
									errorMap.put(groupName, new ErrorStatistics());
									statMap.put(groupName, new HashMap<String, DescriptiveStatistics>());
									groupNames.add(groupName);
								}
								myGroupNames.add(groupName);
							}
						} else if (i == 1) {
							// do nothing
						} else {
							String rowName = cells.get(0);
							int j = 0;
							for (String groupName : myGroupNames) {
								ErrorStatistics errorStats = errorMap.get(groupName);
								Map<String, DescriptiveStatistics> stats = statMap.get(groupName);
								double correctCount = Double.parseDouble(cells.get(j * 5 + 1));
								double errorCount = Double.parseDouble(cells.get(j * 5 + 2));
								double totalCount = Double.parseDouble(cells.get(j * 5 + 3));
								Boolean haveCount = haveCountMap.get(groupName);

								if (rowName.equals("known")) {
									errorStats.knownWordCorrectCount += correctCount;
									errorStats.knownWordErrorCount += errorCount;
								} else if (rowName.equals("unknown")) {
									errorStats.unknownWordCorrectCount += correctCount;
									errorStats.unknownWordErrorCount += errorCount;
								} else if (rowName.equals("goodSeg")) {
									errorStats.goodSegCorrectCount += correctCount;
									errorStats.goodSegErrorCount += errorCount;
								} else if (rowName.equals("badSeg")) {
									errorStats.badSegCorrectCount += correctCount;
									errorStats.badSegErrorCount += errorCount;
								} else if (rowName.equals("knownLetters")) {
									errorStats.knownWordCorrectLetterCount += correctCount;
									errorStats.knownWordErrorLetterCount += errorCount;
								} else if (rowName.equals("unknownLetters")) {
									errorStats.unknownWordCorrectLetterCount += correctCount;
									errorStats.unknownWordErrorLetterCount += errorCount;
								} else if (rowName.equals("goodSegLetters")) {
									errorStats.goodSegCorrectLetterCount += correctCount;
									errorStats.goodSegErrorLetterCount += errorCount;
								} else if (rowName.equals("badSegLetters")) {
									errorStats.badSegCorrectLetterCount += correctCount;
									errorStats.badSegErrorLetterCount += errorCount;
								} else if (rowName.equals("inBeam")) {
									errorStats.answerInBeamCorrectCount += correctCount;
									errorStats.answerInBeamErrorCount += errorCount;
								} else if (rowName.equals("total")) {
									haveCountMap.put(groupName, totalCount > 0);
								} else if (rowName.endsWith("%")) {
									if (haveCount) {
										String keyPrefix = rowName.substring(0, rowName.length() - 1);
										String key = keyPrefix + "|correct";
										DescriptiveStatistics correctStat = stats.get(key);
										if (correctStat == null) {
											correctStat = new DescriptiveStatistics();
											stats.put(key, correctStat);
										}
										correctStat.addValue(correctCount);
										key = keyPrefix + "|error";
										DescriptiveStatistics errorStat = stats.get(key);
										if (errorStat == null) {
											errorStat = new DescriptiveStatistics();
											stats.put(key, errorStat);
										}
										errorStat.addValue(errorCount);
										key = keyPrefix + "|total";
										DescriptiveStatistics totalStat = stats.get(key);
										if (totalStat == null) {
											totalStat = new DescriptiveStatistics();
											stats.put(key, totalStat);
										}
										totalStat.addValue(totalCount);
									}
								}

								j++;
							}
						}
						i++;
					}
					scanner.close();
				} else {
					Scanner scanner = new Scanner(file);
					boolean firstLine = true;
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();
						if (firstLine) {
							if (firstFile)
								writer.write(line + "\n");
							firstLine = false;
						} else {
							writer.write(line + "\n");
						}
						writer.flush();
					}
					scanner.close();
				} // file type
			} // next file

			Writer statsWriter = writers.get("KEMatrix");
			writeStats(statsWriter, errorMap);
			statsWriter.write("\n");
			String[] statTypes = new String[] { "known", "unknown", "goodSeg", "badSeg", "inBeam", "total", "knownLetter", "unknownLetter", "goodSegLetter",
					"badSegLetter", "totalLetter" };
			for (String statType : statTypes) {
				for (String groupName : groupNames) {
					Map<String, DescriptiveStatistics> statsMap = statMap.get(groupName);
					DescriptiveStatistics correctStat = statsMap.get(statType + "|correct");
					DescriptiveStatistics errorStat = statsMap.get(statType + "|error");
					DescriptiveStatistics totalStat = statsMap.get(statType + "|total");

					statsWriter.write(CSV.format(statType + "%Avg") + CSV.format(correctStat.getMean()) + CSV.format(errorStat.getMean())
							+ CSV.format(totalStat.getMean()) + CSV.getCsvSeparator());

				} // next group
				statsWriter.write("\n");
				for (String groupName : groupNames) {
					Map<String, DescriptiveStatistics> statsMap = statMap.get(groupName);
					DescriptiveStatistics correctStat = statsMap.get(statType + "|correct");
					DescriptiveStatistics errorStat = statsMap.get(statType + "|error");
					DescriptiveStatistics totalStat = statsMap.get(statType + "|total");

					statsWriter.write(CSV.format(statType + "%Dev") + CSV.format(correctStat.getStandardDeviation()) + CSV.format(errorStat.getStandardDeviation())
							+ CSV.format(totalStat.getStandardDeviation()) + CSV.getCsvSeparator());

				} // next group
				statsWriter.write("\n");
				statsWriter.flush();
			}
			statsWriter.close();

		} catch (IOException e) {
			LOG.error("Failed to write to LexiconErrorWriter", e);
			throw new RuntimeException(e);
		}
	}

	public boolean isIncludeBeam() {
		return includeBeam;
	}

	public void setIncludeBeam(boolean includeBeam) {
		this.includeBeam = includeBeam;
	}

}
