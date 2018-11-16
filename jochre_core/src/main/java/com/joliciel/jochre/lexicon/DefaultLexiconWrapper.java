///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.jochre.lexicon;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For each word in the lexicon, adds variants with an initial uppercase and all
 * upper-case.
 * 
 * @author Assaf Urieli
 *
 */
public class DefaultLexiconWrapper implements Lexicon {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(DefaultLexiconWrapper.class);
  private final Lexicon baseLexicon;
  private final Set<String> upperCaseLexicon = new HashSet<String>();
  private final Locale locale;

  public DefaultLexiconWrapper(Lexicon baseLexicon, Locale locale) {
    this.locale = locale;
    this.baseLexicon = baseLexicon;
    Iterator<String> words = baseLexicon.getWords();
    while (words.hasNext()) {
      String word = words.next();
      if (word.length() > 0) {
        String firstLetter = word.substring(0, 1);

        if (word.length() == 1)
          upperCaseLexicon.add(this.toUpperCaseNoAccents(firstLetter));
        else
          upperCaseLexicon.add(this.toUpperCaseNoAccents(firstLetter) + word.substring(1));

        upperCaseLexicon.add(this.toUpperCaseNoAccents(word));
      }
    }
  }

  @Override
  public int getFrequency(String word) {
    int frequency = baseLexicon.getFrequency(word);
    if (frequency > 0)
      return frequency;

    if (upperCaseLexicon.contains(word))
      return 1;

    return 0;
  }

  String toUpperCaseNoAccents(String string) {
    // decompose accents
    String decomposed = Normalizer.normalize(string, Form.NFD);
    // removing diacritics
    String removed = decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

    String uppercase = removed.toUpperCase(locale);
    return uppercase;
  }

  char getLetterWithoutAccents(char letter) {
    switch (letter) {
    case 'à':
    case 'â':
    case 'á':
    case 'ă':
    case 'ä':
    case 'ą':
      return 'a';
    case 'ç':
    case 'ć':
    case 'ĉ':
    case 'č':
    case 'ċ':
      return 'c';
    case 'ď':
    case 'đ':
      return 'd';
    case 'è':
    case 'é':
    case 'ê':
    case 'ĕ':
    case 'ě':
    case 'ë':
    case 'ė':
    case 'ę':
      return 'e';
    case 'ĝ':
    case 'ģ':
      return 'g';
    case 'ĥ':
    case 'ꜧ':
      return 'h';
    case 'î':
    case 'i':
    case 'ı':
    case 'į':
    case 'í':
    case 'ì':
    case 'ï':
      return 'i';
    case 'ĵ':
      return 'j';
    case 'ł':
      return 'l';
    case 'ń':
    case 'ň':
    case 'ñ':
      return 'n';
    case 'ò':
    case 'ó':
    case 'ô':
    case 'ö':
      return 'o';
    case 'ŕ':
    case 'ř':
      return 'r';
    case 'ś':
    case 'ŝ':
    case 'š':
    case 'ș':
      return 's';
    case 'ṭ':
      return 't';
    case 'ü':
    case 'ŭ':
    case 'ű':
    case 'ū':
    case 'ú':
    case 'ù':
    case 'û':
      return 'u';
    case 'ỿ':
      return 'y';
    case 'ź':
    case 'ž':
    case 'ż':
    case 'ⱬ':
      return 'z';
    }
    return letter;
  }

  @Override
  public Iterator<String> getWords() {
    return baseLexicon.getWords();
  }
}
