///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
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
package com.joliciel.jochre.search.highlight;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochreSearchConfig;

public interface SnippetFinder {
	static final Logger LOG = LoggerFactory.getLogger(SnippetFinder.class);
	static Map<String, SnippetFinder> instances = new HashMap<>();

	/**
	 * Find the best n snippets corresponding to a list of highlight terms.
	 * 
	 * @param docId
	 *            The Lucene document whose snippets we want
	 * @param highlightTerms
	 *            The previously retrieved highlight terms for the document.
	 * @param maxSnippets
	 *            The maximum number of snippets to return.
	 */
	public List<Snippet> findSnippets(IndexSearcher indexSearcher, int docId, Set<String> fields, Set<HighlightTerm> highlightTerms, int maxSnippets)
			throws IOException;

	public static SnippetFinder getInstance(JochreSearchConfig config) {
		SnippetFinder instance = instances.get(config.getConfigId());
		if (instance == null) {
			try {
				String className = config.getConfig().getString("snippet-finder.class");

				@SuppressWarnings("unchecked")
				Class<? extends SnippetFinder> clazz = (Class<? extends SnippetFinder>) Class.forName(className);
				Constructor<? extends SnippetFinder> cons = clazz.getConstructor(JochreSearchConfig.class);

				instance = cons.newInstance(config);
				instances.put(config.getConfigId(), instance);
			} catch (ReflectiveOperationException e) {
				LOG.error("Unable to construct SnippetFinder", e);
				throw new RuntimeException(e);
			}
		}
		return instance;
	}
}
