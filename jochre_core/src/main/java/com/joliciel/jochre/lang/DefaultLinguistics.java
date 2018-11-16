package com.joliciel.jochre.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.JochreSession;
import com.joliciel.talismane.utils.CountedOutcome;
import com.typesafe.config.Config;

public class DefaultLinguistics implements Linguistics {
  private static final Logger LOG = LoggerFactory.getLogger(Linguistics.class);

  // notice no dash or single quote in the punctuation
  private static final String PUNCTUATION = ":,.?!;*()[]{}<>—\\\"«»|/%“„";
  private static final Pattern NUMBER = Pattern.compile("[\\+\\-]?\\d+([\\.\\,]\\d+)*");

  private Set<String> dualCharacterLetters;
  private Set<String> validLetters;
  private Set<Character> validCharacters;
  private Set<Character> punctuation;

  private boolean leftToRight = false;
  private boolean leftToRightChecked = false;
  private boolean characterValidationActive = false;
  private JochreSession jochreSession;

  public DefaultLinguistics() {
  }

  @Override
  public void setJochreSession(JochreSession jochreSession) {
    this.jochreSession = jochreSession;

    Config linguisticsConfig = jochreSession.getConfig().getConfig("jochre.linguistics");
    this.dualCharacterLetters = new TreeSet<>(linguisticsConfig.getStringList("dual-character-letters"));

    List<String> validCharacterList = linguisticsConfig.getStringList("valid-characters");
    validCharacters = new TreeSet<>();
    for (String validCharacter : validCharacterList) {
      if (validCharacter.length() != 1)
        throw new RuntimeException("valid-character: " + validCharacter + " longer than 1 character");
      validCharacters.add(validCharacter.charAt(0));
    }
    if (validCharacters.size()>0)
      characterValidationActive = true;

    List<String> punctuationList = linguisticsConfig.getStringList("punctuation");
    punctuation = new TreeSet<>();
    for (String punct : punctuationList) {
      if (punct.length() != 1)
        throw new RuntimeException("valid-character: " + punct + " longer than 1 character");
      punctuation.add(punct.charAt(0));
      if (validCharacters.size() > 0)
        validCharacters.add(punct.charAt(0));
    }

    validLetters = new TreeSet<>();
    validLetters.addAll(linguisticsConfig.getStringList("valid-characters"));
    if (validCharacters.size() > 0)
      validLetters.addAll(linguisticsConfig.getStringList("punctuation"));
    validLetters.addAll(dualCharacterLetters);
    
    LOG.debug("characterValidationActive: " + characterValidationActive);
    LOG.debug("validLetters: " + validLetters.toString());
    LOG.debug("validCharacters: " + validCharacters.toString());
    LOG.debug("dualCharacterLetters: " + dualCharacterLetters.toString());
  }

  @Override
  public Set<String> getValidLetters() {
    return validLetters;
  }

  @Override
  public Set<Character> getValidCharacters() {
    return validCharacters;
  }

  @Override
  public Set<String> getDualCharacterLetters() {
    return dualCharacterLetters;
  }

  @Override
  public Set<Character> getPunctuation() {
    return punctuation;
  }

  @Override
  public boolean isCharacterValidationActive() {
    return characterValidationActive;
  }

  @Override
  public boolean isLeftToRight() {
    if (!leftToRightChecked) {
      leftToRight = true;
      Locale locale = jochreSession.getLocale();
      LOG.debug("Locale language: " + locale.getLanguage());
      if (locale.getLanguage().equals("he") || locale.getLanguage().equals("yi") || locale.getLanguage().equals("ji")
          || locale.getLanguage().equals("ar")) {
        LOG.debug("Right-to-left");
        leftToRight = false;
      } else {
        LOG.debug("Left-to-right");
      }
      leftToRightChecked = true;
    }
    return leftToRight;
  }

  @Override
  public String standardiseWord(String originalWord) {
    return originalWord;
  }

  @Override
  public boolean isWordPossible(String word) {
    if (word.indexOf('|') >= 0)
      return false;
    return true;
  }

