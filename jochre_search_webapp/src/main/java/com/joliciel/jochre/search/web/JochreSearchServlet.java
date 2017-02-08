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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.joliciel.jochre.search.JochreIndexBuilder;
import com.joliciel.jochre.search.JochreIndexDocument;
import com.joliciel.jochre.search.JochreIndexField;
import com.joliciel.jochre.search.JochreIndexSearcher;
import com.joliciel.jochre.search.JochreIndexTermLister;
import com.joliciel.jochre.search.JochreIndexWord;
import com.joliciel.jochre.search.JochreQuery;
import com.joliciel.jochre.search.JochreQueryParseException;
import com.joliciel.jochre.search.JochreSearchConstants;
import com.joliciel.jochre.search.SearchService;
import com.joliciel.jochre.search.SearchServiceLocator;
import com.joliciel.jochre.search.SearchStatusHolder;
import com.joliciel.jochre.search.feedback.FeedbackCriterion;
import com.joliciel.jochre.search.feedback.FeedbackQuery;
import com.joliciel.jochre.search.feedback.FeedbackService;
import com.joliciel.jochre.search.feedback.FeedbackServiceLocator;
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

/**
 * Restful web service for Jochre search.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreSearchServlet extends HttpServlet {
	private static final Logger LOG = LoggerFactory.getLogger(JochreSearchServlet.class);
	private static final long serialVersionUID = 1L;
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
		this.doGet(req, response);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
		long startTime = System.currentTimeMillis();
		String user = null;
		try {
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setCharacterEncoding("UTF-8");

			if (LOG.isDebugEnabled()) {
				LOG.debug(getURI(req));
			}

			JochreSearchProperties props = JochreSearchProperties.getInstance(this.getServletContext());

			String command = req.getParameter("command");
			if (command == null)
				command = "search";

			if (command.equals("log4j")) {
				response.setContentType("application/json;charset=UTF-8");
				Slf4jListener.reloadLog4jProperties(this.getServletContext());
				PrintWriter out = response.getWriter();
				out.write("{\"response\":\"log4j reloaded\"}\n");
				out.flush();
				return;
			}

			Map<String, String> argMap = new HashMap<String, String>();
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

			// suggestions
			String suggestion = null;
			String suggestion2 = null;
			String languageCode = null;
			String fontCode = null;
			String ip = null;

			for (Entry<String, String> argEntry : argMap.entrySet()) {
				String argName = argEntry.getKey();
				String argValue = argEntry.getValue();
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
					if (argValue.length() > 0) {
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
				} else if (argName.equals("user")) {
					user = argValue;
				} else if (argName.equals("ip")) {
					ip = argValue;
				} else if (argName.equals("suggestion")) {
					suggestion = argValue;
				} else if (argName.equals("suggestion2")) {
					suggestion2 = argValue;
				} else if (argName.equals("languageCode")) {
					languageCode = argValue;
				} else if (argName.equals("fontCode")) {
					fontCode = argValue;
				} else {
					throw new RuntimeException("Unknown option: " + argName);
				}
			}

			PrintWriter out = null;

			if (!command.equals("imageSnippet") && !command.equals("wordImage"))
				out = response.getWriter();

			File indexDir = new File(props.getIndexDirPath());
			LOG.debug("Index dir: " + indexDir.getAbsolutePath());
			File contentDir = new File(props.getContentDirPath());
			LOG.debug("Content dir: " + contentDir.getAbsolutePath());
			SearchServiceLocator searchServiceLocator = SearchServiceLocator.getInstance(props.getLocale(), indexDir, contentDir);
			SearchService searchService = searchServiceLocator.getSearchService();

			String lexiconPath = props.getLexiconPath();
			if (lexiconPath != null && searchService.getLexicon() == null) {
				LexiconServiceLocator lexiconServiceLocator = LexiconServiceLocator.getInstance(searchServiceLocator);
				LexiconService lexiconService = lexiconServiceLocator.getLexiconService();
				File lexiconFile = new File(lexiconPath);
				Lexicon lexicon = lexiconService.deserializeLexicon(lexiconFile);
				searchService.setLexicon(lexicon);
			}

			JochreIndexSearcher searcher = searchService.getJochreIndexSearcher();

			if (command.equals("search") || command.equals("highlight") || command.equals("snippets")) {
				response.setContentType("application/json;charset=UTF-8");
				JochreQuery query = searchService.getJochreQuery();
				query.setQueryString(queryString);
				query.setAuthorQueryString(authorQueryString);
				query.setTitleQueryString(titleQueryString);
				if (decimalPlaces >= 0)
					query.setDecimalPlaces(decimalPlaces);
				if (maxDocs >= 0)
					query.setMaxDocs(maxDocs);
				if (expandInflections != null)
					query.setExpandInflections(expandInflections);

				try {
					if (command.equals("search")) {
						int resultCount = searcher.search(query, out);

						String databasePropsPath = props.getDatabasePropertiesPath();
						if (databasePropsPath != null) {
							FeedbackServiceLocator feedbackServiceLocator = FeedbackServiceLocator.getInstance(searchServiceLocator);

							feedbackServiceLocator.setDatabasePropertiesPath(props.getDatabasePropertiesPath());
							FeedbackService feedbackService = feedbackServiceLocator.getFeedbackService();
							FeedbackQuery feedbackQuery = feedbackService.getEmptyQuery(user, ip);
							feedbackQuery.setResultCount(resultCount);
							feedbackQuery.addClause(FeedbackCriterion.text, query.getQueryString());
							if (query.getAuthorQueryString() != null && query.getAuthorQueryString().length() > 0)
								feedbackQuery.addClause(FeedbackCriterion.author, query.getAuthorQueryString());
							if (query.getTitleQueryString() != null && query.getTitleQueryString().length() > 0)
								feedbackQuery.addClause(FeedbackCriterion.title, query.getTitleQueryString());
							if (!query.isExpandInflections())
								feedbackQuery.addClause(FeedbackCriterion.strict, "true");
							feedbackQuery.save();
						}
					} else {
						if (docIds == null)
							throw new RuntimeException("Command " + command + " requires docIds");
						HighlightServiceLocator highlightServiceLocator = HighlightServiceLocator.getInstance(searchServiceLocator);
						HighlightService highlightService = highlightServiceLocator.getHighlightService();
						Highlighter highlighter = highlightService.getHighlighter(query, searcher);
						HighlightManager highlightManager = highlightService.getHighlightManager(searcher);
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
				HighlightManager highlightManager = highlightService.getHighlightManager(searcher);
				String text = highlightManager.displaySnippet(snippet);

				out.write(text);
			} else if (command.equals("imageSnippet")) {
				String mimeType = "image/png";
				response.setContentType(mimeType);
				if (snippetJson == null)
					throw new JochreException("Command " + command + " requires a snippet");
				Snippet snippet = new Snippet(snippetJson);

				if (LOG.isDebugEnabled()) {
					Document doc = searcher.getIndexSearcher().doc(snippet.getDocId());
					LOG.debug("Snippet in: " + doc.get(JochreIndexField.path.name()));
				}

				HighlightServiceLocator highlightServiceLocator = HighlightServiceLocator.getInstance(searchServiceLocator);
				HighlightService highlightService = highlightServiceLocator.getHighlightService();
				HighlightManager highlightManager = highlightService.getHighlightManager(searcher);
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
				String mimeType = "image/png";
				response.setContentType(mimeType);
				if (startOffset < 0)
					throw new JochreException("Command " + command + " requires a startOffset");
				if (docId < 0 && (docName == null || docIndex < 0))
					throw new RuntimeException("For command " + command + " either a docName and docIndex, or a docId is required");

				if (docId < 0) {
					Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
					docId = docs.keySet().iterator().next();
				}
				JochreIndexDocument jochreDoc = searchService.getJochreIndexDocument(searcher, docId);
				JochreIndexWord jochreWord = jochreDoc.getWord(startOffset);
				BufferedImage wordImage = jochreWord.getImage();

				OutputStream os = response.getOutputStream();
				ImageOutputStream ios = ImageIO.createImageOutputStream(os);

				ImageReader imageReader = ImageIO.getImageReadersByMIMEType(mimeType).next();
				ImageWriter imageWriter = ImageIO.getImageWriter(imageReader);
				imageWriter.setOutput(ios);
				imageWriter.write(wordImage);
				ios.flush();
			} else if (command.equals("word")) {
				response.setContentType("application/json;charset=UTF-8");
				if (startOffset < 0)
					throw new JochreException("Command " + command + " requires a startOffset");
				if (docId < 0 && (docName == null || docIndex < 0))
					throw new RuntimeException("For command " + command + " either a docName and docIndex, or a docId is required");

				if (docId < 0) {
					Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
					docId = docs.keySet().iterator().next();
				}
				JochreIndexDocument jochreDoc = searchService.getJochreIndexDocument(searcher, docId);
				JochreIndexWord jochreWord = jochreDoc.getWord(startOffset);
				String word = jochreWord.getText();
				String word2 = null;
				if (word.contains(JochreSearchConstants.INDEX_NEWLINE)) {
					word2 = word.substring(word.indexOf(JochreSearchConstants.INDEX_NEWLINE) + 1);
					word = word.substring(0, word.indexOf(JochreSearchConstants.INDEX_NEWLINE));
				}
				JsonFactory jsonFactory = new JsonFactory();
				JsonGenerator jsonGen = jsonFactory.createGenerator(out);
				jsonGen.writeStartObject();
				jsonGen.writeStringField("word", word);
				if (word2 != null)
					jsonGen.writeStringField("word2", word2);
				jsonGen.writeEndObject();
				jsonGen.flush();
			} else if (command.equals("suggest")) {
				if (startOffset < 0)
					throw new RuntimeException("For command " + command + " startOffset is required");
				if (docId < 0 && (docName == null || docIndex < 0))
					throw new RuntimeException("For command " + command + " either a docName and docIndex, or a docId is required");
				if (suggestion == null)
					throw new RuntimeException("For command " + command + " suggestion is required");
				if (user == null)
					throw new RuntimeException("For command " + command + " user is required");
				if (fontCode == null)
					throw new RuntimeException("For command " + command + " fontCode is required");
				if (languageCode == null)
					throw new RuntimeException("For command " + command + " languageCode is required");

				if (docId < 0) {
					Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
					docId = docs.keySet().iterator().next();
				}

				FeedbackServiceLocator feedbackServiceLocator = FeedbackServiceLocator.getInstance(searchServiceLocator);
				String databasePropsPath = props.getDatabasePropertiesPath();
				if (databasePropsPath == null)
					throw new JochreException("No database properties");

				if (suggestion2 != null && suggestion2.length() > 0)
					suggestion += JochreSearchConstants.INDEX_NEWLINE + suggestion2;

				feedbackServiceLocator.setDatabasePropertiesPath(databasePropsPath);
				FeedbackService feedbackService = feedbackServiceLocator.getFeedbackService();
				feedbackService.makeSuggestion(searcher, docId, startOffset, suggestion, user, ip, fontCode, languageCode);
				out.write("{\"response\":\"suggestion saved\"}\n");
			} else if (command.equals("index")) {
				response.setContentType("application/json;charset=UTF-8");

				JochreIndexBuilder builder = searchService.getJochreIndexBuilder();
				builder.setForceUpdate(forceUpdate);

				new Thread(builder).start();
				out.write("{\"response\":\"index thread started\"}\n");
			} else if (command.equals("status")) {
				response.setContentType("application/json;charset=UTF-8");
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
				response.setContentType("application/json;charset=UTF-8");
				if (docId < 0 && docIndex < 0)
					throw new RuntimeException("For command " + command + " either docName and docIndex, or docId are required");
				if (docId < 0) {
					if (docName == null)
						throw new RuntimeException("For command " + command + " docName is required");
					if (docIndex < 0)
						throw new RuntimeException("For command " + command + " docIndex is required");
				}

				if (docId < 0) {
					Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
					docId = docs.keySet().iterator().next();
				}

				Document doc = searcher.getIndexSearcher().doc(docId);
				JsonFactory jsonFactory = new JsonFactory();
				JsonGenerator jsonGen = jsonFactory.createGenerator(out);

				jsonGen.writeStartObject();
				for (IndexableField field : doc.getFields()) {
					if (!field.name().equals(JochreIndexField.text.name()))
						jsonGen.writeStringField(field.name(), field.stringValue());
				}
				jsonGen.writeEndObject();
				jsonGen.flush();
			} else if (command.equals("list")) {
				response.setContentType("application/json;charset=UTF-8");
				if (docId < 0 && docIndex < 0)
					throw new RuntimeException("For command " + command + " either docName and docIndex, or docId are required");
				if (docId < 0) {
					if (docName == null)
						throw new RuntimeException("For command " + command + " docName is required");
					if (docIndex < 0)
						throw new RuntimeException("For command " + command + " docIndex is required");
				}

				if (docId < 0) {
					Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
					docId = docs.keySet().iterator().next();
				}

				JochreIndexTermLister lister = new JochreIndexTermLister(docId, searcher.getIndexSearcher());
				lister.list(out);
				out.flush();
			} else if (command.equals("purge")) {
				response.setContentType("application/json;charset=UTF-8");
				JochreSearchProperties.purgeInstance();
				searchService.purge();
				out.write("{\"response\":\"purge performed\"}\n");
			} else {
				throw new RuntimeException("Unknown command: " + command);
			}

			if (out != null)
				out.flush();
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (RuntimeException e) {
			LOG.error("Failed to run " + req.getRequestURI() + "?" + req.getQueryString(), e);
			throw e;
		} finally {
			long duration = System.currentTimeMillis() - startTime;
			LOG.info("User:" + user + " " + req.getRequestURI() + "?" + req.getQueryString() + " Duration:" + duration);
		}
	}

	public static String getURI(HttpServletRequest request) {
		String uri = request.getScheme() + "://" + request.getServerName()
				+ ("http".equals(request.getScheme()) && request.getServerPort() == 80 || "https".equals(request.getScheme()) && request.getServerPort() == 443
						? "" : ":" + request.getServerPort())
				+ request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

		return uri;
	}
}
