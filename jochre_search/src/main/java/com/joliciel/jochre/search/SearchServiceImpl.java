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
	public JochreXmlLetter newLetter(JochreXmlWord word, String text, int left, int top, int right, int bottom) {
		JochreXmlLetterImpl letter = new JochreXmlLetterImpl(word, text, left, top, right, bottom);
		return letter;
	}
	public JochreXmlWord newWord(JochreXmlRow row, String text, int left, int top, int right, int bottom) {
		JochreXmlWordImpl word = new JochreXmlWordImpl(row, text, left, top, right, bottom);
		return word;
	}
	public JochreXmlRow newRow(JochreXmlParagraph paragraph, int left, int top, int right, int bottom) {
		JochreXmlRowImpl row = new JochreXmlRowImpl(paragraph, left, top, right, bottom);
		return row;
	}
	public JochreXmlParagraph newParagraph(JochreXmlImage page, int left, int top, int right, int bottom) {
		JochreXmlParagraphImpl paragraph = new JochreXmlParagraphImpl(page, left, top, right, bottom);
		return paragraph;
	}
	public JochreXmlImage newImage(String fileNameBase, int pageIndex, int imageIndex, int width, int height) {
		JochreXmlImageImpl page = new JochreXmlImageImpl(fileNameBase, pageIndex, imageIndex, width, height);
		return page;
	}
	public JochreXmlDocument newDocument() {
		JochreXmlDocumentImpl doc = new JochreXmlDocumentImpl();
		return doc;
	}
	@Override
	public JochreXmlReader getJochreXmlReader(JochreXmlDocument doc) {
		JochreXmlReaderImpl reader = new JochreXmlReaderImpl(doc);
		reader.setSearchService(this);
		return reader;
	}
	@Override
	public JochreIndexBuilder getJochreIndexBuilder(File indexDir) {
		JochreIndexBuilderImpl builder = new JochreIndexBuilderImpl(indexDir);
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
	
	@Override
	public JochreIndexDocument newJochreIndexDocument(File directory,
			int index, StringBuilder sb, CoordinateStorage coordinateStorage,
			int startPage, int endPage, Map<String, String> fields) {
		JochreIndexDocumentImpl doc = new JochreIndexDocumentImpl(directory, index, sb, coordinateStorage, startPage, endPage, fields);
		return doc;
	}
}
