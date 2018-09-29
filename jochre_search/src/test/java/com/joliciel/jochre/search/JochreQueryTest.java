package com.joliciel.jochre.search;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

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
			JochreQuery query = new JochreQuery(config, "קײנער", Arrays.asList("שלום עליכם"), true, "");
			JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, config);
			TopDocs topDocs = searcher.search(query);
			assertEquals(1, topDocs.totalHits);

			query = new JochreQuery(config, "קײנער", Arrays.asList("שלום עליכם"), false, "");
			topDocs = searcher.search(query);
			assertEquals(0, topDocs.totalHits);

		} finally {
			manager.getManager().release(indexSearcher);
		}
	}

}
