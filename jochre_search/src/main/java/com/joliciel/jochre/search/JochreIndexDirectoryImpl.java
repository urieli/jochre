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
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.LogUtils;

class JochreIndexDirectoryImpl implements JochreIndexDirectory {
	private static final Log LOG = LogFactory.getLog(JochreIndexDirectoryImpl.class);

	File directory;
	File pdfFile;
	File altoFile;
	String name;
	Map<String, String> metaData;
	Instructions instructions;
	
	public JochreIndexDirectoryImpl(File directory) {
		this.directory = directory;
		this.name = this.directory.getName();
	}

	public File getPdfFile() {
		if (this.pdfFile==null) {
			File pdfFile = new File(this.directory, this.name + ".pdf");
			if (!pdfFile.exists()) {
				pdfFile = null;
				File[] pdfFiles = this.directory.listFiles(new FilenameFilter() {
					
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".pdf");
					}
				});
				if (pdfFiles.length>0) {
					pdfFile = pdfFiles[0];
				}
			}
			if (pdfFile==null)
				throw new RuntimeException("Could not find PDF file in " + this.directory.getAbsolutePath());
			this.pdfFile = pdfFile;
		}
		return this.pdfFile;
	}

	public File getAltoFile() {
		if (this.altoFile==null) {
			File altoFile = new File(this.directory, this.name + ".zip");
			if (!altoFile.exists())
				altoFile = new File(this.directory, this.name + ".xml");
			if (!altoFile.exists()) {
				altoFile = null;
				File[] altoFiles = this.directory.listFiles(new FilenameFilter() {
					
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".zip");
					}
				});
				if (altoFiles.length>0) {
					altoFile = altoFiles[0];
				}
				if (altoFile==null) {
					altoFiles = this.directory.listFiles(new FilenameFilter() {
						
						@Override
						public boolean accept(File dir, String name) {
							return name.endsWith(".xml");
						}
					});
					if (altoFiles.length>0) {
						altoFile = altoFiles[0];
					}
				}
			}
			if (altoFile==null)
				throw new RuntimeException("Could not find Alto file in " + this.directory.getAbsolutePath());
			this.altoFile = altoFile;
		}
		return altoFile;
	}

	public Map<String, String> getMetaData() {
		if (this.metaData==null) {
			PdfMetadataReader pdfMetadataReader = new PdfMetadataReader(this.getPdfFile());
			this.metaData = pdfMetadataReader.getFields();
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
		if (instructions==null) {
			File deleteFile = new File(this.directory, "delete");
			if (deleteFile.exists())
				instructions = Instructions.Delete;
			deleteFile = new File(this.directory, "delete.txt");
			if (deleteFile.exists())
				instructions = Instructions.Delete;
			if (instructions==null) {
				File skipFile = new File(this.directory, "skip");
				if (skipFile.exists())
					instructions = Instructions.Skip;
				skipFile = new File(this.directory, "skip.txt");
				if (skipFile.exists())
					instructions = Instructions.Skip;
			}
			if (instructions==null) {
				File updateFile = new File(this.directory, "update");
				if (updateFile.exists())
					instructions = Instructions.Update;
				updateFile = new File(this.directory, "update.txt");
				if (updateFile.exists())
					instructions = Instructions.Update;
			}
			if (instructions==null)
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
				ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(altoFile)));
				@SuppressWarnings("unused")
				ZipEntry ze = null;
			    if ((ze = zis.getNextEntry()) != null) {
					uis = new UnclosableInputStream(zis);
			    }
			} else {
				InputStream is = new BufferedInputStream(new FileInputStream(altoFile));
				uis = new UnclosableInputStream(is);
			}
			return uis;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

}
