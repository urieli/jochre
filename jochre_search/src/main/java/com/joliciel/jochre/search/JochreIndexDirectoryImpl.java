///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Assaf Urieli
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.joliciel.jochre.utils.pdf.PdfMetadataReader;
import com.joliciel.jochre.utils.text.DiacriticRemover;
import com.joliciel.talismane.utils.LogUtils;

class JochreIndexDirectoryImpl implements JochreIndexDirectory {
	private static final Log LOG = LogFactory
			.getLog(JochreIndexDirectoryImpl.class);

	File directory;
	File pdfFile;
	File altoFile;
	File metaFile;
	String name;
	Map<String, String> metaData;
	Instructions instructions;
	boolean metaFileRetrieved = false;

	public JochreIndexDirectoryImpl(File directory) {
		this.directory = directory;
		this.name = this.directory.getName();
	}

	public File getPdfFile() {
		if (this.pdfFile == null) {
			File pdfFile = new File(this.directory, this.name + ".pdf");
			if (!pdfFile.exists()) {
				pdfFile = null;
				File[] pdfFiles = this.directory
						.listFiles(new FilenameFilter() {

							@Override
							public boolean accept(File dir, String name) {
								return name.toLowerCase().endsWith(".pdf");
							}
						});
				if (pdfFiles.length > 0) {
					pdfFile = pdfFiles[0];
				}
			}
			if (pdfFile == null)
				throw new RuntimeException("Could not find PDF file in "
						+ this.directory.getAbsolutePath());
			this.pdfFile = pdfFile;
		}
		return this.pdfFile;
	}

	public File getAltoFile() {
		if (this.altoFile == null) {
			File altoFile = new File(this.directory, this.name + ".zip");
			if (!altoFile.exists())
				altoFile = new File(this.directory, this.name + ".xml");
			if (!altoFile.exists()) {
				altoFile = null;
				File[] altoFiles = this.directory
						.listFiles(new FilenameFilter() {

							@Override
							public boolean accept(File dir, String name) {
								return name.toLowerCase().endsWith(".zip");
							}
						});
				if (altoFiles.length > 0) {
					altoFile = altoFiles[0];
				}
				if (altoFile == null) {
					altoFiles = this.directory.listFiles(new FilenameFilter() {

						@Override
						public boolean accept(File dir, String name) {
							return name.toLowerCase().endsWith(".xml") && !name.toLowerCase().endsWith("_meta.xml");
						}
					});
					if (altoFiles.length > 0) {
						altoFile = altoFiles[0];
					}
				}
			}
			if (altoFile == null)
				throw new RuntimeException("Could not find Alto file in "
						+ this.directory.getAbsolutePath());
			this.altoFile = altoFile;
		}
		return altoFile;
	}

	public File getMetaDataFile() {
		if (!metaFileRetrieved) {
			File pdfFile = this.getPdfFile();
			if (pdfFile!=null) {
				String fileBase = pdfFile.getName().substring(0, pdfFile.getName().length()-".pdf".length());
				File metaFile = new File(this.directory, fileBase + "_meta.xml");
				if (metaFile.exists()) {
					this.metaFile = metaFile;
				}
			}
			metaFileRetrieved = true;
		}
		return this.metaFile;
	}

