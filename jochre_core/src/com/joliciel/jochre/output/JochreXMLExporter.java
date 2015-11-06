package com.joliciel.jochre.output;

import java.io.BufferedReader;
import java.io.File;
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
* Outputs to Jochre's lossless XML format.
**/
class JochreXMLExporter extends AbstractExporter implements DocumentObserver {
	private static final Log LOG = LogFactory.getLog(JochreXMLExporter.class);
	private Writer writer;
	private Template template;

	JochreImage jochreImage = null;
	

	public JochreXMLExporter(File outDir) {
		super(outDir,"_jochre.xml");
		this.initialize();
	}
	
	public JochreXMLExporter(Writer writer) {
		super(writer);
		this.initialize();
	}
	
	private void initialize() {
		try {
			Configuration cfg = new Configuration();
			cfg.setCacheStorage(new NullCacheStorage());
			cfg.setObjectWrapper(new DefaultObjectWrapper());
		
			Reader templateReader = new BufferedReader(new InputStreamReader(AltoXMLExporter.class.getResourceAsStream("jochre.ftl")));
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
	public void onDocumentStartInternal(JochreDocument jochreDocument) {
		try {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<document version=\"1.0\" producer=\"Jochre XML Exporter\"" +
					" pageCount=\"" + jochreDocument.getTotalPageCount() + "\">\n");
			writer.flush();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public void onPageStart(JochrePage jochrePage) {
		try {
			writer.write("<page index=\"" + jochrePage.getIndex() + "\">\n");
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
		try {
			writer.write("</page>\n");
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onDocumentCompleteInternal(JochreDocument jochreDocument) {
		try {
			writer.write("</document>\n");
			writer.flush();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
}
