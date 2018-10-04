package com.joliciel.jochre.search;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

public class JochreSearcher extends IndexSearcher {
	private final int bookCount;

	public JochreSearcher(IndexReader reader, JochreSearchConfig config) throws IOException {
		super(reader);
		File contentDir = config.getContentDir();
		bookCount = (int) (Files.find(contentDir.toPath(), 1, // how deep do we want to descend
				(path, attributes) -> attributes.isDirectory()).count() - 1);
	}

	/**
	 * The number of books indexed by this searcher.
	 */
	public int getBookCount() {
		return bookCount;
	}
}
