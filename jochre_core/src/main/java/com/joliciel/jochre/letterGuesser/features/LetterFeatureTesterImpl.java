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
package com.joliciel.jochre.letterGuesser.features;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.jochre.boundaries.ShapeInSequence;
import com.joliciel.jochre.boundaries.ShapeSequence;
import com.joliciel.jochre.graphics.GraphicsDao;
import com.joliciel.jochre.graphics.GroupOfShapes;
import com.joliciel.jochre.graphics.ImageStatus;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.graphics.Paragraph;
import com.joliciel.jochre.graphics.RowOfShapes;
import com.joliciel.jochre.graphics.Shape;
import com.joliciel.jochre.letterGuesser.LetterGuesserContext;
import com.joliciel.jochre.letterGuesser.LetterGuesserService;
import com.joliciel.jochre.letterGuesser.LetterSequence;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

class LetterFeatureTesterImpl implements LetterFeatureTester {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(LetterFeatureTesterImpl.class);
	private LetterGuesserService letterGuesserService;

	private final JochreSession jochreSession;

	public LetterFeatureTesterImpl(JochreSession jochreSession) {
		this.jochreSession = jochreSession;
	}

	@Override
	public void applyFeatures(Set<LetterFeature<?>> features, Set<String> letters, int minImageId, int minShapeId) {
		GraphicsDao graphicsDao = GraphicsDao.getInstance(jochreSession);
		List<JochreImage> images = graphicsDao.findImages(new ImageStatus[] { ImageStatus.TRAINING_VALIDATED });
		for (JochreImage image : images) {
			if (image.getId() >= minImageId) {
				this.testFeatures(image, features, letters, minShapeId);
			}
			image.clearMemory();
		}
	}

	void testFeatures(JochreImage jochreImage, Set<LetterFeature<?>> features, Set<String> letters, int minShapeId) {
		for (Paragraph paragraph : jochreImage.getParagraphs()) {
			for (RowOfShapes row : paragraph.getRows()) {
				for (GroupOfShapes group : row.getGroups()) {
					// simply add this group's shapes
					ShapeSequence shapeSequence = new ShapeSequence();
					for (Shape shape : group.getShapes())
						shapeSequence.addShape(shape);
					for (ShapeInSequence shapeInSequence : shapeSequence) {
						Shape shape = shapeInSequence.getShape();
						if (shape.getId() >= minShapeId && (letters == null || letters.size() == 0 || letters.contains(shape.getLetter())))
							this.testFeatures(shapeInSequence, features);
					} // next shape
				} // next group
			} // next row
		} // next paragraph
	}

	void testFeatures(ShapeInSequence shapeInSequence, Set<LetterFeature<?>> features) {
		LetterSequence history = null;
		LetterGuesserContext context = this.letterGuesserService.getContext(shapeInSequence, history);
		for (LetterFeature<?> feature : features) {
			RuntimeEnvironment env = new RuntimeEnvironment();
			feature.check(context, env);
		}
	}

	public LetterGuesserService getLetterGuesserService() {
		return letterGuesserService;
	}

	public void setLetterGuesserService(LetterGuesserService letterGuesserService) {
		this.letterGuesserService = letterGuesserService;
	}
}
