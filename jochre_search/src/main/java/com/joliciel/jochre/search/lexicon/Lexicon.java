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
package com.joliciel.jochre.search.lexicon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A lexicon providing information tied to a given word (inflected form) or
 * lemma.
 * 
 * @author Assaf Urieli
 *
 */
public interface Lexicon {
  static final Logger LOG = LoggerFactory.getLogger(Lexicon.class);

  static Map<String, Lexicon> lexiconMap = new HashMap<>();

  public static Lexicon deserializeLexicon(File lexiconFile) {
    try {
      String path = lexiconFile.getAbsolutePath();
      if (lexiconMap.containsKey(path))
        return lexiconMap.get(path);

      LOG.info("Loading lexicon from: " + path);

      TextFileLexicon lexicon = null;
      FileInputStream fis = new FileInputStream(lexiconFile);
      ZipInputStream zis = new ZipInputStream(fis);
      ZipEntry ze = null;
      while ((ze = zis.getNextEntry()) != null) {
        LOG.debug(ze.getName());
        if (ze.getName().endsWith(".obj")) {
          LOG.debug("deserializing " + ze.getName());
          @SuppressWarnings("resource")
          ObjectInputStream in = new ObjectInputStream(zis);
          lexicon = (TextFileLexicon) in.readObject();
          break;
        }
      }
      zis.close();

      lexiconMap.put(path, lexicon);
      return lexicon;
    } catch (IOException e) {
      LOG.error("Failed to deserialize lexicon " + lexiconFile.getAbsolutePath(), e);
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      LOG.error("Failed to deserialize lexicon " + lexiconFile.getAbsolutePath(), e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Return all lemmas associated with a given word.
   */
  public Set<String> getLemmas(String word);

  /**
   * Return all words associated with a given lemma.
   */
  public Set<String> getWords(String lemma);
}
