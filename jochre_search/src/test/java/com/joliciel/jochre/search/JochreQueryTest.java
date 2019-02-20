package com.joliciel.jochre.search;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class JochreQueryTest {

  @Test
  public void test() throws IOException {
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
      JochreQuery query = new JochreQuery(config, "קײנער", Arrays.asList("שלום עליכם"), true, "", null, null, false,
          null);
      JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, config);
      Pair<TopDocs, Integer> results = searcher.search(query, 0, 100);
      assertEquals(1, results.getRight().intValue());

      query = new JochreQuery(config, "קײנער", Arrays.asList("שלום עליכם"), false, "", null, null, false, null);
      results = searcher.search(query, 0, 100);
      assertEquals(0, results.getRight().intValue());

      query = new JochreQuery(config, "קײנער", new ArrayList<>(), false, "", 1917, 1917, false, null);
      results = searcher.search(query, 0, 100);
      assertEquals(1, results.getRight().intValue());

      query = new JochreQuery(config, "קײנער", new ArrayList<>(), false, "", 1918, 1920, false, null);
      results = searcher.search(query, 0, 100);
      assertEquals(0, results.getRight().intValue());

      query = new JochreQuery(config, "קײנער", new ArrayList<>(), false, "", null, null, false, "nybc200089");
      results = searcher.search(query, 0, 100);
      assertEquals(1, results.getRight().intValue());

      query = new JochreQuery(config, "קײנער", new ArrayList<>(), false, "", null, null, false, "nybc200088");
      results = searcher.search(query, 0, 100);
      assertEquals(0, results.getRight().intValue());

    } finally {
      manager.getManager().release(indexSearcher);
    }
  }

}
