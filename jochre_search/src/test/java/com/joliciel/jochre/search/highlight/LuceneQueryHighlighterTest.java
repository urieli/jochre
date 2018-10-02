package com.joliciel.jochre.search.highlight;

import static org.junit.Assert.assertEquals;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochreIndexBuilder;
import com.joliciel.jochre.search.JochreIndexField;
import com.joliciel.jochre.search.JochreIndexSearcher;
import com.joliciel.jochre.search.JochreQuery;
import com.joliciel.jochre.search.JochreSearchConfig;
import com.joliciel.jochre.search.JochreSearchManager;
import com.joliciel.jochre.search.SearchStatusHolder;
import com.joliciel.jochre.utils.Either;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class LuceneQueryHighlighterTest {
	private static final Logger LOG = LoggerFactory.getLogger(LuceneQueryHighlighterTest.class);

	@Test
	public void testFindSnippets() throws IOException {
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
			JochreQuery query = new JochreQuery(config, "קײנער");
			JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, config);
			Pair<TopDocs, Integer> results = searcher.search(query, 0, 100);
			int docId = results.getLeft().scoreDocs[0].doc;
			Set<Integer> docIds = new HashSet<>();
			docIds.add(docId);

			Set<String> fields = new HashSet<>();
			fields.add(JochreIndexField.text.name());

			LuceneQueryHighlighter highlighter = new LuceneQueryHighlighter(query, indexSearcher, fields);

			Map<Integer, NavigableSet<HighlightTerm>> terms = highlighter.highlight(docIds);
			NavigableSet<HighlightTerm> myTerms = terms.get(docId);
			assertEquals(1, myTerms.size());

			int i = 0;
			for (HighlightTerm term : myTerms) {
				LOG.debug(term.toString());
				if (i == 0) {
					assertEquals(20, term.getPosition());
					assertEquals(108, term.getStartOffset());
					assertEquals(114, term.getEndOffset());
				}
				i++;
			}

			HighlightManager highlightManager = new HighlightManager(indexSearcher, fields, config);
			Map<Integer, Either<List<Snippet>, Exception>> snippets = highlightManager.findSnippets(docIds, terms, 2);
			List<Snippet> mySnippets = snippets.get(docId).getLeft();
			assertEquals(1, mySnippets.size());

			for (Snippet snippet : mySnippets) {
				LOG.debug(snippet.toString());
				assertEquals(1, snippet.getHighlightTerms().size());
				String text = highlightManager.displaySnippet(snippet);
				LOG.debug(text);
				ImageSnippet imageSnippet = highlightManager.getImageSnippet(snippet);
				LOG.debug(imageSnippet.getRectangle().toString());
				assertEquals(new Rectangle(658, 2264, 2383, 363), imageSnippet.getRectangle());
				BufferedImage image = imageSnippet.getImage();
				assertEquals(2383, image.getWidth());
				assertEquals(363, image.getHeight());
			}

		} finally {
			manager.getManager().release(indexSearcher);
		}
	}

}
