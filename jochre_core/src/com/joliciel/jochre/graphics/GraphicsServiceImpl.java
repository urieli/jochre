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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.joliciel.jochre.EntityNotFoundException;
import com.joliciel.jochre.boundaries.BoundaryService;
import com.joliciel.jochre.doc.DocumentService;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.util.ImagePixelGrabber;
import com.joliciel.jochre.graphics.util.ImagePixelGrabberImpl;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.jochre.security.SecurityService;
import com.joliciel.talismane.utils.ObjectCache;

final class GraphicsServiceImpl implements GraphicsServiceInternal {
	private GraphicsDao graphicsDao;
	private ObjectCache objectCache;
	private LetterGuesserService letterGuesserService;
	private SecurityService securityService;
	private DocumentService documentService;
	private BoundaryService boundaryService;
	
	public GraphicsServiceImpl() {
	}
	
	public Segmenter getSegmenter(SourceImage sourceImage) {
		SegmenterImpl shapeExtractor = new SegmenterImpl(sourceImage);
		shapeExtractor.setGraphicsService(this);
		return shapeExtractor;
	}
	
	@Override
	public Vectorizer getVectorizer() {
		VectorizerImpl vectorizer = new VectorizerImpl();
		vectorizer.setGraphicsService(this);
		return vectorizer;
	}

	@Override
	public Shape getDot(JochreImage sourceImage, int x, int y) {
		ShapeImpl shape = new ShapeImpl(sourceImage);
		shape.setGraphicsService(this);
		shape.setLetterGuesserService(this.getLetterGuesserService());
		shape.setLeft(x);
		shape.setRight(x);
		shape.setTop(y);
		shape.setBottom(y);
		
		shape.setStartingPoint(new int[] {x,y});
		
		return shape;
	}

	@Override
	public WritableImageGrid getEmptyMirror(ImageGrid imageGrid) {
		return new ImageMirror(imageGrid);
	}

	@Override
	public SourceImage getSourceImage(JochrePage page, String name,
			BufferedImage image) {
		SourceImageInternal sourceImage = this.getSourceImageInternal(name, image);
		sourceImage.setPage(page);
		return sourceImage;
	}

	@Override
	public SourceImageInternal getSourceImageInternal(String name, BufferedImage image) {
		SourceImageImpl sourceImage =  new SourceImageImpl(this, name, image);
		sourceImage.setGraphicsService(this);
		sourceImage.setDocumentService(this.getDocumentService());
		sourceImage.setSecurityService(this.getSecurityService());
		return sourceImage;
	}

	public RowOfShapes getEmptyRow(SourceImage sourceImage) {
		RowOfShapesImpl row = new RowOfShapesImpl(sourceImage);
		row.setGraphicsService(this);
		return row;
	}
	
	public JochreImage getEmptyJochreImage() {
		return this.getEmptyJochreImageInternal();
	}
	
	

	@Override
	public ShapeInternal getEmptyShapeInternal() {
		ShapeImpl shape = new ShapeImpl();
		shape.setGraphicsService(this);
		shape.setLetterGuesserService(this.getLetterGuesserService());
		shape.setBoundaryService(this.getBoundaryService());
		
		return shape;
	}

	@Override
	public JochreImageInternal getEmptyJochreImageInternal() {
		JochreImageImpl jochreImage = new JochreImageImpl();
		jochreImage.setGraphicsService(this);
		jochreImage.setDocumentService(this.getDocumentService());
		jochreImage.setSecurityService(this.getSecurityService());
		return jochreImage;
	}

	@Override
	public RowOfShapesInternal getEmptyRowOfShapesInternal() {
		RowOfShapesImpl rowOfShapes = new RowOfShapesImpl();
		rowOfShapes.setGraphicsService(this);
		return rowOfShapes;
	}
	
	@Override
	public GroupOfShapesInternal getEmptyGroupOfShapesInternal() {
		GroupOfShapesImpl groupOfShapes = new GroupOfShapesImpl();
		groupOfShapes.setGraphicsService(this);
		return groupOfShapes;
	}

	@Override
	public Shape loadShape(int shapeId) {
		Shape shape = (Shape) this.objectCache.getEntity(Shape.class, shapeId);
        if (shape==null) {
        	shape = this.getGraphicsDao().loadShape(shapeId);
            if (shape==null) {
                throw new EntityNotFoundException("No ContiguousShape found for shapeId " + shapeId);
            }
            this.objectCache.putEntity(Shape.class, shapeId, shape);
        }
        return shape;
	}

