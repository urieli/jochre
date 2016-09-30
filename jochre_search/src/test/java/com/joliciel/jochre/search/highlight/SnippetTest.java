package com.joliciel.jochre.search.highlight;

import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

public class SnippetTest {
	private static final Logger LOG = LoggerFactory.getLogger(SnippetTest.class);

	@Test
	public void testSnippetString() {
		String json =
			"{"
			+ "	\"docId\": 8,"
			+ "	\"field\": \"text\","
			+ "	\"start\": 974,"
			+ "	\"end\": 1058,"
			+ "	\"score\": 2.4079,"
			+ "	\"terms\": [{"
			+ "		\"start\": 982,"
			+ "		\"end\": 984,"
			+ "		\"weight\": 1.204"
			+ "	},"
			+ "	{"
			+ "		\"start\": 1052,"
			+ "		\"end\": 1054,"
			+ "		\"weight\": 1.212"
			+ "	}]"
			+ "}";
		
		Snippet snippet = new Snippet(json);
		assertEquals(8, snippet.getDocId());
		assertEquals("text", snippet.getField());
		assertEquals(974, snippet.getStartOffset());
		assertEquals(1058, snippet.getEndOffset());
		assertEquals(2.4079, snippet.getScore(), 0.0001);
		assertEquals(2, snippet.getHighlightTerms().size());
		HighlightTerm term = snippet.getHighlightTerms().get(0);
		assertEquals(982, term.getStartOffset());
		assertEquals(984, term.getEndOffset());
		assertEquals(1.204, term.getWeight(), 0.0001);
		term = snippet.getHighlightTerms().get(1);
		assertEquals(1052, term.getStartOffset());
		assertEquals(1054, term.getEndOffset());
		assertEquals(1.212, term.getWeight(), 0.0001);
		
		String jsonOut = snippet.toJson();
		LOG.debug(jsonOut);
		snippet = new Snippet(jsonOut);
		assertEquals(8, snippet.getDocId());
		assertEquals("text", snippet.getField());
		assertEquals(974, snippet.getStartOffset());
		assertEquals(1058, snippet.getEndOffset());
		assertEquals(2.41, snippet.getScore(), 0.0001);
		assertEquals(2, snippet.getHighlightTerms().size());
		term = snippet.getHighlightTerms().get(0);
		assertEquals(982, term.getStartOffset());
		assertEquals(984, term.getEndOffset());
		assertEquals(1.20, term.getWeight(), 0.0001);
		term = snippet.getHighlightTerms().get(1);
		assertEquals(1052, term.getStartOffset());
		assertEquals(1054, term.getEndOffset());
		assertEquals(1.21, term.getWeight(), 0.0001);
		
	}

}