	public Map<String, String> getMetaData() {
		if (this.metaData == null) {
			if (this.getMetaDataFile()==null) {
				PdfMetadataReader pdfMetadataReader = new PdfMetadataReader(
						this.getPdfFile());
				Map<String,String> pdfMetaData = pdfMetadataReader.getFields();
				pdfMetadataReader.close();
				
				this.metaData = new HashMap<String, String>();
				
				//TODO: hack for Yiddish - need to generalize this through config settings
				
		        String bookUrl = pdfMetaData.get("Keywords");
		        String title = pdfMetaData.get("Title");
		        String author = pdfMetaData.get("Author");
				
		        if (bookUrl!=null && bookUrl.length()>0) {
		        	this.metaData.put(JochreIndexField.url.name(), bookUrl);
		        	String id = bookUrl.substring(bookUrl.lastIndexOf('/')+1);
		        	this.metaData.put(JochreIndexField.id.name(), id);
		        }
		        if (title!=null && title.length()>0) {
					title = DiacriticRemover.apply(title);
		        	this.metaData.put(JochreIndexField.title.name(), title);
		        }
		        if (author!=null && author.length()>0)
		        	this.metaData.put(JochreIndexField.author.name(), author);

			} else {
				try {
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = dbf.newDocumentBuilder();
					Document dom = db.parse(this.getMetaDataFile());
					Element docElement = dom.getDocumentElement();
					
			        XPathFactory xpf = XPathFactory.newInstance();
			        XPath xp = xpf.newXPath();
			        
			        String id = xp.evaluate("/metadata/identifier/text()", docElement);
			        String bookUrl = xp.evaluate("/metadata/identifier-access/text()", docElement);
			        String title = xp.evaluate("/metadata/title/text()", docElement);
			        String author = xp.evaluate("/metadata/creator/text()", docElement);
			        String publisher = xp.evaluate("/metadata/publisher/text()", docElement);
			        String date = xp.evaluate("/metadata/date/text()", docElement);
			        String authorLang = xp.evaluate("/metadata/creator-alt-script/text()", docElement);
			        String titleLang = xp.evaluate("/metadata/title-alt-script/text()", docElement);
			        String volume = xp.evaluate("/metadata/volume/text()", docElement);
			        
			        LOG.debug("id: " + id);
			        LOG.debug("bookUrl: " + bookUrl);
			        LOG.debug("title: " + title);
			        LOG.debug("author: " + author);
			        LOG.debug("publisher: " + publisher);
			        LOG.debug("date: " + date);
			        LOG.debug("authorLang: " + authorLang);
			        LOG.debug("titleLang: " + titleLang);
			        LOG.debug("volume: " + volume);
			        
			        this.metaData = new HashMap<String, String>();
			        if (id.length()>0)
			        	this.metaData.put(JochreIndexField.id.name(), id);
			        if (bookUrl.length()>0)
			        	this.metaData.put(JochreIndexField.url.name(), bookUrl);
			        if (title.length()>0)
			        	this.metaData.put(JochreIndexField.title.name(), title);
			        if (author.length()>0)
			        	this.metaData.put(JochreIndexField.author.name(), author);
			        if (publisher.length()>0)
			        	this.metaData.put(JochreIndexField.publisher.name(), publisher);
			        if (date.length()>0)
			        	this.metaData.put(JochreIndexField.date.name(), date);
			        if (authorLang.length()>0)
			        	this.metaData.put(JochreIndexField.authorLang.name(), authorLang);
			        if (titleLang.length()>0)
			        	this.metaData.put(JochreIndexField.titleLang.name(), titleLang);
			        if (volume.length()>0)
			        	this.metaData.put(JochreIndexField.volume.name(), volume);
			        
				} catch (IOException e) {
					LogUtils.logError(LOG, e);
					throw new RuntimeException(e);
				} catch (XPathExpressionException e) {
					LogUtils.logError(LOG, e);
					throw new RuntimeException(e);
				} catch (ParserConfigurationException e) {
					LogUtils.logError(LOG, e);
					throw new RuntimeException(e);
				} catch (SAXException e) {
					LogUtils.logError(LOG, e);
					throw new RuntimeException(e);
				}
			}
		}
		return metaData;
	}

	public String getName() {
		return name;
	}

	public File getDirectory() {
		return directory;
	}

	public Instructions getInstructions() {
		if (instructions == null) {
			File deleteFile = new File(this.directory, "delete");
			if (deleteFile.exists())
				instructions = Instructions.Delete;
			deleteFile = new File(this.directory, "delete.txt");
			if (deleteFile.exists())
				instructions = Instructions.Delete;
			if (instructions == null) {
				File skipFile = new File(this.directory, "skip");
				if (skipFile.exists())
					instructions = Instructions.Skip;
				skipFile = new File(this.directory, "skip.txt");
				if (skipFile.exists())
					instructions = Instructions.Skip;
			}
			if (instructions == null) {
				File updateFile = new File(this.directory, "update");
				if (updateFile.exists())
					instructions = Instructions.Update;
				updateFile = new File(this.directory, "update.txt");
				if (updateFile.exists())
					instructions = Instructions.Update;
			}
			if (instructions == null)
				instructions = Instructions.None;
		}
		return instructions;
	}

	@Override
	public UnclosableInputStream getAltoInputStream() {
		try {
			UnclosableInputStream uis = null;
			File altoFile = this.getAltoFile();
			if (altoFile.getName().endsWith(".zip")) {
				ZipInputStream zis = new ZipInputStream(
						new BufferedInputStream(new FileInputStream(altoFile)));
				@SuppressWarnings("unused")
				ZipEntry ze = null;
				if ((ze = zis.getNextEntry()) != null) {
					uis = new UnclosableInputStream(zis);
				}
			} else {
				InputStream is = new BufferedInputStream(new FileInputStream(
						altoFile));
				uis = new UnclosableInputStream(is);
			}
			return uis;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

}
