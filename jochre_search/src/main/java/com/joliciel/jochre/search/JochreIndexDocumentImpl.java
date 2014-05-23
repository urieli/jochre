package com.joliciel.jochre.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.Scanner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;

import com.joliciel.talismane.utils.LogUtils;

class JochreIndexDocumentImpl implements JochreIndexDocument {
	private static final Log LOG = LogFactory.getLog(JochreIndexDocumentImpl.class);
	private String contents;
	private CoordinateStorage coordinateStorage;
	private Document doc;
	private int index;
	private String path;
	private File directory;
	
	public JochreIndexDocumentImpl(IndexSearcher indexSearcher, int docId) {
		try {
			this.doc = indexSearcher.doc(docId);
			this.index = Integer.parseInt(this.doc.get("index"));
			this.path = this.doc.get("path");
			this.directory = new File(this.path);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getContents() {
		try {
			if (this.contents==null) {
				StringBuilder sb = new StringBuilder();
				File contentFile = new File(directory, "text" + index + ".txt");
				Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(contentFile), "UTF-8")));
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					sb.append(line);
					sb.append("\n");
				}
				scanner.close();
			    this.contents = sb.toString();
	
			}
			return contents;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}

	}

	@Override
	public CoordinateStorage getCoordinateStorage() {
		try {
			if (coordinateStorage==null) {
				File coordinateFile = new File(directory, "offsets" + index + ".obj");
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(coordinateFile));
				coordinateStorage = (CoordinateStorage) ois.readObject();
			}
	
			return coordinateStorage;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public File getDirectory() {
		return directory;
	}
	
	
}
