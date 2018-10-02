package com.joliciel.jochre.search;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class JochreIndexBuilderTest {
	private static final Logger LOG = LoggerFactory.getLogger(JochreIndexBuilderTest.class);

	@Test
	public void testUpdateIndex() throws IOException {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();

		Config testConfig = ConfigFactory.load();
		JochreSearchConfig config = new JochreSearchConfig("yiddish", testConfig);
		JochreSearchManager manager = JochreSearchManager.getInstance(config);
		SearchStatusHolder searchStatusHolder = SearchStatusHolder.getInstance();

		JochreIndexBuilder builder = new JochreIndexBuilder(config, manager, false, null, searchStatusHolder);
		builder.updateIndex();

		IndexSearcher indexSearcher = manager.getManager().acquire();
		try {
			JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, config);
			Map<Integer, Document> docs = searcher.findDocuments("MotlPeysiDemKhazns");
			assertEquals(1, docs.size());
			int docId = docs.keySet().iterator().next();
			JochreIndexDocument jochreDoc = new JochreIndexDocument(indexSearcher, docId, config);
			assertEquals("MotlPeysiDemKhazns", jochreDoc.getName());
			LOG.debug(jochreDoc.toString());

			JochreQuery query = new JochreQuery(config, "זיך");
			TopDocs topDocs = searcher.search(query);
			assertEquals(1, topDocs.totalHits);
			for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
				jochreDoc = new JochreIndexDocument(indexSearcher, scoreDoc.doc, config);
				assertEquals("MotlPeysiDemKhazns", jochreDoc.getName());
			}
		} finally {
			manager.getManager().release(indexSearcher);
		}
	}

}
