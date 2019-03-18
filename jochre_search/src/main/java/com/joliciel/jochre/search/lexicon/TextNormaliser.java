///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochreSearchConfig;
import com.joliciel.jochre.search.highlight.SnippetFinder;

/**
 * Normalises text for storage in index and or lexica.
 * 
 * @author Assaf Urieli
 *
 */
public interface TextNormaliser {
  static final Logger LOG = LoggerFactory.getLogger(SnippetFinder.class);
  static Map<String, TextNormaliser> instances = new HashMap<>();

  public static TextNormaliser getInstance(String configId) {
    TextNormaliser instance = instances.get(configId);
    if (instance == null) {
      try {
        JochreSearchConfig config = JochreSearchConfig.getInstance(configId);
        if (config.getConfig().hasPath("text-normaliser.class")) {
          String className = config.getConfig().getString("text-normaliser.class");

          @SuppressWarnings("unchecked")
          Class<? extends TextNormaliser> clazz = (Class<? extends TextNormaliser>) Class.forName(className);
          Constructor<? extends TextNormaliser> cons = clazz.getConstructor(String.class);

          instance = cons.newInstance(configId);
        }
        instances.put(configId, instance);
      } catch (ReflectiveOperationException e) {
        LOG.error("Unable to construct TextNormaliser", e);
        throw new RuntimeException(e);
      }
    }
    return instance;
  }

  public String normalise(String text);
}
