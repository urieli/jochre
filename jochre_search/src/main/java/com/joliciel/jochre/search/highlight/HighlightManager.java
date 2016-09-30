///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
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
package com.joliciel.jochre.search.highlight;

import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import com.joliciel.jochre.utils.Either;

public interface HighlightManager {
	/**
	 * Write a JSON output of terms to highlight for a given set of docIds.
	 * 
	 * @param highlighter
	 *            The highlighter to be used to find the terms to highlight.
	 * @param docIds
	 *            The documents to highlight.
	 * @param out
	 *            The writer where the JSON should be written.
	 */
	public void highlight(Highlighter highlighter, Set<Integer> docIds, Set<String> fields, Writer out);

	/**
	 * Find highlight snippets for a set of documents. Each document either
	 * returns a list of snippets or an exception.
	 * 
	 * @param docIds
	 *            The documents to highlight and snip.
	 * @param termMap
	 *            The previously retrieved terms.
	 * @param maxSnippets
	 *            The maximum number of snippets.
	 * @param snippetSize
	 *            The approximate size of each snippet, in characters.
	 */
	public Map<Integer, Either<List<Snippet>, Exception>> findSnippets(Set<Integer> docIds, Set<String> fields,
			Map<Integer, NavigableSet<HighlightTerm>> termMap, int maxSnippets, int snippetSize);

	/**
	 * Write a JSON output of snippets for a given set of docIds.
	 * 
	 * @param highlighter
	 *            The highlighter to be used to find the terms to highlight.
	 * @param docIds
	 *            The documents to highlight and snip.
	 * @param out
	 *            The writer where the JSON should be written.
	 */
	public void findSnippets(Highlighter highlighter, Set<Integer> docIds, Set<String> fields, Writer out);

	/**
	 * Get a textual representation of a document with terms highlighted.
	 * 
	 */
	public String displayHighlights(int docId, String field, Set<HighlightTerm> terms);

	/**
	 * Get a textual representation of the snippet.
	 */
	public String displaySnippet(Snippet snippet);

	/**
	 * How many decimal places to write in the JSON.
	 */
	public int getDecimalPlaces();

	public void setDecimalPlaces(int decimalPlaces);

	/**
	 * The snippet finder (a default is used if none provided).
	 */
	public SnippetFinder getSnippetFinder();

	public void setSnippetFinder(SnippetFinder snippetFinder);

	/**
	 * The highlight term decorator (a default is used if none provided).
	 */
	public HighlightTermDecorator getDecorator();

	public void setDecorator(HighlightTermDecorator decorator);

	/**
	 * Whether or not the text should be included in the JSON output, along with
	 * the offsets.
	 */
	public boolean isIncludeText();

	public void setIncludeText(boolean includeText);

	public boolean isIncludeGraphics();

	public void setIncludeGraphics(boolean includeGraphics);

	/**
	 * The minimum weight to display a highlighted term.
	 */
	public double getMinWeight();

	public void setMinWeight(double minWeight);

	/**
	 * How many title snippets do we want? Default is 1.
	 */
	public int getTitleSnippetCount();

	public void setTitleSnippetCount(int titleSnippetCount);

	/**
	 * How many narrative snippets to we want? Default is 3.
	 */
	public int getSnippetCount();

	public void setSnippetCount(int snippetCount);

	/**
	 * Approximate snippet size. Default is 80.
	 */
	public int getSnippetSize();

	public void setSnippetSize(int snippetSize);

	public ImageSnippet getImageSnippet(Snippet snippet);

}