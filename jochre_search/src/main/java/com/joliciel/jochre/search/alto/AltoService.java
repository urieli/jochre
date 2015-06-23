package com.joliciel.jochre.search.alto;

public interface AltoService {
	public AltoDocument newDocument(String name);
	public AltoReader getAltoReader();
	public AltoReader getAltoReader(AltoDocument doc);
}
