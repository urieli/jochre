package com.joliciel.jochre.search.alto;

interface AltoServiceInternal extends AltoService {
	public AltoString newString(AltoTextLine textLine, String content, int left, int top, int width, int height);
	public AltoTextLine newTextLine(AltoTextBlock textBlock, int left, int top, int width, int height);
	public AltoTextBlock newTextBlock(AltoPage page, int left, int top, int width, int height);
	public AltoPage newPage(AltoDocument doc, int pageIndex, int width, int height);
}
