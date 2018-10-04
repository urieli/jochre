package com.joliciel.jochre.search;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherFactory;

public class JochreSearcherFactory extends SearcherFactory {
	private final JochreSearchConfig config;

	public JochreSearcherFactory(JochreSearchConfig config) {
		this.config = config;
	}

	@Override
	public IndexSearcher newSearcher(IndexReader reader, IndexReader previousReader) throws IOException {
		return new JochreSearcher(reader, config);
	}

}
