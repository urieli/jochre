package com.joliciel.jochre.output;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.doc.DocumentObserver;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.graphics.JochreImage;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

/**
 * Outputs to the XML spec indicated by http://finereader.abbyy.com/
 **/
public class AbbyyFineReader8Exporter extends AbstractExporter implements DocumentObserver {
	private static final Logger LOG = LoggerFactory.getLogger(AbbyyFineReader8Exporter.class);
	private Template template;
	private boolean firstPage = true;

	JochreImage jochreImage = null;

	public AbbyyFineReader8Exporter(File outDir) {
		super(outDir, "_abbyy8.xml");
		this.initialize();
	}

	public AbbyyFineReader8Exporter(Writer writer) {
		super(writer);
		this.initialize();
	}

	private void initialize() {
		try {
			Configuration cfg = new Configuration(new Version(2, 3, 23));
			cfg.setCacheStorage(new NullCacheStorage());
			cfg.setObjectWrapper(new DefaultObjectWrapperBuilder(new Version(2, 3, 23)).build());

			Reader templateReader = new BufferedReader(new InputStreamReader(AltoXMLExporter.class.getResourceAsStream("abbyy_8.ftl")));
			this.template = new Template("freemarkerTemplate", templateReader, cfg);
		} catch (IOException e) {
			LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onImageStart(JochreImage jochreImage) {
		this.jochreImage = jochreImage;
	}

	void process(Map<String, Object> model) {
		try {
			template.process(model, writer);
			writer.flush();
		} catch (TemplateException e) {
			LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onDocumentStartInternal(JochreDocument jochreDocument) {
		try {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<document version=\"1.0\" producer=\"Jochre XML Exporter for ABBYY FineReader\"" + " pagesCount=\""
					+ jochreDocument.getTotalPageCount() + "\"" + " xmlns=\"http://www.abbyy.com/FineReader_xml/FineReader6-schema-v1.xml\""
					+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
					+ " xsi:schemaLocation=\"http://www.abbyy.com/FineReader_xml/FineReader6-schema-v1.xml http://www.abbyy.com/FineReader_xml/FineReader6-schema-v1.xml\">\n");
			writer.flush();
		} catch (IOException e) {
			LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
			throw new RuntimeException(e);
		}

	}

	@Override
	public void onPageStart(JochrePage jochrePage) {
		try {
			if (firstPage) {
				if (jochrePage.getIndex() > 1) {
					for (int i = 1; i < jochrePage.getIndex(); i++) {
						writer.write("<page/>\n");
					}
				}
				firstPage = false;
			}
		} catch (IOException e) {
			LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onImageComplete(JochreImage jochreImage) {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("image", jochreImage);
		this.process(model);
	}

	@Override
	public void onPageComplete(JochrePage jochrePage) {
	}

	@Override
	public void onDocumentCompleteInternal(JochreDocument jochreDocument) {
		try {
			writer.write("</document>\n");
			writer.flush();
		} catch (IOException e) {
			LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
			throw new RuntimeException(e);
		}
	}
}
