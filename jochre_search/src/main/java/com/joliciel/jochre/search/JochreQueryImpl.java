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
package com.joliciel.jochre.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import com.joliciel.talismane.utils.LogUtils;

class JochreQueryImpl implements JochreQuery {
	private static final Log LOG = LogFactory.getLog(JochreQueryImpl.class);
	private int decimalPlaces = 4;
	private int maxDocs = 20;
	private String queryString = null;
	private String language = "en";
	private String[] docFilter = null;
	private String filterField = JochreIndexField.id.name();
	private Query luceneQuery = null;
	private Filter luceneFilter = null;
	private int[] docIds = null;

	public JochreQueryImpl() {}
	
	public JochreQueryImpl(Map<String,String> argMap) {
		try {
			for (Entry<String, String> argEntry : argMap.entrySet()) {
				String argName = argEntry.getKey();
				String argValue = argEntry.getValue();
				if (argName.equalsIgnoreCase("query")) {
					this.setQueryString(argValue);
				} else if (argName.equalsIgnoreCase("queryFile")) {
					File queryFile = new File(argValue);
					Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(queryFile), "UTF-8")));
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();
						this.setQueryString(line);
						break;
					}
					scanner.close();
				} else if (argName.equalsIgnoreCase("maxDocs")) {
					int maxDocs = Integer.parseInt(argValue);
					this.setMaxDocs(maxDocs);
				} else if (argName.equalsIgnoreCase("decimalPlaces")) {
					int decimalPlaces = Integer.parseInt(argValue);
					this.setDecimalPlaces(decimalPlaces);
				} else if (argName.equalsIgnoreCase("lang")) {
					this.setLanguage(argValue);
				} else if (argName.equalsIgnoreCase("filter")) {
					if (argValue.length()>0) {
						String[] idArray = argValue.split(",");
						this.setDocFilter(idArray);
					}
				} else if (argName.equalsIgnoreCase("filterField")) {
					if (argValue.length()>0)
						this.setFilterField(argValue);
				} else {
					LOG.trace("JochreQuery unknown option: " + argName);
				}
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#getDecimalPlaces()
	 */
	@Override
	public int getDecimalPlaces() {
		return decimalPlaces;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#setDecimalPlaces(int)
	 */
	@Override
	public void setDecimalPlaces(int decimalPlaces) {
		if (this.decimalPlaces!=decimalPlaces) {
			this.decimalPlaces = decimalPlaces;

		}
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#getMaxDocs()
	 */
	@Override
	public int getMaxDocs() {
		return maxDocs;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#setMaxDocs(int)
	 */
	@Override
	public void setMaxDocs(int maxDocs) {
		this.maxDocs = maxDocs;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#getQueryString()
	 */
	@Override
	public String getQueryString() {
		return queryString;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#setQueryString(java.lang.String)
	 */
	@Override
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#getLanguage()
	 */
	@Override
	public String getLanguage() {
		return language;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#setLanguage(java.lang.String)
	 */
	@Override
	public void setLanguage(String language) {
		this.language = language;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#getDocFilter()
	 */
	@Override
	public String[] getDocFilter() {
		return docFilter;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#setDocFilter(java.lang.String[])
	 */
	@Override
	public void setDocFilter(String[] docFilter) {
		this.docFilter = docFilter;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#getFilterField()
	 */
	@Override
	public String getFilterField() {
		return filterField;
	}

	/* (non-Javadoc)
	 * @see fr.cfh.search.CFHQuery#setFilterField(java.lang.String)
	 */
	@Override
	public void setFilterField(String filterField) {
		this.filterField = filterField;
	}
	
	public int[] getDocIds() {
		return docIds;
	}

	public void setDocIds(int[] docIds) {
		this.docIds = docIds;
	}

	@Override
	public Query getLuceneQuery() {
		try {
			if (luceneQuery==null) {
				LOG.debug("Parsing query: " + this.getQueryString());
				LOG.debug("Max docs: " + this.getMaxDocs());
				Analyzer analyzer = new JochreWhitespaceAnalyser();
				QueryParser queryParser = new QueryParser(JochreIndexField.text.name(), analyzer);
				luceneQuery = queryParser.parse(this.getQueryString());
			}
			return luceneQuery;
		} catch (ParseException pe) {
			LogUtils.logError(LOG, pe);
			throw new RuntimeException(pe);
		}
	}

	@Override
	public Filter getLuceneFilter() {
		if (luceneFilter==null && this.getDocFilter()!=null) {
			
//			luceneFilter = new FieldCacheTermsFilter(this.getFilterField(), this.getDocFilter());
		}
		return luceneFilter;
	}
}
