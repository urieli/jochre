package com.joliciel.jochre.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexWriter;
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
	private String author;
	private String title;
	private String url;
	private int startPage;
	private int endPage;
	
	@SuppressWarnings("unused")
	private Map<String,String> fields;

	/* Indexed, tokenized, not stored. */
	public static final FieldType TYPE_NOT_STORED = new FieldType();

	/* Indexed, tokenized, stored. */
	public static final FieldType TYPE_STORED = new FieldType();

	/* Not indexed, not tokenized, stored. */
	public static final FieldType TYPE_NOT_INDEXED = new FieldType();

	static {
	    TYPE_NOT_STORED.setIndexed(true);
	    TYPE_NOT_STORED.setTokenized(true);
	    TYPE_NOT_STORED.setStoreTermVectors(true);
	    TYPE_NOT_STORED.setStoreTermVectorPositions(true);
	    TYPE_NOT_STORED.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
	    TYPE_NOT_STORED.freeze();

	    TYPE_STORED.setIndexed(true);
	    TYPE_STORED.setTokenized(true);
	    TYPE_STORED.setStored(true);
	    TYPE_STORED.setStoreTermVectors(true);
	    TYPE_STORED.setStoreTermVectorPositions(true);
	    TYPE_STORED.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
	    TYPE_STORED.freeze();
	    
	    TYPE_NOT_INDEXED.setIndexed(false);
	    TYPE_NOT_INDEXED.setTokenized(false);
	    TYPE_NOT_INDEXED.setStored(true);
	    TYPE_NOT_INDEXED.setStoreTermVectors(false);
	    TYPE_NOT_INDEXED.setStoreTermVectorPositions(false);
	    TYPE_NOT_INDEXED.freeze();
	}
	
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

	public JochreIndexDocumentImpl(File directory, int index, StringBuilder sb, CoordinateStorage coordinateStorage, int startPage, int endPage, Map<String,String> fields) {
		this.directory = directory;
		this.fields = fields;
		this.coordinateStorage = coordinateStorage;
		this.index = index;
		this.startPage = startPage;
		this.endPage = endPage;
		
		this.contents = sb.toString();
		LOG.trace(contents);

		this.author = fields.get("Author");
		this.title = fields.get("Title");
		this.url = fields.get("Keywords");
	}
	
	public void save(IndexWriter indexWriter) {
		try {
			doc = new Document();
			doc.add(new StringField("id", directory.getName(), Field.Store.YES));
			doc.add(new IntField("startPage", startPage, Field.Store.YES));
			doc.add(new IntField("endPage", endPage, Field.Store.YES));
			doc.add(new IntField("index", index, Field.Store.YES));
			doc.add(new Field("text", contents, TYPE_STORED));
			doc.add(new Field("path", directory.getAbsolutePath(), TYPE_NOT_INDEXED));
			
			
			if (author!=null)
				doc.add(new StringField("author", author, Field.Store.YES));
			if (title!=null)
				doc.add(new StringField("title", title, Field.Store.YES));
			if (url!=null)
				doc.add(new StringField("url", url, Field.Store.YES));		

			indexWriter.addDocument(doc);
			
			File offsetPositionFile = new File(this.directory, "offsets" + this.index + ".obj");
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(offsetPositionFile, false));
			oos.writeObject(coordinateStorage);
			oos.flush();
			oos.close();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	@Override
	public String getContents() {
		if (this.contents==null) {
			this.contents = doc.get("text");
		}
		return contents;
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

	public String getAuthor() {
		return author;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}
	
	
}