	@Override
	public JochreImage loadJochreImage(int imageId) {
		JochreImage image = (JochreImage) this.objectCache.getEntity(JochreImage.class, imageId);
        if (image==null) {
        	image = this.getGraphicsDao().loadJochreImage(imageId);
            if (image==null) {
                throw new EntityNotFoundException("No JochreImage found for imageId " + imageId);
            }
            this.objectCache.putEntity(JochreImage.class, imageId, image);
        }
        return image;
	}

	@Override
	public RowOfShapes loadRowOfShapes(int rowId) {
		RowOfShapes row = (RowOfShapes) this.objectCache.getEntity(RowOfShapes.class, rowId);
        if (row==null) {
        	row = this.getGraphicsDao().loadRowOfShapes(rowId);
            if (row==null) {
                throw new EntityNotFoundException("No RowOfShapes found for rowId " + rowId);
            }
            this.objectCache.putEntity(RowOfShapes.class, rowId, row);
        }
        return row;
	}
	
	@Override
	public GroupOfShapes loadGroupOfShapes(int groupId) {
		GroupOfShapes group = (GroupOfShapes) this.objectCache.getEntity(GroupOfShapes.class, groupId);
        if (group==null) {
        	group = this.getGraphicsDao().loadGroupOfShapes(groupId);
            if (group==null) {
                throw new EntityNotFoundException("No GroupOfShapes found for groupId " + groupId);
            }
            this.objectCache.putEntity(GroupOfShapes.class, groupId, group);
        }
        return group;
	}

	@Override
	public void saveShape(Shape shape) {
		this.graphicsDao.saveShape(shape);
	}

	@Override
	public void saveJochreImage(JochreImage image) {
		this.graphicsDao.saveJochreImage(image);
	}

	@Override
	public void saveRowOfShapes(RowOfShapes row) {
		this.graphicsDao.saveRowOfShapes(row);
	}

	@Override
	public void saveGroupOfShapes(GroupOfShapes group) {
		this.graphicsDao.saveGroupOfShapes(group);
	}
	
	public GraphicsDao getGraphicsDao() {
		return graphicsDao;
	}

	public void setGraphicsDao(GraphicsDao graphicsDao) {
		this.graphicsDao = graphicsDao;
		graphicsDao.setGraphicsServiceInternal(this);
	}

	public ObjectCache getObjectCache() {
		return objectCache;
	}

	public void setObjectCache(ObjectCache objectCache) {
		this.objectCache = objectCache;
	}

	@Override
	public void deleteShapeInternal(
			ShapeInternal contiguousShapeInternal) {
		this.graphicsDao.deleteContiguousShapeInternal(contiguousShapeInternal);
	}

	@Override
	public List<RowOfShapes> findRows(Paragraph paragraph) {
		return this.graphicsDao.findRows(paragraph);
	}

	@Override
	public List<GroupOfShapes> findGroups(RowOfShapes row) {
		return this.graphicsDao.findGroups(row);
	}

	@Override
	public List<Shape> findShapes(GroupOfShapes group) {
		return this.graphicsDao.findShapes(group);
	}

	@Override
	public void saveParagraph(Paragraph paragraph) {
		this.graphicsDao.saveParagraph(paragraph);
	}

	@Override
	public Paragraph loadParagraph(int paragraphId) {
		Paragraph paragraph = (Paragraph) this.objectCache.getEntity(Paragraph.class, paragraphId);
        if (paragraph==null) {
        	paragraph = this.getGraphicsDao().loadParagraph(paragraphId);
            if (paragraph==null) {
                throw new EntityNotFoundException("No Paragraph found for paragraphId " + paragraphId);
            }
            this.objectCache.putEntity(Paragraph.class, paragraphId, paragraph);
        }
        return paragraph;
	}

	@Override
	public List<Paragraph> findParagraphs(JochreImage jochreImage) {
		return this.graphicsDao.findParagraphs(jochreImage);
	}

	@Override
	public ParagraphInternal getEmptyParagraphInternal() {
		ParagraphImpl paragraph = new ParagraphImpl();
		paragraph.setGraphicsService(this);
		return paragraph;
	}

	@Override
	public List<JochreImage> findImages(JochrePage page) {
		List<JochreImage> images = this.getGraphicsDao().findImages(page);
		return this.findCachedImages(images);
	}

	@Override
	public List<JochreImage> findImages(ImageStatus[] imageStatuses) {
		List<JochreImage> images = this.getGraphicsDao().findImages(imageStatuses);
		return this.findCachedImages(images);
	}

