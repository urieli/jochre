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
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.boundaries.BoundaryDetector;
import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.JochreCorpusImageReader;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.jochre.letterGuesser.Letter;
import com.joliciel.jochre.letterGuesser.LetterGuesser;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.jochre.lexicon.MostLikelyWordChooser;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.Monitorable;
import com.joliciel.talismane.utils.PerformanceMonitor;
import com.joliciel.talismane.utils.ProgressMonitor;
import com.joliciel.talismane.utils.SimpleProgressMonitor;

/**
 * Perform a analysis using a beam search.
 * @author Assaf Urieli
 *
 */
class BeamSearchImageAnalyser implements ImageAnalyser, Monitorable {
    private static final Log LOG = LogFactory.getLog(BeamSearchImageAnalyser.class);
    
    private static final int DEFAULT_BEAM_WIDTH = 5;
    
	private AnalyserServiceInternal analyserServiceInternal;
	private LetterGuesserService letterGuesserService;
	private GraphicsService graphicsService;
	private BoundaryService boundaryService;

	private MostLikelyWordChooser mostLikelyWordChooser;
	private BoundaryDetector boundaryDetector;
	
	private int beamWidth = DEFAULT_BEAM_WIDTH;
	private double minOutcomeWeight = 0;	

	private int shapeCount = 0;
	private int totalShapeCount = -1;
	
	private SimpleProgressMonitor currentMonitor;
	
	private List<LetterGuessObserver> observers = new ArrayList<LetterGuessObserver>();
	
	public BeamSearchImageAnalyser(int beamWidth, double minOutcomeWeight) {
		if (beamWidth>0)
			this.beamWidth = beamWidth;
		this.minOutcomeWeight = minOutcomeWeight;
	}
	
	@Override
	public void analyse(LetterGuesser letterGuesser, JochreCorpusImageReader imageReader) {
		PerformanceMonitor.startTask("BeamSearchImageAnalyser.analyseAll");
		try {
			
			while (imageReader.hasNext()) {
				JochreImage image = imageReader.next();
				this.analyseInternal(letterGuesser, image);
				image.clearMemory();
			} // next image
	
			for (LetterGuessObserver observer : observers) {
				observer.onFinish();
			}
		} finally {
			PerformanceMonitor.endTask("BeamSearchImageAnalyser.analyseAll");
		}
	}
	
	@Override
	public void analyse(LetterGuesser letterGuesser,
			JochreImage image) {
		PerformanceMonitor.startTask("BeamSearchImageAnalyser.analyse");
		try {

			this.analyseInternal(letterGuesser, image);
			image.clearMemory();
			
			for (LetterGuessObserver observer : observers) {
				observer.onFinish();
			}
		} finally {
			PerformanceMonitor.endTask("BeamSearchImageAnalyser.analyse");
		}
	}
	
