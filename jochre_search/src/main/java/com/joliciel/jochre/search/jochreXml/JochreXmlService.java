package com.joliciel.jochre.search.jochreXml;

public interface JochreXmlService {
	public JochreXmlLetter newLetter(JochreXmlWord word, String text, int left, int top, int right, int bottom);
	public JochreXmlWord newWord(JochreXmlRow row, String text, int left, int top, int right, int bottom);
	public JochreXmlRow newRow(JochreXmlParagraph paragraph, int left, int top, int right, int bottom);
	public JochreXmlParagraph newParagraph(JochreXmlImage page, int left, int top, int right, int bottom);
	public JochreXmlImage newImage(String fileNameBase, int pageIndex, int imageIndex, int width, int height);
	public JochreXmlDocument newDocument();
	public JochreXmlReader getJochreXmlReader(JochreXmlDocument doc);
}
