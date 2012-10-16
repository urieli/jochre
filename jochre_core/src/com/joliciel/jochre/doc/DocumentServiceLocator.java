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
package com.joliciel.jochre.doc;

import javax.sql.DataSource;

import com.joliciel.jochre.JochreServiceLocator;

public class DocumentServiceLocator {
	DocumentServiceImpl documentService = null;
	DocumentDaoJdbc documentDao = null;
	private DataSource dataSource;
	private JochreServiceLocator jochreServiceLocator;
	
	public DocumentServiceLocator(JochreServiceLocator jochreServiceLocator, DataSource dataSource) {
		this.jochreServiceLocator = jochreServiceLocator;
		this.dataSource = dataSource;
	}
	
	public DocumentService getDocumentService() {
		if (documentService==null) {
			documentService = new DocumentServiceImpl();
			documentService.setDocumentDao(this.getDocumentDao());
			documentService.setObjectCache(this.jochreServiceLocator.getObjectCache());
			documentService.setBoundaryFeatureService(this.jochreServiceLocator.getBoundaryFeatureServiceLocator().getBoundaryFeatureService());
			documentService.setGraphicsService(this.jochreServiceLocator.getGraphicsServiceLocator().getGraphicsService());
			documentService.setLetterFeatureService(this.jochreServiceLocator.getLetterFeatureServiceLocator().getLetterFeatureService());
			documentService.setLetterGuesserService(this.jochreServiceLocator.getLetterGuesserServiceLocator().getLetterGuesserService());
			documentService.setSecurityService(this.jochreServiceLocator.getSecurityServiceLocator().getSecurityService());
			documentService.setAnalyserService(this.jochreServiceLocator.getAnalyserServiceLocator().getAnalyserService());
			documentService.setBoundaryService(this.jochreServiceLocator.getBoundaryServiceLocator().getBoundaryService());
			documentService.setMachineLearningService(this.jochreServiceLocator.getMachineLearningServiceLocator().getMachineLearningService());
		}
		return documentService;
	}
	
	DocumentDao getDocumentDao() {
		if (this.documentDao == null) {
			this.documentDao = new DocumentDaoJdbc();
			this.documentDao.setDataSource(dataSource);
		}
		return documentDao;
	}

	public JochreServiceLocator getJochreServiceLocator() {
		return jochreServiceLocator;
	}
	
	
}
