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
package com.joliciel.jochre.search.web;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.joliciel.jochre.search.JochreIndexBuilder;
import com.joliciel.jochre.search.JochreIndexDocument;
import com.joliciel.jochre.search.JochreIndexField;
import com.joliciel.jochre.search.JochreIndexSearcher;
import com.joliciel.jochre.search.JochreIndexTermLister;
import com.joliciel.jochre.search.JochreQuery;
import com.joliciel.jochre.search.JochreQueryParseException;
import com.joliciel.jochre.search.SearchService;
import com.joliciel.jochre.search.SearchServiceLocator;
import com.joliciel.jochre.search.SearchStatusHolder;
import com.joliciel.jochre.search.highlight.HighlightManager;
import com.joliciel.jochre.search.highlight.HighlightService;
import com.joliciel.jochre.search.highlight.HighlightServiceLocator;
import com.joliciel.jochre.search.highlight.Highlighter;
import com.joliciel.jochre.search.highlight.ImageSnippet;
import com.joliciel.jochre.search.highlight.Snippet;
import com.joliciel.jochre.search.lexicon.Lexicon;
import com.joliciel.jochre.search.lexicon.LexiconService;
import com.joliciel.jochre.search.lexicon.LexiconServiceLocator;
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.utils.LogUtils;

/**
 * Restful web service for Jochre search.
 * @author Assaf Urieli
 *
 */
