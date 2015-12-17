package com.joliciel.jochre.search.webClient;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.joliciel.jochre.search.JochreIndexField;

public class SearchDocument {
	private static final Log LOG = LogFactory.getLog(SearchDocument.class);
	private int docId;
	private String path;
	private int startPage;
	private int endPage;
	private int index;
	private double score;
	private String author;
	private String title;
	private String url;
	private int date;
	private String publisher;
	private String authorLang;
	private String titleLang;
	private String id;
	private String volume;
	
	public SearchDocument(JsonParser jsonParser) {
		try {
			while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
				if (LOG.isTraceEnabled()) {
					LOG.trace(jsonParser.getCurrentToken());
					LOG.trace(jsonParser.getCurrentName());
					LOG.trace(jsonParser.getCurrentLocation());
				}

				String fieldName = jsonParser.getCurrentName();
				
				if (fieldName.equals("docId")) {
					docId = jsonParser.nextIntValue(0);
				} else if (fieldName.equals(JochreIndexField.path.name())) {
					path = jsonParser.nextTextValue();
				} else if (fieldName.equals(JochreIndexField.author.name())) {
					author = jsonParser.nextTextValue();
				} else if (fieldName.equals(JochreIndexField.title.name())) {
					title = jsonParser.nextTextValue();
				} else if (fieldName.equals(JochreIndexField.authorLang.name())) {
					authorLang = jsonParser.nextTextValue();
				} else if (fieldName.equals(JochreIndexField.titleLang.name())) {
					titleLang = jsonParser.nextTextValue();
				} else if (fieldName.equals(JochreIndexField.volume.name())) {
					volume = jsonParser.nextTextValue();
				} else if (fieldName.equals(JochreIndexField.publisher.name())) {
					publisher = jsonParser.nextTextValue();
				} else if (fieldName.equals(JochreIndexField.id.name())) {
					id = jsonParser.nextTextValue();
				} else if (fieldName.equals(JochreIndexField.url.name())) {
					url = jsonParser.nextTextValue();
				} else if (fieldName.equals(JochreIndexField.startPage.name())) {
					startPage = jsonParser.nextIntValue(0);
				} else if (fieldName.equals(JochreIndexField.endPage.name())) {
					endPage = jsonParser.nextIntValue(0);
				} else if (fieldName.equals(JochreIndexField.index.name())) {
					index = jsonParser.nextIntValue(0);
				} else if (fieldName.equals(JochreIndexField.date.name())) {
					date = jsonParser.nextIntValue(0);
				} else if (fieldName.equals("score")) {
					jsonParser.nextValue();
					score = jsonParser.getDoubleValue();
				}
			}
			LOG.debug("Loaded document " + docId);
		} catch (JsonParseException e) {
			LOG.error(e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LOG.error(e);
			throw new RuntimeException(e);
		}
	}
	
	public int getDocId() {
		return docId;
	}

	public String getPath() {
		return path;
	}
	
	public int getStartPage() {
		return startPage;
	}
	public int getEndPage() {
		return endPage;
	}
	public int getIndex() {
		return index;
	}
	
	public double getScore() {
		return score;
	}

	@Override
	public int hashCode() {
		return docId;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SearchDocument other = (SearchDocument) obj;
		if (docId != other.docId)
			return false;
		return true;
	}

	public String getAuthor() {
		return author;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	public int getDate() {
		return date;
	}

	public String getPublisher() {
		return publisher;
	}

	public String getAuthorLang() {
		return authorLang;
	}

	public String getTitleLang() {
		return titleLang;
	}

	public String getId() {
		return id;
	}

	public String getVolume() {
		return volume;
	}
	
	
}
