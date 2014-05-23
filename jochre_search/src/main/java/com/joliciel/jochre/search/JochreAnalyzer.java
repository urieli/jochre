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

import java.io.Reader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

public class JochreAnalyzer extends StopwordAnalyzerBase {
	private TokenOffsetObserver observer;
	
	public JochreAnalyzer(Version matchVersion) {
		super(matchVersion);
	}

	@Override
	protected TokenStreamComponents createComponents(String contents, Reader reader) {
	      final Tokenizer source = new StandardTokenizer(matchVersion, reader);
	      TokenStream result = new StandardFilter(matchVersion, source);
	      if (observer!=null)
	    	  result = new LocationTokenFilter(observer, result);
	      return new TokenStreamComponents(source, result);
	}

	public TokenOffsetObserver getObserver() {
		return observer;
	}

	public void setObserver(TokenOffsetObserver observer) {
		this.observer = observer;
	}


}
