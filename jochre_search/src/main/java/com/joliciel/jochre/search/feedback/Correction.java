package com.joliciel.jochre.search.feedback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochreIndexField;
import com.joliciel.jochre.search.JochreIndexSearcher;
import com.joliciel.jochre.search.JochreSearchConfig;
import com.typesafe.config.Config;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

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
  private boolean ignore = false;
  private boolean sent = false;
  private List<String> documents = Collections.emptyList();
  private final String configId;

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
    this.configId = configId;
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

  /**
   * Should this correction be ignored?
   */
  public boolean isIgnore() {
    return ignore;
  }

  public void setIgnore(boolean ignore) {
    this.ignore = ignore;
  }

  /**
   * Has this correction been sent via e-mail?
   */
  public boolean isSent() {
    return sent;
  }

  public void setSent(boolean sent) {
    this.sent = sent;
  }

  /**
   * If {@link #isApplyEverywhere()}, a list of documents affected by this
   * correction.
   */
  public List<String> getDocuments() {
    return documents;
  }

  public void setDocuments(List<String> documents) {
    this.documents = documents;
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

  /**
   * If configuration indicates correction should be sent as e-mail, send the
   * e-mail and mark this correction as sent.
   * 
   * @throws IOException
   *           if an exception occurs while reading the Freemarker template
   * @throws TemplateException
   *           if an exception occurs while constructing the message body using
   *           Freemarker
   * @throws MessagingException
   *           if an exception occurs while sending the e-mail
   */
  public void sendEmail() throws IOException, TemplateException, MessagingException {
    JochreSearchConfig config = JochreSearchConfig.getInstance(configId);
    if (config.getConfig().getBoolean("corrections.send-mail")) {
      Config mailConfig = config.getConfig().getConfig("mail");

      Configuration cfg = new Configuration(new Version(2, 3, 23));
      cfg.setCacheStorage(new NullCacheStorage());
      cfg.setObjectWrapper(new DefaultObjectWrapperBuilder(new Version(2, 3, 23)).build());

      Map<String, Object> model = new HashMap<>();
      model.put("correction", this);

      StringWriter writer = new StringWriter();
      Reader templateReader = new BufferedReader(
          new InputStreamReader(Correction.class.getResourceAsStream("correction.ftl")));
      Template template = new Template("correction", templateReader, cfg);
      template.process(model, writer);
      writer.flush();
      String body = writer.toString();

      writer = new StringWriter();
      templateReader = new BufferedReader(
          new InputStreamReader(Correction.class.getResourceAsStream("correction-subject.ftl")));
      template = new Template("correction-subject", templateReader, cfg);
      template.process(model, writer);
      writer.flush();
      String subject = writer.toString();

      Properties mailProps = new Properties();
      mailProps.put("mail.smtp.port", mailConfig.getString("smtp.port"));
      mailProps.put("mail.smtp.auth", mailConfig.getString("smtp.auth"));
      mailProps.put("mail.smtp.starttls.enable", mailConfig.getString("smtp.starttls.enable"));

      Session mailSession = Session.getDefaultInstance(mailProps, null);
      MimeMessage message = new MimeMessage(mailSession);
      message.addRecipient(Message.RecipientType.TO, new InternetAddress(mailConfig.getString("to")));
      if (mailConfig.hasPath("cc"))
        message.addRecipient(Message.RecipientType.CC, new InternetAddress(mailConfig.getString("cc")));
      message.setSubject(subject, "UTF-8");
      message.setText(body, "UTF-8", "html");

      if (LOG.isDebugEnabled())
        LOG.debug("Sending e-mail to " + mailConfig.getString("to") + " for " + subject);
      Transport transport = mailSession.getTransport("smtp");

      transport.connect(mailConfig.getString("smtp.host"), mailConfig.getString("from"),
          mailConfig.getString("password"));
      transport.sendMessage(message, message.getAllRecipients());
      transport.close();

      if (LOG.isDebugEnabled())
        LOG.debug("E-mail sent");
      this.sent = true;
      this.save();
    }
  }
}
