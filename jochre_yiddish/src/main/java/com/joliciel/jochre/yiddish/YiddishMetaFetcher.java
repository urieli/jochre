package com.joliciel.jochre.yiddish;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.joliciel.jochre.utils.pdf.PdfMetadataReader;
import com.joliciel.jochre.utils.text.DiacriticRemover;

public class YiddishMetaFetcher {
  private static final Logger LOG = LoggerFactory.getLogger(YiddishMetaFetcher.class);

  private boolean forceUpdate = false;

  public YiddishMetaFetcher() {

  }

  public void fetchMetaData(File dir) throws Exception {
    File[] pdfFiles = dir.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.getName().toLowerCase().endsWith(".pdf");
      }
    });

    Arrays.sort(pdfFiles);

    for (File pdfFile : pdfFiles) {
      String fileBase = pdfFile.getName().substring(0, pdfFile.getName().length() - ".pdf".length());
      File metaFile = new File(dir, fileBase + "_meta.xml");
      if (!metaFile.exists() || metaFile.length() == 0 || forceUpdate) {
        try {
          Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(metaFile, false), "UTF-8"));
          this.fetchMetaData(pdfFile, writer);
          writer.close();
        } catch (Exception e) {
          LOG.error("Failure fetching meta data from " + metaFile.getAbsolutePath(), e);
          metaFile.delete();
        }
      }
    }

    File[] subdirs = dir.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory();
      }
    });

    Arrays.sort(subdirs);

    for (File subdir : subdirs) {
      this.fetchMetaData(subdir);
    }
  }

  public void fetchMetaData(File pdfFile, Writer writer) throws Exception {
    PdfMetadataReader pdfMetadataReader = new PdfMetadataReader(pdfFile);
    Map<String, String> metadata = pdfMetadataReader.getFields();
    pdfMetadataReader.close();
    String url = metadata.get("Keywords");
    if (url == null || !url.startsWith("http"))
      return;

    String reference = url.substring(url.lastIndexOf('/') + 1);
    URL metaUrl = new URL("https://archive.org/download/" + reference + "/" + reference + "_meta.xml");

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document dom = db.parse(metaUrl.openStream());
    Element docElement = dom.getDocumentElement();

    XPathFactory xpf = XPathFactory.newInstance();
    XPath xp = xpf.newXPath();

    String id = xp.evaluate("/metadata/identifier/text()", docElement);
    String bookUrl = xp.evaluate("/metadata/identifier-access/text()", docElement);
    String title = xp.evaluate("/metadata/title/text()", docElement);
    String author = xp.evaluate("/metadata/creator/text()", docElement);
    String publisher = xp.evaluate("/metadata/publisher/text()", docElement);
    String date = xp.evaluate("/metadata/date/text()", docElement);
    String authorYid = xp.evaluate("/metadata/creator-alt-script/text()", docElement);
    String titleYid = xp.evaluate("/metadata/title-alt-script/text()", docElement);
    String pageCount = xp.evaluate("/metadata/imagecount/text()", docElement);
    String volume = xp.evaluate("/metadata/volume/text()", docElement);

    title = DiacriticRemover.apply(title);
    author = DiacriticRemover.apply(author);
    publisher = DiacriticRemover.apply(publisher);

    LOG.debug("id: " + id);
    LOG.debug("bookUrl: " + bookUrl);
    LOG.debug("title: " + title);
    LOG.debug("volume: " + volume);
    LOG.debug("author: " + author);
    LOG.debug("publisher: " + publisher);
    LOG.debug("date: " + date);
    LOG.debug("authorYid: " + authorYid);
    LOG.debug("titleYid: " + titleYid);
    LOG.debug("pageCount: " + pageCount);

    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    writer.write("<metadata>\n");
    writer.write("  <identifier>" + StringEscapeUtils.escapeXml10(id) + "</identifier>\n");
    writer.write("  <identifier-access>" + StringEscapeUtils.escapeXml10(bookUrl) + "</identifier-access>\n");
    writer.write("  <title>" + StringEscapeUtils.escapeXml10(title) + "</title>\n");
    writer.write("  <volume>" + StringEscapeUtils.escapeXml10(volume) + "</volume>\n");
    writer.write("  <creator>" + StringEscapeUtils.escapeXml10(author) + "</creator>\n");
    writer.write("  <publisher>" + StringEscapeUtils.escapeXml10(publisher) + "</publisher>\n");
    writer.write("  <date>" + StringEscapeUtils.escapeXml10(date) + "</date>\n");
    writer.write("  <creator-alt-script>" + StringEscapeUtils.escapeXml10(authorYid) + "</creator-alt-script>\n");
    writer.write("  <title-alt-script>" + StringEscapeUtils.escapeXml10(titleYid) + "</title-alt-script>\n");
    writer.write("  <imagecount>" + StringEscapeUtils.escapeXml10(pageCount) + "</imagecount>\n");
    writer.write("</metadata>\n");
    writer.flush();
  }

  public void buildBookHtml(File dir, Writer writer) throws Exception {
    Map<String, Map<String, String>> metaMap = new TreeMap<>();
    this.collectBookMeta(dir, metaMap);

    writer.write("<html>");
    writer.write("<head>");
    writer.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
    writer.write("<title>Jochre Book List</title>");
    writer.write("<style>");
    writer.write("table, th, td {");
    writer.write("  border: 1px solid black;");
    writer.write("}");
    writer.write("</style>");
    writer.write("</head>");
    writer.write("<body>");
    writer.write("<h1>Jochre Book List</h1>");
    int i = 1;
    for (Map<String, String> myMeta : metaMap.values()) {
      writer.write("<table style=\"width: 600px;\">");
      writer.write("<tr><td style=\"width: 150px;\"><b>id " + i + "</b></td><td><a href=\"" + myMeta.get("bookUrl") + "\">"
          + StringEscapeUtils.escapeHtml4(myMeta.get("id")) + "</a></td></tr>");
      writer.write("<tr><td><b>title</b></td><td>" + StringEscapeUtils.escapeHtml4(myMeta.get("title")) + "</td></tr>");
      writer.write("<tr><td><b>author</b></td><td>" + StringEscapeUtils.escapeHtml4(myMeta.get("author")) + "</td></tr>");
      writer.write("<tr><td><b>publisher</b></td><td>" + StringEscapeUtils.escapeHtml4(myMeta.get("publisher")) + "</td></tr>");
      writer.write("<tr><td><b>date</b></td><td>" + StringEscapeUtils.escapeHtml4(myMeta.get("date")) + "</td></tr>");
      writer.write(
          "<tr><td><b>authorLang</b></td><td style=\"direction: rtl;\">" + StringEscapeUtils.escapeHtml4(myMeta.get("authorLang")) + "</td></tr>");
      writer.write("<tr><td><b>titleLang</b></td><td style=\"direction: rtl;\">" + StringEscapeUtils.escapeHtml4(myMeta.get("titleLang")) + "</td></tr>");
      writer.write("</table><br/>");
      writer.flush();
      i++;
    }
    writer.write("</body>");
    writer.flush();
  }

  public void collectBookMeta(File dir, Map<String, Map<String, String>> metaMap) throws Exception {
    File[] metaFiles = dir.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.getName().toLowerCase().endsWith("_meta.xml");
      }
    });

    Arrays.sort(metaFiles);

    for (File metaFile : metaFiles) {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document dom = db.parse(metaFile);
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

      Map<String, String> myMeta = new HashMap<>();
      myMeta.put("id", id);
      myMeta.put("bookUrl", bookUrl);
      myMeta.put("title", title);
      myMeta.put("author", author);
      myMeta.put("publisher", publisher);
      myMeta.put("date", date);
      myMeta.put("authorLang", authorLang);
      myMeta.put("titleLang", titleLang);

      LOG.info(myMeta.toString());
      String key = authorLang == null || authorLang.length() == 0 ? author + title + id : authorLang + titleLang + id;
      metaMap.put(key, myMeta);
    }

    File[] subdirs = dir.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory();
      }
    });

    Arrays.sort(subdirs);

    for (File subdir : subdirs) {
      this.collectBookMeta(subdir, metaMap);
    }
  }

  public static void main(String[] args) throws Exception {
    String path = args[0];
    String outPath = args[1];
    File pdfFile = new File(path);
    File outFile = new File(outPath);
    File outDir = outFile.getParentFile();
    outDir.mkdirs();

    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, false), "UTF-8"));
    YiddishMetaFetcher fetcher = new YiddishMetaFetcher();
    fetcher.fetchMetaData(pdfFile, writer);
    writer.close();
  }

  public boolean isForceUpdate() {
    return forceUpdate;
  }

  public void setForceUpdate(boolean forceUpdate) {
    this.forceUpdate = forceUpdate;
  }

}
