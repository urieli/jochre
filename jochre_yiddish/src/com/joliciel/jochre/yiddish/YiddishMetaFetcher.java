package com.joliciel.jochre.yiddish;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.joliciel.jochre.utils.pdf.PdfMetadataReader;
import com.joliciel.jochre.utils.text.DiacriticRemover;
import com.joliciel.talismane.utils.LogUtils;

public class YiddishMetaFetcher {
	private static final Log LOG = LogFactory.getLog(YiddishMetaFetcher.class);

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
			String fileBase = pdfFile.getName().substring(0, pdfFile.getName().length()-".pdf".length());
			File metaFile = new File(dir, fileBase + "_meta.xml");
			if (!metaFile.exists()||metaFile.length()==0) {
				try {
					Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(metaFile, false), "UTF-8"));
					this.fetchMetaData(pdfFile, writer);
					writer.close();
				} catch (Exception e) {
					LogUtils.logError(LOG, e);
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
		if (url==null || !url.startsWith("http"))
			return;
		
		String reference = url.substring(url.lastIndexOf('/')+1);
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
        writer.write("  <identifier>" + id + "</identifier>\n");
        writer.write("  <identifier-access>" + bookUrl + "</identifier-access>\n");
        writer.write("  <title>" + title + "</title>\n");
        writer.write("  <volume>" + volume + "</volume>\n");
        writer.write("  <creator>" + author + "</creator>\n");
        writer.write("  <publisher>" + publisher + "</publisher>\n");
        writer.write("  <date>" + date + "</date>\n");
        writer.write("  <creator-alt-script>" + authorYid + "</creator-alt-script>\n");
        writer.write("  <title-alt-script>" + titleYid + "</title-alt-script>\n");
        writer.write("  <imagecount>" + pageCount + "</imagecount>\n");
        writer.write("</metadata>\n");
        writer.flush();
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
}
