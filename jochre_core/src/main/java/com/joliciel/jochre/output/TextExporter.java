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
import com.joliciel.jochre.utils.JochreException;
import com.joliciel.talismane.utils.LogUtils;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

/**
* Outputs plain text based on guesses.
**/
class TextExporter extends AbstractExporter implements DocumentObserver {
	private static final Log LOG = LogFactory.getLog(TextExporter.class);
	private Template template;

	JochreImage jochreImage = null;

	public TextExporter(File outDir) {
		super(outDir,".txt");
		this.initialize();
	}
	
	public TextExporter(Writer writer) {
		super(writer);
		this.initialize();
	}
	
	private void initialize() {
		try {
			Configuration cfg = new Configuration(new Version(2,3,23));
			cfg.setCacheStorage(new NullCacheStorage());
			cfg.setObjectWrapper(new DefaultObjectWrapperBuilder(new Version(2,3,23)).build());
		
			Reader templateReader = new BufferedReader(new InputStreamReader(AltoXMLExporter.class.getResourceAsStream("text.ftl")));
			this.template = new Template("freemarkerTemplate", templateReader, cfg);
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new JochreException(ioe);
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
			throw new JochreException(te);
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new JochreException(ioe);
		}
	}

	@Override
	public void onDocumentStartInternal(JochreDocument jochreDocument) {
		
	}

	@Override
	public void onPageStart(JochrePage jochrePage) {
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
	public void onDocumentCompleteInternal(JochreDocument jochreDocument) {
	}
}
