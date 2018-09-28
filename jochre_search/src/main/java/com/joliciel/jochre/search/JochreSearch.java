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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.joliciel.jochre.search.feedback.FeedbackCriterion;
import com.joliciel.jochre.search.feedback.FeedbackDAO;
import com.joliciel.jochre.search.feedback.FeedbackQuery;
import com.joliciel.jochre.search.feedback.FeedbackSuggestion;
import com.joliciel.jochre.search.highlight.HighlightManager;
import com.joliciel.jochre.search.highlight.Highlighter;
import com.joliciel.jochre.search.highlight.ImageSnippet;
import com.joliciel.jochre.search.highlight.LuceneQueryHighlighter;
import com.joliciel.jochre.search.highlight.Snippet;
import com.joliciel.jochre.search.lexicon.LexicalEntryReader;
import com.joliciel.jochre.search.lexicon.Lexicon;
import com.joliciel.jochre.search.lexicon.RegexLexicalEntryReader;
import com.joliciel.jochre.search.lexicon.TextFileLexicon;
import com.joliciel.jochre.utils.Either;
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.utils.StringUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;

/**
 * Command-line entry point into Jochre Search.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreSearch {
	private static final Logger LOG = LoggerFactory.getLogger(JochreSearch.class);

	private final String configId;

	public JochreSearch(String configId) {
		if (configId == null) {
			Config config = ConfigFactory.load();
			this.configId = config.getString("jochre.search.config-id");
		} else {
			this.configId = configId;
		}
	}

	public JochreSearch() {
		this(null);
	}

	public enum Command {
		/**
		 * Start a thread to update the index, takin ginto account any recent changes.
		 */
		index,
		/**
		 * Search the index.
		 */
		search,
		/**
		 * Return the terms corresponding to a given search query.
		 */
		highlight,
		/**
		 * Return the most relevant snippets corresponding to a given search query.
		 */
		snippets,
		/**
		 * List all of the fields in a given index document.
		 */
		view,
		/**
		 * List all of the terms in a given index document.
		 */
		list,
		/**
		 * Return the image corresponding to a specific word.
		 */
		wordImage,
		/**
		 * Make a suggestion for an OCR error.
		 */
		suggest,
		/**
		 * Serialize a lexicon.
		 */
		serializeLexicon,
		/**
		 * Deserialize a lexicon and test certain words.
		 */
		deserializeLexicon,
		/**
		 * Return the highlighted text corresponding to a particular snippet.
		 */
		textSnippet,
		/**
		 * Return the image corresponding to a particular snippet.
		 */
		imageSnippet,
		/**
		 * Find the word corresponding to a particular document location.
		 */
		word,
		/**
		 * Check the status of the index update thread.
		 */
		indexStatus,
		/**
		 * Refresh the index reader, to take into account any index updates.
		 */
		refresh
	}

	public void execute(Map<String, String> argMap, Either<PrintWriter, OutputStream> output) {
		long startTime = System.currentTimeMillis();
		Command command = null;
		try {
			command = Command.valueOf(argMap.get("command"));
			argMap.remove("command");

			Config config = ConfigFactory.load();

			LOG.debug("##### Arguments:");
			for (Entry<String, String> arg : argMap.entrySet()) {
				LOG.debug(arg.getKey() + ": " + arg.getValue());
			}

			boolean forceUpdate = false;
			String docName = null;
			int docIndex = -1;
			int docId = -1;
			Set<Integer> docIds = null;

			// query
			String queryPath = null;
			String queryString = null;
			String authorQueryString = null;
			String titleQueryString = null;
			int maxDocs = -1;
			int decimalPlaces = -1;
			Boolean expandInflections = null;

			// lexicon handling
			String lexiconDirPath = null;
			String lexiconRegexPath = null;
			String word = null;

			// snippets
			int snippetCount = -1;
			double minWeight = 0.0;
			boolean includeText = false;
			boolean includeGraphics = false;
			String snippetJson = null;

			// word images
			int startOffset = -1;

			// suggestions
			String suggestion = null;
			String suggestion2 = null;
			String user = null;
			String languageCode = null;
			String fontCode = null;
			String ip = "1.2.3.4";

			for (Entry<String, String> argMapEntry : argMap.entrySet()) {
				String argName = argMapEntry.getKey();
				String argValue = argMapEntry.getValue();

				if (argName.equals("forceUpdate")) {
					forceUpdate = argValue.equals("true");
				} else if (argName.equals("docName")) {
					docName = argValue;
				} else if (argName.equals("docIndex")) {
					docIndex = Integer.parseInt(argValue);
				} else if (argName.equals("docId")) {
					docId = Integer.parseInt(argValue);
				} else if (argName.equals("queryFile")) {
					queryPath = argValue;
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
				} else if (argName.equals("expand")) {
					expandInflections = argValue.equals("true");
				} else if (argName.equals("lexiconDir")) {
					lexiconDirPath = argValue;
				} else if (argName.equals("lexiconRegex")) {
					lexiconRegexPath = argValue;
				} else if (argName.equals("word")) {
					word = argValue;
				} else if (argName.equals("snippetCount")) {
					snippetCount = Integer.parseInt(argValue);
				} else if (argName.equals("minWeight")) {
					minWeight = Double.parseDouble(argValue);
				} else if (argName.equals("startOffset")) {
					startOffset = Integer.parseInt(argValue);
				} else if (argName.equals("suggestion")) {
					suggestion = argValue;
				} else if (argName.equals("suggestion2")) {
					suggestion2 = argValue;
				} else if (argName.equals("languageCode")) {
					languageCode = argValue;
				} else if (argName.equals("fontCode")) {
					fontCode = argValue;
				} else if (argName.equals("ip")) {
					ip = argValue;
				} else if (argName.equals("includeText")) {
					includeText = argValue.equalsIgnoreCase("true");
				} else if (argName.equals("includeGraphics")) {
					includeGraphics = argValue.equalsIgnoreCase("true");
				} else if (argName.equals("snippet")) {
					snippetJson = argValue;
				} else if (argName.equalsIgnoreCase("docIds")) {
					if (argValue.length() > 0) {
						String[] idArray = argValue.split(",");
						docIds = new HashSet<>();
						for (String id : idArray)
							docIds.add(Integer.parseInt(id));
					}
				} else if (argName.equals("startOffset")) {
					startOffset = Integer.parseInt(argValue);
				} else if (argName.equals("user")) {
					user = argValue;
				} else {
					throw new RuntimeException("Unknown option: " + argName);
				}
			}

			JochreSearchConfig searchConfig = new JochreSearchConfig(configId, config);

			JochreSearchManager searchManager = JochreSearchManager.getInstance(searchConfig);
			FeedbackDAO feedbackDAO = null;
			if (searchConfig.hasDatabase()) {
				feedbackDAO = FeedbackDAO.getInstance(searchConfig.getDataSource());
			}

			PrintWriter out = null;
			if (output.isLeft())
				out = output.getLeft();

			switch (command) {
			case index: {
				JochreSearchManager manager = JochreSearchManager.getInstance(searchConfig);
				SearchStatusHolder statusHolder = SearchStatusHolder.getInstance();
				JochreIndexBuilder builder = new JochreIndexBuilder(searchConfig, manager, forceUpdate, feedbackDAO, statusHolder);
				new Thread(builder).start();
				out.write("{\"response\":\"index thread started\"}\n");
				break;
			}
			case indexStatus: {
				SearchStatusHolder searchStatusHolder = SearchStatusHolder.getInstance();
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

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
			}
			case refresh: {
				JochreSearchManager manager = JochreSearchManager.getInstance(searchConfig);
				manager.getManager().maybeRefresh();
				out.write("{\"response\":\"index reader refreshed\"}\n");
				break;
			}
			case search:
			case highlight:
			case snippets: {
				IndexSearcher indexSearcher = searchManager.getManager().acquire();
				try {
					JochreQuery query = new JochreQuery(searchConfig);

					if (queryPath == null && queryString == null)
						throw new RuntimeException("For command " + command + " queryFile or query is required");

					if (queryPath != null) {
						Map<String, String> queryArgs = StringUtils.getArgMap(queryPath);
						for (String argName : queryArgs.keySet()) {
							String argValue = queryArgs.get(argName);
							if (argName.equals("query")) {
								queryString = argValue;
							} else if (argName.equals("author")) {
								authorQueryString = argValue;
							} else if (argName.equals("title")) {
								titleQueryString = argValue;
							} else if (argName.equals("maxDocs")) {
								maxDocs = Integer.parseInt(argValue);
							} else if (argName.equals("decimalPlaces")) {
								decimalPlaces = Integer.parseInt(argValue);
							} else if (argName.equals("expand")) {
								expandInflections = argValue.equals("true");
							} else {
								throw new RuntimeException("Unknown option in queryFile: " + argName);
							}
						}
					}

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
						JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, searchConfig);

						switch (command) {
						case search: {
							long resultCount = searcher.search(query, out);

							if (feedbackDAO != null) {
								FeedbackQuery feedbackQuery = new FeedbackQuery(user, ip, feedbackDAO);
								feedbackQuery.setResultCount((int) resultCount);
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
							if (docIds == null) {
								TopDocs topDocs = searcher.search(query);

								docIds = new LinkedHashSet<>();
								for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
									docIds.add(scoreDoc.doc);
									LOG.debug("### Next document");
									Document doc = indexSearcher.doc(scoreDoc.doc);
									for (IndexableField field : doc.getFields()) {
										if (!field.name().equals(JochreIndexField.text.name()) && !field.name().startsWith(JochreIndexField.rect.name())
												&& !field.name().startsWith(JochreIndexField.start.name()))
											LOG.debug(field.toString());
									}
								}
							}
							Set<String> fields = new HashSet<>();
							fields.add(JochreIndexField.text.name());

							Highlighter highlighter = new LuceneQueryHighlighter(query, indexSearcher, fields);
							HighlightManager highlightManager = new HighlightManager(indexSearcher, fields, searchConfig);
							highlightManager.setDecimalPlaces(query.getDecimalPlaces());
							highlightManager.setMinWeight(minWeight);
							highlightManager.setIncludeText(includeText);
							highlightManager.setIncludeGraphics(includeGraphics);
							if (snippetCount > 0)
								highlightManager.setSnippetCount(snippetCount);

							if (command == Command.highlight) {
								highlightManager.highlight(highlighter, docIds, out);
							} else {
								highlightManager.findSnippets(highlighter, docIds, out);
							}
							break;
						}
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
					out.write("\n");
					out.flush();
				} finally {
					searchManager.getManager().release(indexSearcher);
				}
				break;
			}
			case textSnippet: {
				if (snippetJson == null)
					throw new JochreException("Command " + command + " requires a snippet");
				IndexSearcher indexSearcher = searchManager.getManager().acquire();
				try {
					Snippet snippet = new Snippet(snippetJson);

					if (LOG.isDebugEnabled()) {
						Document doc = indexSearcher.doc(snippet.getDocId());
						LOG.debug("Snippet in: " + doc.get(JochreIndexField.path.name()));
					}

					Set<String> fields = new HashSet<>();
					fields.add(JochreIndexField.text.name());
					HighlightManager highlightManager = new HighlightManager(indexSearcher, fields, searchConfig);
					String text = highlightManager.displaySnippet(snippet);

					out.write(text);
				} finally {
					searchManager.getManager().release(indexSearcher);
				}
				break;
			}
			case imageSnippet: {
				if (snippetJson == null)
					throw new JochreException("Command " + command + " requires a snippet");
				IndexSearcher indexSearcher = searchManager.getManager().acquire();
				try {
					Snippet snippet = new Snippet(snippetJson);

					if (LOG.isDebugEnabled()) {
						Document doc = indexSearcher.doc(snippet.getDocId());
						LOG.debug("Snippet in: " + doc.get(JochreIndexField.path.name()));
					}
					Set<String> fields = new HashSet<>();
					fields.add(JochreIndexField.text.name());
					HighlightManager highlightManager = new HighlightManager(indexSearcher, fields, searchConfig);
					ImageSnippet imageSnippet = highlightManager.getImageSnippet(snippet);
					ImageOutputStream ios = ImageIO.createImageOutputStream(output.getRight());
					BufferedImage image = imageSnippet.getImage();
					ImageReader imageReader = ImageIO.getImageReadersByMIMEType("image/png").next();
					ImageWriter imageWriter = ImageIO.getImageWriter(imageReader);
					imageWriter.setOutput(ios);
					imageWriter.write(image);
					ios.flush();
				} finally {
					searchManager.getManager().release(indexSearcher);
				}
				break;
			}
			case word: {
				if (startOffset < 0)
					throw new JochreException("Command " + command + " requires a startOffset");
				if (docId < 0 && (docName == null || docIndex < 0))
					throw new RuntimeException("For command " + command + " either a docName and docIndex, or a docId is required");

				IndexSearcher indexSearcher = searchManager.getManager().acquire();
				try {
					JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, searchConfig);
					if (docId < 0) {
						Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
						docId = docs.keySet().iterator().next();
					}
					JochreIndexDocument jochreDoc = new JochreIndexDocument(indexSearcher, docId, searchConfig);
					JochreIndexWord jochreWord = jochreDoc.getWord(startOffset);
					String word1 = jochreWord.getText();
					String word2 = null;
					if (word1.contains(JochreSearchConstants.INDEX_NEWLINE)) {
						word2 = word1.substring(word1.indexOf(JochreSearchConstants.INDEX_NEWLINE) + 1);
						word1 = word1.substring(0, word1.indexOf(JochreSearchConstants.INDEX_NEWLINE));
					}
					JsonFactory jsonFactory = new JsonFactory();
					JsonGenerator jsonGen = jsonFactory.createGenerator(out);
					jsonGen.writeStartObject();
					jsonGen.writeStringField("word", word1);
					if (word2 != null)
						jsonGen.writeStringField("word2", word2);
					jsonGen.writeEndObject();
					jsonGen.flush();
				} finally {
					searchManager.getManager().release(indexSearcher);
				}
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

				IndexSearcher indexSearcher = searchManager.getManager().acquire();
				try {
					JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, searchConfig);
					if (docId < 0) {
						Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
						docId = docs.keySet().iterator().next();
					}

					Document doc = indexSearcher.doc(docId);

					JsonFactory jsonFactory = new JsonFactory();
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
				} finally {
					searchManager.getManager().release(indexSearcher);
				}
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

				IndexSearcher indexSearcher = searchManager.getManager().acquire();
				try {
					JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, searchConfig);
					if (docId < 0) {
						Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
						docId = docs.keySet().iterator().next();
					}
					JochreIndexTermLister lister = new JochreIndexTermLister(docId, indexSearcher);
					lister.list(out);
					out.write("\n");
					out.flush();
				} finally {
					searchManager.getManager().release(indexSearcher);
				}
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

				IndexSearcher indexSearcher = searchManager.getManager().acquire();
				try {
					JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, searchConfig);
					if (docId < 0) {
						Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
						docId = docs.keySet().iterator().next();
					}
					JochreIndexDocument jochreDoc = new JochreIndexDocument(indexSearcher, docId, searchConfig);
					JochreIndexWord jochreWord = jochreDoc.getWord(startOffset);
					LOG.debug("jochreDoc: " + jochreDoc.getPath());
					LOG.debug("word: " + jochreWord.getText());
					LOG.debug("startOffset: " + jochreWord.getStartOffset());
					BufferedImage wordImage = jochreWord.getImage();

					ImageOutputStream ios = ImageIO.createImageOutputStream(output.getRight());
					ImageReader imageReader = ImageIO.getImageReadersByMIMEType("image/png").next();
					ImageWriter imageWriter = ImageIO.getImageWriter(imageReader);
					imageWriter.setOutput(ios);
					imageWriter.write(wordImage);
					ios.flush();
				} finally {
					searchManager.getManager().release(indexSearcher);
				}
				break;
			}
			case suggest: {
				if (feedbackDAO == null)
					throw new RuntimeException("For command " + command + " a database is required");
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
				if (user == null)
					throw new RuntimeException("For command " + command + " user is required");
				if (fontCode == null)
					throw new RuntimeException("For command " + command + " fontCode is required");
				if (languageCode == null)
					throw new RuntimeException("For command " + command + " languageCode is required");

				String fullSuggestion = suggestion;
				if (suggestion2 != null && suggestion2.length() > 0)
					fullSuggestion += JochreSearchConstants.INDEX_NEWLINE + suggestion2;

				IndexSearcher indexSearcher = searchManager.getManager().acquire();
				try {

					JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, searchConfig);
					if (docId < 0) {
						Map<Integer, Document> docs = searcher.findDocument(docName, docIndex);
						docId = docs.keySet().iterator().next();
					}
					FeedbackSuggestion sug = new FeedbackSuggestion(indexSearcher, docId, startOffset, fullSuggestion, user, ip, fontCode, languageCode,
							feedbackDAO, searchConfig);
					sug.save();
				} finally {
					searchManager.getManager().release(indexSearcher);
				}
				out.write("{\"response\":\"suggestion saved\"}\n");
				break;
			}
			case serializeLexicon: {
				if (lexiconDirPath == null)
					throw new RuntimeException("For command " + command + " lexiconDir is required");
				if (lexiconRegexPath == null)
					throw new RuntimeException("For command " + command + " lexiconRegex is required");

				File lexiconDir = new File(lexiconDirPath);
				File[] lexiconFiles = lexiconDir.listFiles();
				TextFileLexicon lexicon = new TextFileLexicon(searchConfig.getLocale());

				File regexFile = new File(lexiconRegexPath);
				Scanner regexScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(regexFile), "UTF-8")));
				LexicalEntryReader lexicalEntryReader = new RegexLexicalEntryReader(regexScanner);

				for (File file : lexiconFiles) {
					LOG.info("Adding " + file.getName());
					lexicon.addLexiconFile(file, lexicalEntryReader);
				}

				File outFile = searchConfig.getLexiconFile();
				lexicon.serialize(outFile);
				break;
			}
			case deserializeLexicon: {
				if (word == null)
					throw new RuntimeException("For command " + command + " word is required");

				Lexicon lexicon = searchConfig.getLexicon();
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

	/**
	 * Command-line entry point.
	 */
	public static void main(String[] args) throws Exception {

		Map<String, String> argMap = new HashMap<>();

		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos + 1);
			argMap.put(argName, argValue);
		}

		String logConfigPath = argMap.get("logConfigFile");
		if (logConfigPath != null) {
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

			File slf4jFile = new File(logConfigPath);

			if (slf4jFile.exists()) {
				JoranConfigurator configurator = new JoranConfigurator();
				configurator.setContext(loggerContext);
				// Call context.reset() to clear any previous configuration,
				// e.g. default configuration
				loggerContext.reset();
				configurator.doConfigure(slf4jFile);
			} else {
				throw new Exception("LogConfigFile not found: " + logConfigPath);
			}
			argMap.remove("logConfigFile");
		}

		long startTime = System.currentTimeMillis();
		try {
			String configId = argMap.get("configId");
			argMap.remove("configId");

			String command = argMap.get("command");
			Either<PrintWriter, OutputStream> output;
			if (command.equals(JochreSearch.Command.imageSnippet.name()) && !command.equals(JochreSearch.Command.wordImage.name())) {
				String outFilePath = argMap.get("outFile");
				argMap.remove("outFile");
				File outfile = new File(outFilePath);
				FileOutputStream fos = new FileOutputStream(outfile);
				output = Either.ofRight(fos);
			} else {
				PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
				output = Either.ofLeft(out);
			}

			JochreSearch main = new JochreSearch(configId);
			main.execute(argMap, output);

			if (output.isRight())
				output.getRight().close();
		} finally {
			long endTime = System.currentTimeMillis();
			LOG.info("Total time: " + (endTime - startTime) + " ms");
		}
	}

}
