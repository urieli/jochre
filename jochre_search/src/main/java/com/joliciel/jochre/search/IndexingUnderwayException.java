package com.joliciel.jochre.search;

/**
 * Thrown when an index update is requested, but indexing is already underway.
 *
 */
public class IndexingUnderwayException extends JochreSearchException {
  private static final long serialVersionUID = 1L;

  public IndexingUnderwayException() {
    super("Search index construction already underway. Try again later.");
  }
}
