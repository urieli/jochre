package com.joliciel.jochre.search.alto;

import java.util.Locale;

import com.joliciel.jochre.search.SearchService;

public class AltoServiceImpl implements AltoServiceInternal {
	private SearchService searchService;
	
	public AltoString newString(AltoTextLine textLine, String text, int left, int top, int width, int height) {
		AltoStringImpl string = new AltoStringImpl(textLine, text, left, top, width, height);
		string.setAltoService(this);
		return string;
	}
	public AltoTextLine newTextLine(AltoTextBlock textBlock, int left, int top, int width, int height) {
		AltoTextLineImpl textLine = new AltoTextLineImpl(textBlock, left, top, width, height);
		return textLine;
	}
	public AltoTextBlock newTextBlock(AltoPage page, int left, int top, int width, int height) {
		AltoTextBlockImpl textBlock = new AltoTextBlockImpl(page, left, top, width, height);
		return textBlock;
	}
	public AltoPage newPage(AltoDocument doc, int pageIndex, int width, int height) {
		AltoPageImpl page = new AltoPageImpl(doc, pageIndex, width, height);
		return page;
	}
	public AltoDocument newDocument(String name) {
		AltoDocumentImpl doc = new AltoDocumentImpl(name);
		return doc;
	}
	@Override
	public AltoReader getAltoReader() {
		AltoReaderImpl reader = new AltoReaderImpl();
		reader.setAltoService(this);
		return reader;
	}
	@Override
	public AltoReader getAltoReader(AltoDocument doc) {
		AltoReaderImpl reader = new AltoReaderImpl(doc);
		reader.setAltoService(this);
		return reader;
	}

	public AltoStringFixer getAltoStringFixer() {
		AltoStringFixer altoStringFixer = null;
		Locale locale = searchService.getLocale();
		if (locale.getLanguage().equals("yi")||
				locale.getLanguage().equals("ji")) {
			altoStringFixer = new YiddishAltoStringFixer();
		}
		return altoStringFixer;
	}
	public SearchService getSearchService() {
		return searchService;
	}
	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}
	
	
}
