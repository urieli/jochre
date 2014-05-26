package com.joliciel.jochre.search;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.joliciel.talismane.utils.LogUtils;

/**
 * A reusable index searcher tied to a given index directory.
 * @author Assaf Urieli
 *
 */
class JochreIndexSearcherImpl implements JochreIndexSearcher {
	private static final Log LOG = LogFactory.getLog(JochreIndexSearcherImpl.class);
	private File indexDir;
	private IndexReader indexReader;
	private IndexSearcher indexSearcher;
	private DecimalFormatSymbols enSymbols = new DecimalFormatSymbols(Locale.US);
	private Map<Integer,DecimalFormat> decimalFormats = new HashMap<Integer, DecimalFormat>();
	private SearchService searchService;
	
	public JochreIndexSearcherImpl(File indexDir) {
		super();
		this.setIndexDir(indexDir);
	}
	
	private void setIndexDir(File indexDir) {
		try {
			this.indexDir = indexDir;
			if (!indexDir.exists()) {
				throw new RuntimeException("Index directory does not exist: " + indexDir.getAbsolutePath());
			}
			Directory directory = FSDirectory.open(this.indexDir);
			indexReader = DirectoryReader.open(directory);
			
			indexSearcher = new IndexSearcher(indexReader);		
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	@Override
	public TopDocs search(JochreQuery jochreQuery) {
		try {
			TopDocs topDocs = indexSearcher.search(jochreQuery.getLuceneQuery(), jochreQuery.getLuceneFilter(), jochreQuery.getMaxDocs());

			if (LOG.isTraceEnabled()) {
				LOG.trace("Search results: ");
				for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
					Document doc = indexSearcher.doc(scoreDoc.doc);
					String extId = doc.get("id");
					LOG.trace(extId + "(docId " + scoreDoc.doc + "): " + scoreDoc.score);
				}
			}
			return topDocs;
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}		
	}

	@Override
	public void search(JochreQuery jochreQuery, Writer out) {
		try {
			TopDocs topDocs = this.search(jochreQuery);
			DecimalFormat df = this.getDecimalFormat(jochreQuery.getDecimalPlaces());
			
			JsonFactory jsonFactory = new JsonFactory();
			JsonGenerator jsonGen = jsonFactory.createGenerator(out);

			jsonGen.writeStartObject();
			
			for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
				Document doc = indexSearcher.doc(scoreDoc.doc);
				jsonGen.writeObjectFieldStart(doc.get("id"));
				jsonGen.writeNumberField("docId", scoreDoc.doc);
				jsonGen.writeNumberField("startPage", Integer.parseInt(doc.get("startPage")));
				jsonGen.writeNumberField("endPage", Integer.parseInt(doc.get("endPage")));
				jsonGen.writeNumberField("index", Integer.parseInt(doc.get("index")));
				jsonGen.writeStringField("path", doc.get("path"));
				String author = doc.get("author");
				if (author!=null)
					jsonGen.writeStringField("author", doc.get("author"));
				String title = doc.get("title");
				if (title!=null)
					jsonGen.writeStringField("title", doc.get("title"));
				String keywords = doc.get("keywords");
				if (keywords!=null)
					jsonGen.writeStringField("keywords", doc.get("keywords"));
				
				double roundedScore = df.parse(df.format(scoreDoc.score)).doubleValue();
				jsonGen.writeNumberField("score", roundedScore);
				
				jsonGen.writeEndObject();
			}

			jsonGen.writeEndObject();
			jsonGen.flush();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (ParseException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	private DecimalFormat getDecimalFormat(int decimalPlaces) {
		DecimalFormat df = this.decimalFormats.get(decimalPlaces);
		if (df==null) {
			String dfFormat = "0.";
			for (int i = 0; i<decimalPlaces;i++) {
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
}
