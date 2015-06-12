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
* Outputs to Alto 3.0 XML format, see http://www.loc.gov/standards/alto/
**/
class AltoXMLExporter implements DocumentObserver {
	private static final Log LOG = LogFactory.getLog(AltoXMLExporter.class);
	private Writer writer;
	private Template template;
	
	public AltoXMLExporter(Writer writer) {
		super();
		try {
			this.writer = writer;
			Configuration cfg = new Configuration();
			cfg.setCacheStorage(new NullCacheStorage());
			cfg.setObjectWrapper(new DefaultObjectWrapper());
	
			Reader templateReader = new BufferedReader(new InputStreamReader(AltoXMLExporter.class.getResourceAsStream("alto_body_3_0.ftl")));
			this.template = new Template("alto_body", templateReader, cfg);
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	@Override
	public void onImageStart(JochreImage jochreImage) {
	}


	@Override
	public void onDocumentStart(JochreDocument jochreDocument) {
		try {
			Configuration cfg = new Configuration();
			cfg.setCacheStorage(new NullCacheStorage());
			cfg.setObjectWrapper(new DefaultObjectWrapper());

			Reader templateReader = new BufferedReader(new InputStreamReader(AltoXMLExporter.class.getResourceAsStream("alto_header_3_0.ftl")));
			Map<String,Object> model = new HashMap<String, Object>();
			model.put("document", jochreDocument);
			
			String version = this.getClass().getPackage().getImplementationVersion();
			if (version==null)
				version = "unknown";
			
			model.put("version", version);
			
			Template template = new Template("alto_header", templateReader, cfg);
			template.process(model, writer);
			writer.flush();
		} catch (TemplateException te) {
			LogUtils.logError(LOG, te);
			throw new RuntimeException(te);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public void onPageStart(JochrePage jochrePage) {
	}

	@Override
	public void onImageComplete(JochreImage jochreImage) {
		try {
			Map<String,Object> model = new HashMap<String, Object>();
			model.put("image", jochreImage);
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
	public void onPageComplete(JochrePage jochrePage) {
	}

	@Override
	public void onDocumentComplete(JochreDocument jochreDocument) {
		try {
			Configuration cfg = new Configuration();
			cfg.setCacheStorage(new NullCacheStorage());
			cfg.setObjectWrapper(new DefaultObjectWrapper());

			Reader templateReader = new BufferedReader(new InputStreamReader(AltoXMLExporter.class.getResourceAsStream("alto_footer_3_0.ftl")));
			Map<String,Object> model = new HashMap<String, Object>();
			model.put("document", jochreDocument);
			
			Template template = new Template("alto_footer", templateReader, cfg);
			template.process(model, writer);
			writer.flush();
		} catch (TemplateException te) {
			LogUtils.logError(LOG, te);
			throw new RuntimeException(te);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
}
