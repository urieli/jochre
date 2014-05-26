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

import java.io.File;
import java.util.Map;

import org.apache.lucene.search.IndexSearcher;

class SearchServiceImpl implements SearchServiceInternal {
	public SearchLetter newLetter(SearchWord word, String text, int left, int top, int right, int bottom) {
		SearchLetterImpl letter = new SearchLetterImpl(word, text, left, top, right, bottom);
		return letter;
	}
	public SearchWord newWord(SearchRow row, String text, int left, int top, int right, int bottom) {
		SearchWordImpl word = new SearchWordImpl(row, text, left, top, right, bottom);
		return word;
	}
	public SearchRow newRow(SearchParagraph paragraph, int left, int top, int right, int bottom) {
		SearchRowImpl row = new SearchRowImpl(paragraph, left, top, right, bottom);
		return row;
	}
	public SearchParagraph newParagraph(SearchImage page, int left, int top, int right, int bottom) {
		SearchParagraphImpl paragraph = new SearchParagraphImpl(page, left, top, right, bottom);
		return paragraph;
	}
	public SearchImage newImage(String fileNameBase, int pageIndex, int imageIndex, int width, int height) {
		SearchImageImpl page = new SearchImageImpl(fileNameBase, pageIndex, imageIndex, width, height);
		return page;
	}
	public SearchDocument newDocument() {
		SearchDocumentImpl doc = new SearchDocumentImpl();
		return doc;
	}
	@Override
	public JochreXmlReader getJochreXmlReader(SearchDocument doc) {
		JochreXmlReaderImpl reader = new JochreXmlReaderImpl(doc);
		reader.setSearchService(this);
		return reader;
	}
	@Override
	public JochreIndexBuilder getJochreIndexBuilder(File indexDir,
			File documentDir) {
		JochreIndexBuilderImpl builder = new JochreIndexBuilderImpl(indexDir, documentDir);
		builder.setSearchService(this);
		return builder;
	}
	@Override
	public CoordinateStorage getCoordinateStorage() {
		CoordinateStorageImpl storage = new CoordinateStorageImpl();
		return storage;
	}
	@Override
	public JochreQuery getJochreQuery(Map<String, String> argMap) {
		JochreQueryImpl query = new JochreQueryImpl(argMap);
		return query;
	}
	@Override
	public JochreIndexSearcher getJochreIndexSearcher(File indexDir) {
		JochreIndexSearcherImpl searcher = new JochreIndexSearcherImpl(indexDir);
		searcher.setSearchService(this);
		return searcher;
	}
	@Override
	public JochreIndexDocument getJochreIndexDocument(
			IndexSearcher indexSearcher, int docId) {
		JochreIndexDocumentImpl doc = new JochreIndexDocumentImpl(indexSearcher, docId);
		return doc;
	}
}
