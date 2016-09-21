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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.joliciel.jochre.EntityNotFoundException;
import com.joliciel.jochre.analyser.AnalyserService;
import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.boundaries.features.BoundaryFeatureService;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.jochre.letterGuesser.features.LetterFeatureService;
import com.joliciel.jochre.security.SecurityService;
import com.joliciel.talismane.utils.ObjectCache;

final class DocumentServiceImpl implements DocumentServiceInternal {
	private DocumentDao documentDao;
	private ObjectCache objectCache;

	private AnalyserService analyserService;
	private GraphicsService graphicsService;
	private SecurityService securityService;
	private LetterGuesserService letterGuesserService;
	private LetterFeatureService letterFeatureService;
	private BoundaryFeatureService boundaryFeatureService;
	private BoundaryService boundaryService;

	public DocumentDao getDocumentDao() {
		return documentDao;
	}

	public void setDocumentDao(DocumentDao documentDao) {
		this.documentDao = documentDao;
		documentDao.setDocumentServiceInternal(this);
	}

	public ObjectCache getObjectCache() {
		return objectCache;
	}

	public void setObjectCache(ObjectCache objectCache) {
		this.objectCache = objectCache;
	}

	@Override
	public JochreDocument getEmptyJochreDocument() {
		return this.getEmptyJochreDocumentInternal();
	}

	@Override
	public JochrePageInternal getEmptyJochrePageInternal() {
		JochrePageImpl page = new JochrePageImpl();
		page.setDocumentService(this);
		page.setGraphicsService(this.getGraphicsService());
		return page;
	}

	@Override
	public JochreDocumentInternal getEmptyJochreDocumentInternal() {
		JochreDocumentImpl doc = new JochreDocumentImpl();
		doc.setDocumentServiceInternal(this);
		doc.setSecurityService(this.getSecurityService());
		doc.setGraphicsService(this.getGraphicsService());
		return doc;
	}

	@Override
	public JochreDocument loadJochreDocument(int documentId) {
		JochreDocument document = this.objectCache.getEntity(JochreDocument.class, documentId);
		if (document == null) {
			document = this.getDocumentDao().loadJochreDocument(documentId);
			if (document == null) {
				throw new EntityNotFoundException("No JochreDocument found for documentId " + documentId);
			}
			this.objectCache.putEntity(JochreDocument.class, documentId, document);
		}
		return document;
	}

	@Override
	public JochreDocument loadJochreDocument(String name) {
		JochreDocument document = this.getDocumentDao().loadJochreDocument(name);
		if (document == null) {
			throw new EntityNotFoundException("No JochreDocument found for name " + name);
		}
		return document;
	}

	@Override
	public void saveJochrePage(JochrePage page) {
		this.documentDao.saveJochrePage(page);
	}

	@Override
	public JochrePage loadJochrePage(int pageId) {
		JochrePage page = this.objectCache.getEntity(JochrePage.class, pageId);
		if (page == null) {
			page = this.getDocumentDao().loadJochrePage(pageId);
			if (page == null) {
				throw new EntityNotFoundException("No JochrePage found for pageId " + pageId);
			}
			this.objectCache.putEntity(JochrePage.class, pageId, page);
		}
		return page;
	}

	@Override
	public List<JochrePage> findPages(JochreDocument document) {
		return this.documentDao.findJochrePages(document);
	}

	@Override
	public void saveJochreDocument(JochreDocument jochreDocument) {
		this.documentDao.saveJochreDocument(jochreDocument);
	}

	@Override
	public List<JochreDocument> findDocuments() {
		List<JochreDocument> documents = this.documentDao.findDocuments();
		List<JochreDocument> docsFinal = new ArrayList<JochreDocument>();
		for (JochreDocument doc : documents) {
			// JochreDocument otherDoc = (JochreDocument)
			// this.objectCache.getEntity(JochreDocument.class, doc.getId());
			// if (otherDoc==null) {
			// this.objectCache.putEntity(JochreDocument.class, doc.getId(), doc);
			// docsFinal.add(doc);
			// } else {
			// docsFinal.add(otherDoc);
			// }
			this.objectCache.putEntity(JochreDocument.class, doc.getId(), doc);
			docsFinal.add(doc);
		}
		return docsFinal;
	}

