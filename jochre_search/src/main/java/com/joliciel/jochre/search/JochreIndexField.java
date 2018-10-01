package com.joliciel.jochre.search;

/**
 * Different fields indexed with each Jochre Document.
 * 
 * @author Assaf Urieli
 *
 */
public enum JochreIndexField {
	/** Name of the folder containing the book */
	name,
	/** Path to the folder containing the book */
	path,
	/**
	 * Section number from 0 to n, in the case where the book is broken into
	 * multiple sections
	 */
	sectionNumber,
	/** Physical start page of the current section */
	startPage,
	/** Physical end page of the current section */
	endPage,
	/** Tokenised contents of the current section */
	text,
	/** Time when this book was last indexed */
	indexTime,
	/** Full author name (not tokenised) in English */
	authorEnglish,
	/** Tokenised title in English */
	titleEnglish,
	/** Untokenised publisher */
	publisher,
	/** Year stored as a string for retrieval */
	date,
	/** Year stored as a number for range queries */
	year,
	/** The year to be used for sorting */
	yearSort,
	/** Full author name (not tokenised) in the original language */
	author,
	/** Tokenised title in the original language */
	title,
	/** Unique ID of this work in some external catalog system */
	id,
	/** URL where the book may be browsed on the Internet */
	url,
	/** Volume for multi-volume works */
	volume,
	/** Length of current section in characters */
	length,
	/** Prefix for each row's rectangle. */
	rect,
	/** Prefix for each row's start position in characters. */
	start,
	/** Prefix for the number of rows in each page */
	rowCount;
}
