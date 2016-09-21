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
package com.joliciel.jochre.graphics;

import javax.sql.DataSource;

import com.joliciel.jochre.JochreServiceLocator;

public class GraphicsServiceLocator {
	GraphicsServiceImpl graphicsService = null;
	GraphicsDaoJdbc graphicsDao = null;
	private DataSource dataSource;
	private JochreServiceLocator jochreServiceLocator;

	public GraphicsServiceLocator(JochreServiceLocator jochreServiceLocator, DataSource dataSource) {
		this.jochreServiceLocator = jochreServiceLocator;
		this.dataSource = dataSource;
	}

	public GraphicsService getGraphicsService() {
		if (graphicsService == null) {
			graphicsService = new GraphicsServiceImpl();
			graphicsService.setGraphicsDao(this.getGraphicsDao());
			graphicsService.setObjectCache(this.jochreServiceLocator.getObjectCache());
			graphicsService.setBoundaryService(this.getJochreServiceLocator().getBoundaryServiceLocator().getBoundaryService());
			graphicsService.setDocumentService(this.getJochreServiceLocator().getDocumentServiceLocator().getDocumentService());
			graphicsService.setLetterGuesserService(this.getJochreServiceLocator().getLetterGuesserServiceLocator().getLetterGuesserService());
		}
		return graphicsService;
	}

	GraphicsDao getGraphicsDao() {
		if (this.graphicsDao == null) {
			this.graphicsDao = new GraphicsDaoJdbc();
			this.graphicsDao.setDataSource(dataSource);
		}
		return graphicsDao;
	}

	public JochreServiceLocator getJochreServiceLocator() {
		return jochreServiceLocator;
	}

}
