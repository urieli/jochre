package com.joliciel.jochre.boundaries;

import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.Shape;

class TrainingCorpusShapeMerger implements ShapeMerger {
	GraphicsService graphicsService;
	
	@Override
	public double checkMerge(Shape shape1, Shape shape2) {
		if (shape1.getLetter().startsWith("|")&&shape2.getLetter().endsWith("|"))
			return 1;
		if (shape1.getLetter().startsWith("|")&&shape2.getLetter().equals(""))
			return 1;
		if (shape1.getLetter().equals("")&&shape2.getLetter().endsWith("|"))
			return 1;
		
		return 0;
	}

	@Override
	public Shape merge(Shape shape1, Shape shape2) {
		int left = shape1.getLeft() < shape2.getLeft() ? shape1.getLeft() : shape2.getLeft();
		int top = shape1.getTop() < shape2.getTop() ? shape1.getTop() : shape2.getTop();
		int right = shape1.getRight() > shape2.getRight() ? shape1.getRight() : shape2.getRight();
		int bottom = shape1.getBottom() > shape2.getBottom() ? shape1.getBottom() : shape2.getBottom();
		
		Shape mergedShape = shape1.getJochreImage().getShape(left, top, right, bottom);
		
		String letter = "";
		if (shape1.getLetter().length()==0)
			letter = shape2.getLetter();
		else if (shape2.getLetter().length()==0)
			letter = shape1.getLetter();
		else
			letter = shape1.getLetter().substring(shape1.getLetter().indexOf("|")+1);
		mergedShape.setLetter(letter);
		return mergedShape;
	}

	public GraphicsService getGraphicsService() {
		return graphicsService;
	}

	public void setGraphicsService(GraphicsService graphicsService) {
		this.graphicsService = graphicsService;
	}

}
