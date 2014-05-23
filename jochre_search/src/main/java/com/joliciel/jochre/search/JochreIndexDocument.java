package com.joliciel.jochre.search;

import java.io.File;

public interface JochreIndexDocument {
	public File getDirectory();
	public String getContents();

	public CoordinateStorage getCoordinateStorage();

}