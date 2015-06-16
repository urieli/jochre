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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.CountedOutcome;

/**
 * Constructs a lexicon from a tab-separated text file resource,
 * organised as:
 * word tab frequency.
 * If there is no tab and frequency, the word will be assumed to have a frequency of 1.
 * Lines starting with a # will be ignored.
 * @author Assaf Urieli
 *
 */
public class TextFileLexicon implements Lexicon, Serializable {

	private static final long serialVersionUID = 1278484873657866572L;
	private static final Log LOG = LogFactory.getLog(TextFileLexicon.class);
	private Map<String,Integer> entries = new HashMap<String, Integer>();

	public TextFileLexicon() {
	}
	
	public TextFileLexicon(Map<String,Integer> entries) {
		this.entries = entries;
	}

	public TextFileLexicon(File textFile) {
		this(textFile, null);
	}
	
	public TextFileLexicon(File textFile, String charset) {
		Scanner scanner;
		try {
			scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(textFile), charset)));

			try {
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					if (!line.startsWith("#")) {
						String[] parts = line.split("\t");
						
						if (parts.length>0) {
							String word = parts[0];
							int frequency = 1;
							if (parts.length>1)
								frequency = Integer.parseInt(parts[1]);
							entries.put(word, frequency);
						}
							
					}
					
				}
			} finally {
				scanner.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void writeFile(Writer writer) {
		for (Entry<String, Integer> entry : entries.entrySet()) {
			try {
				writer.write(entry.getKey() + "\t" + entry.getValue() + "\n");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	public void incrementEntry(String word) {
		Integer freqObj = entries.get(word);
		if (freqObj==null)
			entries.put(word, 1);
		else
			entries.put(word, freqObj.intValue()+1);
	}
	
	public void setEntry(String word, int frequency) {
		entries.put(word, frequency);
	}
	
	@Override
	public int getFrequency(String word) {
		Integer freqObj = entries.get(word);
		if (freqObj!=null)
			return freqObj.intValue();
		else
			return 0;
	}

	@Override
	public List<CountedOutcome<String>> getFrequencies(String word) {
		int frequency = this.getFrequency(word);
		List<CountedOutcome<String>> results = new ArrayList<CountedOutcome<String>>();
		if (frequency>0) {
			results.add(new CountedOutcome<String>(word, frequency));
		}
		return results;
	}
	

	public void serialize(File memoryBaseFile) {
		LOG.debug("serialize");
		boolean isZip = false;
		if (memoryBaseFile.getName().endsWith(".zip"))
			isZip = true;

		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		ZipOutputStream zos = null;
		try
		{
			fos = new FileOutputStream(memoryBaseFile);
			if (isZip) {
				zos = new ZipOutputStream(fos);
				zos.putNextEntry(new ZipEntry("lexicon.obj"));
				out = new ObjectOutputStream(zos);
			} else {
				out = new ObjectOutputStream(fos);
			}
			
			try {
				out.writeObject(this);
			} finally {
				out.flush();
				out.close();
			}
		} catch(IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	public static TextFileLexicon deserialize(ZipInputStream zis) {
		TextFileLexicon memoryBase = null;
		try {
			ZipEntry zipEntry;
			if ((zipEntry = zis.getNextEntry()) != null) {
				LOG.debug("Scanning zip entry " + zipEntry.getName());

				ObjectInputStream in = new ObjectInputStream(zis);
				memoryBase = (TextFileLexicon) in.readObject();
				zis.closeEntry();
				in.close();
			} else {
				throw new RuntimeException("No zip entry in input stream");
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new RuntimeException(cnfe);
		}
		
		return memoryBase;
	}
	
	public static TextFileLexicon deserialize(File memoryBaseFile) {
		LOG.debug("deserializeMemoryBase");
		boolean isZip = false;
		if (memoryBaseFile.getName().endsWith(".zip"))
			isZip = true;

		TextFileLexicon memoryBase = null;
		ZipInputStream zis = null;
		FileInputStream fis = null;
		ObjectInputStream in = null;
	
		try {
			fis = new FileInputStream(memoryBaseFile);
			if (isZip) {
				zis = new ZipInputStream(fis);
				memoryBase = TextFileLexicon.deserialize(zis);
			} else {
				in = new ObjectInputStream(fis);
				try {
					memoryBase = (TextFileLexicon)in.readObject();
				} finally {
					in.close();					
				}
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new RuntimeException(cnfe);
		}
		
		return memoryBase;
	}

	@Override
	public Iterator<String> getWords() {
		return entries.keySet().iterator();
	}
}
