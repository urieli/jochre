package com.joliciel.jochre.yiddish;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.joliciel.jochre.lang.DefaultLinguistics;

public class YiddishLinguistics extends DefaultLinguistics {
  private static final String PUNCTUATION = ":,.?!;*()[]{}<>—\\\"'«»|/%“„-";
  private static final String DIGITS = "0123456789";
  private static final Pattern NUMBER = Pattern.compile("\\d+");

  // notice no dash in the punctuation
  private static final String SPLITTER_PUNCTUATION = ":,.?!;*()[]{}<>—\\\"'«»|/%“„";

  @Override
  public boolean isLeftToRight() {
    return false;
  }

  @Override
  public String standardiseWord(String originalWord) {
    String word = originalWord;
    // double-character fixes
    word = word.replaceAll("וו", "װ");
    word = word.replaceAll("וי", "ױ");
    word = word.replaceAll("ױִ", "ויִ");

    // systematic replacements, including redundent melupm vov and khirik
    // yud
    word = word.replaceAll("װוּ", "װוּּ");
    word = word.replaceAll("וּװ", "וּּװ");
    word = word.replaceAll("וּי", "וּּי");
    word = word.replaceAll("וּ", "ו");
    word = word.replaceAll("עיִ", "עיִִ");
    word = word.replaceAll("אַיִ", "אַיִִ");
    word = word.replaceAll("אָיִ", "אָיִִ");
    word = word.replaceAll("ויִ", "ויִִ");
    word = word.replaceAll("וּיִ", "וּיִִ");
    word = word.replaceAll("יִי", "יִִי");
    word = word.replaceAll("ייִ", "ייִִ");
    word = word.replaceAll("יִ", "י");
    word = word.replaceAll("עֶ", "ע");
    word = word.replaceAll("עֵ", "ע");
    word = word.replaceAll("אֵ", "ע");
    word = word.replaceAll("אֶ", "ע");
    word = word.replaceAll("שׁ", "ש");
    word = word.replaceAll("וֹ", "ו");
    word = word.replaceAll("\\Aת([^ּ])", "תּ$1");

    // more double-character fixes
    word = word.replaceAll("(.)יי", "$1ײ");
    word = word.replaceAll("ייַ", "ײַ");
    word = word.replaceAll("“", "\"");
    word = word.replaceAll("''", "\"");
    word = word.replaceAll(",,", "„");
    if (word.startsWith("יי"))
      word = "ייִ" + word.substring(2);

    // silent
    if (word.equals("װאו"))
      word = "װוּ";
    word = word.replaceAll("װאו([^ּ])", "װוּ$1");
    word = word.replaceAll("ואװ", "וּװ");
    word = word.replaceAll("װאױ", "װױ");
    word = word.replaceAll("אַא", "אַ");

    // silent ה
    word = word.replaceAll("טהו", "טו");
    word = word.replaceAll("טהאָ", "טאָ");

    // apostrophes all over the place (except at the end)
    word = word.replaceAll("(.)'(.)", "$1$2");

    // adjectives with דיג instread of דיק
    word = word.replaceAll("(.)דיג\\z", "$1דיק");
    word = word.replaceAll("(.)דיגן\\z", "$1דיקן");

    // word = YiddishWordSplitter.getEndForm(word);

    if (NUMBER.matcher(word).matches()) {
      // reverse numbers
      char[] newWord = new char[word.length()];
      for (int i = 0; i < word.length(); i++) {
        newWord[i] = word.charAt(word.length() - i - 1);
      }
      word = new String(newWord);
    }
    return word;
  }

  public static String getEndForm(String form) {
    String endForm = form;
    if (endForm.endsWith("מ")) {
      endForm = endForm.substring(0, endForm.length() - 1) + "ם";
    } else if (endForm.endsWith("נ")) {
      endForm = endForm.substring(0, endForm.length() - 1) + "ן";
    } else if (endForm.endsWith("צ")) {
      endForm = endForm.substring(0, endForm.length() - 1) + "ץ";
    } else if (endForm.endsWith("פֿ")) {
      endForm = endForm.substring(0, endForm.length() - 1) + "ף";
    } else if (endForm.endsWith("כ")) {
      endForm = endForm.substring(0, endForm.length() - 1) + "ך";
    }

    return endForm;

  }

