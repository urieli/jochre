package com.joliciel.jochre.search.alto;

import java.util.Locale;

public interface AltoService {
	public AltoDocument newDocument(String name);
	public AltoReader getAltoReader();
	public AltoReader getAltoReader(AltoDocument doc);
	
	public AltoStringFixer getAltoStringFixer(Locale locale);
}
