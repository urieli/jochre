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
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.StringUtils;

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
			
			String indexDirPath = null;
			String documentDirPath = null;
			int startPage = -1;
			int endPage = -1;
			boolean forceUpdate = false;
			String docName = null;
			int docIndex = -1;
			String queryPath = null;
			
			for (Entry<String, String> argMapEntry : argMap.entrySet()) {
				String argName = argMapEntry.getKey();
				String argValue = argMapEntry.getValue();
				
				if (argName.equals("indexDir")) {
					indexDirPath = argValue;
				} else if (argName.equals("documentDir")) {
					documentDirPath = argValue;
				} else if (argName.equals(JochreIndexField.startPage.name())) {
					startPage = Integer.parseInt(argValue);
				} else if (argName.equals(JochreIndexField.endPage.name())) {
					endPage = Integer.parseInt(argValue);
				} else if (argName.equals("forceUpdate")) {
					forceUpdate = argValue.equals("true");
				} else if (argName.equals("docName")) {
					docName = argValue;
				} else if (argName.equals("docIndex")) {
					docIndex = Integer.parseInt(argValue);
				} else if (argName.equals("queryFile")) {
					queryPath = argValue;
				} else {
					LOG.info("Unknown option: " + argName);
				}
			}
			
			if (indexDirPath==null)
				throw new RuntimeException("for command " + command + ", indexDir is required");
			
			SearchServiceLocator locator = SearchServiceLocator.getInstance();
			SearchService searchService = locator.getSearchService();
			
			if (command.equals("buildIndex")) {
				if (documentDirPath==null)
					throw new RuntimeException("for command " + command + ", documentDir is required");

				File indexDir = new File(indexDirPath);
				indexDir.mkdirs();
				File documentDir = new File(documentDirPath);
				
				JochreIndexBuilder builder = searchService.getJochreIndexBuilder(indexDir);
				builder.updateDocument(documentDir, startPage, endPage);
			} else if (command.equals("updateIndex")) {
				if (documentDirPath==null)
					throw new RuntimeException("for command " + command + ", documentDir is required");

				File indexDir = new File(indexDirPath);
				indexDir.mkdirs();
				File documentDir = new File(documentDirPath);
				
				JochreIndexBuilder builder = searchService.getJochreIndexBuilder(indexDir);
				builder.updateIndex(documentDir, forceUpdate);
			} else if (command.equals("search")||command.equals("highlight")||command.equals("snippets")) {
				HighlightServiceLocator highlightServiceLocator = HighlightServiceLocator.getInstance(locator);
				HighlightService highlightService = highlightServiceLocator.getHighlightService();
				
				File indexDir = new File(indexDirPath);
				JochreQuery query = searchService.getJochreQuery();
				
				if (queryPath==null)
					throw new RuntimeException("For command " + command + " queryFile is required");
				
				Map<String,String> queryArgs = StringUtils.getArgMap(queryPath);
				for (String argName : queryArgs.keySet()) {
					String argValue = queryArgs.get(argName);
					if (argName.equals("query")) {
						query.setQueryString(argValue);
					} else if (argName.equals("author")) {
						query.setAuthorQueryString(argValue);
					} else if (argName.equals("title")) {
						query.setTitleQueryString(argValue);
					} else if (argName.equals("maxDocs")) {
						query.setMaxDocs(Integer.parseInt(argValue));
					} else if (argName.equals("decimalPlaces")) {
						query.setDecimalPlaces(Integer.parseInt(argValue));
					} else {
						throw new RuntimeException("Unknown option in queryFile: " + argName);
					}
				}
				
				
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
							if (!field.name().equals(JochreIndexField.text.name()))
								LOG.debug(field);
						}
					}
					Set<String> fields = new HashSet<String>();
					fields.add(JochreIndexField.text.name());
					
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
				if (docName==null)
					throw new RuntimeException("For command " + command + " docName is required");
				File indexDir = new File(indexDirPath);
				JochreIndexSearcher searcher = searchService.getJochreIndexSearcher(indexDir);
				Map<Integer,Document> docs = searcher.findDocument(docName, docIndex);
				JsonFactory jsonFactory = new JsonFactory();
				Writer out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
				JsonGenerator jsonGen = jsonFactory.createGenerator(out);

				jsonGen.writeStartArray();
				for (Document doc : docs.values()) {
					jsonGen.writeStartObject();
					for (IndexableField field : doc.getFields()) {
						if (!field.name().equals(JochreIndexField.text.name()))
							jsonGen.writeStringField(field.name(), field.stringValue());
					}
					jsonGen.writeEndObject();
				}
				jsonGen.writeEndArray();
				jsonGen.flush();
			} else if (command.equals("list")) {
				if (docName==null)
					throw new RuntimeException("For command " + command + " docName is required");
				if (docIndex<0)
					throw new RuntimeException("For command " + command + " docIndex is required");
				File indexDir = new File(indexDirPath);
				JochreIndexSearcher searcher = searchService.getJochreIndexSearcher(indexDir);
				Map<Integer,Document> docs = searcher.findDocument(docName, docIndex);
				JochreIndexTermLister lister = new JochreIndexTermLister(docs.keySet().iterator().next(), searcher.getIndexSearcher());
				Writer out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
				lister.list(out);
				out.flush();
			} else {
				throw new RuntimeException("Unknown command: " + command);
			}
		} catch (RuntimeException e) {
			LogUtils.logError(LOG, e);
			throw e;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new JochreException(e);
		}
	}

}
