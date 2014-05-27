package com.joliciel.jochre.search.webClient;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class SearchDocument {
	private static final Log LOG = LogFactory.getLog(SearchDocument.class);
	private int docId;
	private String baseName;
	private String path;
	private int startPage;
	private int endPage;
	private int index;
	private double score;
	private String author;
	private String title;
	private String url;
	
	public SearchDocument(String baseName, JsonParser jsonParser) {
		try {
			this.baseName = baseName;
			while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
				String fieldName = jsonParser.getCurrentName();
			
				if (fieldName.equals("docId")) {
					docId = jsonParser.nextIntValue(0);
				} else if (fieldName.equals("path")) {
					path = jsonParser.nextTextValue();
				} else if (fieldName.equals("author")) {
					author = jsonParser.nextTextValue();
				} else if (fieldName.equals("title")) {
					title = jsonParser.nextTextValue();
				} else if (fieldName.equals("url")) {
					url = jsonParser.nextTextValue();
				} else if (fieldName.equals("startPage")) {
					startPage = jsonParser.nextIntValue(0);
				} else if (fieldName.equals("endPage")) {
					endPage = jsonParser.nextIntValue(0);
				} else if (fieldName.equals("index")) {
					index = jsonParser.nextIntValue(0);
				} else if (fieldName.equals("score")) {
					jsonParser.nextValue();
					score = jsonParser.getDoubleValue();
				}
			}
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
	public void setDocId(int docId) {
		this.docId = docId;
	}
	public String getBaseName() {
		return baseName;
	}
	public void setBaseName(String baseName) {
		this.baseName = baseName;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	
	
	public int getStartPage() {
		return startPage;
	}
	public void setStartPage(int startPage) {
		this.startPage = startPage;
	}
	public int getEndPage() {
		return endPage;
	}
	public void setEndPage(int endPage) {
		this.endPage = endPage;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	
	
	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
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

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getTitle() {
		if (title==null)
			return baseName;
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	
}
