package com.joliciel.jochre.search.lexicon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

class TextFileLexiconImpl implements TextFileLexicon {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(TextFileLexiconImpl.class);
	public Map<String, Set<String>> wordToLemmaMap = new THashMap<>();
	public Map<String, Set<String>> lemmaToWordMap = new THashMap<>();

	private Locale locale;
	private transient LexiconService lexiconService;

	public TextFileLexiconImpl(Locale locale) {
		this.locale = locale;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.joliciel.jochre.search.lexicon.TextFileLexicon#addLexiconFile(java.io
	 * .File, com.joliciel.jochre.search.lexicon.LexicalEntryReader)
	 */
	@Override
	public void addLexiconFile(File lexiconFile, LexicalEntryReader lexicalEntryReader) {
		try {
			String fileName = lexiconFile.getName();
			Scanner lexiconScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(lexiconFile), "UTF-8")));

			TextNormaliser textNormaliser = lexiconService.getTextNormaliser(locale);

			while (lexiconScanner.hasNextLine()) {
				String line = lexiconScanner.nextLine();
				if (line.trim().length() > 0 && !line.startsWith("#")) {
					LexicalEntry lexicalEntry = lexicalEntryReader.readEntry(line);
					lexicalEntry.setLexiconName(fileName);

					if (textNormaliser != null) {
						lexicalEntry.setWord(textNormaliser.normalise(lexicalEntry.getWord()));
					}

					if (lexicalEntry.getLemma().length() > 0) {
						Set<String> lemmaSet = wordToLemmaMap.get(lexicalEntry.getWord());
						if (lemmaSet == null) {
							lemmaSet = new THashSet<>();
							wordToLemmaMap.put(lexicalEntry.getWord(), lemmaSet);
						}
						lemmaSet.add(lexicalEntry.getLemma());
						Set<String> wordSet = lemmaToWordMap.get(lexicalEntry.getLemma());
						if (wordSet == null) {
							wordSet = new THashSet<>();
							lemmaToWordMap.put(lexicalEntry.getLemma(), wordSet);
						}
						wordSet.add(lexicalEntry.getWord());
					}
				}
			}

			lexiconScanner.close();
		} catch (UnsupportedEncodingException e) {
			LOG.error("Failed to add lexicon " + lexiconFile.getAbsolutePath(), e);
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			LOG.error("Failed to add lexicon " + lexiconFile.getAbsolutePath(), e);
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.joliciel.jochre.search.lexicon.TextFileLexicon#serialize(java.io.
	 * File)
	 */
	@Override
	public void serialize(File outFile) {
		try {
			File parentFile = outFile.getParentFile();
			parentFile.mkdirs();
			FileOutputStream fos = new FileOutputStream(outFile);
			ZipOutputStream zos = new ZipOutputStream(fos);
			zos.putNextEntry(new ZipEntry("lexicon.obj"));
			ObjectOutputStream out = new ObjectOutputStream(zos);
			try {
				out.writeObject(this);
			} finally {
				out.flush();
			}
			zos.flush();
			zos.close();
		} catch (IOException e) {
			LOG.error("Failed to serialize to " + outFile.getAbsolutePath(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Set<String> getLemmas(String word) {
		return this.wordToLemmaMap.get(word);
	}

	@Override
	public Set<String> getWords(String lemma) {
		return this.lemmaToWordMap.get(lemma);
	}

	public LexiconService getLexiconService() {
		return lexiconService;
	}

	public void setLexiconService(LexiconService lexiconService) {
		this.lexiconService = lexiconService;
	}

}
