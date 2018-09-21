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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.search.JochreSearchConfig;

public interface HighlightTermDecorator {
	static final Logger LOG = LoggerFactory.getLogger(HighlightTermDecorator.class);
	static Map<String, HighlightTermDecorator> instances = new HashMap<>();

	public String decorate(String term);

	public static HighlightTermDecorator getInstance(JochreSearchConfig config) {
		HighlightTermDecorator instance = instances.get(config.getConfigId());
		if (instance == null) {
			try {
				String className = config.getConfig().getString("highlighter.decorator-class");

				@SuppressWarnings("unchecked")
				Class<? extends HighlightTermDecorator> clazz = (Class<? extends HighlightTermDecorator>) Class.forName(className);
				Constructor<? extends HighlightTermDecorator> cons = clazz.getConstructor(JochreSearchConfig.class);

				instance = cons.newInstance(config);
				instances.put(config.getConfigId(), instance);
			} catch (ReflectiveOperationException e) {
				LOG.error("Unable to construct HighlightTermDecorator", e);
				throw new RuntimeException(e);
			}
		}
		return instance;
	}
}
