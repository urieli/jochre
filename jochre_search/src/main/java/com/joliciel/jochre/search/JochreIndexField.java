package com.joliciel.jochre.search;

/**
 * Different fields indexed with each Jochre Document.
 * 
 * @author Assaf Urieli
 *
 */
public enum JochreIndexField {
	name,
	path,
	sectionNumber,
	startPage,
	endPage,
	text,
	indexTime,
	author,
	title,
	publisher,
	date,
	authorLang,
	titleLang,
	id,
	url,
	volume,
	length,
	/** Prefix for each row's rectangle. */
	rect,
	/** Prefix for each row's start position in characters. */
	start,
	/** Prefix for the number of rows in each page */
	rowCount;
}
