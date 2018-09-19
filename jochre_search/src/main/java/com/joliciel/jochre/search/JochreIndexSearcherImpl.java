package com.joliciel.jochre.search;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * A reusable index searcher tied to a given index directory.
 * 
 * @author Assaf Urieli
 *
 */
class JochreIndexSearcherImpl implements JochreIndexSearcher {
	private static final Logger LOG = LoggerFactory.getLogger(JochreIndexSearcherImpl.class);
	private File indexDir;
	private File contentDir;
	private IndexReader indexReader;
	private IndexSearcher indexSearcher;
	private DecimalFormatSymbols enSymbols = new DecimalFormatSymbols(Locale.US);
	private Map<Integer, DecimalFormat> decimalFormats = new HashMap<>();
	private SearchService searchService;

	public JochreIndexSearcherImpl(File indexDir, File contentDir) {
		super();
		this.setIndexDir(indexDir);
		this.contentDir = contentDir;
	}

	private void setIndexDir(File indexDir) {
		try {
			this.indexDir = indexDir;
			if (!indexDir.exists()) {
				throw new RuntimeException("Index directory does not exist: " + indexDir.getAbsolutePath());
			}
			Path path = this.indexDir.toPath();
			Directory directory = FSDirectory.open(path);

			try {
				indexReader = DirectoryReader.open(directory);

				indexSearcher = new IndexSearcher(indexReader);
			} catch (IndexNotFoundException e) {
				LOG.info("No index at : " + indexDir.getAbsolutePath());
			}
		} catch (IOException e) {
			LOG.error("Failed set index dir to " + indexDir.getAbsolutePath(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public TopDocs search(JochreQuery jochreQuery) {
		try {
			TopDocs topDocs = indexSearcher.search(jochreQuery.getLuceneQuery(), jochreQuery.getMaxDocs());

			if (LOG.isTraceEnabled()) {
				LOG.trace("Search results: ");
				for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
					Document doc = indexSearcher.doc(scoreDoc.doc);
					String extId = doc.get(JochreIndexField.id.name());
					LOG.trace(extId + "(docId " + scoreDoc.doc + "): " + scoreDoc.score);
				}
			}
			return topDocs;
		} catch (IOException e) {
			LOG.error("Failed search using jochreQuery " + jochreQuery.toString(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public long search(JochreQuery jochreQuery, Writer out) {
		try {
			TopDocs topDocs = this.search(jochreQuery);
			DecimalFormat df = this.getDecimalFormat(jochreQuery.getDecimalPlaces());

			JsonFactory jsonFactory = new JsonFactory();
			JsonGenerator jsonGen = jsonFactory.createGenerator(out);

			jsonGen.writeStartArray();

			for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
				Document doc = indexSearcher.doc(scoreDoc.doc);
				jsonGen.writeStartObject();
				jsonGen.writeNumberField("docId", scoreDoc.doc);
				jsonGen.writeStringField(JochreIndexField.name.name(), doc.get(JochreIndexField.name.name()));
				jsonGen.writeNumberField(JochreIndexField.startPage.name(), doc.getField(JochreIndexField.startPage.name()).numericValue().intValue());
				jsonGen.writeNumberField(JochreIndexField.endPage.name(), doc.getField(JochreIndexField.endPage.name()).numericValue().intValue());
				jsonGen.writeNumberField(JochreIndexField.sectionNumber.name(),
						Integer.parseInt(doc.getField(JochreIndexField.sectionNumber.name()).stringValue()));
				jsonGen.writeStringField(JochreIndexField.path.name(), doc.get(JochreIndexField.path.name()));
				jsonGen.writeStringField(JochreIndexField.id.name(), doc.get(JochreIndexField.id.name()));
				String author = doc.get(JochreIndexField.author.name());
				if (author != null)
					jsonGen.writeStringField(JochreIndexField.author.name(), author);
				String title = doc.get(JochreIndexField.title.name());
				if (title != null)
					jsonGen.writeStringField(JochreIndexField.title.name(), title);
				String url = doc.get(JochreIndexField.url.name());
				if (url != null)
					jsonGen.writeStringField(JochreIndexField.url.name(), url);
				String authorLang = doc.get(JochreIndexField.authorLang.name());
				if (authorLang != null)
					jsonGen.writeStringField(JochreIndexField.authorLang.name(), authorLang);
				String titleLang = doc.get(JochreIndexField.titleLang.name());
				if (titleLang != null)
					jsonGen.writeStringField(JochreIndexField.titleLang.name(), titleLang);
				String volume = doc.get(JochreIndexField.volume.name());
				if (volume != null)
					jsonGen.writeStringField(JochreIndexField.volume.name(), volume);
				String publisher = doc.get(JochreIndexField.publisher.name());
				if (publisher != null)
					jsonGen.writeStringField(JochreIndexField.publisher.name(), publisher);
				String date = doc.get(JochreIndexField.date.name());
				if (date != null)
					jsonGen.writeStringField(JochreIndexField.date.name(), date);

				double roundedScore = df.parse(df.format(scoreDoc.score)).doubleValue();
				jsonGen.writeNumberField("score", roundedScore);

				jsonGen.writeEndObject();
			}

			jsonGen.writeEndArray();
			jsonGen.flush();

			return topDocs.totalHits;
		} catch (IOException e) {
			LOG.error("Failed search using jochreQuery " + jochreQuery.toString(), e);
			throw new RuntimeException(e);
		} catch (ParseException e) {
			LOG.error("Failed search using jochreQuery " + jochreQuery.toString(), e);
			throw new RuntimeException(e);
		}
	}

	private DecimalFormat getDecimalFormat(int decimalPlaces) {
		DecimalFormat df = this.decimalFormats.get(decimalPlaces);
		if (df == null) {
			String dfFormat = "0.";
			for (int i = 0; i < decimalPlaces; i++) {
				dfFormat += "0";
			}
			df = new DecimalFormat(dfFormat, enSymbols);
			decimalFormats.put(decimalPlaces, df);
		}
		return df;
	}

	@Override
	public File getIndexDir() {
		return indexDir;
	}

	@Override
	public IndexSearcher getIndexSearcher() {
		return indexSearcher;
	}

	public SearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	@Override
	public Map<Integer, Document> findDocuments(String name) {
		return this.findDocumentsInternal(name, -1);
	}

	@Override
	public Map<Integer, Document> findDocument(String name, int index) {
		return this.findDocumentsInternal(name, index);
	}

	private Map<Integer, Document> findDocumentsInternal(String name, int index) {
		try {
			Map<Integer, Document> docs = new LinkedHashMap<>();
			BooleanQuery.Builder builder = new Builder();
			TermQuery termQuery = new TermQuery(new Term(JochreIndexField.name.name(), name));
			builder.add(termQuery, Occur.MUST);
			if (index >= 0) {
				Query indexQuery = new TermQuery(new Term(JochreIndexField.sectionNumber.name(), "" + index));
				builder.add(indexQuery, Occur.MUST);
			}
			Query query = builder.build();
			LOG.debug(query.toString());
			TopDocs topDocs = indexSearcher.search(query, 200);
			for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
				Document doc = indexSearcher.doc(scoreDoc.doc);
				docs.put(scoreDoc.doc, doc);
				LOG.debug("Found doc " + scoreDoc.doc + ", name: " + doc.get(JochreIndexField.name.name()) + ", section: "
						+ doc.get(JochreIndexField.sectionNumber.name()));
			}
			return docs;
		} catch (IOException e) {
			LOG.error("Failed find documents for name " + name + ", index " + index, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public File getContentDir() {
		return contentDir;
	}
}
