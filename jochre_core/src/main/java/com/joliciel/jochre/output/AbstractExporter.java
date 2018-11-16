package com.joliciel.jochre.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.doc.JochreDocument;

public abstract class AbstractExporter implements DocumentObserver {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractExporter.class);
  private final File outputDir;
  protected Writer writer;
  private final String suffix;
  private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
  private final String dateString = format.format(new Date());
  private boolean includeDate = false;
  private String baseName = null;

  public AbstractExporter(File outputDir, String suffix) {
    this.outputDir = outputDir;
    this.suffix = suffix;
  }

  public AbstractExporter(Writer writer) {
    this.writer = writer;
    this.outputDir = null;
    this.suffix = null;
  }

  @Override
  public final void onDocumentStart(JochreDocument jochreDocument) {
    try {
      if (this.outputDir != null) {
        String fileName = baseName;
        if (fileName == null)
          fileName = jochreDocument.getFileBase();
        if (includeDate)
          fileName += "_" + dateString;
        fileName += suffix;
        File file = new File(outputDir, fileName);

        if (suffix.endsWith(".zip")) {
          ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file, false));
          ZipEntry zipEntry = new ZipEntry("contents.txt");
          zos.putNextEntry(zipEntry);
          this.writer = new BufferedWriter(new OutputStreamWriter(zos, "UTF-8"));
        } else {
          this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF8"));
        }
      }
      this.onDocumentStartInternal(jochreDocument);
    } catch (IOException e) {
      LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
      throw new RuntimeException(e);
    }
  }

  protected abstract void onDocumentStartInternal(JochreDocument jochreDocument);

  @Override
  public final void onDocumentComplete(JochreDocument jochreDocument) {
    try {
      this.onDocumentCompleteInternal(jochreDocument);
      this.writer.flush();
      this.writer.close();
    } catch (IOException e) {
      LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
      throw new RuntimeException(e);
    }
  }

  protected abstract void onDocumentCompleteInternal(JochreDocument jochreDocument);

  @Override
  public void onAnalysisComplete() {
  }

  public boolean isIncludeDate() {
    return includeDate;
  }

  public void setIncludeDate(boolean includeDate) {
    this.includeDate = includeDate;
  }

  public String getBaseName() {
    return baseName;
  }

  public void setBaseName(String baseName) {
    this.baseName = baseName;
  }

}
