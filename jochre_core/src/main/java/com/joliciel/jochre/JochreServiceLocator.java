///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.jochre;

import com.joliciel.jochre.doc.DocumentServiceLocator;
import com.joliciel.talismane.utils.ObjectCache;
import com.joliciel.talismane.utils.SimpleObjectCache;

/**
 * Top-level locator for implementations of Jochre interfaces.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreServiceLocator {

	private DocumentServiceLocator documentServiceLocator;

	private String dataSourcePropertiesFile;
	private ObjectCache objectCache;
	private static JochreServiceLocator instance = null;

	private final JochreSession jochreSession;

	private JochreServiceLocator(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
	}

	public static JochreServiceLocator getInstance(JochreSession jochreSession) {
		if (instance == null) {
			instance = new JochreServiceLocator(jochreSession);
		}
		return instance;
	}

	public DocumentServiceLocator getDocumentServiceLocator() {
		if (this.documentServiceLocator == null) {
			this.documentServiceLocator = new DocumentServiceLocator(this, jochreSession);
		}
		return documentServiceLocator;
	}

	public ObjectCache getObjectCache() {
		if (this.objectCache == null)
			this.objectCache = new SimpleObjectCache();
		return objectCache;
	}

	public void setObjectCache(ObjectCache objectCache) {
		this.objectCache = objectCache;
	}

	public String getDataSourcePropertiesFile() {
		return dataSourcePropertiesFile;
	}

	public void setDataSourcePropertiesFile(String dataSourcePropertiesFile) {
		this.dataSourcePropertiesFile = dataSourcePropertiesFile;
	}

	public JochreSession getJochreSession() {
		return jochreSession;
	}

}
