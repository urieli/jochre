package com.joliciel.jochre.search;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.apache.lucene.index.IndexWriter;

public interface JochreIndexDocument {
	
	/**
	 * The full string contents of the document.
	 * @return
	 */
	public String getContents();
	
	/**
	 * Save the document to the Lucene index.
	 * @param indexWriter
	 */
	public void save(IndexWriter indexWriter);
	
	/**
	 * Get the rectangle enclosing a particular text line.
	 * @param pageIndex
	 * @param rowIndex
	 * @return
	 */
	public Rectangle getRectangle(int pageIndex, int rowIndex);
	
	/**
	 * The index of the first page contained in this document.
	 * @return
	 */
	public int getStartPage();
	
	/**
	 * The index of the last page contained in this document.
	 * @return
	 */
	public int getEndPage();
	
	/**
	 * The document's name, used as a prefix for various files in the document's directory.
	 * @return
	 */
	public String getName();
	
	/**
	 * Get the image corresponding to a particular page index.
	 * @param pageIndex
	 * @return
	 */
	public BufferedImage getImage(int pageIndex);

	/**
	 * Return the content index of the first character on a given row.
	 * @param pageIndex
	 * @param rowIndex
	 * @return
	 */
	public int getStartIndex(int pageIndex, int rowIndex);

	/**
	 * Return the content index following the last character on a given row.
	 * @param pageIndex
	 * @param rowIndex
	 * @return
	 */
	public int getEndIndex(int pageIndex, int rowIndex);
	
	/**
	 * Get the number of rows on a given page.
	 * @param pageIndex
	 * @return
	 */
	public int getRowCount(int pageIndex);
	
	/**
	 * Get the image corresponding to a word starting at the given offset, or at the maximum offset prior to this one.
	 * @param startOffset
	 * @return
	 */
	public BufferedImage getWordImage(int startOffset);

	/**
	 * Get the word starting at the given offset, or at the maximum offset prior to this one.
	 * @param startOffset
	 * @return
	 */
	String getWord(int startOffset);
}