public class JochreSearchServlet extends HttpServlet {
	private static final Log LOG = LogFactory.getLog(JochreSearchServlet.class);
	private static final long serialVersionUID = 1L;
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse response)
			throws ServletException, IOException {
		this.doGet(req, response);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setCharacterEncoding("UTF-8");
			
			JochreSearchProperties props = JochreSearchProperties.getInstance(this.getServletContext());
			
			String command = req.getParameter("command");
			if (command==null) {
				command="search";
			}
			
			SearchServiceLocator searchServiceLocator = SearchServiceLocator.getInstance(props.getLocale());
			SearchService searchService = searchServiceLocator.getSearchService();
			
			if (command.equals("log4j")) {
				Log4jListener.reloadLog4jProperties(this.getServletContext());
				PrintWriter out = response.getWriter();
				out.write("{\"response\":\"log4j reloaded\"}\n");
				out.flush();
				return;
			} else if (command.equals("purge")) {
				JochreSearchProperties.purgeInstance();
				searchService.purge();
				return;
			}
			
			String lexiconPath = props.getLexiconPath();
			if (lexiconPath!=null && searchService.getLexicon()==null) {
				LexiconServiceLocator lexiconServiceLocator = LexiconServiceLocator.getInstance(searchServiceLocator);
				LexiconService lexiconService = lexiconServiceLocator.getLexiconService();
				File lexiconFile = new File(lexiconPath);
				Lexicon lexicon = lexiconService.deserializeLexicon(lexiconFile);
				searchService.setLexicon(lexicon);
			}
			
			Map<String,String> argMap = new HashMap<String, String>();
			@SuppressWarnings("rawtypes")
			Enumeration params = req.getParameterNames();
			while (params.hasMoreElements()) {
				String paramName = (String) params.nextElement();
				String value = req.getParameter(paramName);
				argMap.put(paramName, value);
			}
			argMap.remove("command");
				
			double minWeight = 0;
			int titleSnippetCount = 1;
			int snippetCount = 3;
			int snippetSize = 80;
			boolean includeText = false;
			boolean includeGraphics = false;
			boolean forceUpdate = false;
			String snippetJson = null;
			Set<Integer> docIds = null;
			String queryString = null;
			String authorQueryString = null;
			String titleQueryString = null;
			int maxDocs = -1;
			int decimalPlaces = -1;
			String docName = null;
			int docIndex = -1;
			Boolean expandInflections = null;
			
			int startOffset = -1;
			int docId = -1;

			for (Entry<String, String> argEntry : argMap.entrySet()) {
				String argName = argEntry.getKey();
				String argValue = argEntry.getValue();
				argValue = URLDecoder.decode(argValue, "UTF-8");
				LOG.debug(argName + ": " + argValue);

				if (argName.equals("minWeight")) {
					minWeight = Double.parseDouble(argValue);
				} else if (argName.equals("titleSnippetCount")) {
					titleSnippetCount = Integer.parseInt(argValue);
				} else if (argName.equals("snippetCount")) {
					snippetCount = Integer.parseInt(argValue);
				} else if (argName.equals("snippetSize")) {
					snippetSize = Integer.parseInt(argValue);
				} else if (argName.equals("includeText")) {
					includeText = argValue.equalsIgnoreCase("true");
				} else if (argName.equals("includeGraphics")) {
					includeGraphics = argValue.equalsIgnoreCase("true");
				} else if (argName.equals("forceUpdate")) {
					forceUpdate = argValue.equalsIgnoreCase("true");
				} else if (argName.equals("snippet")) {
					snippetJson = argValue;
				} else if (argName.equalsIgnoreCase("docIds")) {
					if (argValue.length()>0) {
						String[] idArray = argValue.split(",");
						docIds = new HashSet<Integer>();
						for (String id : idArray)
							docIds.add(Integer.parseInt(id));
					}
				} else if (argName.equalsIgnoreCase("query")) {
					queryString = argValue;
				} else if (argName.equalsIgnoreCase("author")) {
					authorQueryString = argValue;
				} else if (argName.equalsIgnoreCase("title")) {
					titleQueryString = argValue;
				} else if (argName.equalsIgnoreCase("maxDocs")) {
					maxDocs = Integer.parseInt(argValue);
				} else if (argName.equalsIgnoreCase("decimalPlaces")) {
					decimalPlaces = Integer.parseInt(argValue);
				} else if (argName.equals("docName")) {
					docName = argValue;
				} else if (argName.equals("docIndex")) {
					docIndex = Integer.parseInt(argValue);
				} else if (argName.equals("expand")) {
					expandInflections = argValue.equals("true");
				} else if (argName.equals("startOffset")) {
					startOffset = Integer.parseInt(argValue);
				} else if (argName.equals("docId")) {
					docId = Integer.parseInt(argValue);
				} else {
					throw new RuntimeException("Unknown option: " + argName);
				}
			}
	
			PrintWriter out = null;
			
			if (!command.equals("imageSnippet") && !command.equals("wordImage"))
				out = response.getWriter();
			
			String indexDirPath = props.getIndexDirPath();
			File indexDir = new File(indexDirPath);
			LOG.info("Index dir: " + indexDir.getAbsolutePath());
			JochreIndexSearcher searcher = searchService.getJochreIndexSearcher(indexDir);
			
			if (command.equals("search") || command.equals("highlight") || command.equals("snippets")) {		
				response.setContentType("text/plain;charset=UTF-8");
				JochreQuery query = searchService.getJochreQuery();
				query.setQueryString(queryString);
				query.setAuthorQueryString(authorQueryString);
				query.setTitleQueryString(titleQueryString);
				if (decimalPlaces>=0)
					query.setDecimalPlaces(decimalPlaces);
				if (maxDocs>=0)
					query.setMaxDocs(maxDocs);
				if (expandInflections!=null)
					query.setExpandInflections(expandInflections);
				
				try {
					if (command.equals("search")) {
						searcher.search(query, out);
					} else {
						if (docIds==null)
							throw new RuntimeException("Command " + command + " requires docIds");
						HighlightServiceLocator highlightServiceLocator = HighlightServiceLocator.getInstance(searchServiceLocator);
						HighlightService highlightService = highlightServiceLocator.getHighlightService();
						Highlighter highlighter = highlightService.getHighlighter(query, searcher.getIndexSearcher());
						HighlightManager highlightManager = highlightService.getHighlightManager(searcher.getIndexSearcher());
						highlightManager.setDecimalPlaces(query.getDecimalPlaces());
						highlightManager.setMinWeight(minWeight);
						highlightManager.setIncludeText(includeText);
						highlightManager.setIncludeGraphics(includeGraphics);
						highlightManager.setTitleSnippetCount(titleSnippetCount);
						highlightManager.setSnippetCount(snippetCount);
						highlightManager.setSnippetSize(snippetSize);
			
						Set<String> fields = new HashSet<String>();
						fields.add(JochreIndexField.text.name());
						
						if (command.equals("highlight"))
							highlightManager.highlight(highlighter, docIds, fields, out);
						else
							highlightManager.findSnippets(highlighter, docIds, fields, out);
					}
				} catch (JochreQueryParseException e) {
					JsonFactory jsonFactory = new JsonFactory();
					JsonGenerator jsonGen = jsonFactory.createGenerator(out);

					jsonGen.writeStartArray();
					jsonGen.writeStartObject();
					jsonGen.writeStringField("parseException", "true");
					jsonGen.writeStringField("message", e.getMessage());

					jsonGen.writeEndObject();
					jsonGen.writeEndArray();
					jsonGen.flush();
				}
			} else if (command.equals("textSnippet")) {
				response.setContentType("text/plain;charset=UTF-8");
				Snippet snippet = new Snippet(snippetJson);
				
				if (LOG.isDebugEnabled()) {
					Document doc = searcher.getIndexSearcher().doc(snippet.getDocId());
					LOG.debug("Snippet in: " + doc.get(JochreIndexField.path.name()));
				}
				
				HighlightServiceLocator highlightServiceLocator = HighlightServiceLocator.getInstance(searchServiceLocator);
				HighlightService highlightService = highlightServiceLocator.getHighlightService();
				HighlightManager highlightManager = highlightService.getHighlightManager(searcher.getIndexSearcher());
				String text = highlightManager.displaySnippet(snippet);
				
				out.write(text);
			} else if (command.equals("imageSnippet")) {
				String mimeType="image/png";
				response.setContentType(mimeType);
				if (snippetJson==null)
					throw new JochreException("Command " + command + " requires a snippet");
				Snippet snippet = new Snippet(snippetJson);
				
				if (LOG.isDebugEnabled()) {
					Document doc = searcher.getIndexSearcher().doc(snippet.getDocId());
					LOG.debug("Snippet in: " + doc.get(JochreIndexField.path.name()));
				}
				
				HighlightServiceLocator highlightServiceLocator = HighlightServiceLocator.getInstance(searchServiceLocator);
				HighlightService highlightService = highlightServiceLocator.getHighlightService();
				HighlightManager highlightManager = highlightService.getHighlightManager(searcher.getIndexSearcher());
				ImageSnippet imageSnippet = highlightManager.getImageSnippet(snippet);
				OutputStream os = response.getOutputStream();
				ImageOutputStream ios = ImageIO.createImageOutputStream(os);
				BufferedImage image = imageSnippet.getImage();
				ImageReader imageReader = ImageIO.getImageReadersByMIMEType(mimeType).next();
				ImageWriter imageWriter = ImageIO.getImageWriter(imageReader);
				imageWriter.setOutput(ios);
				imageWriter.write(image);
				ios.flush();
			} else if (command.equals("wordImage")) {
				String mimeType="image/png";
				response.setContentType(mimeType);
				if (startOffset<0)
					throw new JochreException("Command " + command + " requires a startOffset");
				if (docId<0 && (docName==null || docIndex<0))
					throw new RuntimeException("For command " + command + " either a docName and docIndex, or a docId is required");
				
				if (docId<0) {
					Map<Integer,Document> docs = searcher.findDocument(docName, docIndex);
					docId = docs.keySet().iterator().next();
				}
				JochreIndexDocument jochreDoc = searchService.getJochreIndexDocument(searcher.getIndexSearcher(), docId);
				BufferedImage wordImage = jochreDoc.getWordImage(startOffset);

				OutputStream os = response.getOutputStream();
				ImageOutputStream ios = ImageIO.createImageOutputStream(os);
				
				ImageReader imageReader = ImageIO.getImageReadersByMIMEType(mimeType).next();
				ImageWriter imageWriter = ImageIO.getImageWriter(imageReader);
				imageWriter.setOutput(ios);
				imageWriter.write(wordImage);
				ios.flush();
			} else if (command.equals("index")) {
				File contentDir = new File(props.getContentDirPath());
				
				JochreIndexBuilder builder = searchService.getJochreIndexBuilder(indexDir, contentDir);
				builder.setForceUpdate(forceUpdate);
				
				new Thread(builder).start();
				out.write("{\"response\":\"index thread started\"}\n");
			} else if (command.equals("status")) {
				SearchStatusHolder searchStatusHolder = searchService.getSearchStatusHolder();
				JsonFactory jsonFactory = new JsonFactory();
				JsonGenerator jsonGen = jsonFactory.createGenerator(out);

				jsonGen.writeStartObject();
				jsonGen.writeStringField("status", searchStatusHolder.getStatus().name());
				jsonGen.writeStringField("message", searchStatusHolder.getMessage());
				jsonGen.writeNumberField("total", searchStatusHolder.getTotalCount());
				jsonGen.writeNumberField("processed", searchStatusHolder.getProcessedCount());
				jsonGen.writeNumberField("success", searchStatusHolder.getSuccessCount());
				jsonGen.writeNumberField("failure", searchStatusHolder.getFailureCount());
				Date updateDate = new Date(searchStatusHolder.getLastUpdated());
				jsonGen.writeStringField("lastUpdated", dateFormat.format(updateDate));
				jsonGen.writeNumberField("totalTime", searchStatusHolder.getTotalTime());
				
				jsonGen.writeEndObject();
				jsonGen.flush();
			} else if (command.equals("view")) {
				if (docName==null)
					throw new RuntimeException("For command " + command + " docName is required");
				Map<Integer,Document> docs = searcher.findDocument(docName, docIndex);
				JsonFactory jsonFactory = new JsonFactory();
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
				Map<Integer,Document> docs = searcher.findDocument(docName, docIndex);
				JochreIndexTermLister lister = new JochreIndexTermLister(docs.keySet().iterator().next(), searcher.getIndexSearcher());
				lister.list(out);
				out.flush();
			} else {
				throw new RuntimeException("Unknown command: " + command);
			}
			
			if (out!=null)
				out.flush();
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (RuntimeException e) {
			LogUtils.logError(LOG, e);
			throw e;
		}
	}
}
