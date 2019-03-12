package com.joliciel.jochre.search;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.ConfigFactory;

public class JochreIndexBuilderTest {
  private static final Logger LOG = LoggerFactory.getLogger(JochreIndexBuilderTest.class);

  @Test
  public void testUpdateIndex() throws IOException {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();

    String configId = "yiddish";
    JochreIndexBuilder builder = new JochreIndexBuilder(configId, false);
    builder.updateIndex();

    JochreSearchManager manager = JochreSearchManager.getInstance(configId);
    IndexSearcher indexSearcher = manager.getManager().acquire();
    try {
      JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, configId);
      Map<Integer, Document> docs = searcher.findDocuments("MotlPeysiDemKhazns");
      assertEquals(1, docs.size());
      int docId = docs.keySet().iterator().next();
      JochreIndexDocument jochreDoc = new JochreIndexDocument(indexSearcher, docId, configId);
      assertEquals("MotlPeysiDemKhazns", jochreDoc.getName());
      LOG.debug(jochreDoc.toString());

      JochreQuery query = new JochreQuery(configId, "זיך");
      Pair<TopDocs, Integer> results = searcher.search(query, 0, 100);
      assertEquals(1, results.getRight().intValue());
      for (ScoreDoc scoreDoc : results.getLeft().scoreDocs) {
        jochreDoc = new JochreIndexDocument(indexSearcher, scoreDoc.doc, configId);
        assertEquals("MotlPeysiDemKhazns", jochreDoc.getName());
      }
    } finally {
      manager.getManager().release(indexSearcher);
    }
  }

}
