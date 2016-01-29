package com.joliciel.jochre.graphics;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

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
	private Map<String,Set<Integer>> documentSelections = null;
	
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
		if (documentSelections!=null)
			attributes.put("documentSelections", documentSelections.toString());
		
		return attributes;
	}
	
	public void loadSelection(Scanner scanner) {
		documentSelections = new HashMap<String, Set<Integer>>();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine().trim();
			if (line.length()>0 && !line.startsWith("#")) {
				int tabPos = line.indexOf('\t');
				String docName = line;
				String selections = null;
				if (tabPos>0) {
					docName = line.substring(0, tabPos);
					if (tabPos+1<line.length())
						selections = line.substring(tabPos+1, line.length());
				}
				Set<Integer> pages = new TreeSet<>();
				if (selections!=null) {
					String[] parts = selections.split("[\\;\\,]");
					for (String part : parts) {
						if (part.indexOf('-')>=0) {
							int first = Integer.parseInt(part.substring(0, part.indexOf('-')));
							int last = Integer.parseInt(part.substring(part.indexOf('-')+1));
							for (int i=first; i<=last; i++) {
								pages.add(i);
							}
						} else {
							int page = Integer.parseInt(part);
							pages.add(page);
						}
					}
				} // have selections?
				documentSelections.put(docName, pages);
			} // valid line?
		} // have next line?
	}
	
	public Map<String, Set<Integer>> getDocumentSelections() {
		return documentSelections;
	}
	
	
}