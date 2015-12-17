///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.jochre.search.webClient;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class SearchResults {
	private static final Log LOG = LogFactory.getLog(SearchResults.class);
	private List<SearchDocument> scoreDocs;
	
	public SearchResults(String json) {
		try {
			if (LOG.isTraceEnabled()) {
				LOG.trace(json);
			}
			scoreDocs = new ArrayList<SearchDocument>();
			Reader reader = new StringReader(json);
			JsonFactory jsonFactory = new JsonFactory(); // or, for data binding, org.codehaus.jackson.mapper.MappingJsonFactory 
			JsonParser jsonParser = jsonFactory.createJsonParser(reader); 
			// Sanity check: verify that we got "Json Object":
			if (jsonParser.nextToken() != JsonToken.START_ARRAY)
				throw new RuntimeException("Expected " + JsonToken.START_ARRAY + ", but was " + jsonParser.getCurrentToken() + " at " + jsonParser.getCurrentLocation());
			while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
				LOG.trace(jsonParser.getCurrentToken());
				LOG.trace(jsonParser.getCurrentName());
				LOG.trace(jsonParser.getCurrentLocation());
				if (jsonParser.getCurrentToken() != JsonToken.START_OBJECT)
					throw new RuntimeException("Expected " + JsonToken.START_OBJECT + ", but was " + jsonParser.getCurrentToken() + " at " + jsonParser.getCurrentLocation());


				SearchDocument doc = new SearchDocument(jsonParser);
				scoreDocs.add(doc);
				
			} // next scoreDoc
		} catch (JsonParseException e) {
			LOG.error(e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LOG.error(e);
			throw new RuntimeException(e);
		}
	}

	public List<SearchDocument> getScoreDocs() {
		return scoreDocs;
	}
	
}
