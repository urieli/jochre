package com.joliciel.jochre.search.alto;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.EuclideanIntegerPoint;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochreSearchConfig;

class YiddishAltoStringFixer implements AltoStringFixer {
  private static final Logger LOG = LoggerFactory.getLogger(YiddishAltoStringFixer.class);
  private Set<String> dualCharacterLetters = null;

  public YiddishAltoStringFixer(JochreSearchConfig config) {
  }

  @Override
  public String getHyphenatedContent(String content1, String content2) {
    String newContent = content1;
    if (newContent.endsWith("-")) {
      newContent = newContent.substring(0, newContent.length() - 1);
    }
    char c = newContent.charAt(newContent.length() - 1);
    if (c == 'ם' || c == 'ן' || c == 'ך' || c == 'ף' || c == 'ץ')
      newContent += "-" + content2;
    else
      newContent += content2;
    return newContent;
  }

  @Override
  public void fix(AltoTextBlock block) {
    for (int i = 0; i < block.getTextLines().size(); i++) {
      AltoTextLine row = block.getTextLines().get(i);
      // look for acronyms (with quotes inside).
      for (int j = 1; j < row.getStrings().size() - 1; j++) {
        AltoString prevString = row.getStrings().get(j - 1);
        AltoString string = row.getStrings().get(j);
        AltoString nextString = row.getStrings().get(j + 1);
        if (string.getContent().equals("\"") & !prevString.isWhiteSpace() && !prevString.isPunctuation() && !nextString.isWhiteSpace()
            && !nextString.isPunctuation()) {
          // quote in the middle of two alphabetic strings, must be an
          // acronym
          // merge prev string with double quote
          prevString.mergeWithNext();
          // merge prev string with next string
          prevString.mergeWithNext();

          // since the current string and next string were removed,
          // we decrement j by 1, placing it on the word which
          // followed the
          // next string
          j--;
        }
      }

      // always merge apostrophes with previous if previous is one letter
      // or immediately followed by word
      for (int j = 1; j < row.getStrings().size(); j++) {
        AltoString prevString = row.getStrings().get(j - 1);
        AltoString string = row.getStrings().get(j);
        AltoString nextString = null;
        if (j + 1 < row.getStrings().size()) {
          nextString = row.getStrings().get(j + 1);
        }
        String stringContent = string.getContent();
        String prevStringContent = prevString.getContent();

        if (stringContent.equals("'") & !prevString.isWhiteSpace() && !prevString.isPunctuation()) {
          if (prevStringContent.length() == 1 || this.getDualCharacterLetters().contains(prevStringContent))
            prevString.mergeWithNext();
          else if (nextString != null && !nextString.isWhiteSpace() && !nextString.isPunctuation()) {
            prevString.mergeWithNext();
          }
        }
      }

      // merge apostrophes with next if inside a word
      for (int j = 0; j < row.getStrings().size() - 1; j++) {
        AltoString string = row.getStrings().get(j);
        AltoString nextString = row.getStrings().get(j + 1);
        boolean afterWhiteSpaceOrPunctuation = j == 0 || row.getStrings().get(j - 1).isWhiteSpace() || row.getStrings().get(j - 1).isPunctuation();
        String stringContent = string.getContent();

        if (!string.isPunctuation() && stringContent.length() > 1 && stringContent.endsWith("'")) {
          if (afterWhiteSpaceOrPunctuation && stringContent.equals("ס'") || stringContent.equals("מ'") || stringContent.equals("ר'")
              || stringContent.equals("כ'")) {
            // do nothing
          } else if (!nextString.isWhiteSpace() && !nextString.isPunctuation()) {
            // merge
            string.mergeWithNext();
            j--;
          }
        }
      } // next string

      // try to fix words emphasized by increasing separation
      List<List<AltoString>> emphasizeCandidates = new ArrayList<>();
      List<AltoString> currentSequence = new ArrayList<>();
      for (AltoString string : row.getStrings()) {
        if (string.isWhiteSpace())
          continue;
        if ((string.getContent().length() == 1 || this.getDualCharacterLetters().contains(string.getContent())) && (!string.isPunctuation())) {
          currentSequence.add(string);
        } else if (currentSequence.size() > 0) {
          currentSequence = new ArrayList<>();
        }
        if (currentSequence.size() == 2)
          emphasizeCandidates.add(currentSequence);
      }

      // for each candidate, check if separations can be broken into two
      // clear groups or not
      List<List<AltoString>> emphasized = new ArrayList<>();
      Random random = new Random();
      EuclideanIntegerPoint origin = new EuclideanIntegerPoint(new int[] { 0 });
      for (List<AltoString> candidate : emphasizeCandidates) {
        logEmph(candidate);

        if (candidate.size() > 2) {
          List<EuclideanIntegerPoint> points = new ArrayList<>();
          for (int j = 1; j < candidate.size(); j++) {
            AltoString string1 = candidate.get(j - 1);
            AltoString string2 = candidate.get(j);
            int distance = (string1.getRectangle().x - string2.getRectangle().x) - string2.getRectangle().width;
            EuclideanIntegerPoint point = new EuclideanIntegerPoint(new int[] { distance });
            points.add(point);
          }
          if (LOG.isTraceEnabled())
            LOG.trace("points: " + points);
          KMeansPlusPlusClusterer<EuclideanIntegerPoint> kMeansClusterer = new KMeansPlusPlusClusterer<>(random);
          List<Cluster<EuclideanIntegerPoint>> clusters = null;
          try {
            clusters = kMeansClusterer.cluster(points, 2, 100);
          } catch (ArithmeticException e) {
            // all points exactly equal, impossible to cluster by 2
            // do nothing
            if (LOG.isTraceEnabled())
              LOG.trace("Impossible to cluster in 2");
          }
          boolean separated = false;
          if (clusters != null && clusters.size() == 2) {
            Cluster<EuclideanIntegerPoint> cluster1 = clusters.get(0);
            Cluster<EuclideanIntegerPoint> cluster2 = clusters.get(1);
            double distance1 = cluster1.getCenter().distanceFrom(origin);
            double distance2 = cluster2.getCenter().distanceFrom(origin);
            if (distance2 < distance1) {
              Cluster<EuclideanIntegerPoint> cluster3 = cluster2;
              cluster2 = cluster1;
              cluster1 = cluster3;
              double distance3 = distance1;
              distance1 = distance2;
              distance2 = distance3;
            }
            double ratio = distance2 / distance1;
            if (LOG.isTraceEnabled()) {
              LOG.trace("cluster1 (" + cluster1.getPoints().size() + "): " + cluster1.getPoints());
              LOG.trace("distance1: " + distance1);
              LOG.trace("cluster2 (" + cluster2.getPoints().size() + "):: " + cluster2.getPoints());
              LOG.trace("distance2: " + distance2);
              LOG.trace("ratio: " + ratio);
            }
            // The criteria here for separation into words are
            // arbitrary.
            // What we want is a sufficient difference between the
            // two clusters to assume
            // it is a word break, and at least twice as many
            // letters inside the words as we have words
            if (cluster1.getPoints().size() >= cluster2.getPoints().size() * 2 && ratio > 1.8) {
              // separate this candidate into multiple words
              separated = true;

              int minSepForWord = Integer.MAX_VALUE;
              for (EuclideanIntegerPoint point : cluster2.getPoints()) {
                if (point.getPoint()[0] < minSepForWord)
                  minSepForWord = point.getPoint()[0];
              }

              currentSequence = new ArrayList<>();
              currentSequence.add(candidate.get(0));
              for (int j = 1; j < candidate.size(); j++) {
                AltoString string1 = candidate.get(j - 1);
                AltoString string2 = candidate.get(j);
                int distance = (string1.getRectangle().x - string2.getRectangle().x) - string2.getRectangle().width;
                if (distance < minSepForWord) {
                  currentSequence.add(string2);
                } else {
                  emphasized.add(currentSequence);
                  logEmph(currentSequence);
                  currentSequence = new ArrayList<>();
                  currentSequence.add(string2);
                }
              }
              emphasized.add(currentSequence);
              logEmph(currentSequence);
            }
          }
          if (!separated) {
            emphasized.add(candidate);
            logEmph(candidate);
          }
        } else {
          emphasized.add(candidate);
          logEmph(candidate);
        }
      } // next candidate

      List<AltoString> emphasizedWords = new ArrayList<>();

      for (List<AltoString> word : emphasized) {
        if (word.size() > 1) {
          AltoString string0 = word.get(0);
          for (int j = 1; j < word.size(); j++) {
            string0.mergeWithNext();
          }
          string0.setStyle(AltoString.SEP_EMPH_STYLE);
          emphasizedWords.add(string0);
        }
      }

      if (LOG.isTraceEnabled()) {
        if (emphasizedWords.size() > 0) {
          LOG.trace(emphasizedWords.toString());
          emphasizedWords.size();
        }
      }

    } // next row
  }

  private void logEmph(List<AltoString> candidate) {
    if (LOG.isTraceEnabled()) {
      StringBuilder candidateText = new StringBuilder();
      for (AltoString string : candidate) {
        candidateText.append(" " + string.getContent());
      }
      LOG.trace("Emph: " + candidateText.toString());
    }
  }

  private Set<String> getDualCharacterLetters() {
    if (dualCharacterLetters == null) {
      dualCharacterLetters = new TreeSet<>();
      String[] dualCharacterLetterArray = new String[] { "אָ", "אַ", "בּ", "פּ", "וּ", "פֿ", "שׁ", "וֹ", "יִ", "ײַ", "כֿ", "תּ", "אֶ", "כּ", "בֿ", "עֵ",
          "אִ", "שׂ", "נָ", "מְ", "הֶ", "מַ", "בָּ", "לִ", "נִ", "עֶ", "כֶ", "יי", "וו", "אֵ", "וי" };
      for (String letter : dualCharacterLetterArray) {
        dualCharacterLetters.add(letter);
      }
    }
    return dualCharacterLetters;
  }
}
