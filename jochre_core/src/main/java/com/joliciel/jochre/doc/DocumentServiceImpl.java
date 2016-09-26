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
import java.util.List;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.analyser.AnalyserService;
import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.boundaries.features.BoundaryFeatureService;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.jochre.letterGuesser.features.LetterFeatureService;

final class DocumentServiceImpl implements DocumentServiceInternal {

	private AnalyserService analyserService;
	private LetterGuesserService letterGuesserService;
	private LetterFeatureService letterFeatureService;
	private BoundaryFeatureService boundaryFeatureService;
	private BoundaryService boundaryService;

	@SuppressWarnings("unused")
	private final JochreSession jochreSession;
	private final DocumentDao documentDao;

	public DocumentServiceImpl(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
		this.documentDao = DocumentDao.getInstance(jochreSession);
	}

	@Override
	public JochreDocument getEmptyJochreDocument(JochreSession jochreSession) {
		return this.getEmptyJochreDocumentInternal(jochreSession);
	}

	@Override
	public JochrePage getEmptyJochrePageInternal(JochreSession jochreSession) {
		JochrePage page = new JochrePage(jochreSession);
		page.setDocumentService(this);
		return page;
	}

	@Override
	public JochreDocument getEmptyJochreDocumentInternal(JochreSession jochreSession) {
		JochreDocument doc = new JochreDocument(jochreSession);
		doc.setDocumentServiceInternal(this);
		return doc;
	}

	@Override
	public JochreDocument loadJochreDocument(int documentId) {
		return documentDao.loadJochreDocument(documentId);
	}

	@Override
	public JochreDocument loadJochreDocument(String name) {
		return documentDao.loadJochreDocument(name);
	}

	@Override
	public void saveJochrePage(JochrePage page) {
		this.documentDao.saveJochrePage(page);
	}

	@Override
	public JochrePage loadJochrePage(int pageId) {
		return documentDao.loadJochrePage(pageId);
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
		return this.documentDao.findDocuments();
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
	public Author getEmptyAuthorInternal() {
		Author author = new Author();
		author.setDocumentServiceInternal(this);
		return author;
	}

	@Override
	public Author loadAuthor(int authorId) {
		return this.documentDao.loadAuthor(authorId);
	}

	@Override
	public void saveAuthor(Author author) {
		this.documentDao.saveAuthor(author);
	}

	@Override
	public void replaceAuthors(JochreDocument doc) {
		this.documentDao.replaceAuthors(doc);
	}

	@Override
	public void deleteJochrePage(JochrePage page) {
		this.documentDao.deleteJochrePage(page);
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
	public JochreDocumentGenerator getJochreDocumentGenerator(JochreDocument jochreDocument, JochreSession jochreSession) {
		JochreDocumentGenerator generator = new JochreDocumentGenerator(jochreDocument, jochreSession);
		generator.setAnalyserService(this.getAnalyserService());
		generator.setBoundaryFeatureService(this.getBoundaryFeatureService());
		generator.setBoundaryService(this.getBoundaryService());
		generator.setDocumentService(this);
		generator.setLetterFeatureService(this.getLetterFeatureService());
		generator.setLetterGuesserService(this.getLetterGuesserService());
		return generator;
	}

	@Override
	public JochreDocumentGenerator getJochreDocumentGenerator(String filename, String userFriendlyName, JochreSession jochreSession) {
		JochreDocumentGenerator generator = new JochreDocumentGenerator(filename, userFriendlyName, jochreSession);
		generator.setAnalyserService(this.getAnalyserService());
		generator.setBoundaryFeatureService(this.getBoundaryFeatureService());
		generator.setBoundaryService(this.getBoundaryService());
		generator.setDocumentService(this);
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
