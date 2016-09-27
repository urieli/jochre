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
 * Outputs to Alto 3.0 XML format, see http://www.loc.gov/standards/alto/
 **/
public class AltoXMLExporter extends AbstractExporter implements DocumentObserver {
	private static final Logger LOG = LoggerFactory.getLogger(AltoXMLExporter.class);
	private Template template;

	public AltoXMLExporter(File outDir) {
		super(outDir, "_alto3.xml");
		this.initialize();
	}

	public AltoXMLExporter(Writer writer) {
		super(writer);
		this.initialize();
	}

	private void initialize() {
		try {
			Configuration cfg = new Configuration(new Version(2, 3, 23));
			cfg.setCacheStorage(new NullCacheStorage());
			cfg.setObjectWrapper(new DefaultObjectWrapperBuilder(new Version(2, 3, 23)).build());

			Reader templateReader = new BufferedReader(new InputStreamReader(AltoXMLExporter.class.getResourceAsStream("alto_body_3_0.ftl")));
			this.template = new Template("alto_body", templateReader, cfg);
		} catch (IOException e) {
			LOG.error("Failed writing to " + this.getClass().getSimpleName(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onImageStart(JochreImage jochreImage) {
	}

	@Override
	public void onDocumentStartInternal(JochreDocument jochreDocument) {
		try {
			Configuration cfg = new Configuration(new Version(2, 3, 23));
			cfg.setCacheStorage(new NullCacheStorage());
			cfg.setObjectWrapper(new DefaultObjectWrapperBuilder(new Version(2, 3, 23)).build());

			Reader templateReader = new BufferedReader(new InputStreamReader(AltoXMLExporter.class.getResourceAsStream("alto_header_3_0.ftl")));
			Map<String, Object> model = new HashMap<String, Object>();
			model.put("document", jochreDocument);

			String version = this.getClass().getPackage().getImplementationVersion();
			if (version == null)
				version = "unknown";

			model.put("version", version);

			Template template = new Template("alto_header", templateReader, cfg);
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
	public void onPageStart(JochrePage jochrePage) {
	}

	@Override
	public void onImageComplete(JochreImage jochreImage) {
		try {
			Map<String, Object> model = new HashMap<String, Object>();
			model.put("image", jochreImage);
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
	public void onPageComplete(JochrePage jochrePage) {
	}

	@Override
	public void onDocumentCompleteInternal(JochreDocument jochreDocument) {
		try {
			Configuration cfg = new Configuration(new Version(2, 3, 23));
			cfg.setCacheStorage(new NullCacheStorage());
			cfg.setObjectWrapper(new DefaultObjectWrapperBuilder(new Version(2, 3, 23)).build());

			Reader templateReader = new BufferedReader(new InputStreamReader(AltoXMLExporter.class.getResourceAsStream("alto_footer_3_0.ftl")));
			Map<String, Object> model = new HashMap<String, Object>();
			model.put("document", jochreDocument);

			Template template = new Template("alto_footer", templateReader, cfg);
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
}
