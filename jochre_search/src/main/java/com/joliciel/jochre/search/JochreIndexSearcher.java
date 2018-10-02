package com.joliciel.jochre.search;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
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
public class JochreIndexSearcher {
	private static final Logger LOG = LoggerFactory.getLogger(JochreIndexSearcher.class);

	private final IndexSearcher indexSearcher;

	private DecimalFormatSymbols enSymbols = new DecimalFormatSymbols(Locale.US);
	private Map<Integer, DecimalFormat> decimalFormats = new HashMap<>();
	private final int maxDocs;

	public JochreIndexSearcher(IndexSearcher indexSearcher, JochreSearchConfig config) {
		this.indexSearcher = indexSearcher;
		this.maxDocs = config.getMaxResults();
	}

	/**
	 * Return paginated results for a query.
	 * 
	 * @param jochreQuery
	 *            the query to run
	 * @param pageNumber
	 *            the page number to return
	 * @param resultsPerPage
	 *            results per page
	 * @return a pair giving the TopDocs corresponding to the paginated results, and
	 *         the total hits
	 * @throws IOException
	 */
	public Pair<TopDocs, Integer> search(JochreQuery jochreQuery, int pageNumber, int resultsPerPage) throws IOException {
		TopDocsCollector<? extends ScoreDoc> topDocsCollector = null;
		switch (jochreQuery.getSortBy()) {
		case Score:
			topDocsCollector = TopScoreDocCollector.create(this.maxDocs);
		case Year:
			topDocsCollector = TopFieldCollector.create(
					new Sort(new SortedNumericSortField(JochreIndexField.yearSort.name(), SortField.Type.INT, !jochreQuery.isSortAscending())), this.maxDocs,
					false, false, false, true);
		}

		indexSearcher.search(jochreQuery.getLuceneQuery(), topDocsCollector);
		TopDocs topDocs = topDocsCollector.topDocs(pageNumber * resultsPerPage, resultsPerPage);
		int totalHits = topDocsCollector.getTotalHits();

		if (LOG.isTraceEnabled()) {
			LOG.trace("Search results: ");
			for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
				Document doc = indexSearcher.doc(scoreDoc.doc);
				String extId = doc.get(JochreIndexField.id.name());
				LOG.trace(extId + "(docId " + scoreDoc.doc + "): " + scoreDoc.score);
			}
		}
		return Pair.of(topDocs, totalHits);
	}

	/**
	 * Write query results in JSON to the provided Writer.
	 * 
	 * @return the number of results
	 */
	public long search(JochreQuery jochreQuery, int pageNumber, int resultsPerPage, Writer out) throws IOException {
		try {
			Pair<TopDocs, Integer> result = this.search(jochreQuery, pageNumber, resultsPerPage);
			DecimalFormat df = this.getDecimalFormat(jochreQuery.getDecimalPlaces());

			JsonFactory jsonFactory = new JsonFactory();
			JsonGenerator jsonGen = jsonFactory.createGenerator(out);

			jsonGen.writeStartObject();
			jsonGen.writeNumberField("totalHits", result.getRight());
			jsonGen.writeArrayFieldStart("results");

			for (ScoreDoc scoreDoc : result.getLeft().scoreDocs) {
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
				String author = doc.get(JochreIndexField.authorEnglish.name());
				if (author != null)
					jsonGen.writeStringField(JochreIndexField.authorEnglish.name(), author);
				String title = doc.get(JochreIndexField.titleEnglish.name());
				if (title != null)
					jsonGen.writeStringField(JochreIndexField.titleEnglish.name(), title);
				String url = doc.get(JochreIndexField.url.name());
				if (url != null)
					jsonGen.writeStringField(JochreIndexField.url.name(), url);
				String authorLang = doc.get(JochreIndexField.author.name());
				if (authorLang != null)
					jsonGen.writeStringField(JochreIndexField.author.name(), authorLang);
				String titleLang = doc.get(JochreIndexField.title.name());
				if (titleLang != null)
					jsonGen.writeStringField(JochreIndexField.title.name(), titleLang);
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
			jsonGen.writeEndObject();
			jsonGen.flush();

			return result.getRight();
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

	/**
	 * Find all documents corresponding to a given name.
	 * 
	 * @throws IOException
	 */
	public Map<Integer, Document> findDocuments(String name) throws IOException {
		return this.findDocumentsInternal(name, -1);
	}

	/**
	 * Find the documents for a given name and index.
	 * 
	 * @throws IOException
	 */
	public Map<Integer, Document> findDocument(String name, int index) throws IOException {
		return this.findDocumentsInternal(name, index);
	}

	private Map<Integer, Document> findDocumentsInternal(String name, int index) throws IOException {
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
	}
}
