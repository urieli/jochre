package com.joliciel.jochre.search;

import com.joliciel.jochre.utils.JochreException;

public class JochreQueryParseException extends JochreException {
  private static final long serialVersionUID = 1L;

  public JochreQueryParseException(String message) {
    super(message);
  }

  public JochreQueryParseException(Exception e) {
    super(e);
  }

  public JochreQueryParseException(Throwable cause) {
    super(cause);
  }

  public JochreQueryParseException(String message, Throwable cause) {
    super(message, cause);
  }

}