	List<JochreImage> findCachedImages(List<JochreImage> images) {
		List<JochreImage> cachedImages = new ArrayList<JochreImage>();
		for (JochreImage image : images) {
			JochreImage cachedImage = (JochreImage) this.objectCache.getEntity(JochreImage.class, image.getId());
	        if (cachedImage==null) {
	            this.objectCache.putEntity(JochreImage.class, image.getId(), image);
	            cachedImages.add(image);
	        } else {
	        	cachedImages.add(cachedImage);
	        }
		}
		return cachedImages;		
	}
	
	@Override
	public LineSegment getEmptyLineSegment(Shape shape, LineDefinition lineDefinition, int startX, int startY, int endX, int endY) {
		return new LineSegmentImpl(shape, lineDefinition, startX, startY, endX, endY);
	}

	@Override
	public LineDefinition getEmptyLineDefinition(int sector, int index) {
		return new LineDefinitionImpl(sector, index);
	}

	@Override
	public ImagePixelGrabber getPixelGrabber(Image image) {
		return new ImagePixelGrabberImpl(image);
	}

	@Override
	public ShapeFiller getShapeFiller() {
		return new ShapeFillerImpl();
	}

	@Override
	public List<Integer> findShapeIds(String letter) {
		return this.getGraphicsDao().findShapeIds(letter);
	}

	public LetterGuesserService getLetterGuesserService() {
		return this.letterGuesserService;
	}
	
	public void setLetterGuesserService(LetterGuesserService letterGuesserService) {
		this.letterGuesserService = letterGuesserService;
	}
	
	public SecurityService getSecurityService() {
		return this.securityService;
	}

	public DocumentService getDocumentService() {
		return this.documentService;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	public void setDocumentService(DocumentService documentService) {
		this.documentService = documentService;
	}

	@Override
	public List<Shape> findShapes(RowOfShapes row) {
		return this.getGraphicsDao().findShapes(row);
	}

	@Override
	public void saveOriginalImage(JochreImage jochreImage) {
		this.getGraphicsDao().saveOriginalImage(jochreImage);
	}

	@Override
	public void loadOriginalImage(JochreImageInternal jochreImage) {
		this.getGraphicsDao().loadOriginalImage(jochreImage);
	}

	@Override
	public int getShapeCount(JochreImage jochreImage) {
		return this.getGraphicsDao().getShapeCount(jochreImage);
	}

	@Override
	public void deleteJochreImage(JochreImage image) {
		this.getGraphicsDao().deleteJochreImage(image);
	}

	@Override
	public List<Shape> findShapesToSplit(Locale locale) {
		return this.getGraphicsDao().findShapesToSplit(locale);
	}

	public BoundaryService getBoundaryService() {
		return boundaryService;
	}

	public void setBoundaryService(BoundaryService boundaryService) {
		this.boundaryService = boundaryService;
	}

	@Override
	public JochreCorpusShapeReader getJochreCorpusShapeReader() {
		JochreCorpusShapeReaderImpl jochreCorpusReader = new JochreCorpusShapeReaderImpl();
		jochreCorpusReader.setGraphicsService(this);
		jochreCorpusReader.setDocumentService(this.getDocumentService());
		return jochreCorpusReader;
	}

	@Override
	public JochreCorpusGroupReader getJochreCorpusGroupReader() {
		JochreCorpusGroupReaderImpl jochreCorpusReader = new JochreCorpusGroupReaderImpl();
		jochreCorpusReader.setGraphicsService(this);
		jochreCorpusReader.setDocumentService(this.getDocumentService());
		return jochreCorpusReader;
	}

	@Override
	public List<GroupOfShapes> findGroupsForMerge() {
		return this.getGraphicsDao().findGroupsForMerge();
	}

	@Override
	public JochreCorpusImageReader getJochreCorpusImageReader() {
		JochreCorpusImageReaderImpl jochreCorpusReader = new JochreCorpusImageReaderImpl();
		jochreCorpusReader.setGraphicsService(this);
		jochreCorpusReader.setDocumentService(this.getDocumentService());
		return jochreCorpusReader;
	}

	@Override
	public CorpusSelectionCriteria getCorpusSelectionCriteria() {
		CorpusSelectionCriteria criteria = new CorpusSelectionCriteriaImpl();
		return criteria;
	}

	@Override
	public JochreCorpusImageProcessor getJochreCorpusImageProcessor(
			CorpusSelectionCriteria corpusSelectionCriteria) {
		JochreCorpusImageProcessorImpl processor = new JochreCorpusImageProcessorImpl(corpusSelectionCriteria);
		processor.setGraphicsService(this);
		processor.setDocumentService(this.getDocumentService());
		return processor;
	}

	
}
