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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.joliciel.jochre.utils.pdf.PdfMetadataReader;
import com.joliciel.jochre.utils.text.DiacriticRemover;

/**
 * A directory to be indexed by JochreSearch. We assume this directory will
 * continue to exist after indexing, since we store the directory path in the
 * index, and retrieve images from the PDF file located in this directory.<br/>
 * The directory name is used to uniquely identify a particular indexed work.
 * When indexing a directory any previous work indexed using the same directory
 * name will be deleted first.<br/>
 * The directory must contain the following files:<br/>
 * <ul>
 * <li><i>filename</i>.pdf: the pdf file containing the images of the work that
 * was ocred.</li>
 * <li><i>filename</i>.zip/.xml: a file in Alto3 format (either zipped or not)
 * containing the OCR text layer of the PDF. If a zip file is found, it is used,
 * otherwise an XML file is used.</li>
 * <li>delete|delete.txt|skip|skip.txt|update|update.txt: if one of these files
 * is present (processed in this order), provides explicit instructions on what
 * to do when indexing this directory. The file contents are ignored, and can be
 * empty - only the filename is important.</li>
 * </ul>
 * The PDF/ZIP/XML filename above is arbitrary: only the extension is required.
 * However, the system will first look for a file with the same name as the
 * directory name, and only then look for any arbitrary file.<br/>
 * 
 * @author Assaf Urieli
 *
 */
public class JochreIndexDirectory {

  public enum Instructions {
    None, Delete, Skip, Update
  }

  private static final Logger LOG = LoggerFactory.getLogger(JochreIndexDirectory.class);

  private File directory;
  private File pdfFile;
  private File altoFile;
  private File metaFile;
  private String name;
  private Map<JochreIndexField, String> metaData;
  private Instructions instructions;
  boolean metaFileRetrieved = false;
  private String path;
  private final Path updateInstructionsPath;

  /**
   * @param directory
   *          the source directory on the file system
   * @param configId
   */
  public JochreIndexDirectory(File directory, String configId) {
    this.directory = directory;
    this.name = this.directory.getName();
    JochreSearchConfig config = JochreSearchConfig.getInstance(configId);
    this.path = config.getContentDir().toURI().relativize(directory.toURI()).getPath();
    this.updateInstructionsPath = (new File(this.directory, "update")).toPath();
  }

  /**
   * @param path
   *          the path to the source directory on the file system relative to the
   *          content directory.
   * @param configId
   */
  public JochreIndexDirectory(String path, String configId) {
    this(new File(JochreSearchConfig.getInstance(configId).getContentDir(), path), configId);
  }