	public void analyseInternal(LetterGuesser letterGuesser,
			JochreImage image) {
		LOG.debug("Analysing image " + image.getId());
		if (currentMonitor!=null) {
			currentMonitor.setCurrentAction("imageMonitor.analysingImage", new Object[] {image.getPage().getIndex()});
		}
		for (LetterGuessObserver observer : observers) {
			observer.onImageStart(image);
		}
		if (totalShapeCount<0)
			totalShapeCount = image.getShapeCount();

		for (Paragraph paragraph : image.getParagraphs()) {
			LOG.debug("Analysing paragraph " + paragraph.getIndex() + " (id=" + paragraph.getId() + ")");
			List<LetterSequence> holdoverSequences = null;
			for (RowOfShapes row: paragraph.getRows()) {
				LOG.debug("Analysing row " + row.getIndex() + " (id=" + row.getId() + ")");
				for (GroupOfShapes group : row.getGroups()) {
					LOG.debug("Analysing group " + group.getIndex() + " (id=" + group.getId() + ")");
					
					int width = group.getRight() - group.getLeft() + 1;
					
					List<ShapeSequence> shapeSequences = null;
					if (boundaryDetector!=null) {
						shapeSequences = boundaryDetector.findBoundaries(group);
					} else {
						// simply add this groups shape's
						shapeSequences = new ArrayList<ShapeSequence>();
						ShapeSequence shapeSequence = boundaryService.getEmptyShapeSequence();
						for (Shape shape : group.getShapes())
							shapeSequence.addShape(shape);
						shapeSequences.add(shapeSequence);
					}
					
					// Perform a beam search to guess the most likely sequence for this word
					TreeMap<Integer, PriorityQueue<LetterSequence>> heaps = new TreeMap<Integer, PriorityQueue<LetterSequence>>();

					// prime a starter heap with the n best shape boundary analyses for this group
					PriorityQueue<LetterSequence> starterHeap = new PriorityQueue<LetterSequence>(1);
					for (ShapeSequence shapeSequence : shapeSequences) {
						LetterSequence emptySequence = this.getLetterGuesserService().getEmptyLetterSequence(shapeSequence);
						starterHeap.add(emptySequence);
					}
					heaps.put(0, starterHeap);
					
					PriorityQueue<LetterSequence> finalHeap = null;
					while (heaps.size()>0) {
						Entry<Integer, PriorityQueue<LetterSequence>> heapEntry = heaps.pollFirstEntry();
						if (LOG.isTraceEnabled())
							LOG.trace("heap for index: " + heapEntry.getKey().intValue() + ", width: " + width);
						if (heapEntry.getKey().intValue()==width) {
							finalHeap = heapEntry.getValue();
							break;
						}

						PriorityQueue<LetterSequence> previousHeap = heapEntry.getValue();

						// limit the breadth to K
						int maxSequences = previousHeap.size() > this.beamWidth ? this.beamWidth : previousHeap.size();
						
						for (int j = 0; j<maxSequences; j++) {
							LetterSequence history = previousHeap.poll();
							ShapeInSequence shapeInSequence = history.getNextShape();
							Shape shape = shapeInSequence.getShape();
							if (LOG.isTraceEnabled()) {
								LOG.trace("Sequence " + history + ", shape: " + shape);				
							}
							LogUtils.logMemory(LOG);
							int position = 0;
							if (Linguistics.getInstance(image.getPage().getDocument().getLocale()).isLeftToRight()) {
								position = shape.getRight() - group.getLeft() + 1;
							} else {
								position = group.getRight() - shape.getLeft() + 1;
							}
							PriorityQueue<LetterSequence> heap = heaps.get(position);
							if (heap==null) {
								heap = new PriorityQueue<LetterSequence>();
								heaps.put(position, heap);
							}
							
							PerformanceMonitor.startTask("guess letter");
							try {
								letterGuesser.guessLetter(shapeInSequence, history);
							} finally {
								PerformanceMonitor.endTask("guess letter");
							}
							
							PerformanceMonitor.startTask("heap sort");
							try {
								for (Decision<Letter> letterGuess : shape.getLetterGuesses()) {
									// leave out very low probability outcomes
									if (letterGuess.getProbability() > this.minOutcomeWeight) {
										LetterSequence sequence = this.getLetterGuesserService().getLetterSequencePlusOne(history);
										sequence.add(letterGuess.getOutcome());
										sequence.addDecision(letterGuess);
										heap.add(sequence);
									} // weight big enough to include
								} // next letter guess for this shape
							} finally {
								PerformanceMonitor.endTask("heap sort");
							}
						} // next history in heap
					} // any more heaps?
					
					LetterSequence bestSequence = null;
					boolean shouldCombineWithHoldover = false;
					boolean isHoldover = false;
					PerformanceMonitor.startTask("best sequence");
					try {
						if (this.getMostLikelyWordChooser()==null) {
							// most likely sequence is on top of the last heap
							bestSequence = finalHeap.peek();
						} else {
							// get most likely sequence as adjusted for word frequencies
							List<LetterSequence> finalSequences = new ArrayList<LetterSequence>();
							for (int i=0;i<this.beamWidth;i++) {
								if (finalHeap.isEmpty())
									break;
								finalSequences.add(finalHeap.poll());
							}
							
							if (holdoverSequences!=null) {
								// we have a holdover from the previous row ending with a dash
								bestSequence = this.getMostLikelyWordChooser().chooseMostLikelyWord(finalSequences, holdoverSequences, this.beamWidth);
								shouldCombineWithHoldover = true;
							} else {
								// check if this is the last group on the row and could end with a dash
								boolean shouldBeHeldOver = false;
								if (group.getIndex()==row.getGroups().size()-1
										&& row.getIndex()<paragraph.getRows().size()-1) {
									for (LetterSequence letterSequence : finalSequences) {
										if (letterSequence.toString().endsWith("-")) {
											shouldBeHeldOver = true;
											break;
										}
									}
								}
								if (shouldBeHeldOver) {
									holdoverSequences = finalSequences;
									isHoldover = true;
								} else {
									// simplest case: no holdover
									bestSequence = this.getMostLikelyWordChooser().chooseMostLikelyWord(finalSequences, this.beamWidth);
								}
							}
						}
					} finally {
						PerformanceMonitor.endTask("best sequence");
					}
					
					PerformanceMonitor.startTask("assign letter");
					try {
						if (shouldCombineWithHoldover) {
							holdoverSequences = null;
						}
						if (!isHoldover) {
							for (LetterGuessObserver observer : observers) {
								observer.onStartSequence(bestSequence);
							}
	
							int i = 0;
							for (ShapeInSequence shapeInSequence : bestSequence.getUnderlyingShapeSequence()) {
								String bestOutcome = bestSequence.get(i).getString();
								this.assignLetter(shapeInSequence, bestOutcome);
								i++;
							} // next shape
							
							for (LetterGuessObserver observer : observers) {
								observer.onGuessSequence(bestSequence);
							}
						}
						
						this.shapeCount += group.getShapes().size();
						if (this.currentMonitor!=null) {
							double progress = (double) shapeCount / (double) totalShapeCount;
							LOG.debug("progress: " + progress);
							currentMonitor.setPercentComplete(progress);
						}
					} finally {
						PerformanceMonitor.endTask("assign letter");
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
	
	public AnalyserServiceInternal getAnalyserServiceInternal() {
		return analyserServiceInternal;
	}
	public void setAnalyserServiceInternal(
			AnalyserServiceInternal analyserServiceInternal) {
		this.analyserServiceInternal = analyserServiceInternal;
	}
	public GraphicsService getGraphicsService() {
		return graphicsService;
	}
	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}


	public MostLikelyWordChooser getMostLikelyWordChooser() {
		return mostLikelyWordChooser;
	}

	public void setMostLikelyWordChooser(MostLikelyWordChooser mostLikelyWordChooser) {
		this.mostLikelyWordChooser = mostLikelyWordChooser;
	}

	public LetterGuesserService getLetterGuesserService() {
		return letterGuesserService;
	}

	public void setLetterGuesserService(LetterGuesserService letterGuesserService) {
		this.letterGuesserService = letterGuesserService;
	}

	public BoundaryService getBoundaryService() {
		return boundaryService;
	}

	public void setBoundaryService(BoundaryService boundaryService) {
		this.boundaryService = boundaryService;
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
	public BoundaryDetector getBoundaryDetector() {
		return boundaryDetector;
	}

	@Override
	public void setBoundaryDetector(BoundaryDetector boundaryDetector) {
		this.boundaryDetector = boundaryDetector;
	}
	
	
}
