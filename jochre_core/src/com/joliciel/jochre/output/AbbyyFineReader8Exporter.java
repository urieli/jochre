package com.joliciel.jochre.output;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.talismane.utils.LogUtils;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
* Outputs to the XML spec indicated by http://finereader.abbyy.com/
**/
class AbbyyFineReader8Exporter implements DocumentObserver {
	private static final Log LOG = LogFactory.getLog(AbbyyFineReader8Exporter.class);
	private Writer writer;
	private Template template;
	private boolean firstPage = true;

	JochreImage jochreImage = null;
	public AbbyyFineReader8Exporter(Writer writer) {
		this(writer, new BufferedReader(new InputStreamReader(AbbyyFineReader8Exporter.class.getResourceAsStream("abbyy_8.ftl"))));
	}
	
	AbbyyFineReader8Exporter(Writer writer, Reader templateReader) {
		super();
		try {
			this.writer = writer;
			Configuration cfg = new Configuration();
			cfg.setCacheStorage(new NullCacheStorage());
			cfg.setObjectWrapper(new DefaultObjectWrapper());
	
			this.template = new Template("freemarkerTemplate", templateReader, cfg);
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	@Override
	public void onImageStart(JochreImage jochreImage) {
		this.jochreImage = jochreImage;
	}

	void process(Map<String,Object> model) {
		try {
			template.process(model, writer);
			writer.flush();
		} catch (TemplateException te) {
			LogUtils.logError(LOG, te);
			throw new RuntimeException(te);
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	@Override
	public void onDocumentStart(JochreDocument jochreDocument) {
		try {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<document version=\"1.0\" producer=\"Jochre XML Exporter for ABBYY FineReader\"" +
					" pagesCount=\"" + jochreDocument.getTotalPageCount() + "\"" +
					" xmlns=\"http://www.abbyy.com/FineReader_xml/FineReader6-schema-v1.xml\"" +
					" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
					" xsi:schemaLocation=\"http://www.abbyy.com/FineReader_xml/FineReader6-schema-v1.xml http://www.abbyy.com/FineReader_xml/FineReader6-schema-v1.xml\">\n");
			writer.flush();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public void onPageStart(JochrePage jochrePage) {
		try {
			if (firstPage) {
				if (jochrePage.getIndex()>1) {
					for (int i=1; i<jochrePage.getIndex(); i++) {
						writer.write("<page/>\n");
					}
				}
				firstPage = false;
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onImageComplete(JochreImage jochreImage) {
		Map<String,Object> model = new HashMap<String, Object>();
		model.put("image", jochreImage);
		this.process(model);
	}

	@Override
	public void onPageComplete(JochrePage jochrePage) {
	}

	@Override
	public void onDocumentComplete(JochreDocument jochreDocument) {
		try {
			writer.write("</document>\n");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
}
