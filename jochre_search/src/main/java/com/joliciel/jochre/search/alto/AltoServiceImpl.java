package com.joliciel.jochre.search.alto;

public class AltoServiceImpl implements AltoServiceInternal {

	public AltoString newString(AltoTextLine textLine, String text, int left, int top, int width, int height) {
		AltoString string = new AltoStringImpl(textLine, text, left, top, width, height);
		return string;
	}
	public AltoTextLine nextTextLine(AltoTextBlock textBlock, int left, int top, int width, int height) {
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

}
