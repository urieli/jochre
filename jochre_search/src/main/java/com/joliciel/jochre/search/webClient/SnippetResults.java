package com.joliciel.jochre.search.webClient;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.joliciel.jochre.search.highlight.Snippet;

public class SnippetResults {
	private static final Log LOG = LogFactory.getLog(SearchResults.class);
	
	private Map<Integer,List<Snippet>> snippetMap = new HashMap<Integer, List<Snippet>>();
	
	public SnippetResults(String json) {
		try {
			LOG.debug("Reading snippets from: " + json);
			Reader reader = new StringReader(json);
			JsonFactory jsonFactory = new JsonFactory(); // or, for data binding, org.codehaus.jackson.mapper.MappingJsonFactory 
			JsonParser jsonParser = jsonFactory.createJsonParser(reader); 
			// Sanity check: verify that we got "Json Object":
			if (jsonParser.nextToken() != JsonToken.START_OBJECT)
				throw new RuntimeException("Expected START_OBJECT, but was " + jsonParser.getCurrentToken() + " at " + jsonParser.getCurrentLocation());
			while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
				
				String baseName = jsonParser.getCurrentName();
				LOG.debug("Found baseName: " + baseName);
				if (jsonParser.nextToken() != JsonToken.START_OBJECT)
					throw new RuntimeException("Expected START_OBJECT, but was " + jsonParser.getCurrentToken() + " at " + jsonParser.getCurrentLocation());
	
				int docId = 0;
				List<Snippet> snippets = new ArrayList<Snippet>();
				while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
					String fieldName = jsonParser.getCurrentName();
				
					if (fieldName.equals("docId")) {
						docId = jsonParser.nextIntValue(0);
					} else if (fieldName.equals("snippets")) {
						if (jsonParser.nextToken() != JsonToken.START_ARRAY)
							throw new RuntimeException("Expected START_ARRAY, but was " + jsonParser.getCurrentToken() + " at " + jsonParser.getCurrentLocation());
						while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
							Snippet snippet = new Snippet(jsonParser);
							snippets.add(snippet);
						}
					}
				}
				
				snippetMap.put(docId, snippets);
			} // next scoreDoc
		} catch (JsonParseException e) {
			LOG.error(e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LOG.error(e);
			throw new RuntimeException(e);
		}
	}

	public Map<Integer, List<Snippet>> getSnippetMap() {
		return snippetMap;
	}
}
