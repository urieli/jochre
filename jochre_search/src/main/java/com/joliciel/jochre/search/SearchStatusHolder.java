package com.joliciel.jochre.search;

/**
 * Stores the current search status.
 * @author Assaf Urieli
 *
 */
public final class SearchStatusHolder implements TaskStatusHolder {
	public static enum SearchStatus {
		WAITING,
		PREPARING,
		BUSY,
		COMMITING;
	}
	
	private SearchStatus status = SearchStatus.WAITING;
	private int successCount = 0;
	private int failureCount = 0;
	private int totalCount = 0;
	private long lastUpdated = System.currentTimeMillis();
	private long startTime = 0;
	private long endTime = 0;
	private String action = "";
	
	public long getLastUpdated() {
		return lastUpdated;
	}
	public void setStatus(SearchStatus status) {
		this.status = status;
		this.lastUpdated = System.currentTimeMillis();
		this.action = "";
		if (status==SearchStatus.PREPARING) {
			this.startTime = System.currentTimeMillis();
			this.endTime = 0;
		} else if (status==SearchStatus.WAITING) {
			this.endTime = System.currentTimeMillis();
		}
	}
	
	public synchronized void setSuccessCount(int successCount) {
		this.successCount = successCount;
		this.lastUpdated = System.currentTimeMillis();
	}
	public synchronized void setFailureCount(int failureCount) {
		this.failureCount = failureCount;
		this.lastUpdated = System.currentTimeMillis();
	}
	
	public synchronized void incrementSuccessCount(int increment) {
		this.successCount += increment;
		this.lastUpdated = System.currentTimeMillis();
	}
	public synchronized void incrementFailureCount(int increment) {
		this.failureCount += increment;
		this.lastUpdated = System.currentTimeMillis();
	}
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
		this.successCount = 0;
		this.failureCount = 0;
		lastUpdated = System.currentTimeMillis();
	}
	public SearchStatus getStatus() {
		return status;
	}
	public int getProcessedCount() {
		return successCount + failureCount;
	}
	public int getTotalCount() {
		return totalCount;
	}
	
	public int getSuccessCount() {
		return successCount;
	}
	public int getFailureCount() {
		return failureCount;
	}
	public String getMessage() {
		String message = "";
		switch (status) {
		case WAITING:
			message = "Waiting...";
			break;
		case PREPARING:
			message =  "Preparing preliminary index data...";
			break;
		case BUSY:
			message =  "Indexed " + this.getProcessedCount() + " out of " + totalCount;
			break;
		case COMMITING:
			message =  "Commiting index...";
			break;
		}
		if (action!=null && action.length()>0)
			message += " " + action;
		return message;
	}

	
	public long getTotalTime() {
		if (this.startTime==0)
			return 0;
		if (this.endTime==0) 
			return System.currentTimeMillis() - this.startTime;
		return this.endTime - this.startTime;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
}