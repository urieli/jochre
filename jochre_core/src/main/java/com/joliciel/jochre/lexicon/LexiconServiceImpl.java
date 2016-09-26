///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

import com.joliciel.jochre.JochreSession;

class LexiconServiceImpl implements LexiconServiceInternal {
	LexiconServiceLocator lexiconServiceLocator;

	private final JochreSession jochreSession;

	public LexiconServiceImpl(LexiconServiceLocator locator, JochreSession jochreSession) {
		this.lexiconServiceLocator = locator;
		this.jochreSession = jochreSession;
	}

	@Override
	public CorpusLexiconBuilder getCorpusLexiconBuilder(WordSplitter wordSplitter) {
		CorpusLexiconBuilderImpl lexiconBuilder = new CorpusLexiconBuilderImpl(jochreSession);
		lexiconBuilder.setLexiconService(this);
		lexiconBuilder.setWordSplitter(wordSplitter);
		return lexiconBuilder;
	}

	public LexiconServiceLocator getLexiconServiceLocator() {
		return lexiconServiceLocator;
	}

	public void setLexiconServiceLocator(LexiconServiceLocator lexiconServiceLocator) {
		this.lexiconServiceLocator = lexiconServiceLocator;
	}

	@Override
	public MostLikelyWordChooser getMostLikelyWordChooser(Lexicon lexicon, WordSplitter wordSplitter) {
		MostLikelyWordChooserImpl wordChooser = new MostLikelyWordChooserImpl(jochreSession);
		wordChooser.setLexiconServiceInternal(this);
		wordChooser.setLetterGuesserService(this.getLexiconServiceLocator().getJochreServiceLocator().getLetterGuesserServiceLocator().getLetterGuesserService());
		wordChooser.setWordSplitter(wordSplitter);
		wordChooser.setLexicon(lexicon);
		return wordChooser;
	}
}