  /**
   * The PDF file being indexed.
   */
  public File getPdfFile() {
    if (this.pdfFile == null) {
      File pdfFile = new File(this.directory, this.name + ".pdf");
      if (!pdfFile.exists()) {
        pdfFile = null;
        File[] pdfFiles = this.directory.listFiles(new FilenameFilter() {

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
        throw new RuntimeException("Could not find PDF file in " + this.directory.getAbsolutePath());
      this.pdfFile = pdfFile;
    }
    return this.pdfFile;
  }

  /**
   * The Alto text layer of the PDF file being indexed.
   */
  public File getAltoFile() {
    if (this.altoFile == null) {
      File altoFile = new File(this.directory, this.name + ".zip");
      if (!altoFile.exists())
        altoFile = new File(this.directory, this.name + ".xml");
      if (!altoFile.exists()) {
        altoFile = null;
        File[] altoFiles = this.directory.listFiles(new FilenameFilter() {

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
        throw new RuntimeException("Could not find Alto file in " + this.directory.getAbsolutePath());
      this.altoFile = altoFile;
    }
    return altoFile;
  }

  /**
   * An optional file containing metadata, with the same name as the PDF file +
   * _meta.xml.
   */
  public File getMetaDataFile() {
    if (!metaFileRetrieved) {
      File pdfFile = this.getPdfFile();
      if (pdfFile != null) {
        String fileBase = pdfFile.getName().substring(0, pdfFile.getName().length() - ".pdf".length());
        File metaFile = new File(this.directory, fileBase + "_meta.xml");
        if (metaFile.exists()) {
          this.metaFile = metaFile;
        }
      }
      metaFileRetrieved = true;
    }
    return this.metaFile;
  }

  /**
   * The metadata contained in the PDF file.
   */
  public Map<JochreIndexField, String> getMetaData() {
    if (this.metaData == null) {
      if (this.getMetaDataFile() == null) {
        PdfMetadataReader pdfMetadataReader = new PdfMetadataReader(this.getPdfFile());
        Map<String, String> pdfMetaData = pdfMetadataReader.getFields();
        pdfMetadataReader.close();

        this.metaData = new HashMap<>();

        // TODO: hack for Yiddish - need to generalize this through
        // config settings

        String bookUrl = pdfMetaData.get("Keywords");
        String title = pdfMetaData.get("Title");
        String author = pdfMetaData.get("Author");

        if (bookUrl != null && bookUrl.length() > 0) {
          this.metaData.put(JochreIndexField.url, bookUrl);
          String id = bookUrl.substring(bookUrl.lastIndexOf('/') + 1);
          this.metaData.put(JochreIndexField.id, id);
        }
        if (title != null && title.length() > 0) {
          title = DiacriticRemover.apply(title);
          this.metaData.put(JochreIndexField.titleEnglish, title);
        }
        if (author != null && author.length() > 0)
          this.metaData.put(JochreIndexField.authorEnglish, author);

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

          this.metaData = new HashMap<>();
          if (id.length() > 0)
            this.metaData.put(JochreIndexField.id, id);
          if (bookUrl.length() > 0)
            this.metaData.put(JochreIndexField.url, bookUrl);
          if (title.length() > 0)
            this.metaData.put(JochreIndexField.titleEnglish, title);
          if (author.length() > 0)
            this.metaData.put(JochreIndexField.authorEnglish, author);
          if (publisher.length() > 0)
            this.metaData.put(JochreIndexField.publisher, publisher);
          if (date.length() > 0)
            this.metaData.put(JochreIndexField.date, date);
          if (authorLang.length() > 0)
            this.metaData.put(JochreIndexField.author, authorLang);
          if (titleLang.length() > 0)
            this.metaData.put(JochreIndexField.title, titleLang);
          if (volume.length() > 0)
            this.metaData.put(JochreIndexField.volume, volume);

        } catch (IOException e) {
          LOG.error("Failed to read metadata from  " + this.getMetaDataFile().getAbsolutePath(), e);
          throw new RuntimeException(e);
        } catch (XPathExpressionException e) {
          LOG.error("Failed to read metadata from  " + this.getMetaDataFile().getAbsolutePath(), e);
          throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
          LOG.error("Failed to read metadata from  " + this.getMetaDataFile().getAbsolutePath(), e);
          throw new RuntimeException(e);
        } catch (SAXException e) {
          LOG.error("Failed to read metadata from  " + this.getMetaDataFile().getAbsolutePath(), e);
          throw new RuntimeException(e);
        }
      }
    }
    return metaData;
  }

  /**
   * The unique directory name.
   */
  public String getName() {
    return name;
  }

  /**
   * The directory wrapped by this object.
   */
  public File getDirectory() {
    return directory;
  }

  /**
   * Explicit instructions on what to do with this directory.
   */
  public Instructions getInstructions() {
    if (instructions == null) {
      File deleteFile = new File(this.directory, "delete");
      if (deleteFile.exists())
        instructions = Instructions.Delete;
      if (instructions == null) {
        File skipFile = new File(this.directory, "skip");
        if (skipFile.exists())
          instructions = Instructions.Skip;
      }
      if (instructions == null) {
        if (Files.exists(updateInstructionsPath))
          instructions = Instructions.Update;
      }
      if (instructions == null)
        instructions = Instructions.None;
    }
    return instructions;
  }

  public void addUpdateInstructions() throws IOException {
    try {
      Files.createFile(updateInstructionsPath);
    } catch (FileAlreadyExistsException ignored) {
    }
  }

  public void removeUpdateInstructions() throws IOException {
    Files.deleteIfExists(updateInstructionsPath);
  }

  /**
   * An input stream for the Alto XML content.
   */
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
      LOG.error("Failed to get altoInputStream", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * The relative path to this directory.
   */
  public String getPath() {
    return path;
  }
}