	@Override
	public ImageDocumentExtractor getImageDocumentExtractor(File imageFile, SourceFileProcessor documentProcessor) {
		ImageDocumentExtractor extractor = new ImageDocumentExtractorImpl(imageFile, documentProcessor);
		return extractor;
	}

	@Override
	public List<Author> findAuthors() {
		return this.documentDao.findAuthors();
	}

	@Override
	public List<? extends Author> findAuthors(JochreDocument doc) {
		return this.documentDao.findAuthors(doc);
	}

	@Override
	public Author getEmptyAuthor() {
		return this.getEmptyAuthorInternal();
	}

	@Override
	public AuthorInternal getEmptyAuthorInternal() {
		AuthorImpl author = new AuthorImpl();
		author.setDocumentServiceInternal(this);
		return author;
	}

	@Override
	public Author loadAuthor(int authorId) {
		Author author = this.objectCache.getEntity(Author.class, authorId);
		if (author == null) {
			author = this.getDocumentDao().loadAuthor(authorId);
			if (author == null) {
				throw new EntityNotFoundException("No Author found for authorId " + authorId);
			}
			this.objectCache.putEntity(Author.class, authorId, author);
		}
		return author;
	}

	@Override
	public void saveAuthor(Author author) {
		this.getDocumentDao().saveAuthor(author);
	}

	@Override
	public void replaceAuthors(JochreDocument doc) {
		this.getDocumentDao().replaceAuthors(doc);
	}

	@Override
	public void deleteJochrePage(JochrePage page) {
		this.getDocumentDao().deleteJochrePage(page);
	}

	public GraphicsService getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}

	public SecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	public LetterGuesserService getLetterGuesserService() {
		return letterGuesserService;
	}

	public void setLetterGuesserService(LetterGuesserService letterGuesserService) {
		this.letterGuesserService = letterGuesserService;
	}

	public LetterFeatureService getLetterFeatureService() {
		return letterFeatureService;
	}

	public void setLetterFeatureService(LetterFeatureService letterFeatureService) {
		this.letterFeatureService = letterFeatureService;
	}

	public BoundaryFeatureService getBoundaryFeatureService() {
		return boundaryFeatureService;
	}

	public void setBoundaryFeatureService(BoundaryFeatureService boundaryFeatureService) {
		this.boundaryFeatureService = boundaryFeatureService;
	}

	@Override
	public JochreDocumentGenerator getJochreDocumentGenerator(JochreDocument jochreDocument) {
		JochreDocumentGeneratorImpl generator = new JochreDocumentGeneratorImpl(jochreDocument);
		generator.setAnalyserService(this.getAnalyserService());
		generator.setBoundaryFeatureService(this.getBoundaryFeatureService());
		generator.setBoundaryService(this.getBoundaryService());
		generator.setDocumentService(this);
		generator.setGraphicsService(this.getGraphicsService());
		generator.setLetterFeatureService(this.getLetterFeatureService());
		generator.setLetterGuesserService(this.getLetterGuesserService());
		return generator;
	}

	@Override
	public JochreDocumentGenerator getJochreDocumentGenerator(String filename, String userFriendlyName, Locale locale) {
		JochreDocumentGeneratorImpl generator = new JochreDocumentGeneratorImpl(filename, userFriendlyName, locale);
		generator.setAnalyserService(this.getAnalyserService());
		generator.setBoundaryFeatureService(this.getBoundaryFeatureService());
		generator.setBoundaryService(this.getBoundaryService());
		generator.setDocumentService(this);
		generator.setGraphicsService(this.getGraphicsService());
		generator.setLetterFeatureService(this.getLetterFeatureService());
		generator.setLetterGuesserService(this.getLetterGuesserService());
		return generator;
	}

	public AnalyserService getAnalyserService() {
		return analyserService;
	}

	public void setAnalyserService(AnalyserService analyserService) {
		this.analyserService = analyserService;
	}

	public BoundaryService getBoundaryService() {
		return boundaryService;
	}

	public void setBoundaryService(BoundaryService boundaryService) {
		this.boundaryService = boundaryService;
	}

}
