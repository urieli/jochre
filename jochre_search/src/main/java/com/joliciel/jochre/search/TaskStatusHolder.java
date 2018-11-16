package com.joliciel.jochre.search;

/**
 * Counts success and failure statuses.
 * @author Assaf Urieli
 *
 */
public interface TaskStatusHolder {
  public int getProcessedCount();
  public int getTotalCount();
  public int getSuccessCount();
  public int getFailureCount();
  public void incrementSuccessCount(int increment);
  public void incrementFailureCount(int increment);
  public void setTotalCount(int totalCount);
  public long getTotalTime();
  public String getMessage();
  public long getLastUpdated();
  
  public String getAction();
  public void setAction(String action);
}
