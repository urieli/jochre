package com.joliciel.jochre.search;

import java.io.File;
import java.io.Writer;
import java.util.List;

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
	 * Return a list of Lucene docIds and scores corresponding to a given query.
	 * @param cfhQuery
	 * @return
	 */
	public TopDocs search(JochreQuery query);
	
	public List<Document> findDocuments(String docId);

	/**
	 * Write query results in JSON to the provided Writer.
	 * @param cfhQuery
	 * @param out
	 */
	public void search(JochreQuery query, Writer out);

	public File getIndexDir();

	public IndexSearcher getIndexSearcher();

}