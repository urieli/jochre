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
package com.joliciel.jochre.analyser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.BoundaryDetector;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.letterGuesser.LetterGuesser;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.jochre.lexicon.MostLikelyWordChooser;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.Monitorable;
import com.joliciel.talismane.utils.PerformanceMonitor;
import com.joliciel.talismane.utils.ProgressMonitor;
import com.joliciel.talismane.utils.SimpleProgressMonitor;
import com.typesafe.config.Config;

/**
 * Perform a analysis using a beam search.
 * 
 * @author Assaf Urieli
 *
 */
public class BeamSearchImageAnalyser implements ImageAnalyser, Monitorable {
	private static final Logger LOG = LoggerFactory.getLogger(BeamSearchImageAnalyser.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(BeamSearchImageAnalyser.class);

	private final MostLikelyWordChooser mostLikelyWordChooser;
	private final BoundaryDetector boundaryDetector;
	private final LetterGuesser letterGuesser;

	private final int beamWidth;
	private final double minOutcomeWeight;

	private int shapeCount = 0;
	private int totalShapeCount = -1;

	private SimpleProgressMonitor currentMonitor;

	private List<LetterGuessObserver> observers = new ArrayList<LetterGuessObserver>();

	private final JochreSession jochreSession;

	/**
	 * 
	 * @param boundaryDetector
	 *            if null, boundaries are not changed from the original shapes
	 * @param letterGuesser
	 *            cannot be null - used to guess letters
	 * @param mostLikelyWordChooser
	 *            if not null, adjusts the probabilities of the variou guesses
	 * @param jochreSession
	 */
	public BeamSearchImageAnalyser(BoundaryDetector boundaryDetector, LetterGuesser letterGuesser, MostLikelyWordChooser mostLikelyWordChooser,
			JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.letterGuesser = letterGuesser;
		this.boundaryDetector = boundaryDetector;
		this.mostLikelyWordChooser = mostLikelyWordChooser;
		Config imageAnalyserConfig = jochreSession.getConfig().getConfig("jochre.image-analyser");
		this.beamWidth = imageAnalyserConfig.getInt("beam-width");
		this.minOutcomeWeight = imageAnalyserConfig.getDouble("min-outcome-prob");
	}

	@Override
	public void analyse(JochreImage image) {
		MONITOR.startTask("analyse");
		try {
			this.analyseInternal(image);

			for (LetterGuessObserver observer : observers) {
				observer.onFinish();
			}
		} finally {
			MONITOR.endTask();
		}
	}

	public void analyseInternal(JochreImage image) {
		LOG.debug("Analysing image " + image.getId());
		if (currentMonitor != null) {
			currentMonitor.setCurrentAction("imageMonitor.analysingImage", new Object[] { image.getPage().getIndex() });
		}
		for (LetterGuessObserver observer : observers) {
			observer.onImageStart(image);
		}
		if (totalShapeCount < 0)
			totalShapeCount = image.getShapeCount();

		for (Paragraph paragraph : image.getParagraphs()) {
			LOG.debug("Analysing paragraph " + paragraph.getIndex() + " (id=" + paragraph.getId() + ")");
			List<LetterSequence> holdoverSequences = null;
			GroupOfShapes holdoverGroup = null;
			for (RowOfShapes row : paragraph.getRows()) {
				LOG.debug("Analysing row " + row.getIndex() + " (id=" + row.getId() + ")");
				for (GroupOfShapes group : row.getGroups()) {
					if (group.isSkip()) {
						LOG.debug("Skipping group " + group.getIndex() + " (id=" + group.getId() + ")");
						continue;
					}
					LOG.debug("Analysing group " + group.getIndex() + " (id=" + group.getId() + ")");

					int width = group.getRight() - group.getLeft() + 1;

					List<ShapeSequence> shapeSequences = null;
					if (boundaryDetector != null) {
						shapeSequences = boundaryDetector.findBoundaries(group);
					} else {
						// simply add this groups shape's
						shapeSequences = new ArrayList<ShapeSequence>();
						ShapeSequence shapeSequence = new ShapeSequence();
						for (Shape shape : group.getShapes())
							shapeSequence.addShape(shape);
						shapeSequences.add(shapeSequence);
					}

					// Perform a beam search to guess the most likely sequence
					// for this
					// word
					TreeMap<Integer, PriorityQueue<LetterSequence>> heaps = new TreeMap<Integer, PriorityQueue<LetterSequence>>();

					// prime a starter heap with the n best shape boundary
					// analyses for
					// this group
					PriorityQueue<LetterSequence> starterHeap = new PriorityQueue<LetterSequence>(1);
					for (ShapeSequence shapeSequence : shapeSequences) {
						LetterSequence emptySequence = new LetterSequence(shapeSequence, jochreSession);
						starterHeap.add(emptySequence);
					}
					heaps.put(0, starterHeap);

					PriorityQueue<LetterSequence> finalHeap = null;
					while (heaps.size() > 0) {
						Entry<Integer, PriorityQueue<LetterSequence>> heapEntry = heaps.pollFirstEntry();
						if (LOG.isTraceEnabled())
							LOG.trace("heap for index: " + heapEntry.getKey().intValue() + ", width: " + width);
						if (heapEntry.getKey().intValue() == width) {
							finalHeap = heapEntry.getValue();
							break;
						}

						PriorityQueue<LetterSequence> previousHeap = heapEntry.getValue();

						// limit the breadth to K
						int maxSequences = previousHeap.size() > this.beamWidth ? this.beamWidth : previousHeap.size();

						for (int j = 0; j < maxSequences; j++) {
							LetterSequence history = previousHeap.poll();
							ShapeInSequence shapeInSequence = history.getNextShape();
							Shape shape = shapeInSequence.getShape();
							if (LOG.isTraceEnabled()) {
								LOG.trace("Sequence " + history + ", shape: " + shape);
							}
							LogUtils.logMemory(LOG);
							int position = 0;
							if (jochreSession.getLinguistics().isLeftToRight()) {
								position = shape.getRight() - group.getLeft() + 1;
							} else {
								position = group.getRight() - shape.getLeft() + 1;
							}
							PriorityQueue<LetterSequence> heap = heaps.get(position);
							if (heap == null) {
								heap = new PriorityQueue<LetterSequence>();
								heaps.put(position, heap);
							}

							MONITOR.startTask("guess letter");
							try {
								letterGuesser.guessLetter(shapeInSequence, history);
							} finally {
								MONITOR.endTask();
							}

							MONITOR.startTask("heap sort");
							try {
								for (Decision letterGuess : shape.getLetterGuesses()) {
									// leave out very low probability outcomes
									if (letterGuess.getProbability() > this.minOutcomeWeight) {
										LetterSequence sequence = new LetterSequence(history);
										sequence.getLetters().add(letterGuess.getOutcome());
										sequence.addDecision(letterGuess);
										heap.add(sequence);
									} // weight big enough to include
								} // next letter guess for this shape
							} finally {
								MONITOR.endTask();
							}
						} // next history in heap
					} // any more heaps?

					LetterSequence bestSequence = null;
					boolean isHoldover = false;
					MONITOR.startTask("best sequence");
					try {
						List<LetterSequence> finalSequences = new ArrayList<LetterSequence>();
						for (int i = 0; i < this.beamWidth; i++) {
							if (finalHeap.isEmpty())
								break;
							finalSequences.add(finalHeap.poll());
						}

						if (this.mostLikelyWordChooser == null) {
							// most likely sequence is on top of the last heap
							bestSequence = finalSequences.get(0);
						} else {
							// get most likely sequence using lexicon
							if (holdoverSequences != null) {
								// we have a holdover from the previous row
								// ending with a dash
								bestSequence = this.mostLikelyWordChooser.chooseMostLikelyWord(finalSequences, holdoverSequences, this.beamWidth);
							} else {
								// check if this is the last group on the row
								// and could end with
								// a dash
								boolean shouldBeHeldOver = false;
								if (group.getIndex() == row.getGroups().size() - 1 && row.getIndex() < paragraph.getRows().size() - 1) {
									for (LetterSequence letterSequence : finalSequences) {
										if (letterSequence.toString().endsWith("-")) {
											shouldBeHeldOver = true;
											break;
										}
									}
								}
								if (shouldBeHeldOver) {
									holdoverSequences = finalSequences;
									holdoverGroup = group;
									isHoldover = true;
								} else {
									// simplest case: no holdover
									bestSequence = this.mostLikelyWordChooser.chooseMostLikelyWord(finalSequences, this.beamWidth);
								}
							} // have we holdover sequences?
						} // have we a most likely word chooser?

						if (!isHoldover) {
							for (LetterGuessObserver observer : observers) {
								observer.onBeamSearchEnd(bestSequence, finalSequences, holdoverSequences);
							}
						}
					} finally {
						MONITOR.endTask();
					}

					MONITOR.startTask("assign letter");
					try {
						if (!isHoldover) {
							for (LetterGuessObserver observer : observers) {
								observer.onStartSequence(bestSequence);
							}

							if (holdoverGroup == null) {
								group.setBestLetterSequence(bestSequence);
							} else {
								// split bestSequence by group
								List<LetterSequence> sequencesByGroup = bestSequence.splitByGroup();
								for (LetterSequence sequenceByGroup : sequencesByGroup) {
									if (sequenceByGroup.getGroups().get(0).equals(holdoverGroup))
										holdoverGroup.setBestLetterSequence(sequenceByGroup);
									else if (sequenceByGroup.getGroups().get(0).equals(group))
										group.setBestLetterSequence(sequenceByGroup);
								}
								holdoverSequences = null;
								holdoverGroup = null;
							}

							int i = 0;
							for (ShapeInSequence shapeInSequence : bestSequence.getUnderlyingShapeSequence()) {
								String bestOutcome = bestSequence.getLetters().get(i);
								this.assignLetter(shapeInSequence, bestOutcome);
								i++;
							} // next shape

							for (LetterGuessObserver observer : observers) {
								observer.onGuessSequence(bestSequence);
							}
						}

						this.shapeCount += group.getShapes().size();
						if (this.currentMonitor != null) {
							double progress = (double) shapeCount / (double) totalShapeCount;
							LOG.debug("progress: " + progress);
							currentMonitor.setPercentComplete(progress);
						}
					} finally {
						MONITOR.endTask();
					}
				} // next group
			} // next row
		} // next paragraph

		for (LetterGuessObserver observer : observers) {
			observer.onImageEnd();
		}
	}

	private void assignLetter(ShapeInSequence shapeInSequence, String bestGuess) {
		for (LetterGuessObserver observer : observers) {
			observer.onGuessLetter(shapeInSequence, bestGuess);
		}

	}

	@Override
	public ProgressMonitor monitorTask() {
		currentMonitor = new SimpleProgressMonitor();
		return currentMonitor;
	}

	@Override
	public void addObserver(LetterGuessObserver letterGuessObserver) {
		this.observers.add(letterGuessObserver);
	}

	@Override
	public int getBeamWidth() {
		return beamWidth;
	}

	@Override
	public double getMinOutcomeWeight() {
		return minOutcomeWeight;
	}

	@Override
	public void onDocumentStart(JochreDocument jochreDocument) {
	}

	@Override
	public void onPageStart(JochrePage jochrePage) {
	}

	@Override
	public void onImageStart(JochreImage jochreImage) {
	}

	@Override
	public void onImageComplete(JochreImage jochreImage) {
		this.analyseInternal(jochreImage);
	}

	@Override
	public void onPageComplete(JochrePage jochrePage) {
	}

	@Override
	public void onDocumentComplete(JochreDocument jochreDocument) {
		for (LetterGuessObserver observer : observers) {
			observer.onFinish();
		}
	}
}
