package com.joliciel.jochre.search;

import java.io.File;

import org.apache.lucene.index.IndexWriter;

public interface JochreIndexDocument {
	public File getDirectory();
	public String getContents();
	public CoordinateStorage getCoordinateStorage();	
	public void save(IndexWriter indexWriter);
	public String getAuthor();
	public String getTitle();
	public String getUrl();
}