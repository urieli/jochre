///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Assaf Urieli
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
package com.joliciel.jochre.search.feedback;

/**
 * A representation of a document which has been indexed. Although a document
 * can be broken up into multiple sub-documents, these could potentially change
 * from one index run to the next, while the top-level path will not. Hence we
 * don't store the document sub-index, only it's path.
 * 
 * @author Assaf Urieli
 *
 */
public class FeedbackDocument {
  private int id;
  private String path;

  private final FeedbackDAO feedbackDAO;

  public static FeedbackDocument findOrCreateDocument(String path, String configId) {
    FeedbackDAO feedbackDAO = FeedbackDAO.getInstance(configId);
    FeedbackDocument doc = feedbackDAO.findDocument(path);
    if (doc == null) {
      doc = new FeedbackDocument(path, configId);
      doc.save();
    }
    return doc;
  }

  FeedbackDocument(String path, String configId) {
    this(configId);
    this.setPath(path);
  }

  FeedbackDocument(String configId) {
    this.feedbackDAO = FeedbackDAO.getInstance(configId);
  }

  /**
   * The document's unique internal id.
   */
  public int getId() {
    return id;
  }

  void setId(int id) {
    this.id = id;
  }

  /**
   * The document's path, which identifies it outside of the index.
   */
  public String getPath() {
    return path;
  }

  void setPath(String path) {
    this.path = path;
  }

  public String getName() {
    String name = path;
    if (name.endsWith("/"))
      name = name.substring(0, name.length() - 1);
    int lastSlash = name.indexOf('/');
    if (lastSlash > 0) {
      name = name.substring(lastSlash + 1);
    }
    return name;
  }

  boolean isNew() {
    return id == 0;
  }

  void save() {
    feedbackDAO.saveDocument(this);
  }
}
