package com.joliciel.jochre.search;

import java.io.File;
import java.io.Writer;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;

/**
 * A reusable index searcher tied to a given index directory.
 * @author Assaf Urieli
 *
 */
public interface JochreIndexSearcher {
	/**
	 * The directory in which the original PDF files are contained.
	 * @return
	 */
	public File getContentDir();
	
	/**
	 * Return a list of Lucene docIds and scores corresponding to a given query.
	 * @param cfhQuery
	 * @return
	 */
	public TopDocs search(JochreQuery query);
	
	/**
	 * Find all documents corresponding to a given name.
	 * @param name
	 * @return
	 */
	public Map<Integer,Document> findDocuments(String name);
	
	/**
	 * Find the documents for a given name and index.
	 * @param name
	 * @param index
	 * @return
	 */
	public Map<Integer,Document> findDocument(String name, int index);

	/**
	 * Write query results in JSON to the provided Writer.
	 * @param cfhQuery
	 * @param out
	 * @return the number of results
	 */
	public int search(JochreQuery query, Writer out);

	public File getIndexDir();

	public IndexSearcher getIndexSearcher();

}