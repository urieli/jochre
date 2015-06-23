package com.joliciel.jochre.search.jochreXml;

public class JochreXmlServiceImpl implements JochreXmlService {
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
		reader.setJochreXmlService(this);
		return reader;
	}

}
