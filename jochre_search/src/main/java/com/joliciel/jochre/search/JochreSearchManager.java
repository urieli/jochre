package com.joliciel.jochre.search;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JochreSearchManager {
	private static final Logger LOG = LoggerFactory.getLogger(JochreSearchManager.class);
	private static final String IN_MEMORY = "IN-MEMORY";
	private final SearcherManager manager;
	private final Directory indexDir;

	private static JochreSearchManager instance;

	public static JochreSearchManager getInstance(JochreSearchConfig config) {
		if (instance == null)
			instance = new JochreSearchManager(config);
		return instance;
	}

	private JochreSearchManager(JochreSearchConfig config) {
		try {
			String indexDirLocation = config.getConfig().getString("index-dir");
			LOG.info("Loading index from: " + indexDirLocation);
			if (indexDirLocation.equals(IN_MEMORY)) {
				File tempFile = File.createTempFile("index", ".tmp");
				tempFile.delete();
				indexDir = new MMapDirectory(tempFile.toPath());
			} else {
				Path indexDirPath = new File(indexDirLocation).toPath();
				indexDir = FSDirectory.open(indexDirPath, new SingleInstanceLockFactory());
			}

			SearcherManager manager = null;
			SearcherFactory searcherFactory = new JochreSearcherFactory(config);
			try {
				manager = new SearcherManager(indexDir, searcherFactory);
			} catch (IndexNotFoundException infe) {
				// brand new index
				StandardAnalyzer analyzer = new StandardAnalyzer();
				IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
				IndexWriter indexWriter = new IndexWriter(indexDir, iwc);
				Document doc = new Document();
				indexWriter.addDocument(doc);
				indexWriter.commit();
				indexWriter.close();
				manager = new SearcherManager(indexDir, searcherFactory);
			}
			this.manager = manager;
		} catch (IOException e) {
			throw new RuntimeException("Unable to open index directories", e);
		}
	}

	public SearcherManager getManager() {
		return manager;
	}

	public Directory getIndexDir() {
		return indexDir;
	}

}