  public void setDualCharacterLetters(Set<String> dualCharacterLetters) {
    this.dualCharacterLetters = dualCharacterLetters;
  }

  public void setValidLetters(Set<String> validLetters) {
    this.validLetters = validLetters;
  }

  public void setValidCharacters(Set<Character> validCharacters) {
    this.validCharacters = validCharacters;
  }

  public void setPunctuation(Set<Character> punctuation) {
    this.punctuation = punctuation;
  }

  @Override
  public List<String> splitText(String wordText) {
    List<String> results = new ArrayList<String>();

    if (wordText.length() == 0) {
      results.add("");
      return results;
    }

    // all numerals are treated identically
    wordText = wordText.replaceAll("[0-9]", "0");

    // split letters are joined back together
    wordText = wordText.replaceAll("\\|(.)\\1\\|", "$1");
    wordText = wordText.replaceAll("\\|(..)\\1\\|", "$1");
    wordText = wordText.replaceAll("\\|(...)\\1\\|", "$1");

    // replace multiple underscores by a single underscore
    wordText = wordText.replaceAll("_++", "_");

    // fix other punctuation
    wordText = wordText.replaceAll("''", "\"");

    StringTokenizer tokenizer = new StringTokenizer(wordText, PUNCTUATION, true);

    String previousWord = "";
    String currentWord = null;
    String previousToken = "";
    boolean prevWasPunctuation = false;

    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      boolean isPunctuation = (PUNCTUATION.contains(token));

      if (prevWasPunctuation) {
        if (isPunctuation) {
          if (previousToken.equals("—")) {
            // always separate out long dashes
            this.addResult(results, previousWord);
            currentWord = token;
          } else {
            // combine punctuation marks together
            currentWord = previousWord + token;
          }
        } else {
          this.addResult(results, previousWord);
          currentWord = token;
        }
      } else {
        // the last word wasn't punctuation
        // so it has to be dealt with separately
        this.addResult(results, previousWord);
        currentWord = token;
      }

      prevWasPunctuation = isPunctuation;
      previousWord = currentWord;
      previousToken = token;
    }
    this.addResult(results, previousWord);

    return results;
  }

  private void addResult(List<String> results, String word) {
    if (word != null && word.length() > 0) {
      results.add(word);
    }
  }

  @Override
  public Set<String> findVariants(String originalWord) {
    return null;
  }

  @Override
  public List<CountedOutcome<String>> getFrequencies(String word) {
    List<CountedOutcome<String>> results = new ArrayList<CountedOutcome<String>>();
    String standardisedWord = this.standardiseWord(word);
    if (LOG.isTraceEnabled()) {
      LOG.trace("getFrequency for: " + word + ", standardised to: " + standardisedWord);
    }

    if (NUMBER.matcher(word).matches()) {
      if (LOG.isTraceEnabled()) {
        LOG.trace(word + " is a number, setting freq to 1");
      }
      results.add(new CountedOutcome<String>(word, 1));
      return results;
    }

    // systematic replacements for non-hebraic words
    Set<String> variants = this.findVariants(standardisedWord);
    if (variants == null) {
      int frequency = jochreSession.getLexicon().getFrequency(standardisedWord);
      if (frequency > 0) {
        results.add(new CountedOutcome<String>(word, frequency));
      }
    } else {
      Set<CountedOutcome<String>> orderedResults = new TreeSet<CountedOutcome<String>>();
      for (String variant : variants) {
        int frequency = jochreSession.getLexicon().getFrequency(variant);
        if (frequency > 0) {
          orderedResults.add(new CountedOutcome<String>(variant, frequency));
        }
        // only count multiple occurrences if it's the exact spelling
        if (!variant.equals(word) && !variant.equals(standardisedWord) && frequency > 1)
          frequency = 1;
        if (LOG.isTraceEnabled()) {
          if (frequency > 0) {
            LOG.trace(variant + ": " + frequency);
          }
        }
      }
      results.addAll(orderedResults);
    }

    if (results.size() == 0 && !jochreSession.getLinguistics().isWordPossible(word)) {
      results.add(new CountedOutcome<String>(word, -1));
    }

    return results;
  }

}
