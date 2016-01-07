package com.joliciel.jochre.search.lexicon;

import java.io.File;
import java.io.Serializable;

public interface TextFileLexicon extends Lexicon, Serializable {

	public void addLexiconFile(File lexiconFile,
			LexicalEntryReader lexicalEntryReader);

	public void serialize(File outFile);

}