  @Override
  public boolean isWordPossible(String word) {
    if (word.indexOf('|') >= 0)
      return false;

    boolean possible = true;
    char lastChar = ' ';
    // cannot have "langer" letters in the middle of a word
    for (int i = 0; i < word.length(); i++) {
      char c = word.charAt(i);
      if (lastChar == 'ם' || lastChar == 'ן' || lastChar == 'ך' || lastChar == 'ף' || lastChar == 'ץ') {
        if (PUNCTUATION.indexOf(c) < 0) {
          possible = false;
          break;
        }
      }
      lastChar = c;
    }

    if (possible) {
      // avoid mix of digits and other letters
      boolean haveDigit = false;
      boolean haveNonDigit = false;
      for (int i = 0; i < word.length(); i++) {
        char c = word.charAt(i);
        if (DIGITS.indexOf(c) >= 0) {
          haveDigit = true;
        } else if (PUNCTUATION.indexOf(c) >= 0) {
          if (haveDigit & haveNonDigit) {
            possible = false;
            break;
          }

          haveDigit = false;
          haveNonDigit = false;
        } else {
          haveNonDigit = true;
        }
      }
      if (haveDigit & haveNonDigit)
        possible = false;
    }
    return possible;
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
    wordText = wordText.replaceAll(",,", "„");

    StringTokenizer tokenizer = new StringTokenizer(wordText, SPLITTER_PUNCTUATION, true);

    String previousWord = "";
    String currentWord = null;
    String previousToken = "";
    boolean prevWasPunctuation = false;
    boolean singleQuoteFound = false;
    boolean doubleQuoteFound = false;

    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      boolean isPunctuation = (PUNCTUATION.contains(token));
      if (token.equals("'") && !prevWasPunctuation && previousWord != null) {
        // Yiddish allows a single quote inside a word
        singleQuoteFound = true;
        currentWord = previousWord;
      } else if (token.equals("\"") && !prevWasPunctuation && previousWord != null) {
        // Yiddish marks abbreviations by a double quote
        doubleQuoteFound = true;
        currentWord = previousWord;
      } else if (prevWasPunctuation) {
        if (isPunctuation) {
          if (previousToken.equals("'")) {
            // previous item was single quote
            this.addResult(results, previousWord);
            currentWord = "'" + token;
            singleQuoteFound = false;
          } else if (previousToken.equals("\"")) {
            // previous item was double quote
            this.addResult(results, previousWord);
            currentWord = "\"" + token;
            doubleQuoteFound = false;
          } else if (previousToken.equals("—")) {
            // always separate out long dashes
            this.addResult(results, previousWord);
            currentWord = token;
          } else {
            // combine punctuation marks together
            currentWord = previousWord + token;
          }
        } else if (singleQuoteFound) {
          // single quote in the middle of a word
          currentWord = previousWord + "'" + token;
          singleQuoteFound = false;
        } else if (doubleQuoteFound) {
          // double quote in the middle of a word
          currentWord = previousWord + "\"" + token;
          doubleQuoteFound = false;
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
    if (singleQuoteFound)
      this.addResult(results, "'");
    if (doubleQuoteFound)
      this.addResult(results, "\"");
    return results;
  }

  private void addResult(List<String> results, String word) {
    if (word != null && word.length() > 0) {
      word = this.standardiseWord(word);
      results.add(word);
    }
  }

  @Override
  public Set<String> findVariants(String originalWord) {
    // systematic replacements for non-hebraic words
    Set<String> variants = new TreeSet<String>();

    // in case it's a hebraic word, we keep the initial word in the mix
    variants.add(originalWord);

    // silent ה
    variants = this.addVariants(variants, "(.)עה", "$1ע");
    variants = this.addVariants(variants, "(.)יה", "$1י");
    variants = this.addVariants(variants, "(.)אַה", "$1אַ");
    variants = this.addVariants(variants, "אָה", "אָ");
    variants = this.addVariants(variants, "(.)וה", "$1ו");

    // silent א
    variants = this.addVariants(variants, "(.)יא", "$1י");

    // diminutives with על
    variants = this.addVariants(variants, "(.)על\\z", "$1ל");

    // the vowel י spelled יע
    variants = this.addVariants(variants, "(.)יע(.)", "$1י$2");

    // accusative ען instead of ן
    variants = this.addVariants(variants, "(.)ען\\z", "$1ן");

    // ח instead of כ
    variants = this.addVariants(variants, "ח(.)", "$1כ");

    // double letters
    variants = this.addVariants(variants, "סס", "ס");
    variants = this.addVariants(variants, "פּפּ", "פּ");
    variants = this.addVariants(variants, "פּפּ", "פּ");
    variants = this.addVariants(variants, "פֿפֿ", "פֿ");
    variants = this.addVariants(variants, "ננ", "נ");
    variants = this.addVariants(variants, "ממ", "מ");
    variants = this.addVariants(variants, "לל", "ל");

    // בּ instead of ב
    variants = this.addVariants(variants, "א([^ַָ])", "אַ$1");
    variants = this.addVariants(variants, "א([^ַָ])", "אָ$1");
    variants = this.addVariants(variants, "יִ", "י");
    variants = this.addVariants(variants, "פ([^ּֿ])", "פֿ$1");
    variants = this.addVariants(variants, "פ([^ּֿ])", "פּ$1");
    variants = this.addVariants(variants, "ב([^ּֿ])", "בֿ$1");
    variants = this.addVariants(variants, "ב([^ּֿ])", "בּ$1");
    variants = this.addVariants(variants, "וּ", "ו");

    // niqqud
    variants = this.addVariants(variants, "כ", "כּ");
    variants = this.addVariants(variants, "ב", "בֿ");
    variants = this.addVariants(variants, "בּ", "ב");
    variants = this.addVariants(variants, "כֿ", "כ");
    variants = this.addVariants(variants, "פ", "פֿ");
    variants = this.addVariants(variants, "פּ", "פ");
    variants = this.addVariants(variants, "װו", "װוּ");

    // other typical variants
    variants = this.addVariants(variants, "דט", "ט");
    variants = this.addVariants(variants, "\\Aפֿער(.)", "פֿאַר$1");
    variants = this.addVariants(variants, "\\Aפער(.)", "פֿאַר$1");
    variants = this.addVariants(variants, "\\Aבע(.)", "באַ$1");
    variants = this.addVariants(variants, "\\Aבּע(.)", "באַ$1");

    variants = this.addVariants(variants, "ײ", "ײַ");
    variants = this.addVariants(variants, "(.)דיג\\z", "$1דיק");
    variants = this.addVariants(variants, "(.)דיגער\\z", "$1דיקער");
    variants = this.addVariants(variants, "(.)דיגע\\z", "$1דיקע");
    variants = this.addVariants(variants, "(.)דיגן\\z", "$1דיקן");

    return variants;
  }

  Set<String> addVariants(Set<String> variants, String regex, String replacement) {
    Set<String> newVariants = new TreeSet<String>();

    for (String variant : variants) {
      newVariants.add(variant);
      newVariants.add(variant.replaceAll(regex, replacement));
    }
    return newVariants;
  }
}
