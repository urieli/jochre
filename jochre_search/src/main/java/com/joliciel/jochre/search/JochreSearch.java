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

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joliciel.jochre.search.feedback.FeedbackCriterion;
import com.joliciel.jochre.search.feedback.FeedbackQuery;
import com.joliciel.jochre.search.feedback.FeedbackService;
import com.joliciel.jochre.search.feedback.FeedbackServiceLocator;
import com.joliciel.jochre.search.highlight.HighlightManager;
import com.joliciel.jochre.search.highlight.HighlightService;
import com.joliciel.jochre.search.highlight.HighlightServiceLocator;
import com.joliciel.jochre.search.highlight.Highlighter;
import com.joliciel.jochre.search.lexicon.LexicalEntryReader;
import com.joliciel.jochre.search.lexicon.Lexicon;
import com.joliciel.jochre.search.lexicon.LexiconService;
import com.joliciel.jochre.search.lexicon.LexiconServiceLocator;
import com.joliciel.jochre.search.lexicon.RegexLexicalEntryReader;
import com.joliciel.jochre.search.lexicon.TextFileLexicon;
import com.joliciel.jochre.utils.JochreLogUtils;
import com.joliciel.talismane.utils.StringUtils;

/**
 * Command-line entry point into Jochre Search.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreSearch {
	private static final Logger LOG = LoggerFactory.getLogger(JochreSearch.class);

	public enum Command {
		updateIndex, search, highlight, snippets, view, list, wordImage, suggest, serializeLexicon, deserializeLexicon
	}

	/**
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		Command command = null;
		try {
			Map<String, String> argMap = new HashMap<String, String>();

			for (String arg : args) {
				int equalsPos = arg.indexOf('=');
				String argName = arg.substring(0, equalsPos);
				String argValue = arg.substring(equalsPos + 1);
				argMap.put(argName, argValue);
			}

			command = Command.valueOf(argMap.get("command"));
			argMap.remove("command");

			String logConfigPath = argMap.get("logConfigFile");
			argMap.remove("logConfigFile");
			JochreLogUtils.configureLogging(logConfigPath);

			LOG.debug("##### Arguments:");
			for (Entry<String, String> arg : argMap.entrySet()) {
				LOG.debug(arg.getKey() + ": " + arg.getValue());
			}

			String language = null;
			String indexDirPath = null;
			String contentDirPath = null;
			boolean forceUpdate = false;
			String docName = null;
			int docIndex = -1;
			int docId = -1;
			String queryPath = null;

			// lexicon handling
			String lexiconDirPath = null;
			String lexiconRegexPath = null;
			String lexiconFilePath = null;
			String word = null;

			// snippets
			int snippetCount = -1;
			int snippetSize = -1;

			// word images
			int startOffset = -1;
			String outDirPath = null;

			// suggestions
			String databasePropertiesPath = null;
			String suggestion = null;
			String username = null;
			String languageCode = null;
			String fontCode = null;

			for (Entry<String, String> argMapEntry : argMap.entrySet()) {
				String argName = argMapEntry.getKey();
				String argValue = argMapEntry.getValue();

				if (argName.equals("indexDir")) {
					indexDirPath = argValue;
				} else if (argName.equals("contentDir")) {
					contentDirPath = argValue;
				} else if (argName.equals("forceUpdate")) {
					forceUpdate = argValue.equals("true");
				} else if (argName.equals("docName")) {
					docName = argValue;
				} else if (argName.equals("docIndex")) {
					docIndex = Integer.parseInt(argValue);
				} else if (argName.equals("docId")) {
					docId = Integer.parseInt(argValue);
				} else if (argName.equals("queryFile")) {
					queryPath = argValue;
				} else if (argName.equals("lexiconDir")) {
					lexiconDirPath = argValue;
				} else if (argName.equals("lexiconRegex")) {
					lexiconRegexPath = argValue;
				} else if (argName.equals("lexicon")) {
					lexiconFilePath = argValue;
				} else if (argName.equals("word")) {
					word = argValue;
				} else if (argName.equals("language")) {
					language = argValue;
				} else if (argName.equals("snippetCount")) {
					snippetCount = Integer.parseInt(argValue);
				} else if (argName.equals("snippetSize")) {
					snippetSize = Integer.parseInt(argValue);
				} else if (argName.equals("startOffset")) {
					startOffset = Integer.parseInt(argValue);
				} else if (argName.equals("outDir")) {
					outDirPath = argValue;
				} else if (argName.equals("databaseProperties")) {
					databasePropertiesPath = argValue;
				} else if (argName.equals("suggestion")) {
					suggestion = argValue;
				} else if (argName.equals("username")) {
					username = argValue;
				} else if (argName.equals("languageCode")) {
					languageCode = argValue;
				} else if (argName.equals("fontCode")) {
					fontCode = argValue;
				} else {
					throw new RuntimeException("Unknown option: " + argName);
				}
			}

			if (language == null)
				throw new RuntimeException("for command " + command + ", language is required");

			File indexDir = null;
			File contentDir = null;
			if (!(command == Command.serializeLexicon || command == Command.deserializeLexicon)) {
				if (indexDirPath == null)
					throw new RuntimeException("for command " + command + ", indexDir is required");
				if (contentDirPath == null)
					throw new RuntimeException("for command " + command + ", contentDir is required");

				indexDir = new File(indexDirPath);
				indexDir.mkdirs();
				contentDir = new File(contentDirPath);
			}

			SearchServiceLocator locator = SearchServiceLocator.getInstance(Locale.forLanguageTag(language), indexDir, contentDir);
			SearchService searchService = locator.getSearchService();
			LexiconServiceLocator lexiconServiceLocator = LexiconServiceLocator.getInstance(locator);
			LexiconService lexiconService = lexiconServiceLocator.getLexiconService();

			FeedbackServiceLocator feedbackServiceLocator = FeedbackServiceLocator.getInstance(locator);
			if (databasePropertiesPath != null) {
				feedbackServiceLocator.setDatabasePropertiesPath(databasePropertiesPath);
			}

			switch (command) {
			case updateIndex: {
				JochreIndexBuilder builder = searchService.getJochreIndexBuilder();
				builder.updateIndex(forceUpdate);
				break;
			}
			case search:
			case highlight:
			case snippets: {
				if (lexiconFilePath != null) {
					File lexiconFile = new File(lexiconFilePath);
					Lexicon lexicon = lexiconService.deserializeLexicon(lexiconFile);
					searchService.setLexicon(lexicon);
				}

				HighlightServiceLocator highlightServiceLocator = HighlightServiceLocator.getInstance(locator);
				HighlightService highlightService = highlightServiceLocator.getHighlightService();

				JochreQuery query = searchService.getJochreQuery();

				if (queryPath == null)
					throw new RuntimeException("For command " + command + " queryFile is required");

				Map<String, String> queryArgs = StringUtils.getArgMap(queryPath);
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
					} else if (argName.equals("expand")) {
						query.setExpandInflections(argValue.equals("true"));
					} else {
						throw new RuntimeException("Unknown option in queryFile: " + argName);
					}
				}

				JochreIndexSearcher searcher = searchService.getJochreIndexSearcher();
				Writer out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));

				switch (command) {
				case search: {
					StringWriter stringWriter = new StringWriter();
					int resultCount = searcher.search(query, stringWriter);
					out.write(stringWriter.toString());
					out.write("\n");

					ObjectMapper mapper = new ObjectMapper();
					List<Map<String, Object>> result = mapper.readValue(stringWriter.toString(), new TypeReference<ArrayList<Map<String, Object>>>() {
					});
					out.write(result.toString());
					out.write("\n");

					if (databasePropertiesPath != null) {
						FeedbackService feedbackService = feedbackServiceLocator.getFeedbackService();
						FeedbackQuery feedbackQuery = feedbackService.getEmptyQuery(username, "1.2.3.4");
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
					break;
				}
				default: {
					TopDocs topDocs = searcher.search(query);

					Set<Integer> docIds = new LinkedHashSet<Integer>();
					for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
						docIds.add(scoreDoc.doc);
						LOG.debug("### Next document");
						Document doc = searcher.getIndexSearcher().doc(scoreDoc.doc);
						for (IndexableField field : doc.getFields()) {
							if (!field.name().equals(JochreIndexField.text.name()) && !field.name().startsWith("rect") && !field.name().startsWith("start"))
								LOG.debug(field.toString());
						}
					}
					Set<String> fields = new HashSet<String>();
					fields.add(JochreIndexField.text.name());

					Highlighter highlighter = highlightService.getHighlighter(query, searcher);
					HighlightManager highlightManager = highlightService.getHighlightManager(searcher);
					highlightManager.setDecimalPlaces(query.getDecimalPlaces());
					highlightManager.setMinWeight(0.0);
					highlightManager.setIncludeText(true);
					highlightManager.setIncludeGraphics(true);
					if (snippetCount > 0)
						highlightManager.setSnippetCount(snippetCount);
					if (snippetSize > 0)
						highlightManager.setSnippetSize(snippetSize);

					if (command == Command.highlight) {
						highlightManager.highlight(highlighter, docIds, fields, out);
					} else {
						highlightManager.findSnippets(highlighter, docIds, fields, out);
					}
					break;
				}
				}
				out.write("\n");
				out.flush();
				break;
			}
			case view: {
				if (docId < 0 && docIndex < 0)
					throw new RuntimeException("For command " + command + " either docName and docIndex, or docId are required");
				if (docId < 0) {
					if (docName == null)
						throw new RuntimeException("For command " + command + " docName is required");
					if (docIndex < 0)
						throw new RuntimeException("For command " + command + " docIndex is required");
				}

				JochreIndexSearcher searcher = searchService.getJochreIndexSearcher();
				if (docId < 0) {
					Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
					docId = docs.keySet().iterator().next();
				}

				Document doc = searcher.getIndexSearcher().doc(docId);

				JsonFactory jsonFactory = new JsonFactory();
				Writer out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
				JsonGenerator jsonGen = jsonFactory.createGenerator(out);

				jsonGen.writeStartObject();
				for (IndexableField field : doc.getFields()) {
					if (!field.name().equals(JochreIndexField.text.name()))
						jsonGen.writeStringField(field.name(), field.stringValue());
				}
				jsonGen.writeEndObject();

				jsonGen.flush();
				out.write("\n");
				out.flush();
				break;
			}
			case list: {
				if (docId < 0 && docIndex < 0)
					throw new RuntimeException("For command " + command + " either docName and docIndex, or docId are required");
				if (docId < 0) {
					if (docName == null)
						throw new RuntimeException("For command " + command + " docName is required");
					if (docIndex < 0)
						throw new RuntimeException("For command " + command + " docIndex is required");
				}

				JochreIndexSearcher searcher = searchService.getJochreIndexSearcher();
				if (docId < 0) {
					Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
					docId = docs.keySet().iterator().next();
				}
				JochreIndexTermLister lister = new JochreIndexTermLister(docId, searcher.getIndexSearcher());
				Writer out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
				lister.list(out);
				out.write("\n");
				out.flush();
				break;
			}
			case wordImage: {
				if (docId < 0 && docIndex < 0)
					throw new RuntimeException("For command " + command + " either docName and docIndex, or docId are required");
				if (docId < 0) {
					if (docName == null)
						throw new RuntimeException("For command " + command + " docName is required");
					if (docIndex < 0)
						throw new RuntimeException("For command " + command + " docIndex is required");
				}
				if (startOffset < 0)
					throw new RuntimeException("For command " + command + " startOffset is required");
				if (outDirPath == null)
					throw new RuntimeException("For command " + command + " outDir is required");
				JochreIndexSearcher searcher = searchService.getJochreIndexSearcher();
				if (docId < 0) {
					Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
					docId = docs.keySet().iterator().next();
				}
				JochreIndexDocument jochreDoc = searchService.getJochreIndexDocument(searcher, docId);
				JochreIndexWord jochreWord = jochreDoc.getWord(startOffset);
				LOG.debug("jochreDoc: " + jochreDoc.getPath());
				LOG.debug("word: " + jochreWord.getText());
				LOG.debug("startOffset: " + jochreWord.getStartOffset());
				BufferedImage wordImage = jochreWord.getImage();

				File outDir = new File(outDirPath);
				outDir.mkdirs();
				File outputfile = new File(outDir, "word.png");
				ImageIO.write(wordImage, "png", outputfile);
				break;
			}
			case suggest: {
				if (databasePropertiesPath == null)
					throw new RuntimeException("For command " + command + " databaseProperties is required");
				if (docId < 0 && docIndex < 0)
					throw new RuntimeException("For command " + command + " either docName and docIndex, or docId are required");
				if (docId < 0) {
					if (docName == null)
						throw new RuntimeException("For command " + command + " docName is required");
					if (docIndex < 0)
						throw new RuntimeException("For command " + command + " docIndex is required");
				}
				if (startOffset < 0)
					throw new RuntimeException("For command " + command + " startOffset is required");
				if (suggestion == null)
					throw new RuntimeException("For command " + command + " suggestion is required");
				if (username == null)
					throw new RuntimeException("For command " + command + " username is required");
				if (fontCode == null)
					throw new RuntimeException("For command " + command + " fontCode is required");
				if (languageCode == null)
					throw new RuntimeException("For command " + command + " languageCode is required");

				JochreIndexSearcher searcher = searchService.getJochreIndexSearcher();
				if (docId < 0) {
					Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
					docId = docs.keySet().iterator().next();
				}

				FeedbackService feedbackService = feedbackServiceLocator.getFeedbackService();
				feedbackService.makeSuggestion(searcher, docId, startOffset, suggestion, username, "1.2.3.4", fontCode, languageCode);
				break;
			}
			case serializeLexicon: {
				if (lexiconDirPath == null)
					throw new RuntimeException("For command " + command + " lexiconDir is required");
				if (lexiconRegexPath == null)
					throw new RuntimeException("For command " + command + " lexiconRegex is required");
				if (lexiconFilePath == null)
					throw new RuntimeException("For command " + command + " lexicon is required");

				File lexiconDir = new File(lexiconDirPath);
				File[] lexiconFiles = lexiconDir.listFiles();
				TextFileLexicon lexicon = lexiconService.getTextFileLexicon(searchService.getLocale());

				File regexFile = new File(lexiconRegexPath);
				Scanner regexScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(regexFile), "UTF-8")));
				LexicalEntryReader lexicalEntryReader = new RegexLexicalEntryReader(regexScanner);

				for (File file : lexiconFiles) {
					LOG.info("Adding " + file.getName());
					lexicon.addLexiconFile(file, lexicalEntryReader);
				}

				File outFile = new File(lexiconFilePath);
				lexicon.serialize(outFile);
				break;
			}
			case deserializeLexicon: {
				if (lexiconFilePath == null)
					throw new RuntimeException("For command " + command + " lexicon is required");
				if (word == null)
					throw new RuntimeException("For command " + command + " word is required");

				File lexiconFile = new File(lexiconFilePath);
				Lexicon lexicon = lexiconService.deserializeLexicon(lexiconFile);
				Set<String> lemmas = lexicon.getLemmas(word);
				LOG.info("Word: " + word);
				if (lemmas != null) {
					for (String lemma : lemmas) {
						Set<String> words = lexicon.getWords(lemma);
						LOG.info("# Lemma: " + lemma + ", words: " + words.toString());
					}
				}
				break;
			}
			default: {
				throw new RuntimeException("Unknown command: " + command);
			}
			}
		} catch (RuntimeException e) {
			LOG.error("Failed to run command " + command, e);
			throw e;
		} catch (IOException e) {
			LOG.error("Failed to run command " + command, e);
			throw new RuntimeException(e);
		} finally {
			long endTime = System.currentTimeMillis();
			LOG.info("Completed in " + (endTime - startTime) + " ms");
		}
	}

}
