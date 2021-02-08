package com.joliciel.jochre.graphics;

import com.joliciel.jochre.utils.JochreException;

public class SegmentationException extends JochreException {
  public SegmentationException(String s) {
    super(s);
  }

  public SegmentationException(Throwable cause) {
    super(cause);
  }

  public SegmentationException(String message, Throwable cause) {
    super(message, cause);
  }
}
