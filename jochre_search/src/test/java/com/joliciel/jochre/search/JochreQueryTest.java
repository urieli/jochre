package com.joliciel.jochre.search;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class JochreQueryTest {

  @Test
  public void test() throws IOException {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();

    String configId = "yiddish";

    JochreIndexBuilder builder = new JochreIndexBuilder(configId, false);
    builder.updateIndex();

    JochreSearchManager manager = JochreSearchManager.getInstance(configId);

    IndexSearcher indexSearcher = manager.getManager().acquire();
    try {
      JochreQuery query = new JochreQuery(configId, "קײנער", Arrays.asList("שלום עליכם"), true, "", null, null, false,
          null);
      JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, configId);
      Pair<TopDocs, Integer> results = searcher.search(query, 0, 100);
      assertEquals(1, results.getRight().intValue());

      query = new JochreQuery(configId, "קײנער", Arrays.asList("שלום עליכם"), false, "", null, null, false, null);
      results = searcher.search(query, 0, 100);
      assertEquals(0, results.getRight().intValue());

      query = new JochreQuery(configId, "קײנער", new ArrayList<>(), false, "", 1917, 1917, false, null);
      results = searcher.search(query, 0, 100);
      assertEquals(1, results.getRight().intValue());

      query = new JochreQuery(configId, "קײנער", new ArrayList<>(), false, "", 1918, 1920, false, null);
      results = searcher.search(query, 0, 100);
      assertEquals(0, results.getRight().intValue());

      query = new JochreQuery(configId, "קײנער", new ArrayList<>(), false, "", null, null, false, "nybc200089");
      results = searcher.search(query, 0, 100);
      assertEquals(1, results.getRight().intValue());

      query = new JochreQuery(configId, "קײנער", new ArrayList<>(), false, "", null, null, false, "nybc200088");
      results = searcher.search(query, 0, 100);
      assertEquals(0, results.getRight().intValue());

    } finally {
      manager.getManager().release(indexSearcher);
    }
  }

}
