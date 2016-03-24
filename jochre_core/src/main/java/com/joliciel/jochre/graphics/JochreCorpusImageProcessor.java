package com.joliciel.jochre.graphics;

import java.util.List;

import com.joliciel.jochre.doc.DocumentObserver;

/**
 * An interface used for processing pages within an existing Jochre database Corpus,
 * via a set of document observers.
 * @author Assaf Urieli
 *
 */
public interface JochreCorpusImageProcessor extends JochreCorpusReader {

	public void process();

	public List<DocumentObserver> getObservers();
	public void addObserver(DocumentObserver observer);

}