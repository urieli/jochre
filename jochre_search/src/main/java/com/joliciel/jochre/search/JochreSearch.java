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
package com.joliciel.jochre.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.joliciel.jochre.search.highlight.HighlightManager;
import com.joliciel.jochre.search.highlight.HighlightService;
import com.joliciel.jochre.search.highlight.HighlightServiceLocator;
import com.joliciel.jochre.search.highlight.Highlighter;
import com.joliciel.talismane.utils.LogUtils;

public class JochreSearch {
	private static final Log LOG = LogFactory.getLog(JochreSearch.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Map<String, String> argMap = new HashMap<String, String>();
			
			for (String arg : args) {
				int equalsPos = arg.indexOf('=');
				String argName = arg.substring(0, equalsPos);
				String argValue = arg.substring(equalsPos+1);
				argMap.put(argName, argValue);
			}
			
			String command = argMap.get("command");
			argMap.remove("command");
			
			String logConfigPath = argMap.get("logConfigFile");
			if (logConfigPath!=null) {
				argMap.remove("logConfigFile");
				Properties props = new Properties();
				props.load(new FileInputStream(logConfigPath));
				PropertyConfigurator.configure(props);
			}
			
			LOG.debug("##### Arguments:");
			for (Entry<String, String> arg : argMap.entrySet()) {
				LOG.debug(arg.getKey() + ": " + arg.getValue());
			}
			
			SearchServiceLocator locator = SearchServiceLocator.getInstance();
			SearchService searchService = locator.getSearchService();
			
			if (command.equals("buildIndex")) {
				String indexDirPath = argMap.get("indexDir");
				String documentDirPath = argMap.get("documentDir");
				File indexDir = new File(indexDirPath);
				indexDir.mkdirs();
				File documentDir = new File(documentDirPath);
				
				JochreIndexBuilder builder = searchService.getJochreIndexBuilder(indexDir);
				builder.updateDocument(documentDir);
			} else if (command.equals("updateIndex")) {
				String indexDirPath = argMap.get("indexDir");
				String documentDirPath = argMap.get("documentDir");
				boolean forceUpdate = false;
				if (argMap.containsKey("forceUpdate")) {
					forceUpdate = argMap.get("forceUpdate").equals("true");
				}
				File indexDir = new File(indexDirPath);
				indexDir.mkdirs();
				File documentDir = new File(documentDirPath);
				
				JochreIndexBuilder builder = searchService.getJochreIndexBuilder(indexDir);
				builder.updateIndex(documentDir, forceUpdate);
			} else if (command.equals("search")||command.equals("highlight")||command.equals("snippets")) {
				HighlightServiceLocator highlightServiceLocator = HighlightServiceLocator.getInstance(locator);
				HighlightService highlightService = highlightServiceLocator.getHighlightService();
				
				String indexDirPath = argMap.get("indexDir");
				File indexDir = new File(indexDirPath);
				JochreQuery query = searchService.getJochreQuery(argMap);
				
				JochreIndexSearcher searcher = searchService.getJochreIndexSearcher(indexDir);
				Writer out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
				if (command.equals("search")) {
					searcher.search(query, out);
				} else {
					TopDocs topDocs = searcher.search(query);
					
					Set<Integer> docIds = new LinkedHashSet<Integer>();
					for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
						docIds.add(scoreDoc.doc);
						LOG.debug("### Next document");
						Document doc = searcher.getIndexSearcher().doc(scoreDoc.doc);
						for (IndexableField field : doc.getFields()) {
							if (!field.name().equals("text"))
								LOG.debug(field);
						}
					}
					Set<String> fields = new HashSet<String>();
					fields.add("text");
					
					Highlighter highlighter = highlightService.getHighlighter(query, searcher.getIndexSearcher());
					HighlightManager highlightManager = highlightService.getHighlightManager(searcher.getIndexSearcher());
					highlightManager.setDecimalPlaces(query.getDecimalPlaces());
					highlightManager.setMinWeight(0.0);
					highlightManager.setIncludeText(true);
					highlightManager.setIncludeGraphics(true);
	
					if (command.equals("highlight")) {
						highlightManager.highlight(highlighter, docIds, fields, out);
					} else {
						highlightManager.findSnippets(highlighter, docIds, fields, out);
					}
				}
			} else if (command.equals("view")) {
				String docId = argMap.get("docId");
				String indexDirPath = argMap.get("indexDir");
				File indexDir = new File(indexDirPath);
				JochreIndexSearcher searcher = searchService.getJochreIndexSearcher(indexDir);
				List<Document> docs = searcher.findDocuments(docId);
				JsonFactory jsonFactory = new JsonFactory();
				Writer out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
				JsonGenerator jsonGen = jsonFactory.createGenerator(out);

				jsonGen.writeStartArray();
				for (Document doc : docs) {
					jsonGen.writeStartObject();
					for (IndexableField field : doc.getFields()) {
						jsonGen.writeStringField(field.name(), field.stringValue());
					}
					jsonGen.writeEndObject();
				}
				jsonGen.writeEndArray();
				jsonGen.flush();
			} else {
				throw new RuntimeException("Unknown command: " + command);
			}
		} catch (RuntimeException e) {
			LogUtils.logError(LOG, e);
			throw e;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

}