package com.joliciel.jochre.search.feedback;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.IndexSearcher;

import com.joliciel.jochre.search.JochreIndexDocument;
import com.joliciel.jochre.search.JochreIndexWord;
import com.joliciel.jochre.search.JochreSearchConfig;

/**
 * A user's suggestion for a given ocr'd word.
 * 
 * @author Assaf Urieli
 *
 */
public class FeedbackSuggestion {
  private int id;
  private String user;
  private FeedbackWord word;
  private int wordId;
  private String font;
  private String language;
  private String text;
  private String previousText;
  private Date createDate;
  private boolean ignored = false;
  private String ip = null;

  private final FeedbackDAO feedbackDAO;

  /**
   * Find any suggestions made on a given document path and page index, in order
   * of creation.
   * 
   * @param path
   *          the path to the document
   * @param pageIndex
   *          the page index inside the document
   */
  public static List<FeedbackSuggestion> findSuggestions(String path, int pageIndex, String configId) {
    JochreSearchConfig config = JochreSearchConfig.getInstance(configId);
    if (!config.hasDatabase())
      return Collections.emptyList();
    FeedbackDAO feedbackDAO = FeedbackDAO.getInstance(configId);

    FeedbackDocument doc = feedbackDAO.findDocument(path);
    if (doc == null) {
      return Collections.emptyList();
    }
    return feedbackDAO.findSuggestions(doc, pageIndex);
  }

  /**
   * Find any suggestions made on a given document, grouped by page index and
   * ordered by creation date.
   * 
   * @param path
   *          the path to the document.
   */
  public static Map<Integer, List<FeedbackSuggestion>> findSuggestions(String path, String configId) {
    JochreSearchConfig config = JochreSearchConfig.getInstance(configId);
    if (!config.hasDatabase())
      return Collections.emptyMap();
    FeedbackDAO feedbackDAO = FeedbackDAO.getInstance(configId);

    FeedbackDocument doc = feedbackDAO.findDocument(path);
    if (doc == null) {
      return Collections.emptyMap();
    }
    return feedbackDAO.findSuggestions(doc);
  }

  /**
   * Make a suggestion for a given word in a JochreDocument.
   * 
   * @param docId
   *          The Lucene docId
   * @param offset
   *          The word's offset within the document.
   * @param suggestion
   *          The new suggestion
   * @param username
   *          The user who made the suggestion
   * @param ip
   *          The ip address for this suggestion
   * @param fontCode
   *          The font code for this suggestion
   * @param languageCode
   *          The language code for this suggestion
   * @return the suggestion created
   * @throws IOException
   */
  public FeedbackSuggestion(IndexSearcher indexSearcher, int docId, int offset, String text, String username, String ip,
      String fontCode, String languageCode, String configId) throws IOException {
    this(configId);
    JochreIndexDocument jochreDoc = new JochreIndexDocument(indexSearcher, docId, configId);
    JochreIndexWord jochreWord = jochreDoc.getWord(offset);
    FeedbackWord word = FeedbackWord.findOrCreateWord(jochreWord, configId);

    this.setWord(word);
    this.setUser(username);
    this.setIp(ip);
    this.setFont(fontCode);
    this.setLanguage(languageCode);
    this.setPreviousText(jochreWord.getText());
    this.setText(text);
  }

  FeedbackSuggestion(String configId) {
    this.feedbackDAO = FeedbackDAO.getInstance(configId);
  }

  /**
   * The unique internal id for this suggestion.
   */
  public int getId() {
    return id;
  }

  void setId(int id) {
    this.id = id;
  }

  /**
   * The user who made this suggestion.
   */
  public String getUser() {
    return user;
  }

  void setUser(String user) {
    this.user = user;
  }

  /**
   * The word for which the suggestion was made.
   */
  public FeedbackWord getWord() {
    if (this.word == null && this.wordId != 0)
      this.word = this.feedbackDAO.loadWord(this.wordId);
    return word;
  }

  void setWord(FeedbackWord word) {
    this.word = word;
    if (word != null)
      this.wordId = word.getId();
  }

  public int getWordId() {
    return wordId;
  }

  void setWordId(int wordId) {
    this.wordId = wordId;
  }

  /**
   * The font which the user indicated for this suggestion.
   */
  public String getFont() {
    return font;
  }

  void setFont(String font) {
    this.font = font;
  }

  /**
   * The language which the user indicated for this suggestion.
   */
  public String getLanguage() {
    return language;
  }

  void setLanguage(String language) {
    this.language = language;

  }

  /**
   * The suggested text.
   */
  public String getText() {
    return text;
  }

  void setText(String text) {
    this.text = text;
  }

  /**
   * The text previous to the suggestion.
   */
  public String getPreviousText() {
    return previousText;
  }

  void setPreviousText(String previousText) {
    this.previousText = previousText;
  }

  /**
   * The date when the suggestion was made.
   */
  public Date getCreateDate() {
    return createDate;
  }

  void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  /**
   * Should this suggestion be ignored?
   */
  public boolean isIgnored() {
    return ignored;
  }

  public void setIgnored(boolean ignored) {
    this.ignored = ignored;
  }

  public void save() {
    this.feedbackDAO.saveSuggestion(this);
  }

  boolean isNew() {
    return id == 0;
  }

  /**
   * IP address of this suggestion, represented as a string.
   */
  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }
}
