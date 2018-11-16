package com.joliciel.jochre.search;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class FieldTermPrefixFinderTest {
  private static final Logger LOG = LoggerFactory.getLogger(FieldTermPrefixFinderTest.class);

  @Test
  public void test() throws IOException {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();

    Config testConfig = ConfigFactory.load();
    JochreSearchConfig config = new JochreSearchConfig("yiddish", testConfig);
    JochreSearchManager manager = JochreSearchManager.getInstance(config);

    Analyzer analyzer = new StandardAnalyzer();
    IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
    iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
    IndexWriter indexWriter = new IndexWriter(manager.getIndexDir(), iwc);
    Document doc = new Document();
    doc.add(new StringField("food", "apple", Store.YES));
    indexWriter.addDocument(doc);

    doc = new Document();
    doc.add(new StringField("food", "artichoke", Store.YES));
    indexWriter.addDocument(doc);

    doc = new Document();
    doc.add(new StringField("food", "artichoke", Store.YES));
    indexWriter.addDocument(doc);

    doc = new Document();
    doc.add(new StringField("food", "apple pie", Store.YES));
    indexWriter.addDocument(doc);

    doc = new Document();
    doc.add(new StringField("food", "apple pie", Store.YES));
    indexWriter.addDocument(doc);

    doc = new Document();
    doc.add(new StringField("food", "banana", Store.YES));
    indexWriter.addDocument(doc);

    indexWriter.commit();
    indexWriter.close();

    manager.getManager().maybeRefresh();

    IndexSearcher searcher = manager.getManager().acquire();
    try {
      FieldTermPrefixFinder finder = new FieldTermPrefixFinder(searcher, "food", "a", 2, config);
      List<String> results = finder.getResults();
      LOG.debug("results: " + results);
      assertEquals(2, results.size());
      assertEquals("apple pie", results.get(0));
      assertEquals("artichoke", results.get(1));

      finder = new FieldTermPrefixFinder(searcher, "food", "ap", 2, config);
      results = finder.getResults();
      LOG.debug("results: " + results);
      assertEquals(2, results.size());
      assertEquals("apple", results.get(0));
      assertEquals("apple pie", results.get(1));

    } finally {
      manager.getManager().release(searcher);
    }
  }

}
