package com.joliciel.jochre.search.feedback;

import java.util.List;

public class FeedbackService {
  private final String configId;
  private final FeedbackDAO feedbackDAO;

  public FeedbackService(String configId) {
    this.configId = configId;
    this.feedbackDAO = FeedbackDAO.getInstance(configId);
  }

  public List<FeedbackSuggestion> findSuggestions(int startIndex, int endIndex) {
    return this.feedbackDAO.findSuggestions(startIndex, endIndex);
  }
}
