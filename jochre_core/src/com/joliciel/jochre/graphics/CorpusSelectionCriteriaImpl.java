package com.joliciel.jochre.graphics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class CorpusSelectionCriteriaImpl implements CorpusSelectionCriteria {
	private ImageStatus[] imageStatusesToInclude = new ImageStatus[] { ImageStatus.TRAINING_HELD_OUT };
	private int imageCount = 0;
	private int imageId = 0;
	
	private int includeIndex = -1;
	private int excludeIndex = -1;
	private int crossValidationSize = -1;
	private int excludeImageId = 0;
	private int documentId = 0;
	private Set<Integer> documentIds = null;
	
	public ImageStatus[] getImageStatusesToInclude() {
		return imageStatusesToInclude;
	}
	public void setImageStatusesToInclude(ImageStatus[] imageStatusesToInclude) {
		this.imageStatusesToInclude = imageStatusesToInclude;
	}
	public int getImageCount() {
		return imageCount;
	}
	public void setImageCount(int imageCount) {
		this.imageCount = imageCount;
	}
	public int getImageId() {
		return imageId;
	}
	public void setImageId(int imageId) {
		this.imageId = imageId;
	}
	public int getIncludeIndex() {
		return includeIndex;
	}
	public void setIncludeIndex(int includeIndex) {
		this.includeIndex = includeIndex;
	}
	public int getExcludeIndex() {
		return excludeIndex;
	}
	public void setExcludeIndex(int excludeIndex) {
		this.excludeIndex = excludeIndex;
	}
	public int getCrossValidationSize() {
		return crossValidationSize;
	}
	public void setCrossValidationSize(int crossValidationSize) {
		this.crossValidationSize = crossValidationSize;
	}
	public int getExcludeImageId() {
		return excludeImageId;
	}
	public void setExcludeImageId(int excludeImageId) {
		this.excludeImageId = excludeImageId;
	}
	
	public int getDocumentId() {
		return documentId;
	}
	public void setDocumentId(int documentId) {
		this.documentId = documentId;
	}
	
	
	@Override
	public void setDocumentIds(Set<Integer> documentIds) {
		this.documentIds = documentIds;
	}
	@Override
	public Set<Integer> getDocumentIds() {
		return this.documentIds;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String,String> attributes = new LinkedHashMap<String, String>();
		attributes.put("imageCount", "" + imageCount);
		attributes.put("imageStatusesToInclude", imageStatusesToInclude.toString());
		attributes.put("imageId", "" + imageId);
		attributes.put("excludeImageId", "" + excludeImageId);
		attributes.put("crossValidationSize", "" + crossValidationSize);
		attributes.put("includeIndex", "" + includeIndex);
		attributes.put("excludeIndex", "" + excludeIndex);
		attributes.put("documentId", "" + documentId);
		if (documentIds!=null)
			attributes.put("documentIds", documentIds.toString());
		
		return attributes;
	}
}
