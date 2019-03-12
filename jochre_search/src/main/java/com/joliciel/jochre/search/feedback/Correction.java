package com.joliciel.jochre.search.feedback;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochreIndexField;
import com.joliciel.jochre.search.JochreIndexSearcher;
import com.joliciel.jochre.search.JochreSearchConfig;

/**
 * A correction submitted by a user for a document's metadata.
 *
 */
public class Correction {
  private static final Logger LOG = LoggerFactory.getLogger(Correction.class);

  /**
   * Return all corrections to apply to a specific document path, including all
   * general corrections, in the order in which they were added.
   */
  public static Map<JochreIndexField, List<Correction>> findCorrections(String docPath, String configId) {
    JochreSearchConfig config = JochreSearchConfig.getInstance(configId);
    if (!config.hasDatabase())
      return Collections.emptyMap();
    FeedbackDAO feedbackDAO = FeedbackDAO.getInstance(configId);
    return feedbackDAO.findCorrections(docPath);
  }

  private int id = 0;
  private FeedbackDocument document;
  private final int documentId;
  private final JochreIndexField field;
  private final String user;
  private final String ip;
  private final String value;
  private final String previousValue;
  private final boolean applyEverywhere;
  private final Date createDate;

  private final FeedbackDAO feedbackDAO;

  public static Correction createCorrection(IndexSearcher indexSearcher, String docPath, int docIndex,
      JochreIndexField field, String user, String ip, String value, boolean applyEverywhere, String configId)
      throws IOException {
    JochreIndexSearcher searcher = new JochreIndexSearcher(indexSearcher, configId);

    Map<Integer, Document> docs = searcher.findDocument(docPath, docIndex);
    Document luceneDoc = docs.values().iterator().next();
    String previousValue = luceneDoc.get(field.name());
    FeedbackDocument feedbackDoc = FeedbackDocument.findOrCreateDocument(luceneDoc.get(JochreIndexField.path.name()),
        configId);

    Correction correction = new Correction(feedbackDoc, field, user, ip, value, previousValue, applyEverywhere,
        configId);
    return correction;
  }

  public Correction(int documentId, JochreIndexField field, String user, String ip, String value, String previousValue,
      boolean applyEverywhere, Date createDate, String configId) {
    this.feedbackDAO = FeedbackDAO.getInstance(configId);
    this.documentId = documentId;
    this.field = field;
    this.user = user;
    this.ip = ip;
    this.value = value;
    this.previousValue = previousValue;
    this.applyEverywhere = applyEverywhere;
    this.createDate = createDate;
  }

  public Correction(FeedbackDocument document, JochreIndexField field, String user, String ip, String value,
      String previousValue, boolean applyEverywhere, String configId) {
    this(document.getId(), field, user, ip, value, previousValue, applyEverywhere, new Date(System.currentTimeMillis()),
        configId);
    this.document = document;
  }

  public int getId() {
    return id;
  }

  void setId(int id) {
    this.id = id;
  }

  public int getDocumentId() {
    return documentId;
  }

  public FeedbackDocument getDocument() {
    if (this.document == null && this.documentId > 0) {
      this.document = this.feedbackDAO.loadDocument(this.documentId);
    }
    return document;
  }

  void setDocument(FeedbackDocument document) {
    this.document = document;
  }

  public JochreIndexField getField() {
    return field;
  }

  public String getUser() {
    return user;
  }

  public String getIp() {
    return ip;
  }

  public String getValue() {
    return value;
  }

  public String getPreviousValue() {
    return previousValue;
  }

  /**
   * If true, this correction should be applied to all documents whose current
   * value is {@link #getPreviousValue()} for field {@link #getField()}.
   */
  public boolean isApplyEverywhere() {
    return applyEverywhere;
  }

  public Date getCreateDate() {
    return createDate;
  }

  boolean isNew() {
    return this.id == 0;
  }

  public void save() {
    this.feedbackDAO.saveCorrection(this);
  }

  /**
   * Apply the current correction to a document if applicable.
   * 
   * @param documentPath
   *          the path to the document
   * @param metaData
   *          the document's metadata
   * @return true if applied, false if not
   */
  public boolean apply(String documentPath, Map<JochreIndexField, String> metaData) {
    boolean apply = (documentPath.equals(this.getDocument().getPath()));
    if (!apply && this.applyEverywhere) {
      apply = metaData.containsKey(this.field) && metaData.get(this.field).equals(this.previousValue);
    }
    if (apply) {
      if (LOG.isDebugEnabled()) {
        String prevValue = metaData.containsKey(this.field) ? metaData.get(this.field) : null;
        LOG.debug(
            "For " + documentPath + ", field " + field + ", replacing '" + prevValue + "' with '" + this.value + "'");
      }
      metaData.put(this.field, this.value);
    }
    return apply;
  }
}
